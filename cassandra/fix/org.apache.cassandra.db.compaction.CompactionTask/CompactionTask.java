

import com.codahale.metrics.Counter;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.compaction.AbstractCompactionStrategy;
import org.apache.cassandra.db.compaction.AbstractCompactionTask;
import org.apache.cassandra.db.compaction.CompactionController;
import org.apache.cassandra.db.compaction.CompactionInfo;
import org.apache.cassandra.db.compaction.CompactionInterruptedException;
import org.apache.cassandra.db.compaction.CompactionIterator;
import org.apache.cassandra.db.compaction.CompactionLogger;
import org.apache.cassandra.db.compaction.CompactionManager;
import org.apache.cassandra.db.compaction.CompactionStrategyManager;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.compaction.writers.CompactionAwareWriter;
import org.apache.cassandra.db.compaction.writers.DefaultCompactionWriter;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.ISSTableScanner;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;
import org.apache.cassandra.io.sstable.metadata.StatsMetadata;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.service.ActiveRepairService;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.WrappedRunnable;
import org.apache.cassandra.utils.concurrent.Ref;
import org.apache.cassandra.utils.concurrent.Refs;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CompactionTask extends AbstractCompactionTask {
	protected static final Logger logger = LoggerFactory.getLogger(CompactionTask.class);

	protected final int gcBefore;

	protected final boolean keepOriginals;

	protected static long totalBytesCompacted = 0;

	private CompactionManager.CompactionExecutorStatsCollector collector;

	public CompactionTask(ColumnFamilyStore cfs, LifecycleTransaction txn, int gcBefore) {
		this(cfs, txn, gcBefore, false);
	}

	@Deprecated
	public CompactionTask(ColumnFamilyStore cfs, LifecycleTransaction txn, int gcBefore, boolean offline, boolean keepOriginals) {
		this(cfs, txn, gcBefore, keepOriginals);
	}

	public CompactionTask(ColumnFamilyStore cfs, LifecycleTransaction txn, int gcBefore, boolean keepOriginals) {
		super(cfs, txn);
		this.gcBefore = gcBefore;
		this.keepOriginals = keepOriginals;
	}

	public static synchronized long addToTotalBytesCompacted(long bytesCompacted) {
		return CompactionTask.totalBytesCompacted += bytesCompacted;
	}

	protected int executeInternal(CompactionManager.CompactionExecutorStatsCollector collector) {
		this.collector = collector;
		run();
		return transaction.originals().size();
	}

	public boolean reduceScopeForLimitedSpace(long expectedSize) {
		if ((partialCompactionsAcceptable()) && ((transaction.originals().size()) > 1)) {
			CompactionTask.logger.warn("insufficient space to compact all requested files. {}MB required, {}", ((((float) (expectedSize)) / 1024) / 1024), StringUtils.join(transaction.originals(), ", "));
			SSTableReader removedSSTable = cfs.getMaxSizeFile(transaction.originals());
			transaction.cancel(removedSSTable);
			return true;
		}
		return false;
	}

	protected void runMayThrow() throws Exception {
		assert (transaction) != null;
		if (transaction.originals().isEmpty())
			return;

		CompactionStrategyManager strategy = cfs.getCompactionStrategyManager();
		if (DatabaseDescriptor.isSnapshotBeforeCompaction())
			cfs.snapshotWithoutFlush((((System.currentTimeMillis()) + "-compact-") + (cfs.name)));

		checkAvailableDiskSpace();
		assert !(Iterables.any(transaction.originals(), new Predicate<SSTableReader>() {
			@Override
			public boolean apply(SSTableReader sstable) {
				return !(sstable.descriptor.cfname.equals(cfs.name));
			}
		}));
		UUID taskId = transaction.opId();
		StringBuilder ssTableLoggerMsg = new StringBuilder("[");
		for (SSTableReader sstr : transaction.originals()) {
			ssTableLoggerMsg.append(String.format("%s:level=%d, ", sstr.getFilename(), sstr.getSSTableLevel()));
		}
		ssTableLoggerMsg.append("]");
		CompactionTask.logger.debug("Compacting ({}) {}", taskId, ssTableLoggerMsg);
		RateLimiter limiter = CompactionManager.instance.getRateLimiter();
		long start = System.nanoTime();
		long startTime = System.currentTimeMillis();
		long totalKeysWritten = 0;
		long estimatedKeys = 0;
		long inputSizeBytes;
		try (CompactionController controller = getCompactionController(transaction.originals())) {
			Set<SSTableReader> actuallyCompact = Sets.difference(transaction.originals(), controller.getFullyExpiredSSTables());
			Collection<SSTableReader> newSStables;
			long[] mergedRowCounts;
			long totalSourceCQLRows;
			int nowInSec = FBUtilities.nowInSeconds();
			try (Refs<SSTableReader> refs = Refs.ref(actuallyCompact);AbstractCompactionStrategy.ScannerList scanners = strategy.getScanners(actuallyCompact);CompactionIterator ci = new CompactionIterator(compactionType, scanners.scanners, controller, nowInSec, taskId)) {
				long lastCheckObsoletion = start;
				inputSizeBytes = scanners.getTotalCompressedSize();
				double compressionRatio = scanners.getCompressionRatio();
				if (compressionRatio == (MetadataCollector.NO_COMPRESSION_RATIO))
					compressionRatio = 1.0;

				long lastBytesScanned = 0;
				if (!(controller.cfs.getCompactionStrategyManager().isActive()))
					throw new CompactionInterruptedException(ci.getCompactionInfo());

				if ((collector) != null)
					collector.beginCompaction(ci);

				try (CompactionAwareWriter writer = getCompactionAwareWriter(cfs, getDirectories(), transaction, actuallyCompact)) {
					estimatedKeys = writer.estimatedKeys();
					while (ci.hasNext()) {
						if (ci.isStopRequested())
							throw new CompactionInterruptedException(ci.getCompactionInfo());

						if (writer.append(ci.next()))
							totalKeysWritten++;

						long bytesScanned = scanners.getTotalBytesScanned();
						lastBytesScanned = bytesScanned;
						if (((System.nanoTime()) - lastCheckObsoletion) > (TimeUnit.MINUTES.toNanos(1L))) {
							controller.maybeRefreshOverlaps();
							lastCheckObsoletion = System.nanoTime();
						}
					} 
					newSStables = writer.finish();
				} finally {
					if ((collector) != null)
						collector.finishCompaction(ci);

					mergedRowCounts = ci.getMergedRowCounts();
					totalSourceCQLRows = ci.getTotalSourceCQLRows();
				}
			}
			if (transaction.isOffline()) {
				Refs.release(Refs.selfRefs(newSStables));
			}else {
				long durationInNano = (System.nanoTime()) - start;
				long dTime = TimeUnit.NANOSECONDS.toMillis(durationInNano);
				long startsize = inputSizeBytes;
				long endsize = SSTableReader.getTotalBytes(newSStables);
				double ratio = ((double) (endsize)) / ((double) (startsize));
				StringBuilder newSSTableNames = new StringBuilder();
				for (SSTableReader reader : newSStables)
					newSSTableNames.append(reader.descriptor.baseFilename()).append(",");

				long totalSourceRows = 0;
				for (int i = 0; i < (mergedRowCounts.length); i++)
					totalSourceRows += (mergedRowCounts[i]) * (i + 1);

				String mergeSummary = CompactionTask.updateCompactionHistory(cfs.keyspace.getName(), cfs.getTableName(), mergedRowCounts, startsize, endsize);
				CompactionTask.logger.debug(String.format("Compacted (%s) %d sstables to [%s] to level=%d.  %s to %s (~%d%% of original) in %,dms.  Read Throughput = %s, Write Throughput = %s, Row Throughput = ~%,d/s.  %,d total partitions merged to %,d.  Partition merge counts were {%s}", taskId, transaction.originals().size(), newSSTableNames.toString(), getLevel(), FBUtilities.prettyPrintMemory(startsize), FBUtilities.prettyPrintMemory(endsize), ((int) (ratio * 100)), dTime, FBUtilities.prettyPrintMemoryPerSecond(startsize, durationInNano), FBUtilities.prettyPrintMemoryPerSecond(endsize, durationInNano), (((int) (totalSourceCQLRows)) / ((TimeUnit.NANOSECONDS.toSeconds(durationInNano)) + 1)), totalSourceRows, totalKeysWritten, mergeSummary));
				CompactionTask.logger.trace("CF Total Bytes Compacted: {}", FBUtilities.prettyPrintMemory(CompactionTask.addToTotalBytesCompacted(endsize)));
				CompactionTask.logger.trace("Actual #keys: {}, Estimated #keys:{}, Err%: {}", totalKeysWritten, estimatedKeys, (((double) (totalKeysWritten - estimatedKeys)) / totalKeysWritten));
				cfs.getCompactionStrategyManager().compactionLogger.compaction(startTime, transaction.originals(), System.currentTimeMillis(), newSStables);
				cfs.metric.compactionBytesWritten.inc(endsize);
			}
		}
	}

	@Override
	public CompactionAwareWriter getCompactionAwareWriter(ColumnFamilyStore cfs, Directories directories, LifecycleTransaction transaction, Set<SSTableReader> nonExpiredSSTables) {
		return new DefaultCompactionWriter(cfs, directories, transaction, nonExpiredSSTables, keepOriginals, getLevel());
	}

	public static String updateCompactionHistory(String keyspaceName, String columnFamilyName, long[] mergedRowCounts, long startSize, long endSize) {
		StringBuilder mergeSummary = new StringBuilder(((mergedRowCounts.length) * 10));
		Map<Integer, Long> mergedRows = new HashMap<>();
		for (int i = 0; i < (mergedRowCounts.length); i++) {
			long count = mergedRowCounts[i];
			if (count == 0)
				continue;

			int rows = i + 1;
			mergeSummary.append(String.format("%d:%d, ", rows, count));
			mergedRows.put(rows, count);
		}
		SystemKeyspace.updateCompactionHistory(keyspaceName, columnFamilyName, System.currentTimeMillis(), startSize, endSize, mergedRows);
		return mergeSummary.toString();
	}

	protected Directories getDirectories() {
		return cfs.getDirectories();
	}

	public static long getMinRepairedAt(Set<SSTableReader> actuallyCompact) {
		long minRepairedAt = Long.MAX_VALUE;
		for (SSTableReader sstable : actuallyCompact)
			minRepairedAt = Math.min(minRepairedAt, sstable.getSSTableMetadata().repairedAt);

		if (minRepairedAt == (Long.MAX_VALUE))
			return ActiveRepairService.UNREPAIRED_SSTABLE;

		return minRepairedAt;
	}

	protected void checkAvailableDiskSpace() {
		if ((!(cfs.isCompactionDiskSpaceCheckEnabled())) && ((compactionType) == (OperationType.COMPACTION))) {
			CompactionTask.logger.info("Compaction space check is disabled");
			return;
		}
		CompactionStrategyManager strategy = cfs.getCompactionStrategyManager();
		while (true) {
			long expectedWriteSize = cfs.getExpectedCompactedFileSize(transaction.originals(), compactionType);
			long estimatedSSTables = Math.max(1, (expectedWriteSize / (strategy.getMaxSSTableBytes())));
			if (cfs.getDirectories().hasAvailableDiskSpace(estimatedSSTables, expectedWriteSize))
				break;

			if (!(reduceScopeForLimitedSpace(expectedWriteSize))) {
				String msg = String.format("Not enough space for compaction, estimated sstables = %d, expected write size = %d", estimatedSSTables, expectedWriteSize);
				CompactionTask.logger.warn(msg);
				throw new RuntimeException(msg);
			}
			CompactionTask.logger.warn("Not enough space for compaction, {}MB estimated.  Reducing scope.", ((((float) (expectedWriteSize)) / 1024) / 1024));
		} 
	}

	protected int getLevel() {
		return 0;
	}

	protected CompactionController getCompactionController(Set<SSTableReader> toCompact) {
		return new CompactionController(cfs, toCompact, gcBefore);
	}

	protected boolean partialCompactionsAcceptable() {
		return !(isUserDefined);
	}

	public static long getMaxDataAge(Collection<SSTableReader> sstables) {
		long max = 0;
		for (SSTableReader sstable : sstables) {
			if ((sstable.maxDataAge) > max)
				max = sstable.maxDataAge;

		}
		return max;
	}
}

