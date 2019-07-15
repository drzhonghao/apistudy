

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.apache.cassandra.hints.ChecksummedDataInput;
import org.apache.cassandra.hints.InputPosition;
import org.apache.cassandra.io.FSReadError;
import org.apache.cassandra.io.compress.ICompressor;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.RebufferingInputStream;
import org.apache.cassandra.utils.memory.BufferPool;


public final class CompressedChecksummedDataInput extends ChecksummedDataInput {
	private final ICompressor compressor;

	private volatile long filePosition = 0;

	private volatile long sourcePosition = 0;

	private volatile ByteBuffer compressedBuffer = null;

	public boolean isEOF() {
		return ((filePosition) == (channel.size())) && ((buffer.remaining()) == 0);
	}

	public long getSourcePosition() {
		return sourcePosition;
	}

	static class Position {
		final long bufferStart;

		final int bufferPosition;

		public Position(long sourcePosition, long bufferStart, int bufferPosition) {
			this.bufferStart = bufferStart;
			this.bufferPosition = bufferPosition;
		}

		public long subtract(InputPosition o) {
			CompressedChecksummedDataInput.Position other = ((CompressedChecksummedDataInput.Position) (o));
			return (((bufferStart) - (other.bufferStart)) + (bufferPosition)) - (other.bufferPosition);
		}
	}

	public InputPosition getSeekPosition() {
		return null;
	}

	public void seek(InputPosition p) {
		CompressedChecksummedDataInput.Position pos = ((CompressedChecksummedDataInput.Position) (p));
		bufferOffset = pos.bufferStart;
		buffer.position(0).limit(0);
		resetCrc();
		reBuffer();
		buffer.position(pos.bufferPosition);
		assert (bufferOffset) == (pos.bufferStart);
		assert (buffer.position()) == (pos.bufferPosition);
	}

	@Override
	protected void readBuffer() {
		sourcePosition = filePosition;
		if (isEOF())
			return;

		compressedBuffer.clear();
		channel.read(compressedBuffer, filePosition);
		compressedBuffer.rewind();
		buffer.clear();
		try {
			compressor.uncompress(compressedBuffer, buffer);
			buffer.flip();
		} catch (IOException e) {
			throw new FSReadError(e, getPath());
		}
	}

	@Override
	public void close() {
		BufferPool.put(compressedBuffer);
		super.close();
	}

	@SuppressWarnings("resource")
	public static ChecksummedDataInput upgradeInput(ChecksummedDataInput input, ICompressor compressor) {
		input.close();
		return null;
	}

	@com.google.common.annotations.VisibleForTesting
	ICompressor getCompressor() {
		return compressor;
	}
}

