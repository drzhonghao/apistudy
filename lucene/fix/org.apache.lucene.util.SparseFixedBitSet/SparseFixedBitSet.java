

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.RamUsageEstimator;


public class SparseFixedBitSet extends BitSet implements Accountable , Bits {
	private static final long BASE_RAM_BYTES_USED = RamUsageEstimator.shallowSizeOfInstance(SparseFixedBitSet.class);

	private static final long SINGLE_ELEMENT_ARRAY_BYTES_USED = RamUsageEstimator.sizeOf(new long[1]);

	private static final int MASK_4096 = (1 << 12) - 1;

	private static int blockCount(int length) {
		int blockCount = length >>> 12;
		if ((blockCount << 12) < length) {
			++blockCount;
		}
		assert (blockCount << 12) >= length;
		return blockCount;
	}

	final long[] indices;

	final long[][] bits;

	final int length;

	int nonZeroLongCount;

	long ramBytesUsed;

	public SparseFixedBitSet(int length) {
		if (length < 1) {
			throw new IllegalArgumentException("length needs to be >= 1");
		}
		this.length = length;
		final int blockCount = SparseFixedBitSet.blockCount(length);
		indices = new long[blockCount];
		bits = new long[blockCount][];
		ramBytesUsed = ((SparseFixedBitSet.BASE_RAM_BYTES_USED) + (RamUsageEstimator.shallowSizeOf(indices))) + (RamUsageEstimator.shallowSizeOf(bits));
	}

	@Override
	public int length() {
		return length;
	}

	private boolean consistent(int index) {
		assert (index >= 0) && (index < (length)) : (("index=" + index) + ",length=") + (length);
		return true;
	}

	@Override
	public int cardinality() {
		int cardinality = 0;
		for (long[] bitArray : bits) {
			if (bitArray != null) {
				for (long bits : bitArray) {
					cardinality += Long.bitCount(bits);
				}
			}
		}
		return cardinality;
	}

	@Override
	public int approximateCardinality() {
		final int totalLongs = ((length) + 63) >>> 6;
		assert totalLongs >= (nonZeroLongCount);
		final int zeroLongs = totalLongs - (nonZeroLongCount);
		final long estimate = Math.round((totalLongs * (Math.log((((double) (totalLongs)) / zeroLongs)))));
		return ((int) (Math.min(length, estimate)));
	}

	@Override
	public boolean get(int i) {
		assert consistent(i);
		final int i4096 = i >>> 12;
		final long index = indices[i4096];
		final int i64 = i >>> 6;
		if ((index & (1L << i64)) == 0) {
			return false;
		}
		final long bits = this.bits[i4096][Long.bitCount((index & ((1L << i64) - 1)))];
		return (bits & (1L << i)) != 0;
	}

	private static int oversize(int s) {
		int newSize = s + (s >>> 1);
		if (newSize > 50) {
			newSize = 64;
		}
		return newSize;
	}

	public void set(int i) {
		assert consistent(i);
		final int i4096 = i >>> 12;
		final long index = indices[i4096];
		final int i64 = i >>> 6;
		if ((index & (1L << i64)) != 0) {
			bits[i4096][Long.bitCount((index & ((1L << i64) - 1)))] |= 1L << i;
		}else
			if (index == 0) {
				insertBlock(i4096, i64, i);
			}else {
				insertLong(i4096, i64, i, index);
			}

	}

	private void insertBlock(int i4096, int i64, int i) {
		indices[i4096] = 1L << i64;
		assert (bits[i4096]) == null;
		bits[i4096] = new long[]{ 1L << i };
		++(nonZeroLongCount);
		ramBytesUsed += SparseFixedBitSet.SINGLE_ELEMENT_ARRAY_BYTES_USED;
	}

	private void insertLong(int i4096, int i64, int i, long index) {
		indices[i4096] |= 1L << i64;
		final int o = Long.bitCount((index & ((1L << i64) - 1)));
		final long[] bitArray = bits[i4096];
		if ((bitArray[((bitArray.length) - 1)]) == 0) {
			System.arraycopy(bitArray, o, bitArray, (o + 1), (((bitArray.length) - o) - 1));
			bitArray[o] = 1L << i;
		}else {
			final int newSize = SparseFixedBitSet.oversize(((bitArray.length) + 1));
			final long[] newBitArray = new long[newSize];
			System.arraycopy(bitArray, 0, newBitArray, 0, o);
			newBitArray[o] = 1L << i;
			System.arraycopy(bitArray, o, newBitArray, (o + 1), ((bitArray.length) - o));
			bits[i4096] = newBitArray;
			ramBytesUsed += (RamUsageEstimator.sizeOf(newBitArray)) - (RamUsageEstimator.sizeOf(bitArray));
		}
		++(nonZeroLongCount);
	}

	public void clear(int i) {
		assert consistent(i);
		final int i4096 = i >>> 12;
		final int i64 = i >>> 6;
		and(i4096, i64, (~(1L << i)));
	}

	private void and(int i4096, int i64, long mask) {
		final long index = indices[i4096];
		if ((index & (1L << i64)) != 0) {
			final int o = Long.bitCount((index & ((1L << i64) - 1)));
			long bits = (this.bits[i4096][o]) & mask;
			if (bits == 0) {
				removeLong(i4096, i64, index, o);
			}else {
				this.bits[i4096][o] = bits;
			}
		}
	}

	private void removeLong(int i4096, int i64, long index, int o) {
		index &= ~(1L << i64);
		indices[i4096] = index;
		if (index == 0) {
			this.bits[i4096] = null;
		}else {
			final int length = Long.bitCount(index);
			final long[] bitArray = bits[i4096];
			System.arraycopy(bitArray, (o + 1), bitArray, o, (length - o));
			bitArray[length] = 0L;
		}
		nonZeroLongCount -= 1;
	}

	@Override
	public void clear(int from, int to) {
		assert from >= 0;
		assert to <= (length);
		if (from >= to) {
			return;
		}
		final int firstBlock = from >>> 12;
		final int lastBlock = (to - 1) >>> 12;
		if (firstBlock == lastBlock) {
			clearWithinBlock(firstBlock, (from & (SparseFixedBitSet.MASK_4096)), ((to - 1) & (SparseFixedBitSet.MASK_4096)));
		}else {
			clearWithinBlock(firstBlock, (from & (SparseFixedBitSet.MASK_4096)), SparseFixedBitSet.MASK_4096);
			for (int i = firstBlock + 1; i < lastBlock; ++i) {
				nonZeroLongCount -= Long.bitCount(indices[i]);
				indices[i] = 0;
				bits[i] = null;
			}
			clearWithinBlock(lastBlock, 0, ((to - 1) & (SparseFixedBitSet.MASK_4096)));
		}
	}

	private static long mask(int from, int to) {
		return (((1L << (to - from)) << 1) - 1) << from;
	}

	private void clearWithinBlock(int i4096, int from, int to) {
		int firstLong = from >>> 6;
		int lastLong = to >>> 6;
		if (firstLong == lastLong) {
			and(i4096, firstLong, (~(SparseFixedBitSet.mask(from, to))));
		}else {
			assert firstLong < lastLong;
			and(i4096, lastLong, (~(SparseFixedBitSet.mask(0, to))));
			for (int i = lastLong - 1; i >= (firstLong + 1); --i) {
				and(i4096, i, 0L);
			}
			and(i4096, firstLong, (~(SparseFixedBitSet.mask(from, 63))));
		}
	}

	private int firstDoc(int i4096) {
		long index = 0;
		while (i4096 < (indices.length)) {
			index = indices[i4096];
			if (index != 0) {
				final int i64 = Long.numberOfTrailingZeros(index);
				return ((i4096 << 12) | (i64 << 6)) | (Long.numberOfTrailingZeros(bits[i4096][0]));
			}
			i4096 += 1;
		} 
		return DocIdSetIterator.NO_MORE_DOCS;
	}

	@Override
	public int nextSetBit(int i) {
		assert i < (length);
		final int i4096 = i >>> 12;
		final long index = indices[i4096];
		final long[] bitArray = this.bits[i4096];
		int i64 = i >>> 6;
		int o = Long.bitCount((index & ((1L << i64) - 1)));
		if ((index & (1L << i64)) != 0) {
			final long bits = (bitArray[o]) >>> i;
			if (bits != 0) {
				return i + (Long.numberOfTrailingZeros(bits));
			}
			o += 1;
		}
		final long indexBits = (index >>> i64) >>> 1;
		if (indexBits == 0) {
			return firstDoc((i4096 + 1));
		}
		i64 += 1 + (Long.numberOfTrailingZeros(indexBits));
		final long bits = bitArray[o];
		return (i64 << 6) | (Long.numberOfTrailingZeros(bits));
	}

	private int lastDoc(int i4096) {
		long index;
		while (i4096 >= 0) {
			index = indices[i4096];
			if (index != 0) {
				final int i64 = 63 - (Long.numberOfLeadingZeros(index));
				final long bits = this.bits[i4096][((Long.bitCount(index)) - 1)];
				return ((i4096 << 12) | (i64 << 6)) | (63 - (Long.numberOfLeadingZeros(bits)));
			}
			i4096 -= 1;
		} 
		return -1;
	}

	@Override
	public int prevSetBit(int i) {
		assert i >= 0;
		final int i4096 = i >>> 12;
		final long index = indices[i4096];
		final long[] bitArray = this.bits[i4096];
		int i64 = i >>> 6;
		final long indexBits = index & ((1L << i64) - 1);
		final int o = Long.bitCount(indexBits);
		if ((index & (1L << i64)) != 0) {
			final long bits = (bitArray[o]) & (((1L << i) << 1) - 1);
			if (bits != 0) {
				return (i64 << 6) | (63 - (Long.numberOfLeadingZeros(bits)));
			}
		}
		if (indexBits == 0) {
			return lastDoc((i4096 - 1));
		}
		i64 = 63 - (Long.numberOfLeadingZeros(indexBits));
		final long bits = bitArray[(o - 1)];
		return ((i4096 << 12) | (i64 << 6)) | (63 - (Long.numberOfLeadingZeros(bits)));
	}

	private long longBits(long index, long[] bits, int i64) {
		if ((index & (1L << i64)) == 0) {
			return 0L;
		}else {
			return bits[Long.bitCount((index & ((1L << i64) - 1)))];
		}
	}

	private void or(final int i4096, final long index, long[] bits, int nonZeroLongCount) {
		assert (Long.bitCount(index)) == nonZeroLongCount;
		final long currentIndex = indices[i4096];
		if (currentIndex == 0) {
			indices[i4096] = index;
			this.bits[i4096] = Arrays.copyOf(bits, nonZeroLongCount);
			this.nonZeroLongCount += nonZeroLongCount;
			return;
		}
		final long[] currentBits = this.bits[i4096];
		final long[] newBits;
		final long newIndex = currentIndex | index;
		final int requiredCapacity = Long.bitCount(newIndex);
		if ((currentBits.length) >= requiredCapacity) {
			newBits = currentBits;
		}else {
			newBits = new long[SparseFixedBitSet.oversize(requiredCapacity)];
		}
		for (int i = Long.numberOfLeadingZeros(newIndex), newO = (Long.bitCount(newIndex)) - 1; i < 64; i += 1 + (Long.numberOfLeadingZeros((newIndex << (i + 1)))) , newO -= 1) {
			final int bitIndex = 63 - i;
			assert newO == (Long.bitCount((newIndex & ((1L << bitIndex) - 1))));
			newBits[newO] = (longBits(currentIndex, currentBits, bitIndex)) | (longBits(index, bits, bitIndex));
		}
		indices[i4096] = newIndex;
		this.bits[i4096] = newBits;
		this.nonZeroLongCount += nonZeroLongCount - (Long.bitCount((currentIndex & index)));
	}

	private void or(SparseFixedBitSet other) {
		for (int i = 0; i < (other.indices.length); ++i) {
			final long index = other.indices[i];
			if (index != 0) {
				or(i, index, other.bits[i], Long.bitCount(index));
			}
		}
	}

	private void orDense(DocIdSetIterator it) throws IOException {
		checkUnpositioned(it);
		final int firstDoc = it.nextDoc();
		if (firstDoc == (DocIdSetIterator.NO_MORE_DOCS)) {
			return;
		}
		int i4096 = firstDoc >>> 12;
		int i64 = firstDoc >>> 6;
		long index = 1L << i64;
		long currentLong = 1L << firstDoc;
		long[] longs = new long[64];
		int numLongs = 0;
		for (int doc = it.nextDoc(); doc != (DocIdSetIterator.NO_MORE_DOCS); doc = it.nextDoc()) {
			final int doc64 = doc >>> 6;
			if (doc64 == i64) {
				currentLong |= 1L << doc;
			}else {
				longs[(numLongs++)] = currentLong;
				final int doc4096 = doc >>> 12;
				if (doc4096 == i4096) {
					index |= 1L << doc64;
				}else {
					or(i4096, index, longs, numLongs);
					i4096 = doc4096;
					index = 1L << doc64;
					numLongs = 0;
				}
				i64 = doc64;
				currentLong = 1L << doc;
			}
		}
		longs[(numLongs++)] = currentLong;
		or(i4096, index, longs, numLongs);
	}

	@Override
	public void or(DocIdSetIterator it) throws IOException {
		{
		}
		if ((it.cost()) < (indices.length)) {
			super.or(it);
		}else {
			orDense(it);
		}
	}

	@Override
	public long ramBytesUsed() {
		return ramBytesUsed;
	}

	@Override
	public String toString() {
		return (("SparseFixedBitSet(size=" + (length)) + ",cardinality=~") + (approximateCardinality());
	}
}

