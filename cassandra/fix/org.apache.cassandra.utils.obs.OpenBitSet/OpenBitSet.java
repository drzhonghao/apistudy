

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.utils.concurrent.Ref;
import org.apache.cassandra.utils.obs.IBitSet;


public class OpenBitSet implements IBitSet {
	private final long[][] bits;

	private int wlen;

	private final int pageCount;

	private static final int PAGE_SIZE = 4096;

	public OpenBitSet(long numBits) {
		wlen = ((int) (OpenBitSet.bits2words(numBits)));
		int lastPageSize = (wlen) % (OpenBitSet.PAGE_SIZE);
		int fullPageCount = (wlen) / (OpenBitSet.PAGE_SIZE);
		pageCount = fullPageCount + (lastPageSize == 0 ? 0 : 1);
		bits = new long[pageCount][];
		for (int i = 0; i < fullPageCount; ++i)
			bits[i] = new long[OpenBitSet.PAGE_SIZE];

		if (lastPageSize != 0)
			bits[((bits.length) - 1)] = new long[lastPageSize];

	}

	public OpenBitSet() {
		this(64);
	}

	public int getPageSize() {
		return OpenBitSet.PAGE_SIZE;
	}

	public int getPageCount() {
		return pageCount;
	}

	public long[] getPage(int pageIdx) {
		return bits[pageIdx];
	}

	public long capacity() {
		return ((long) (wlen)) << 6;
	}

	@Override
	public long offHeapSize() {
		return 0;
	}

	public void addTo(Ref.IdentityCollection identities) {
	}

	public long size() {
		return capacity();
	}

	public long length() {
		return capacity();
	}

	public boolean isEmpty() {
		return (cardinality()) == 0;
	}

	public int getNumWords() {
		return wlen;
	}

	public boolean get(int index) {
		int i = index >> 6;
		int bit = index & 63;
		long bitmask = 1L << bit;
		return ((bits[(i / (OpenBitSet.PAGE_SIZE))][(i % (OpenBitSet.PAGE_SIZE))]) & bitmask) != 0;
	}

	public boolean get(long index) {
		int i = ((int) (index >> 6));
		int bit = ((int) (index)) & 63;
		long bitmask = 1L << bit;
		return ((bits[(i / (OpenBitSet.PAGE_SIZE))][(i % (OpenBitSet.PAGE_SIZE))]) & bitmask) != 0;
	}

	public void set(long index) {
		int wordNum = ((int) (index >> 6));
		int bit = ((int) (index)) & 63;
		long bitmask = 1L << bit;
		bits[(wordNum / (OpenBitSet.PAGE_SIZE))][(wordNum % (OpenBitSet.PAGE_SIZE))] |= bitmask;
	}

	public void set(int index) {
		int wordNum = index >> 6;
		int bit = index & 63;
		long bitmask = 1L << bit;
		bits[(wordNum / (OpenBitSet.PAGE_SIZE))][(wordNum % (OpenBitSet.PAGE_SIZE))] |= bitmask;
	}

	public void clear(int index) {
		int wordNum = index >> 6;
		int bit = index & 63;
		long bitmask = 1L << bit;
		bits[(wordNum / (OpenBitSet.PAGE_SIZE))][(wordNum % (OpenBitSet.PAGE_SIZE))] &= ~bitmask;
	}

	public void clear(long index) {
		int wordNum = ((int) (index >> 6));
		int bit = ((int) (index)) & 63;
		long bitmask = 1L << bit;
		bits[(wordNum / (OpenBitSet.PAGE_SIZE))][(wordNum % (OpenBitSet.PAGE_SIZE))] &= ~bitmask;
	}

	public void clear(int startIndex, int endIndex) {
		if (endIndex <= startIndex)
			return;

		int startWord = startIndex >> 6;
		if (startWord >= (wlen))
			return;

		int endWord = (endIndex - 1) >> 6;
		long startmask = (-1L) << startIndex;
		long endmask = (-1L) >>> (-endIndex);
		startmask = ~startmask;
		endmask = ~endmask;
		if (startWord == endWord) {
			bits[(startWord / (OpenBitSet.PAGE_SIZE))][(startWord % (OpenBitSet.PAGE_SIZE))] &= startmask | endmask;
			return;
		}
		bits[(startWord / (OpenBitSet.PAGE_SIZE))][(startWord % (OpenBitSet.PAGE_SIZE))] &= startmask;
		int middle = Math.min(wlen, endWord);
		if ((startWord / (OpenBitSet.PAGE_SIZE)) == (middle / (OpenBitSet.PAGE_SIZE))) {
			Arrays.fill(bits[(startWord / (OpenBitSet.PAGE_SIZE))], ((startWord + 1) % (OpenBitSet.PAGE_SIZE)), (middle % (OpenBitSet.PAGE_SIZE)), 0L);
		}else {
			while ((++startWord) < middle)
				bits[(startWord / (OpenBitSet.PAGE_SIZE))][(startWord % (OpenBitSet.PAGE_SIZE))] = 0L;

		}
		if (endWord < (wlen)) {
			bits[(endWord / (OpenBitSet.PAGE_SIZE))][(endWord % (OpenBitSet.PAGE_SIZE))] &= endmask;
		}
	}

	public void clear(long startIndex, long endIndex) {
		if (endIndex <= startIndex)
			return;

		int startWord = ((int) (startIndex >> 6));
		if (startWord >= (wlen))
			return;

		int endWord = ((int) ((endIndex - 1) >> 6));
		long startmask = (-1L) << startIndex;
		long endmask = (-1L) >>> (-endIndex);
		startmask = ~startmask;
		endmask = ~endmask;
		if (startWord == endWord) {
			bits[(startWord / (OpenBitSet.PAGE_SIZE))][(startWord % (OpenBitSet.PAGE_SIZE))] &= startmask | endmask;
			return;
		}
		bits[(startWord / (OpenBitSet.PAGE_SIZE))][(startWord % (OpenBitSet.PAGE_SIZE))] &= startmask;
		int middle = Math.min(wlen, endWord);
		if ((startWord / (OpenBitSet.PAGE_SIZE)) == (middle / (OpenBitSet.PAGE_SIZE))) {
			Arrays.fill(bits[(startWord / (OpenBitSet.PAGE_SIZE))], ((startWord + 1) % (OpenBitSet.PAGE_SIZE)), (middle % (OpenBitSet.PAGE_SIZE)), 0L);
		}else {
			while ((++startWord) < middle)
				bits[(startWord / (OpenBitSet.PAGE_SIZE))][(startWord % (OpenBitSet.PAGE_SIZE))] = 0L;

		}
		if (endWord < (wlen)) {
			bits[(endWord / (OpenBitSet.PAGE_SIZE))][(endWord % (OpenBitSet.PAGE_SIZE))] &= endmask;
		}
	}

	public long cardinality() {
		long bitCount = 0L;
		for (int i = getPageCount(); (i--) > 0;) {
		}
		return bitCount;
	}

	public void intersect(OpenBitSet other) {
		int newLen = Math.min(this.wlen, other.wlen);
		long[][] thisArr = this.bits;
		long[][] otherArr = other.bits;
		int thisPageSize = OpenBitSet.PAGE_SIZE;
		int otherPageSize = OpenBitSet.PAGE_SIZE;
		int pos = newLen;
		while ((--pos) >= 0) {
			thisArr[(pos / thisPageSize)][(pos % thisPageSize)] &= otherArr[(pos / otherPageSize)][(pos % otherPageSize)];
		} 
		if ((this.wlen) > newLen) {
			for (pos = wlen; (pos--) > newLen;)
				thisArr[(pos / thisPageSize)][(pos % thisPageSize)] = 0;

		}
		this.wlen = newLen;
	}

	public void and(OpenBitSet other) {
		intersect(other);
	}

	public void trimTrailingZeros() {
		int idx = (wlen) - 1;
		while ((idx >= 0) && ((bits[(idx / (OpenBitSet.PAGE_SIZE))][(idx % (OpenBitSet.PAGE_SIZE))]) == 0))
			idx--;

		wlen = idx + 1;
	}

	public static long bits2words(long numBits) {
		return ((numBits - 1) >>> 6) + 1;
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if (!(o instanceof OpenBitSet))
			return false;

		OpenBitSet a;
		OpenBitSet b = ((OpenBitSet) (o));
		if ((b.wlen) > (this.wlen)) {
			a = b;
			b = this;
		}else {
			a = this;
		}
		int aPageSize = OpenBitSet.PAGE_SIZE;
		int bPageSize = OpenBitSet.PAGE_SIZE;
		for (int i = (a.wlen) - 1; i >= (b.wlen); i--) {
			if ((a.bits[(i / aPageSize)][(i % aPageSize)]) != 0)
				return false;

		}
		for (int i = (b.wlen) - 1; i >= 0; i--) {
			if ((a.bits[(i / aPageSize)][(i % aPageSize)]) != (b.bits[(i / bPageSize)][(i % bPageSize)]))
				return false;

		}
		return true;
	}

	@Override
	public int hashCode() {
		long h = 0;
		for (int i = wlen; (--i) >= 0;) {
			h ^= bits[(i / (OpenBitSet.PAGE_SIZE))][(i % (OpenBitSet.PAGE_SIZE))];
			h = (h << 1) | (h >>> 63);
		}
		return ((int) ((h >> 32) ^ h)) + -1737092556;
	}

	public void close() {
	}

	public void serialize(DataOutput out) throws IOException {
		int bitLength = getNumWords();
		int pageSize = getPageSize();
		int pageCount = getPageCount();
		out.writeInt(bitLength);
		for (int p = 0; p < pageCount; p++) {
			long[] bits = getPage(p);
			for (int i = 0; (i < pageSize) && ((bitLength--) > 0); i++) {
				out.writeLong(bits[i]);
			}
		}
	}

	public long serializedSize() {
		int bitLength = getNumWords();
		int pageSize = getPageSize();
		int pageCount = getPageCount();
		long size = TypeSizes.sizeof(bitLength);
		for (int p = 0; p < pageCount; p++) {
			long[] bits = getPage(p);
			for (int i = 0; (i < pageSize) && ((bitLength--) > 0); i++)
				size += TypeSizes.sizeof(bits[i]);

		}
		return size;
	}

	public void clear() {
		clear(0, capacity());
	}

	public static OpenBitSet deserialize(DataInput in) throws IOException {
		long bitLength = in.readInt();
		OpenBitSet bs = new OpenBitSet((bitLength << 6));
		int pageSize = bs.getPageSize();
		int pageCount = bs.getPageCount();
		for (int p = 0; p < pageCount; p++) {
			long[] bits = bs.getPage(p);
			for (int i = 0; (i < pageSize) && ((bitLength--) > 0); i++)
				bits[i] = in.readLong();

		}
		return bs;
	}
}

