

import java.io.DataInput;
import java.io.IOException;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.utils.AlwaysPresentFilter;
import org.apache.cassandra.utils.BloomCalculations;
import org.apache.cassandra.utils.IFilter;
import org.apache.cassandra.utils.obs.IBitSet;
import org.apache.cassandra.utils.obs.OffHeapBitSet;
import org.apache.cassandra.utils.obs.OpenBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FilterFactory {
	public static final IFilter AlwaysPresent = new AlwaysPresentFilter();

	private static final Logger logger = LoggerFactory.getLogger(FilterFactory.class);

	private static final long BITSET_EXCESS = 20;

	public static void serialize(IFilter bf, DataOutputPlus output) throws IOException {
	}

	public static IFilter deserialize(DataInput input, boolean offheap, boolean oldBfHashOrder) throws IOException {
		return null;
	}

	public static IFilter getFilter(long numElements, int targetBucketsPerElem, boolean offheap, boolean oldBfHashOrder) {
		int maxBucketsPerElement = Math.max(1, BloomCalculations.maxBucketsPerElement(numElements));
		int bucketsPerElement = Math.min(targetBucketsPerElem, maxBucketsPerElement);
		if (bucketsPerElement < targetBucketsPerElem) {
			FilterFactory.logger.warn("Cannot provide an optimal BloomFilter for {} elements ({}/{} buckets per element).", numElements, bucketsPerElement, targetBucketsPerElem);
		}
		BloomCalculations.BloomSpecification spec = BloomCalculations.computeBloomSpec(bucketsPerElement);
		return null;
	}

	public static IFilter getFilter(long numElements, double maxFalsePosProbability, boolean offheap, boolean oldBfHashOrder) {
		assert maxFalsePosProbability <= 1.0 : "Invalid probability";
		if (maxFalsePosProbability == 1.0)
			return new AlwaysPresentFilter();

		int bucketsPerElement = BloomCalculations.maxBucketsPerElement(numElements);
		BloomCalculations.BloomSpecification spec = BloomCalculations.computeBloomSpec(bucketsPerElement, maxFalsePosProbability);
		return null;
	}

	@SuppressWarnings("resource")
	private static IFilter createFilter(int hash, long numElements, int bucketsPer, boolean offheap, boolean oldBfHashOrder) {
		long numBits = (numElements * bucketsPer) + (FilterFactory.BITSET_EXCESS);
		IBitSet bitset = (offheap) ? new OffHeapBitSet(numBits) : new OpenBitSet(numBits);
		return null;
	}
}

