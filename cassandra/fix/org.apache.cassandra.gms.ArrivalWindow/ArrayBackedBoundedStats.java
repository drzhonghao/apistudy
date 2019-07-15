

class ArrayBackedBoundedStats {
	private final long[] arrivalIntervals;

	private long sum = 0;

	private int index = 0;

	private boolean isFilled = false;

	private volatile double mean = 0;

	public ArrayBackedBoundedStats(final int size) {
		arrivalIntervals = new long[size];
	}

	public void add(long interval) {
		if ((index) == (arrivalIntervals.length)) {
			isFilled = true;
			index = 0;
		}
		if (isFilled)
			sum = (sum) - (arrivalIntervals[index]);

		arrivalIntervals[((index)++)] = interval;
		sum += interval;
		mean = ((double) (sum)) / (size());
	}

	private int size() {
		return isFilled ? arrivalIntervals.length : index;
	}

	public double mean() {
		return mean;
	}

	public long[] getArrivalIntervals() {
		return arrivalIntervals;
	}
}

