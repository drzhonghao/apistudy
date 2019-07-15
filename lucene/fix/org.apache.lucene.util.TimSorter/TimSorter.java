

import java.util.Arrays;
import org.apache.lucene.util.Sorter;


public abstract class TimSorter extends Sorter {
	static final int MINRUN = 32;

	static final int THRESHOLD = 64;

	static final int STACKSIZE = 49;

	static final int MIN_GALLOP = 7;

	final int maxTempSlots;

	int minRun;

	int to;

	int stackSize;

	int[] runEnds;

	protected TimSorter(int maxTempSlots) {
		super();
		runEnds = new int[1 + (TimSorter.STACKSIZE)];
		this.maxTempSlots = maxTempSlots;
	}

	static int minRun(int length) {
		assert length >= (TimSorter.MINRUN);
		int n = length;
		int r = 0;
		while (n >= 64) {
			r |= n & 1;
			n >>>= 1;
		} 
		final int minRun = n + r;
		assert (minRun >= (TimSorter.MINRUN)) && (minRun <= (TimSorter.THRESHOLD));
		return minRun;
	}

	int runLen(int i) {
		final int off = (stackSize) - i;
		return (runEnds[off]) - (runEnds[(off - 1)]);
	}

	int runBase(int i) {
		return runEnds[(((stackSize) - i) - 1)];
	}

	int runEnd(int i) {
		return runEnds[((stackSize) - i)];
	}

	void setRunEnd(int i, int runEnd) {
		runEnds[((stackSize) - i)] = runEnd;
	}

	void pushRunLen(int len) {
		runEnds[((stackSize) + 1)] = (runEnds[stackSize]) + len;
		++(stackSize);
	}

	int nextRun() {
		final int runBase = runEnd(0);
		assert runBase < (to);
		if (runBase == ((to) - 1)) {
			return 1;
		}
		int o = runBase + 2;
		if ((compare(runBase, (runBase + 1))) > 0) {
			while ((o < (to)) && ((compare((o - 1), o)) > 0)) {
				++o;
			} 
		}else {
			while ((o < (to)) && ((compare((o - 1), o)) <= 0)) {
				++o;
			} 
		}
		final int runHi = Math.max(o, Math.min(to, (runBase + (minRun))));
		return runHi - runBase;
	}

	void ensureInvariants() {
		while ((stackSize) > 1) {
			final int runLen0 = runLen(0);
			final int runLen1 = runLen(1);
			if ((stackSize) > 2) {
				final int runLen2 = runLen(2);
				if (runLen2 <= (runLen1 + runLen0)) {
					if (runLen2 < runLen0) {
						mergeAt(1);
					}else {
						mergeAt(0);
					}
					continue;
				}
			}
			if (runLen1 <= runLen0) {
				mergeAt(0);
				continue;
			}
			break;
		} 
	}

	void exhaustStack() {
		while ((stackSize) > 1) {
			mergeAt(0);
		} 
	}

	void reset(int from, int to) {
		stackSize = 0;
		Arrays.fill(runEnds, 0);
		runEnds[0] = from;
		this.to = to;
		final int length = to - from;
		this.minRun = (length <= (TimSorter.THRESHOLD)) ? length : TimSorter.minRun(length);
	}

	void mergeAt(int n) {
		assert (stackSize) >= 2;
		merge(runBase((n + 1)), runBase(n), runEnd(n));
		for (int j = n + 1; j > 0; --j) {
			setRunEnd(j, runEnd((j - 1)));
		}
		--(stackSize);
	}

	void merge(int lo, int mid, int hi) {
		if ((compare((mid - 1), mid)) <= 0) {
			return;
		}
		if (((hi - mid) <= (mid - lo)) && ((hi - mid) <= (maxTempSlots))) {
			mergeHi(lo, mid, hi);
		}else
			if ((mid - lo) <= (maxTempSlots)) {
				mergeLo(lo, mid, hi);
			}else {
			}

	}

	@Override
	public void sort(int from, int to) {
		if ((to - from) <= 1) {
			return;
		}
		reset(from, to);
		do {
			ensureInvariants();
			pushRunLen(nextRun());
		} while ((runEnd(0)) < to );
		exhaustStack();
		assert (runEnd(0)) == to;
	}

	void doRotate(int lo, int mid, int hi) {
		final int len1 = mid - lo;
		final int len2 = hi - mid;
		if (len1 == len2) {
			while (mid < hi) {
				swap((lo++), (mid++));
			} 
		}else
			if ((len2 < len1) && (len2 <= (maxTempSlots))) {
				save(mid, len2);
				for (int i = (lo + len1) - 1, j = hi - 1; i >= lo; --i , --j) {
					copy(i, j);
				}
				for (int i = 0, j = lo; i < len2; ++i , ++j) {
					restore(i, j);
				}
			}else
				if (len1 <= (maxTempSlots)) {
					save(lo, len1);
					for (int i = mid, j = lo; i < hi; ++i , ++j) {
						copy(i, j);
					}
					for (int i = 0, j = lo + len2; j < hi; ++i , ++j) {
						restore(i, j);
					}
				}else {
				}


	}

	void mergeLo(int lo, int mid, int hi) {
		assert (compare(lo, mid)) > 0;
		int len1 = mid - lo;
		save(lo, len1);
		copy(mid, lo);
		int i = 0;
		int j = mid + 1;
		int dest = lo + 1;
		outer : for (; ;) {
			for (int count = 0; count < (TimSorter.MIN_GALLOP);) {
				if ((i >= len1) || (j >= hi)) {
					break outer;
				}else
					if ((compareSaved(i, j)) <= 0) {
						restore((i++), (dest++));
						count = 0;
					}else {
						copy((j++), (dest++));
						++count;
					}

			}
			int next = lowerSaved3(j, hi, i);
			for (; j < next; ++dest) {
				copy((j++), dest);
			}
			restore((i++), (dest++));
		}
		for (; i < len1; ++dest) {
			restore((i++), dest);
		}
		assert j == dest;
	}

	void mergeHi(int lo, int mid, int hi) {
		assert (compare((mid - 1), (hi - 1))) > 0;
		int len2 = hi - mid;
		save(mid, len2);
		copy((mid - 1), (hi - 1));
		int i = mid - 2;
		int j = len2 - 1;
		int dest = hi - 2;
		outer : for (; ;) {
			for (int count = 0; count < (TimSorter.MIN_GALLOP);) {
				if ((i < lo) || (j < 0)) {
					break outer;
				}else
					if ((compareSaved(j, i)) >= 0) {
						restore((j--), (dest--));
						count = 0;
					}else {
						copy((i--), (dest--));
						++count;
					}

			}
			int next = upperSaved3(lo, (i + 1), j);
			while (i >= next) {
				copy((i--), (dest--));
			} 
			restore((j--), (dest--));
		}
		for (; j >= 0; --dest) {
			restore((j--), dest);
		}
		assert i == dest;
	}

	int lowerSaved(int from, int to, int val) {
		int len = to - from;
		while (len > 0) {
			final int half = len >>> 1;
			final int mid = from + half;
			if ((compareSaved(val, mid)) > 0) {
				from = mid + 1;
				len = (len - half) - 1;
			}else {
				len = half;
			}
		} 
		return from;
	}

	int upperSaved(int from, int to, int val) {
		int len = to - from;
		while (len > 0) {
			final int half = len >>> 1;
			final int mid = from + half;
			if ((compareSaved(val, mid)) < 0) {
				len = half;
			}else {
				from = mid + 1;
				len = (len - half) - 1;
			}
		} 
		return from;
	}

	int lowerSaved3(int from, int to, int val) {
		int f = from;
		int t = f + 1;
		while (t < to) {
			if ((compareSaved(val, t)) <= 0) {
				return lowerSaved(f, t, val);
			}
			int delta = t - f;
			f = t;
			t += delta << 1;
		} 
		return lowerSaved(f, to, val);
	}

	int upperSaved3(int from, int to, int val) {
		int f = to - 1;
		int t = to;
		while (f > from) {
			if ((compareSaved(val, f)) >= 0) {
				return upperSaved(f, t, val);
			}
			final int delta = t - f;
			t = f;
			f -= delta << 1;
		} 
		return upperSaved(from, t, val);
	}

	protected abstract void copy(int src, int dest);

	protected abstract void save(int i, int len);

	protected abstract void restore(int i, int j);

	protected abstract int compareSaved(int i, int j);
}

