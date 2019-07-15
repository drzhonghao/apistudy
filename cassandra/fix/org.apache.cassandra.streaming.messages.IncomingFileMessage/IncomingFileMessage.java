

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.apache.cassandra.io.sstable.SSTableMultiWriter;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputStreamPlus;
import org.apache.cassandra.streaming.StreamSession;
import org.apache.cassandra.streaming.messages.FileMessageHeader;
import org.apache.cassandra.streaming.messages.StreamMessage;
import org.apache.cassandra.utils.JVMStabilityInspector;

import static org.apache.cassandra.streaming.messages.StreamMessage.Type.FILE;


public class IncomingFileMessage extends StreamMessage {
	public static StreamMessage.Serializer<IncomingFileMessage> serializer = new StreamMessage.Serializer<IncomingFileMessage>() {
		@SuppressWarnings("resource")
		public IncomingFileMessage deserialize(ReadableByteChannel in, int version, StreamSession session) throws IOException {
			DataInputPlus input = new DataInputPlus.DataInputStreamPlus(Channels.newInputStream(in));
			try {
			} catch (Throwable t) {
				JVMStabilityInspector.inspectThrowable(t);
				throw t;
			}
			return null;
		}

		public void serialize(IncomingFileMessage message, DataOutputStreamPlus out, int version, StreamSession session) {
			throw new UnsupportedOperationException("Not allowed to call serialize on an incoming file");
		}
	};

	public FileMessageHeader header;

	public SSTableMultiWriter sstable;

	public IncomingFileMessage(SSTableMultiWriter sstable, FileMessageHeader header) {
		super(FILE);
		this.header = header;
		this.sstable = sstable;
	}

	@Override
	public String toString() {
		return ((("File (" + (header)) + ", file: ") + (sstable.getFilename())) + ")";
	}
}

