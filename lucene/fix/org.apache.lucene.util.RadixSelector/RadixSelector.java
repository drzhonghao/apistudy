

import java.util.Arrays;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.IntroSelector;
import org.apache.lucene.util.Selector;


public abstract class RadixSelector extends Selector {
	private static final int LEVEL_THRESHOLD = 8;

	private static final int HISTOGRAM_SIZE = 257;

	private static final int LENGTH_THRESHOLD = 100;

	private final int[] histogram = new int[RadixSelector.HISTOGRAM_SIZE];

	private final int[] commonPrefix;

	private final int maxLength;

	protected RadixSelector(int maxLength) {
		this.maxLength = maxLength;
		this.commonPrefix = new int[Math.min(24, maxLength)];
	}

	protected abstract int byteAt(int i, int k);

	protected Selector getFallbackSelector(int d) {
		return new IntroSelector() {
			@Override
			protected void swap(int i, int j) {
				RadixSelector.this.swap(i, j);
			}

			@Override
			protected int compare(int i, int j) {
				for (int o = d; o < (maxLength); ++o) {
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
				for (int o = d; o < (maxLength); ++o) {
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
					final int b2 = byteAt(j, (d + o));
					if (b1 != b2) {
						return b1 - b2;
					}
				}
				if ((d + (pivot.length())) == (maxLength)) {
					return 0;
				}
				return (-1) - (byteAt(j, (d + (pivot.length()))));
			}

			private final BytesRefBuilder pivot = new BytesRefBuilder();
		};
	}

	@Override
	public void select(int from, int to, int k) {
		select(from, to, k, 0, 0);
	}

	private void select(int from, int to, int k, int d, int l) {
		if (((to - from) <= (RadixSelector.LENGTH_THRESHOLD)) || (d >= (RadixSelector.LEVEL_THRESHOLD))) {
			getFallbackSelector(d).select(from, to, k);
		}else {
			radixSelect(from, to, k, d, l);
		}
	}

	private void radixSelect(int from, int to, int k, int d, int l) {
		final int[] histogram = this.histogram;
		Arrays.fill(histogram, 0);
		final int commonPrefixLength = computeCommonPrefixLengthAndBuildHistogram(from, to, d, histogram);
		if (commonPrefixLength > 0) {
			if (((d + commonPrefixLength) < (maxLength)) && ((histogram[0]) < (to - from))) {
				radixSelect(from, to, k, (d + commonPrefixLength), l);
			}
			return;
		}
		assert assertHistogram(commonPrefixLength, histogram);
		int bucketFrom = from;
		for (int bucket = 0; bucket < (RadixSelector.HISTOGRAM_SIZE); ++bucket) {
			final int bucketTo = bucketFrom + (histogram[bucket]);
			if (bucketTo > k) {
				partition(from, to, bucket, bucketFrom, bucketTo, d);
				if ((bucket != 0) && ((d + 1) < (maxLength))) {
					select(bucketFrom, bucketTo, k, (d + 1), (l + 1));
				}
				return;
			}
			bucketFrom = bucketTo;
		}
		throw new AssertionError("Unreachable code");
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
			assert commonPrefixLength == 0;
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

	private void partition(int from, int to, int bucket, int bucketFrom, int bucketTo, int d) {
		int left = from;
		int right = to - 1;
		int slot = bucketFrom;
		for (; ;) {
			int leftBucket = getBucket(left, d);
			int rightBucket = getBucket(right, d);
			while ((leftBucket <= bucket) && (left < bucketFrom)) {
				if (leftBucket == bucket) {
					swap(left, (slot++));
				}else {
					++left;
				}
				leftBucket = getBucket(left, d);
			} 
			while ((rightBucket >= bucket) && (right >= bucketTo)) {
				if (rightBucket == bucket) {
					swap(right, (slot++));
				}else {
					--right;
				}
				rightBucket = getBucket(right, d);
			} 
			if ((left < bucketFrom) && (right >= bucketTo)) {
				swap((left++), (right--));
			}else {
				assert left == bucketFrom;
				assert right == (bucketTo - 1);
				break;
			}
		}
	}
}

