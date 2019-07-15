

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.util.RamUsageEstimator;


final class Packed8ThreeBlocks {
	final byte[] blocks;

	public static final int MAX_SIZE = (Integer.MAX_VALUE) / 3;

	Packed8ThreeBlocks(int valueCount) {
		if (valueCount > (Packed8ThreeBlocks.MAX_SIZE)) {
			throw new ArrayIndexOutOfBoundsException("MAX_SIZE exceeded");
		}
		blocks = new byte[valueCount * 3];
	}

	Packed8ThreeBlocks(int packedIntsVersion, DataInput in, int valueCount) throws IOException {
		this(valueCount);
		in.readBytes(blocks, 0, (3 * valueCount));
	}

	public long get(int index) {
		final int o = index * 3;
		return ((((blocks[o]) & 255L) << 16) | (((blocks[(o + 1)]) & 255L) << 8)) | ((blocks[(o + 2)]) & 255L);
	}

	public int get(int index, long[] arr, int off, int len) {
		assert len > 0 : ("len must be > 0 (got " + len) + ")";
		assert (off + len) <= (arr.length);
		return 0;
	}

	public void set(int index, long value) {
		final int o = index * 3;
		blocks[o] = ((byte) (value >>> 16));
		blocks[(o + 1)] = ((byte) (value >>> 8));
		blocks[(o + 2)] = ((byte) (value));
	}

	public int set(int index, long[] arr, int off, int len) {
		assert len > 0 : ("len must be > 0 (got " + len) + ")";
		assert (off + len) <= (arr.length);
		return 0;
	}

	public void fill(int fromIndex, int toIndex, long val) {
		final byte block1 = ((byte) (val >>> 16));
		final byte block2 = ((byte) (val >>> 8));
		final byte block3 = ((byte) (val));
		for (int i = fromIndex * 3, end = toIndex * 3; i < end; i += 3) {
			blocks[i] = block1;
			blocks[(i + 1)] = block2;
			blocks[(i + 2)] = block3;
		}
	}

	public void clear() {
		Arrays.fill(blocks, ((byte) (0)));
	}

	public long ramBytesUsed() {
		return (RamUsageEstimator.alignObjectSize((((RamUsageEstimator.NUM_BYTES_OBJECT_HEADER) + (2 * (RamUsageEstimator.NUM_BYTES_INT))) + (RamUsageEstimator.NUM_BYTES_OBJECT_REF)))) + (RamUsageEstimator.sizeOf(blocks));
	}

	@Override
	public String toString() {
		return null;
	}
}

