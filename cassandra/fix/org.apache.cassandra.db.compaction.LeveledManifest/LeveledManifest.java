

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.util.AbstractCollection;
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
import java.util.Set;
import java.util.SortedSet;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.compaction.AbstractCompactionStrategy;
import org.apache.cassandra.db.compaction.CompactionStrategyManager;
import org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy;
import org.apache.cassandra.db.compaction.SizeTieredCompactionStrategyOptions;
import org.apache.cassandra.db.lifecycle.Tracker;
import org.apache.cassandra.dht.Bounds;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.RingPosition;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.io.sstable.Component;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.metadata.IMetadataSerializer;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LeveledManifest {
	private static final Logger logger = LoggerFactory.getLogger(LeveledManifest.class);

	private static final int MAX_COMPACTING_L0 = 32;

	private static final int NO_COMPACTION_LIMIT = 25;

	public static final int MAX_LEVEL_COUNT = ((int) (Math.log10(((1000 * 1000) * 1000))));

	private final ColumnFamilyStore cfs;

	@VisibleForTesting
	protected final List<SSTableReader>[] generations;

	private final PartitionPosition[] lastCompactedKeys;

	private final long maxSSTableSizeInBytes;

	private final SizeTieredCompactionStrategyOptions options;

	private final int[] compactionCounter;

	private final int levelFanoutSize;

	LeveledManifest(ColumnFamilyStore cfs, int maxSSTableSizeInMB, int fanoutSize, SizeTieredCompactionStrategyOptions options) {
		this.cfs = cfs;
		this.maxSSTableSizeInBytes = (maxSSTableSizeInMB * 1024L) * 1024L;
		this.options = options;
		this.levelFanoutSize = fanoutSize;
		generations = new List[LeveledManifest.MAX_LEVEL_COUNT];
		lastCompactedKeys = new PartitionPosition[LeveledManifest.MAX_LEVEL_COUNT];
		for (int i = 0; i < (generations.length); i++) {
			generations[i] = new ArrayList<>();
			lastCompactedKeys[i] = cfs.getPartitioner().getMinimumToken().minKeyBound();
		}
		compactionCounter = new int[LeveledManifest.MAX_LEVEL_COUNT];
	}

	public static LeveledManifest create(ColumnFamilyStore cfs, int maxSSTableSize, int fanoutSize, List<SSTableReader> sstables) {
		return LeveledManifest.create(cfs, maxSSTableSize, fanoutSize, sstables, new SizeTieredCompactionStrategyOptions());
	}

	public static LeveledManifest create(ColumnFamilyStore cfs, int maxSSTableSize, int fanoutSize, Iterable<SSTableReader> sstables, SizeTieredCompactionStrategyOptions options) {
		LeveledManifest manifest = new LeveledManifest(cfs, maxSSTableSize, fanoutSize, options);
		for (SSTableReader ssTableReader : sstables) {
			manifest.add(ssTableReader);
		}
		for (int i = 1; i < (manifest.getAllLevelSize().length); i++) {
			manifest.repairOverlappingSSTables(i);
		}
		manifest.calculateLastCompactedKeys();
		return manifest;
	}

	public void calculateLastCompactedKeys() {
		for (int i = 0; i < ((generations.length) - 1); i++) {
			if (generations[(i + 1)].isEmpty())
				continue;

			SSTableReader sstableWithMaxModificationTime = null;
			long maxModificationTime = Long.MIN_VALUE;
			for (SSTableReader ssTableReader : generations[(i + 1)]) {
				long modificationTime = ssTableReader.getCreationTimeFor(Component.DATA);
				if (modificationTime >= maxModificationTime) {
					sstableWithMaxModificationTime = ssTableReader;
					maxModificationTime = modificationTime;
				}
			}
			lastCompactedKeys[i] = sstableWithMaxModificationTime.last;
		}
	}

	public synchronized void add(SSTableReader reader) {
		int level = reader.getSSTableLevel();
		assert level < (generations.length) : (("Invalid level " + level) + " out of ") + ((generations.length) - 1);
		logDistribution();
		if (canAddSSTable(reader)) {
			LeveledManifest.logger.trace("Adding {} to L{}", reader, level);
			generations[level].add(reader);
		}else {
			try {
				reader.descriptor.getMetadataSerializer().mutateLevel(reader.descriptor, 0);
				reader.reloadSSTableMetadata();
			} catch (IOException e) {
				LeveledManifest.logger.error("Could not change sstable level - adding it at level 0 anyway, we will find it at restart.", e);
			}
			if (!(contains(reader))) {
				generations[0].add(reader);
			}else {
				LeveledManifest.logger.warn("SSTable {} is already present on leveled manifest and should not be re-added.", reader, new RuntimeException());
			}
		}
	}

	private boolean contains(SSTableReader reader) {
		for (int i = 0; i < (generations.length); i++) {
			if (generations[i].contains(reader))
				return true;

		}
		return false;
	}

	public synchronized void replace(Collection<SSTableReader> removed, Collection<SSTableReader> added) {
		assert !(removed.isEmpty());
		logDistribution();
		if (LeveledManifest.logger.isTraceEnabled())
			LeveledManifest.logger.trace("Replacing [{}]", toString(removed));

		int minLevel = Integer.MAX_VALUE;
		for (SSTableReader sstable : removed) {
			int thisLevel = remove(sstable);
			minLevel = Math.min(minLevel, thisLevel);
		}
		if (added.isEmpty())
			return;

		if (LeveledManifest.logger.isTraceEnabled())
			LeveledManifest.logger.trace("Adding [{}]", toString(added));

		for (SSTableReader ssTableReader : added)
			add(ssTableReader);

		lastCompactedKeys[minLevel] = SSTableReader.sstableOrdering.max(added).last;
	}

	public synchronized void repairOverlappingSSTables(int level) {
		SSTableReader previous = null;
		Collections.sort(generations[level], SSTableReader.sstableComparator);
		List<SSTableReader> outOfOrderSSTables = new ArrayList<>();
		for (SSTableReader current : generations[level]) {
			if ((previous != null) && ((current.first.compareTo(previous.last)) <= 0)) {
				LeveledManifest.logger.warn(("At level {}, {} [{}, {}] overlaps {} [{}, {}].  This could be caused by a bug in Cassandra 1.1.0 .. 1.1.3 or due to the fact that you have dropped sstables from another node into the data directory. " + "Sending back to L0.  If you didn't drop in sstables, and have not yet run scrub, you should do so since you may also have rows out-of-order within an sstable"), level, previous, previous.first, previous.last, current, current.first, current.last);
				outOfOrderSSTables.add(current);
			}else {
				previous = current;
			}
		}
		if (!(outOfOrderSSTables.isEmpty())) {
			for (SSTableReader sstable : outOfOrderSSTables)
				sendBackToL0(sstable);

		}
	}

	private boolean canAddSSTable(SSTableReader sstable) {
		int level = sstable.getSSTableLevel();
		if (level == 0)
			return true;

		List<SSTableReader> copyLevel = new ArrayList<>(generations[level]);
		copyLevel.add(sstable);
		Collections.sort(copyLevel, SSTableReader.sstableComparator);
		SSTableReader previous = null;
		for (SSTableReader current : copyLevel) {
			if ((previous != null) && ((current.first.compareTo(previous.last)) <= 0))
				return false;

			previous = current;
		}
		return true;
	}

	private synchronized void sendBackToL0(SSTableReader sstable) {
		remove(sstable);
		try {
			sstable.descriptor.getMetadataSerializer().mutateLevel(sstable.descriptor, 0);
			sstable.reloadSSTableMetadata();
			add(sstable);
		} catch (IOException e) {
			throw new RuntimeException("Could not reload sstable meta data", e);
		}
	}

	private String toString(Collection<SSTableReader> sstables) {
		StringBuilder builder = new StringBuilder();
		for (SSTableReader sstable : sstables) {
			builder.append(sstable.descriptor.cfname).append('-').append(sstable.descriptor.generation).append("(L").append(sstable.getSSTableLevel()).append("), ");
		}
		return builder.toString();
	}

	public long maxBytesForLevel(int level, long maxSSTableSizeInBytes) {
		return LeveledManifest.maxBytesForLevel(level, levelFanoutSize, maxSSTableSizeInBytes);
	}

	public static long maxBytesForLevel(int level, int levelFanoutSize, long maxSSTableSizeInBytes) {
		if (level == 0)
			return 4L * maxSSTableSizeInBytes;

		double bytes = (Math.pow(levelFanoutSize, level)) * maxSSTableSizeInBytes;
		if (bytes > (Long.MAX_VALUE))
			throw new RuntimeException(((("At most " + (Long.MAX_VALUE)) + " bytes may be in a compaction level; your maxSSTableSize must be absurdly high to compute ") + bytes));

		return ((long) (bytes));
	}

	public synchronized LeveledManifest.CompactionCandidate getCompactionCandidates() {
		if (StorageService.instance.isBootstrapMode()) {
			List<SSTableReader> mostInteresting = getSSTablesForSTCS(getLevel(0));
			if (!(mostInteresting.isEmpty())) {
				LeveledManifest.logger.info("Bootstrapping - doing STCS in L0");
				return new LeveledManifest.CompactionCandidate(mostInteresting, 0, Long.MAX_VALUE);
			}
			return null;
		}
		for (int i = (generations.length) - 1; i > 0; i--) {
			List<SSTableReader> sstables = getLevel(i);
			if (sstables.isEmpty())
				continue;

			Set<SSTableReader> sstablesInLevel = Sets.newHashSet(sstables);
			Set<SSTableReader> remaining = Sets.difference(sstablesInLevel, cfs.getTracker().getCompacting());
			double score = ((double) (SSTableReader.getTotalBytes(remaining))) / ((double) (maxBytesForLevel(i, maxSSTableSizeInBytes)));
			LeveledManifest.logger.trace("Compaction score for level {} is {}", i, score);
			if (score > 1.001) {
				LeveledManifest.CompactionCandidate l0Compaction = getSTCSInL0CompactionCandidate();
				if (l0Compaction != null)
					return l0Compaction;

				Collection<SSTableReader> candidates = getCandidatesFor(i);
				if (!(candidates.isEmpty())) {
					int nextLevel = getNextLevel(candidates);
					candidates = getOverlappingStarvedSSTables(nextLevel, candidates);
					if (LeveledManifest.logger.isTraceEnabled())
						LeveledManifest.logger.trace("Compaction candidates for L{} are {}", i, toString(candidates));

					return new LeveledManifest.CompactionCandidate(candidates, nextLevel, cfs.getCompactionStrategyManager().getMaxSSTableBytes());
				}else {
					LeveledManifest.logger.trace("No compaction candidates for L{}", i);
				}
			}
		}
		if (getLevel(0).isEmpty())
			return null;

		Collection<SSTableReader> candidates = getCandidatesFor(0);
		if (candidates.isEmpty()) {
			return getSTCSInL0CompactionCandidate();
		}
		return new LeveledManifest.CompactionCandidate(candidates, getNextLevel(candidates), maxSSTableSizeInBytes);
	}

	private LeveledManifest.CompactionCandidate getSTCSInL0CompactionCandidate() {
		if ((!(DatabaseDescriptor.getDisableSTCSInL0())) && ((getLevel(0).size()) > (LeveledManifest.MAX_COMPACTING_L0))) {
			List<SSTableReader> mostInteresting = getSSTablesForSTCS(getLevel(0));
			if (!(mostInteresting.isEmpty())) {
				LeveledManifest.logger.debug("L0 is too far behind, performing size-tiering there first");
				return new LeveledManifest.CompactionCandidate(mostInteresting, 0, Long.MAX_VALUE);
			}
		}
		return null;
	}

	private List<SSTableReader> getSSTablesForSTCS(Collection<SSTableReader> sstables) {
		Iterable<SSTableReader> candidates = cfs.getTracker().getUncompacting(sstables);
		List<Pair<SSTableReader, Long>> pairs = SizeTieredCompactionStrategy.createSSTableAndLengthPairs(AbstractCompactionStrategy.filterSuspectSSTables(candidates));
		return null;
	}

	private Collection<SSTableReader> getOverlappingStarvedSSTables(int targetLevel, Collection<SSTableReader> candidates) {
		Set<SSTableReader> withStarvedCandidate = new HashSet<>(candidates);
		for (int i = (generations.length) - 1; i > 0; i--)
			(compactionCounter[i])++;

		compactionCounter[targetLevel] = 0;
		if (LeveledManifest.logger.isTraceEnabled()) {
			for (int j = 0; j < (compactionCounter.length); j++)
				LeveledManifest.logger.trace("CompactionCounter: {}: {}", j, compactionCounter[j]);

		}
		for (int i = (generations.length) - 1; i > 0; i--) {
			if ((getLevelSize(i)) > 0) {
				if ((compactionCounter[i]) > (LeveledManifest.NO_COMPACTION_LIMIT)) {
					PartitionPosition max = null;
					PartitionPosition min = null;
					for (SSTableReader candidate : candidates) {
						if ((min == null) || ((candidate.first.compareTo(min)) < 0))
							min = candidate.first;

						if ((max == null) || ((candidate.last.compareTo(max)) > 0))
							max = candidate.last;

					}
					if (((min == null) || (max == null)) || (min.equals(max)))
						return candidates;

					Set<SSTableReader> compacting = cfs.getTracker().getCompacting();
					Range<PartitionPosition> boundaries = new Range<>(min, max);
					for (SSTableReader sstable : getLevel(i)) {
						Range<PartitionPosition> r = new Range<PartitionPosition>(sstable.first, sstable.last);
						if ((boundaries.contains(r)) && (!(compacting.contains(sstable)))) {
							LeveledManifest.logger.info("Adding high-level (L{}) {} to candidates", sstable.getSSTableLevel(), sstable);
							withStarvedCandidate.add(sstable);
							return withStarvedCandidate;
						}
					}
				}
				return candidates;
			}
		}
		return candidates;
	}

	public synchronized int getLevelSize(int i) {
		if (i >= (generations.length))
			throw new ArrayIndexOutOfBoundsException(("Maximum valid generation is " + ((generations.length) - 1)));

		return getLevel(i).size();
	}

	public synchronized int[] getAllLevelSize() {
		int[] counts = new int[generations.length];
		for (int i = 0; i < (counts.length); i++)
			counts[i] = getLevel(i).size();

		return counts;
	}

	private void logDistribution() {
		if (LeveledManifest.logger.isTraceEnabled()) {
			for (int i = 0; i < (generations.length); i++) {
				if (!(getLevel(i).isEmpty())) {
					LeveledManifest.logger.trace("L{} contains {} SSTables ({}) in {}", i, getLevel(i).size(), FBUtilities.prettyPrintMemory(SSTableReader.getTotalBytes(getLevel(i))), this);
				}
			}
		}
	}

	@VisibleForTesting
	public synchronized int remove(SSTableReader reader) {
		int level = reader.getSSTableLevel();
		assert level >= 0 : (reader + " not present in manifest: ") + level;
		generations[level].remove(reader);
		return level;
	}

	private static Set<SSTableReader> overlapping(Collection<SSTableReader> candidates, Iterable<SSTableReader> others) {
		assert !(candidates.isEmpty());
		Iterator<SSTableReader> iter = candidates.iterator();
		SSTableReader sstable = iter.next();
		Token first = sstable.first.getToken();
		Token last = sstable.last.getToken();
		while (iter.hasNext()) {
			sstable = iter.next();
			first = ((first.compareTo(sstable.first.getToken())) <= 0) ? first : sstable.first.getToken();
			last = ((last.compareTo(sstable.last.getToken())) >= 0) ? last : sstable.last.getToken();
		} 
		return LeveledManifest.overlapping(first, last, others);
	}

	private static Set<SSTableReader> overlappingWithBounds(SSTableReader sstable, Map<SSTableReader, Bounds<Token>> others) {
		return LeveledManifest.overlappingWithBounds(sstable.first.getToken(), sstable.last.getToken(), others);
	}

	@VisibleForTesting
	static Set<SSTableReader> overlapping(Token start, Token end, Iterable<SSTableReader> sstables) {
		return LeveledManifest.overlappingWithBounds(start, end, LeveledManifest.genBounds(sstables));
	}

	private static Set<SSTableReader> overlappingWithBounds(Token start, Token end, Map<SSTableReader, Bounds<Token>> sstables) {
		assert (start.compareTo(end)) <= 0;
		Set<SSTableReader> overlapped = new HashSet<>();
		Bounds<Token> promotedBounds = new Bounds<Token>(start, end);
		for (Map.Entry<SSTableReader, Bounds<Token>> pair : sstables.entrySet()) {
			if (pair.getValue().intersects(promotedBounds))
				overlapped.add(pair.getKey());

		}
		return overlapped;
	}

	private static final Predicate<SSTableReader> suspectP = new Predicate<SSTableReader>() {
		public boolean apply(SSTableReader candidate) {
			return candidate.isMarkedSuspect();
		}
	};

	private static Map<SSTableReader, Bounds<Token>> genBounds(Iterable<SSTableReader> ssTableReaders) {
		Map<SSTableReader, Bounds<Token>> boundsMap = new HashMap<>();
		for (SSTableReader sstable : ssTableReaders) {
			boundsMap.put(sstable, new Bounds<Token>(sstable.first.getToken(), sstable.last.getToken()));
		}
		return boundsMap;
	}

	private Collection<SSTableReader> getCandidatesFor(int level) {
		assert !(getLevel(level).isEmpty());
		LeveledManifest.logger.trace("Choosing candidates for L{}", level);
		final Set<SSTableReader> compacting = cfs.getTracker().getCompacting();
		if (level == 0) {
			Set<SSTableReader> compactingL0 = getCompacting(0);
			PartitionPosition lastCompactingKey = null;
			PartitionPosition firstCompactingKey = null;
			for (SSTableReader candidate : compactingL0) {
				if ((firstCompactingKey == null) || ((candidate.first.compareTo(firstCompactingKey)) < 0))
					firstCompactingKey = candidate.first;

				if ((lastCompactingKey == null) || ((candidate.last.compareTo(lastCompactingKey)) > 0))
					lastCompactingKey = candidate.last;

			}
			Set<SSTableReader> candidates = new HashSet<>();
			Map<SSTableReader, Bounds<Token>> remaining = LeveledManifest.genBounds(Iterables.filter(getLevel(0), Predicates.not(LeveledManifest.suspectP)));
			for (SSTableReader sstable : ageSortedSSTables(remaining.keySet())) {
				if (candidates.contains(sstable))
					continue;

				Sets.SetView<SSTableReader> overlappedL0 = Sets.union(Collections.singleton(sstable), LeveledManifest.overlappingWithBounds(sstable, remaining));
				if (!(Sets.intersection(overlappedL0, compactingL0).isEmpty()))
					continue;

				for (SSTableReader newCandidate : overlappedL0) {
					if (((firstCompactingKey == null) || (lastCompactingKey == null)) || ((LeveledManifest.overlapping(firstCompactingKey.getToken(), lastCompactingKey.getToken(), Arrays.asList(newCandidate)).size()) == 0))
						candidates.add(newCandidate);

					remaining.remove(newCandidate);
				}
				if ((candidates.size()) > (LeveledManifest.MAX_COMPACTING_L0)) {
					candidates = new HashSet<>(ageSortedSSTables(candidates).subList(0, LeveledManifest.MAX_COMPACTING_L0));
					break;
				}
			}
			if ((SSTableReader.getTotalBytes(candidates)) > (maxSSTableSizeInBytes)) {
				Set<SSTableReader> l1overlapping = LeveledManifest.overlapping(candidates, getLevel(1));
				if ((Sets.intersection(l1overlapping, compacting).size()) > 0)
					return Collections.emptyList();

				if (!(LeveledManifest.overlapping(candidates, compactingL0).isEmpty()))
					return Collections.emptyList();

				candidates = Sets.union(candidates, l1overlapping);
			}
			if ((candidates.size()) < 2)
				return Collections.emptyList();
			else
				return candidates;

		}
		Collections.sort(getLevel(level), SSTableReader.sstableComparator);
		int start = 0;
		for (int i = 0; i < (getLevel(level).size()); i++) {
			SSTableReader sstable = getLevel(level).get(i);
			if ((sstable.first.compareTo(lastCompactedKeys[level])) > 0) {
				start = i;
				break;
			}
		}
		Map<SSTableReader, Bounds<Token>> sstablesNextLevel = LeveledManifest.genBounds(getLevel((level + 1)));
		for (int i = 0; i < (getLevel(level).size()); i++) {
			SSTableReader sstable = getLevel(level).get(((start + i) % (getLevel(level).size())));
			Set<SSTableReader> candidates = Sets.union(Collections.singleton(sstable), LeveledManifest.overlappingWithBounds(sstable, sstablesNextLevel));
			if (Iterables.any(candidates, LeveledManifest.suspectP))
				continue;

			if (Sets.intersection(candidates, compacting).isEmpty())
				return candidates;

		}
		return Collections.emptyList();
	}

	private Set<SSTableReader> getCompacting(int level) {
		Set<SSTableReader> sstables = new HashSet<>();
		Set<SSTableReader> levelSSTables = new HashSet<>(getLevel(level));
		for (SSTableReader sstable : cfs.getTracker().getCompacting()) {
			if (levelSSTables.contains(sstable))
				sstables.add(sstable);

		}
		return sstables;
	}

	private List<SSTableReader> ageSortedSSTables(Collection<SSTableReader> candidates) {
		List<SSTableReader> ageSortedCandidates = new ArrayList<>(candidates);
		Collections.sort(ageSortedCandidates, SSTableReader.maxTimestampComparator);
		return ageSortedCandidates;
	}

	public synchronized Set<SSTableReader>[] getSStablesPerLevelSnapshot() {
		Set<SSTableReader>[] sstablesPerLevel = new Set[generations.length];
		for (int i = 0; i < (generations.length); i++) {
			sstablesPerLevel[i] = new HashSet<>(generations[i]);
		}
		return sstablesPerLevel;
	}

	@Override
	public String toString() {
		return "Manifest@" + (hashCode());
	}

	public int getLevelCount() {
		for (int i = (generations.length) - 1; i >= 0; i--) {
			if ((getLevel(i).size()) > 0)
				return i;

		}
		return 0;
	}

	public synchronized SortedSet<SSTableReader> getLevelSorted(int level, Comparator<SSTableReader> comparator) {
		return ImmutableSortedSet.copyOf(comparator, getLevel(level));
	}

	public List<SSTableReader> getLevel(int i) {
		return generations[i];
	}

	public synchronized int getEstimatedTasks() {
		long tasks = 0;
		long[] estimated = new long[generations.length];
		for (int i = (generations.length) - 1; i >= 0; i--) {
			List<SSTableReader> sstables = getLevel(i);
			estimated[i] = ((long) (Math.ceil((((double) (Math.max(0L, ((SSTableReader.getTotalBytes(sstables)) - ((long) ((maxBytesForLevel(i, maxSSTableSizeInBytes)) * 1.001)))))) / ((double) (maxSSTableSizeInBytes))))));
			tasks += estimated[i];
		}
		LeveledManifest.logger.trace("Estimating {} compactions to do for {}.{}", Arrays.toString(estimated), cfs.keyspace.getName(), cfs.name);
		return Ints.checkedCast(tasks);
	}

	public int getNextLevel(Collection<SSTableReader> sstables) {
		int maximumLevel = Integer.MIN_VALUE;
		int minimumLevel = Integer.MAX_VALUE;
		for (SSTableReader sstable : sstables) {
			maximumLevel = Math.max(sstable.getSSTableLevel(), maximumLevel);
			minimumLevel = Math.min(sstable.getSSTableLevel(), minimumLevel);
		}
		int newLevel;
		if (((minimumLevel == 0) && (minimumLevel == maximumLevel)) && ((SSTableReader.getTotalBytes(sstables)) < (maxSSTableSizeInBytes))) {
			newLevel = 0;
		}else {
			newLevel = (minimumLevel == maximumLevel) ? maximumLevel + 1 : maximumLevel;
			assert newLevel > 0;
		}
		return newLevel;
	}

	public Iterable<SSTableReader> getAllSSTables() {
		Set<SSTableReader> sstables = new HashSet<>();
		for (List<SSTableReader> generation : generations) {
			sstables.addAll(generation);
		}
		return sstables;
	}

	public static class CompactionCandidate {
		public final Collection<SSTableReader> sstables;

		public final int level;

		public final long maxSSTableBytes;

		public CompactionCandidate(Collection<SSTableReader> sstables, int level, long maxSSTableBytes) {
			this.sstables = sstables;
			this.level = level;
			this.maxSSTableBytes = maxSSTableBytes;
		}
	}
}

