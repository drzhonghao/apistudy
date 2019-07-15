

import java.util.Arrays;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IntroSorter;
import org.apache.lucene.util.Sorter;


public abstract class MSBRadixSorter extends Sorter {
	private static final int LEVEL_THRESHOLD = 8;

	private static final int HISTOGRAM_SIZE = 257;

	private static final int LENGTH_THRESHOLD = 100;

	private final int[][] histograms = new int[MSBRadixSorter.LEVEL_THRESHOLD][];

	private final int[] endOffsets = new int[MSBRadixSorter.HISTOGRAM_SIZE];

	private final int[] commonPrefix;

	private final int maxLength;

	protected MSBRadixSorter(int maxLength) {
		this.maxLength = maxLength;
		this.commonPrefix = new int[Math.min(24, maxLength)];
	}

	protected abstract int byteAt(int i, int k);

	protected Sorter getFallbackSorter(int k) {
		return new IntroSorter() {
			@Override
			protected void swap(int i, int j) {
				MSBRadixSorter.this.swap(i, j);
			}

			@Override
			protected int compare(int i, int j) {
				for (int o = k; o < (maxLength); ++o) {
					final int b1 = byteAt(i, o);
					final int b2 = byteAt(j, o);
					if (b1 != b2) {
						return b1 - b2;
					}else
						if (b1 == (-1)) {
							break;
						}

				}
				return 0;
			}

			@Override
			protected void setPivot(int i) {
				pivot.setLength(0);
				for (int o = k; o < (maxLength); ++o) {
					final int b = byteAt(i, o);
					if (b == (-1)) {
						break;
					}
					pivot.append(((byte) (b)));
				}
			}

			@Override
			protected int comparePivot(int j) {
				for (int o = 0; o < (pivot.length()); ++o) {
					final int b1 = (pivot.byteAt(o)) & 255;
					final int b2 = byteAt(j, (k + o));
					if (b1 != b2) {
						return b1 - b2;
					}
				}
				if ((k + (pivot.length())) == (maxLength)) {
					return 0;
				}
				return (-1) - (byteAt(j, (k + (pivot.length()))));
			}

			private final BytesRefBuilder pivot = new BytesRefBuilder();
		};
	}

	@Override
	protected final int compare(int i, int j) {
		throw new UnsupportedOperationException("unused: not a comparison-based sort");
	}

	@Override
	public void sort(int from, int to) {
		sort(from, to, 0, 0);
	}

	private void sort(int from, int to, int k, int l) {
		if (((to - from) <= (MSBRadixSorter.LENGTH_THRESHOLD)) || (l >= (MSBRadixSorter.LEVEL_THRESHOLD))) {
			introSort(from, to, k);
		}else {
			radixSort(from, to, k, l);
		}
	}

	private void introSort(int from, int to, int k) {
		getFallbackSorter(k).sort(from, to);
	}

	private void radixSort(int from, int to, int k, int l) {
		int[] histogram = histograms[l];
		if (histogram == null) {
			histogram = histograms[l] = new int[MSBRadixSorter.HISTOGRAM_SIZE];
		}else {
			Arrays.fill(histogram, 0);
		}
		final int commonPrefixLength = computeCommonPrefixLengthAndBuildHistogram(from, to, k, histogram);
		if (commonPrefixLength > 0) {
			if (((k + commonPrefixLength) < (maxLength)) && ((histogram[0]) < (to - from))) {
				radixSort(from, to, (k + commonPrefixLength), l);
			}
			return;
		}
		assert assertHistogram(commonPrefixLength, histogram);
		int[] startOffsets = histogram;
		int[] endOffsets = this.endOffsets;
		MSBRadixSorter.sumHistogram(histogram, endOffsets);
		reorder(from, to, startOffsets, endOffsets, k);
		endOffsets = startOffsets;
		if ((k + 1) < (maxLength)) {
			for (int prev = endOffsets[0], i = 1; i < (MSBRadixSorter.HISTOGRAM_SIZE); ++i) {
				int h = endOffsets[i];
				final int bucketLen = h - prev;
				if (bucketLen > 1) {
					sort((from + prev), (from + h), (k + 1), (l + 1));
				}
				prev = h;
			}
		}
	}

	private boolean assertHistogram(int commonPrefixLength, int[] histogram) {
		int numberOfUniqueBytes = 0;
		for (int freq : histogram) {
			if (freq > 0) {
				numberOfUniqueBytes++;
			}
		}
		if (numberOfUniqueBytes == 1) {
			assert commonPrefixLength >= 1;
		}else {
			assert commonPrefixLength == 0 : commonPrefixLength;
		}
		return true;
	}

	private int getBucket(int i, int k) {
		return (byteAt(i, k)) + 1;
	}

	private int computeCommonPrefixLengthAndBuildHistogram(int from, int to, int k, int[] histogram) {
		final int[] commonPrefix = this.commonPrefix;
		int commonPrefixLength = Math.min(commonPrefix.length, ((maxLength) - k));
		for (int j = 0; j < commonPrefixLength; ++j) {
			final int b = byteAt(from, (k + j));
			commonPrefix[j] = b;
			if (b == (-1)) {
				commonPrefixLength = j + 1;
				break;
			}
		}
		int i;
		outer : for (i = from + 1; i < to; ++i) {
			for (int j = 0; j < commonPrefixLength; ++j) {
				final int b = byteAt(i, (k + j));
				if (b != (commonPrefix[j])) {
					commonPrefixLength = j;
					if (commonPrefixLength == 0) {
						histogram[((commonPrefix[0]) + 1)] = i - from;
						histogram[(b + 1)] = 1;
						break outer;
					}
					break;
				}
			}
		}
		if (i < to) {
			assert commonPrefixLength == 0;
			buildHistogram((i + 1), to, k, histogram);
		}else {
			assert commonPrefixLength > 0;
			histogram[((commonPrefix[0]) + 1)] = to - from;
		}
		return commonPrefixLength;
	}

	private void buildHistogram(int from, int to, int k, int[] histogram) {
		for (int i = from; i < to; ++i) {
			(histogram[getBucket(i, k)])++;
		}
	}

	private static void sumHistogram(int[] histogram, int[] endOffsets) {
		int accum = 0;
		for (int i = 0; i < (MSBRadixSorter.HISTOGRAM_SIZE); ++i) {
			final int count = histogram[i];
			histogram[i] = accum;
			accum += count;
			endOffsets[i] = accum;
		}
	}

	private void reorder(int from, int to, int[] startOffsets, int[] endOffsets, int k) {
		for (int i = 0; i < (MSBRadixSorter.HISTOGRAM_SIZE); ++i) {
			final int limit = endOffsets[i];
			for (int h1 = startOffsets[i]; h1 < limit; h1 = startOffsets[i]) {
				final int b = getBucket((from + h1), k);
				final int h2 = (startOffsets[b])++;
				swap((from + h1), (from + h2));
			}
		}
	}
}

