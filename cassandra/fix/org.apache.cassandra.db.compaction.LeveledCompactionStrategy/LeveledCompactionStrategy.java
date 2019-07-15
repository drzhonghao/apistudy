

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Doubles;
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
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.compaction.AbstractCompactionStrategy;
import org.apache.cassandra.db.compaction.AbstractCompactionTask;
import org.apache.cassandra.db.compaction.CompactionLogger;
import org.apache.cassandra.db.compaction.CompactionStrategyManager;
import org.apache.cassandra.db.compaction.LeveledCompactionTask;
import org.apache.cassandra.db.compaction.LeveledManifest;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.compaction.SizeTieredCompactionStrategyOptions;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.lifecycle.Tracker;
import org.apache.cassandra.db.partitions.BasePartitionIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.sstable.ISSTableScanner;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LeveledCompactionStrategy extends AbstractCompactionStrategy {
	private static final Logger logger = LoggerFactory.getLogger(LeveledCompactionStrategy.class);

	private static final String SSTABLE_SIZE_OPTION = "sstable_size_in_mb";

	private static final boolean tolerateSstableSize = Boolean.getBoolean(((Config.PROPERTY_PREFIX) + "tolerate_sstable_size"));

	private static final String LEVEL_FANOUT_SIZE_OPTION = "fanout_size";

	public static final int DEFAULT_LEVEL_FANOUT_SIZE = 10;

	@VisibleForTesting
	final LeveledManifest manifest = null;

	private final int maxSSTableSizeInMB;

	private final int levelFanoutSize;

	public LeveledCompactionStrategy(ColumnFamilyStore cfs, Map<String, String> options) {
		super(cfs, options);
		int configuredMaxSSTableSize = 160;
		int configuredLevelFanoutSize = LeveledCompactionStrategy.DEFAULT_LEVEL_FANOUT_SIZE;
		SizeTieredCompactionStrategyOptions localOptions = new SizeTieredCompactionStrategyOptions(options);
		if (options != null) {
			if (options.containsKey(LeveledCompactionStrategy.SSTABLE_SIZE_OPTION)) {
				configuredMaxSSTableSize = Integer.parseInt(options.get(LeveledCompactionStrategy.SSTABLE_SIZE_OPTION));
				if (!(LeveledCompactionStrategy.tolerateSstableSize)) {
					if (configuredMaxSSTableSize >= 1000)
						LeveledCompactionStrategy.logger.warn("Max sstable size of {}MB is configured for {}.{}; having a unit of compaction this large is probably a bad idea", configuredMaxSSTableSize, cfs.name, cfs.getColumnFamilyName());

					if (configuredMaxSSTableSize < 50)
						LeveledCompactionStrategy.logger.warn("Max sstable size of {}MB is configured for {}.{}.  Testing done for CASSANDRA-5727 indicates that performance improves up to 160MB", configuredMaxSSTableSize, cfs.name, cfs.getColumnFamilyName());

				}
			}
			if (options.containsKey(LeveledCompactionStrategy.LEVEL_FANOUT_SIZE_OPTION)) {
				configuredLevelFanoutSize = Integer.parseInt(options.get(LeveledCompactionStrategy.LEVEL_FANOUT_SIZE_OPTION));
			}
		}
		maxSSTableSizeInMB = configuredMaxSSTableSize;
		levelFanoutSize = configuredLevelFanoutSize;
		LeveledCompactionStrategy.logger.trace("Created {}", manifest);
	}

	public int getLevelSize(int i) {
		return manifest.getLevelSize(i);
	}

	public int[] getAllLevelSize() {
		return manifest.getAllLevelSize();
	}

	@Override
	public void startup() {
		manifest.calculateLastCompactedKeys();
		super.startup();
	}

	@SuppressWarnings("resource")
	public AbstractCompactionTask getNextBackgroundTask(int gcBefore) {
		Collection<SSTableReader> previousCandidate = null;
		while (true) {
			OperationType op;
			LeveledManifest.CompactionCandidate candidate = manifest.getCompactionCandidates();
			if (candidate == null) {
				SSTableReader sstable = findDroppableSSTable(gcBefore);
				if (sstable == null) {
					LeveledCompactionStrategy.logger.trace("No compaction necessary for {}", this);
					return null;
				}
				candidate = new LeveledManifest.CompactionCandidate(Collections.singleton(sstable), sstable.getSSTableLevel(), getMaxSSTableBytes());
				op = OperationType.TOMBSTONE_COMPACTION;
			}else {
				op = OperationType.COMPACTION;
			}
			if (candidate.sstables.equals(previousCandidate)) {
				LeveledCompactionStrategy.logger.warn(("Could not acquire references for compacting SSTables {} which is not a problem per se," + "unless it happens frequently, in which case it must be reported. Will retry later."), candidate.sstables);
				return null;
			}
			LifecycleTransaction txn = cfs.getTracker().tryModify(candidate.sstables, OperationType.COMPACTION);
			if (txn != null) {
				LeveledCompactionTask newTask = new LeveledCompactionTask(cfs, txn, candidate.level, gcBefore, candidate.maxSSTableBytes, false);
				newTask.setCompactionType(op);
				return newTask;
			}
			previousCandidate = candidate.sstables;
		} 
	}

	@SuppressWarnings("resource")
	public synchronized Collection<AbstractCompactionTask> getMaximalTask(int gcBefore, boolean splitOutput) {
		Iterable<SSTableReader> sstables = manifest.getAllSSTables();
		Iterable<SSTableReader> filteredSSTables = AbstractCompactionStrategy.filterSuspectSSTables(sstables);
		if (Iterables.isEmpty(sstables))
			return null;

		LifecycleTransaction txn = cfs.getTracker().tryModify(filteredSSTables, OperationType.COMPACTION);
		if (txn == null)
			return null;

		return Arrays.<AbstractCompactionTask>asList(new LeveledCompactionTask(cfs, txn, 0, gcBefore, getMaxSSTableBytes(), true));
	}

	@Override
	@SuppressWarnings("resource")
	public AbstractCompactionTask getUserDefinedTask(Collection<SSTableReader> sstables, int gcBefore) {
		if (sstables.isEmpty())
			return null;

		LifecycleTransaction transaction = cfs.getTracker().tryModify(sstables, OperationType.COMPACTION);
		if (transaction == null) {
			LeveledCompactionStrategy.logger.trace("Unable to mark {} for compaction; probably a background compaction got to it first.  You can disable background compactions temporarily if this is a problem", sstables);
			return null;
		}
		int level = ((sstables.size()) > 1) ? 0 : sstables.iterator().next().getSSTableLevel();
		return new LeveledCompactionTask(cfs, transaction, level, gcBefore, (level == 0 ? Long.MAX_VALUE : getMaxSSTableBytes()), false);
	}

	@Override
	public AbstractCompactionTask getCompactionTask(LifecycleTransaction txn, int gcBefore, long maxSSTableBytes) {
		assert (txn.originals().size()) > 0;
		int level = -1;
		for (SSTableReader sstable : txn.originals()) {
			if (level == (-1))
				level = sstable.getSSTableLevel();

			if (level != (sstable.getSSTableLevel()))
				level = 0;

		}
		return new LeveledCompactionTask(cfs, txn, level, gcBefore, maxSSTableBytes, false);
	}

	@Override
	public Collection<Collection<SSTableReader>> groupSSTablesForAntiCompaction(Collection<SSTableReader> ssTablesToGroup) {
		int groupSize = 2;
		Map<Integer, Collection<SSTableReader>> sstablesByLevel = new HashMap<>();
		for (SSTableReader sstable : ssTablesToGroup) {
			Integer level = sstable.getSSTableLevel();
			Collection<SSTableReader> sstablesForLevel = sstablesByLevel.get(level);
			if (sstablesForLevel == null) {
				sstablesForLevel = new ArrayList<SSTableReader>();
				sstablesByLevel.put(level, sstablesForLevel);
			}
			sstablesForLevel.add(sstable);
		}
		Collection<Collection<SSTableReader>> groupedSSTables = new ArrayList<>();
		for (Collection<SSTableReader> levelOfSSTables : sstablesByLevel.values()) {
			Collection<SSTableReader> currGroup = new ArrayList<>();
			for (SSTableReader sstable : levelOfSSTables) {
				currGroup.add(sstable);
				if ((currGroup.size()) == groupSize) {
					groupedSSTables.add(currGroup);
					currGroup = new ArrayList<>();
				}
			}
			if ((currGroup.size()) != 0)
				groupedSSTables.add(currGroup);

		}
		return groupedSSTables;
	}

	public int getEstimatedRemainingTasks() {
		int n = manifest.getEstimatedTasks();
		cfs.getCompactionStrategyManager().compactionLogger.pending(this, n);
		return n;
	}

	public long getMaxSSTableBytes() {
		return ((maxSSTableSizeInMB) * 1024L) * 1024L;
	}

	public int getLevelFanoutSize() {
		return levelFanoutSize;
	}

	public AbstractCompactionStrategy.ScannerList getScanners(Collection<SSTableReader> sstables, Collection<Range<Token>> ranges) {
		Set<SSTableReader>[] sstablesPerLevel = manifest.getSStablesPerLevelSnapshot();
		Multimap<Integer, SSTableReader> byLevel = ArrayListMultimap.create();
		for (SSTableReader sstable : sstables) {
			int level = sstable.getSSTableLevel();
			if ((level >= (sstablesPerLevel.length)) || (!(sstablesPerLevel[level].contains(sstable)))) {
				LeveledCompactionStrategy.logger.warn(("Live sstable {} from level {} is not on corresponding level in the leveled manifest." + (" This is not a problem per se, but may indicate an orphaned sstable due to a failed" + " compaction not cleaned up properly.")), sstable.getFilename(), level);
				level = -1;
			}
			byLevel.get(level).add(sstable);
		}
		List<ISSTableScanner> scanners = new ArrayList<ISSTableScanner>(sstables.size());
		try {
			for (Integer level : byLevel.keySet()) {
				if (level <= 0) {
					for (SSTableReader sstable : byLevel.get(level))
						scanners.add(sstable.getScanner(ranges, null));

				}else {
					Collection<SSTableReader> intersecting = LeveledCompactionStrategy.LeveledScanner.intersecting(byLevel.get(level), ranges);
					if (!(intersecting.isEmpty())) {
						@SuppressWarnings("resource")
						ISSTableScanner scanner = new LeveledCompactionStrategy.LeveledScanner(intersecting, ranges);
						scanners.add(scanner);
					}
				}
			}
		} catch (Throwable t) {
			try {
				new AbstractCompactionStrategy.ScannerList(scanners).close();
			} catch (Throwable t2) {
				t.addSuppressed(t2);
			}
			throw t;
		}
		return new AbstractCompactionStrategy.ScannerList(scanners);
	}

	@Override
	public void replaceSSTables(Collection<SSTableReader> removed, Collection<SSTableReader> added) {
		manifest.replace(removed, added);
	}

	@Override
	public void addSSTable(SSTableReader added) {
		manifest.add(added);
	}

	@Override
	public void removeSSTable(SSTableReader sstable) {
		manifest.remove(sstable);
	}

	private static class LeveledScanner extends AbstractIterator<UnfilteredRowIterator> implements ISSTableScanner {
		private final Collection<Range<Token>> ranges;

		private final List<SSTableReader> sstables;

		private final Iterator<SSTableReader> sstableIterator;

		private final long totalLength;

		private final long compressedLength;

		private ISSTableScanner currentScanner;

		private long positionOffset;

		private long totalBytesScanned = 0;

		public LeveledScanner(Collection<SSTableReader> sstables, Collection<Range<Token>> ranges) {
			this.ranges = ranges;
			this.sstables = new ArrayList<>(sstables.size());
			long length = 0;
			long cLength = 0;
			for (SSTableReader sstable : sstables) {
				this.sstables.add(sstable);
				long estimatedKeys = sstable.estimatedKeys();
				double estKeysInRangeRatio = 1.0;
				if ((estimatedKeys > 0) && (ranges != null))
					estKeysInRangeRatio = ((double) (sstable.estimatedKeysForRanges(ranges))) / estimatedKeys;

				length += (sstable.uncompressedLength()) * estKeysInRangeRatio;
				cLength += (sstable.onDiskLength()) * estKeysInRangeRatio;
			}
			totalLength = length;
			compressedLength = cLength;
			Collections.sort(this.sstables, SSTableReader.sstableComparator);
			sstableIterator = this.sstables.iterator();
			assert sstableIterator.hasNext();
			SSTableReader currentSSTable = sstableIterator.next();
			currentScanner = currentSSTable.getScanner(ranges, null);
		}

		public static Collection<SSTableReader> intersecting(Collection<SSTableReader> sstables, Collection<Range<Token>> ranges) {
			if (ranges == null)
				return Lists.newArrayList(sstables);

			Set<SSTableReader> filtered = new HashSet<>();
			for (Range<Token> range : ranges) {
				for (SSTableReader sstable : sstables) {
					Range<Token> sstableRange = new Range<>(sstable.first.getToken(), sstable.last.getToken());
					if ((range == null) || (sstableRange.intersects(range)))
						filtered.add(sstable);

				}
			}
			return filtered;
		}

		public boolean isForThrift() {
			return false;
		}

		public CFMetaData metadata() {
			return sstables.get(0).metadata;
		}

		protected UnfilteredRowIterator computeNext() {
			if ((currentScanner) == null)
				return endOfData();

			while (true) {
				if (currentScanner.hasNext())
					return currentScanner.next();

				positionOffset += currentScanner.getLengthInBytes();
				totalBytesScanned += currentScanner.getBytesScanned();
				currentScanner.close();
				if (!(sstableIterator.hasNext())) {
					currentScanner = null;
					return endOfData();
				}
				SSTableReader currentSSTable = sstableIterator.next();
				currentScanner = currentSSTable.getScanner(ranges, null);
			} 
		}

		public void close() {
			if ((currentScanner) != null)
				currentScanner.close();

		}

		public long getLengthInBytes() {
			return totalLength;
		}

		public long getCurrentPosition() {
			return (positionOffset) + ((currentScanner) == null ? 0L : currentScanner.getCurrentPosition());
		}

		public long getCompressedLengthInBytes() {
			return compressedLength;
		}

		public long getBytesScanned() {
			return (currentScanner) == null ? totalBytesScanned : (totalBytesScanned) + (currentScanner.getBytesScanned());
		}

		public String getBackingFiles() {
			return Joiner.on(", ").join(sstables);
		}
	}

	@Override
	public String toString() {
		return String.format("LCS@%d(%s)", hashCode(), cfs.name);
	}

	private SSTableReader findDroppableSSTable(final int gcBefore) {
		level : for (int i = manifest.getLevelCount(); i >= 0; i--) {
			SortedSet<SSTableReader> sstables = manifest.getLevelSorted(i, new Comparator<SSTableReader>() {
				public int compare(SSTableReader o1, SSTableReader o2) {
					double r1 = o1.getEstimatedDroppableTombstoneRatio(gcBefore);
					double r2 = o2.getEstimatedDroppableTombstoneRatio(gcBefore);
					return (-1) * (Doubles.compare(r1, r2));
				}
			});
			if (sstables.isEmpty())
				continue;

			Set<SSTableReader> compacting = cfs.getTracker().getCompacting();
			for (SSTableReader sstable : sstables) {
				if ((sstable.getEstimatedDroppableTombstoneRatio(gcBefore)) <= (tombstoneThreshold))
					continue level;
				else
					if (((!(compacting.contains(sstable))) && (!(sstable.isMarkedSuspect()))) && (worthDroppingTombstones(sstable, gcBefore)))
						return sstable;


			}
		}
		return null;
	}

	public CompactionLogger.Strategy strategyLogger() {
		return new CompactionLogger.Strategy() {
			public JsonNode sstable(SSTableReader sstable) {
				ObjectNode node = JsonNodeFactory.instance.objectNode();
				node.put("level", sstable.getSSTableLevel());
				node.put("min_token", sstable.first.getToken().toString());
				node.put("max_token", sstable.last.getToken().toString());
				return node;
			}

			public JsonNode options() {
				return null;
			}
		};
	}

	public static Map<String, String> validateOptions(Map<String, String> options) throws ConfigurationException {
		Map<String, String> uncheckedOptions = AbstractCompactionStrategy.validateOptions(options);
		String size = (options.containsKey(LeveledCompactionStrategy.SSTABLE_SIZE_OPTION)) ? options.get(LeveledCompactionStrategy.SSTABLE_SIZE_OPTION) : "1";
		try {
			int ssSize = Integer.parseInt(size);
			if (ssSize < 1) {
				throw new ConfigurationException(String.format("%s must be larger than 0, but was %s", LeveledCompactionStrategy.SSTABLE_SIZE_OPTION, ssSize));
			}
		} catch (NumberFormatException ex) {
			throw new ConfigurationException(String.format("%s is not a parsable int (base10) for %s", size, LeveledCompactionStrategy.SSTABLE_SIZE_OPTION), ex);
		}
		uncheckedOptions.remove(LeveledCompactionStrategy.SSTABLE_SIZE_OPTION);
		String levelFanoutSize = (options.containsKey(LeveledCompactionStrategy.LEVEL_FANOUT_SIZE_OPTION)) ? options.get(LeveledCompactionStrategy.LEVEL_FANOUT_SIZE_OPTION) : String.valueOf(LeveledCompactionStrategy.DEFAULT_LEVEL_FANOUT_SIZE);
		try {
			int fanoutSize = Integer.parseInt(levelFanoutSize);
			if (fanoutSize < 1) {
				throw new ConfigurationException(String.format("%s must be larger than 0, but was %s", LeveledCompactionStrategy.LEVEL_FANOUT_SIZE_OPTION, fanoutSize));
			}
		} catch (NumberFormatException ex) {
			throw new ConfigurationException(String.format("%s is not a parsable int (base10) for %s", size, LeveledCompactionStrategy.LEVEL_FANOUT_SIZE_OPTION), ex);
		}
		uncheckedOptions.remove(LeveledCompactionStrategy.LEVEL_FANOUT_SIZE_OPTION);
		uncheckedOptions = SizeTieredCompactionStrategyOptions.validateOptions(options, uncheckedOptions);
		return uncheckedOptions;
	}
}

