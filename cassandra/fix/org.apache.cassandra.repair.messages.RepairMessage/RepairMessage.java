

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.repair.RepairJobDesc;

import static org.apache.cassandra.net.MessagingService.Verb.REPAIR_MESSAGE;


public abstract class RepairMessage {
	public static final IVersionedSerializer<RepairMessage> serializer = new RepairMessage.RepairMessageSerializer();

	public static interface MessageSerializer<T extends RepairMessage> extends IVersionedSerializer<T> {}

	public static enum Type {
		;

		private final byte type;

		private final RepairMessage.MessageSerializer<RepairMessage> serializer;

		private Type(int type, RepairMessage.MessageSerializer<RepairMessage> serializer) {
			this.type = ((byte) (type));
			this.serializer = serializer;
		}

		public static RepairMessage.Type fromByte(byte b) {
			for (RepairMessage.Type t : RepairMessage.Type.values()) {
				if ((t.type) == b)
					return t;

			}
			throw new IllegalArgumentException(("Unknown RepairMessage.Type: " + b));
		}
	}

	public final RepairMessage.Type messageType;

	public final RepairJobDesc desc;

	protected RepairMessage(RepairMessage.Type messageType, RepairJobDesc desc) {
		this.messageType = messageType;
		this.desc = desc;
	}

	public MessageOut<RepairMessage> createMessage() {
		return new MessageOut<>(REPAIR_MESSAGE, this, RepairMessage.serializer);
	}

	public static class RepairMessageSerializer implements RepairMessage.MessageSerializer<RepairMessage> {
		public void serialize(RepairMessage message, DataOutputPlus out, int version) throws IOException {
			out.write(message.messageType.type);
			message.messageType.serializer.serialize(message, out, version);
		}

		public RepairMessage deserialize(DataInputPlus in, int version) throws IOException {
			RepairMessage.Type messageType = RepairMessage.Type.fromByte(in.readByte());
			return messageType.serializer.deserialize(in, version);
		}

		public long serializedSize(RepairMessage message, int version) {
			long size = 1;
			size += message.messageType.serializer.serializedSize(message, version);
			return size;
		}
	}
}

