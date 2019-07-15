

import java.io.IOException;
import java.net.SocketException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.io.util.DataOutputStreamPlus;
import org.apache.cassandra.streaming.StreamSession;


public abstract class StreamMessage {
	public static final int VERSION_20 = 2;

	public static final int VERSION_22 = 3;

	public static final int VERSION_30 = 4;

	public static final int CURRENT_VERSION = StreamMessage.VERSION_30;

	private volatile transient boolean sent = false;

	public static void serialize(StreamMessage message, DataOutputStreamPlus out, int version, StreamSession session) throws IOException {
		ByteBuffer buff = ByteBuffer.allocate(1);
		buff.put(message.type.type);
		buff.flip();
		out.write(buff);
		message.type.outSerializer.serialize(message, out, version, session);
	}

	public static StreamMessage deserialize(ReadableByteChannel in, int version, StreamSession session) throws IOException {
		ByteBuffer buff = ByteBuffer.allocate(1);
		int readBytes = in.read(buff);
		if (readBytes > 0) {
			buff.flip();
			StreamMessage.Type type = StreamMessage.Type.get(buff.get());
			return type.inSerializer.deserialize(in, version, session);
		}else
			if (readBytes == 0) {
				return null;
			}else {
				throw new SocketException("End-of-stream reached");
			}

	}

	public void sent() {
		sent = true;
	}

	public boolean wasSent() {
		return sent;
	}

	public static interface Serializer<V extends StreamMessage> {
		V deserialize(ReadableByteChannel in, int version, StreamSession session) throws IOException;

		void serialize(V message, DataOutputStreamPlus out, int version, StreamSession session) throws IOException;
	}

	public static enum Type {
		;

		public static StreamMessage.Type get(byte type) {
			for (StreamMessage.Type t : StreamMessage.Type.values()) {
				if ((t.type) == type)
					return t;

			}
			throw new IllegalArgumentException(("Unknown type " + type));
		}

		private final byte type;

		public final int priority;

		public final StreamMessage.Serializer<StreamMessage> inSerializer;

		public final StreamMessage.Serializer<StreamMessage> outSerializer;

		@SuppressWarnings("unchecked")
		private Type(int type, int priority, StreamMessage.Serializer serializer) {
			this(type, priority, serializer, serializer);
		}

		@SuppressWarnings("unchecked")
		private Type(int type, int priority, StreamMessage.Serializer inSerializer, StreamMessage.Serializer outSerializer) {
			this.type = ((byte) (type));
			this.priority = priority;
			this.inSerializer = inSerializer;
			this.outSerializer = outSerializer;
		}
	}

	public final StreamMessage.Type type;

	protected StreamMessage(StreamMessage.Type type) {
		this.type = type;
	}

	public int getPriority() {
		return type.priority;
	}
}

