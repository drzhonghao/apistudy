

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.UUID;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.io.compress.CompressionMetadata;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.io.util.DataOutputStreamPlus;
import org.apache.cassandra.streaming.StreamSession;
import org.apache.cassandra.streaming.messages.FileMessageHeader;
import org.apache.cassandra.streaming.messages.StreamMessage;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.concurrent.Ref;

import static org.apache.cassandra.streaming.messages.StreamMessage.Type.FILE;


public class OutgoingFileMessage extends StreamMessage {
	public static StreamMessage.Serializer<OutgoingFileMessage> serializer = new StreamMessage.Serializer<OutgoingFileMessage>() {
		public OutgoingFileMessage deserialize(ReadableByteChannel in, int version, StreamSession session) {
			throw new UnsupportedOperationException("Not allowed to call deserialize on an outgoing file");
		}

		public void serialize(OutgoingFileMessage message, DataOutputStreamPlus out, int version, StreamSession session) throws IOException {
			message.startTransfer();
			try {
				message.serialize(out, version, session);
				session.fileSent(message.header);
			} finally {
				message.finishTransfer();
			}
		}
	};

	public final FileMessageHeader header;

	private final Ref<SSTableReader> ref;

	private final String filename;

	private boolean completed = false;

	private boolean transferring = false;

	public OutgoingFileMessage(Ref<SSTableReader> ref, int sequenceNumber, long estimatedKeys, List<Pair<Long, Long>> sections, long repairedAt, boolean keepSSTableLevel) {
		super(FILE);
		this.ref = ref;
		SSTableReader sstable = ref.get();
		filename = sstable.getFilename();
		this.header = new FileMessageHeader(sstable.metadata.cfId, sequenceNumber, sstable.descriptor.version, sstable.descriptor.formatType, estimatedKeys, sections, (sstable.compression ? sstable.getCompressionMetadata() : null), repairedAt, (keepSSTableLevel ? sstable.getSSTableLevel() : 0), ((sstable.header) == null ? null : sstable.header.toComponent()));
	}

	public synchronized void serialize(DataOutputStreamPlus out, int version, StreamSession session) throws IOException {
		if (completed) {
			return;
		}
		final SSTableReader reader = ref.get();
	}

	@com.google.common.annotations.VisibleForTesting
	public synchronized void finishTransfer() {
		transferring = false;
		if (completed) {
			ref.release();
		}
	}

	@com.google.common.annotations.VisibleForTesting
	public synchronized void startTransfer() {
		if (completed)
			throw new RuntimeException(String.format("Transfer of file %s already completed or aborted (perhaps session failed?).", filename));

		transferring = true;
	}

	public synchronized void complete() {
		if (!(completed)) {
			completed = true;
			if (!(transferring)) {
				ref.release();
			}
		}
	}

	@Override
	public String toString() {
		return ((("File (" + (header)) + ", file: ") + (filename)) + ")";
	}
}

