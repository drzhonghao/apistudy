

import com.google.common.collect.ImmutableSet;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.Operator;
import org.apache.cassandra.cql3.statements.IndexTarget;
import org.apache.cassandra.cql3.statements.IndexTarget.Type;
import org.apache.cassandra.cql3.statements.RequestValidations;
import org.apache.cassandra.db.CBuilder;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Columns;
import org.apache.cassandra.db.CompactTables;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.RangeTombstone;
import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.commitlog.CommitLogPosition;
import org.apache.cassandra.db.compaction.CompactionManager;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.filter.RowFilter.Expression;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.db.lifecycle.Tracker;
import org.apache.cassandra.db.lifecycle.View;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.CollectionType;
import org.apache.cassandra.db.marshal.EmptyType;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.ComplexColumnData;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.LocalPartitioner;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.IndexRegistry;
import org.apache.cassandra.index.SecondaryIndexBuilder;
import org.apache.cassandra.index.SecondaryIndexManager;
import org.apache.cassandra.index.TargetParser;
import org.apache.cassandra.index.internal.CassandraIndexFunctions;
import org.apache.cassandra.index.internal.CollatedViewIndexBuilder;
import org.apache.cassandra.index.internal.IndexEntry;
import org.apache.cassandra.index.transactions.IndexTransaction;
import org.apache.cassandra.index.transactions.UpdateTransaction;
import org.apache.cassandra.io.sstable.ReducingKeyIterator;
import org.apache.cassandra.io.sstable.SSTable;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.concurrent.Refs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.config.CFMetaData.Builder.create;
import static org.apache.cassandra.config.ColumnDefinition.Kind.CLUSTERING;
import static org.apache.cassandra.config.ColumnDefinition.Kind.PARTITION_KEY;
import static org.apache.cassandra.config.ColumnDefinition.Kind.REGULAR;
import static org.apache.cassandra.config.ColumnDefinition.Kind.STATIC;
import static org.apache.cassandra.cql3.statements.IndexTarget.Type.KEYS;
import static org.apache.cassandra.cql3.statements.IndexTarget.Type.KEYS_AND_VALUES;
import static org.apache.cassandra.cql3.statements.IndexTarget.Type.VALUES;
import static org.apache.cassandra.db.marshal.CollectionType.Kind.LIST;
import static org.apache.cassandra.db.marshal.CollectionType.Kind.MAP;
import static org.apache.cassandra.db.marshal.CollectionType.Kind.SET;
import static org.apache.cassandra.db.rows.Row.Deletion.regular;


public abstract class CassandraIndex implements Index {
	private static final Logger logger = LoggerFactory.getLogger(CassandraIndex.class);

	public final ColumnFamilyStore baseCfs;

	protected IndexMetadata metadata;

	protected ColumnFamilyStore indexCfs;

	protected ColumnDefinition indexedColumn;

	protected CassandraIndexFunctions functions;

	protected CassandraIndex(ColumnFamilyStore baseCfs, IndexMetadata indexDef) {
		this.baseCfs = baseCfs;
		setMetadata(indexDef);
	}

	protected boolean supportsOperator(ColumnDefinition indexedColumn, Operator operator) {
		return operator == (Operator.EQ);
	}

	protected abstract CBuilder buildIndexClusteringPrefix(ByteBuffer partitionKey, ClusteringPrefix prefix, CellPath path);

	public abstract IndexEntry decodeEntry(DecoratedKey indexedValue, Row indexEntry);

	public abstract boolean isStale(Row row, ByteBuffer indexValue, int nowInSec);

	protected abstract ByteBuffer getIndexedValue(ByteBuffer partitionKey, Clustering clustering, CellPath path, ByteBuffer cellValue);

	public ColumnDefinition getIndexedColumn() {
		return indexedColumn;
	}

	public ClusteringComparator getIndexComparator() {
		return indexCfs.metadata.comparator;
	}

	public ColumnFamilyStore getIndexCfs() {
		return indexCfs;
	}

	public void register(IndexRegistry registry) {
		registry.registerIndex(this);
	}

	public Callable<?> getInitializationTask() {
		return (isBuilt()) || (baseCfs.isEmpty()) ? null : getBuildIndexTask();
	}

	public IndexMetadata getIndexMetadata() {
		return metadata;
	}

	public Optional<ColumnFamilyStore> getBackingTable() {
		return (indexCfs) == null ? Optional.empty() : Optional.of(indexCfs);
	}

	public Callable<Void> getBlockingFlushTask() {
		return () -> {
			indexCfs.forceBlockingFlush();
			return null;
		};
	}

	public Callable<?> getInvalidateTask() {
		return () -> {
			invalidate();
			return null;
		};
	}

	public Callable<?> getMetadataReloadTask(IndexMetadata indexDef) {
		return () -> {
			indexCfs.metadata.reloadIndexMetadataProperties(baseCfs.metadata);
			indexCfs.reload();
			return null;
		};
	}

	@Override
	public void validate(ReadCommand command) throws InvalidRequestException {
		Optional<RowFilter.Expression> target = getTargetExpression(command.rowFilter().getExpressions());
		if (target.isPresent()) {
			ByteBuffer indexValue = target.get().getIndexValue();
			RequestValidations.checkFalse(((indexValue.remaining()) > (FBUtilities.MAX_UNSIGNED_SHORT)), "Index expression values may not be larger than 64K");
		}
	}

	private void setMetadata(IndexMetadata indexDef) {
		metadata = indexDef;
		Pair<ColumnDefinition, IndexTarget.Type> target = TargetParser.parse(baseCfs.metadata, indexDef);
		functions = CassandraIndex.getFunctions(indexDef, target);
		CFMetaData cfm = CassandraIndex.indexCfsMetadata(baseCfs.metadata, indexDef);
		indexCfs = ColumnFamilyStore.createColumnFamilyStore(baseCfs.keyspace, cfm.cfName, cfm, baseCfs.getTracker().loadsstables);
		indexedColumn = target.left;
	}

	public Callable<?> getTruncateTask(final long truncatedAt) {
		return () -> {
			indexCfs.discardSSTables(truncatedAt);
			return null;
		};
	}

	public boolean shouldBuildBlocking() {
		return true;
	}

	public boolean dependsOn(ColumnDefinition column) {
		return indexedColumn.name.equals(column.name);
	}

	public boolean supportsExpression(ColumnDefinition column, Operator operator) {
		return (indexedColumn.name.equals(column.name)) && (supportsOperator(indexedColumn, operator));
	}

	private boolean supportsExpression(RowFilter.Expression expression) {
		return supportsExpression(expression.column(), expression.operator());
	}

	public AbstractType<?> customExpressionValueType() {
		return null;
	}

	public long getEstimatedResultRows() {
		return indexCfs.getMeanColumns();
	}

	public BiFunction<PartitionIterator, ReadCommand, PartitionIterator> postProcessorFor(ReadCommand command) {
		return ( partitionIterator, readCommand) -> partitionIterator;
	}

	public RowFilter getPostIndexQueryFilter(RowFilter filter) {
		return getTargetExpression(filter.getExpressions()).map(filter::without).orElse(filter);
	}

	private Optional<RowFilter.Expression> getTargetExpression(List<RowFilter.Expression> expressions) {
		return expressions.stream().filter(this::supportsExpression).findFirst();
	}

	public Index.Searcher searcherFor(ReadCommand command) {
		Optional<RowFilter.Expression> target = getTargetExpression(command.rowFilter().getExpressions());
		if (target.isPresent()) {
		}
		return null;
	}

	public void validate(PartitionUpdate update) throws InvalidRequestException {
		switch (indexedColumn.kind) {
			case PARTITION_KEY :
				validatePartitionKey(update.partitionKey());
				break;
			case CLUSTERING :
				validateClusterings(update);
				break;
			case REGULAR :
				if (update.columns().regulars.contains(indexedColumn))
					validateRows(update);

				break;
			case STATIC :
				if (update.columns().statics.contains(indexedColumn))
					validateRows(Collections.singleton(update.staticRow()));

				break;
		}
	}

	public Index.Indexer indexerFor(final DecoratedKey key, final PartitionColumns columns, final int nowInSec, final OpOrder.Group opGroup, final IndexTransaction.Type transactionType) {
		if ((!(isPrimaryKeyIndex())) && (!(columns.contains(indexedColumn))))
			return null;

		return new Index.Indexer() {
			public void begin() {
			}

			public void partitionDelete(DeletionTime deletionTime) {
			}

			public void rangeTombstone(RangeTombstone tombstone) {
			}

			public void insertRow(Row row) {
				if (((row.isStatic()) && (!(indexedColumn.isStatic()))) && (!(indexedColumn.isPartitionKey())))
					return;

				if (isPrimaryKeyIndex()) {
					indexPrimaryKey(row.clustering(), getPrimaryKeyIndexLiveness(row), row.deletion());
				}else {
					if (indexedColumn.isComplex())
						indexCells(row.clustering(), row.getComplexColumnData(indexedColumn));
					else
						indexCell(row.clustering(), row.getCell(indexedColumn));

				}
			}

			public void removeRow(Row row) {
				if (isPrimaryKeyIndex())
					return;

				if (indexedColumn.isComplex())
					removeCells(row.clustering(), row.getComplexColumnData(indexedColumn));
				else
					removeCell(row.clustering(), row.getCell(indexedColumn));

			}

			public void updateRow(Row oldRow, Row newRow) {
				assert (oldRow.isStatic()) == (newRow.isStatic());
				if ((newRow.isStatic()) != (indexedColumn.isStatic()))
					return;

				if (isPrimaryKeyIndex())
					indexPrimaryKey(newRow.clustering(), newRow.primaryKeyLivenessInfo(), newRow.deletion());

				if (indexedColumn.isComplex()) {
					indexCells(newRow.clustering(), newRow.getComplexColumnData(indexedColumn));
					removeCells(oldRow.clustering(), oldRow.getComplexColumnData(indexedColumn));
				}else {
					indexCell(newRow.clustering(), newRow.getCell(indexedColumn));
					removeCell(oldRow.clustering(), oldRow.getCell(indexedColumn));
				}
			}

			public void finish() {
			}

			private void indexCells(Clustering clustering, Iterable<Cell> cells) {
				if (cells == null)
					return;

				for (Cell cell : cells)
					indexCell(clustering, cell);

			}

			private void indexCell(Clustering clustering, Cell cell) {
				if ((cell == null) || (!(cell.isLive(nowInSec))))
					return;

				insert(key.getKey(), clustering, cell, LivenessInfo.withExpirationTime(cell.timestamp(), cell.ttl(), cell.localDeletionTime()), opGroup);
			}

			private void removeCells(Clustering clustering, Iterable<Cell> cells) {
				if (cells == null)
					return;

				for (Cell cell : cells)
					removeCell(clustering, cell);

			}

			private void removeCell(Clustering clustering, Cell cell) {
				if ((cell == null) || (!(cell.isLive(nowInSec))))
					return;

				delete(key.getKey(), clustering, cell, opGroup, nowInSec);
			}

			private void indexPrimaryKey(final Clustering clustering, final LivenessInfo liveness, final Row.Deletion deletion) {
				if ((liveness.timestamp()) != (LivenessInfo.NO_TIMESTAMP))
					insert(key.getKey(), clustering, null, liveness, opGroup);

				if (!(deletion.isLive()))
					delete(key.getKey(), clustering, deletion.time(), opGroup);

			}

			private LivenessInfo getPrimaryKeyIndexLiveness(Row row) {
				long timestamp = row.primaryKeyLivenessInfo().timestamp();
				int ttl = row.primaryKeyLivenessInfo().ttl();
				for (Cell cell : row.cells()) {
					long cellTimestamp = cell.timestamp();
					if (cell.isLive(nowInSec)) {
						if (cellTimestamp > timestamp) {
							timestamp = cellTimestamp;
							ttl = cell.ttl();
						}
					}
				}
				return LivenessInfo.create(timestamp, ttl, nowInSec);
			}
		};
	}

	public void deleteStaleEntry(DecoratedKey indexKey, Clustering indexClustering, DeletionTime deletion, OpOrder.Group opGroup) {
		doDelete(indexKey, indexClustering, deletion, opGroup);
		CassandraIndex.logger.trace("Removed index entry for stale value {}", indexKey);
	}

	private void insert(ByteBuffer rowKey, Clustering clustering, Cell cell, LivenessInfo info, OpOrder.Group opGroup) {
		DecoratedKey valueKey = getIndexKeyFor(getIndexedValue(rowKey, clustering, cell));
		Row row = BTreeRow.noCellLiveRow(buildIndexClustering(rowKey, clustering, cell), info);
		PartitionUpdate upd = partitionUpdate(valueKey, row);
		indexCfs.apply(upd, UpdateTransaction.NO_OP, opGroup, null);
		CassandraIndex.logger.trace("Inserted entry into index for value {}", valueKey);
	}

	private void delete(ByteBuffer rowKey, Clustering clustering, Cell cell, OpOrder.Group opGroup, int nowInSec) {
		DecoratedKey valueKey = getIndexKeyFor(getIndexedValue(rowKey, clustering, cell));
		doDelete(valueKey, buildIndexClustering(rowKey, clustering, cell), new DeletionTime(cell.timestamp(), nowInSec), opGroup);
	}

	private void delete(ByteBuffer rowKey, Clustering clustering, DeletionTime deletion, OpOrder.Group opGroup) {
		DecoratedKey valueKey = getIndexKeyFor(getIndexedValue(rowKey, clustering, null));
		doDelete(valueKey, buildIndexClustering(rowKey, clustering, null), deletion, opGroup);
	}

	private void doDelete(DecoratedKey indexKey, Clustering indexClustering, DeletionTime deletion, OpOrder.Group opGroup) {
		Row row = BTreeRow.emptyDeletedRow(indexClustering, regular(deletion));
		PartitionUpdate upd = partitionUpdate(indexKey, row);
		indexCfs.apply(upd, UpdateTransaction.NO_OP, opGroup, null);
		CassandraIndex.logger.trace("Removed index entry for value {}", indexKey);
	}

	private void validatePartitionKey(DecoratedKey partitionKey) throws InvalidRequestException {
		assert indexedColumn.isPartitionKey();
		validateIndexedValue(getIndexedValue(partitionKey.getKey(), null, null));
	}

	private void validateClusterings(PartitionUpdate update) throws InvalidRequestException {
		assert indexedColumn.isClusteringColumn();
		for (Row row : update)
			validateIndexedValue(getIndexedValue(null, row.clustering(), null));

	}

	private void validateRows(Iterable<Row> rows) {
		assert !(indexedColumn.isPrimaryKeyColumn());
		for (Row row : rows) {
			if (indexedColumn.isComplex()) {
				ComplexColumnData data = row.getComplexColumnData(indexedColumn);
				if (data != null) {
					for (Cell cell : data) {
						validateIndexedValue(getIndexedValue(null, null, cell.path(), cell.value()));
					}
				}
			}else {
				validateIndexedValue(getIndexedValue(null, null, row.getCell(indexedColumn)));
			}
		}
	}

	private void validateIndexedValue(ByteBuffer value) {
		if ((value != null) && ((value.remaining()) >= (FBUtilities.MAX_UNSIGNED_SHORT)))
			throw new InvalidRequestException(String.format("Cannot index value of size %d for index %s on %s.%s(%s) (maximum allowed size=%d)", value.remaining(), metadata.name, baseCfs.metadata.ksName, baseCfs.metadata.cfName, indexedColumn.name.toString(), FBUtilities.MAX_UNSIGNED_SHORT));

	}

	private ByteBuffer getIndexedValue(ByteBuffer rowKey, Clustering clustering, Cell cell) {
		return getIndexedValue(rowKey, clustering, (cell == null ? null : cell.path()), (cell == null ? null : cell.value()));
	}

	private Clustering buildIndexClustering(ByteBuffer rowKey, Clustering clustering, Cell cell) {
		return buildIndexClusteringPrefix(rowKey, clustering, (cell == null ? null : cell.path())).build();
	}

	private DecoratedKey getIndexKeyFor(ByteBuffer value) {
		return indexCfs.decorateKey(value);
	}

	private PartitionUpdate partitionUpdate(DecoratedKey valueKey, Row row) {
		return PartitionUpdate.singleRowUpdate(indexCfs.metadata, valueKey, row);
	}

	private void invalidate() {
		Collection<ColumnFamilyStore> cfss = Collections.singleton(indexCfs);
		CompactionManager.instance.interruptCompactionForCFs(cfss, true);
		CompactionManager.instance.waitForCessation(cfss);
		Keyspace.writeOrder.awaitNewBarrier();
		indexCfs.forceBlockingFlush();
		indexCfs.readOrdering.awaitNewBarrier();
		indexCfs.invalidate();
	}

	private boolean isBuilt() {
		return SystemKeyspace.isIndexBuilt(baseCfs.keyspace.getName(), metadata.name);
	}

	private boolean isPrimaryKeyIndex() {
		return indexedColumn.isPrimaryKeyColumn();
	}

	private Callable<?> getBuildIndexTask() {
		return () -> {
			buildBlocking();
			return null;
		};
	}

	private void buildBlocking() {
		baseCfs.forceBlockingFlush();
		try (ColumnFamilyStore.RefViewFragment viewFragment = baseCfs.selectAndReference(View.selectFunction(SSTableSet.CANONICAL));Refs<SSTableReader> sstables = viewFragment.refs) {
			if (sstables.isEmpty()) {
				CassandraIndex.logger.info("No SSTable data for {}.{} to build index {} from, marking empty index as built", baseCfs.metadata.ksName, baseCfs.metadata.cfName, metadata.name);
				baseCfs.indexManager.markIndexBuilt(metadata.name);
				return;
			}
			CassandraIndex.logger.info("Submitting index build of {} for data in {}", metadata.name, CassandraIndex.getSSTableNames(sstables));
			SecondaryIndexBuilder builder = new CollatedViewIndexBuilder(baseCfs, Collections.singleton(this), new ReducingKeyIterator(sstables));
			Future<?> future = CompactionManager.instance.submitIndexBuild(builder);
			FBUtilities.waitOnFuture(future);
			indexCfs.forceBlockingFlush();
			baseCfs.indexManager.markIndexBuilt(metadata.name);
		}
		CassandraIndex.logger.info("Index build of {} complete", metadata.name);
	}

	private static String getSSTableNames(Collection<SSTableReader> sstables) {
		return StreamSupport.stream(sstables.spliterator(), false).map(SSTableReader::toString).collect(Collectors.joining(", "));
	}

	public static final CFMetaData indexCfsMetadata(CFMetaData baseCfsMetadata, IndexMetadata indexMetadata) {
		Pair<ColumnDefinition, IndexTarget.Type> target = TargetParser.parse(baseCfsMetadata, indexMetadata);
		CassandraIndexFunctions utils = CassandraIndex.getFunctions(indexMetadata, target);
		ColumnDefinition indexedColumn = target.left;
		AbstractType<?> indexedValueType = utils.getIndexedValueType(indexedColumn);
		CFMetaData.Builder builder = (indexMetadata.isKeys()) ? CFMetaData.Builder.create(baseCfsMetadata.ksName, baseCfsMetadata.indexColumnFamilyName(indexMetadata), true, false, false) : create(baseCfsMetadata.ksName, baseCfsMetadata.indexColumnFamilyName(indexMetadata));
		builder = builder.withId(baseCfsMetadata.cfId).withPartitioner(new LocalPartitioner(indexedValueType)).addPartitionKey(indexedColumn.name, indexedColumn.type).addClusteringColumn("partition_key", baseCfsMetadata.partitioner.partitionOrdering());
		if (indexMetadata.isKeys()) {
			CompactTables.DefaultNames names = CompactTables.defaultNameGenerator(ImmutableSet.of(indexedColumn.name.toString(), "partition_key"));
			builder = builder.addRegularColumn(names.defaultCompactValueName(), EmptyType.instance);
		}else {
			builder = utils.addIndexClusteringColumns(builder, baseCfsMetadata, indexedColumn);
		}
		return builder.build().reloadIndexMetadataProperties(baseCfsMetadata);
	}

	public static CassandraIndex newIndex(ColumnFamilyStore baseCfs, IndexMetadata indexMetadata) {
		return null;
	}

	static CassandraIndexFunctions getFunctions(IndexMetadata indexDef, Pair<ColumnDefinition, IndexTarget.Type> target) {
		if (indexDef.isKeys())
			return CassandraIndexFunctions.KEYS_INDEX_FUNCTIONS;

		ColumnDefinition indexedColumn = target.left;
		if ((indexedColumn.type.isCollection()) && (indexedColumn.type.isMultiCell())) {
			switch (((CollectionType) (indexedColumn.type)).kind) {
				case LIST :
					return CassandraIndexFunctions.COLLECTION_VALUE_INDEX_FUNCTIONS;
				case SET :
					return CassandraIndexFunctions.COLLECTION_KEY_INDEX_FUNCTIONS;
				case MAP :
					switch (target.right) {
						case KEYS :
							return CassandraIndexFunctions.COLLECTION_KEY_INDEX_FUNCTIONS;
						case KEYS_AND_VALUES :
							return CassandraIndexFunctions.COLLECTION_ENTRY_INDEX_FUNCTIONS;
						case VALUES :
							return CassandraIndexFunctions.COLLECTION_VALUE_INDEX_FUNCTIONS;
					}
					throw new AssertionError();
			}
		}
		switch (indexedColumn.kind) {
			case CLUSTERING :
				return CassandraIndexFunctions.CLUSTERING_COLUMN_INDEX_FUNCTIONS;
			case REGULAR :
			case STATIC :
				return CassandraIndexFunctions.REGULAR_COLUMN_INDEX_FUNCTIONS;
			case PARTITION_KEY :
				return CassandraIndexFunctions.PARTITION_KEY_INDEX_FUNCTIONS;
		}
		throw new AssertionError();
	}
}

