

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.compaction.CompactionInfo;
import org.apache.cassandra.db.compaction.CompactionInterruptedException;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.Downsampling;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.metrics.RestorableMeter;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.concurrent.Refs;
import org.apache.cassandra.utils.concurrent.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IndexSummaryRedistribution extends CompactionInfo.Holder {
	private static final Logger logger = LoggerFactory.getLogger(IndexSummaryRedistribution.class);

	static final double UPSAMPLE_THRESHOLD = 1.5;

	static final double DOWNSAMPLE_THESHOLD = 0.75;

	private final List<SSTableReader> compacting;

	private final Map<UUID, LifecycleTransaction> transactions;

	private final long memoryPoolBytes;

	private final UUID compactionId;

	private volatile long remainingSpace;

	public IndexSummaryRedistribution(List<SSTableReader> compacting, Map<UUID, LifecycleTransaction> transactions, long memoryPoolBytes) {
		this.compacting = compacting;
		this.transactions = transactions;
		this.memoryPoolBytes = memoryPoolBytes;
		this.compactionId = UUID.randomUUID();
	}

	public List<SSTableReader> redistributeSummaries() throws IOException {
		IndexSummaryRedistribution.logger.info("Redistributing index summaries");
		List<SSTableReader> oldFormatSSTables = new ArrayList<>();
		List<SSTableReader> redistribute = new ArrayList<>();
		for (LifecycleTransaction txn : transactions.values()) {
			for (SSTableReader sstable : ImmutableList.copyOf(txn.originals())) {
				IndexSummaryRedistribution.logger.trace("SSTable {} cannot be re-sampled due to old sstable format", sstable);
				if (!(sstable.descriptor.version.hasSamplingLevel())) {
					oldFormatSSTables.add(sstable);
					txn.cancel(sstable);
				}
			}
			redistribute.addAll(txn.originals());
		}
		long total = 0;
		for (SSTableReader sstable : Iterables.concat(compacting, redistribute))
			total += sstable.getIndexSummaryOffHeapSize();

		IndexSummaryRedistribution.logger.trace("Beginning redistribution of index summaries for {} sstables with memory pool size {} MB; current spaced used is {} MB", redistribute.size(), (((memoryPoolBytes) / 1024L) / 1024L), ((total / 1024.0) / 1024.0));
		final Map<SSTableReader, Double> readRates = new HashMap<>(redistribute.size());
		double totalReadsPerSec = 0.0;
		for (SSTableReader sstable : redistribute) {
			if (isStopRequested())
				throw new CompactionInterruptedException(getCompactionInfo());

			if ((sstable.getReadMeter()) != null) {
				Double readRate = sstable.getReadMeter().fifteenMinuteRate();
				totalReadsPerSec += readRate;
				readRates.put(sstable, readRate);
			}
		}
		IndexSummaryRedistribution.logger.trace("Total reads/sec across all sstables in index summary resize process: {}", totalReadsPerSec);
		List<SSTableReader> sstablesByHotness = new ArrayList<>(redistribute);
		Collections.sort(sstablesByHotness, new IndexSummaryRedistribution.ReadRateComparator(readRates));
		long remainingBytes = memoryPoolBytes;
		for (SSTableReader sstable : Iterables.concat(compacting, oldFormatSSTables))
			remainingBytes -= sstable.getIndexSummaryOffHeapSize();

		IndexSummaryRedistribution.logger.trace("Index summaries for compacting SSTables are using {} MB of space", ((((memoryPoolBytes) - remainingBytes) / 1024.0) / 1024.0));
		List<SSTableReader> newSSTables;
		try (Refs<SSTableReader> refs = Refs.ref(sstablesByHotness)) {
			newSSTables = adjustSamplingLevels(sstablesByHotness, transactions, totalReadsPerSec, remainingBytes);
			for (LifecycleTransaction txn : transactions.values())
				txn.finish();

		}
		total = 0;
		for (SSTableReader sstable : Iterables.concat(compacting, oldFormatSSTables, newSSTables))
			total += sstable.getIndexSummaryOffHeapSize();

		IndexSummaryRedistribution.logger.trace("Completed resizing of index summaries; current approximate memory used: {}", FBUtilities.prettyPrintMemory(total));
		return newSSTables;
	}

	private List<SSTableReader> adjustSamplingLevels(List<SSTableReader> sstables, Map<UUID, LifecycleTransaction> transactions, double totalReadsPerSec, long memoryPoolCapacity) throws IOException {
		List<IndexSummaryRedistribution.ResampleEntry> toDownsample = new ArrayList<>(((sstables.size()) / 4));
		List<IndexSummaryRedistribution.ResampleEntry> toUpsample = new ArrayList<>(((sstables.size()) / 4));
		List<IndexSummaryRedistribution.ResampleEntry> forceResample = new ArrayList<>();
		List<IndexSummaryRedistribution.ResampleEntry> forceUpsample = new ArrayList<>();
		List<SSTableReader> newSSTables = new ArrayList<>(sstables.size());
		remainingSpace = memoryPoolCapacity;
		for (SSTableReader sstable : sstables) {
			if (isStopRequested())
				throw new CompactionInterruptedException(getCompactionInfo());

			int minIndexInterval = sstable.metadata.params.minIndexInterval;
			int maxIndexInterval = sstable.metadata.params.maxIndexInterval;
			double readsPerSec = ((sstable.getReadMeter()) == null) ? 0.0 : sstable.getReadMeter().fifteenMinuteRate();
			long idealSpace = Math.round(((remainingSpace) * (readsPerSec / totalReadsPerSec)));
			int currentNumEntries = sstable.getIndexSummarySize();
			double avgEntrySize = (sstable.getIndexSummaryOffHeapSize()) / ((double) (currentNumEntries));
			long targetNumEntries = Math.max(1, Math.round((idealSpace / avgEntrySize)));
			int currentSamplingLevel = sstable.getIndexSummarySamplingLevel();
			int maxSummarySize = sstable.getMaxIndexSummarySize();
			if ((sstable.getMinIndexInterval()) != minIndexInterval) {
				int effectiveSamplingLevel = ((int) (Math.round((currentSamplingLevel * (minIndexInterval / ((double) (sstable.getMinIndexInterval())))))));
				maxSummarySize = ((int) (Math.round((maxSummarySize * ((sstable.getMinIndexInterval()) / ((double) (minIndexInterval)))))));
				IndexSummaryRedistribution.logger.trace("min_index_interval changed from {} to {}, so the current sampling level for {} is effectively now {} (was {})", sstable.getMinIndexInterval(), minIndexInterval, sstable, effectiveSamplingLevel, currentSamplingLevel);
				currentSamplingLevel = effectiveSamplingLevel;
			}
			double effectiveIndexInterval = sstable.getEffectiveIndexInterval();
			if (effectiveIndexInterval < minIndexInterval) {
				IndexSummaryRedistribution.logger.trace("Forcing resample of {} because the current index interval ({}) is below min_index_interval ({})", sstable, effectiveIndexInterval, minIndexInterval);
			}else
				if (effectiveIndexInterval > maxIndexInterval) {
					IndexSummaryRedistribution.logger.trace("Forcing upsample of {} because the current index interval ({}) is above max_index_interval ({})", sstable, effectiveIndexInterval, maxIndexInterval);
				}else {
				}

			totalReadsPerSec -= readsPerSec;
		}
		if ((remainingSpace) > 0) {
			Pair<List<SSTableReader>, List<IndexSummaryRedistribution.ResampleEntry>> result = IndexSummaryRedistribution.distributeRemainingSpace(toDownsample, remainingSpace);
			toDownsample = result.right;
			newSSTables.addAll(result.left);
			for (SSTableReader sstable : result.left)
				transactions.get(sstable.metadata.cfId).cancel(sstable);

		}
		toDownsample.addAll(forceResample);
		toDownsample.addAll(toUpsample);
		toDownsample.addAll(forceUpsample);
		for (IndexSummaryRedistribution.ResampleEntry entry : toDownsample) {
			if (isStopRequested())
				throw new CompactionInterruptedException(getCompactionInfo());

			SSTableReader sstable = entry.sstable;
			IndexSummaryRedistribution.logger.trace("Re-sampling index summary for {} from {}/{} to {}/{} of the original number of entries", sstable, sstable.getIndexSummarySamplingLevel(), Downsampling.BASE_SAMPLING_LEVEL, entry.newSamplingLevel, Downsampling.BASE_SAMPLING_LEVEL);
			ColumnFamilyStore cfs = Keyspace.open(sstable.metadata.ksName).getColumnFamilyStore(sstable.metadata.cfId);
			SSTableReader replacement = sstable.cloneWithNewSummarySamplingLevel(cfs, entry.newSamplingLevel);
			newSSTables.add(replacement);
			transactions.get(sstable.metadata.cfId).update(replacement, true);
		}
		return newSSTables;
	}

	@com.google.common.annotations.VisibleForTesting
	static Pair<List<SSTableReader>, List<IndexSummaryRedistribution.ResampleEntry>> distributeRemainingSpace(List<IndexSummaryRedistribution.ResampleEntry> toDownsample, long remainingSpace) {
		Collections.sort(toDownsample, new Comparator<IndexSummaryRedistribution.ResampleEntry>() {
			public int compare(IndexSummaryRedistribution.ResampleEntry o1, IndexSummaryRedistribution.ResampleEntry o2) {
				return Double.compare(((o1.sstable.getIndexSummaryOffHeapSize()) - (o1.newSpaceUsed)), ((o2.sstable.getIndexSummaryOffHeapSize()) - (o2.newSpaceUsed)));
			}
		});
		int noDownsampleCutoff = 0;
		List<SSTableReader> willNotDownsample = new ArrayList<>();
		while ((remainingSpace > 0) && (noDownsampleCutoff < (toDownsample.size()))) {
			IndexSummaryRedistribution.ResampleEntry entry = toDownsample.get(noDownsampleCutoff);
			long extraSpaceRequired = (entry.sstable.getIndexSummaryOffHeapSize()) - (entry.newSpaceUsed);
			if (extraSpaceRequired <= remainingSpace) {
				IndexSummaryRedistribution.logger.trace("Using leftover space to keep {} at the current sampling level ({})", entry.sstable, entry.sstable.getIndexSummarySamplingLevel());
				willNotDownsample.add(entry.sstable);
				remainingSpace -= extraSpaceRequired;
			}else {
				break;
			}
			noDownsampleCutoff++;
		} 
		return Pair.create(willNotDownsample, toDownsample.subList(noDownsampleCutoff, toDownsample.size()));
	}

	public CompactionInfo getCompactionInfo() {
		return new CompactionInfo(OperationType.INDEX_SUMMARY, ((memoryPoolBytes) - (remainingSpace)), memoryPoolBytes, "bytes", compactionId);
	}

	private static class ReadRateComparator implements Comparator<SSTableReader> {
		private final Map<SSTableReader, Double> readRates;

		ReadRateComparator(Map<SSTableReader, Double> readRates) {
			this.readRates = readRates;
		}

		@Override
		public int compare(SSTableReader o1, SSTableReader o2) {
			Double readRate1 = readRates.get(o1);
			Double readRate2 = readRates.get(o2);
			if ((readRate1 == null) && (readRate2 == null))
				return 0;
			else
				if (readRate1 == null)
					return -1;
				else
					if (readRate2 == null)
						return 1;
					else
						return Double.compare(readRate1, readRate2);



		}
	}

	private static class ResampleEntry {
		public final SSTableReader sstable;

		public final long newSpaceUsed;

		public final int newSamplingLevel;

		ResampleEntry(SSTableReader sstable, long newSpaceUsed, int newSamplingLevel) {
			this.sstable = sstable;
			this.newSpaceUsed = newSpaceUsed;
			this.newSamplingLevel = newSamplingLevel;
		}
	}
}

