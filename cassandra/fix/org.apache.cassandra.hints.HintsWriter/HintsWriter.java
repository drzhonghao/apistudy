

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.util.zip.CRC32;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.hints.Hint;
import org.apache.cassandra.io.FSWriteError;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.NativeLibrary;
import org.apache.cassandra.utils.SyncUtil;
import org.apache.cassandra.utils.Throwables;

import static org.apache.cassandra.utils.Throwables.FileOpType.WRITE;


class HintsWriter implements AutoCloseable {
	static final int PAGE_SIZE = 4096;

	private final File directory = null;

	private final File file = null;

	protected final FileChannel channel = null;

	private final int fd = 0;

	protected final CRC32 globalCRC = null;

	private volatile long lastSyncPosition = 0L;

	private void writeChecksum() {
	}

	public void close() {
		Throwables.perform(file, WRITE, this::doFsync, channel::close);
		writeChecksum();
	}

	public void fsync() {
		Throwables.perform(file, WRITE, this::doFsync);
	}

	private void doFsync() throws IOException {
		SyncUtil.force(channel, true);
		lastSyncPosition = channel.position();
	}

	HintsWriter.Session newSession(ByteBuffer buffer) {
		try {
			return new HintsWriter.Session(buffer, channel.size());
		} catch (IOException e) {
			throw new FSWriteError(e, file);
		}
	}

	protected void writeBuffer(ByteBuffer bb) throws IOException {
		FBUtilities.updateChecksum(globalCRC, bb);
		channel.write(bb);
	}

	final class Session implements AutoCloseable {
		private final ByteBuffer buffer;

		private final long initialSize;

		private long bytesWritten;

		Session(ByteBuffer buffer, long initialSize) {
			buffer.clear();
			bytesWritten = 0L;
			this.buffer = buffer;
			this.initialSize = initialSize;
		}

		@com.google.common.annotations.VisibleForTesting
		long getBytesWritten() {
			return bytesWritten;
		}

		long position() {
			return (initialSize) + (bytesWritten);
		}

		void append(ByteBuffer hint) throws IOException {
			bytesWritten += hint.remaining();
			if ((hint.remaining()) > (buffer.remaining())) {
				buffer.flip();
				writeBuffer(buffer);
				buffer.clear();
			}
			if ((hint.remaining()) <= (buffer.remaining())) {
				buffer.put(hint);
			}else {
				writeBuffer(hint);
			}
		}

		void append(Hint hint) throws IOException {
			CRC32 crc = new CRC32();
		}

		public void close() throws IOException {
			flushBuffer();
			maybeFsync();
			maybeSkipCache();
		}

		private void flushBuffer() throws IOException {
			buffer.flip();
			if ((buffer.remaining()) > 0) {
				writeBuffer(buffer);
			}
			buffer.clear();
		}

		private void maybeFsync() {
			if ((position()) >= ((lastSyncPosition) + ((DatabaseDescriptor.getTrickleFsyncIntervalInKb()) * 1024L)))
				fsync();

		}

		private void maybeSkipCache() {
			long position = position();
			if (position >= ((DatabaseDescriptor.getTrickleFsyncIntervalInKb()) * 1024L))
				NativeLibrary.trySkipCache(fd, 0, (position - (position % (HintsWriter.PAGE_SIZE))), file.getPath());

		}
	}
}

