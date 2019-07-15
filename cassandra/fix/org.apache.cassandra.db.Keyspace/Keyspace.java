

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.WriteType;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.compaction.CompactionManager;
import org.apache.cassandra.db.compaction.CompactionStrategyManager;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.view.TableViews;
import org.apache.cassandra.db.view.ViewManager;
import org.apache.cassandra.exceptions.WriteTimeoutException;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.SecondaryIndexManager;
import org.apache.cassandra.index.transactions.UpdateTransaction;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.locator.AbstractReplicationStrategy;
import org.apache.cassandra.locator.IEndpointSnitch;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.cassandra.metrics.KeyspaceMetrics;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.schema.ReplicationParams;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Keyspace {
	private static final Logger logger = LoggerFactory.getLogger(Keyspace.class);

	private static final String TEST_FAIL_WRITES_KS = System.getProperty("cassandra.test.fail_writes_ks", "");

	private static final boolean TEST_FAIL_WRITES = !(Keyspace.TEST_FAIL_WRITES_KS.isEmpty());

	private static int TEST_FAIL_MV_LOCKS_COUNT = Integer.getInteger("cassandra.test.fail_mv_locks_count", 0);

	public final KeyspaceMetrics metric;

	static {
		if ((DatabaseDescriptor.isDaemonInitialized()) || (DatabaseDescriptor.isToolInitialized()))
			DatabaseDescriptor.createAllDirectories();

	}

	private volatile KeyspaceMetadata metadata;

	public static final OpOrder writeOrder = new OpOrder();

	private final ConcurrentMap<UUID, ColumnFamilyStore> columnFamilyStores = new ConcurrentHashMap<>();

	private volatile AbstractReplicationStrategy replicationStrategy;

	public final ViewManager viewManager = null;

	private volatile ReplicationParams replicationParams;

	public static final Function<String, Keyspace> keyspaceTransformer = new Function<String, Keyspace>() {
		public Keyspace apply(String keyspaceName) {
			return Keyspace.open(keyspaceName);
		}
	};

	private static volatile boolean initialized = false;

	public static void setInitialized() {
		Keyspace.initialized = true;
	}

	public static Keyspace open(String keyspaceName) {
		assert (Keyspace.initialized) || (SchemaConstants.isLocalSystemKeyspace(keyspaceName));
		return Keyspace.open(keyspaceName, Schema.instance, true);
	}

	public static Keyspace openWithoutSSTables(String keyspaceName) {
		return Keyspace.open(keyspaceName, Schema.instance, false);
	}

	private static Keyspace open(String keyspaceName, Schema schema, boolean loadSSTables) {
		return null;
	}

	public static Keyspace clear(String keyspaceName) {
		return Keyspace.clear(keyspaceName, Schema.instance);
	}

	public static Keyspace clear(String keyspaceName, Schema schema) {
		synchronized(Keyspace.class) {
		}
		return null;
	}

	public static ColumnFamilyStore openAndGetStore(CFMetaData cfm) {
		return Keyspace.open(cfm.ksName).getColumnFamilyStore(cfm.cfId);
	}

	public static void removeUnreadableSSTables(File directory) {
		for (Keyspace keyspace : Keyspace.all()) {
			for (ColumnFamilyStore baseCfs : keyspace.getColumnFamilyStores()) {
				for (ColumnFamilyStore cfs : baseCfs.concatWithIndexes()) {
				}
			}
		}
	}

	public void setMetadata(KeyspaceMetadata metadata) {
		this.metadata = metadata;
		createReplicationStrategy(metadata);
	}

	public KeyspaceMetadata getMetadata() {
		return metadata;
	}

	public Collection<ColumnFamilyStore> getColumnFamilyStores() {
		return Collections.unmodifiableCollection(columnFamilyStores.values());
	}

	public ColumnFamilyStore getColumnFamilyStore(String cfName) {
		UUID id = Schema.instance.getId(getName(), cfName);
		if (id == null)
			throw new IllegalArgumentException(String.format("Unknown keyspace/cf pair (%s.%s)", getName(), cfName));

		return getColumnFamilyStore(id);
	}

	public ColumnFamilyStore getColumnFamilyStore(UUID id) {
		ColumnFamilyStore cfs = columnFamilyStores.get(id);
		if (cfs == null)
			throw new IllegalArgumentException(("Unknown CF " + id));

		return cfs;
	}

	public boolean hasColumnFamilyStore(UUID id) {
		return columnFamilyStores.containsKey(id);
	}

	public void snapshot(String snapshotName, String columnFamilyName, boolean skipFlush) throws IOException {
		assert snapshotName != null;
		boolean tookSnapShot = false;
		for (ColumnFamilyStore cfStore : columnFamilyStores.values()) {
			if ((columnFamilyName == null) || (cfStore.name.equals(columnFamilyName))) {
				tookSnapShot = true;
				cfStore.snapshot(snapshotName, skipFlush);
			}
		}
		if ((columnFamilyName != null) && (!tookSnapShot))
			throw new IOException((("Failed taking snapshot. Table " + columnFamilyName) + " does not exist."));

	}

	public void snapshot(String snapshotName, String columnFamilyName) throws IOException {
		snapshot(snapshotName, columnFamilyName, false);
	}

	public static String getTimestampedSnapshotName(String clientSuppliedName) {
		String snapshotName = Long.toString(System.currentTimeMillis());
		if ((clientSuppliedName != null) && (!(clientSuppliedName.equals("")))) {
			snapshotName = (snapshotName + "-") + clientSuppliedName;
		}
		return snapshotName;
	}

	public static String getTimestampedSnapshotNameWithPrefix(String clientSuppliedName, String prefix) {
		return (prefix + "-") + (Keyspace.getTimestampedSnapshotName(clientSuppliedName));
	}

	public boolean snapshotExists(String snapshotName) {
		assert snapshotName != null;
		for (ColumnFamilyStore cfStore : columnFamilyStores.values()) {
			if (cfStore.snapshotExists(snapshotName))
				return true;

		}
		return false;
	}

	public static void clearSnapshot(String snapshotName, String keyspace) {
		List<File> snapshotDirs = Directories.getKSChildDirectories(keyspace, ColumnFamilyStore.getInitialDirectories());
		Directories.clearSnapshot(snapshotName, snapshotDirs);
	}

	public List<SSTableReader> getAllSSTables(SSTableSet sstableSet) {
		List<SSTableReader> list = new ArrayList<>(columnFamilyStores.size());
		for (ColumnFamilyStore cfStore : columnFamilyStores.values())
			Iterables.addAll(list, cfStore.getSSTables(sstableSet));

		return list;
	}

	private Keyspace(String keyspaceName, boolean loadSSTables) {
		metadata = Schema.instance.getKSMetaData(keyspaceName);
		assert (metadata) != null : "Unknown keyspace " + keyspaceName;
		createReplicationStrategy(metadata);
		for (CFMetaData cfm : metadata.tablesAndViews()) {
			Keyspace.logger.trace("Initializing {}.{}", getName(), cfm.cfName);
			initCf(cfm, loadSSTables);
		}
		this.viewManager.reload();
		metric = null;
	}

	private Keyspace(KeyspaceMetadata metadata) {
		this.metadata = metadata;
		createReplicationStrategy(metadata);
		metric = null;
	}

	public static Keyspace mockKS(KeyspaceMetadata metadata) {
		return new Keyspace(metadata);
	}

	private void createReplicationStrategy(KeyspaceMetadata ksm) {
		replicationStrategy = AbstractReplicationStrategy.createReplicationStrategy(ksm.name, ksm.params.replication.klass, StorageService.instance.getTokenMetadata(), DatabaseDescriptor.getEndpointSnitch(), ksm.params.replication.options);
		if (!(ksm.params.replication.equals(replicationParams))) {
			Keyspace.logger.debug("New replication settings for keyspace {} - invalidating disk boundary caches", ksm.name);
			columnFamilyStores.values().forEach(ColumnFamilyStore::invalidateDiskBoundaries);
		}
		replicationParams = ksm.params.replication;
	}

	public void dropCf(UUID cfId) {
		assert columnFamilyStores.containsKey(cfId);
		ColumnFamilyStore cfs = columnFamilyStores.remove(cfId);
		if (cfs == null)
			return;

		cfs.getCompactionStrategyManager().shutdown();
		CompactionManager.instance.interruptCompactionForCFs(cfs.concatWithIndexes(), true);
		Keyspace.writeOrder.awaitNewBarrier();
		cfs.readOrdering.awaitNewBarrier();
		unloadCf(cfs);
	}

	private void unloadCf(ColumnFamilyStore cfs) {
		cfs.forceBlockingFlush();
		cfs.invalidate();
	}

	public void initCfCustom(ColumnFamilyStore newCfs) {
		ColumnFamilyStore cfs = columnFamilyStores.get(newCfs.metadata.cfId);
		if (cfs == null) {
			ColumnFamilyStore oldCfs = columnFamilyStores.putIfAbsent(newCfs.metadata.cfId, newCfs);
			if (oldCfs != null)
				throw new IllegalStateException(("added multiple mappings for cf id " + (newCfs.metadata.cfId)));

		}else {
			throw new IllegalStateException(("CFS is already initialized: " + (cfs.name)));
		}
	}

	public void initCf(CFMetaData metadata, boolean loadSSTables) {
		ColumnFamilyStore cfs = columnFamilyStores.get(metadata.cfId);
		if (cfs == null) {
		}else {
			assert cfs.name.equals(metadata.cfName);
			cfs.reload();
		}
	}

	public CompletableFuture<?> applyFuture(Mutation mutation, boolean writeCommitLog, boolean updateIndexes) {
		return applyInternal(mutation, writeCommitLog, updateIndexes, true, true, new CompletableFuture<>());
	}

	public CompletableFuture<?> applyFuture(Mutation mutation, boolean writeCommitLog, boolean updateIndexes, boolean isDroppable, boolean isDeferrable) {
		return applyInternal(mutation, writeCommitLog, updateIndexes, isDroppable, isDeferrable, new CompletableFuture<>());
	}

	public void apply(Mutation mutation, boolean writeCommitLog, boolean updateIndexes) {
		apply(mutation, writeCommitLog, updateIndexes, true);
	}

	public void apply(final Mutation mutation, final boolean writeCommitLog) {
		apply(mutation, writeCommitLog, true, true);
	}

	public void apply(final Mutation mutation, final boolean writeCommitLog, boolean updateIndexes, boolean isDroppable) {
		applyInternal(mutation, writeCommitLog, updateIndexes, isDroppable, false, null);
	}

	private CompletableFuture<?> applyInternal(final Mutation mutation, final boolean writeCommitLog, boolean updateIndexes, boolean isDroppable, boolean isDeferrable, CompletableFuture<?> future) {
		if ((Keyspace.TEST_FAIL_WRITES) && (metadata.name.equals(Keyspace.TEST_FAIL_WRITES_KS)))
			throw new RuntimeException("Testing write failures");

		Lock[] locks = null;
		boolean requiresViewUpdate = updateIndexes && (viewManager.updatesAffectView(Collections.singleton(mutation), false));
		if (requiresViewUpdate) {
			mutation.viewLockAcquireStart.compareAndSet(0L, System.currentTimeMillis());
			Collection<UUID> columnFamilyIds = mutation.getColumnFamilyIds();
			Iterator<UUID> idIterator = columnFamilyIds.iterator();
			locks = new Lock[columnFamilyIds.size()];
			for (int i = 0; i < (columnFamilyIds.size()); i++) {
				UUID cfid = idIterator.next();
				int lockKey = Objects.hash(mutation.key().getKey(), cfid);
				while (true) {
					Lock lock = null;
					if ((Keyspace.TEST_FAIL_MV_LOCKS_COUNT) == 0)
						lock = ViewManager.acquireLockFor(lockKey);
					else
						(Keyspace.TEST_FAIL_MV_LOCKS_COUNT)--;

					if (lock == null) {
						if (isDroppable && (((System.currentTimeMillis()) - (mutation.createdAt)) > (DatabaseDescriptor.getWriteRpcTimeout()))) {
							for (int j = 0; j < i; j++)
								locks[j].unlock();

							Keyspace.logger.trace("Could not acquire lock for {} and table {}", ByteBufferUtil.bytesToHex(mutation.key().getKey()), columnFamilyStores.get(cfid).name);
							Tracing.trace("Could not acquire MV lock");
							if (future != null) {
								future.completeExceptionally(new WriteTimeoutException(WriteType.VIEW, ConsistencyLevel.LOCAL_ONE, 0, 1));
								return future;
							}else
								throw new WriteTimeoutException(WriteType.VIEW, ConsistencyLevel.LOCAL_ONE, 0, 1);

						}else
							if (isDeferrable) {
								for (int j = 0; j < i; j++)
									locks[j].unlock();

								final CompletableFuture<?> mark = future;
								StageManager.getStage(Stage.MUTATION).execute(() -> applyInternal(mutation, writeCommitLog, true, isDroppable, true, mark));
								return future;
							}else {
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
								}
								continue;
							}

					}else {
						locks[i] = lock;
					}
					break;
				} 
			}
			long acquireTime = (System.currentTimeMillis()) - (mutation.viewLockAcquireStart.get());
			if (isDroppable) {
				for (UUID cfid : columnFamilyIds)
					columnFamilyStores.get(cfid).metric.viewLockAcquireTime.update(acquireTime, TimeUnit.MILLISECONDS);

			}
		}
		int nowInSec = FBUtilities.nowInSeconds();
		try (OpOrder.Group opGroup = Keyspace.writeOrder.start()) {
			CommitLogPosition commitLogPosition = null;
			if (writeCommitLog) {
				Tracing.trace("Appending to commitlog");
				commitLogPosition = CommitLog.instance.add(mutation);
			}
			for (PartitionUpdate upd : mutation.getPartitionUpdates()) {
				ColumnFamilyStore cfs = columnFamilyStores.get(upd.metadata().cfId);
				if (cfs == null) {
					Keyspace.logger.error("Attempting to mutate non-existant table {} ({}.{})", upd.metadata().cfId, upd.metadata().ksName, upd.metadata().cfName);
					continue;
				}
				AtomicLong baseComplete = new AtomicLong(Long.MAX_VALUE);
				if (requiresViewUpdate) {
					try {
						Tracing.trace("Creating materialized view mutations from base table replica");
						viewManager.forTable(upd.metadata()).pushViewReplicaUpdates(upd, writeCommitLog, baseComplete);
					} catch (Throwable t) {
						JVMStabilityInspector.inspectThrowable(t);
						Keyspace.logger.error(String.format("Unknown exception caught while attempting to update MaterializedView! %s.%s", upd.metadata().ksName, upd.metadata().cfName), t);
						throw t;
					}
				}
				Tracing.trace("Adding to {} memtable", upd.metadata().cfName);
				UpdateTransaction indexTransaction = (updateIndexes) ? cfs.indexManager.newUpdateTransaction(upd, opGroup, nowInSec) : UpdateTransaction.NO_OP;
				cfs.apply(upd, indexTransaction, opGroup, commitLogPosition);
				if (requiresViewUpdate)
					baseComplete.set(System.currentTimeMillis());

			}
			if (future != null) {
				future.complete(null);
			}
			return future;
		} finally {
			if (locks != null) {
				for (Lock lock : locks)
					if (lock != null)
						lock.unlock();


			}
		}
	}

	public AbstractReplicationStrategy getReplicationStrategy() {
		return replicationStrategy;
	}

	public List<Future<?>> flush() {
		List<Future<?>> futures = new ArrayList<>(columnFamilyStores.size());
		for (ColumnFamilyStore cfs : columnFamilyStores.values())
			futures.add(cfs.forceFlush());

		return futures;
	}

	public Iterable<ColumnFamilyStore> getValidColumnFamilies(boolean allowIndexes, boolean autoAddIndexes, String... cfNames) throws IOException {
		Set<ColumnFamilyStore> valid = new HashSet<>();
		if ((cfNames.length) == 0) {
			for (ColumnFamilyStore cfStore : getColumnFamilyStores()) {
				valid.add(cfStore);
				if (autoAddIndexes)
					valid.addAll(getIndexColumnFamilyStores(cfStore));

			}
			return valid;
		}
		for (String cfName : cfNames) {
			if (SecondaryIndexManager.isIndexColumnFamily(cfName)) {
				if (!allowIndexes) {
					Keyspace.logger.warn("Operation not allowed on secondary Index table ({})", cfName);
					continue;
				}
				String baseName = SecondaryIndexManager.getParentCfsName(cfName);
				String indexName = SecondaryIndexManager.getIndexName(cfName);
				ColumnFamilyStore baseCfs = getColumnFamilyStore(baseName);
				Index index = baseCfs.indexManager.getIndexByName(indexName);
				if (index == null)
					throw new IllegalArgumentException(String.format("Invalid index specified: %s/%s.", baseCfs.metadata.cfName, indexName));

				if (index.getBackingTable().isPresent())
					valid.add(index.getBackingTable().get());

			}else {
				ColumnFamilyStore cfStore = getColumnFamilyStore(cfName);
				valid.add(cfStore);
				if (autoAddIndexes)
					valid.addAll(getIndexColumnFamilyStores(cfStore));

			}
		}
		return valid;
	}

	private Set<ColumnFamilyStore> getIndexColumnFamilyStores(ColumnFamilyStore baseCfs) {
		Set<ColumnFamilyStore> stores = new HashSet<>();
		for (ColumnFamilyStore indexCfs : baseCfs.indexManager.getAllIndexColumnFamilyStores()) {
			Keyspace.logger.info("adding secondary index table {} to operation", indexCfs.metadata.cfName);
			stores.add(indexCfs);
		}
		return stores;
	}

	public static Iterable<Keyspace> all() {
		return Iterables.transform(Schema.instance.getKeyspaces(), Keyspace.keyspaceTransformer);
	}

	public static Iterable<Keyspace> nonSystem() {
		return Iterables.transform(Schema.instance.getNonSystemKeyspaces(), Keyspace.keyspaceTransformer);
	}

	public static Iterable<Keyspace> nonLocalStrategy() {
		return Iterables.transform(Schema.instance.getNonLocalStrategyKeyspaces(), Keyspace.keyspaceTransformer);
	}

	public static Iterable<Keyspace> system() {
		return Iterables.transform(SchemaConstants.LOCAL_SYSTEM_KEYSPACE_NAMES, Keyspace.keyspaceTransformer);
	}

	@Override
	public String toString() {
		return (((getClass().getSimpleName()) + "(name='") + (getName())) + "')";
	}

	public String getName() {
		return metadata.name;
	}
}

