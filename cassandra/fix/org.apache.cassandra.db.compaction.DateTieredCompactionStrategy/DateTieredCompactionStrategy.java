

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
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
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.compaction.AbstractCompactionStrategy;
import org.apache.cassandra.db.compaction.AbstractCompactionTask;
import org.apache.cassandra.db.compaction.CompactionLogger;
import org.apache.cassandra.db.compaction.CompactionStrategyManager;
import org.apache.cassandra.db.compaction.CompactionTask;
import org.apache.cassandra.db.compaction.DateTieredCompactionStrategyOptions;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy;
import org.apache.cassandra.db.compaction.SizeTieredCompactionStrategyOptions;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.db.lifecycle.Tracker;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.schema.CompactionParams;
import org.apache.cassandra.utils.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.schema.CompactionParams.Option.MAX_THRESHOLD;
import static org.apache.cassandra.schema.CompactionParams.Option.MIN_THRESHOLD;


@Deprecated
public class DateTieredCompactionStrategy extends AbstractCompactionStrategy {
	private static final Logger logger = LoggerFactory.getLogger(DateTieredCompactionStrategy.class);

	private final DateTieredCompactionStrategyOptions options;

	protected volatile int estimatedRemainingTasks;

	private final Set<SSTableReader> sstables = new HashSet<>();

	private long lastExpiredCheck;

	private final SizeTieredCompactionStrategyOptions stcsOptions;

	public DateTieredCompactionStrategy(ColumnFamilyStore cfs, Map<String, String> options) {
		super(cfs, options);
		this.estimatedRemainingTasks = 0;
		this.options = new DateTieredCompactionStrategyOptions(options);
		if ((!(options.containsKey(AbstractCompactionStrategy.TOMBSTONE_COMPACTION_INTERVAL_OPTION))) && (!(options.containsKey(AbstractCompactionStrategy.TOMBSTONE_THRESHOLD_OPTION)))) {
			disableTombstoneCompactions = true;
			DateTieredCompactionStrategy.logger.trace("Disabling tombstone compactions for DTCS");
		}else
			DateTieredCompactionStrategy.logger.trace("Enabling tombstone compactions for DTCS");

		this.stcsOptions = new SizeTieredCompactionStrategyOptions(options);
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
				DateTieredCompactionStrategy.logger.warn(("Could not acquire references for compacting SSTables {} which is not a problem per se," + "unless it happens frequently, in which case it must be reported. Will retry later."), latestBucket);
				return null;
			}
			LifecycleTransaction modifier = cfs.getTracker().tryModify(latestBucket, OperationType.COMPACTION);
			if (modifier != null)
				return new CompactionTask(cfs, modifier, gcBefore);

			previousCandidate = latestBucket;
		} 
	}

	private synchronized List<SSTableReader> getNextBackgroundSSTables(final int gcBefore) {
		if (sstables.isEmpty())
			return Collections.emptyList();

		Set<SSTableReader> uncompacting = ImmutableSet.copyOf(Iterables.filter(cfs.getUncompactingSSTables(), sstables::contains));
		Set<SSTableReader> expired = Collections.emptySet();
		Set<SSTableReader> candidates = Sets.newHashSet(AbstractCompactionStrategy.filterSuspectSSTables(uncompacting));
		List<SSTableReader> compactionCandidates = new ArrayList<>(getNextNonExpiredSSTables(Sets.difference(candidates, expired), gcBefore));
		if (!(expired.isEmpty())) {
			DateTieredCompactionStrategy.logger.trace("Including expired sstables: {}", expired);
			compactionCandidates.addAll(expired);
		}
		return compactionCandidates;
	}

	private List<SSTableReader> getNextNonExpiredSSTables(Iterable<SSTableReader> nonExpiringSSTables, final int gcBefore) {
		int base = cfs.getMinimumCompactionThreshold();
		long now = getNow();
		List<SSTableReader> mostInteresting = getCompactionCandidates(nonExpiringSSTables, now, base);
		if (mostInteresting != null) {
			return mostInteresting;
		}
		List<SSTableReader> sstablesWithTombstones = Lists.newArrayList();
		for (SSTableReader sstable : nonExpiringSSTables) {
			if (worthDroppingTombstones(sstable, gcBefore))
				sstablesWithTombstones.add(sstable);

		}
		if (sstablesWithTombstones.isEmpty())
			return Collections.emptyList();

		return Collections.singletonList(Collections.min(sstablesWithTombstones, SSTableReader.sizeComparator));
	}

	private List<SSTableReader> getCompactionCandidates(Iterable<SSTableReader> candidateSSTables, long now, int base) {
		return null;
	}

	private long getNow() {
		List<SSTableReader> list = new ArrayList<>();
		Iterables.addAll(list, cfs.getSSTables(SSTableSet.LIVE));
		if (list.isEmpty())
			return 0;

		return Collections.max(list, ( o1, o2) -> Long.compare(o1.getMaxTimestamp(), o2.getMaxTimestamp())).getMaxTimestamp();
	}

	@com.google.common.annotations.VisibleForTesting
	static Iterable<SSTableReader> filterOldSSTables(List<SSTableReader> sstables, long maxSSTableAge, long now) {
		if (maxSSTableAge == 0)
			return sstables;

		final long cutoff = now - maxSSTableAge;
		return Iterables.filter(sstables, new Predicate<SSTableReader>() {
			@Override
			public boolean apply(SSTableReader sstable) {
				return (sstable.getMaxTimestamp()) >= cutoff;
			}
		});
	}

	public static List<Pair<SSTableReader, Long>> createSSTableAndMinTimestampPairs(Iterable<SSTableReader> sstables) {
		List<Pair<SSTableReader, Long>> sstableMinTimestampPairs = Lists.newArrayListWithCapacity(Iterables.size(sstables));
		for (SSTableReader sstable : sstables)
			sstableMinTimestampPairs.add(Pair.create(sstable, sstable.getMinTimestamp()));

		return sstableMinTimestampPairs;
	}

	@Override
	public void addSSTable(SSTableReader sstable) {
		sstables.add(sstable);
	}

	@Override
	public void removeSSTable(SSTableReader sstable) {
		sstables.remove(sstable);
	}

	private static class Target {
		public final long size;

		public final long divPosition;

		public final long maxWindowSize;

		public Target(long size, long divPosition, long maxWindowSize) {
			this.size = size;
			this.divPosition = divPosition;
			this.maxWindowSize = maxWindowSize;
		}

		public int compareToTimestamp(long timestamp) {
			return Long.compare(divPosition, (timestamp / (size)));
		}

		public boolean onTarget(long timestamp) {
			return (compareToTimestamp(timestamp)) == 0;
		}

		public DateTieredCompactionStrategy.Target nextTarget(int base) {
			if ((((divPosition) % base) > 0) || (((size) * base) > (maxWindowSize)))
				return new DateTieredCompactionStrategy.Target(size, ((divPosition) - 1), maxWindowSize);
			else
				return new DateTieredCompactionStrategy.Target(((size) * base), (((divPosition) / base) - 1), maxWindowSize);

		}
	}

	@com.google.common.annotations.VisibleForTesting
	static <T> List<List<T>> getBuckets(Collection<Pair<T, Long>> files, long timeUnit, int base, long now, long maxWindowSize) {
		final List<Pair<T, Long>> sortedFiles = Lists.newArrayList(files);
		Collections.sort(sortedFiles, Collections.reverseOrder(new Comparator<Pair<T, Long>>() {
			public int compare(Pair<T, Long> p1, Pair<T, Long> p2) {
				return p1.right.compareTo(p2.right);
			}
		}));
		List<List<T>> buckets = Lists.newArrayList();
		DateTieredCompactionStrategy.Target target = DateTieredCompactionStrategy.getInitialTarget(now, timeUnit, maxWindowSize);
		PeekingIterator<Pair<T, Long>> it = Iterators.peekingIterator(sortedFiles.iterator());
		outerLoop : while (it.hasNext()) {
			while (!(target.onTarget(it.peek().right))) {
				if ((target.compareToTimestamp(it.peek().right)) < 0) {
					it.next();
					if (!(it.hasNext()))
						break outerLoop;

				}else
					target = target.nextTarget(base);

			} 
			List<T> bucket = Lists.newArrayList();
			while (target.onTarget(it.peek().right)) {
				bucket.add(it.next().left);
				if (!(it.hasNext()))
					break;

			} 
			buckets.add(bucket);
		} 
		return buckets;
	}

	@com.google.common.annotations.VisibleForTesting
	static DateTieredCompactionStrategy.Target getInitialTarget(long now, long timeUnit, long maxWindowSize) {
		return new DateTieredCompactionStrategy.Target(timeUnit, (now / timeUnit), maxWindowSize);
	}

	private void updateEstimatedCompactionsByTasks(List<List<SSTableReader>> tasks) {
		int n = 0;
		for (List<SSTableReader> bucket : tasks) {
			for (List<SSTableReader> stcsBucket : DateTieredCompactionStrategy.getSTCSBuckets(bucket, stcsOptions))
				if ((stcsBucket.size()) >= (cfs.getMinimumCompactionThreshold()))
					n += Math.ceil((((double) (stcsBucket.size())) / (cfs.getMaximumCompactionThreshold())));


		}
		estimatedRemainingTasks = n;
		cfs.getCompactionStrategyManager().compactionLogger.pending(this, n);
	}

	@com.google.common.annotations.VisibleForTesting
	static List<SSTableReader> newestBucket(List<List<SSTableReader>> buckets, int minThreshold, int maxThreshold, long now, long baseTime, long maxWindowSize, SizeTieredCompactionStrategyOptions stcsOptions) {
		DateTieredCompactionStrategy.Target incomingWindow = DateTieredCompactionStrategy.getInitialTarget(now, baseTime, maxWindowSize);
		for (List<SSTableReader> bucket : buckets) {
			boolean inFirstWindow = incomingWindow.onTarget(bucket.get(0).getMinTimestamp());
			if (((bucket.size()) >= minThreshold) || (((bucket.size()) >= 2) && (!inFirstWindow))) {
				List<SSTableReader> stcsSSTables = DateTieredCompactionStrategy.getSSTablesForSTCS(bucket, (inFirstWindow ? minThreshold : 2), maxThreshold, stcsOptions);
				if (!(stcsSSTables.isEmpty()))
					return stcsSSTables;

			}
		}
		return Collections.emptyList();
	}

	private static List<SSTableReader> getSSTablesForSTCS(Collection<SSTableReader> sstables, int minThreshold, int maxThreshold, SizeTieredCompactionStrategyOptions stcsOptions) {
		List<SSTableReader> s = SizeTieredCompactionStrategy.mostInterestingBucket(DateTieredCompactionStrategy.getSTCSBuckets(sstables, stcsOptions), minThreshold, maxThreshold);
		DateTieredCompactionStrategy.logger.debug("Got sstables {} for STCS from {}", s, sstables);
		return s;
	}

	private static List<List<SSTableReader>> getSTCSBuckets(Collection<SSTableReader> sstables, SizeTieredCompactionStrategyOptions stcsOptions) {
		List<Pair<SSTableReader, Long>> pairs = SizeTieredCompactionStrategy.createSSTableAndLengthPairs(AbstractCompactionStrategy.filterSuspectSSTables(sstables));
		return null;
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

		return Collections.<AbstractCompactionTask>singleton(new CompactionTask(cfs, txn, gcBefore));
	}

	@Override
	@SuppressWarnings("resource")
	public synchronized AbstractCompactionTask getUserDefinedTask(Collection<SSTableReader> sstables, int gcBefore) {
		assert !(sstables.isEmpty());
		LifecycleTransaction modifier = cfs.getTracker().tryModify(sstables, OperationType.COMPACTION);
		if (modifier == null) {
			DateTieredCompactionStrategy.logger.trace("Unable to mark {} for compaction; probably a background compaction got to it first.  You can disable background compactions temporarily if this is a problem", sstables);
			return null;
		}
		return new CompactionTask(cfs, modifier, gcBefore).setUserDefined(true);
	}

	public int getEstimatedRemainingTasks() {
		return estimatedRemainingTasks;
	}

	public long getMaxSSTableBytes() {
		return Long.MAX_VALUE;
	}

	@Override
	public Collection<Collection<SSTableReader>> groupSSTablesForAntiCompaction(Collection<SSTableReader> sstablesToGroup) {
		Collection<Collection<SSTableReader>> groups = new ArrayList<>();
		for (SSTableReader sstable : sstablesToGroup) {
			groups.add(Collections.singleton(sstable));
		}
		return groups;
	}

	public static Map<String, String> validateOptions(Map<String, String> options) throws ConfigurationException {
		Map<String, String> uncheckedOptions = AbstractCompactionStrategy.validateOptions(options);
		uncheckedOptions = DateTieredCompactionStrategyOptions.validateOptions(options, uncheckedOptions);
		uncheckedOptions.remove(MIN_THRESHOLD.toString());
		uncheckedOptions.remove(MAX_THRESHOLD.toString());
		uncheckedOptions = SizeTieredCompactionStrategyOptions.validateOptions(options, uncheckedOptions);
		return uncheckedOptions;
	}

	public CompactionLogger.Strategy strategyLogger() {
		return new CompactionLogger.Strategy() {
			public JsonNode sstable(SSTableReader sstable) {
				ObjectNode node = JsonNodeFactory.instance.objectNode();
				node.put("min_timestamp", sstable.getMinTimestamp());
				node.put("max_timestamp", sstable.getMaxTimestamp());
				return node;
			}

			public JsonNode options() {
				ObjectNode node = JsonNodeFactory.instance.objectNode();
				return node;
			}
		};
	}

	public String toString() {
		return String.format("DateTieredCompactionStrategy[%s/%s]", cfs.getMinimumCompactionThreshold(), cfs.getMaximumCompactionThreshold());
	}
}

