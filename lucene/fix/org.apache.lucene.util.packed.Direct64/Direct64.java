

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.util.RamUsageEstimator;


final class Direct64 {
	final long[] values;

	Direct64(int valueCount) {
		values = new long[valueCount];
	}

	Direct64(int packedIntsVersion, DataInput in, int valueCount) throws IOException {
		this(valueCount);
		for (int i = 0; i < valueCount; ++i) {
			values[i] = in.readLong();
		}
	}

	public long get(final int index) {
		return values[index];
	}

	public void set(final int index, final long value) {
		values[index] = value;
	}

	public long ramBytesUsed() {
		return (RamUsageEstimator.alignObjectSize((((RamUsageEstimator.NUM_BYTES_OBJECT_HEADER) + (2 * (RamUsageEstimator.NUM_BYTES_INT))) + (RamUsageEstimator.NUM_BYTES_OBJECT_REF)))) + (RamUsageEstimator.sizeOf(values));
	}

	public void clear() {
		Arrays.fill(values, 0L);
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
		Arrays.fill(values, fromIndex, toIndex, val);
	}
}

