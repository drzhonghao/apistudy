

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.cassandra.cache.CacheProvider;
import org.apache.cassandra.cache.ICache;
import org.apache.cassandra.cache.IRowCacheEntry;
import org.apache.cassandra.cache.RowCacheKey;
import org.apache.cassandra.cache.RowCacheSentinel;
import org.apache.cassandra.cache.SerializingCache;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.partitions.CachedPartition;
import org.apache.cassandra.io.ISerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;


public class SerializingCacheProvider implements CacheProvider<RowCacheKey, IRowCacheEntry> {
	public ICache<RowCacheKey, IRowCacheEntry> create() {
		return SerializingCache.create((((DatabaseDescriptor.getRowCacheSizeInMB()) * 1024) * 1024), new SerializingCacheProvider.RowCacheSerializer());
	}

	static class RowCacheSerializer implements ISerializer<IRowCacheEntry> {
		public void serialize(IRowCacheEntry entry, DataOutputPlus out) throws IOException {
			assert entry != null;
			boolean isSentinel = entry instanceof RowCacheSentinel;
			out.writeBoolean(isSentinel);
			if (isSentinel) {
			}else
				CachedPartition.cacheSerializer.serialize(((CachedPartition) (entry)), out);

		}

		public IRowCacheEntry deserialize(DataInputPlus in) throws IOException {
			boolean isSentinel = in.readBoolean();
			if (isSentinel) {
			}
			return CachedPartition.cacheSerializer.deserialize(in);
		}

		public long serializedSize(IRowCacheEntry entry) {
			int size = TypeSizes.sizeof(true);
			if (entry instanceof RowCacheSentinel) {
			}else
				size += CachedPartition.cacheSerializer.serializedSize(((CachedPartition) (entry)));

			return size;
		}
	}
}

