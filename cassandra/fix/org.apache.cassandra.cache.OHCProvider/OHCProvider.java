

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import org.apache.cassandra.cache.CacheKey;
import org.apache.cassandra.cache.CacheProvider;
import org.apache.cassandra.cache.ICache;
import org.apache.cassandra.cache.IRowCacheEntry;
import org.apache.cassandra.cache.RowCacheKey;
import org.apache.cassandra.cache.RowCacheSentinel;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.partitions.CachedPartition;
import org.apache.cassandra.io.ISerializer;
import org.apache.cassandra.io.util.BufferedDataOutputStreamPlus;
import org.apache.cassandra.io.util.DataInputBuffer;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.DataOutputBufferFixed;
import org.apache.cassandra.io.util.RebufferingInputStream;
import org.apache.cassandra.utils.Pair;
import org.caffinitas.ohc.CacheSerializer;
import org.caffinitas.ohc.CloseableIterator;
import org.caffinitas.ohc.OHCache;
import org.caffinitas.ohc.OHCacheBuilder;


public class OHCProvider implements CacheProvider<RowCacheKey, IRowCacheEntry> {
	public ICache<RowCacheKey, IRowCacheEntry> create() {
		OHCacheBuilder<RowCacheKey, IRowCacheEntry> builder = OHCacheBuilder.newBuilder();
		builder.capacity((((DatabaseDescriptor.getRowCacheSizeInMB()) * 1024) * 1024)).keySerializer(OHCProvider.KeySerializer.instance).valueSerializer(OHCProvider.ValueSerializer.instance).throwOOME(true);
		return new OHCProvider.OHCacheAdapter(builder.build());
	}

	private static class OHCacheAdapter implements ICache<RowCacheKey, IRowCacheEntry> {
		private final OHCache<RowCacheKey, IRowCacheEntry> ohCache;

		public OHCacheAdapter(OHCache<RowCacheKey, IRowCacheEntry> ohCache) {
			this.ohCache = ohCache;
		}

		public long capacity() {
			return ohCache.capacity();
		}

		public void setCapacity(long capacity) {
			ohCache.setCapacity(capacity);
		}

		public void put(RowCacheKey key, IRowCacheEntry value) {
			ohCache.put(key, value);
		}

		public boolean putIfAbsent(RowCacheKey key, IRowCacheEntry value) {
			return ohCache.putIfAbsent(key, value);
		}

		public boolean replace(RowCacheKey key, IRowCacheEntry old, IRowCacheEntry value) {
			return ohCache.addOrReplace(key, old, value);
		}

		public IRowCacheEntry get(RowCacheKey key) {
			return ohCache.get(key);
		}

		public void remove(RowCacheKey key) {
			ohCache.remove(key);
		}

		public int size() {
			return ((int) (ohCache.size()));
		}

		public long weightedSize() {
			return ohCache.memUsed();
		}

		public void clear() {
			ohCache.clear();
		}

		public Iterator<RowCacheKey> hotKeyIterator(int n) {
			return ohCache.hotKeyIterator(n);
		}

		public Iterator<RowCacheKey> keyIterator() {
			return ohCache.keyIterator();
		}

		public boolean containsKey(RowCacheKey key) {
			return ohCache.containsKey(key);
		}
	}

	private static class KeySerializer implements CacheSerializer<RowCacheKey> {
		private static OHCProvider.KeySerializer instance = new OHCProvider.KeySerializer();

		public void serialize(RowCacheKey rowCacheKey, ByteBuffer buf) {
			@SuppressWarnings("resource")
			DataOutputBuffer dataOutput = new DataOutputBufferFixed(buf);
			try {
				dataOutput.writeUTF(rowCacheKey.ksAndCFName.left);
				dataOutput.writeUTF(rowCacheKey.ksAndCFName.right);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			buf.putInt(rowCacheKey.key.length);
			buf.put(rowCacheKey.key);
		}

		public RowCacheKey deserialize(ByteBuffer buf) {
			@SuppressWarnings("resource")
			DataInputBuffer dataInput = new DataInputBuffer(buf, false);
			String ksName = null;
			String cfName = null;
			try {
				ksName = dataInput.readUTF();
				cfName = dataInput.readUTF();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			byte[] key = new byte[buf.getInt()];
			buf.get(key);
			return new RowCacheKey(Pair.create(ksName, cfName), key);
		}

		public int serializedSize(RowCacheKey rowCacheKey) {
			return (((TypeSizes.sizeof(rowCacheKey.ksAndCFName.left)) + (TypeSizes.sizeof(rowCacheKey.ksAndCFName.right))) + 4) + (rowCacheKey.key.length);
		}
	}

	private static class ValueSerializer implements CacheSerializer<IRowCacheEntry> {
		private static OHCProvider.ValueSerializer instance = new OHCProvider.ValueSerializer();

		public void serialize(IRowCacheEntry entry, ByteBuffer buf) {
			assert entry != null;
			try (DataOutputBufferFixed out = new DataOutputBufferFixed(buf)) {
				boolean isSentinel = entry instanceof RowCacheSentinel;
				out.writeBoolean(isSentinel);
				if (isSentinel) {
				}else
					CachedPartition.cacheSerializer.serialize(((CachedPartition) (entry)), out);

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@SuppressWarnings("resource")
		public IRowCacheEntry deserialize(ByteBuffer buf) {
			try {
				RebufferingInputStream in = new DataInputBuffer(buf, false);
				boolean isSentinel = in.readBoolean();
				if (isSentinel) {
				}
				return CachedPartition.cacheSerializer.deserialize(in);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public int serializedSize(IRowCacheEntry entry) {
			int size = TypeSizes.sizeof(true);
			if (entry instanceof RowCacheSentinel) {
			}else
				size += CachedPartition.cacheSerializer.serializedSize(((CachedPartition) (entry)));

			return size;
		}
	}
}

