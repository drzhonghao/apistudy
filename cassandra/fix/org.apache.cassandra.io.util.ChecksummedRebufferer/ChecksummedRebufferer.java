

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.apache.cassandra.io.util.BufferManagingRebufferer;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.ChunkReader;
import org.apache.cassandra.io.util.CorruptFileException;
import org.apache.cassandra.io.util.DataIntegrityMetadata;
import org.apache.cassandra.io.util.ReaderFileProxy;
import org.apache.cassandra.io.util.Rebufferer;
import org.apache.cassandra.utils.ByteBufferUtil;


class ChecksummedRebufferer extends BufferManagingRebufferer {
	private final DataIntegrityMetadata.ChecksumValidator validator;

	@Override
	public Rebufferer.BufferHolder rebuffer(long desiredPosition) {
		if (desiredPosition != ((offset) + (buffer.position())))
			validator.seek(desiredPosition);

		offset = alignedPosition(desiredPosition);
		source.readChunk(offset, buffer);
		try {
			validator.validate(ByteBufferUtil.getArray(buffer), 0, buffer.remaining());
		} catch (IOException e) {
			throw new CorruptFileException(e, channel().filePath());
		}
		return this;
	}

	@Override
	public void close() {
		try {
			source.close();
		} finally {
			validator.close();
		}
	}

	long alignedPosition(long desiredPosition) {
		return (desiredPosition / (buffer.capacity())) * (buffer.capacity());
	}
}

