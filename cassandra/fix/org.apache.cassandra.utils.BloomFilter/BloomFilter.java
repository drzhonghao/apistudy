

import io.netty.util.concurrent.FastThreadLocal;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.IFilter;
import org.apache.cassandra.utils.concurrent.Ref;
import org.apache.cassandra.utils.concurrent.SharedCloseableImpl;
import org.apache.cassandra.utils.concurrent.WrappedSharedCloseable;
import org.apache.cassandra.utils.obs.IBitSet;


public class BloomFilter extends WrappedSharedCloseable implements IFilter {
	private static final FastThreadLocal<long[]> reusableIndexes = new FastThreadLocal<long[]>() {
		protected long[] initialValue() {
			return new long[21];
		}
	};

	public final IBitSet bitset;

	public final int hashCount;

	public final boolean oldBfHashOrder;

	BloomFilter(int hashCount, IBitSet bitset, boolean oldBfHashOrder) {
		super(bitset);
		this.hashCount = hashCount;
		this.bitset = bitset;
		this.oldBfHashOrder = oldBfHashOrder;
	}

	private BloomFilter(BloomFilter copy) {
		super(copy);
		this.hashCount = copy.hashCount;
		this.bitset = copy.bitset;
		this.oldBfHashOrder = copy.oldBfHashOrder;
	}

	public long serializedSize() {
		return 0l;
	}

	@com.google.common.annotations.VisibleForTesting
	public long[] getHashBuckets(IFilter.FilterKey key, int hashCount, long max) {
		long[] hash = new long[2];
		key.filterHash(hash);
		long[] indexes = new long[hashCount];
		setIndexes(hash[1], hash[0], hashCount, max, indexes);
		return indexes;
	}

	@net.nicoulaj.compilecommand.annotations.Inline
	private long[] indexes(IFilter.FilterKey key) {
		long[] indexes = BloomFilter.reusableIndexes.get();
		key.filterHash(indexes);
		setIndexes(indexes[1], indexes[0], hashCount, bitset.capacity(), indexes);
		return indexes;
	}

	@net.nicoulaj.compilecommand.annotations.Inline
	private void setIndexes(long base, long inc, int count, long max, long[] results) {
		if (oldBfHashOrder) {
			long x = inc;
			inc = base;
			base = x;
		}
		for (int i = 0; i < count; i++) {
			results[i] = FBUtilities.abs((base % max));
			base += inc;
		}
	}

	public void add(IFilter.FilterKey key) {
		long[] indexes = indexes(key);
		for (int i = 0; i < (hashCount); i++) {
			bitset.set(indexes[i]);
		}
	}

	public final boolean isPresent(IFilter.FilterKey key) {
		long[] indexes = indexes(key);
		for (int i = 0; i < (hashCount); i++) {
			if (!(bitset.get(indexes[i]))) {
				return false;
			}
		}
		return true;
	}

	public void clear() {
		bitset.clear();
	}

	public IFilter sharedCopy() {
		return new BloomFilter(this);
	}

	@Override
	public long offHeapSize() {
		return bitset.offHeapSize();
	}

	public String toString() {
		return ((((("BloomFilter[hashCount=" + (hashCount)) + ";oldBfHashOrder=") + (oldBfHashOrder)) + ";capacity=") + (bitset.capacity())) + ']';
	}

	public void addTo(Ref.IdentityCollection identities) {
		super.addTo(identities);
		bitset.addTo(identities);
	}
}

