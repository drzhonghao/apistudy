

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.packed.PackedInts;

import static org.apache.lucene.util.packed.PackedInts.Format.PACKED;


final class Direct32 {
	final int[] values;

	Direct32(int valueCount) {
		values = new int[valueCount];
	}

	Direct32(int packedIntsVersion, DataInput in, int valueCount) throws IOException {
		this(valueCount);
		for (int i = 0; i < valueCount; ++i) {
			values[i] = in.readInt();
		}
		final int remaining = ((int) ((PACKED.byteCount(packedIntsVersion, valueCount, 32)) - (4L * valueCount)));
		for (int i = 0; i < remaining; ++i) {
			in.readByte();
		}
	}

	public long get(final int index) {
		return (values[index]) & 4294967295L;
	}

	public void set(final int index, final long value) {
		values[index] = ((int) (value));
	}

	public long ramBytesUsed() {
		return (RamUsageEstimator.alignObjectSize((((RamUsageEstimator.NUM_BYTES_OBJECT_HEADER) + (2 * (RamUsageEstimator.NUM_BYTES_INT))) + (RamUsageEstimator.NUM_BYTES_OBJECT_REF)))) + (RamUsageEstimator.sizeOf(values));
	}

	public void clear() {
		Arrays.fill(values, ((int) (0L)));
	}

	public int get(int index, long[] arr, int off, int len) {
		assert len > 0 : ("len must be > 0 (got " + len) + ")";
		assert (off + len) <= (arr.length);
		return 0;
	}

	public int set(int index, long[] arr, int off, int len) {
		assert len > 0 : ("len must be > 0 (got " + len) + ")";
		assert (off + len) <= (arr.length);
		return 0;
	}

	public void fill(int fromIndex, int toIndex, long val) {
		assert val == (val & 4294967295L);
		Arrays.fill(values, fromIndex, toIndex, ((int) (val)));
	}
}

