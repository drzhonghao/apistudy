

import com.google.common.primitives.Ints;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.DataPosition;
import org.apache.cassandra.io.util.FileDataInput;
import org.apache.cassandra.io.util.ReaderFileProxy;
import org.apache.cassandra.io.util.Rebufferer;
import org.apache.cassandra.io.util.RebufferingInputStream;
import org.apache.cassandra.utils.concurrent.SharedCloseableImpl;


public class RandomAccessReader extends RebufferingInputStream implements FileDataInput {
	public static final int DEFAULT_BUFFER_SIZE = 4096;

	private long markedPointer;

	final Rebufferer rebufferer;

	private Rebufferer.BufferHolder bufferHolder = Rebufferer.EMPTY;

	RandomAccessReader(Rebufferer rebufferer) {
		super(Rebufferer.EMPTY.buffer());
		this.rebufferer = rebufferer;
	}

	public void reBuffer() {
		if (isEOF())
			return;

		reBufferAt(current());
	}

	private void reBufferAt(long position) {
		bufferHolder.release();
		bufferHolder = rebufferer.rebuffer(position);
		buffer = bufferHolder.buffer();
		buffer.position(Ints.checkedCast((position - (bufferHolder.offset()))));
		assert (buffer.order()) == (ByteOrder.BIG_ENDIAN) : "Buffer must have BIG ENDIAN byte ordering";
	}

	@Override
	public long getFilePointer() {
		if ((buffer) == null)
			return rebufferer.fileLength();

		return current();
	}

	protected long current() {
		return (bufferHolder.offset()) + (buffer.position());
	}

	public String getPath() {
		return getChannel().filePath();
	}

	public ChannelProxy getChannel() {
		return rebufferer.channel();
	}

	@Override
	public void reset() throws IOException {
		seek(markedPointer);
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	public long bytesPastMark() {
		long bytes = (current()) - (markedPointer);
		assert bytes >= 0;
		return bytes;
	}

	public DataPosition mark() {
		markedPointer = current();
		return new RandomAccessReader.BufferedRandomAccessFileMark(markedPointer);
	}

	public void reset(DataPosition mark) {
		assert mark instanceof RandomAccessReader.BufferedRandomAccessFileMark;
		seek(((RandomAccessReader.BufferedRandomAccessFileMark) (mark)).pointer);
	}

	public long bytesPastMark(DataPosition mark) {
		assert mark instanceof RandomAccessReader.BufferedRandomAccessFileMark;
		long bytes = (current()) - (((RandomAccessReader.BufferedRandomAccessFileMark) (mark)).pointer);
		assert bytes >= 0;
		return bytes;
	}

	public boolean isEOF() {
		return (current()) == (length());
	}

	public long bytesRemaining() {
		return (length()) - (getFilePointer());
	}

	@Override
	public int available() throws IOException {
		return Ints.saturatedCast(bytesRemaining());
	}

	@Override
	public void close() {
		if ((buffer) == null)
			return;

		bufferHolder.release();
		rebufferer.closeReader();
		buffer = null;
		bufferHolder = null;
	}

	@Override
	public String toString() {
		return ((getClass().getSimpleName()) + ':') + (rebufferer.toString());
	}

	private static class BufferedRandomAccessFileMark implements DataPosition {
		final long pointer;

		private BufferedRandomAccessFileMark(long pointer) {
			this.pointer = pointer;
		}
	}

	@Override
	public void seek(long newPosition) {
		if (newPosition < 0)
			throw new IllegalArgumentException("new position should not be negative");

		if ((buffer) == null)
			throw new IllegalStateException("Attempted to seek in a closed RAR");

		long bufferOffset = bufferHolder.offset();
		if ((newPosition >= bufferOffset) && (newPosition < (bufferOffset + (buffer.limit())))) {
			buffer.position(((int) (newPosition - bufferOffset)));
			return;
		}
		if (newPosition > (length()))
			throw new IllegalArgumentException(String.format("Unable to seek to position %d in %s (%d bytes) in read-only mode", newPosition, getPath(), length()));

		reBufferAt(newPosition);
	}

	public final String readLine() throws IOException {
		StringBuilder line = new StringBuilder(80);
		boolean foundTerminator = false;
		long unreadPosition = -1;
		while (true) {
			int nextByte = read();
			switch (nextByte) {
				case -1 :
					return (line.length()) != 0 ? line.toString() : null;
				case ((byte) ('\r')) :
					if (foundTerminator) {
						seek(unreadPosition);
						return line.toString();
					}
					foundTerminator = true;
					unreadPosition = getPosition();
					break;
				case ((byte) ('\n')) :
					return line.toString();
				default :
					if (foundTerminator) {
						seek(unreadPosition);
						return line.toString();
					}
					line.append(((char) (nextByte)));
			}
		} 
	}

	public long length() {
		return rebufferer.fileLength();
	}

	public long getPosition() {
		return current();
	}

	public double getCrcCheckChance() {
		return rebufferer.getCrcCheckChance();
	}

	static class RandomAccessReaderWithOwnChannel extends RandomAccessReader {
		RandomAccessReaderWithOwnChannel(Rebufferer rebufferer) {
			super(rebufferer);
		}

		@Override
		public void close() {
			try {
				super.close();
			} finally {
				try {
					rebufferer.close();
				} finally {
					getChannel().close();
				}
			}
		}
	}

	@SuppressWarnings("resource")
	public static RandomAccessReader open(File file) {
		ChannelProxy channel = new ChannelProxy(file);
		try {
		} catch (Throwable t) {
			channel.close();
			throw t;
		}
		return null;
	}
}

