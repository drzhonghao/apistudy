

import com.googlecode.concurrenttrees.common.Iterables;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.cql3.Operator;
import org.apache.cassandra.cql3.statements.IndexTarget;
import org.apache.cassandra.cql3.statements.IndexTarget.Type;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.Memtable;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.RangeTombstone;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.lifecycle.Tracker;
import org.apache.cassandra.db.lifecycle.View;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.IndexRegistry;
import org.apache.cassandra.index.SecondaryIndexBuilder;
import org.apache.cassandra.index.TargetParser;
import org.apache.cassandra.index.sasi.conf.ColumnIndex;
import org.apache.cassandra.index.sasi.conf.IndexMode;
import org.apache.cassandra.index.sasi.disk.OnDiskIndexBuilder;
import org.apache.cassandra.index.sasi.disk.PerSSTableIndexWriter;
import org.apache.cassandra.index.sasi.plan.QueryPlan;
import org.apache.cassandra.index.transactions.IndexTransaction;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableFlushObserver;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.notifications.INotification;
import org.apache.cassandra.notifications.INotificationConsumer;
import org.apache.cassandra.notifications.MemtableDiscardedNotification;
import org.apache.cassandra.notifications.MemtableRenewedNotification;
import org.apache.cassandra.notifications.MemtableSwitchedNotification;
import org.apache.cassandra.notifications.SSTableAddedNotification;
import org.apache.cassandra.notifications.SSTableListChangedNotification;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.memory.MemtableAllocator;

import static org.apache.cassandra.index.sasi.disk.OnDiskIndexBuilder.Mode.SPARSE;
import static org.apache.cassandra.index.transactions.IndexTransaction.Type.UPDATE;


public class SASIIndex implements Index , INotificationConsumer {
	private static class SASIIndexBuildingSupport implements Index.IndexBuildingSupport {
		public SecondaryIndexBuilder getIndexBuildTask(ColumnFamilyStore cfs, Set<Index> indexes, Collection<SSTableReader> sstablesToRebuild) {
			NavigableMap<SSTableReader, Map<ColumnDefinition, ColumnIndex>> sstables = new TreeMap<>(( a, b) -> {
				return Integer.compare(a.descriptor.generation, b.descriptor.generation);
			});
			indexes.stream().filter(( i) -> i instanceof SASIIndex).forEach(( i) -> {
				SASIIndex sasi = ((SASIIndex) (i));
				sasi.index.dropData(sstablesToRebuild);
				sstablesToRebuild.stream().filter(( sstable) -> !(sasi.index.hasSSTable(sstable))).forEach(( sstable) -> {
					Map<ColumnDefinition, ColumnIndex> toBuild = sstables.get(sstable);
					if (toBuild == null)
						sstables.put(sstable, (toBuild = new HashMap<>()));

					toBuild.put(sasi.index.getDefinition(), sasi.index);
				});
			});
			return null;
		}
	}

	private static final SASIIndex.SASIIndexBuildingSupport INDEX_BUILDER_SUPPORT = new SASIIndex.SASIIndexBuildingSupport();

	private final ColumnFamilyStore baseCfs;

	private final IndexMetadata config;

	private final ColumnIndex index;

	public SASIIndex(ColumnFamilyStore baseCfs, IndexMetadata config) {
		this.baseCfs = baseCfs;
		this.config = config;
		ColumnDefinition column = TargetParser.parse(baseCfs.metadata, config).left;
		this.index = new ColumnIndex(baseCfs.metadata.getKeyValidator(), column, config);
		Tracker tracker = baseCfs.getTracker();
		tracker.subscribe(this);
		SortedMap<SSTableReader, Map<ColumnDefinition, ColumnIndex>> toRebuild = new TreeMap<>(( a, b) -> Integer.compare(a.descriptor.generation, b.descriptor.generation));
		for (SSTableReader sstable : index.init(tracker.getView().liveSSTables())) {
			Map<ColumnDefinition, ColumnIndex> perSSTable = toRebuild.get(sstable);
			if (perSSTable == null)
				toRebuild.put(sstable, (perSSTable = new HashMap<>()));

			perSSTable.put(index.getDefinition(), index);
		}
	}

	public static Map<String, String> validateOptions(Map<String, String> options, CFMetaData cfm) {
		if (!((cfm.partitioner) instanceof Murmur3Partitioner))
			throw new ConfigurationException("SASI only supports Murmur3Partitioner.");

		String targetColumn = options.get("target");
		if (targetColumn == null)
			throw new ConfigurationException("unknown target column");

		Pair<ColumnDefinition, IndexTarget.Type> target = TargetParser.parse(cfm, targetColumn);
		if (target == null)
			throw new ConfigurationException(("failed to retrieve target column for: " + targetColumn));

		if (target.left.isComplex())
			throw new ConfigurationException("complex columns are not yet supported by SASI");

		if (target.left.isPartitionKey())
			throw new ConfigurationException("partition key columns are not yet supported by SASI");

		IndexMode.validateAnalyzer(options);
		IndexMode mode = IndexMode.getMode(target.left, options);
		if ((mode.mode) == (SPARSE)) {
			if (mode.isLiteral)
				throw new ConfigurationException("SPARSE mode is only supported on non-literal columns.");

			if (mode.isAnalyzed)
				throw new ConfigurationException("SPARSE mode doesn't support analyzers.");

		}
		return Collections.emptyMap();
	}

	public void register(IndexRegistry registry) {
		registry.registerIndex(this);
	}

	public IndexMetadata getIndexMetadata() {
		return config;
	}

	public Callable<?> getInitializationTask() {
		return null;
	}

	public Callable<?> getMetadataReloadTask(IndexMetadata indexMetadata) {
		return null;
	}

	public Callable<?> getBlockingFlushTask() {
		return null;
	}

	public Callable<?> getInvalidateTask() {
		return getTruncateTask(FBUtilities.timestampMicros());
	}

	public Callable<?> getTruncateTask(long truncatedAt) {
		return () -> {
			index.dropData(truncatedAt);
			return null;
		};
	}

	public boolean shouldBuildBlocking() {
		return true;
	}

	public Optional<ColumnFamilyStore> getBackingTable() {
		return Optional.empty();
	}

	public boolean indexes(PartitionColumns columns) {
		return columns.contains(index.getDefinition());
	}

	public boolean dependsOn(ColumnDefinition column) {
		return (index.getDefinition().compareTo(column)) == 0;
	}

	public boolean supportsExpression(ColumnDefinition column, Operator operator) {
		return (dependsOn(column)) && (index.supports(operator));
	}

	public AbstractType<?> customExpressionValueType() {
		return null;
	}

	public RowFilter getPostIndexQueryFilter(RowFilter filter) {
		return filter.withoutExpressions();
	}

	public long getEstimatedResultRows() {
		return Long.MIN_VALUE;
	}

	public void validate(PartitionUpdate update) throws InvalidRequestException {
	}

	public Index.Indexer indexerFor(DecoratedKey key, PartitionColumns columns, int nowInSec, OpOrder.Group opGroup, IndexTransaction.Type transactionType) {
		return new Index.Indexer() {
			public void begin() {
			}

			public void partitionDelete(DeletionTime deletionTime) {
			}

			public void rangeTombstone(RangeTombstone tombstone) {
			}

			public void insertRow(Row row) {
				if (isNewData())
					adjustMemtableSize(index.index(key, row), opGroup);

			}

			public void updateRow(Row oldRow, Row newRow) {
				insertRow(newRow);
			}

			public void removeRow(Row row) {
			}

			public void finish() {
			}

			private boolean isNewData() {
				return transactionType == (UPDATE);
			}

			public void adjustMemtableSize(long additionalSpace, OpOrder.Group opGroup) {
				baseCfs.getTracker().getView().getCurrentMemtable().getAllocator().onHeap().allocate(additionalSpace, opGroup);
			}
		};
	}

	public Index.Searcher searcherFor(ReadCommand command) throws InvalidRequestException {
		CFMetaData config = command.metadata();
		ColumnFamilyStore cfs = Schema.instance.getColumnFamilyStoreInstance(config.cfId);
		return ( controller) -> new QueryPlan(cfs, command, DatabaseDescriptor.getRangeRpcTimeout()).execute(controller);
	}

	public SSTableFlushObserver getFlushObserver(Descriptor descriptor, OperationType opType) {
		return SASIIndex.newWriter(baseCfs.metadata.getKeyValidator(), descriptor, Collections.singletonMap(index.getDefinition(), index), opType);
	}

	public BiFunction<PartitionIterator, ReadCommand, PartitionIterator> postProcessorFor(ReadCommand command) {
		return ( partitionIterator, readCommand) -> partitionIterator;
	}

	public Index.IndexBuildingSupport getBuildTaskSupport() {
		return SASIIndex.INDEX_BUILDER_SUPPORT;
	}

	public void handleNotification(INotification notification, Object sender) {
		if (notification instanceof SSTableAddedNotification) {
			SSTableAddedNotification notice = ((SSTableAddedNotification) (notification));
			index.update(Collections.<SSTableReader>emptyList(), Iterables.toList(notice.added));
		}else
			if (notification instanceof SSTableListChangedNotification) {
				SSTableListChangedNotification notice = ((SSTableListChangedNotification) (notification));
				index.update(notice.removed, notice.added);
			}else
				if (notification instanceof MemtableRenewedNotification) {
					index.switchMemtable();
				}else
					if (notification instanceof MemtableSwitchedNotification) {
						index.switchMemtable(((MemtableSwitchedNotification) (notification)).memtable);
					}else
						if (notification instanceof MemtableDiscardedNotification) {
							index.discardMemtable(((MemtableDiscardedNotification) (notification)).memtable);
						}




	}

	public ColumnIndex getIndex() {
		return index;
	}

	protected static PerSSTableIndexWriter newWriter(AbstractType<?> keyValidator, Descriptor descriptor, Map<ColumnDefinition, ColumnIndex> indexes, OperationType opType) {
		return new PerSSTableIndexWriter(keyValidator, descriptor, opType, indexes);
	}
}

