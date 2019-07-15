

import java.io.IOException;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexInput;


final class DirectPacked64SingleBlockReader {
	private final IndexInput in;

	private final int bitsPerValue;

	private final long startPointer;

	private final int valuesPerBlock;

	private final long mask;

	DirectPacked64SingleBlockReader(int bitsPerValue, int valueCount, IndexInput in) {
		this.in = in;
		this.bitsPerValue = bitsPerValue;
		startPointer = in.getFilePointer();
		valuesPerBlock = 64 / bitsPerValue;
		mask = ~((~0L) << bitsPerValue);
	}

	public long get(int index) {
		final int blockOffset = index / (valuesPerBlock);
		final long skip = ((long) (blockOffset)) << 3;
		try {
			in.seek(((startPointer) + skip));
			long block = in.readLong();
			final int offsetInBlock = index % (valuesPerBlock);
			return (block >>> (offsetInBlock * (bitsPerValue))) & (mask);
		} catch (IOException e) {
			throw new IllegalStateException("failed", e);
		}
	}

	public long ramBytesUsed() {
		return 0;
	}
}

