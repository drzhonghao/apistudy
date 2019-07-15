

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.util.RamUsageEstimator;


final class Packed16ThreeBlocks {
	final short[] blocks;

	public static final int MAX_SIZE = (Integer.MAX_VALUE) / 3;

	Packed16ThreeBlocks(int valueCount) {
		if (valueCount > (Packed16ThreeBlocks.MAX_SIZE)) {
			throw new ArrayIndexOutOfBoundsException("MAX_SIZE exceeded");
		}
		blocks = new short[valueCount * 3];
	}

	Packed16ThreeBlocks(int packedIntsVersion, DataInput in, int valueCount) throws IOException {
		this(valueCount);
		for (int i = 0; i < (3 * valueCount); ++i) {
			blocks[i] = in.readShort();
		}
	}

	public long get(int index) {
		final int o = index * 3;
		return ((((blocks[o]) & 65535L) << 32) | (((blocks[(o + 1)]) & 65535L) << 16)) | ((blocks[(o + 2)]) & 65535L);
	}

	public int get(int index, long[] arr, int off, int len) {
		assert len > 0 : ("len must be > 0 (got " + len) + ")";
		assert (off + len) <= (arr.length);
		return 0;
	}

	public void set(int index, long value) {
		final int o = index * 3;
		blocks[o] = ((short) (value >>> 32));
		blocks[(o + 1)] = ((short) (value >>> 16));
		blocks[(o + 2)] = ((short) (value));
	}

	public int set(int index, long[] arr, int off, int len) {
		assert len > 0 : ("len must be > 0 (got " + len) + ")";
		assert (off + len) <= (arr.length);
		return 0;
	}

	public void fill(int fromIndex, int toIndex, long val) {
		final short block1 = ((short) (val >>> 32));
		final short block2 = ((short) (val >>> 16));
		final short block3 = ((short) (val));
		for (int i = fromIndex * 3, end = toIndex * 3; i < end; i += 3) {
			blocks[i] = block1;
			blocks[(i + 1)] = block2;
			blocks[(i + 2)] = block3;
		}
	}

	public void clear() {
		Arrays.fill(blocks, ((short) (0)));
	}

	public long ramBytesUsed() {
		return (RamUsageEstimator.alignObjectSize((((RamUsageEstimator.NUM_BYTES_OBJECT_HEADER) + (2 * (RamUsageEstimator.NUM_BYTES_INT))) + (RamUsageEstimator.NUM_BYTES_OBJECT_REF)))) + (RamUsageEstimator.sizeOf(blocks));
	}

	@Override
	public String toString() {
		return null;
	}
}

