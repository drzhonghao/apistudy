

import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.packed.PackedInts;


class MonotonicLongValues {
	private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(MonotonicLongValues.class);

	final float[] averages;

	MonotonicLongValues(int pageShift, int pageMask, PackedInts.Reader[] values, long[] mins, float[] averages, long size, long ramBytesUsed) {
		assert (values.length) == (averages.length);
		this.averages = averages;
	}

	long get(int block, int element) {
		return 0L;
	}

	int decodeBlock(int block, long[] dest) {
		final float average = averages[block];
		return 0;
	}

	static class Builder {
		private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(MonotonicLongValues.Builder.class);

		float[] averages;

		Builder(int pageSize, float acceptableOverheadRatio) {
		}

		long baseRamBytesUsed() {
			return MonotonicLongValues.Builder.BASE_RAM_BYTES_USED;
		}

		public MonotonicLongValues build() {
			return null;
		}
	}
}

