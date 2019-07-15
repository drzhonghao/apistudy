

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.compaction.AbstractCompactionStrategy;
import org.apache.cassandra.db.compaction.AbstractCompactionTask;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy;
import org.apache.cassandra.db.compaction.SizeTieredCompactionStrategyOptions;
import org.apache.cassandra.db.compaction.TimeWindowCompactionStrategyOptions;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.db.lifecycle.Tracker;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.schema.CompactionParams;
import org.apache.cassandra.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.schema.CompactionParams.Option.MAX_THRESHOLD;
import static org.apache.cassandra.schema.CompactionParams.Option.MIN_THRESHOLD;


public class TimeWindowCompactionStrategy extends AbstractCompactionStrategy {
	private static final Logger logger = LoggerFactory.getLogger(TimeWindowCompactionStrategy.class);

	private final TimeWindowCompactionStrategyOptions options;

	protected volatile int estimatedRemainingTasks;

	private final Set<SSTableReader> sstables = new HashSet<>();

	private long lastExpiredCheck;

	private long highestWindowSeen;

	public TimeWindowCompactionStrategy(ColumnFamilyStore cfs, Map<String, String> options) {
		super(cfs, options);
		this.estimatedRemainingTasks = 0;
		this.options = new TimeWindowCompactionStrategyOptions(options);
		if ((!(options.containsKey(AbstractCompactionStrategy.TOMBSTONE_COMPACTION_INTERVAL_OPTION))) && (!(options.containsKey(AbstractCompactionStrategy.TOMBSTONE_THRESHOLD_OPTION)))) {
			disableTombstoneCompactions = true;
			TimeWindowCompactionStrategy.logger.debug("Disabling tombstone compactions for TWCS");
		}else
			TimeWindowCompactionStrategy.logger.debug("Enabling tombstone compactions for TWCS");

	}

	@Override
	@SuppressWarnings("resource")
	public AbstractCompactionTask getNextBackgroundTask(int gcBefore) {
		List<SSTableReader> previousCandidate = null;
		while (true) {
			List<SSTableReader> latestBucket = getNextBackgroundSSTables(gcBefore);
			if (latestBucket.isEmpty())
				return null;

			if (latestBucket.equals(previousCandidate)) {
				TimeWindowCompactionStrategy.logger.warn(("Could not acquire references for compacting SSTables {} which is not a problem per se," + "unless it happens frequently, in which case it must be reported. Will retry later."), latestBucket);
				return null;
			}
			LifecycleTransaction modifier = cfs.getTracker().tryModify(latestBucket, OperationType.COMPACTION);
			if (modifier != null) {
			}
			previousCandidate = latestBucket;
		} 
	}

	private synchronized List<SSTableReader> getNextBackgroundSSTables(final int gcBefore) {
		if (Iterables.isEmpty(cfs.getSSTables(SSTableSet.LIVE)))
			return Collections.emptyList();

		Set<SSTableReader> uncompacting = ImmutableSet.copyOf(Iterables.filter(cfs.getUncompactingSSTables(), sstables::contains));
		Set<SSTableReader> expired = Collections.emptySet();
		Set<SSTableReader> candidates = Sets.newHashSet(AbstractCompactionStrategy.filterSuspectSSTables(uncompacting));
		List<SSTableReader> compactionCandidates = new ArrayList<>(getNextNonExpiredSSTables(Sets.difference(candidates, expired), gcBefore));
		if (!(expired.isEmpty())) {
			TimeWindowCompactionStrategy.logger.debug("Including expired sstables: {}", expired);
			compactionCandidates.addAll(expired);
		}
		return compactionCandidates;
	}

	private List<SSTableReader> getNextNonExpiredSSTables(Iterable<SSTableReader> nonExpiringSSTables, final int gcBefore) {
		List<SSTableReader> mostInteresting = getCompactionCandidates(nonExpiringSSTables);
		if (mostInteresting != null) {
			return mostInteresting;
		}
		List<SSTableReader> sstablesWithTombstones = new ArrayList<>();
		for (SSTableReader sstable : nonExpiringSSTables) {
			if (worthDroppingTombstones(sstable, gcBefore))
				sstablesWithTombstones.add(sstable);

		}
		if (sstablesWithTombstones.isEmpty())
			return Collections.emptyList();

		return Collections.singletonList(Collections.min(sstablesWithTombstones, SSTableReader.sizeComparator));
	}

	private List<SSTableReader> getCompactionCandidates(Iterable<SSTableReader> candidateSSTables) {
		return null;
	}

	@Override
	public void addSSTable(SSTableReader sstable) {
		sstables.add(sstable);
	}

	@Override
	public void removeSSTable(SSTableReader sstable) {
		sstables.remove(sstable);
	}

	public static Pair<Long, Long> getWindowBoundsInMillis(TimeUnit windowTimeUnit, int windowTimeSize, long timestampInMillis) {
		long lowerTimestamp;
		long upperTimestamp;
		long timestampInSeconds = TimeUnit.SECONDS.convert(timestampInMillis, TimeUnit.MILLISECONDS);
		switch (windowTimeUnit) {
			case MINUTES :
				lowerTimestamp = timestampInSeconds - (timestampInSeconds % (60L * windowTimeSize));
				upperTimestamp = (lowerTimestamp + (60L * (windowTimeSize - 1L))) + 59L;
				break;
			case HOURS :
				lowerTimestamp = timestampInSeconds - (timestampInSeconds % (3600L * windowTimeSize));
				upperTimestamp = (lowerTimestamp + (3600L * (windowTimeSize - 1L))) + 3599L;
				break;
			case DAYS :
			default :
				lowerTimestamp = timestampInSeconds - (timestampInSeconds % (86400L * windowTimeSize));
				upperTimestamp = (lowerTimestamp + (86400L * (windowTimeSize - 1L))) + 86399L;
				break;
		}
		return Pair.create(TimeUnit.MILLISECONDS.convert(lowerTimestamp, TimeUnit.SECONDS), TimeUnit.MILLISECONDS.convert(upperTimestamp, TimeUnit.SECONDS));
	}

	@com.google.common.annotations.VisibleForTesting
	static Pair<HashMultimap<Long, SSTableReader>, Long> getBuckets(Iterable<SSTableReader> files, TimeUnit sstableWindowUnit, int sstableWindowSize, TimeUnit timestampResolution) {
		HashMultimap<Long, SSTableReader> buckets = HashMultimap.create();
		long maxTimestamp = 0;
		for (SSTableReader f : files) {
			long tStamp = TimeUnit.MILLISECONDS.convert(f.getMaxTimestamp(), timestampResolution);
			Pair<Long, Long> bounds = TimeWindowCompactionStrategy.getWindowBoundsInMillis(sstableWindowUnit, sstableWindowSize, tStamp);
			buckets.put(bounds.left, f);
			if ((bounds.left) > maxTimestamp)
				maxTimestamp = bounds.left;

		}
		TimeWindowCompactionStrategy.logger.trace("buckets {}, max timestamp {}", buckets, maxTimestamp);
		return Pair.create(buckets, maxTimestamp);
	}

	private void updateEstimatedCompactionsByTasks(HashMultimap<Long, SSTableReader> tasks) {
		int n = 0;
		long now = this.highestWindowSeen;
		for (Long key : tasks.keySet()) {
			if (((key.compareTo(now)) >= 0) && ((tasks.get(key).size()) >= (cfs.getMinimumCompactionThreshold())))
				n++;
			else
				if (((key.compareTo(now)) < 0) && ((tasks.get(key).size()) >= 2))
					n++;


		}
		this.estimatedRemainingTasks = n;
	}

	@com.google.common.annotations.VisibleForTesting
	static List<SSTableReader> newestBucket(HashMultimap<Long, SSTableReader> buckets, int minThreshold, int maxThreshold, SizeTieredCompactionStrategyOptions stcsOptions, long now) {
		TreeSet<Long> allKeys = new TreeSet<>(buckets.keySet());
		Iterator<Long> it = allKeys.descendingIterator();
		while (it.hasNext()) {
			Long key = it.next();
			Set<SSTableReader> bucket = buckets.get(key);
			TimeWindowCompactionStrategy.logger.trace("Key {}, now {}", key, now);
			if (((bucket.size()) >= minThreshold) && (key >= now)) {
				List<Pair<SSTableReader, Long>> pairs = SizeTieredCompactionStrategy.createSSTableAndLengthPairs(bucket);
				TimeWindowCompactionStrategy.logger.debug("Using STCS compaction for first window of bucket: data files {} , options {}", pairs, stcsOptions);
			}else
				if (((bucket.size()) >= 2) && (key < now)) {
					TimeWindowCompactionStrategy.logger.debug("bucket size {} >= 2 and not in current bucket, compacting what's here: {}", bucket.size(), bucket);
					return TimeWindowCompactionStrategy.trimToThreshold(bucket, maxThreshold);
				}else {
					TimeWindowCompactionStrategy.logger.debug("No compaction necessary for bucket size {} , key {}, now {}", bucket.size(), key, now);
				}

		} 
		return Collections.<SSTableReader>emptyList();
	}

	@com.google.common.annotations.VisibleForTesting
	static List<SSTableReader> trimToThreshold(Set<SSTableReader> bucket, int maxThreshold) {
		List<SSTableReader> ssTableReaders = new ArrayList<>(bucket);
		Collections.sort(ssTableReaders, SSTableReader.sizeComparator);
		return ImmutableList.copyOf(Iterables.limit(ssTableReaders, maxThreshold));
	}

	@Override
	@SuppressWarnings("resource")
	public synchronized Collection<AbstractCompactionTask> getMaximalTask(int gcBefore, boolean splitOutput) {
		Iterable<SSTableReader> filteredSSTables = AbstractCompactionStrategy.filterSuspectSSTables(sstables);
		if (Iterables.isEmpty(filteredSSTables))
			return null;

		LifecycleTransaction txn = cfs.getTracker().tryModify(filteredSSTables, OperationType.COMPACTION);
		if (txn == null)
			return null;

		return null;
	}

	@Override
	@SuppressWarnings("resource")
	public synchronized AbstractCompactionTask getUserDefinedTask(Collection<SSTableReader> sstables, int gcBefore) {
		assert !(sstables.isEmpty());
		LifecycleTransaction modifier = cfs.getTracker().tryModify(sstables, OperationType.COMPACTION);
		if (modifier == null) {
			TimeWindowCompactionStrategy.logger.debug("Unable to mark {} for compaction; probably a background compaction got to it first.  You can disable background compactions temporarily if this is a problem", sstables);
			return null;
		}
		return null;
	}

	public int getEstimatedRemainingTasks() {
		return this.estimatedRemainingTasks;
	}

	public long getMaxSSTableBytes() {
		return Long.MAX_VALUE;
	}

	public static Map<String, String> validateOptions(Map<String, String> options) throws ConfigurationException {
		Map<String, String> uncheckedOptions = AbstractCompactionStrategy.validateOptions(options);
		uncheckedOptions = TimeWindowCompactionStrategyOptions.validateOptions(options, uncheckedOptions);
		uncheckedOptions.remove(MIN_THRESHOLD.toString());
		uncheckedOptions.remove(MAX_THRESHOLD.toString());
		return uncheckedOptions;
	}

	public String toString() {
		return String.format("TimeWindowCompactionStrategy[%s/%s]", cfs.getMinimumCompactionThreshold(), cfs.getMaximumCompactionThreshold());
	}
}

