

import com.clearspring.analytics.stream.cardinality.CardinalityMergeException;
import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;
import com.clearspring.analytics.stream.cardinality.ICardinality;
import com.codahale.metrics.Counter;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.RateLimiter;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.cassandra.cache.AutoSavingCache;
import org.apache.cassandra.cache.ChunkCache;
import org.apache.cassandra.cache.InstrumentingCache;
import org.apache.cassandra.cache.KeyCacheKey;
import org.apache.cassandra.concurrent.DebuggableScheduledThreadPoolExecutor;
import org.apache.cassandra.concurrent.DebuggableThreadPoolExecutor;
import org.apache.cassandra.concurrent.NamedThreadFactory;
import org.apache.cassandra.concurrent.ScheduledExecutors;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DataRange;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.index.internal.CassandraIndex;
import org.apache.cassandra.io.FSError;
import org.apache.cassandra.io.compress.CompressionMetadata;
import org.apache.cassandra.io.sstable.BloomFilterTracker;
import org.apache.cassandra.io.sstable.Component;
import org.apache.cassandra.io.sstable.CorruptSSTableException;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.Downsampling;
import org.apache.cassandra.io.sstable.ISSTableScanner;
import org.apache.cassandra.io.sstable.IndexSummary;
import org.apache.cassandra.io.sstable.IndexSummaryBuilder;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.io.sstable.format.SSTableReadsListener;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.io.sstable.metadata.CompactionMetadata;
import org.apache.cassandra.io.sstable.metadata.IMetadataSerializer;
import org.apache.cassandra.io.sstable.metadata.MetadataComponent;
import org.apache.cassandra.io.sstable.metadata.MetadataType;
import org.apache.cassandra.io.sstable.metadata.StatsMetadata;
import org.apache.cassandra.io.sstable.metadata.ValidationMetadata;
import org.apache.cassandra.io.util.BufferedDataOutputStreamPlus;
import org.apache.cassandra.io.util.ChannelProxy;
import org.apache.cassandra.io.util.DataOutputStreamPlus;
import org.apache.cassandra.io.util.DiskOptimizationStrategy;
import org.apache.cassandra.io.util.FileDataInput;
import org.apache.cassandra.io.util.FileHandle;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.io.util.RandomAccessReader;
import org.apache.cassandra.metrics.RestorableMeter;
import org.apache.cassandra.metrics.StorageMetrics;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.schema.CachingParams;
import org.apache.cassandra.schema.CompressionParams;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.schema.Indexes;
import org.apache.cassandra.schema.TableParams;
import org.apache.cassandra.service.ActiveRepairService;
import org.apache.cassandra.service.CacheService;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.EstimatedHistogram;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.FilterFactory;
import org.apache.cassandra.utils.IFilter;
import org.apache.cassandra.utils.NativeLibrary;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.concurrent.Ref;
import org.apache.cassandra.utils.concurrent.RefCounted;
import org.apache.cassandra.utils.concurrent.SelfRefCounted;
import org.apache.cassandra.utils.concurrent.SharedCloseable;
import org.apache.cassandra.utils.concurrent.SharedCloseableImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.config.Config.DiskAccessMode.mmap;
import static org.apache.cassandra.db.RowIndexEntry.Serializer.skip;


public abstract class SSTableReader extends SSTable implements SelfRefCounted<SSTableReader> {
	private static final Logger logger = LoggerFactory.getLogger(SSTableReader.class);

	private static final ScheduledThreadPoolExecutor syncExecutor = SSTableReader.initSyncExecutor();

	private static ScheduledThreadPoolExecutor initSyncExecutor() {
		if (DatabaseDescriptor.isClientOrToolInitialized())
			return null;

		ScheduledThreadPoolExecutor syncExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("read-hotness-tracker"));
		syncExecutor.setRemoveOnCancelPolicy(true);
		return syncExecutor;
	}

	private static final RateLimiter meterSyncThrottle = RateLimiter.create(100.0);

	public static final Comparator<SSTableReader> maxTimestampComparator = ( o1, o2) -> Long.compare(o2.getMaxTimestamp(), o1.getMaxTimestamp());

	public static final class UniqueIdentifier {}

	public static final Comparator<SSTableReader> sstableComparator = ( o1, o2) -> o1.first.compareTo(o2.first);

	public static final Comparator<SSTableReader> generationReverseComparator = ( o1, o2) -> -(Integer.compare(o1.descriptor.generation, o2.descriptor.generation));

	public static final Ordering<SSTableReader> sstableOrdering = Ordering.from(SSTableReader.sstableComparator);

	public static final Comparator<SSTableReader> sizeComparator = new Comparator<SSTableReader>() {
		public int compare(SSTableReader o1, SSTableReader o2) {
			return Longs.compare(o1.onDiskLength(), o2.onDiskLength());
		}
	};

	public final long maxDataAge;

	public enum OpenReason {

		NORMAL,
		EARLY,
		METADATA_CHANGE,
		MOVED_START;}

	public final SSTableReader.OpenReason openReason;

	public final SSTableReader.UniqueIdentifier instanceId = new SSTableReader.UniqueIdentifier();

	protected FileHandle ifile;

	protected FileHandle dfile;

	protected IndexSummary indexSummary;

	protected IFilter bf;

	protected final RowIndexEntry.IndexSerializer rowIndexEntrySerializer;

	protected InstrumentingCache<KeyCacheKey, RowIndexEntry> keyCache;

	protected final BloomFilterTracker bloomFilterTracker = new BloomFilterTracker();

	protected final AtomicBoolean isSuspect = new AtomicBoolean(false);

	protected volatile StatsMetadata sstableMetadata;

	public final SerializationHeader header;

	protected final AtomicLong keyCacheHit = new AtomicLong(0);

	protected final AtomicLong keyCacheRequest = new AtomicLong(0);

	private final SSTableReader.InstanceTidier tidy = new SSTableReader.InstanceTidier(descriptor, metadata);

	private final Ref<SSTableReader> selfRef = new Ref<>(this, tidy);

	private RestorableMeter readMeter;

	private volatile double crcCheckChance;

	public static long getApproximateKeyCount(Iterable<SSTableReader> sstables) {
		long count = -1;
		boolean cardinalityAvailable = (!(Iterables.isEmpty(sstables))) && (Iterables.all(sstables, new Predicate<SSTableReader>() {
			public boolean apply(SSTableReader sstable) {
				return sstable.descriptor.version.hasNewStatsFile();
			}
		}));
		if (cardinalityAvailable) {
			boolean failed = false;
			ICardinality cardinality = null;
			for (SSTableReader sstable : sstables) {
				if ((sstable.openReason) == (SSTableReader.OpenReason.EARLY))
					continue;

				try {
					CompactionMetadata metadata = ((CompactionMetadata) (sstable.descriptor.getMetadataSerializer().deserialize(sstable.descriptor, MetadataType.COMPACTION)));
					if (metadata == null) {
						SSTableReader.logger.warn("Reading cardinality from Statistics.db failed for {}", sstable.getFilename());
						failed = true;
						break;
					}
					if (cardinality == null)
						cardinality = metadata.cardinalityEstimator;
					else
						cardinality = cardinality.merge(metadata.cardinalityEstimator);

				} catch (IOException e) {
					SSTableReader.logger.warn("Reading cardinality from Statistics.db failed.", e);
					failed = true;
					break;
				} catch (CardinalityMergeException e) {
					SSTableReader.logger.warn("Cardinality merge failed.", e);
					failed = true;
					break;
				}
			}
			if ((cardinality != null) && (!failed))
				count = cardinality.cardinality();

		}
		if (count < 0) {
			for (SSTableReader sstable : sstables)
				count += sstable.estimatedKeys();

		}
		return count;
	}

	public static double estimateCompactionGain(Set<SSTableReader> overlapping) {
		Set<ICardinality> cardinalities = new HashSet<>(overlapping.size());
		for (SSTableReader sstable : overlapping) {
			try {
				ICardinality cardinality = ((CompactionMetadata) (sstable.descriptor.getMetadataSerializer().deserialize(sstable.descriptor, MetadataType.COMPACTION))).cardinalityEstimator;
				if (cardinality != null)
					cardinalities.add(cardinality);
				else
					SSTableReader.logger.trace("Got a null cardinality estimator in: {}", sstable.getFilename());

			} catch (IOException e) {
				SSTableReader.logger.warn("Could not read up compaction metadata for {}", sstable, e);
			}
		}
		long totalKeyCountBefore = 0;
		for (ICardinality cardinality : cardinalities) {
			totalKeyCountBefore += cardinality.cardinality();
		}
		if (totalKeyCountBefore == 0)
			return 1;

		long totalKeyCountAfter = SSTableReader.mergeCardinalities(cardinalities).cardinality();
		SSTableReader.logger.trace("Estimated compaction gain: {}/{}={}", totalKeyCountAfter, totalKeyCountBefore, (((double) (totalKeyCountAfter)) / totalKeyCountBefore));
		return ((double) (totalKeyCountAfter)) / totalKeyCountBefore;
	}

	private static ICardinality mergeCardinalities(Collection<ICardinality> cardinalities) {
		ICardinality base = new HyperLogLogPlus(13, 25);
		try {
			base = base.merge(cardinalities.toArray(new ICardinality[cardinalities.size()]));
		} catch (CardinalityMergeException e) {
			SSTableReader.logger.warn("Could not merge cardinalities", e);
		}
		return base;
	}

	public static SSTableReader open(Descriptor descriptor) throws IOException {
		CFMetaData metadata;
		if (descriptor.cfname.contains(Directories.SECONDARY_INDEX_NAME_SEPARATOR)) {
			int i = descriptor.cfname.indexOf(Directories.SECONDARY_INDEX_NAME_SEPARATOR);
			String parentName = descriptor.cfname.substring(0, i);
			String indexName = descriptor.cfname.substring((i + 1));
			CFMetaData parent = Schema.instance.getCFMetaData(descriptor.ksname, parentName);
			IndexMetadata def = parent.getIndexes().get(indexName).orElseThrow(() -> new AssertionError(("Could not find index metadata for index cf " + i)));
			metadata = CassandraIndex.indexCfsMetadata(parent, def);
		}else {
			metadata = Schema.instance.getCFMetaData(descriptor.ksname, descriptor.cfname);
		}
		return SSTableReader.open(descriptor, metadata);
	}

	public static SSTableReader open(Descriptor desc, CFMetaData metadata) throws IOException {
		return SSTableReader.open(desc, SSTable.componentsFor(desc), metadata);
	}

	public static SSTableReader open(Descriptor descriptor, Set<Component> components, CFMetaData metadata) throws IOException {
		return SSTableReader.open(descriptor, components, metadata, true, true);
	}

	public static SSTableReader openNoValidation(Descriptor descriptor, Set<Component> components, ColumnFamilyStore cfs) throws IOException {
		return SSTableReader.open(descriptor, components, cfs.metadata, false, false);
	}

	public static SSTableReader openNoValidation(Descriptor descriptor, CFMetaData metadata) throws IOException {
		return SSTableReader.open(descriptor, SSTable.componentsFor(descriptor), metadata, false, false);
	}

	public static SSTableReader openForBatch(Descriptor descriptor, Set<Component> components, CFMetaData metadata) throws IOException {
		assert components.contains(Component.DATA) : "Data component is missing for sstable " + descriptor;
		assert components.contains(Component.PRIMARY_INDEX) : "Primary index component is missing for sstable " + descriptor;
		EnumSet<MetadataType> types = EnumSet.of(MetadataType.VALIDATION, MetadataType.STATS, MetadataType.HEADER);
		Map<MetadataType, MetadataComponent> sstableMetadata = descriptor.getMetadataSerializer().deserialize(descriptor, types);
		ValidationMetadata validationMetadata = ((ValidationMetadata) (sstableMetadata.get(MetadataType.VALIDATION)));
		StatsMetadata statsMetadata = ((StatsMetadata) (sstableMetadata.get(MetadataType.STATS)));
		SerializationHeader.Component header = ((SerializationHeader.Component) (sstableMetadata.get(MetadataType.HEADER)));
		String partitionerName = metadata.partitioner.getClass().getCanonicalName();
		if ((validationMetadata != null) && (!(partitionerName.equals(validationMetadata.partitioner)))) {
			SSTableReader.logger.error("Cannot open {}; partitioner {} does not match system partitioner {}.  Note that the default partitioner starting with Cassandra 1.2 is Murmur3Partitioner, so you will need to edit that to match your old partitioner if upgrading.", descriptor, validationMetadata.partitioner, partitionerName);
			System.exit(1);
		}
		long fileLength = new File(descriptor.filenameFor(Component.DATA)).length();
		SSTableReader.logger.debug("Opening {} ({})", descriptor, FBUtilities.prettyPrintMemory(fileLength));
		SSTableReader sstable = SSTableReader.internalOpen(descriptor, components, metadata, System.currentTimeMillis(), statsMetadata, SSTableReader.OpenReason.NORMAL, (header == null ? null : header.toHeader(metadata)));
		try (FileHandle.Builder ibuilder = new FileHandle.Builder(sstable.descriptor.filenameFor(Component.PRIMARY_INDEX)).mmapped(((DatabaseDescriptor.getIndexAccessMode()) == (mmap))).withChunkCache(ChunkCache.instance);FileHandle.Builder dbuilder = new FileHandle.Builder(sstable.descriptor.filenameFor(Component.DATA)).compressed(sstable.compression).mmapped(((DatabaseDescriptor.getDiskAccessMode()) == (mmap))).withChunkCache(ChunkCache.instance)) {
			if (!(sstable.loadSummary()))
				sstable.buildSummary(false, false, Downsampling.BASE_SAMPLING_LEVEL);

			long indexFileLength = new File(descriptor.filenameFor(Component.PRIMARY_INDEX)).length();
			int dataBufferSize = sstable.optimizationStrategy.bufferSize(statsMetadata.estimatedPartitionSize.percentile(DatabaseDescriptor.getDiskOptimizationEstimatePercentile()));
			int indexBufferSize = sstable.optimizationStrategy.bufferSize((indexFileLength / (sstable.indexSummary.size())));
			sstable.ifile = ibuilder.bufferSize(indexBufferSize).complete();
			sstable.dfile = dbuilder.bufferSize(dataBufferSize).complete();
			sstable.bf = FilterFactory.AlwaysPresent;
			sstable.setup(false);
			return sstable;
		}
	}

	public static SSTableReader open(Descriptor descriptor, Set<Component> components, CFMetaData metadata, boolean validate, boolean trackHotness) throws IOException {
		assert components.contains(Component.DATA) : "Data component is missing for sstable " + descriptor;
		assert (!validate) || (components.contains(Component.PRIMARY_INDEX)) : "Primary index component is missing for sstable " + descriptor;
		assert (!(descriptor.version.storeRows())) || (components.contains(Component.STATS)) : "Stats component is missing for sstable " + descriptor;
		EnumSet<MetadataType> types = EnumSet.of(MetadataType.VALIDATION, MetadataType.STATS, MetadataType.HEADER);
		Map<MetadataType, MetadataComponent> sstableMetadata;
		try {
			sstableMetadata = descriptor.getMetadataSerializer().deserialize(descriptor, types);
		} catch (IOException e) {
			throw new CorruptSSTableException(e, descriptor.filenameFor(Component.STATS));
		}
		ValidationMetadata validationMetadata = ((ValidationMetadata) (sstableMetadata.get(MetadataType.VALIDATION)));
		StatsMetadata statsMetadata = ((StatsMetadata) (sstableMetadata.get(MetadataType.STATS)));
		SerializationHeader.Component header = ((SerializationHeader.Component) (sstableMetadata.get(MetadataType.HEADER)));
		assert (!(descriptor.version.storeRows())) || (header != null);
		String partitionerName = metadata.partitioner.getClass().getCanonicalName();
		if ((validationMetadata != null) && (!(partitionerName.equals(validationMetadata.partitioner)))) {
			SSTableReader.logger.error("Cannot open {}; partitioner {} does not match system partitioner {}.  Note that the default partitioner starting with Cassandra 1.2 is Murmur3Partitioner, so you will need to edit that to match your old partitioner if upgrading.", descriptor, validationMetadata.partitioner, partitionerName);
			System.exit(1);
		}
		long fileLength = new File(descriptor.filenameFor(Component.DATA)).length();
		SSTableReader.logger.debug("Opening {} ({})", descriptor, FBUtilities.prettyPrintMemory(fileLength));
		SSTableReader sstable = SSTableReader.internalOpen(descriptor, components, metadata, System.currentTimeMillis(), statsMetadata, SSTableReader.OpenReason.NORMAL, (header == null ? null : header.toHeader(metadata)));
		try {
			long start = System.nanoTime();
			sstable.load(validationMetadata);
			SSTableReader.logger.trace("INDEX LOAD TIME for {}: {} ms.", descriptor, TimeUnit.NANOSECONDS.toMillis(((System.nanoTime()) - start)));
			sstable.setup(trackHotness);
			if (validate)
				sstable.validate();

			if ((sstable.getKeyCache()) != null)
				SSTableReader.logger.trace("key cache contains {}/{} keys", sstable.getKeyCache().size(), sstable.getKeyCache().getCapacity());

			return sstable;
		} catch (IOException e) {
			sstable.selfRef().release();
			throw new CorruptSSTableException(e, sstable.getFilename());
		} catch (Throwable t) {
			sstable.selfRef().release();
			throw t;
		}
	}

	public static void logOpenException(Descriptor descriptor, IOException e) {
		if (e instanceof FileNotFoundException)
			SSTableReader.logger.error("Missing sstable component in {}; skipped because of {}", descriptor, e.getMessage());
		else
			SSTableReader.logger.error("Corrupt sstable {}; skipped", descriptor, e);

	}

	public static Collection<SSTableReader> openAll(Set<Map.Entry<Descriptor, Set<Component>>> entries, final CFMetaData metadata) {
		final Collection<SSTableReader> sstables = new LinkedBlockingQueue<>();
		ExecutorService executor = DebuggableThreadPoolExecutor.createWithFixedPoolSize("SSTableBatchOpen", FBUtilities.getAvailableProcessors());
		for (final Map.Entry<Descriptor, Set<Component>> entry : entries) {
			Runnable runnable = new Runnable() {
				public void run() {
					SSTableReader sstable;
					try {
						sstable = SSTableReader.open(entry.getKey(), entry.getValue(), metadata);
					} catch (CorruptSSTableException ex) {
						FileUtils.handleCorruptSSTable(ex);
						SSTableReader.logger.error("Corrupt sstable {}; skipping table", entry, ex);
						return;
					} catch (FSError ex) {
						FileUtils.handleFSError(ex);
						SSTableReader.logger.error("Cannot read sstable {}; file system error, skipping table", entry, ex);
						return;
					} catch (IOException ex) {
						FileUtils.handleCorruptSSTable(new CorruptSSTableException(ex, entry.getKey().filenameFor(Component.DATA)));
						SSTableReader.logger.error("Cannot read sstable {}; other IO error, skipping table", entry, ex);
						return;
					}
					sstables.add(sstable);
				}
			};
			executor.submit(runnable);
		}
		executor.shutdown();
		try {
			executor.awaitTermination(7, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		}
		return sstables;
	}

	public static SSTableReader internalOpen(Descriptor desc, Set<Component> components, CFMetaData metadata, FileHandle ifile, FileHandle dfile, IndexSummary isummary, IFilter bf, long maxDataAge, StatsMetadata sstableMetadata, SSTableReader.OpenReason openReason, SerializationHeader header) {
		assert (((((desc != null) && (ifile != null)) && (dfile != null)) && (isummary != null)) && (bf != null)) && (sstableMetadata != null);
		SSTableReader reader = SSTableReader.internalOpen(desc, components, metadata, maxDataAge, sstableMetadata, openReason, header);
		reader.bf = bf;
		reader.ifile = ifile;
		reader.dfile = dfile;
		reader.indexSummary = isummary;
		reader.setup(true);
		return reader;
	}

	private static SSTableReader internalOpen(final Descriptor descriptor, Set<Component> components, CFMetaData metadata, Long maxDataAge, StatsMetadata sstableMetadata, SSTableReader.OpenReason openReason, SerializationHeader header) {
		return null;
	}

	protected SSTableReader(final Descriptor desc, Set<Component> components, CFMetaData metadata, long maxDataAge, StatsMetadata sstableMetadata, SSTableReader.OpenReason openReason, SerializationHeader header) {
		super(desc, components, metadata, DatabaseDescriptor.getDiskOptimizationStrategy());
		this.sstableMetadata = sstableMetadata;
		this.header = header;
		this.maxDataAge = maxDataAge;
		this.openReason = openReason;
		this.rowIndexEntrySerializer = descriptor.version.getSSTableFormat().getIndexSerializer(metadata, desc.version, header);
	}

	public static long getTotalBytes(Iterable<SSTableReader> sstables) {
		long sum = 0;
		for (SSTableReader sstable : sstables)
			sum += sstable.onDiskLength();

		return sum;
	}

	public static long getTotalUncompressedBytes(Iterable<SSTableReader> sstables) {
		long sum = 0;
		for (SSTableReader sstable : sstables)
			sum += sstable.uncompressedLength();

		return sum;
	}

	public boolean equals(Object that) {
		return (that instanceof SSTableReader) && (((SSTableReader) (that)).descriptor.equals(this.descriptor));
	}

	public int hashCode() {
		return this.descriptor.hashCode();
	}

	public String getFilename() {
		return dfile.path();
	}

	public void setupOnline() {
		keyCache = CacheService.instance.keyCache;
		final ColumnFamilyStore cfs = Schema.instance.getColumnFamilyStoreInstance(metadata.cfId);
		if (cfs != null)
			setCrcCheckChance(cfs.getCrcCheckChance());

	}

	public boolean isKeyCacheSetup() {
		return (keyCache) != null;
	}

	private void load(ValidationMetadata validation) throws IOException {
		if ((metadata.params.bloomFilterFpChance) == 1.0) {
			load(false, true);
			bf = FilterFactory.AlwaysPresent;
		}else
			if (!(components.contains(Component.PRIMARY_INDEX))) {
				load(false, false);
			}else
				if ((!(components.contains(Component.FILTER))) || (validation == null)) {
					load(true, true);
				}else
					if ((validation.bloomFilterFPChance) != (metadata.params.bloomFilterFpChance)) {
						load(true, true);
					}else {
						load(false, true);
						loadBloomFilter(descriptor.version.hasOldBfHashOrder());
					}



	}

	private void loadBloomFilter(boolean oldBfHashOrder) throws IOException {
		try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(descriptor.filenameFor(Component.FILTER))))) {
			bf = FilterFactory.deserialize(stream, true, oldBfHashOrder);
		}
	}

	private void load(boolean recreateBloomFilter, boolean saveSummaryIfCreated) throws IOException {
		try (FileHandle.Builder ibuilder = new FileHandle.Builder(descriptor.filenameFor(Component.PRIMARY_INDEX)).mmapped(((DatabaseDescriptor.getIndexAccessMode()) == (mmap))).withChunkCache(ChunkCache.instance);FileHandle.Builder dbuilder = new FileHandle.Builder(descriptor.filenameFor(Component.DATA)).compressed(compression).mmapped(((DatabaseDescriptor.getDiskAccessMode()) == (mmap))).withChunkCache(ChunkCache.instance)) {
			boolean summaryLoaded = loadSummary();
			boolean builtSummary = false;
			if (recreateBloomFilter || (!summaryLoaded)) {
				buildSummary(recreateBloomFilter, summaryLoaded, Downsampling.BASE_SAMPLING_LEVEL);
				builtSummary = true;
			}
			int dataBufferSize = optimizationStrategy.bufferSize(sstableMetadata.estimatedPartitionSize.percentile(DatabaseDescriptor.getDiskOptimizationEstimatePercentile()));
			if (components.contains(Component.PRIMARY_INDEX)) {
				long indexFileLength = new File(descriptor.filenameFor(Component.PRIMARY_INDEX)).length();
				int indexBufferSize = optimizationStrategy.bufferSize((indexFileLength / (indexSummary.size())));
				ifile = ibuilder.bufferSize(indexBufferSize).complete();
			}
			dfile = dbuilder.bufferSize(dataBufferSize).complete();
			if (saveSummaryIfCreated && builtSummary)
				saveSummary();

		} catch (Throwable t) {
			if ((ifile) != null) {
				ifile.close();
				ifile = null;
			}
			if ((dfile) != null) {
				dfile.close();
				dfile = null;
			}
			if ((indexSummary) != null) {
				indexSummary.close();
				indexSummary = null;
			}
			throw t;
		}
	}

	private void buildSummary(boolean recreateBloomFilter, boolean summaryLoaded, int samplingLevel) throws IOException {
		if (!(components.contains(Component.PRIMARY_INDEX)))
			return;

		try (RandomAccessReader primaryIndex = RandomAccessReader.open(new File(descriptor.filenameFor(Component.PRIMARY_INDEX)))) {
			long indexSize = primaryIndex.length();
			long histogramCount = sstableMetadata.estimatedPartitionSize.count();
			long estimatedKeys = ((histogramCount > 0) && (!(sstableMetadata.estimatedPartitionSize.isOverflowed()))) ? histogramCount : estimateRowsFromIndex(primaryIndex);
			if (recreateBloomFilter)
				bf = FilterFactory.getFilter(estimatedKeys, metadata.params.bloomFilterFpChance, true, descriptor.version.hasOldBfHashOrder());

			try (IndexSummaryBuilder summaryBuilder = (summaryLoaded) ? null : new IndexSummaryBuilder(estimatedKeys, metadata.params.minIndexInterval, samplingLevel)) {
				long indexPosition;
				while ((indexPosition = primaryIndex.getFilePointer()) != indexSize) {
					ByteBuffer key = ByteBufferUtil.readWithShortLength(primaryIndex);
					skip(primaryIndex, descriptor.version);
					DecoratedKey decoratedKey = decorateKey(key);
					if ((first) == null)
						first = decoratedKey;

					last = decoratedKey;
					if (recreateBloomFilter)
						bf.add(decoratedKey);

					if (!summaryLoaded) {
						summaryBuilder.maybeAddEntry(decoratedKey, indexPosition);
					}
				} 
				if (!summaryLoaded)
					indexSummary = summaryBuilder.build(getPartitioner());

			}
		}
		first = SSTable.getMinimalKey(first);
		last = SSTable.getMinimalKey(last);
	}

	@SuppressWarnings("resource")
	public boolean loadSummary() {
		File summariesFile = new File(descriptor.filenameFor(Component.SUMMARY));
		if (!(summariesFile.exists()))
			return false;

		DataInputStream iStream = null;
		try {
			iStream = new DataInputStream(new FileInputStream(summariesFile));
			indexSummary = IndexSummary.serializer.deserialize(iStream, getPartitioner(), descriptor.version.hasSamplingLevel(), metadata.params.minIndexInterval, metadata.params.maxIndexInterval);
			first = decorateKey(ByteBufferUtil.readWithLength(iStream));
			last = decorateKey(ByteBufferUtil.readWithLength(iStream));
		} catch (IOException e) {
			if ((indexSummary) != null)
				indexSummary.close();

			SSTableReader.logger.trace("Cannot deserialize SSTable Summary File {}: {}", summariesFile.getPath(), e.getMessage());
			FileUtils.closeQuietly(iStream);
			FileUtils.deleteWithConfirm(summariesFile);
			return false;
		} finally {
			FileUtils.closeQuietly(iStream);
		}
		return true;
	}

	public void saveSummary() {
		SSTableReader.saveSummary(this.descriptor, this.first, this.last, indexSummary);
	}

	private void saveSummary(IndexSummary newSummary) {
		SSTableReader.saveSummary(this.descriptor, this.first, this.last, newSummary);
	}

	public static void saveSummary(Descriptor descriptor, DecoratedKey first, DecoratedKey last, IndexSummary summary) {
		File summariesFile = new File(descriptor.filenameFor(Component.SUMMARY));
		if (summariesFile.exists())
			FileUtils.deleteWithConfirm(summariesFile);

		try (DataOutputStreamPlus oStream = new BufferedDataOutputStreamPlus(new FileOutputStream(summariesFile))) {
			IndexSummary.serializer.serialize(summary, oStream, descriptor.version.hasSamplingLevel());
			ByteBufferUtil.writeWithLength(first.getKey(), oStream);
			ByteBufferUtil.writeWithLength(last.getKey(), oStream);
		} catch (IOException e) {
			SSTableReader.logger.trace("Cannot save SSTable Summary: ", e);
			if (summariesFile.exists())
				FileUtils.deleteWithConfirm(summariesFile);

		}
	}

	public void setReplaced() {
		synchronized(tidy.global) {
			assert !(tidy.isReplaced);
			tidy.isReplaced = true;
		}
	}

	public boolean isReplaced() {
		synchronized(tidy.global) {
			return tidy.isReplaced;
		}
	}

	public void runOnClose(final Runnable runOnClose) {
		synchronized(tidy.global) {
			final Runnable existing = tidy.runOnClose;
			tidy.runOnClose = SSTableReader.AndThen.get(existing, runOnClose);
		}
	}

	private static class AndThen implements Runnable {
		final Runnable runFirst;

		final Runnable runSecond;

		private AndThen(Runnable runFirst, Runnable runSecond) {
			this.runFirst = runFirst;
			this.runSecond = runSecond;
		}

		public void run() {
			runFirst.run();
			runSecond.run();
		}

		static Runnable get(Runnable runFirst, Runnable runSecond) {
			if (runFirst == null)
				return runSecond;

			return new SSTableReader.AndThen(runFirst, runSecond);
		}
	}

	private SSTableReader cloneAndReplace(DecoratedKey newFirst, SSTableReader.OpenReason reason) {
		return cloneAndReplace(newFirst, reason, indexSummary.sharedCopy());
	}

	private SSTableReader cloneAndReplace(DecoratedKey newFirst, SSTableReader.OpenReason reason, IndexSummary newSummary) {
		SSTableReader replacement = SSTableReader.internalOpen(descriptor, components, metadata, ((ifile) != null ? ifile.sharedCopy() : null), dfile.sharedCopy(), newSummary, bf.sharedCopy(), maxDataAge, sstableMetadata, reason, header);
		replacement.first = newFirst;
		replacement.last = last;
		replacement.isSuspect.set(isSuspect.get());
		return replacement;
	}

	public SSTableReader cloneWithRestoredStart(DecoratedKey restoredStart) {
		synchronized(tidy.global) {
			return cloneAndReplace(restoredStart, SSTableReader.OpenReason.NORMAL);
		}
	}

	public SSTableReader cloneWithNewStart(DecoratedKey newStart, final Runnable runOnClose) {
		synchronized(tidy.global) {
			assert (openReason) != (SSTableReader.OpenReason.EARLY);
			if ((newStart.compareTo(first)) > 0) {
				final long dataStart = getPosition(newStart, SSTableReader.Operator.EQ).position;
				final long indexStart = getIndexScanPosition(newStart);
				this.tidy.runOnClose = new SSTableReader.DropPageCache(dfile, dataStart, ifile, indexStart, runOnClose);
			}
			return cloneAndReplace(newStart, SSTableReader.OpenReason.MOVED_START);
		}
	}

	private static class DropPageCache implements Runnable {
		final FileHandle dfile;

		final long dfilePosition;

		final FileHandle ifile;

		final long ifilePosition;

		final Runnable andThen;

		private DropPageCache(FileHandle dfile, long dfilePosition, FileHandle ifile, long ifilePosition, Runnable andThen) {
			this.dfile = dfile;
			this.dfilePosition = dfilePosition;
			this.ifile = ifile;
			this.ifilePosition = ifilePosition;
			this.andThen = andThen;
		}

		public void run() {
			dfile.dropPageCache(dfilePosition);
			if ((ifile) != null)
				ifile.dropPageCache(ifilePosition);

			if ((andThen) != null)
				andThen.run();

		}
	}

	@SuppressWarnings("resource")
	public SSTableReader cloneWithNewSummarySamplingLevel(ColumnFamilyStore parent, int samplingLevel) throws IOException {
		assert descriptor.version.hasSamplingLevel();
		synchronized(tidy.global) {
			assert (openReason) != (SSTableReader.OpenReason.EARLY);
			int minIndexInterval = metadata.params.minIndexInterval;
			int maxIndexInterval = metadata.params.maxIndexInterval;
			double effectiveInterval = indexSummary.getEffectiveIndexInterval();
			IndexSummary newSummary;
			long oldSize = bytesOnDisk();
			if (((samplingLevel > (indexSummary.getSamplingLevel())) || ((indexSummary.getMinIndexInterval()) != minIndexInterval)) || (effectiveInterval > maxIndexInterval)) {
				newSummary = buildSummaryAtLevel(samplingLevel);
			}else
				if (samplingLevel < (indexSummary.getSamplingLevel())) {
					newSummary = IndexSummaryBuilder.downsample(indexSummary, samplingLevel, minIndexInterval, getPartitioner());
				}else {
					throw new AssertionError(("Attempted to clone SSTableReader with the same index summary sampling level and " + "no adjustments to min/max_index_interval"));
				}

			saveSummary(newSummary);
			StorageMetrics.load.dec(oldSize);
			parent.metric.liveDiskSpaceUsed.dec(oldSize);
			parent.metric.totalDiskSpaceUsed.dec(oldSize);
			return cloneAndReplace(first, SSTableReader.OpenReason.METADATA_CHANGE, newSummary);
		}
	}

	private IndexSummary buildSummaryAtLevel(int newSamplingLevel) throws IOException {
		RandomAccessReader primaryIndex = RandomAccessReader.open(new File(descriptor.filenameFor(Component.PRIMARY_INDEX)));
		try {
			long indexSize = primaryIndex.length();
			try (IndexSummaryBuilder summaryBuilder = new IndexSummaryBuilder(estimatedKeys(), metadata.params.minIndexInterval, newSamplingLevel)) {
				long indexPosition;
				while ((indexPosition = primaryIndex.getFilePointer()) != indexSize) {
					summaryBuilder.maybeAddEntry(decorateKey(ByteBufferUtil.readWithShortLength(primaryIndex)), indexPosition);
					skip(primaryIndex, descriptor.version);
				} 
				return summaryBuilder.build(getPartitioner());
			}
		} finally {
			FileUtils.closeQuietly(primaryIndex);
		}
	}

	public RestorableMeter getReadMeter() {
		return readMeter;
	}

	public int getIndexSummarySamplingLevel() {
		return indexSummary.getSamplingLevel();
	}

	public long getIndexSummaryOffHeapSize() {
		return indexSummary.getOffHeapSize();
	}

	public int getMinIndexInterval() {
		return indexSummary.getMinIndexInterval();
	}

	public double getEffectiveIndexInterval() {
		return indexSummary.getEffectiveIndexInterval();
	}

	public void releaseSummary() {
		tidy.releaseSummary();
		indexSummary = null;
	}

	private void validate() {
		if ((this.first.compareTo(this.last)) > 0) {
			throw new CorruptSSTableException(new IllegalStateException(String.format("SSTable first key %s > last key %s", this.first, this.last)), getFilename());
		}
	}

	public long getIndexScanPosition(PartitionPosition key) {
		if (((openReason) == (SSTableReader.OpenReason.MOVED_START)) && ((key.compareTo(first)) < 0))
			key = first;

		return SSTableReader.getIndexScanPositionFromBinarySearchResult(indexSummary.binarySearch(key), indexSummary);
	}

	@com.google.common.annotations.VisibleForTesting
	public static long getIndexScanPositionFromBinarySearchResult(int binarySearchResult, IndexSummary referencedIndexSummary) {
		if (binarySearchResult == (-1))
			return 0;
		else
			return referencedIndexSummary.getPosition(SSTableReader.getIndexSummaryIndexFromBinarySearchResult(binarySearchResult));

	}

	public static int getIndexSummaryIndexFromBinarySearchResult(int binarySearchResult) {
		if (binarySearchResult < 0) {
			int greaterThan = (binarySearchResult + 1) * (-1);
			if (greaterThan == 0)
				return -1;

			return greaterThan - 1;
		}else {
			return binarySearchResult;
		}
	}

	public CompressionMetadata getCompressionMetadata() {
		if (!(compression))
			throw new IllegalStateException(((this) + " is not compressed"));

		return dfile.compressionMetadata().get();
	}

	public long getCompressionMetadataOffHeapSize() {
		if (!(compression))
			return 0;

		return getCompressionMetadata().offHeapSize();
	}

	public void forceFilterFailures() {
		bf = FilterFactory.AlwaysPresent;
	}

	public IFilter getBloomFilter() {
		return bf;
	}

	public long getBloomFilterSerializedSize() {
		return bf.serializedSize();
	}

	public long getBloomFilterOffHeapSize() {
		return bf.offHeapSize();
	}

	public long estimatedKeys() {
		return indexSummary.getEstimatedKeyCount();
	}

	public long estimatedKeysForRanges(Collection<Range<Token>> ranges) {
		long sampleKeyCount = 0;
		List<Pair<Integer, Integer>> sampleIndexes = SSTableReader.getSampleIndexesForRanges(indexSummary, ranges);
		for (Pair<Integer, Integer> sampleIndexRange : sampleIndexes)
			sampleKeyCount += ((sampleIndexRange.right) - (sampleIndexRange.left)) + 1;

		long estimatedKeys = (sampleKeyCount * (((long) (Downsampling.BASE_SAMPLING_LEVEL)) * (indexSummary.getMinIndexInterval()))) / (indexSummary.getSamplingLevel());
		return Math.max(1, estimatedKeys);
	}

	public int getIndexSummarySize() {
		return indexSummary.size();
	}

	public int getMaxIndexSummarySize() {
		return indexSummary.getMaxNumberOfEntries();
	}

	public byte[] getIndexSummaryKey(int index) {
		return indexSummary.getKey(index);
	}

	private static List<Pair<Integer, Integer>> getSampleIndexesForRanges(IndexSummary summary, Collection<Range<Token>> ranges) {
		List<Pair<Integer, Integer>> positions = new ArrayList<>();
		for (Range<Token> range : Range.normalize(ranges)) {
			PartitionPosition leftPosition = range.left.maxKeyBound();
			PartitionPosition rightPosition = range.right.maxKeyBound();
			int left = summary.binarySearch(leftPosition);
			if (left < 0)
				left = (left + 1) * (-1);
			else
				left = left + 1;

			if (left == (summary.size()))
				continue;

			int right = (Range.isWrapAround(range.left, range.right)) ? (summary.size()) - 1 : summary.binarySearch(rightPosition);
			if (right < 0) {
				right = (right + 1) * (-1);
				if (right == 0)
					continue;

				right--;
			}
			if (left > right)
				continue;

			positions.add(Pair.create(left, right));
		}
		return positions;
	}

	public Iterable<DecoratedKey> getKeySamples(final Range<Token> range) {
		final List<Pair<Integer, Integer>> indexRanges = SSTableReader.getSampleIndexesForRanges(indexSummary, Collections.singletonList(range));
		if (indexRanges.isEmpty())
			return Collections.emptyList();

		return new Iterable<DecoratedKey>() {
			public Iterator<DecoratedKey> iterator() {
				return new Iterator<DecoratedKey>() {
					private Iterator<Pair<Integer, Integer>> rangeIter = indexRanges.iterator();

					private Pair<Integer, Integer> current;

					private int idx;

					public boolean hasNext() {
						if (((current) == null) || ((idx) > (current.right))) {
							if (rangeIter.hasNext()) {
								current = rangeIter.next();
								idx = current.left;
								return true;
							}
							return false;
						}
						return true;
					}

					public DecoratedKey next() {
						byte[] bytes = indexSummary.getKey(((idx)++));
						return decorateKey(ByteBuffer.wrap(bytes));
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public List<Pair<Long, Long>> getPositionsForRanges(Collection<Range<Token>> ranges) {
		List<Pair<Long, Long>> positions = new ArrayList<>();
		for (Range<Token> range : Range.normalize(ranges)) {
			assert (!(range.isWrapAround())) || (range.right.isMinimum());
			AbstractBounds<PartitionPosition> bounds = Range.makeRowRange(range);
			PartitionPosition leftBound = ((bounds.left.compareTo(first)) > 0) ? bounds.left : first.getToken().minKeyBound();
			PartitionPosition rightBound = (bounds.right.isMinimum()) ? last.getToken().maxKeyBound() : bounds.right;
			if (((leftBound.compareTo(last)) > 0) || ((rightBound.compareTo(first)) < 0))
				continue;

			long left = getPosition(leftBound, SSTableReader.Operator.GT).position;
			long right = ((rightBound.compareTo(last)) > 0) ? uncompressedLength() : getPosition(rightBound, SSTableReader.Operator.GT).position;
			if (left == right)
				continue;

			assert left < right : String.format("Range=%s openReason=%s first=%s last=%s left=%d right=%d", range, openReason, first, last, left, right);
			positions.add(Pair.create(left, right));
		}
		return positions;
	}

	public KeyCacheKey getCacheKey(DecoratedKey key) {
		return new KeyCacheKey(metadata.ksAndCFName, descriptor, key.getKey());
	}

	public void cacheKey(DecoratedKey key, RowIndexEntry info) {
		CachingParams caching = metadata.params.caching;
		if (((!(caching.cacheKeys())) || ((keyCache) == null)) || ((keyCache.getCapacity()) == 0))
			return;

		KeyCacheKey cacheKey = new KeyCacheKey(metadata.ksAndCFName, descriptor, key.getKey());
		SSTableReader.logger.trace("Adding cache entry for {} -> {}", cacheKey, info);
		keyCache.put(cacheKey, info);
	}

	public RowIndexEntry getCachedPosition(DecoratedKey key, boolean updateStats) {
		return getCachedPosition(new KeyCacheKey(metadata.ksAndCFName, descriptor, key.getKey()), updateStats);
	}

	protected RowIndexEntry getCachedPosition(KeyCacheKey unifiedKey, boolean updateStats) {
		if ((((keyCache) != null) && ((keyCache.getCapacity()) > 0)) && (metadata.params.caching.cacheKeys())) {
			if (updateStats) {
				RowIndexEntry cachedEntry = keyCache.get(unifiedKey);
				keyCacheRequest.incrementAndGet();
				if (cachedEntry != null) {
					keyCacheHit.incrementAndGet();
					bloomFilterTracker.addTruePositive();
				}
				return cachedEntry;
			}else {
				return keyCache.getInternal(unifiedKey);
			}
		}
		return null;
	}

	public final RowIndexEntry getPosition(PartitionPosition key, SSTableReader.Operator op) {
		return getPosition(key, op, SSTableReadsListener.NOOP_LISTENER);
	}

	public final RowIndexEntry getPosition(PartitionPosition key, SSTableReader.Operator op, SSTableReadsListener listener) {
		return getPosition(key, op, true, false, listener);
	}

	public final RowIndexEntry getPosition(PartitionPosition key, SSTableReader.Operator op, boolean updateCacheAndStats) {
		return getPosition(key, op, updateCacheAndStats, false, SSTableReadsListener.NOOP_LISTENER);
	}

	protected abstract RowIndexEntry getPosition(PartitionPosition key, SSTableReader.Operator op, boolean updateCacheAndStats, boolean permitMatchPastLast, SSTableReadsListener listener);

	public abstract UnfilteredRowIterator iterator(DecoratedKey key, Slices slices, ColumnFilter selectedColumns, boolean reversed, boolean isForThrift, SSTableReadsListener listener);

	public abstract UnfilteredRowIterator iterator(FileDataInput file, DecoratedKey key, RowIndexEntry indexEntry, Slices slices, ColumnFilter selectedColumns, boolean reversed, boolean isForThrift);

	public abstract UnfilteredRowIterator simpleIterator(FileDataInput file, DecoratedKey key, RowIndexEntry indexEntry, boolean tombstoneOnly);

	public DecoratedKey firstKeyBeyond(PartitionPosition token) {
		if ((token.compareTo(first)) < 0)
			return first;

		long sampledPosition = getIndexScanPosition(token);
		if ((ifile) == null)
			return null;

		String path = null;
		try (FileDataInput in = ifile.createReader(sampledPosition)) {
			path = in.getPath();
			while (!(in.isEOF())) {
				ByteBuffer indexKey = ByteBufferUtil.readWithShortLength(in);
				DecoratedKey indexDecoratedKey = decorateKey(indexKey);
				if ((indexDecoratedKey.compareTo(token)) > 0)
					return indexDecoratedKey;

				skip(in, descriptor.version);
			} 
		} catch (IOException e) {
			markSuspect();
			throw new CorruptSSTableException(e, path);
		}
		return null;
	}

	public long uncompressedLength() {
		return dfile.dataLength();
	}

	public long onDiskLength() {
		return dfile.onDiskLength;
	}

	@com.google.common.annotations.VisibleForTesting
	public double getCrcCheckChance() {
		return crcCheckChance;
	}

	public void setCrcCheckChance(double crcCheckChance) {
		this.crcCheckChance = crcCheckChance;
		dfile.compressionMetadata().ifPresent(( metadata) -> metadata.parameters.setCrcCheckChance(crcCheckChance));
	}

	public void markObsolete(Runnable tidier) {
		if (SSTableReader.logger.isTraceEnabled())
			SSTableReader.logger.trace("Marking {} compacted", getFilename());

		synchronized(tidy.global) {
			assert !(tidy.isReplaced);
			assert (tidy.global.obsoletion) == null : (this) + " was already marked compacted";
			tidy.global.obsoletion = tidier;
			tidy.global.stopReadMeterPersistence();
		}
	}

	public boolean isMarkedCompacted() {
		return (tidy.global.obsoletion) != null;
	}

	public void markSuspect() {
		if (SSTableReader.logger.isTraceEnabled())
			SSTableReader.logger.trace("Marking {} as a suspect for blacklisting.", getFilename());

		isSuspect.getAndSet(true);
	}

	public boolean isMarkedSuspect() {
		return isSuspect.get();
	}

	public ISSTableScanner getScanner() {
		return getScanner(((RateLimiter) (null)));
	}

	public ISSTableScanner getScanner(ColumnFilter columns, DataRange dataRange, boolean isForThrift, SSTableReadsListener listener) {
		return getScanner(columns, dataRange, null, isForThrift, listener);
	}

	public ISSTableScanner getScanner(Range<Token> range, RateLimiter limiter) {
		if (range == null)
			return getScanner(limiter);

		return getScanner(Collections.singletonList(range), limiter);
	}

	public abstract ISSTableScanner getScanner(RateLimiter limiter);

	public abstract ISSTableScanner getScanner(Collection<Range<Token>> ranges, RateLimiter limiter);

	public abstract ISSTableScanner getScanner(Iterator<AbstractBounds<PartitionPosition>> rangeIterator);

	public abstract ISSTableScanner getScanner(ColumnFilter columns, DataRange dataRange, RateLimiter limiter, boolean isForThrift, SSTableReadsListener listener);

	public FileDataInput getFileDataInput(long position) {
		return dfile.createReader(position);
	}

	public boolean newSince(long age) {
		return (maxDataAge) > age;
	}

	public void createLinks(String snapshotDirectoryPath) {
		for (Component component : components) {
			File sourceFile = new File(descriptor.filenameFor(component));
			if (!(sourceFile.exists()))
				continue;

			File targetLink = new File(snapshotDirectoryPath, sourceFile.getName());
			FileUtils.createHardLink(sourceFile, targetLink);
		}
	}

	public boolean isRepaired() {
		return (sstableMetadata.repairedAt) != (ActiveRepairService.UNREPAIRED_SSTABLE);
	}

	public DecoratedKey keyAt(long indexPosition) throws IOException {
		DecoratedKey key;
		try (FileDataInput in = ifile.createReader(indexPosition)) {
			if (in.isEOF())
				return null;

			key = decorateKey(ByteBufferUtil.readWithShortLength(in));
			if (isKeyCacheSetup())
				cacheKey(key, rowIndexEntrySerializer.deserialize(in, in.getFilePointer()));

		}
		return key;
	}

	public static abstract class Operator {
		public static final SSTableReader.Operator EQ = new SSTableReader.Operator.Equals();

		public static final SSTableReader.Operator GE = new SSTableReader.Operator.GreaterThanOrEqualTo();

		public static final SSTableReader.Operator GT = new SSTableReader.Operator.GreaterThan();

		public abstract int apply(int comparison);

		static final class Equals extends SSTableReader.Operator {
			public int apply(int comparison) {
				return -comparison;
			}
		}

		static final class GreaterThanOrEqualTo extends SSTableReader.Operator {
			public int apply(int comparison) {
				return comparison >= 0 ? 0 : 1;
			}
		}

		static final class GreaterThan extends SSTableReader.Operator {
			public int apply(int comparison) {
				return comparison > 0 ? 0 : 1;
			}
		}
	}

	public long getBloomFilterFalsePositiveCount() {
		return bloomFilterTracker.getFalsePositiveCount();
	}

	public long getRecentBloomFilterFalsePositiveCount() {
		return bloomFilterTracker.getRecentFalsePositiveCount();
	}

	public long getBloomFilterTruePositiveCount() {
		return bloomFilterTracker.getTruePositiveCount();
	}

	public long getRecentBloomFilterTruePositiveCount() {
		return bloomFilterTracker.getRecentTruePositiveCount();
	}

	public InstrumentingCache<KeyCacheKey, RowIndexEntry> getKeyCache() {
		return keyCache;
	}

	public EstimatedHistogram getEstimatedPartitionSize() {
		return sstableMetadata.estimatedPartitionSize;
	}

	public EstimatedHistogram getEstimatedColumnCount() {
		return sstableMetadata.estimatedColumnCount;
	}

	public double getEstimatedDroppableTombstoneRatio(int gcBefore) {
		return sstableMetadata.getEstimatedDroppableTombstoneRatio(gcBefore);
	}

	public double getDroppableTombstonesBefore(int gcBefore) {
		return sstableMetadata.getDroppableTombstonesBefore(gcBefore);
	}

	public double getCompressionRatio() {
		return sstableMetadata.compressionRatio;
	}

	public long getMinTimestamp() {
		return sstableMetadata.minTimestamp;
	}

	public long getMaxTimestamp() {
		return sstableMetadata.maxTimestamp;
	}

	public int getMinLocalDeletionTime() {
		return sstableMetadata.minLocalDeletionTime;
	}

	public int getMaxLocalDeletionTime() {
		return sstableMetadata.maxLocalDeletionTime;
	}

	public boolean mayHaveTombstones() {
		return (!(descriptor.version.storeRows())) || ((getMinLocalDeletionTime()) != (Cell.NO_DELETION_TIME));
	}

	public int getMinTTL() {
		return sstableMetadata.minTTL;
	}

	public int getMaxTTL() {
		return sstableMetadata.maxTTL;
	}

	public long getTotalColumnsSet() {
		return sstableMetadata.totalColumnsSet;
	}

	public long getTotalRows() {
		return sstableMetadata.totalRows;
	}

	public int getAvgColumnSetPerRow() {
		return (sstableMetadata.totalRows) < 0 ? -1 : (sstableMetadata.totalRows) == 0 ? 0 : ((int) ((sstableMetadata.totalColumnsSet) / (sstableMetadata.totalRows)));
	}

	public int getSSTableLevel() {
		return sstableMetadata.sstableLevel;
	}

	public void reloadSSTableMetadata() throws IOException {
		this.sstableMetadata = ((StatsMetadata) (descriptor.getMetadataSerializer().deserialize(descriptor, MetadataType.STATS)));
	}

	public StatsMetadata getSSTableMetadata() {
		return sstableMetadata;
	}

	public RandomAccessReader openDataReader(RateLimiter limiter) {
		assert limiter != null;
		return dfile.createReader(limiter);
	}

	public RandomAccessReader openDataReader() {
		return dfile.createReader();
	}

	public RandomAccessReader openIndexReader() {
		if ((ifile) != null)
			return ifile.createReader();

		return null;
	}

	public ChannelProxy getDataChannel() {
		return dfile.channel;
	}

	public ChannelProxy getIndexChannel() {
		return ifile.channel;
	}

	public FileHandle getIndexFile() {
		return ifile;
	}

	public long getCreationTimeFor(Component component) {
		return new File(descriptor.filenameFor(component)).lastModified();
	}

	public long getKeyCacheHit() {
		return keyCacheHit.get();
	}

	public long getKeyCacheRequest() {
		return keyCacheRequest.get();
	}

	public void incrementReadCount() {
		if ((readMeter) != null)
			readMeter.mark();

	}

	public EncodingStats stats() {
		return new EncodingStats(getMinTimestamp(), getMinLocalDeletionTime(), getMinTTL());
	}

	public Ref<SSTableReader> tryRef() {
		return selfRef.tryRef();
	}

	public Ref<SSTableReader> selfRef() {
		return selfRef;
	}

	public Ref<SSTableReader> ref() {
		return selfRef.ref();
	}

	void setup(boolean trackHotness) {
		tidy.setup(this, trackHotness);
		this.readMeter = tidy.global.readMeter;
	}

	@com.google.common.annotations.VisibleForTesting
	public void overrideReadMeter(RestorableMeter readMeter) {
		this.readMeter = tidy.global.readMeter = readMeter;
	}

	public void addTo(Ref.IdentityCollection identities) {
		identities.add(this);
		identities.add(tidy.globalRef);
		dfile.addTo(identities);
		ifile.addTo(identities);
		bf.addTo(identities);
		indexSummary.addTo(identities);
	}

	private static final class InstanceTidier implements RefCounted.Tidy {
		private final Descriptor descriptor;

		private final CFMetaData metadata;

		private IFilter bf;

		private IndexSummary summary;

		private FileHandle dfile;

		private FileHandle ifile;

		private Runnable runOnClose;

		private boolean isReplaced = false;

		private Ref<SSTableReader.GlobalTidy> globalRef;

		private SSTableReader.GlobalTidy global;

		private volatile boolean setup;

		void setup(SSTableReader reader, boolean trackHotness) {
			this.setup = true;
			this.bf = reader.bf;
			this.summary = reader.indexSummary;
			this.dfile = reader.dfile;
			this.ifile = reader.ifile;
			this.globalRef = SSTableReader.GlobalTidy.get(reader);
			this.global = globalRef.get();
			if (trackHotness)
				global.ensureReadMeter();

		}

		InstanceTidier(Descriptor descriptor, CFMetaData metadata) {
			this.descriptor = descriptor;
			this.metadata = metadata;
		}

		public void tidy() {
			if (SSTableReader.logger.isTraceEnabled())
				SSTableReader.logger.trace("Running instance tidier for {} with setup {}", descriptor, setup);

			if (!(setup))
				return;

			final ColumnFamilyStore cfs = Schema.instance.getColumnFamilyStoreInstance(metadata.cfId);
			final OpOrder.Barrier barrier;
			if (cfs != null) {
				barrier = cfs.readOrdering.newBarrier();
				barrier.issue();
			}else
				barrier = null;

			ScheduledExecutors.nonPeriodicTasks.execute(new Runnable() {
				public void run() {
					if (SSTableReader.logger.isTraceEnabled())
						SSTableReader.logger.trace("Async instance tidier for {}, before barrier", descriptor);

					if (barrier != null)
						barrier.await();

					if (SSTableReader.logger.isTraceEnabled())
						SSTableReader.logger.trace("Async instance tidier for {}, after barrier", descriptor);

					if ((bf) != null)
						bf.close();

					if ((summary) != null)
						summary.close();

					if ((runOnClose) != null)
						runOnClose.run();

					if ((dfile) != null)
						dfile.close();

					if ((ifile) != null)
						ifile.close();

					globalRef.release();
					if (SSTableReader.logger.isTraceEnabled())
						SSTableReader.logger.trace("Async instance tidier for {}, completed", descriptor);

				}
			});
		}

		public String name() {
			return descriptor.toString();
		}

		void releaseSummary() {
			summary.close();
			assert summary.isCleanedUp();
			summary = null;
		}
	}

	static final class GlobalTidy implements RefCounted.Tidy {
		static WeakReference<ScheduledFuture<?>> NULL = new WeakReference<>(null);

		static final ConcurrentMap<Descriptor, Ref<SSTableReader.GlobalTidy>> lookup = new ConcurrentHashMap<>();

		private final Descriptor desc;

		private RestorableMeter readMeter;

		private WeakReference<ScheduledFuture<?>> readMeterSyncFuture = SSTableReader.GlobalTidy.NULL;

		private volatile Runnable obsoletion;

		GlobalTidy(final SSTableReader reader) {
			this.desc = reader.descriptor;
		}

		void ensureReadMeter() {
			if ((readMeter) != null)
				return;

			if ((SchemaConstants.isLocalSystemKeyspace(desc.ksname)) || (DatabaseDescriptor.isClientOrToolInitialized())) {
				readMeter = null;
				readMeterSyncFuture = SSTableReader.GlobalTidy.NULL;
				return;
			}
			readMeter = SystemKeyspace.getSSTableReadMeter(desc.ksname, desc.cfname, desc.generation);
			readMeterSyncFuture = new WeakReference<>(SSTableReader.syncExecutor.scheduleAtFixedRate(new Runnable() {
				public void run() {
					if ((obsoletion) == null) {
						SSTableReader.meterSyncThrottle.acquire();
						SystemKeyspace.persistSSTableReadMeter(desc.ksname, desc.cfname, desc.generation, readMeter);
					}
				}
			}, 1, 5, TimeUnit.MINUTES));
		}

		private void stopReadMeterPersistence() {
			ScheduledFuture<?> readMeterSyncFutureLocal = readMeterSyncFuture.get();
			if (readMeterSyncFutureLocal != null) {
				readMeterSyncFutureLocal.cancel(true);
				readMeterSyncFuture = SSTableReader.GlobalTidy.NULL;
			}
		}

		public void tidy() {
			SSTableReader.GlobalTidy.lookup.remove(desc);
			if ((obsoletion) != null)
				obsoletion.run();

			NativeLibrary.trySkipCache(desc.filenameFor(Component.DATA), 0, 0);
			NativeLibrary.trySkipCache(desc.filenameFor(Component.PRIMARY_INDEX), 0, 0);
		}

		public String name() {
			return desc.toString();
		}

		@SuppressWarnings("resource")
		public static Ref<SSTableReader.GlobalTidy> get(SSTableReader sstable) {
			Descriptor descriptor = sstable.descriptor;
			Ref<SSTableReader.GlobalTidy> refc = SSTableReader.GlobalTidy.lookup.get(descriptor);
			if (refc != null)
				return refc.ref();

			final SSTableReader.GlobalTidy tidy = new SSTableReader.GlobalTidy(sstable);
			refc = new Ref<>(tidy, tidy);
			Ref<?> ex = SSTableReader.GlobalTidy.lookup.putIfAbsent(descriptor, refc);
			if (ex != null) {
				refc.close();
				throw new AssertionError();
			}
			return refc;
		}
	}

	@com.google.common.annotations.VisibleForTesting
	public static void resetTidying() {
		SSTableReader.GlobalTidy.lookup.clear();
	}

	public static abstract class Factory {
		public abstract SSTableReader open(final Descriptor descriptor, Set<Component> components, CFMetaData metadata, Long maxDataAge, StatsMetadata sstableMetadata, SSTableReader.OpenReason openReason, SerializationHeader header);
	}
}

