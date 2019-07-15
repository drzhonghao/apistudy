

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.apache.cassandra.cache.AutoSavingCache;
import org.apache.cassandra.cache.CacheKey;
import org.apache.cassandra.cache.CacheProvider;
import org.apache.cassandra.cache.ConcurrentLinkedHashCache;
import org.apache.cassandra.cache.CounterCacheKey;
import org.apache.cassandra.cache.ICache;
import org.apache.cassandra.cache.IRowCacheEntry;
import org.apache.cassandra.cache.InstrumentingCache;
import org.apache.cassandra.cache.KeyCacheKey;
import org.apache.cassandra.cache.RowCacheKey;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ClockAndCount;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.LegacyLayout;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.context.CounterContext;
import org.apache.cassandra.db.filter.ClusteringIndexFilter;
import org.apache.cassandra.db.filter.ClusteringIndexNamesFilter;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.filter.DataLimits;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.db.partitions.CachedBTreePartition;
import org.apache.cassandra.db.partitions.CachedPartition;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterators;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.io.sstable.format.big.BigFormat;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.schema.CachingParams;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.service.CacheServiceMBean;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.db.RowIndexEntry.Serializer.skipForCache;


public class CacheService implements CacheServiceMBean {
	private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

	public static final String MBEAN_NAME = "org.apache.cassandra.db:type=Caches";

	public enum CacheType {

		KEY_CACHE("KeyCache"),
		ROW_CACHE("RowCache"),
		COUNTER_CACHE("CounterCache");
		private final String name;

		CacheType(String typeName) {
			name = typeName;
		}

		public String toString() {
			return name;
		}
	}

	public static final CacheService instance = new CacheService();

	public final AutoSavingCache<KeyCacheKey, RowIndexEntry> keyCache;

	public final AutoSavingCache<RowCacheKey, IRowCacheEntry> rowCache;

	public final AutoSavingCache<CounterCacheKey, ClockAndCount> counterCache;

	private CacheService() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			mbs.registerMBean(this, new ObjectName(CacheService.MBEAN_NAME));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		keyCache = initKeyCache();
		rowCache = initRowCache();
		counterCache = initCounterCache();
	}

	private AutoSavingCache<KeyCacheKey, RowIndexEntry> initKeyCache() {
		CacheService.logger.info("Initializing key cache with capacity of {} MBs.", DatabaseDescriptor.getKeyCacheSizeInMB());
		long keyCacheInMemoryCapacity = ((DatabaseDescriptor.getKeyCacheSizeInMB()) * 1024) * 1024;
		ICache<KeyCacheKey, RowIndexEntry> kc;
		kc = ConcurrentLinkedHashCache.create(keyCacheInMemoryCapacity);
		int keyCacheKeysToSave = DatabaseDescriptor.getKeyCacheKeysToSave();
		keyCache.scheduleSaving(DatabaseDescriptor.getKeyCacheSavePeriod(), keyCacheKeysToSave);
		return keyCache;
	}

	private AutoSavingCache<RowCacheKey, IRowCacheEntry> initRowCache() {
		CacheService.logger.info("Initializing row cache with capacity of {} MBs", DatabaseDescriptor.getRowCacheSizeInMB());
		CacheProvider<RowCacheKey, IRowCacheEntry> cacheProvider;
		String cacheProviderClassName = ((DatabaseDescriptor.getRowCacheSizeInMB()) > 0) ? DatabaseDescriptor.getRowCacheClassName() : "org.apache.cassandra.cache.NopCacheProvider";
		try {
			Class<CacheProvider<RowCacheKey, IRowCacheEntry>> cacheProviderClass = ((Class<CacheProvider<RowCacheKey, IRowCacheEntry>>) (Class.forName(cacheProviderClassName)));
			cacheProvider = cacheProviderClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(("Cannot find configured row cache provider class " + (DatabaseDescriptor.getRowCacheClassName())));
		}
		ICache<RowCacheKey, IRowCacheEntry> rc = cacheProvider.create();
		int rowCacheKeysToSave = DatabaseDescriptor.getRowCacheKeysToSave();
		rowCache.scheduleSaving(DatabaseDescriptor.getRowCacheSavePeriod(), rowCacheKeysToSave);
		return rowCache;
	}

	private AutoSavingCache<CounterCacheKey, ClockAndCount> initCounterCache() {
		CacheService.logger.info("Initializing counter cache with capacity of {} MBs", DatabaseDescriptor.getCounterCacheSizeInMB());
		long capacity = ((DatabaseDescriptor.getCounterCacheSizeInMB()) * 1024) * 1024;
		int keysToSave = DatabaseDescriptor.getCounterCacheKeysToSave();
		CacheService.logger.info("Scheduling counter cache save to every {} seconds (going to save {} keys).", DatabaseDescriptor.getCounterCacheSavePeriod(), (keysToSave == (Integer.MAX_VALUE) ? "all" : keysToSave));
		return null;
	}

	public int getRowCacheSavePeriodInSeconds() {
		return DatabaseDescriptor.getRowCacheSavePeriod();
	}

	public void setRowCacheSavePeriodInSeconds(int seconds) {
		if (seconds < 0)
			throw new RuntimeException("RowCacheSavePeriodInSeconds must be non-negative.");

		DatabaseDescriptor.setRowCacheSavePeriod(seconds);
		rowCache.scheduleSaving(seconds, DatabaseDescriptor.getRowCacheKeysToSave());
	}

	public int getKeyCacheSavePeriodInSeconds() {
		return DatabaseDescriptor.getKeyCacheSavePeriod();
	}

	public void setKeyCacheSavePeriodInSeconds(int seconds) {
		if (seconds < 0)
			throw new RuntimeException("KeyCacheSavePeriodInSeconds must be non-negative.");

		DatabaseDescriptor.setKeyCacheSavePeriod(seconds);
		keyCache.scheduleSaving(seconds, DatabaseDescriptor.getKeyCacheKeysToSave());
	}

	public int getCounterCacheSavePeriodInSeconds() {
		return DatabaseDescriptor.getCounterCacheSavePeriod();
	}

	public void setCounterCacheSavePeriodInSeconds(int seconds) {
		if (seconds < 0)
			throw new RuntimeException("CounterCacheSavePeriodInSeconds must be non-negative.");

		DatabaseDescriptor.setCounterCacheSavePeriod(seconds);
		counterCache.scheduleSaving(seconds, DatabaseDescriptor.getCounterCacheKeysToSave());
	}

	public int getRowCacheKeysToSave() {
		return DatabaseDescriptor.getRowCacheKeysToSave();
	}

	public void setRowCacheKeysToSave(int count) {
		if (count < 0)
			throw new RuntimeException("RowCacheKeysToSave must be non-negative.");

		DatabaseDescriptor.setRowCacheKeysToSave(count);
		rowCache.scheduleSaving(getRowCacheSavePeriodInSeconds(), count);
	}

	public int getKeyCacheKeysToSave() {
		return DatabaseDescriptor.getKeyCacheKeysToSave();
	}

	public void setKeyCacheKeysToSave(int count) {
		if (count < 0)
			throw new RuntimeException("KeyCacheKeysToSave must be non-negative.");

		DatabaseDescriptor.setKeyCacheKeysToSave(count);
		keyCache.scheduleSaving(getKeyCacheSavePeriodInSeconds(), count);
	}

	public int getCounterCacheKeysToSave() {
		return DatabaseDescriptor.getCounterCacheKeysToSave();
	}

	public void setCounterCacheKeysToSave(int count) {
		if (count < 0)
			throw new RuntimeException("CounterCacheKeysToSave must be non-negative.");

		DatabaseDescriptor.setCounterCacheKeysToSave(count);
		counterCache.scheduleSaving(getCounterCacheSavePeriodInSeconds(), count);
	}

	public void invalidateKeyCache() {
		keyCache.clear();
	}

	public void invalidateKeyCacheForCf(Pair<String, String> ksAndCFName) {
		Iterator<KeyCacheKey> keyCacheIterator = keyCache.keyIterator();
		while (keyCacheIterator.hasNext()) {
			KeyCacheKey key = keyCacheIterator.next();
			if (key.ksAndCFName.equals(ksAndCFName))
				keyCacheIterator.remove();

		} 
	}

	public void invalidateRowCache() {
		rowCache.clear();
	}

	public void invalidateRowCacheForCf(Pair<String, String> ksAndCFName) {
		Iterator<RowCacheKey> rowCacheIterator = rowCache.keyIterator();
		while (rowCacheIterator.hasNext()) {
			RowCacheKey rowCacheKey = rowCacheIterator.next();
			if (rowCacheKey.ksAndCFName.equals(ksAndCFName))
				rowCacheIterator.remove();

		} 
	}

	public void invalidateCounterCacheForCf(Pair<String, String> ksAndCFName) {
		Iterator<CounterCacheKey> counterCacheIterator = counterCache.keyIterator();
		while (counterCacheIterator.hasNext()) {
			CounterCacheKey counterCacheKey = counterCacheIterator.next();
			if (counterCacheKey.ksAndCFName.equals(ksAndCFName))
				counterCacheIterator.remove();

		} 
	}

	public void invalidateCounterCache() {
		counterCache.clear();
	}

	public void setRowCacheCapacityInMB(long capacity) {
		if (capacity < 0)
			throw new RuntimeException("capacity should not be negative.");

		rowCache.setCapacity(((capacity * 1024) * 1024));
	}

	public void setKeyCacheCapacityInMB(long capacity) {
		if (capacity < 0)
			throw new RuntimeException("capacity should not be negative.");

		keyCache.setCapacity(((capacity * 1024) * 1024));
	}

	public void setCounterCacheCapacityInMB(long capacity) {
		if (capacity < 0)
			throw new RuntimeException("capacity should not be negative.");

		counterCache.setCapacity(((capacity * 1024) * 1024));
	}

	public void saveCaches() throws InterruptedException, ExecutionException {
		List<Future<?>> futures = new ArrayList<>(3);
		CacheService.logger.debug("submitting cache saves");
		futures.add(keyCache.submitWrite(DatabaseDescriptor.getKeyCacheKeysToSave()));
		futures.add(rowCache.submitWrite(DatabaseDescriptor.getRowCacheKeysToSave()));
		futures.add(counterCache.submitWrite(DatabaseDescriptor.getCounterCacheKeysToSave()));
		FBUtilities.waitOnFutures(futures);
		CacheService.logger.debug("cache saves completed");
	}

	public static class CounterCacheSerializer implements AutoSavingCache.CacheSerializer<CounterCacheKey, ClockAndCount> {
		public void serialize(CounterCacheKey key, DataOutputPlus out, ColumnFamilyStore cfs) throws IOException {
			assert cfs.metadata.isCounter();
			out.write(cfs.metadata.ksAndCFBytes);
			ByteBufferUtil.writeWithLength(key.partitionKey, out);
			ByteBufferUtil.writeWithLength(key.cellName, out);
		}

		public Future<Pair<CounterCacheKey, ClockAndCount>> deserialize(DataInputPlus in, final ColumnFamilyStore cfs) throws IOException {
			final ByteBuffer partitionKey = ByteBufferUtil.readWithLength(in);
			final ByteBuffer cellName = ByteBufferUtil.readWithLength(in);
			if (((cfs == null) || (!(cfs.metadata.isCounter()))) || (!(cfs.isCounterCacheEnabled())))
				return null;

			assert cfs.metadata.isCounter();
			return StageManager.getStage(Stage.READ).submit(new Callable<Pair<CounterCacheKey, ClockAndCount>>() {
				public Pair<CounterCacheKey, ClockAndCount> call() throws Exception {
					DecoratedKey key = cfs.decorateKey(partitionKey);
					LegacyLayout.LegacyCellName name = LegacyLayout.decodeCellName(cfs.metadata, cellName);
					ColumnDefinition column = name.column;
					CellPath path = ((name.collectionElement) == null) ? null : CellPath.create(name.collectionElement);
					int nowInSec = FBUtilities.nowInSeconds();
					ColumnFilter.Builder builder = ColumnFilter.selectionBuilder();
					if (path == null)
						builder.add(column);
					else
						builder.select(column, path);

					ClusteringIndexFilter filter = new ClusteringIndexNamesFilter(FBUtilities.<Clustering>singleton(name.clustering, cfs.metadata.comparator), false);
					SinglePartitionReadCommand cmd = SinglePartitionReadCommand.create(cfs.metadata, nowInSec, key, builder.build(), filter);
					try (ReadExecutionController controller = cmd.executionController();RowIterator iter = UnfilteredRowIterators.filter(cmd.queryMemtableAndDisk(cfs, controller), nowInSec)) {
						Cell cell;
						if (column.isStatic()) {
							cell = iter.staticRow().getCell(column);
						}else {
							if (!(iter.hasNext()))
								return null;

							cell = iter.next().getCell(column);
						}
						if (cell == null)
							return null;

						ClockAndCount clockAndCount = CounterContext.instance().getLocalClockAndCount(cell.value());
						return Pair.create(CounterCacheKey.create(cfs.metadata.ksAndCFName, partitionKey, name.clustering, column, path), clockAndCount);
					}
				}
			});
		}
	}

	public static class RowCacheSerializer implements AutoSavingCache.CacheSerializer<RowCacheKey, IRowCacheEntry> {
		public void serialize(RowCacheKey key, DataOutputPlus out, ColumnFamilyStore cfs) throws IOException {
			assert !(cfs.isIndex());
			out.write(cfs.metadata.ksAndCFBytes);
			ByteBufferUtil.writeWithLength(key.key, out);
		}

		public Future<Pair<RowCacheKey, IRowCacheEntry>> deserialize(DataInputPlus in, final ColumnFamilyStore cfs) throws IOException {
			final ByteBuffer buffer = ByteBufferUtil.readWithLength(in);
			if ((cfs == null) || (!(cfs.isRowCacheEnabled())))
				return null;

			final int rowsToCache = cfs.metadata.params.caching.rowsPerPartitionToCache();
			assert !(cfs.isIndex());
			return StageManager.getStage(Stage.READ).submit(new Callable<Pair<RowCacheKey, IRowCacheEntry>>() {
				public Pair<RowCacheKey, IRowCacheEntry> call() throws Exception {
					DecoratedKey key = cfs.decorateKey(buffer);
					int nowInSec = FBUtilities.nowInSeconds();
					SinglePartitionReadCommand cmd = SinglePartitionReadCommand.fullPartitionRead(cfs.metadata, nowInSec, key);
					try (ReadExecutionController controller = cmd.executionController();UnfilteredRowIterator iter = cmd.queryMemtableAndDisk(cfs, controller)) {
						CachedPartition toCache = CachedBTreePartition.create(DataLimits.cqlLimits(rowsToCache).filter(iter, nowInSec, true), nowInSec);
						return Pair.create(new RowCacheKey(cfs.metadata.ksAndCFName, key), ((IRowCacheEntry) (toCache)));
					}
				}
			});
		}
	}

	public static class KeyCacheSerializer implements AutoSavingCache.CacheSerializer<KeyCacheKey, RowIndexEntry> {
		public void serialize(KeyCacheKey key, DataOutputPlus out, ColumnFamilyStore cfs) throws IOException {
			if (!(key.desc.version.storeRows()))
				return;

			RowIndexEntry entry = CacheService.instance.keyCache.getInternal(key);
			if (entry == null)
				return;

			out.write(cfs.metadata.ksAndCFBytes);
			ByteBufferUtil.writeWithLength(key.key, out);
			out.writeInt(key.desc.generation);
			out.writeBoolean(true);
			SerializationHeader header = new SerializationHeader(false, cfs.metadata, cfs.metadata.partitionColumns(), EncodingStats.NO_STATS);
			key.desc.getFormat().getIndexSerializer(cfs.metadata, key.desc.version, header).serializeForCache(entry, out);
		}

		public Future<Pair<KeyCacheKey, RowIndexEntry>> deserialize(DataInputPlus input, ColumnFamilyStore cfs) throws IOException {
			int keyLength = input.readInt();
			if (keyLength > (FBUtilities.MAX_UNSIGNED_SHORT)) {
				throw new IOException(String.format("Corrupted key cache. Key length of %d is longer than maximum of %d", keyLength, FBUtilities.MAX_UNSIGNED_SHORT));
			}
			ByteBuffer key = ByteBufferUtil.read(input, keyLength);
			int generation = input.readInt();
			input.readBoolean();
			SSTableReader reader;
			if (((cfs == null) || (!(cfs.isKeyCacheEnabled()))) || ((reader = findDesc(generation, cfs.getSSTables(SSTableSet.CANONICAL))) == null)) {
				skipForCache(input, BigFormat.instance.getLatestVersion());
				return null;
			}
			RowIndexEntry.IndexSerializer<?> indexSerializer = reader.descriptor.getFormat().getIndexSerializer(reader.metadata, reader.descriptor.version, reader.header);
			RowIndexEntry<?> entry = indexSerializer.deserializeForCache(input);
			return Futures.immediateFuture(Pair.create(new KeyCacheKey(cfs.metadata.ksAndCFName, reader.descriptor, key), entry));
		}

		private SSTableReader findDesc(int generation, Iterable<SSTableReader> collection) {
			for (SSTableReader sstable : collection) {
				if ((sstable.descriptor.generation) == generation)
					return sstable;

			}
			return null;
		}
	}
}

