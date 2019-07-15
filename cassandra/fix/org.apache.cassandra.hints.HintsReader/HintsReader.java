

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.RateLimiter;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import javax.annotation.Nullable;
import org.apache.cassandra.db.UnknownColumnFamilyException;
import org.apache.cassandra.hints.ChecksummedDataInput;
import org.apache.cassandra.hints.Hint;
import org.apache.cassandra.hints.InputPosition;
import org.apache.cassandra.io.FSReadError;
import org.apache.cassandra.io.util.RebufferingInputStream;
import org.apache.cassandra.utils.AbstractIterator;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class HintsReader implements AutoCloseable , Iterable<HintsReader.Page> {
	private static final Logger logger = LoggerFactory.getLogger(HintsReader.class);

	private static final int PAGE_SIZE = 512 << 10;

	private final File file = null;

	private final ChecksummedDataInput input = null;

	@Nullable
	private final RateLimiter rateLimiter = null;

	@SuppressWarnings("resource")
	static HintsReader open(File file, RateLimiter rateLimiter) {
		ChecksummedDataInput reader = ChecksummedDataInput.open(file);
		return null;
	}

	static HintsReader open(File file) {
		return HintsReader.open(file, null);
	}

	public void close() {
		input.close();
	}

	void seek(InputPosition newPosition) {
		input.seek(newPosition);
	}

	public Iterator<HintsReader.Page> iterator() {
		return new HintsReader.PagesIterator();
	}

	public ChecksummedDataInput getInput() {
		return input;
	}

	final class Page {
		public final InputPosition position;

		private Page(InputPosition inputPosition) {
			this.position = inputPosition;
		}

		Iterator<Hint> hintsIterator() {
			return new HintsReader.HintsIterator(position);
		}

		Iterator<ByteBuffer> buffersIterator() {
			return new HintsReader.BuffersIterator(position);
		}
	}

	final class PagesIterator extends AbstractIterator<HintsReader.Page> {
		@SuppressWarnings("resource")
		protected HintsReader.Page computeNext() {
			input.tryUncacheRead();
			if (input.isEOF())
				return endOfData();

			return new HintsReader.Page(input.getSeekPosition());
		}
	}

	final class HintsIterator extends AbstractIterator<Hint> {
		private final InputPosition offset;

		HintsIterator(InputPosition offset) {
			super();
			this.offset = offset;
		}

		protected Hint computeNext() {
			Hint hint;
			do {
				InputPosition position = input.getSeekPosition();
				if (input.isEOF())
					return endOfData();

				if ((position.subtract(offset)) >= (HintsReader.PAGE_SIZE))
					return endOfData();

				try {
					hint = computeNextInternal();
				} catch (EOFException e) {
					return endOfData();
				} catch (IOException e) {
					throw new FSReadError(e, file);
				}
			} while (hint == null );
			return hint;
		}

		private Hint computeNextInternal() throws IOException {
			input.resetCrc();
			input.resetLimit();
			int size = input.readInt();
			if (!(input.checkCrc()))
				throw new IOException("Digest mismatch exception");

			return readHint(size);
		}

		private Hint readHint(int size) throws IOException {
			if ((rateLimiter) != null)
				rateLimiter.acquire(size);

			input.limit(size);
			Hint hint;
			try {
				input.checkLimit(0);
			} catch (UnknownColumnFamilyException e) {
				input.skipBytes(Ints.checkedCast((size - (input.bytesPastLimit()))));
				hint = null;
			}
			if (input.checkCrc()) {
				hint = null;
				return hint;
			}
			return null;
		}
	}

	final class BuffersIterator extends AbstractIterator<ByteBuffer> {
		private final InputPosition offset;

		BuffersIterator(InputPosition offset) {
			super();
			this.offset = offset;
		}

		protected ByteBuffer computeNext() {
			ByteBuffer buffer;
			do {
				InputPosition position = input.getSeekPosition();
				if (input.isEOF())
					return endOfData();

				if ((position.subtract(offset)) >= (HintsReader.PAGE_SIZE))
					return endOfData();

				try {
					buffer = computeNextInternal();
				} catch (EOFException e) {
					return endOfData();
				} catch (IOException e) {
					throw new FSReadError(e, file);
				}
			} while (buffer == null );
			return buffer;
		}

		private ByteBuffer computeNextInternal() throws IOException {
			input.resetCrc();
			input.resetLimit();
			int size = input.readInt();
			if (!(input.checkCrc()))
				throw new IOException("Digest mismatch exception");

			return readBuffer(size);
		}

		private ByteBuffer readBuffer(int size) throws IOException {
			if ((rateLimiter) != null)
				rateLimiter.acquire(size);

			input.limit(size);
			ByteBuffer buffer = ByteBufferUtil.read(input, size);
			if (input.checkCrc())
				return buffer;

			return null;
		}
	}
}

