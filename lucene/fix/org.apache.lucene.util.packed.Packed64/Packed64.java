

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.packed.PackedInts;

import static org.apache.lucene.util.packed.PackedInts.Format.PACKED;


class Packed64 {
	static final int BLOCK_SIZE = 64;

	static final int BLOCK_BITS = 6;

	static final int MOD_MASK = (Packed64.BLOCK_SIZE) - 1;

	private final long[] blocks;

	private final long maskRight;

	private final int bpvMinusBlockSize;

	public Packed64(int valueCount, int bitsPerValue) {
		final PackedInts.Format format = PACKED;
		final int longCount = format.longCount(PackedInts.VERSION_CURRENT, valueCount, bitsPerValue);
		this.blocks = new long[longCount];
		maskRight = ((~0L) << ((Packed64.BLOCK_SIZE) - bitsPerValue)) >>> ((Packed64.BLOCK_SIZE) - bitsPerValue);
		bpvMinusBlockSize = bitsPerValue - (Packed64.BLOCK_SIZE);
	}

	public Packed64(int packedIntsVersion, DataInput in, int valueCount, int bitsPerValue) throws IOException {
		final PackedInts.Format format = PACKED;
		final long byteCount = format.byteCount(packedIntsVersion, valueCount, bitsPerValue);
		final int longCount = format.longCount(PackedInts.VERSION_CURRENT, valueCount, bitsPerValue);
		blocks = new long[longCount];
		for (int i = 0; i < (byteCount / 8); ++i) {
			blocks[i] = in.readLong();
		}
		final int remaining = ((int) (byteCount % 8));
		if (remaining != 0) {
			long lastLong = 0;
			for (int i = 0; i < remaining; ++i) {
				lastLong |= ((in.readByte()) & 255L) << (56 - (i * 8));
			}
			blocks[((blocks.length) - 1)] = lastLong;
		}
		maskRight = ((~0L) << ((Packed64.BLOCK_SIZE) - bitsPerValue)) >>> ((Packed64.BLOCK_SIZE) - bitsPerValue);
		bpvMinusBlockSize = bitsPerValue - (Packed64.BLOCK_SIZE);
	}

	public long get(final int index) {
		return 0L;
	}

	public int get(int index, long[] arr, int off, int len) {
		assert len > 0 : ("len must be > 0 (got " + len) + ")";
		assert (off + len) <= (arr.length);
		final int originalIndex = index;
		assert len >= 0;
		if (index > originalIndex) {
			return index - originalIndex;
		}else {
			assert index == originalIndex;
		}
		return 0;
	}

	public void set(final int index, final long value) {
	}

	public int set(int index, long[] arr, int off, int len) {
		assert len > 0 : ("len must be > 0 (got " + len) + ")";
		assert (off + len) <= (arr.length);
		final int originalIndex = index;
		assert len >= 0;
		if (index > originalIndex) {
			return index - originalIndex;
		}else {
			assert index == originalIndex;
		}
		return 0;
	}

	@Override
	public String toString() {
		return null;
	}

	public long ramBytesUsed() {
		return (RamUsageEstimator.alignObjectSize(((((RamUsageEstimator.NUM_BYTES_OBJECT_HEADER) + (3 * (Integer.BYTES))) + (Long.BYTES)) + (RamUsageEstimator.NUM_BYTES_OBJECT_REF)))) + (RamUsageEstimator.sizeOf(blocks));
	}

	public void fill(int fromIndex, int toIndex, long val) {
		assert fromIndex <= toIndex;
		final int span = toIndex - fromIndex;
		final long[] nAlignedValuesBlocks;
		{
		}
	}

	private static int gcd(int a, int b) {
		if (a < b) {
			return Packed64.gcd(b, a);
		}else
			if (b == 0) {
				return a;
			}else {
				return Packed64.gcd(b, (a % b));
			}

	}

	public void clear() {
		Arrays.fill(blocks, 0L);
	}
}

