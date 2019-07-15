

import com.codahale.metrics.Counter;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.cassandra.cache.AutoSavingCache;
import org.apache.cassandra.cache.IRowCacheEntry;
import org.apache.cassandra.cache.InstrumentingCache;
import org.apache.cassandra.cache.RowCacheKey;
import org.apache.cassandra.cache.RowCacheSentinel;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Columns;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.DataRange;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.EmptyIterators;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.Memtable;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.ReadQuery;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.StorageHook;
import org.apache.cassandra.db.compaction.CompactionStrategyManager;
import org.apache.cassandra.db.filter.AbstractClusteringIndexFilter;
import org.apache.cassandra.db.filter.ClusteringIndexFilter;
import org.apache.cassandra.db.filter.ClusteringIndexNamesFilter;
import org.apache.cassandra.db.filter.ClusteringIndexSliceFilter;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.filter.DataLimits;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.db.lifecycle.View;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.CachedBTreePartition;
import org.apache.cassandra.db.partitions.CachedPartition;
import org.apache.cassandra.db.partitions.ImmutableBTreePartition;
import org.apache.cassandra.db.partitions.Partition;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.partitions.SingletonUnfilteredPartitionIterator;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterators;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIteratorWithLowerBound;
import org.apache.cassandra.db.rows.UnfilteredRowIterators;
import org.apache.cassandra.db.rows.WrappingUnfilteredRowIterator;
import org.apache.cassandra.db.transform.Transformation;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.SSTableReadsListener;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.metrics.LatencyMetrics;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.CachingParams;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.service.CacheService;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.pager.PagingState;
import org.apache.cassandra.service.pager.QueryPager;
import org.apache.cassandra.service.pager.SinglePartitionPager;
import org.apache.cassandra.thrift.ThriftResultsMerger;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.transport.ProtocolVersion;
import org.apache.cassandra.utils.CloseableIterator;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.SearchIterator;
import org.apache.cassandra.utils.TopKSampler;
import org.apache.cassandra.utils.btree.BTreeSet;

import static org.apache.cassandra.metrics.TableMetrics.Sampler.READS;
import static org.apache.cassandra.net.MessagingService.Verb.READ;
import static org.apache.commons.lang3.tuple.Pair.of;


public class SinglePartitionReadCommand extends ReadCommand {
	protected static final ReadCommand.SelectionDeserializer selectionDeserializer = new SinglePartitionReadCommand.Deserializer();

	private final DecoratedKey partitionKey;

	private final ClusteringIndexFilter clusteringIndexFilter;

	private int oldestUnrepairedTombstone = Integer.MAX_VALUE;

	public static SinglePartitionReadCommand create(boolean isForThrift, CFMetaData metadata, int nowInSec, ColumnFilter columnFilter, RowFilter rowFilter, DataLimits limits, DecoratedKey partitionKey, ClusteringIndexFilter clusteringIndexFilter, IndexMetadata indexMetadata) {
		return null;
	}

	public static SinglePartitionReadCommand create(CFMetaData metadata, int nowInSec, ColumnFilter columnFilter, RowFilter rowFilter, DataLimits limits, DecoratedKey partitionKey, ClusteringIndexFilter clusteringIndexFilter) {
		return SinglePartitionReadCommand.create(false, metadata, nowInSec, columnFilter, rowFilter, limits, partitionKey, clusteringIndexFilter);
	}

	public static SinglePartitionReadCommand create(boolean isForThrift, CFMetaData metadata, int nowInSec, ColumnFilter columnFilter, RowFilter rowFilter, DataLimits limits, DecoratedKey partitionKey, ClusteringIndexFilter clusteringIndexFilter) {
		return null;
	}

	public static SinglePartitionReadCommand create(CFMetaData metadata, int nowInSec, DecoratedKey key, ColumnFilter columnFilter, ClusteringIndexFilter filter) {
		return SinglePartitionReadCommand.create(metadata, nowInSec, columnFilter, RowFilter.NONE, DataLimits.NONE, key, filter);
	}

	public static SinglePartitionReadCommand fullPartitionRead(CFMetaData metadata, int nowInSec, DecoratedKey key) {
		return SinglePartitionReadCommand.create(metadata, nowInSec, key, Slices.ALL);
	}

	public static SinglePartitionReadCommand fullPartitionRead(CFMetaData metadata, int nowInSec, ByteBuffer key) {
		return SinglePartitionReadCommand.create(metadata, nowInSec, metadata.decorateKey(key), Slices.ALL);
	}

	public static SinglePartitionReadCommand create(CFMetaData metadata, int nowInSec, DecoratedKey key, Slice slice) {
		return SinglePartitionReadCommand.create(metadata, nowInSec, key, Slices.with(metadata.comparator, slice));
	}

	public static SinglePartitionReadCommand create(CFMetaData metadata, int nowInSec, DecoratedKey key, Slices slices) {
		ClusteringIndexSliceFilter filter = new ClusteringIndexSliceFilter(slices, false);
		return SinglePartitionReadCommand.create(metadata, nowInSec, ColumnFilter.all(metadata), RowFilter.NONE, DataLimits.NONE, key, filter);
	}

	public static SinglePartitionReadCommand create(CFMetaData metadata, int nowInSec, ByteBuffer key, Slices slices) {
		return SinglePartitionReadCommand.create(metadata, nowInSec, metadata.decorateKey(key), slices);
	}

	public static SinglePartitionReadCommand create(CFMetaData metadata, int nowInSec, DecoratedKey key, NavigableSet<Clustering> names) {
		ClusteringIndexNamesFilter filter = new ClusteringIndexNamesFilter(names, false);
		return SinglePartitionReadCommand.create(metadata, nowInSec, ColumnFilter.all(metadata), RowFilter.NONE, DataLimits.NONE, key, filter);
	}

	public static SinglePartitionReadCommand create(CFMetaData metadata, int nowInSec, DecoratedKey key, Clustering name) {
		return SinglePartitionReadCommand.create(metadata, nowInSec, key, FBUtilities.singleton(name, metadata.comparator));
	}

	public SinglePartitionReadCommand copy() {
		return null;
	}

	public SinglePartitionReadCommand copyAsDigestQuery() {
		return null;
	}

	public SinglePartitionReadCommand withUpdatedLimit(DataLimits newLimits) {
		return null;
	}

	public SinglePartitionReadCommand withUpdatedClusteringIndexFilter(ClusteringIndexFilter filter) {
		return null;
	}

	static SinglePartitionReadCommand legacySliceCommand(boolean isDigest, int digestVersion, CFMetaData metadata, int nowInSec, ColumnFilter columnFilter, DataLimits limits, DecoratedKey partitionKey, ClusteringIndexSliceFilter filter) {
		return null;
	}

	static SinglePartitionReadCommand legacyNamesCommand(boolean isDigest, int digestVersion, CFMetaData metadata, int nowInSec, ColumnFilter columnFilter, DecoratedKey partitionKey, ClusteringIndexNamesFilter filter) {
		return null;
	}

	public DecoratedKey partitionKey() {
		return partitionKey;
	}

	public ClusteringIndexFilter clusteringIndexFilter() {
		return clusteringIndexFilter;
	}

	public ClusteringIndexFilter clusteringIndexFilter(DecoratedKey key) {
		return clusteringIndexFilter;
	}

	public long getTimeout() {
		return DatabaseDescriptor.getReadRpcTimeout();
	}

	public boolean selectsKey(DecoratedKey key) {
		if (!(this.partitionKey().equals(key)))
			return false;

		return rowFilter().partitionKeyRestrictionsAreSatisfiedBy(key, metadata().getKeyValidator());
	}

	public boolean selectsClustering(DecoratedKey key, Clustering clustering) {
		if (clustering == (Clustering.STATIC_CLUSTERING))
			return !(columnFilter().fetchedColumns().statics.isEmpty());

		if (!(clusteringIndexFilter().selects(clustering)))
			return false;

		return rowFilter().clusteringKeyRestrictionsAreSatisfiedBy(clustering);
	}

	public SinglePartitionReadCommand forPaging(Clustering lastReturned, DataLimits limits) {
		assert !(isDigestQuery());
		return SinglePartitionReadCommand.create(isForThrift(), metadata(), nowInSec(), columnFilter(), rowFilter(), limits, partitionKey(), (lastReturned == null ? clusteringIndexFilter() : clusteringIndexFilter.forPaging(metadata().comparator, lastReturned, false)));
	}

	public PartitionIterator execute(ConsistencyLevel consistency, ClientState clientState, long queryStartNanoTime) throws RequestExecutionException {
		return null;
	}

	public SinglePartitionPager getPager(PagingState pagingState, ProtocolVersion protocolVersion) {
		return SinglePartitionReadCommand.getPager(this, pagingState, protocolVersion);
	}

	private static SinglePartitionPager getPager(SinglePartitionReadCommand command, PagingState pagingState, ProtocolVersion protocolVersion) {
		return null;
	}

	protected void recordLatency(TableMetrics metric, long latencyNanos) {
		metric.readLatency.addNano(latencyNanos);
	}

	@SuppressWarnings("resource")
	protected UnfilteredPartitionIterator queryStorage(final ColumnFamilyStore cfs, ReadExecutionController executionController) {
		UnfilteredRowIterator partition = (cfs.isRowCacheEnabled()) ? getThroughCache(cfs, executionController) : queryMemtableAndDisk(cfs, executionController);
		return new SingletonUnfilteredPartitionIterator(partition, isForThrift());
	}

	private UnfilteredRowIterator getThroughCache(ColumnFamilyStore cfs, ReadExecutionController executionController) {
		assert !(cfs.isIndex());
		assert cfs.isRowCacheEnabled() : String.format("Row cache is not enabled on table [%s]", cfs.name);
		RowCacheKey key = new RowCacheKey(metadata().ksAndCFName, partitionKey());
		IRowCacheEntry cached = CacheService.instance.rowCache.get(key);
		if (cached != null) {
			if (cached instanceof RowCacheSentinel) {
				Tracing.trace("Row cache miss (race)");
				cfs.metric.rowCacheMiss.inc();
				return queryMemtableAndDisk(cfs, executionController);
			}
			CachedPartition cachedPartition = ((CachedPartition) (cached));
			if (cfs.isFilterFullyCoveredBy(clusteringIndexFilter(), limits(), cachedPartition, nowInSec())) {
				cfs.metric.rowCacheHit.inc();
				Tracing.trace("Row cache hit");
				UnfilteredRowIterator unfilteredRowIterator = clusteringIndexFilter().getUnfilteredRowIterator(columnFilter(), cachedPartition);
				cfs.metric.updateSSTableIterated(0);
				return unfilteredRowIterator;
			}
			cfs.metric.rowCacheHitOutOfRange.inc();
			Tracing.trace("Ignoring row cache as cached value could not satisfy query");
			return queryMemtableAndDisk(cfs, executionController);
		}
		cfs.metric.rowCacheMiss.inc();
		Tracing.trace("Row cache miss");
		boolean cacheFullPartitions = ((metadata().clusteringColumns().size()) > 0) ? metadata().params.caching.cacheAllRows() : metadata().params.caching.cacheRows();
		if (cacheFullPartitions || (clusteringIndexFilter().isHeadFilter())) {
			RowCacheSentinel sentinel = new RowCacheSentinel();
			boolean sentinelSuccess = CacheService.instance.rowCache.putIfAbsent(key, sentinel);
			boolean sentinelReplaced = false;
			try {
				final int rowsToCache = metadata().params.caching.rowsPerPartitionToCache();
				final boolean enforceStrictLiveness = metadata().enforceStrictLiveness();
				@SuppressWarnings("resource")
				UnfilteredRowIterator iter = SinglePartitionReadCommand.fullPartitionRead(metadata(), nowInSec(), partitionKey()).queryMemtableAndDisk(cfs, executionController);
				try {
					UnfilteredRowIterator toCacheIterator = new WrappingUnfilteredRowIterator(iter) {
						private int rowsCounted = 0;

						@Override
						public boolean hasNext() {
							return ((rowsCounted) < rowsToCache) && (super.hasNext());
						}

						@Override
						public Unfiltered next() {
							Unfiltered unfiltered = super.next();
							if (unfiltered.isRow()) {
								Row row = ((Row) (unfiltered));
								if (row.hasLiveData(nowInSec(), enforceStrictLiveness))
									(rowsCounted)++;

							}
							return unfiltered;
						}
					};
					CachedPartition toCache = CachedBTreePartition.create(toCacheIterator, nowInSec());
					if (sentinelSuccess && (!(toCache.isEmpty()))) {
						Tracing.trace("Caching {} rows", toCache.rowCount());
						CacheService.instance.rowCache.replace(key, sentinel, toCache);
						sentinelReplaced = true;
					}
					UnfilteredRowIterator cacheIterator = clusteringIndexFilter().getUnfilteredRowIterator(columnFilter(), toCache);
					if (cacheFullPartitions) {
						assert !(iter.hasNext());
						iter.close();
						return cacheIterator;
					}
					return UnfilteredRowIterators.concat(cacheIterator, clusteringIndexFilter().filterNotIndexed(columnFilter(), iter));
				} catch (RuntimeException | Error e) {
					iter.close();
					throw e;
				}
			} finally {
				if (sentinelSuccess && (!sentinelReplaced))
					cfs.invalidateCachedPartition(key);

			}
		}
		Tracing.trace("Fetching data but not populating cache as query does not query from the start of the partition");
		return queryMemtableAndDisk(cfs, executionController);
	}

	public UnfilteredRowIterator queryMemtableAndDisk(ColumnFamilyStore cfs, ReadExecutionController executionController) {
		assert (executionController != null) && (executionController.validForReadOn(cfs));
		Tracing.trace("Executing single-partition query on {}", cfs.name);
		return queryMemtableAndDiskInternal(cfs);
	}

	@Override
	protected int oldestUnrepairedTombstone() {
		return oldestUnrepairedTombstone;
	}

	private UnfilteredRowIterator queryMemtableAndDiskInternal(ColumnFamilyStore cfs) {
		if (((clusteringIndexFilter()) instanceof ClusteringIndexNamesFilter) && (!(queriesMulticellType())))
			return queryMemtableAndSSTablesInTimestampOrder(cfs, ((ClusteringIndexNamesFilter) (clusteringIndexFilter())));

		Tracing.trace("Acquiring sstable references");
		ColumnFamilyStore.ViewFragment view = cfs.select(View.select(SSTableSet.LIVE, partitionKey()));
		List<UnfilteredRowIterator> iterators = new ArrayList<>(((Iterables.size(view.memtables)) + (view.sstables.size())));
		ClusteringIndexFilter filter = clusteringIndexFilter();
		long minTimestamp = Long.MAX_VALUE;
		try {
			for (Memtable memtable : view.memtables) {
				Partition partition = memtable.getPartition(partitionKey());
				if (partition == null)
					continue;

				minTimestamp = Math.min(minTimestamp, memtable.getMinTimestamp());
				@SuppressWarnings("resource")
				UnfilteredRowIterator iter = filter.getUnfilteredRowIterator(columnFilter(), partition);
				oldestUnrepairedTombstone = Math.min(oldestUnrepairedTombstone, partition.stats().minLocalDeletionTime);
				iterators.add((isForThrift() ? ThriftResultsMerger.maybeWrap(iter, nowInSec()) : iter));
			}
			Collections.sort(view.sstables, SSTableReader.maxTimestampComparator);
			long mostRecentPartitionTombstone = Long.MIN_VALUE;
			int nonIntersectingSSTables = 0;
			List<SSTableReader> skippedSSTablesWithTombstones = null;
			SinglePartitionReadCommand.SSTableReadMetricsCollector metricsCollector = new SinglePartitionReadCommand.SSTableReadMetricsCollector();
			for (SSTableReader sstable : view.sstables) {
				if ((sstable.getMaxTimestamp()) < mostRecentPartitionTombstone)
					break;

				if (!(shouldInclude(sstable))) {
					nonIntersectingSSTables++;
					if (sstable.mayHaveTombstones()) {
						if (skippedSSTablesWithTombstones == null)
							skippedSSTablesWithTombstones = new ArrayList<>();

						skippedSSTablesWithTombstones.add(sstable);
					}
					continue;
				}
				minTimestamp = Math.min(minTimestamp, sstable.getMinTimestamp());
				@SuppressWarnings("resource")
				UnfilteredRowIteratorWithLowerBound iter = makeIterator(cfs, sstable, true, metricsCollector);
				if (!(sstable.isRepaired()))
					oldestUnrepairedTombstone = Math.min(oldestUnrepairedTombstone, sstable.getMinLocalDeletionTime());

				iterators.add(iter);
				mostRecentPartitionTombstone = Math.max(mostRecentPartitionTombstone, iter.partitionLevelDeletion().markedForDeleteAt());
			}
			int includedDueToTombstones = 0;
			if (skippedSSTablesWithTombstones != null) {
				for (SSTableReader sstable : skippedSSTablesWithTombstones) {
					if ((sstable.getMaxTimestamp()) <= minTimestamp)
						continue;

					@SuppressWarnings("resource")
					UnfilteredRowIteratorWithLowerBound iter = makeIterator(cfs, sstable, false, metricsCollector);
					if (!(sstable.isRepaired()))
						oldestUnrepairedTombstone = Math.min(oldestUnrepairedTombstone, sstable.getMinLocalDeletionTime());

					iterators.add(iter);
					includedDueToTombstones++;
				}
			}
			if (Tracing.isTracing())
				Tracing.trace("Skipped {}/{} non-slice-intersecting sstables, included {} due to tombstones", nonIntersectingSSTables, view.sstables.size(), includedDueToTombstones);

			if (iterators.isEmpty())
				return EmptyIterators.unfilteredRow(cfs.metadata, partitionKey(), filter.isReversed());

			StorageHook.instance.reportRead(cfs.metadata.cfId, partitionKey());
			return withSSTablesIterated(iterators, cfs.metric, metricsCollector);
		} catch (RuntimeException | Error e) {
			try {
				FBUtilities.closeAll(iterators);
			} catch (Exception suppressed) {
				e.addSuppressed(suppressed);
			}
			throw e;
		}
	}

	private boolean shouldInclude(SSTableReader sstable) {
		if (!(columnFilter().fetchedColumns().statics.isEmpty()))
			return true;

		return clusteringIndexFilter().shouldInclude(sstable);
	}

	private UnfilteredRowIteratorWithLowerBound makeIterator(ColumnFamilyStore cfs, final SSTableReader sstable, boolean applyThriftTransformation, SSTableReadsListener listener) {
		return StorageHook.instance.makeRowIteratorWithLowerBound(cfs, partitionKey(), sstable, clusteringIndexFilter(), columnFilter(), isForThrift(), nowInSec(), applyThriftTransformation, listener);
	}

	private UnfilteredRowIterator withSSTablesIterated(List<UnfilteredRowIterator> iterators, TableMetrics metrics, SinglePartitionReadCommand.SSTableReadMetricsCollector metricsCollector) {
		@SuppressWarnings("resource")
		UnfilteredRowIterator merged = UnfilteredRowIterators.merge(iterators, nowInSec());
		if (!(merged.isEmpty())) {
			DecoratedKey key = merged.partitionKey();
			metrics.samplers.get(READS).addSample(key.getKey(), key.hashCode(), 1);
		}
		class UpdateSstablesIterated extends Transformation {
			public void onPartitionClose() {
				int mergedSSTablesIterated = metricsCollector.getMergedSSTables();
				metrics.updateSSTableIterated(mergedSSTablesIterated);
				Tracing.trace("Merged data from memtables and {} sstables", mergedSSTablesIterated);
			}
		}
		return Transformation.apply(merged, new UpdateSstablesIterated());
	}

	private boolean queriesMulticellType() {
		for (ColumnDefinition column : columnFilter().fetchedColumns()) {
			if ((column.type.isMultiCell()) || (column.type.isCounter()))
				return true;

		}
		return false;
	}

	private UnfilteredRowIterator queryMemtableAndSSTablesInTimestampOrder(ColumnFamilyStore cfs, ClusteringIndexNamesFilter filter) {
		Tracing.trace("Acquiring sstable references");
		ColumnFamilyStore.ViewFragment view = cfs.select(View.select(SSTableSet.LIVE, partitionKey()));
		ImmutableBTreePartition result = null;
		Tracing.trace("Merging memtable contents");
		for (Memtable memtable : view.memtables) {
			Partition partition = memtable.getPartition(partitionKey());
			if (partition == null)
				continue;

			try (final UnfilteredRowIterator iter = filter.getUnfilteredRowIterator(columnFilter(), partition)) {
				if (iter.isEmpty())
					continue;

				result = add((isForThrift() ? ThriftResultsMerger.maybeWrap(iter, nowInSec()) : iter), result, filter, false);
			}
		}
		Collections.sort(view.sstables, SSTableReader.maxTimestampComparator);
		boolean onlyUnrepaired = true;
		SinglePartitionReadCommand.SSTableReadMetricsCollector metricsCollector = new SinglePartitionReadCommand.SSTableReadMetricsCollector();
		for (SSTableReader sstable : view.sstables) {
			if ((result != null) && ((sstable.getMaxTimestamp()) < (result.partitionLevelDeletion().markedForDeleteAt())))
				break;

			long currentMaxTs = sstable.getMaxTimestamp();
			filter = reduceFilter(filter, result, currentMaxTs);
			if (filter == null)
				break;

			if (!(shouldInclude(sstable))) {
				if (!(sstable.mayHaveTombstones()))
					continue;

				try (final UnfilteredRowIterator iter = StorageHook.instance.makeRowIterator(cfs, sstable, partitionKey(), filter.getSlices(metadata()), columnFilter(), filter.isReversed(), isForThrift(), metricsCollector)) {
					if (!(iter.partitionLevelDeletion().isLive()))
						result = add(UnfilteredRowIterators.noRowsIterator(iter.metadata(), iter.partitionKey(), Rows.EMPTY_STATIC_ROW, iter.partitionLevelDeletion(), filter.isReversed()), result, filter, sstable.isRepaired());
					else
						result = add(iter, result, filter, sstable.isRepaired());

				}
				continue;
			}
			try (final UnfilteredRowIterator iter = StorageHook.instance.makeRowIterator(cfs, sstable, partitionKey(), filter.getSlices(metadata()), columnFilter(), filter.isReversed(), isForThrift(), metricsCollector)) {
				if (iter.isEmpty())
					continue;

				if (sstable.isRepaired())
					onlyUnrepaired = false;

				result = add((isForThrift() ? ThriftResultsMerger.maybeWrap(iter, nowInSec()) : iter), result, filter, sstable.isRepaired());
			}
		}
		cfs.metric.updateSSTableIterated(metricsCollector.getMergedSSTables());
		if ((result == null) || (result.isEmpty()))
			return EmptyIterators.unfilteredRow(metadata(), partitionKey(), false);

		DecoratedKey key = result.partitionKey();
		cfs.metric.samplers.get(READS).addSample(key.getKey(), key.hashCode(), 1);
		StorageHook.instance.reportRead(cfs.metadata.cfId, partitionKey());
		if (((((metricsCollector.getMergedSSTables()) > (cfs.getMinimumCompactionThreshold())) && onlyUnrepaired) && (!(cfs.isAutoCompactionDisabled()))) && (cfs.getCompactionStrategyManager().shouldDefragment())) {
			Tracing.trace("Defragmenting requested data");
			try (final UnfilteredRowIterator iter = result.unfilteredIterator(columnFilter(), Slices.ALL, false)) {
				final Mutation mutation = new Mutation(PartitionUpdate.fromIterator(iter, columnFilter()));
				StageManager.getStage(Stage.MUTATION).execute(() -> {
					Keyspace.open(mutation.getKeyspaceName()).apply(mutation, false, false);
				});
			}
		}
		return result.unfilteredIterator(columnFilter(), Slices.ALL, clusteringIndexFilter().isReversed());
	}

	private ImmutableBTreePartition add(UnfilteredRowIterator iter, ImmutableBTreePartition result, ClusteringIndexNamesFilter filter, boolean isRepaired) {
		if (!isRepaired)
			oldestUnrepairedTombstone = Math.min(oldestUnrepairedTombstone, iter.stats().minLocalDeletionTime);

		int maxRows = Math.max(filter.requestedRows().size(), 1);
		if (result == null)
			return ImmutableBTreePartition.create(iter, maxRows);

		try (final UnfilteredRowIterator merged = UnfilteredRowIterators.merge(Arrays.asList(iter, result.unfilteredIterator(columnFilter(), Slices.ALL, filter.isReversed())), nowInSec())) {
			return ImmutableBTreePartition.create(merged, maxRows);
		}
	}

	private ClusteringIndexNamesFilter reduceFilter(ClusteringIndexNamesFilter filter, Partition result, long sstableTimestamp) {
		if (result == null)
			return filter;

		SearchIterator<Clustering, Row> searchIter = result.searchIterator(columnFilter(), false);
		PartitionColumns columns = columnFilter().fetchedColumns();
		NavigableSet<Clustering> clusterings = filter.requestedRows();
		boolean removeStatic = false;
		if (!(columns.statics.isEmpty())) {
			Row staticRow = searchIter.next(Clustering.STATIC_CLUSTERING);
			removeStatic = (staticRow != null) && (canRemoveRow(staticRow, columns.statics, sstableTimestamp));
		}
		NavigableSet<Clustering> toRemove = null;
		for (Clustering clustering : clusterings) {
			Row row = searchIter.next(clustering);
			if ((row == null) || (!(canRemoveRow(row, columns.regulars, sstableTimestamp))))
				continue;

			if (toRemove == null)
				toRemove = new TreeSet<>(result.metadata().comparator);

			toRemove.add(clustering);
		}
		if ((!removeStatic) && (toRemove == null))
			return filter;

		boolean hasNoMoreStatic = (columns.statics.isEmpty()) || removeStatic;
		boolean hasNoMoreClusterings = (clusterings.isEmpty()) || ((toRemove != null) && ((toRemove.size()) == (clusterings.size())));
		if (hasNoMoreStatic && hasNoMoreClusterings)
			return null;

		if (toRemove != null) {
			BTreeSet.Builder<Clustering> newClusterings = BTreeSet.builder(result.metadata().comparator);
			newClusterings.addAll(Sets.difference(clusterings, toRemove));
			clusterings = newClusterings.build();
		}
		return new ClusteringIndexNamesFilter(clusterings, filter.isReversed());
	}

	private boolean canRemoveRow(Row row, Columns requestedColumns, long sstableTimestamp) {
		if ((row.primaryKeyLivenessInfo().isEmpty()) || ((row.primaryKeyLivenessInfo().timestamp()) <= sstableTimestamp))
			return false;

		for (ColumnDefinition column : requestedColumns) {
			Cell cell = row.getCell(column);
			if ((cell == null) || ((cell.timestamp()) <= sstableTimestamp))
				return false;

		}
		return true;
	}

	@Override
	public boolean selectsFullPartition() {
		return (metadata().isStaticCompactTable()) || ((clusteringIndexFilter.selectsAllPartition()) && (!(rowFilter().hasExpressionOnClusteringOrRegularColumns())));
	}

	@Override
	public String toString() {
		return String.format("Read(%s.%s columns=%s rowFilter=%s limits=%s key=%s filter=%s, nowInSec=%d)", metadata().ksName, metadata().cfName, columnFilter(), rowFilter(), limits(), metadata().getKeyValidator().getString(partitionKey().getKey()), clusteringIndexFilter.toString(metadata()), nowInSec());
	}

	public MessageOut<ReadCommand> createMessage(int version) {
		return new MessageOut<>(READ, this, ReadCommand.readSerializer);
	}

	protected void appendCQLWhereClause(StringBuilder sb) {
		sb.append(" WHERE ");
		sb.append(ColumnDefinition.toCQLString(metadata().partitionKeyColumns())).append(" = ");
		DataRange.appendKeyString(sb, metadata().getKeyValidator(), partitionKey().getKey());
		if (!(rowFilter().isEmpty()))
			sb.append(" AND ").append(rowFilter());

		String filterString = clusteringIndexFilter().toCQLString(metadata());
		if (!(filterString.isEmpty()))
			sb.append(" AND ").append(filterString);

	}

	protected void serializeSelection(DataOutputPlus out, int version) throws IOException {
		metadata().getKeyValidator().writeValue(partitionKey().getKey(), out);
		ClusteringIndexFilter.serializer.serialize(clusteringIndexFilter(), out, version);
	}

	protected long selectionSerializedSize(int version) {
		return (metadata().getKeyValidator().writtenLength(partitionKey().getKey())) + (ClusteringIndexFilter.serializer.serializedSize(clusteringIndexFilter(), version));
	}

	public boolean isLimitedToOnePartition() {
		return true;
	}

	public static class Group implements ReadQuery {
		public final List<SinglePartitionReadCommand> commands;

		private final DataLimits limits;

		private final int nowInSec;

		private final boolean selectsFullPartitions;

		public Group(List<SinglePartitionReadCommand> commands, DataLimits limits) {
			assert !(commands.isEmpty());
			this.commands = commands;
			this.limits = limits;
			SinglePartitionReadCommand firstCommand = commands.get(0);
			this.nowInSec = firstCommand.nowInSec();
			this.selectsFullPartitions = firstCommand.selectsFullPartition();
			for (int i = 1; i < (commands.size()); i++)
				assert (commands.get(i).nowInSec()) == (nowInSec);

		}

		public static SinglePartitionReadCommand.Group one(SinglePartitionReadCommand command) {
			return new SinglePartitionReadCommand.Group(Collections.singletonList(command), command.limits());
		}

		public PartitionIterator execute(ConsistencyLevel consistency, ClientState clientState, long queryStartNanoTime) throws RequestExecutionException {
			return null;
		}

		public int nowInSec() {
			return nowInSec;
		}

		public DataLimits limits() {
			return limits;
		}

		public CFMetaData metadata() {
			return commands.get(0).metadata();
		}

		@Override
		public boolean selectsFullPartition() {
			return selectsFullPartitions;
		}

		public ReadExecutionController executionController() {
			return commands.get(0).executionController();
		}

		public PartitionIterator executeInternal(ReadExecutionController controller) {
			boolean enforceStrictLiveness = commands.get(0).metadata().enforceStrictLiveness();
			return limits.filter(UnfilteredPartitionIterators.filter(executeLocally(controller, false), nowInSec), nowInSec, selectsFullPartitions, enforceStrictLiveness);
		}

		public UnfilteredPartitionIterator executeLocally(ReadExecutionController executionController) {
			return executeLocally(executionController, true);
		}

		private UnfilteredPartitionIterator executeLocally(ReadExecutionController executionController, boolean sort) {
			List<org.apache.commons.lang3.tuple.Pair<DecoratedKey, UnfilteredPartitionIterator>> partitions = new ArrayList<>(commands.size());
			for (SinglePartitionReadCommand cmd : commands)
				partitions.add(of(cmd.partitionKey, cmd.executeLocally(executionController)));

			if (sort)
				Collections.sort(partitions, ( p1, p2) -> p1.getLeft().compareTo(p2.getLeft()));

			return UnfilteredPartitionIterators.concat(partitions.stream().map(( p) -> p.getRight()).collect(Collectors.toList()));
		}

		public QueryPager getPager(PagingState pagingState, ProtocolVersion protocolVersion) {
			if ((commands.size()) == 1)
				return SinglePartitionReadCommand.getPager(commands.get(0), pagingState, protocolVersion);

			return null;
		}

		public boolean selectsKey(DecoratedKey key) {
			return Iterables.any(commands, ( c) -> c.selectsKey(key));
		}

		public boolean selectsClustering(DecoratedKey key, Clustering clustering) {
			return Iterables.any(commands, ( c) -> c.selectsClustering(key, clustering));
		}

		@Override
		public String toString() {
			return commands.toString();
		}
	}

	private static class Deserializer extends ReadCommand.SelectionDeserializer {
		public ReadCommand deserialize(DataInputPlus in, int version, boolean isDigest, int digestVersion, boolean isForThrift, CFMetaData metadata, int nowInSec, ColumnFilter columnFilter, RowFilter rowFilter, DataLimits limits, IndexMetadata index) throws IOException {
			DecoratedKey key = metadata.decorateKey(metadata.getKeyValidator().readValue(in, DatabaseDescriptor.getMaxValueSize()));
			ClusteringIndexFilter filter = ClusteringIndexFilter.serializer.deserialize(in, version, metadata);
			return null;
		}
	}

	private static final class SSTableReadMetricsCollector implements SSTableReadsListener {
		private int mergedSSTables;

		@Override
		public void onSSTableSelected(SSTableReader sstable, RowIndexEntry<?> indexEntry, SSTableReadsListener.SelectionReason reason) {
			sstable.incrementReadCount();
			(mergedSSTables)++;
		}

		public int getMergedSSTables() {
			return mergedSSTables;
		}
	}
}

