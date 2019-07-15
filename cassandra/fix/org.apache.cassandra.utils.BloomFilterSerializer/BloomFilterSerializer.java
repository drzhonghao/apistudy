

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.utils.BloomFilter;
import org.apache.cassandra.utils.obs.IBitSet;
import org.apache.cassandra.utils.obs.OffHeapBitSet;
import org.apache.cassandra.utils.obs.OpenBitSet;


final class BloomFilterSerializer {
	private BloomFilterSerializer() {
	}

	public static void serialize(BloomFilter bf, DataOutputPlus out) throws IOException {
		out.writeInt(bf.hashCount);
		bf.bitset.serialize(out);
	}

	public static BloomFilter deserialize(DataInput in, boolean oldBfHashOrder) throws IOException {
		return BloomFilterSerializer.deserialize(in, false, oldBfHashOrder);
	}

	@SuppressWarnings("resource")
	public static BloomFilter deserialize(DataInput in, boolean offheap, boolean oldBfHashOrder) throws IOException {
		int hashes = in.readInt();
		IBitSet bs = (offheap) ? OffHeapBitSet.deserialize(in) : OpenBitSet.deserialize(in);
		return null;
	}

	public static long serializedSize(BloomFilter bf) {
		int size = TypeSizes.sizeof(bf.hashCount);
		size += bf.bitset.serializedSize();
		return size;
	}
}

