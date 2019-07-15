

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.Memtable;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.compaction.AbstractCompactionTask;
import org.apache.cassandra.db.compaction.CompactionController;
import org.apache.cassandra.db.compaction.CompactionLogger;
import org.apache.cassandra.db.compaction.CompactionManager;
import org.apache.cassandra.db.compaction.CompactionTask;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.lifecycle.Tracker;
import org.apache.cassandra.db.partitions.BasePartitionIterator;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.io.sstable.Component;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.ISSTableScanner;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.SSTableMultiWriter;
import org.apache.cassandra.io.sstable.SimpleSSTableMultiWriter;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;
import org.apache.cassandra.schema.CompactionParams;
import org.apache.cassandra.utils.EstimatedHistogram;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.db.compaction.CompactionLogger.Strategy.none;
import static org.apache.cassandra.schema.CompactionParams.Option.PROVIDE_OVERLAPPING_TOMBSTONES;


public abstract class AbstractCompactionStrategy {
	private static final Logger logger = LoggerFactory.getLogger(AbstractCompactionStrategy.class);

	protected static final float DEFAULT_TOMBSTONE_THRESHOLD = 0.2F;

	protected static final long DEFAULT_TOMBSTONE_COMPACTION_INTERVAL = 86400;

	protected static final boolean DEFAULT_UNCHECKED_TOMBSTONE_COMPACTION_OPTION = false;

	protected static final boolean DEFAULT_LOG_ALL_OPTION = false;

	protected static final String TOMBSTONE_THRESHOLD_OPTION = "tombstone_threshold";

	protected static final String TOMBSTONE_COMPACTION_INTERVAL_OPTION = "tombstone_compaction_interval";

	protected static final String UNCHECKED_TOMBSTONE_COMPACTION_OPTION = "unchecked_tombstone_compaction";

	protected static final String LOG_ALL_OPTION = "log_all";

	protected static final String COMPACTION_ENABLED = "enabled";

	public static final String ONLY_PURGE_REPAIRED_TOMBSTONES = "only_purge_repaired_tombstones";

	protected Map<String, String> options;

	protected final ColumnFamilyStore cfs;

	protected float tombstoneThreshold;

	protected long tombstoneCompactionInterval;

	protected boolean uncheckedTombstoneCompaction;

	protected boolean disableTombstoneCompactions = false;

	protected boolean logAll = true;

	private final Directories directories;

	protected boolean isActive = false;

	protected AbstractCompactionStrategy(ColumnFamilyStore cfs, Map<String, String> options) {
		assert cfs != null;
		this.cfs = cfs;
		this.options = ImmutableMap.copyOf(options);
		try {
			AbstractCompactionStrategy.validateOptions(options);
			String optionValue = options.get(AbstractCompactionStrategy.TOMBSTONE_THRESHOLD_OPTION);
			tombstoneThreshold = (optionValue == null) ? AbstractCompactionStrategy.DEFAULT_TOMBSTONE_THRESHOLD : Float.parseFloat(optionValue);
			optionValue = options.get(AbstractCompactionStrategy.TOMBSTONE_COMPACTION_INTERVAL_OPTION);
			tombstoneCompactionInterval = (optionValue == null) ? AbstractCompactionStrategy.DEFAULT_TOMBSTONE_COMPACTION_INTERVAL : Long.parseLong(optionValue);
			optionValue = options.get(AbstractCompactionStrategy.UNCHECKED_TOMBSTONE_COMPACTION_OPTION);
			uncheckedTombstoneCompaction = (optionValue == null) ? AbstractCompactionStrategy.DEFAULT_UNCHECKED_TOMBSTONE_COMPACTION_OPTION : Boolean.parseBoolean(optionValue);
			optionValue = options.get(AbstractCompactionStrategy.LOG_ALL_OPTION);
			logAll = (optionValue == null) ? AbstractCompactionStrategy.DEFAULT_LOG_ALL_OPTION : Boolean.parseBoolean(optionValue);
			if (!(shouldBeEnabled()))
				this.disable();

		} catch (ConfigurationException e) {
			AbstractCompactionStrategy.logger.warn("Error setting compaction strategy options ({}), defaults will be used", e.getMessage());
			tombstoneThreshold = AbstractCompactionStrategy.DEFAULT_TOMBSTONE_THRESHOLD;
			tombstoneCompactionInterval = AbstractCompactionStrategy.DEFAULT_TOMBSTONE_COMPACTION_INTERVAL;
			uncheckedTombstoneCompaction = AbstractCompactionStrategy.DEFAULT_UNCHECKED_TOMBSTONE_COMPACTION_OPTION;
		}
		directories = cfs.getDirectories();
	}

	public Directories getDirectories() {
		return directories;
	}

	public synchronized void pause() {
		isActive = false;
	}

	public synchronized void resume() {
		isActive = true;
	}

	public void startup() {
		isActive = true;
	}

	public void shutdown() {
		isActive = false;
	}

	public abstract AbstractCompactionTask getNextBackgroundTask(final int gcBefore);

	public abstract Collection<AbstractCompactionTask> getMaximalTask(final int gcBefore, boolean splitOutput);

	public abstract AbstractCompactionTask getUserDefinedTask(Collection<SSTableReader> sstables, final int gcBefore);

	public AbstractCompactionTask getCompactionTask(LifecycleTransaction txn, final int gcBefore, long maxSSTableBytes) {
		return new CompactionTask(cfs, txn, gcBefore);
	}

	public abstract int getEstimatedRemainingTasks();

	public abstract long getMaxSSTableBytes();

	public void enable() {
	}

	public void disable() {
	}

	public boolean isAffectedByMeteredFlusher() {
		return true;
	}

	public long getMemtableReservedSize() {
		return 0;
	}

	public void replaceFlushed(Memtable memtable, Collection<SSTableReader> sstables) {
		cfs.getTracker().replaceFlushed(memtable, sstables);
		if ((sstables != null) && (!(sstables.isEmpty())))
			CompactionManager.instance.submitBackground(cfs);

	}

	public static Iterable<SSTableReader> filterSuspectSSTables(Iterable<SSTableReader> originalCandidates) {
		return Iterables.filter(originalCandidates, new Predicate<SSTableReader>() {
			public boolean apply(SSTableReader sstable) {
				return !(sstable.isMarkedSuspect());
			}
		});
	}

	public AbstractCompactionStrategy.ScannerList getScanners(Collection<SSTableReader> sstables, Range<Token> range) {
		return range == null ? getScanners(sstables, ((Collection<Range<Token>>) (null))) : getScanners(sstables, Collections.singleton(range));
	}

	@SuppressWarnings("resource")
	public AbstractCompactionStrategy.ScannerList getScanners(Collection<SSTableReader> sstables, Collection<Range<Token>> ranges) {
		ArrayList<ISSTableScanner> scanners = new ArrayList<ISSTableScanner>();
		try {
			for (SSTableReader sstable : sstables)
				scanners.add(sstable.getScanner(ranges, null));

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

	public boolean shouldDefragment() {
		return false;
	}

	public String getName() {
		return getClass().getSimpleName();
	}

	public synchronized void replaceSSTables(Collection<SSTableReader> removed, Collection<SSTableReader> added) {
		for (SSTableReader remove : removed)
			removeSSTable(remove);

		for (SSTableReader add : added)
			addSSTable(add);

	}

	public abstract void addSSTable(SSTableReader added);

	public synchronized void addSSTables(Iterable<SSTableReader> added) {
		for (SSTableReader sstable : added)
			addSSTable(sstable);

	}

	public abstract void removeSSTable(SSTableReader sstable);

	public static class ScannerList implements AutoCloseable {
		public final List<ISSTableScanner> scanners;

		public ScannerList(List<ISSTableScanner> scanners) {
			this.scanners = scanners;
		}

		public long getTotalBytesScanned() {
			long bytesScanned = 0L;
			for (ISSTableScanner scanner : scanners)
				bytesScanned += scanner.getBytesScanned();

			return bytesScanned;
		}

		public long getTotalCompressedSize() {
			long compressedSize = 0;
			for (ISSTableScanner scanner : scanners)
				compressedSize += scanner.getCompressedLengthInBytes();

			return compressedSize;
		}

		public double getCompressionRatio() {
			double compressed = 0.0;
			double uncompressed = 0.0;
			for (ISSTableScanner scanner : scanners) {
				compressed += scanner.getCompressedLengthInBytes();
				uncompressed += scanner.getLengthInBytes();
			}
			if ((compressed == uncompressed) || (uncompressed == 0))
				return MetadataCollector.NO_COMPRESSION_RATIO;

			return compressed / uncompressed;
		}

		public void close() {
			Throwable t = null;
			for (ISSTableScanner scanner : scanners) {
				try {
					scanner.close();
				} catch (Throwable t2) {
					JVMStabilityInspector.inspectThrowable(t2);
					if (t == null)
						t = t2;
					else
						t.addSuppressed(t2);

				}
			}
			if (t != null)
				throw Throwables.propagate(t);

		}
	}

	public AbstractCompactionStrategy.ScannerList getScanners(Collection<SSTableReader> toCompact) {
		return getScanners(toCompact, ((Collection<Range<Token>>) (null)));
	}

	protected boolean worthDroppingTombstones(SSTableReader sstable, int gcBefore) {
		if ((System.currentTimeMillis()) < ((sstable.getCreationTimeFor(Component.DATA)) + ((tombstoneCompactionInterval) * 1000)))
			return false;

		double droppableRatio = sstable.getEstimatedDroppableTombstoneRatio(gcBefore);
		if (droppableRatio <= (tombstoneThreshold))
			return false;

		if (uncheckedTombstoneCompaction)
			return true;

		Collection<SSTableReader> overlaps = cfs.getOverlappingLiveSSTables(Collections.singleton(sstable));
		if (overlaps.isEmpty()) {
			return true;
		}else
			if ((CompactionController.getFullyExpiredSSTables(cfs, Collections.singleton(sstable), overlaps, gcBefore).size()) > 0) {
				return true;
			}else {
				if ((sstable.getIndexSummarySize()) < 2) {
					return false;
				}
				long keys = sstable.estimatedKeys();
				Set<Range<Token>> ranges = new HashSet<Range<Token>>(overlaps.size());
				for (SSTableReader overlap : overlaps)
					ranges.add(new Range<>(overlap.first.getToken(), overlap.last.getToken()));

				long remainingKeys = keys - (sstable.estimatedKeysForRanges(ranges));
				long columns = (sstable.getEstimatedColumnCount().mean()) * remainingKeys;
				double remainingColumnsRatio = ((double) (columns)) / ((sstable.getEstimatedColumnCount().count()) * (sstable.getEstimatedColumnCount().mean()));
				return (remainingColumnsRatio * droppableRatio) > (tombstoneThreshold);
			}

	}

	public static Map<String, String> validateOptions(Map<String, String> options) throws ConfigurationException {
		String threshold = options.get(AbstractCompactionStrategy.TOMBSTONE_THRESHOLD_OPTION);
		if (threshold != null) {
			try {
				float thresholdValue = Float.parseFloat(threshold);
				if (thresholdValue < 0) {
					throw new ConfigurationException(String.format("%s must be greater than 0, but was %f", AbstractCompactionStrategy.TOMBSTONE_THRESHOLD_OPTION, thresholdValue));
				}
			} catch (NumberFormatException e) {
				throw new ConfigurationException(String.format("%s is not a parsable int (base10) for %s", threshold, AbstractCompactionStrategy.TOMBSTONE_THRESHOLD_OPTION), e);
			}
		}
		String interval = options.get(AbstractCompactionStrategy.TOMBSTONE_COMPACTION_INTERVAL_OPTION);
		if (interval != null) {
			try {
				long tombstoneCompactionInterval = Long.parseLong(interval);
				if (tombstoneCompactionInterval < 0) {
					throw new ConfigurationException(String.format("%s must be greater than 0, but was %d", AbstractCompactionStrategy.TOMBSTONE_COMPACTION_INTERVAL_OPTION, tombstoneCompactionInterval));
				}
			} catch (NumberFormatException e) {
				throw new ConfigurationException(String.format("%s is not a parsable int (base10) for %s", interval, AbstractCompactionStrategy.TOMBSTONE_COMPACTION_INTERVAL_OPTION), e);
			}
		}
		String unchecked = options.get(AbstractCompactionStrategy.UNCHECKED_TOMBSTONE_COMPACTION_OPTION);
		if (unchecked != null) {
			if ((!(unchecked.equalsIgnoreCase("true"))) && (!(unchecked.equalsIgnoreCase("false"))))
				throw new ConfigurationException(String.format("'%s' should be either 'true' or 'false', not '%s'", AbstractCompactionStrategy.UNCHECKED_TOMBSTONE_COMPACTION_OPTION, unchecked));

		}
		String logAll = options.get(AbstractCompactionStrategy.LOG_ALL_OPTION);
		if (logAll != null) {
			if ((!(logAll.equalsIgnoreCase("true"))) && (!(logAll.equalsIgnoreCase("false")))) {
				throw new ConfigurationException(String.format("'%s' should either be 'true' or 'false', not %s", AbstractCompactionStrategy.LOG_ALL_OPTION, logAll));
			}
		}
		String compactionEnabled = options.get(AbstractCompactionStrategy.COMPACTION_ENABLED);
		if (compactionEnabled != null) {
			if ((!(compactionEnabled.equalsIgnoreCase("true"))) && (!(compactionEnabled.equalsIgnoreCase("false")))) {
				throw new ConfigurationException(String.format("enabled should either be 'true' or 'false', not %s", compactionEnabled));
			}
		}
		Map<String, String> uncheckedOptions = new HashMap<String, String>(options);
		uncheckedOptions.remove(AbstractCompactionStrategy.TOMBSTONE_THRESHOLD_OPTION);
		uncheckedOptions.remove(AbstractCompactionStrategy.TOMBSTONE_COMPACTION_INTERVAL_OPTION);
		uncheckedOptions.remove(AbstractCompactionStrategy.UNCHECKED_TOMBSTONE_COMPACTION_OPTION);
		uncheckedOptions.remove(AbstractCompactionStrategy.LOG_ALL_OPTION);
		uncheckedOptions.remove(AbstractCompactionStrategy.COMPACTION_ENABLED);
		uncheckedOptions.remove(AbstractCompactionStrategy.ONLY_PURGE_REPAIRED_TOMBSTONES);
		uncheckedOptions.remove(PROVIDE_OVERLAPPING_TOMBSTONES.toString());
		return uncheckedOptions;
	}

	public boolean shouldBeEnabled() {
		String optionValue = options.get(AbstractCompactionStrategy.COMPACTION_ENABLED);
		return (optionValue == null) || (Boolean.parseBoolean(optionValue));
	}

	public Collection<Collection<SSTableReader>> groupSSTablesForAntiCompaction(Collection<SSTableReader> sstablesToGroup) {
		int groupSize = 2;
		List<SSTableReader> sortedSSTablesToGroup = new ArrayList<>(sstablesToGroup);
		Collections.sort(sortedSSTablesToGroup, SSTableReader.sstableComparator);
		Collection<Collection<SSTableReader>> groupedSSTables = new ArrayList<>();
		Collection<SSTableReader> currGroup = new ArrayList<>();
		for (SSTableReader sstable : sortedSSTablesToGroup) {
			currGroup.add(sstable);
			if ((currGroup.size()) == groupSize) {
				groupedSSTables.add(currGroup);
				currGroup = new ArrayList<>();
			}
		}
		if ((currGroup.size()) != 0)
			groupedSSTables.add(currGroup);

		return groupedSSTables;
	}

	public CompactionLogger.Strategy strategyLogger() {
		return none;
	}

	public SSTableMultiWriter createSSTableMultiWriter(Descriptor descriptor, long keyCount, long repairedAt, MetadataCollector meta, SerializationHeader header, Collection<Index> indexes, LifecycleTransaction txn) {
		return SimpleSSTableMultiWriter.create(descriptor, keyCount, repairedAt, cfs.metadata, meta, header, indexes, txn);
	}

	public boolean supportsEarlyOpen() {
		return true;
	}
}

