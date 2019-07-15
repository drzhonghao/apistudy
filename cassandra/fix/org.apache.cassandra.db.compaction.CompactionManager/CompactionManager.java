

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.util.concurrent.FastThreadLocal;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import org.apache.cassandra.cache.AutoSavingCache;
import org.apache.cassandra.concurrent.DebuggableThreadPoolExecutor;
import org.apache.cassandra.concurrent.JMXEnabledThreadPoolExecutor;
import org.apache.cassandra.concurrent.NamedThreadFactory;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.DiskBoundaries;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.compaction.AbstractCompactionStrategy;
import org.apache.cassandra.db.compaction.AbstractCompactionTask;
import org.apache.cassandra.db.compaction.CompactionController;
import org.apache.cassandra.db.compaction.CompactionInfo;
import org.apache.cassandra.db.compaction.CompactionInterruptedException;
import org.apache.cassandra.db.compaction.CompactionIterator;
import org.apache.cassandra.db.compaction.CompactionManagerMBean;
import org.apache.cassandra.db.compaction.CompactionStrategyManager;
import org.apache.cassandra.db.compaction.CompactionTask;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.compaction.Scrubber;
import org.apache.cassandra.db.compaction.Verifier;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.lifecycle.SSTableIntervalTree;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.db.lifecycle.Tracker;
import org.apache.cassandra.db.lifecycle.View;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.view.ViewBuilder;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.Bounds;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Splitter;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.SecondaryIndexBuilder;
import org.apache.cassandra.index.SecondaryIndexManager;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.ISSTableScanner;
import org.apache.cassandra.io.sstable.IndexSummaryRedistribution;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.SSTableRewriter;
import org.apache.cassandra.io.sstable.SnapshotDeletingTask;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.SSTableWriter;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.io.sstable.metadata.IMetadataSerializer;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;
import org.apache.cassandra.io.sstable.metadata.StatsMetadata;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.metrics.CompactionMetrics;
import org.apache.cassandra.repair.RepairJobDesc;
import org.apache.cassandra.repair.Validator;
import org.apache.cassandra.schema.CompactionParams;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.service.ActiveRepairService;
import org.apache.cassandra.service.CacheService;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.MerkleTree;
import org.apache.cassandra.utils.MerkleTrees;
import org.apache.cassandra.utils.Throwables;
import org.apache.cassandra.utils.UUIDGen;
import org.apache.cassandra.utils.WrappedRunnable;
import org.apache.cassandra.utils.concurrent.Refs;
import org.apache.cassandra.utils.concurrent.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CompactionManager implements CompactionManagerMBean {
	public static final String MBEAN_OBJECT_NAME = "org.apache.cassandra.db:type=CompactionManager";

	private static final Logger logger = LoggerFactory.getLogger(CompactionManager.class);

	public static final CompactionManager instance;

	public static final int NO_GC = Integer.MIN_VALUE;

	public static final int GC_ALL = Integer.MAX_VALUE;

	public static final FastThreadLocal<Boolean> isCompactionManager = new FastThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return false;
		}
	};

	static {
		instance = new CompactionManager();
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			mbs.registerMBean(CompactionManager.instance, new ObjectName(CompactionManager.MBEAN_OBJECT_NAME));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private final CompactionManager.CompactionExecutor executor = new CompactionManager.CompactionExecutor();

	private final CompactionManager.CompactionExecutor validationExecutor = new CompactionManager.ValidationExecutor();

	private static final CompactionManager.CompactionExecutor cacheCleanupExecutor = new CompactionManager.CacheCleanupExecutor();

	private final CompactionMetrics metrics = new CompactionMetrics(executor, validationExecutor);

	@VisibleForTesting
	final Multiset<ColumnFamilyStore> compactingCF = ConcurrentHashMultiset.create();

	private final RateLimiter compactionRateLimiter = RateLimiter.create(Double.MAX_VALUE);

	public RateLimiter getRateLimiter() {
		setRate(DatabaseDescriptor.getCompactionThroughputMbPerSec());
		return compactionRateLimiter;
	}

	public void setRate(final double throughPutMbPerSec) {
		double throughput = (throughPutMbPerSec * 1024.0) * 1024.0;
		if ((throughput == 0) || (StorageService.instance.isBootstrapMode()))
			throughput = Double.MAX_VALUE;

		if ((compactionRateLimiter.getRate()) != throughput)
			compactionRateLimiter.setRate(throughput);

	}

	public List<Future<?>> submitBackground(final ColumnFamilyStore cfs) {
		if (cfs.isAutoCompactionDisabled()) {
			CompactionManager.logger.trace("Autocompaction is disabled");
			return Collections.emptyList();
		}
		int count = compactingCF.count(cfs);
		if ((count > 0) && ((executor.getActiveCount()) >= (executor.getMaximumPoolSize()))) {
			CompactionManager.logger.trace("Background compaction is still running for {}.{} ({} remaining). Skipping", cfs.keyspace.getName(), cfs.name, count);
			return Collections.emptyList();
		}
		CompactionManager.logger.trace("Scheduling a background task check for {}.{} with {}", cfs.keyspace.getName(), cfs.name, cfs.getCompactionStrategyManager().getName());
		List<Future<?>> futures = new ArrayList<>(1);
		Future<?> fut = executor.submitIfRunning(new CompactionManager.BackgroundCompactionCandidate(cfs), "background task");
		if (!(fut.isCancelled()))
			futures.add(fut);
		else
			compactingCF.remove(cfs);

		return futures;
	}

	public boolean isCompacting(Iterable<ColumnFamilyStore> cfses) {
		for (ColumnFamilyStore cfs : cfses)
			if (!(cfs.getTracker().getCompacting().isEmpty()))
				return true;


		return false;
	}

	public void forceShutdown() {
		executor.shutdown();
		validationExecutor.shutdown();
		for (CompactionInfo.Holder compactionHolder : CompactionMetrics.getCompactions()) {
			compactionHolder.stop();
		}
		for (ExecutorService exec : Arrays.asList(executor, validationExecutor)) {
			try {
				if (!(exec.awaitTermination(1, TimeUnit.MINUTES)))
					CompactionManager.logger.warn("Failed to wait for compaction executors shutdown");

			} catch (InterruptedException e) {
				CompactionManager.logger.error("Interrupted while waiting for tasks to be terminated", e);
			}
		}
	}

	public void finishCompactionsAndShutdown(long timeout, TimeUnit unit) throws InterruptedException {
		executor.shutdown();
		executor.awaitTermination(timeout, unit);
	}

	class BackgroundCompactionCandidate implements Runnable {
		private final ColumnFamilyStore cfs;

		BackgroundCompactionCandidate(ColumnFamilyStore cfs) {
			compactingCF.add(cfs);
			this.cfs = cfs;
		}

		public void run() {
			try {
				CompactionManager.logger.trace("Checking {}.{}", cfs.keyspace.getName(), cfs.name);
				if (!(cfs.isValid())) {
					CompactionManager.logger.trace("Aborting compaction for dropped CF");
					return;
				}
				CompactionStrategyManager strategy = cfs.getCompactionStrategyManager();
				AbstractCompactionTask task = strategy.getNextBackgroundTask(CompactionManager.getDefaultGcBefore(cfs, FBUtilities.nowInSeconds()));
				if (task == null) {
					CompactionManager.logger.trace("No tasks available");
					return;
				}
				task.execute(metrics);
			} finally {
				compactingCF.remove(cfs);
			}
			submitBackground(cfs);
		}
	}

	@SuppressWarnings("resource")
	private CompactionManager.AllSSTableOpStatus parallelAllSSTableOperation(final ColumnFamilyStore cfs, final CompactionManager.OneSSTableOperation operation, int jobs, OperationType operationType) throws InterruptedException, ExecutionException {
		List<LifecycleTransaction> transactions = new ArrayList<>();
		try (LifecycleTransaction compacting = cfs.markAllCompacting(operationType)) {
			if (compacting == null)
				return CompactionManager.AllSSTableOpStatus.UNABLE_TO_CANCEL;

			Iterable<SSTableReader> sstables = Lists.newArrayList(operation.filterSSTables(compacting));
			if (Iterables.isEmpty(sstables)) {
				CompactionManager.logger.info("No sstables to {} for {}.{}", operationType.name(), cfs.keyspace.getName(), cfs.name);
				return CompactionManager.AllSSTableOpStatus.SUCCESSFUL;
			}
			List<Future<?>> futures = new ArrayList<>();
			for (final SSTableReader sstable : sstables) {
				final LifecycleTransaction txn = compacting.split(Collections.singleton(sstable));
				transactions.add(txn);
				Callable<Object> callable = new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						operation.execute(txn);
						return this;
					}
				};
				Future<?> fut = executor.submitIfRunning(callable, "paralell sstable operation");
				if (!(fut.isCancelled()))
					futures.add(fut);
				else
					return CompactionManager.AllSSTableOpStatus.ABORTED;

				if ((jobs > 0) && ((futures.size()) == jobs)) {
					FBUtilities.waitOnFutures(futures);
					futures.clear();
				}
			}
			FBUtilities.waitOnFutures(futures);
			assert compacting.originals().isEmpty();
			return CompactionManager.AllSSTableOpStatus.SUCCESSFUL;
		} finally {
			Throwable fail = Throwables.close(null, transactions);
			if (fail != null)
				CompactionManager.logger.error("Failed to cleanup lifecycle transactions {}", fail);

		}
	}

	private static interface OneSSTableOperation {
		Iterable<SSTableReader> filterSSTables(LifecycleTransaction transaction);

		void execute(LifecycleTransaction input) throws IOException;
	}

	public enum AllSSTableOpStatus {

		SUCCESSFUL(0),
		ABORTED(1),
		UNABLE_TO_CANCEL(2);
		public final int statusCode;

		AllSSTableOpStatus(int statusCode) {
			this.statusCode = statusCode;
		}
	}

	public CompactionManager.AllSSTableOpStatus performScrub(final ColumnFamilyStore cfs, final boolean skipCorrupted, final boolean checkData, int jobs) throws InterruptedException, ExecutionException {
		return performScrub(cfs, skipCorrupted, checkData, false, jobs);
	}

	public CompactionManager.AllSSTableOpStatus performScrub(final ColumnFamilyStore cfs, final boolean skipCorrupted, final boolean checkData, final boolean reinsertOverflowedTTL, int jobs) throws InterruptedException, ExecutionException {
		return parallelAllSSTableOperation(cfs, new CompactionManager.OneSSTableOperation() {
			@Override
			public Iterable<SSTableReader> filterSSTables(LifecycleTransaction input) {
				return input.originals();
			}

			@Override
			public void execute(LifecycleTransaction input) throws IOException {
				scrubOne(cfs, input, skipCorrupted, checkData, reinsertOverflowedTTL);
			}
		}, jobs, OperationType.SCRUB);
	}

	public CompactionManager.AllSSTableOpStatus performVerify(final ColumnFamilyStore cfs, final boolean extendedVerify) throws InterruptedException, ExecutionException {
		assert !(cfs.isIndex());
		return parallelAllSSTableOperation(cfs, new CompactionManager.OneSSTableOperation() {
			@Override
			public Iterable<SSTableReader> filterSSTables(LifecycleTransaction input) {
				return input.originals();
			}

			@Override
			public void execute(LifecycleTransaction input) throws IOException {
				verifyOne(cfs, input.onlyOne(), extendedVerify);
			}
		}, 0, OperationType.VERIFY);
	}

	public CompactionManager.AllSSTableOpStatus performSSTableRewrite(final ColumnFamilyStore cfs, final boolean excludeCurrentVersion, int jobs) throws InterruptedException, ExecutionException {
		return parallelAllSSTableOperation(cfs, new CompactionManager.OneSSTableOperation() {
			@Override
			public Iterable<SSTableReader> filterSSTables(LifecycleTransaction transaction) {
				Iterable<SSTableReader> sstables = new ArrayList<>(transaction.originals());
				Iterator<SSTableReader> iter = sstables.iterator();
				while (iter.hasNext()) {
					SSTableReader sstable = iter.next();
					if (excludeCurrentVersion && (sstable.descriptor.version.equals(sstable.descriptor.getFormat().getLatestVersion()))) {
						transaction.cancel(sstable);
						iter.remove();
					}
				} 
				return sstables;
			}

			@Override
			public void execute(LifecycleTransaction txn) {
				AbstractCompactionTask task = cfs.getCompactionStrategyManager().getCompactionTask(txn, CompactionManager.NO_GC, Long.MAX_VALUE);
				task.setUserDefined(true);
				task.setCompactionType(OperationType.UPGRADE_SSTABLES);
				task.execute(metrics);
			}
		}, jobs, OperationType.UPGRADE_SSTABLES);
	}

	public CompactionManager.AllSSTableOpStatus performCleanup(final ColumnFamilyStore cfStore, int jobs) throws InterruptedException, ExecutionException {
		assert !(cfStore.isIndex());
		Keyspace keyspace = cfStore.keyspace;
		if (!(StorageService.instance.isJoined())) {
			CompactionManager.logger.info("Cleanup cannot run before a node has joined the ring");
			return CompactionManager.AllSSTableOpStatus.ABORTED;
		}
		final Collection<Range<Token>> ranges = StorageService.instance.getLocalRanges(keyspace.getName());
		final boolean hasIndexes = cfStore.indexManager.hasIndexes();
		return parallelAllSSTableOperation(cfStore, new CompactionManager.OneSSTableOperation() {
			@Override
			public Iterable<SSTableReader> filterSSTables(LifecycleTransaction transaction) {
				List<SSTableReader> sortedSSTables = Lists.newArrayList(transaction.originals());
				Collections.sort(sortedSSTables, SSTableReader.sizeComparator);
				return sortedSSTables;
			}

			@Override
			public void execute(LifecycleTransaction txn) throws IOException {
				CompactionManager.CleanupStrategy cleanupStrategy = CompactionManager.CleanupStrategy.get(cfStore, ranges, FBUtilities.nowInSeconds());
				doCleanupOne(cfStore, txn, cleanupStrategy, ranges, hasIndexes);
			}
		}, jobs, OperationType.CLEANUP);
	}

	public CompactionManager.AllSSTableOpStatus performGarbageCollection(final ColumnFamilyStore cfStore, CompactionParams.TombstoneOption tombstoneOption, int jobs) throws InterruptedException, ExecutionException {
		assert !(cfStore.isIndex());
		return parallelAllSSTableOperation(cfStore, new CompactionManager.OneSSTableOperation() {
			@Override
			public Iterable<SSTableReader> filterSSTables(LifecycleTransaction transaction) {
				Iterable<SSTableReader> originals = transaction.originals();
				if (cfStore.getCompactionStrategyManager().onlyPurgeRepairedTombstones())
					originals = Iterables.filter(originals, SSTableReader::isRepaired);

				List<SSTableReader> sortedSSTables = Lists.newArrayList(originals);
				Collections.sort(sortedSSTables, SSTableReader.maxTimestampComparator);
				return sortedSSTables;
			}

			@Override
			public void execute(LifecycleTransaction txn) throws IOException {
				CompactionManager.logger.debug("Garbage collecting {}", txn.originals());
				CompactionTask task = new CompactionTask(cfStore, txn, CompactionManager.getDefaultGcBefore(cfStore, FBUtilities.nowInSeconds())) {
					@Override
					protected CompactionController getCompactionController(Set<SSTableReader> toCompact) {
						return new CompactionController(cfStore, toCompact, gcBefore, null, tombstoneOption);
					}
				};
				task.setUserDefined(true);
				task.setCompactionType(OperationType.GARBAGE_COLLECT);
				task.execute(metrics);
			}
		}, jobs, OperationType.GARBAGE_COLLECT);
	}

	public CompactionManager.AllSSTableOpStatus relocateSSTables(final ColumnFamilyStore cfs, int jobs) throws InterruptedException, ExecutionException {
		if (!(cfs.getPartitioner().splitter().isPresent())) {
			CompactionManager.logger.info("Partitioner does not support splitting");
			return CompactionManager.AllSSTableOpStatus.ABORTED;
		}
		final Collection<Range<Token>> r = StorageService.instance.getLocalRanges(cfs.keyspace.getName());
		if (r.isEmpty()) {
			CompactionManager.logger.info("Relocate cannot run before a node has joined the ring");
			return CompactionManager.AllSSTableOpStatus.ABORTED;
		}
		final DiskBoundaries diskBoundaries = cfs.getDiskBoundaries();
		return parallelAllSSTableOperation(cfs, new CompactionManager.OneSSTableOperation() {
			@Override
			public Iterable<SSTableReader> filterSSTables(LifecycleTransaction transaction) {
				Set<SSTableReader> originals = Sets.newHashSet(transaction.originals());
				Set<SSTableReader> needsRelocation = originals.stream().filter(( s) -> !(inCorrectLocation(s))).collect(Collectors.toSet());
				transaction.cancel(Sets.difference(originals, needsRelocation));
				Map<Integer, List<SSTableReader>> groupedByDisk = groupByDiskIndex(needsRelocation);
				int maxSize = 0;
				for (List<SSTableReader> diskSSTables : groupedByDisk.values())
					maxSize = Math.max(maxSize, diskSSTables.size());

				List<SSTableReader> mixedSSTables = new ArrayList<>();
				for (int i = 0; i < maxSize; i++)
					for (List<SSTableReader> diskSSTables : groupedByDisk.values())
						if (i < (diskSSTables.size()))
							mixedSSTables.add(diskSSTables.get(i));



				return mixedSSTables;
			}

			public Map<Integer, List<SSTableReader>> groupByDiskIndex(Set<SSTableReader> needsRelocation) {
				return needsRelocation.stream().collect(Collectors.groupingBy(( s) -> diskBoundaries.getDiskIndex(s)));
			}

			private boolean inCorrectLocation(SSTableReader sstable) {
				if (!(cfs.getPartitioner().splitter().isPresent()))
					return true;

				int diskIndex = diskBoundaries.getDiskIndex(sstable);
				File diskLocation = diskBoundaries.directories.get(diskIndex).location;
				PartitionPosition diskLast = diskBoundaries.positions.get(diskIndex);
				return (sstable.descriptor.directory.getAbsolutePath().startsWith(diskLocation.getAbsolutePath())) && ((sstable.last.compareTo(diskLast)) <= 0);
			}

			@Override
			public void execute(LifecycleTransaction txn) {
				CompactionManager.logger.debug("Relocating {}", txn.originals());
				AbstractCompactionTask task = cfs.getCompactionStrategyManager().getCompactionTask(txn, CompactionManager.NO_GC, Long.MAX_VALUE);
				task.setUserDefined(true);
				task.setCompactionType(OperationType.RELOCATE);
				task.execute(metrics);
			}
		}, jobs, OperationType.RELOCATE);
	}

	public ListenableFuture<?> submitAntiCompaction(final ColumnFamilyStore cfs, final Collection<Range<Token>> ranges, final Refs<SSTableReader> sstables, final long repairedAt, final UUID parentRepairSession) {
		Runnable runnable = new WrappedRunnable() {
			@Override
			@SuppressWarnings("resource")
			public void runMayThrow() throws Exception {
				LifecycleTransaction modifier = null;
				while (modifier == null) {
					for (SSTableReader compactingSSTable : cfs.getTracker().getCompacting())
						sstables.releaseIfHolds(compactingSSTable);

					Set<SSTableReader> compactedSSTables = new HashSet<>();
					for (SSTableReader sstable : sstables)
						if (sstable.isMarkedCompacted())
							compactedSSTables.add(sstable);


					sstables.release(compactedSSTables);
					modifier = cfs.getTracker().tryModify(sstables, OperationType.ANTICOMPACTION);
				} 
				performAnticompaction(cfs, ranges, sstables, modifier, repairedAt, parentRepairSession);
			}
		};
		ListenableFuture<?> ret = null;
		try {
			ret = executor.submitIfRunning(runnable, "anticompaction");
			return ret;
		} finally {
			if ((ret == null) || (ret.isCancelled()))
				sstables.release();

		}
	}

	public void performAnticompaction(ColumnFamilyStore cfs, Collection<Range<Token>> ranges, Refs<SSTableReader> validatedForRepair, LifecycleTransaction txn, long repairedAt, UUID parentRepairSession) throws IOException, InterruptedException {
		CompactionManager.logger.info("[repair #{}] Starting anticompaction for {}.{} on {}/{} sstables", parentRepairSession, cfs.keyspace.getName(), cfs.getTableName(), validatedForRepair.size(), cfs.getLiveSSTables());
		CompactionManager.logger.trace("[repair #{}] Starting anticompaction for ranges {}", parentRepairSession, ranges);
		Set<SSTableReader> sstables = new HashSet<>(validatedForRepair);
		Set<SSTableReader> mutatedRepairStatuses = new HashSet<>();
		Set<SSTableReader> mutatedRepairStatusToNotify = new HashSet<>();
		Map<SSTableReader, Boolean> wasRepairedBefore = new HashMap<>();
		for (SSTableReader sstable : sstables)
			wasRepairedBefore.put(sstable, sstable.isRepaired());

		Set<SSTableReader> nonAnticompacting = new HashSet<>();
		Iterator<SSTableReader> sstableIterator = sstables.iterator();
		try {
			List<Range<Token>> normalizedRanges = Range.normalize(ranges);
			while (sstableIterator.hasNext()) {
				SSTableReader sstable = sstableIterator.next();
				Range<Token> sstableRange = new Range<>(sstable.first.getToken(), sstable.last.getToken());
				boolean shouldAnticompact = false;
				for (Range<Token> r : normalizedRanges) {
					if (r.contains(sstableRange)) {
						CompactionManager.logger.info("[repair #{}] SSTable {} fully contained in range {}, mutating repairedAt instead of anticompacting", parentRepairSession, sstable, r);
						sstable.descriptor.getMetadataSerializer().mutateRepairedAt(sstable.descriptor, repairedAt);
						sstable.reloadSSTableMetadata();
						mutatedRepairStatuses.add(sstable);
						if (!(wasRepairedBefore.get(sstable)))
							mutatedRepairStatusToNotify.add(sstable);

						sstableIterator.remove();
						shouldAnticompact = true;
						break;
					}else
						if (sstableRange.intersects(r)) {
							CompactionManager.logger.info("[repair #{}] SSTable {} ({}) will be anticompacted on range {}", parentRepairSession, sstable, sstableRange, r);
							shouldAnticompact = true;
						}

				}
				if (!shouldAnticompact) {
					CompactionManager.logger.info("[repair #{}] SSTable {} ({}) does not intersect repaired ranges {}, not touching repairedAt.", parentRepairSession, sstable, sstableRange, normalizedRanges);
					nonAnticompacting.add(sstable);
					sstableIterator.remove();
				}
			} 
			cfs.getTracker().notifySSTableRepairedStatusChanged(mutatedRepairStatusToNotify);
			txn.cancel(Sets.union(nonAnticompacting, mutatedRepairStatuses));
			validatedForRepair.release(Sets.union(nonAnticompacting, mutatedRepairStatuses));
			assert txn.originals().equals(sstables);
			if (!(sstables.isEmpty()))
				doAntiCompaction(cfs, ranges, txn, repairedAt);

			txn.finish();
		} finally {
			validatedForRepair.release();
			txn.close();
		}
		CompactionManager.logger.info("[repair #{}] Completed anticompaction successfully", parentRepairSession);
	}

	public void performMaximal(final ColumnFamilyStore cfStore, boolean splitOutput) {
		FBUtilities.waitOnFutures(submitMaximal(cfStore, CompactionManager.getDefaultGcBefore(cfStore, FBUtilities.nowInSeconds()), splitOutput));
	}

	public List<Future<?>> submitMaximal(final ColumnFamilyStore cfStore, final int gcBefore, boolean splitOutput) {
		final Collection<AbstractCompactionTask> tasks = cfStore.getCompactionStrategyManager().getMaximalTasks(gcBefore, splitOutput);
		if (tasks == null)
			return Collections.emptyList();

		List<Future<?>> futures = new ArrayList<>();
		int nonEmptyTasks = 0;
		for (final AbstractCompactionTask task : tasks) {
			Runnable runnable = new WrappedRunnable() {
				protected void runMayThrow() {
					task.execute(metrics);
				}
			};
			Future<?> fut = executor.submitIfRunning(runnable, "maximal task");
			if (!(fut.isCancelled()))
				futures.add(fut);

		}
		if (nonEmptyTasks > 1)
			CompactionManager.logger.info("Major compaction will not result in a single sstable - repaired and unrepaired data is kept separate and compaction runs per data_file_directory.");

		return futures;
	}

	public void forceCompactionForTokenRange(ColumnFamilyStore cfStore, Collection<Range<Token>> ranges) {
		final Collection<AbstractCompactionTask> tasks = cfStore.runWithCompactionsDisabled(() -> {
			Collection<SSTableReader> sstables = CompactionManager.sstablesInBounds(cfStore, ranges);
			if ((sstables == null) || (sstables.isEmpty())) {
				CompactionManager.logger.debug("No sstables found for the provided token range");
				return null;
			}
			return cfStore.getCompactionStrategyManager().getUserDefinedTasks(sstables, CompactionManager.getDefaultGcBefore(cfStore, FBUtilities.nowInSeconds()));
		}, false, false);
		if (tasks == null)
			return;

		Runnable runnable = new WrappedRunnable() {
			protected void runMayThrow() {
				for (AbstractCompactionTask task : tasks)
					if (task != null)
						task.execute(metrics);


			}
		};
		if (executor.isShutdown()) {
			CompactionManager.logger.info("Compaction executor has shut down, not submitting task");
			return;
		}
		FBUtilities.waitOnFuture(executor.submit(runnable));
	}

	private static Collection<SSTableReader> sstablesInBounds(ColumnFamilyStore cfs, Collection<Range<Token>> tokenRangeCollection) {
		final Set<SSTableReader> sstables = new HashSet<>();
		Iterable<SSTableReader> liveTables = cfs.getTracker().getView().select(SSTableSet.LIVE);
		SSTableIntervalTree tree = SSTableIntervalTree.build(liveTables);
		for (Range<Token> tokenRange : tokenRangeCollection) {
			Iterable<SSTableReader> ssTableReaders = View.sstablesInBounds(tokenRange.left.minKeyBound(), tokenRange.right.maxKeyBound(), tree);
			Iterables.addAll(sstables, ssTableReaders);
		}
		return sstables;
	}

	public void forceUserDefinedCompaction(String dataFiles) {
		String[] filenames = dataFiles.split(",");
		Multimap<ColumnFamilyStore, Descriptor> descriptors = ArrayListMultimap.create();
		for (String filename : filenames) {
			Descriptor desc = Descriptor.fromFilename(filename.trim());
			if ((Schema.instance.getCFMetaData(desc)) == null) {
				CompactionManager.logger.warn("Schema does not exist for file {}. Skipping.", filename);
				continue;
			}
			ColumnFamilyStore cfs = Keyspace.open(desc.ksname).getColumnFamilyStore(desc.cfname);
			descriptors.put(cfs, cfs.getDirectories().find(new File(filename.trim()).getName()));
		}
		List<Future<?>> futures = new ArrayList<>();
		int nowInSec = FBUtilities.nowInSeconds();
		for (ColumnFamilyStore cfs : descriptors.keySet())
			futures.add(submitUserDefined(cfs, descriptors.get(cfs), CompactionManager.getDefaultGcBefore(cfs, nowInSec)));

		FBUtilities.waitOnFutures(futures);
	}

	public void forceUserDefinedCleanup(String dataFiles) {
		String[] filenames = dataFiles.split(",");
		HashMap<ColumnFamilyStore, Descriptor> descriptors = Maps.newHashMap();
		for (String filename : filenames) {
			Descriptor desc = Descriptor.fromFilename(filename.trim());
			if ((Schema.instance.getCFMetaData(desc)) == null) {
				CompactionManager.logger.warn("Schema does not exist for file {}. Skipping.", filename);
				continue;
			}
			ColumnFamilyStore cfs = Keyspace.open(desc.ksname).getColumnFamilyStore(desc.cfname);
			desc = cfs.getDirectories().find(new File(filename.trim()).getName());
			if (desc != null)
				descriptors.put(cfs, desc);

		}
		if (!(StorageService.instance.isJoined())) {
			CompactionManager.logger.error("Cleanup cannot run before a node has joined the ring");
			return;
		}
		for (Map.Entry<ColumnFamilyStore, Descriptor> entry : descriptors.entrySet()) {
			ColumnFamilyStore cfs = entry.getKey();
			Keyspace keyspace = cfs.keyspace;
			Collection<Range<Token>> ranges = StorageService.instance.getLocalRanges(keyspace.getName());
			boolean hasIndexes = cfs.indexManager.hasIndexes();
			SSTableReader sstable = lookupSSTable(cfs, entry.getValue());
			if (sstable == null) {
				CompactionManager.logger.warn("Will not clean {}, it is not an active sstable", entry.getValue());
			}else {
				CompactionManager.CleanupStrategy cleanupStrategy = CompactionManager.CleanupStrategy.get(cfs, ranges, FBUtilities.nowInSeconds());
				try (LifecycleTransaction txn = cfs.getTracker().tryModify(sstable, OperationType.CLEANUP)) {
					doCleanupOne(cfs, txn, cleanupStrategy, ranges, hasIndexes);
				} catch (IOException e) {
					CompactionManager.logger.error("forceUserDefinedCleanup failed: {}", e.getLocalizedMessage());
				}
			}
		}
	}

	public Future<?> submitUserDefined(final ColumnFamilyStore cfs, final Collection<Descriptor> dataFiles, final int gcBefore) {
		Runnable runnable = new WrappedRunnable() {
			protected void runMayThrow() {
				Collection<SSTableReader> sstables = new ArrayList<>(dataFiles.size());
				for (Descriptor desc : dataFiles) {
					SSTableReader sstable = lookupSSTable(cfs, desc);
					if (sstable == null) {
						CompactionManager.logger.info("Will not compact {}: it is not an active sstable", desc);
					}else {
						sstables.add(sstable);
					}
				}
				if (sstables.isEmpty()) {
					CompactionManager.logger.info("No files to compact for user defined compaction");
				}else {
					List<AbstractCompactionTask> tasks = cfs.getCompactionStrategyManager().getUserDefinedTasks(sstables, gcBefore);
					for (AbstractCompactionTask task : tasks) {
						if (task != null)
							task.execute(metrics);

					}
				}
			}
		};
		return executor.submitIfRunning(runnable, "user defined task");
	}

	private SSTableReader lookupSSTable(final ColumnFamilyStore cfs, Descriptor descriptor) {
		for (SSTableReader sstable : cfs.getSSTables(SSTableSet.CANONICAL)) {
			if (sstable.descriptor.equals(descriptor))
				return sstable;

		}
		return null;
	}

	public Future<?> submitValidation(final ColumnFamilyStore cfStore, final Validator validator) {
		Callable<Object> callable = new Callable<Object>() {
			public Object call() throws IOException {
				try {
					doValidationCompaction(cfStore, validator);
				} catch (Throwable e) {
					validator.fail();
					throw e;
				}
				return this;
			}
		};
		return validationExecutor.submitIfRunning(callable, "validation");
	}

	public void disableAutoCompaction() {
		for (String ksname : Schema.instance.getNonSystemKeyspaces()) {
			for (ColumnFamilyStore cfs : Keyspace.open(ksname).getColumnFamilyStores())
				cfs.disableAutoCompaction();

		}
	}

	private void scrubOne(ColumnFamilyStore cfs, LifecycleTransaction modifier, boolean skipCorrupted, boolean checkData, boolean reinsertOverflowedTTL) throws IOException {
		CompactionInfo.Holder scrubInfo = null;
		try (Scrubber scrubber = new Scrubber(cfs, modifier, skipCorrupted, checkData, reinsertOverflowedTTL)) {
			scrubInfo = scrubber.getScrubInfo();
			metrics.beginCompaction(scrubInfo);
			scrubber.scrub();
		} finally {
			if (scrubInfo != null)
				metrics.finishCompaction(scrubInfo);

		}
	}

	private void verifyOne(ColumnFamilyStore cfs, SSTableReader sstable, boolean extendedVerify) throws IOException {
		CompactionInfo.Holder verifyInfo = null;
		try (Verifier verifier = new Verifier(cfs, sstable, false)) {
			verifyInfo = verifier.getVerifyInfo();
			metrics.beginCompaction(verifyInfo);
			verifier.verify(extendedVerify);
		} finally {
			if (verifyInfo != null)
				metrics.finishCompaction(verifyInfo);

		}
	}

	@VisibleForTesting
	public static boolean needsCleanup(SSTableReader sstable, Collection<Range<Token>> ownedRanges) {
		if (ownedRanges.isEmpty()) {
			return true;
		}
		List<Range<Token>> sortedRanges = Range.normalize(ownedRanges);
		Range<Token> firstRange = sortedRanges.get(0);
		if ((sstable.first.getToken().compareTo(firstRange.left)) <= 0)
			return true;

		for (int i = 0; i < (sortedRanges.size()); i++) {
			Range<Token> range = sortedRanges.get(i);
			if (range.right.isMinimum()) {
				return false;
			}
			DecoratedKey firstBeyondRange = sstable.firstKeyBeyond(range.right.maxKeyBound());
			if (firstBeyondRange == null) {
				return false;
			}
			if (i == ((sortedRanges.size()) - 1)) {
				return true;
			}
			Range<Token> nextRange = sortedRanges.get((i + 1));
			if ((firstBeyondRange.getToken().compareTo(nextRange.left)) <= 0) {
				return true;
			}
		}
		return false;
	}

	private void doCleanupOne(final ColumnFamilyStore cfs, LifecycleTransaction txn, CompactionManager.CleanupStrategy cleanupStrategy, Collection<Range<Token>> ranges, boolean hasIndexes) throws IOException {
		assert !(cfs.isIndex());
		SSTableReader sstable = txn.onlyOne();
		if ((!hasIndexes) && (!(new Bounds<>(sstable.first.getToken(), sstable.last.getToken()).intersects(ranges)))) {
			txn.obsoleteOriginals();
			txn.finish();
			return;
		}
		if (!(CompactionManager.needsCleanup(sstable, ranges))) {
			CompactionManager.logger.trace("Skipping {} for cleanup; all rows should be kept", sstable);
			return;
		}
		long start = System.nanoTime();
		long totalkeysWritten = 0;
		long expectedBloomFilterSize = Math.max(cfs.metadata.params.minIndexInterval, SSTableReader.getApproximateKeyCount(txn.originals()));
		if (CompactionManager.logger.isTraceEnabled())
			CompactionManager.logger.trace("Expected bloom filter size : {}", expectedBloomFilterSize);

		CompactionManager.logger.info("Cleaning up {}", sstable);
		File compactionFileLocation = sstable.descriptor.directory;
		RateLimiter limiter = getRateLimiter();
		double compressionRatio = sstable.getCompressionRatio();
		if (compressionRatio == (MetadataCollector.NO_COMPRESSION_RATIO))
			compressionRatio = 1.0;

		List<SSTableReader> finished;
		int nowInSec = FBUtilities.nowInSeconds();
		try (SSTableRewriter writer = SSTableRewriter.construct(cfs, txn, false, sstable.maxDataAge);ISSTableScanner scanner = cleanupStrategy.getScanner(sstable, null);CompactionController controller = new CompactionController(cfs, txn.originals(), CompactionManager.getDefaultGcBefore(cfs, nowInSec));Refs<SSTableReader> refs = Refs.ref(Collections.singleton(sstable));CompactionIterator ci = new CompactionIterator(OperationType.CLEANUP, Collections.singletonList(scanner), controller, nowInSec, UUIDGen.getTimeUUID(), metrics)) {
			writer.switchWriter(CompactionManager.createWriter(cfs, compactionFileLocation, expectedBloomFilterSize, sstable.getSSTableMetadata().repairedAt, sstable, txn));
			long lastBytesScanned = 0;
			while (ci.hasNext()) {
				if (ci.isStopRequested())
					throw new CompactionInterruptedException(ci.getCompactionInfo());

				try (UnfilteredRowIterator partition = ci.next();UnfilteredRowIterator notCleaned = cleanupStrategy.cleanup(partition)) {
					if (notCleaned == null)
						continue;

					if ((writer.append(notCleaned)) != null)
						totalkeysWritten++;

					long bytesScanned = scanner.getBytesScanned();
					CompactionManager.compactionRateLimiterAcquire(limiter, bytesScanned, lastBytesScanned, compressionRatio);
					lastBytesScanned = bytesScanned;
				}
			} 
			cfs.indexManager.flushAllIndexesBlocking();
			finished = writer.finish();
		}
		if (!(finished.isEmpty())) {
			String format = "Cleaned up to %s.  %s to %s (~%d%% of original) for %,d keys.  Time: %,dms.";
			long dTime = TimeUnit.NANOSECONDS.toMillis(((System.nanoTime()) - start));
			long startsize = sstable.onDiskLength();
			long endsize = 0;
			for (SSTableReader newSstable : finished)
				endsize += newSstable.onDiskLength();

			double ratio = ((double) (endsize)) / ((double) (startsize));
			CompactionManager.logger.info(String.format(format, finished.get(0).getFilename(), FBUtilities.prettyPrintMemory(startsize), FBUtilities.prettyPrintMemory(endsize), ((int) (ratio * 100)), totalkeysWritten, dTime));
		}
	}

	static void compactionRateLimiterAcquire(RateLimiter limiter, long bytesScanned, long lastBytesScanned, double compressionRatio) {
		long lengthRead = ((long) ((bytesScanned - lastBytesScanned) * compressionRatio)) + 1;
		while (lengthRead >= (Integer.MAX_VALUE)) {
			limiter.acquire(Integer.MAX_VALUE);
			lengthRead -= Integer.MAX_VALUE;
		} 
		if (lengthRead > 0) {
			limiter.acquire(((int) (lengthRead)));
		}
	}

	private static abstract class CleanupStrategy {
		protected final Collection<Range<Token>> ranges;

		protected final int nowInSec;

		protected CleanupStrategy(Collection<Range<Token>> ranges, int nowInSec) {
			this.ranges = ranges;
			this.nowInSec = nowInSec;
		}

		public static CompactionManager.CleanupStrategy get(ColumnFamilyStore cfs, Collection<Range<Token>> ranges, int nowInSec) {
			return cfs.indexManager.hasIndexes() ? new CompactionManager.CleanupStrategy.Full(cfs, ranges, nowInSec) : new CompactionManager.CleanupStrategy.Bounded(cfs, ranges, nowInSec);
		}

		public abstract ISSTableScanner getScanner(SSTableReader sstable, RateLimiter limiter);

		public abstract UnfilteredRowIterator cleanup(UnfilteredRowIterator partition);

		private static final class Bounded extends CompactionManager.CleanupStrategy {
			public Bounded(final ColumnFamilyStore cfs, Collection<Range<Token>> ranges, int nowInSec) {
				super(ranges, nowInSec);
				CompactionManager.cacheCleanupExecutor.submit(new Runnable() {
					@Override
					public void run() {
						cfs.cleanupCache();
					}
				});
			}

			@Override
			public ISSTableScanner getScanner(SSTableReader sstable, RateLimiter limiter) {
				return sstable.getScanner(ranges, limiter);
			}

			@Override
			public UnfilteredRowIterator cleanup(UnfilteredRowIterator partition) {
				return partition;
			}
		}

		private static final class Full extends CompactionManager.CleanupStrategy {
			private final ColumnFamilyStore cfs;

			public Full(ColumnFamilyStore cfs, Collection<Range<Token>> ranges, int nowInSec) {
				super(ranges, nowInSec);
				this.cfs = cfs;
			}

			@Override
			public ISSTableScanner getScanner(SSTableReader sstable, RateLimiter limiter) {
				return sstable.getScanner(limiter);
			}

			@Override
			public UnfilteredRowIterator cleanup(UnfilteredRowIterator partition) {
				if (Range.isInRanges(partition.partitionKey().getToken(), ranges))
					return partition;

				cfs.invalidateCachedPartition(partition.partitionKey());
				cfs.indexManager.deletePartition(partition, nowInSec);
				return null;
			}
		}
	}

	public static SSTableWriter createWriter(ColumnFamilyStore cfs, File compactionFileLocation, long expectedBloomFilterSize, long repairedAt, SSTableReader sstable, LifecycleTransaction txn) {
		FileUtils.createDirectory(compactionFileLocation);
		SerializationHeader header = sstable.header;
		if (header == null)
			header = SerializationHeader.make(sstable.metadata, Collections.singleton(sstable));

		return SSTableWriter.create(cfs.metadata, Descriptor.fromFilename(cfs.getSSTablePath(compactionFileLocation)), expectedBloomFilterSize, repairedAt, sstable.getSSTableLevel(), header, cfs.indexManager.listIndexes(), txn);
	}

	public static SSTableWriter createWriterForAntiCompaction(ColumnFamilyStore cfs, File compactionFileLocation, int expectedBloomFilterSize, long repairedAt, Collection<SSTableReader> sstables, LifecycleTransaction txn) {
		FileUtils.createDirectory(compactionFileLocation);
		int minLevel = Integer.MAX_VALUE;
		for (SSTableReader sstable : sstables) {
			if (minLevel == (Integer.MAX_VALUE))
				minLevel = sstable.getSSTableLevel();

			if (minLevel != (sstable.getSSTableLevel())) {
				minLevel = 0;
				break;
			}
		}
		return SSTableWriter.create(Descriptor.fromFilename(cfs.getSSTablePath(compactionFileLocation)), ((long) (expectedBloomFilterSize)), repairedAt, cfs.metadata, new MetadataCollector(sstables, cfs.metadata.comparator, minLevel), SerializationHeader.make(cfs.metadata, sstables), cfs.indexManager.listIndexes(), txn);
	}

	@SuppressWarnings("resource")
	private void doValidationCompaction(ColumnFamilyStore cfs, Validator validator) throws IOException {
		if (!(cfs.isValid()))
			return;

		Refs<SSTableReader> sstables = null;
		try {
			int gcBefore;
			int nowInSec = FBUtilities.nowInSeconds();
			UUID parentRepairSessionId = validator.desc.parentSessionId;
			String snapshotName;
			boolean isGlobalSnapshotValidation = cfs.snapshotExists(parentRepairSessionId.toString());
			if (isGlobalSnapshotValidation)
				snapshotName = parentRepairSessionId.toString();
			else
				snapshotName = validator.desc.sessionId.toString();

			boolean isSnapshotValidation = cfs.snapshotExists(snapshotName);
			if (isSnapshotValidation) {
				sstables = cfs.getSnapshotSSTableReader(snapshotName);
				gcBefore = cfs.gcBefore(((int) ((cfs.getSnapshotCreationTime(snapshotName)) / 1000)));
			}else {
				StorageService.instance.forceKeyspaceFlush(cfs.keyspace.getName(), cfs.name);
				sstables = getSSTablesToValidate(cfs, validator);
				if (sstables == null)
					return;

				if ((validator.gcBefore) > 0)
					gcBefore = validator.gcBefore;
				else
					gcBefore = CompactionManager.getDefaultGcBefore(cfs, nowInSec);

			}
			MerkleTrees tree = CompactionManager.createMerkleTrees(sstables, validator.desc.ranges, cfs);
			long start = System.nanoTime();
			try (AbstractCompactionStrategy.ScannerList scanners = cfs.getCompactionStrategyManager().getScanners(sstables, validator.desc.ranges);CompactionManager.ValidationCompactionController controller = new CompactionManager.ValidationCompactionController(cfs, gcBefore);CompactionIterator ci = new CompactionManager.ValidationCompactionIterator(scanners.scanners, controller, nowInSec, metrics)) {
				validator.prepare(cfs, tree);
				while (ci.hasNext()) {
					if (ci.isStopRequested())
						throw new CompactionInterruptedException(ci.getCompactionInfo());

					try (UnfilteredRowIterator partition = ci.next()) {
						validator.add(partition);
					}
				} 
				validator.complete();
			} finally {
				if (isSnapshotValidation && (!isGlobalSnapshotValidation)) {
					cfs.clearSnapshot(snapshotName);
				}
			}
			if (CompactionManager.logger.isDebugEnabled()) {
				long duration = TimeUnit.NANOSECONDS.toMillis(((System.nanoTime()) - start));
				CompactionManager.logger.debug("Validation finished in {} msec, for {}", duration, validator.desc);
			}
		} finally {
			if (sstables != null)
				sstables.release();

		}
	}

	private static MerkleTrees createMerkleTrees(Iterable<SSTableReader> sstables, Collection<Range<Token>> ranges, ColumnFamilyStore cfs) {
		MerkleTrees tree = new MerkleTrees(cfs.getPartitioner());
		long allPartitions = 0;
		Map<Range<Token>, Long> rangePartitionCounts = Maps.newHashMapWithExpectedSize(ranges.size());
		for (Range<Token> range : ranges) {
			long numPartitions = 0;
			for (SSTableReader sstable : sstables)
				numPartitions += sstable.estimatedKeysForRanges(Collections.singleton(range));

			rangePartitionCounts.put(range, numPartitions);
			allPartitions += numPartitions;
		}
		for (Range<Token> range : ranges) {
			long numPartitions = rangePartitionCounts.get(range);
			double rangeOwningRatio = (allPartitions > 0) ? ((double) (numPartitions)) / allPartitions : 0;
			int maxDepth = (rangeOwningRatio > 0) ? ((int) (Math.floor((20 - ((Math.log((1 / rangeOwningRatio))) / (Math.log(2))))))) : 0;
			int depth = (numPartitions > 0) ? ((int) (Math.min(Math.ceil(((Math.log(numPartitions)) / (Math.log(2)))), maxDepth))) : 0;
			tree.addMerkleTree(((int) (Math.pow(2, depth))), range);
		}
		if (CompactionManager.logger.isDebugEnabled()) {
			CompactionManager.logger.debug("Created {} merkle trees with merkle trees size {}, {} partitions, {} bytes", tree.ranges().size(), tree.size(), allPartitions, MerkleTrees.serializer.serializedSize(tree, 0));
		}
		return tree;
	}

	private synchronized Refs<SSTableReader> getSSTablesToValidate(ColumnFamilyStore cfs, Validator validator) {
		Refs<SSTableReader> sstables;
		ActiveRepairService.ParentRepairSession prs = ActiveRepairService.instance.getParentRepairSession(validator.desc.parentSessionId);
		if (prs == null)
			return null;

		Set<SSTableReader> sstablesToValidate = new HashSet<>();
		if (prs.isGlobal)
			prs.markSSTablesRepairing(cfs.metadata.cfId, validator.desc.parentSessionId);

		try (ColumnFamilyStore.RefViewFragment sstableCandidates = cfs.selectAndReference(View.select(SSTableSet.CANONICAL, ( s) -> (!(prs.isIncremental)) || (!(s.isRepaired()))))) {
			for (SSTableReader sstable : sstableCandidates.sstables) {
				if (new Bounds<>(sstable.first.getToken(), sstable.last.getToken()).intersects(validator.desc.ranges)) {
					sstablesToValidate.add(sstable);
				}
			}
			sstables = Refs.tryRef(sstablesToValidate);
			if (sstables == null) {
				CompactionManager.logger.error("Could not reference sstables");
				throw new RuntimeException("Could not reference sstables");
			}
		}
		return sstables;
	}

	private void doAntiCompaction(ColumnFamilyStore cfs, Collection<Range<Token>> ranges, LifecycleTransaction repaired, long repairedAt) {
		CompactionManager.logger.info("Performing anticompaction on {} sstables", repaired.originals().size());
		Set<SSTableReader> sstables = repaired.originals();
		Set<SSTableReader> unrepairedSSTables = sstables.stream().filter(( s) -> !(s.isRepaired())).collect(Collectors.toSet());
		Collection<Collection<SSTableReader>> groupedSSTables = cfs.getCompactionStrategyManager().groupSSTablesForAntiCompaction(unrepairedSSTables);
		int antiCompactedSSTableCount = 0;
		for (Collection<SSTableReader> sstableGroup : groupedSSTables) {
			try (LifecycleTransaction txn = repaired.split(sstableGroup)) {
				int antiCompacted = antiCompactGroup(cfs, ranges, txn, repairedAt);
				antiCompactedSSTableCount += antiCompacted;
			}
		}
		String format = "Anticompaction completed successfully, anticompacted from {} to {} sstable(s).";
		CompactionManager.logger.info(format, repaired.originals().size(), antiCompactedSSTableCount);
	}

	private int antiCompactGroup(ColumnFamilyStore cfs, Collection<Range<Token>> ranges, LifecycleTransaction anticompactionGroup, long repairedAt) {
		long groupMaxDataAge = -1;
		for (Iterator<SSTableReader> i = anticompactionGroup.originals().iterator(); i.hasNext();) {
			SSTableReader sstable = i.next();
			if (groupMaxDataAge < (sstable.maxDataAge))
				groupMaxDataAge = sstable.maxDataAge;

		}
		if ((anticompactionGroup.originals().size()) == 0) {
			CompactionManager.logger.info("No valid anticompactions for this group, All sstables were compacted and are no longer available");
			return 0;
		}
		CompactionManager.logger.info("Anticompacting {}", anticompactionGroup);
		Set<SSTableReader> sstableAsSet = anticompactionGroup.originals();
		File destination = cfs.getDirectories().getWriteableLocationAsFile(cfs.getExpectedCompactedFileSize(sstableAsSet, OperationType.ANTICOMPACTION));
		long repairedKeyCount = 0;
		long unrepairedKeyCount = 0;
		int nowInSec = FBUtilities.nowInSeconds();
		CompactionStrategyManager strategy = cfs.getCompactionStrategyManager();
		try (SSTableRewriter repairedSSTableWriter = SSTableRewriter.constructWithoutEarlyOpening(anticompactionGroup, false, groupMaxDataAge);SSTableRewriter unRepairedSSTableWriter = SSTableRewriter.constructWithoutEarlyOpening(anticompactionGroup, false, groupMaxDataAge);AbstractCompactionStrategy.ScannerList scanners = strategy.getScanners(anticompactionGroup.originals());CompactionController controller = new CompactionController(cfs, sstableAsSet, CompactionManager.getDefaultGcBefore(cfs, nowInSec));CompactionIterator ci = new CompactionIterator(OperationType.ANTICOMPACTION, scanners.scanners, controller, nowInSec, UUIDGen.getTimeUUID(), metrics)) {
			int expectedBloomFilterSize = Math.max(cfs.metadata.params.minIndexInterval, ((int) (SSTableReader.getApproximateKeyCount(sstableAsSet))));
			repairedSSTableWriter.switchWriter(CompactionManager.createWriterForAntiCompaction(cfs, destination, expectedBloomFilterSize, repairedAt, sstableAsSet, anticompactionGroup));
			unRepairedSSTableWriter.switchWriter(CompactionManager.createWriterForAntiCompaction(cfs, destination, expectedBloomFilterSize, ActiveRepairService.UNREPAIRED_SSTABLE, sstableAsSet, anticompactionGroup));
			Range.OrderedRangeContainmentChecker containmentChecker = new Range.OrderedRangeContainmentChecker(ranges);
			while (ci.hasNext()) {
				try (UnfilteredRowIterator partition = ci.next()) {
					if (containmentChecker.contains(partition.partitionKey().getToken())) {
						repairedSSTableWriter.append(partition);
						repairedKeyCount++;
					}else {
						unRepairedSSTableWriter.append(partition);
						unrepairedKeyCount++;
					}
				}
			} 
			List<SSTableReader> anticompactedSSTables = new ArrayList<>();
			anticompactionGroup.permitRedundantTransitions();
			repairedSSTableWriter.setRepairedAt(repairedAt).prepareToCommit();
			unRepairedSSTableWriter.prepareToCommit();
			anticompactedSSTables.addAll(repairedSSTableWriter.finished());
			anticompactedSSTables.addAll(unRepairedSSTableWriter.finished());
			repairedSSTableWriter.commit();
			unRepairedSSTableWriter.commit();
			CompactionManager.logger.trace("Repaired {} keys out of {} for {}/{} in {}", repairedKeyCount, (repairedKeyCount + unrepairedKeyCount), cfs.keyspace.getName(), cfs.getColumnFamilyName(), anticompactionGroup);
			return anticompactedSSTables.size();
		} catch (Throwable e) {
			JVMStabilityInspector.inspectThrowable(e);
			CompactionManager.logger.error(("Error anticompacting " + anticompactionGroup), e);
		}
		return 0;
	}

	public Future<?> submitIndexBuild(final SecondaryIndexBuilder builder) {
		Runnable runnable = new Runnable() {
			public void run() {
				metrics.beginCompaction(builder);
				try {
					builder.build();
				} finally {
					metrics.finishCompaction(builder);
				}
			}
		};
		return executor.submitIfRunning(runnable, "index build");
	}

	public Future<?> submitCacheWrite(final AutoSavingCache.Writer writer) {
		Runnable runnable = new Runnable() {
			public void run() {
				if (!(AutoSavingCache.flushInProgress.add(writer.cacheType()))) {
					CompactionManager.logger.trace("Cache flushing was already in progress: skipping {}", writer.getCompactionInfo());
					return;
				}
				try {
					metrics.beginCompaction(writer);
					try {
						writer.saveCache();
					} finally {
						metrics.finishCompaction(writer);
					}
				} finally {
					AutoSavingCache.flushInProgress.remove(writer.cacheType());
				}
			}
		};
		return executor.submitIfRunning(runnable, "cache write");
	}

	public List<SSTableReader> runIndexSummaryRedistribution(IndexSummaryRedistribution redistribution) throws IOException {
		metrics.beginCompaction(redistribution);
		try {
			return redistribution.redistributeSummaries();
		} finally {
			metrics.finishCompaction(redistribution);
		}
	}

	public static int getDefaultGcBefore(ColumnFamilyStore cfs, int nowInSec) {
		return cfs.isIndex() ? nowInSec : cfs.gcBefore(nowInSec);
	}

	private static class ValidationCompactionIterator extends CompactionIterator {
		public ValidationCompactionIterator(List<ISSTableScanner> scanners, CompactionManager.ValidationCompactionController controller, int nowInSec, CompactionMetrics metrics) {
			super(OperationType.VALIDATION, scanners, controller, nowInSec, UUIDGen.getTimeUUID(), metrics);
		}
	}

	private static class ValidationCompactionController extends CompactionController {
		public ValidationCompactionController(ColumnFamilyStore cfs, int gcBefore) {
			super(cfs, gcBefore);
		}

		@Override
		public java.util.function.Predicate<Long> getPurgeEvaluator(DecoratedKey key) {
			return ( time) -> true;
		}
	}

	public Future<?> submitViewBuilder(final ViewBuilder builder) {
		Runnable runnable = new Runnable() {
			public void run() {
				metrics.beginCompaction(builder);
				try {
					builder.run();
				} finally {
					metrics.finishCompaction(builder);
				}
			}
		};
		if (executor.isShutdown()) {
			CompactionManager.logger.info("Compaction executor has shut down, not submitting index build");
			return null;
		}
		return executor.submit(runnable);
	}

	public int getActiveCompactions() {
		return CompactionMetrics.getCompactions().size();
	}

	static class CompactionExecutor extends JMXEnabledThreadPoolExecutor {
		protected CompactionExecutor(int minThreads, int maxThreads, String name, BlockingQueue<Runnable> queue) {
			super(minThreads, maxThreads, 60, TimeUnit.SECONDS, queue, new NamedThreadFactory(name, Thread.MIN_PRIORITY), "internal");
		}

		private CompactionExecutor(int threadCount, String name) {
			this(threadCount, threadCount, name, new LinkedBlockingQueue<Runnable>());
		}

		public CompactionExecutor() {
			this(Math.max(1, DatabaseDescriptor.getConcurrentCompactors()), "CompactionExecutor");
		}

		protected void beforeExecute(Thread t, Runnable r) {
			CompactionManager.isCompactionManager.set(true);
			super.beforeExecute(t, r);
		}

		@Override
		public void afterExecute(Runnable r, Throwable t) {
			DebuggableThreadPoolExecutor.maybeResetTraceSessionWrapper(r);
			if (t == null)
				t = DebuggableThreadPoolExecutor.extractThrowable(r);

			if (t != null) {
				if (t instanceof CompactionInterruptedException) {
					DebuggableThreadPoolExecutor.logger.info(t.getMessage());
					if (((t.getSuppressed()) != null) && ((t.getSuppressed().length) > 0))
						DebuggableThreadPoolExecutor.logger.warn("Interruption of compaction encountered exceptions:", t);
					else
						DebuggableThreadPoolExecutor.logger.trace("Full interruption stack trace:", t);

				}else {
					DebuggableThreadPoolExecutor.handleOrLog(t);
				}
			}
			SnapshotDeletingTask.rescheduleFailedTasks();
		}

		public ListenableFuture<?> submitIfRunning(Runnable task, String name) {
			return submitIfRunning(Executors.callable(task, null), name);
		}

		public ListenableFuture<?> submitIfRunning(Callable<?> task, String name) {
			if (isShutdown()) {
				DebuggableThreadPoolExecutor.logger.info("Executor has been shut down, not submitting {}", name);
				return Futures.immediateCancelledFuture();
			}
			try {
				ListenableFutureTask ret = ListenableFutureTask.create(task);
				execute(ret);
				return ret;
			} catch (RejectedExecutionException ex) {
				if (isShutdown())
					DebuggableThreadPoolExecutor.logger.info("Executor has shut down, could not submit {}", name);
				else
					DebuggableThreadPoolExecutor.logger.error("Failed to submit {}", name, ex);

				return Futures.immediateCancelledFuture();
			}
		}
	}

	private static class ValidationExecutor extends CompactionManager.CompactionExecutor {
		public ValidationExecutor() {
			super(1, Integer.MAX_VALUE, "ValidationExecutor", new SynchronousQueue<Runnable>());
		}
	}

	private static class CacheCleanupExecutor extends CompactionManager.CompactionExecutor {
		public CacheCleanupExecutor() {
			super(1, "CacheCleanupExecutor");
		}
	}

	public interface CompactionExecutorStatsCollector {
		void beginCompaction(CompactionInfo.Holder ci);

		void finishCompaction(CompactionInfo.Holder ci);
	}

	public List<Map<String, String>> getCompactions() {
		List<CompactionInfo.Holder> compactionHolders = CompactionMetrics.getCompactions();
		List<Map<String, String>> out = new ArrayList<Map<String, String>>(compactionHolders.size());
		for (CompactionInfo.Holder ci : compactionHolders)
			out.add(ci.getCompactionInfo().asMap());

		return out;
	}

	public List<String> getCompactionSummary() {
		List<CompactionInfo.Holder> compactionHolders = CompactionMetrics.getCompactions();
		List<String> out = new ArrayList<String>(compactionHolders.size());
		for (CompactionInfo.Holder ci : compactionHolders)
			out.add(ci.getCompactionInfo().toString());

		return out;
	}

	public TabularData getCompactionHistory() {
		try {
			return SystemKeyspace.getCompactionHistory();
		} catch (OpenDataException e) {
			throw new RuntimeException(e);
		}
	}

	public long getTotalBytesCompacted() {
		return metrics.bytesCompacted.getCount();
	}

	public long getTotalCompactionsCompleted() {
		return metrics.totalCompactionsCompleted.getCount();
	}

	public int getPendingTasks() {
		return metrics.pendingTasks.getValue();
	}

	public long getCompletedTasks() {
		return metrics.completedTasks.getValue();
	}

	public void stopCompaction(String type) {
		OperationType operation = OperationType.valueOf(type);
		for (CompactionInfo.Holder holder : CompactionMetrics.getCompactions()) {
			if ((holder.getCompactionInfo().getTaskType()) == operation)
				holder.stop();

		}
	}

	public void stopCompactionById(String compactionId) {
		for (CompactionInfo.Holder holder : CompactionMetrics.getCompactions()) {
			UUID holderId = holder.getCompactionInfo().compactionId();
			if ((holderId != null) && (holderId.equals(UUID.fromString(compactionId))))
				holder.stop();

		}
	}

	public void setConcurrentCompactors(int value) {
		if (value > (executor.getCorePoolSize())) {
			executor.setMaximumPoolSize(value);
			executor.setCorePoolSize(value);
		}else
			if (value < (executor.getCorePoolSize())) {
				executor.setCorePoolSize(value);
				executor.setMaximumPoolSize(value);
			}

	}

	public int getCoreCompactorThreads() {
		return executor.getCorePoolSize();
	}

	public void setCoreCompactorThreads(int number) {
		executor.setCorePoolSize(number);
	}

	public int getMaximumCompactorThreads() {
		return executor.getMaximumPoolSize();
	}

	public void setMaximumCompactorThreads(int number) {
		executor.setMaximumPoolSize(number);
	}

	public int getCoreValidationThreads() {
		return validationExecutor.getCorePoolSize();
	}

	public void setCoreValidationThreads(int number) {
		validationExecutor.setCorePoolSize(number);
	}

	public int getMaximumValidatorThreads() {
		return validationExecutor.getMaximumPoolSize();
	}

	public void setMaximumValidatorThreads(int number) {
		validationExecutor.setMaximumPoolSize(number);
	}

	public void interruptCompactionFor(Iterable<CFMetaData> columnFamilies, boolean interruptValidation) {
		assert columnFamilies != null;
		for (CompactionInfo.Holder compactionHolder : CompactionMetrics.getCompactions()) {
			CompactionInfo info = compactionHolder.getCompactionInfo();
			if (((info.getTaskType()) == (OperationType.VALIDATION)) && (!interruptValidation))
				continue;

			if (Iterables.contains(columnFamilies, info.getCFMetaData()))
				compactionHolder.stop();

		}
	}

	public void interruptCompactionForCFs(Iterable<ColumnFamilyStore> cfss, boolean interruptValidation) {
		List<CFMetaData> metadata = new ArrayList<>();
		for (ColumnFamilyStore cfs : cfss)
			metadata.add(cfs.metadata);

		interruptCompactionFor(metadata, interruptValidation);
	}

	public void waitForCessation(Iterable<ColumnFamilyStore> cfss) {
		long start = System.nanoTime();
		long delay = TimeUnit.MINUTES.toNanos(1);
		while (((System.nanoTime()) - start) < delay) {
			if (CompactionManager.instance.isCompacting(cfss))
				Uninterruptibles.sleepUninterruptibly(1, TimeUnit.MILLISECONDS);
			else
				break;

		} 
	}
}

