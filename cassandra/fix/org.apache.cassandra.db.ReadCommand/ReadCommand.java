

import com.google.common.collect.Lists;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.cql3.Operator;
import org.apache.cassandra.db.AbstractClusteringPrefix;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.ClusteringBoundOrBoundary;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Columns;
import org.apache.cassandra.db.DataRange;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.LegacyLayout;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.PartitionRangeReadCommand;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.ReadQuery;
import org.apache.cassandra.db.ReadResponse;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.TypeSizes;
import org.apache.cassandra.db.UnknownColumnException;
import org.apache.cassandra.db.UnknownColumnFamilyException;
import org.apache.cassandra.db.compaction.CompactionStrategyManager;
import org.apache.cassandra.db.filter.AbstractClusteringIndexFilter;
import org.apache.cassandra.db.filter.ClusteringIndexFilter;
import org.apache.cassandra.db.filter.ClusteringIndexNamesFilter;
import org.apache.cassandra.db.filter.ClusteringIndexSliceFilter;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.filter.DataLimits;
import org.apache.cassandra.db.filter.RowFilter;
import org.apache.cassandra.db.filter.RowFilter.Expression;
import org.apache.cassandra.db.filter.TombstoneOverwhelmingException;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.monitoring.ApproximateTime;
import org.apache.cassandra.db.monitoring.MonitorableImpl;
import org.apache.cassandra.db.partitions.BasePartitionIterator;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.partitions.PurgeFunction;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.db.partitions.UnfilteredPartitionIterators;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.RangeTombstoneMarker;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.transform.StoppingTransformation;
import org.apache.cassandra.db.transform.Transformation;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.IPartitionerDependentSerializer;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.IndexNotAvailableException;
import org.apache.cassandra.index.SecondaryIndexManager;
import org.apache.cassandra.io.ForwardingVersionedSerializer;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.metrics.TableMetrics;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.IndexMetadata;
import org.apache.cassandra.schema.Indexes;
import org.apache.cassandra.schema.UnknownIndexException;
import org.apache.cassandra.service.ClientWarn;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.CloseableIterator;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.db.LegacyLayout.LegacyBound.BOTTOM;
import static org.apache.cassandra.db.filter.ClusteringIndexFilter.Kind.NAMES;
import static org.apache.cassandra.db.filter.ClusteringIndexFilter.Kind.SLICE;
import static org.apache.cassandra.db.filter.DataLimits.Kind.CQL_LIMIT;
import static org.apache.cassandra.db.filter.DataLimits.Kind.THRIFT_LIMIT;


public abstract class ReadCommand extends MonitorableImpl implements ReadQuery {
	private static final int TEST_ITERATION_DELAY_MILLIS = Integer.parseInt(System.getProperty("cassandra.test.read_iteration_delay_ms", "0"));

	protected static final Logger logger = LoggerFactory.getLogger(ReadCommand.class);

	public static final IVersionedSerializer<ReadCommand> serializer = new ReadCommand.Serializer();

	public static final IVersionedSerializer<ReadCommand> readSerializer = new ForwardingVersionedSerializer<ReadCommand>() {
		protected IVersionedSerializer<ReadCommand> delegate(int version) {
			return version < (MessagingService.VERSION_30) ? ReadCommand.legacyReadCommandSerializer : ReadCommand.serializer;
		}
	};

	public static final IVersionedSerializer<ReadCommand> rangeSliceSerializer = new ForwardingVersionedSerializer<ReadCommand>() {
		protected IVersionedSerializer<ReadCommand> delegate(int version) {
			return version < (MessagingService.VERSION_30) ? ReadCommand.legacyRangeSliceCommandSerializer : ReadCommand.serializer;
		}
	};

	public static final IVersionedSerializer<ReadCommand> pagedRangeSerializer = new ForwardingVersionedSerializer<ReadCommand>() {
		protected IVersionedSerializer<ReadCommand> delegate(int version) {
			return version < (MessagingService.VERSION_30) ? ReadCommand.legacyPagedRangeCommandSerializer : ReadCommand.serializer;
		}
	};

	public static final IVersionedSerializer<ReadCommand> legacyRangeSliceCommandSerializer = new ReadCommand.LegacyRangeSliceCommandSerializer();

	public static final IVersionedSerializer<ReadCommand> legacyPagedRangeCommandSerializer = new ReadCommand.LegacyPagedRangeCommandSerializer();

	public static final IVersionedSerializer<ReadCommand> legacyReadCommandSerializer = new ReadCommand.LegacyReadCommandSerializer();

	private final ReadCommand.Kind kind;

	private final CFMetaData metadata;

	private final int nowInSec;

	private final ColumnFilter columnFilter;

	private final RowFilter rowFilter;

	private final DataLimits limits;

	private final boolean isDigestQuery;

	private int digestVersion;

	private final boolean isForThrift;

	@Nullable
	private final IndexMetadata index;

	protected static abstract class SelectionDeserializer {
		public abstract ReadCommand deserialize(DataInputPlus in, int version, boolean isDigest, int digestVersion, boolean isForThrift, CFMetaData metadata, int nowInSec, ColumnFilter columnFilter, RowFilter rowFilter, DataLimits limits, IndexMetadata index) throws IOException;
	}

	protected enum Kind {
		;

		private final ReadCommand.SelectionDeserializer selectionDeserializer;

		Kind(ReadCommand.SelectionDeserializer selectionDeserializer) {
			this.selectionDeserializer = selectionDeserializer;
		}
	}

	protected ReadCommand(ReadCommand.Kind kind, boolean isDigestQuery, int digestVersion, boolean isForThrift, CFMetaData metadata, int nowInSec, ColumnFilter columnFilter, RowFilter rowFilter, DataLimits limits, IndexMetadata index) {
		this.kind = kind;
		this.isDigestQuery = isDigestQuery;
		this.digestVersion = digestVersion;
		this.isForThrift = isForThrift;
		this.metadata = metadata;
		this.nowInSec = nowInSec;
		this.columnFilter = columnFilter;
		this.rowFilter = rowFilter;
		this.limits = limits;
		this.index = index;
	}

	protected abstract void serializeSelection(DataOutputPlus out, int version) throws IOException;

	protected abstract long selectionSerializedSize(int version);

	public abstract boolean isLimitedToOnePartition();

	public abstract ReadCommand withUpdatedLimit(DataLimits newLimits);

	public CFMetaData metadata() {
		return metadata;
	}

	public int nowInSec() {
		return nowInSec;
	}

	public abstract long getTimeout();

	public ColumnFilter columnFilter() {
		return columnFilter;
	}

	public RowFilter rowFilter() {
		return rowFilter;
	}

	public DataLimits limits() {
		return limits;
	}

	public boolean isDigestQuery() {
		return isDigestQuery;
	}

	public int digestVersion() {
		return digestVersion;
	}

	public ReadCommand setDigestVersion(int digestVersion) {
		this.digestVersion = digestVersion;
		return this;
	}

	public boolean isForThrift() {
		return isForThrift;
	}

	@Nullable
	public IndexMetadata indexMetadata() {
		return index;
	}

	public abstract ClusteringIndexFilter clusteringIndexFilter(DecoratedKey key);

	public abstract ReadCommand copy();

	public abstract ReadCommand copyAsDigestQuery();

	protected abstract UnfilteredPartitionIterator queryStorage(ColumnFamilyStore cfs, ReadExecutionController executionController);

	protected abstract int oldestUnrepairedTombstone();

	public ReadResponse createResponse(UnfilteredPartitionIterator iterator) {
		return null;
	}

	long indexSerializedSize(int version) {
		return null != (index) ? IndexMetadata.serializer.serializedSize(index, version) : 0;
	}

	public Index getIndex(ColumnFamilyStore cfs) {
		return null != (index) ? cfs.indexManager.getIndex(index) : null;
	}

	static IndexMetadata findIndex(CFMetaData table, RowFilter rowFilter) {
		if ((table.getIndexes().isEmpty()) || (rowFilter.isEmpty()))
			return null;

		ColumnFamilyStore cfs = Keyspace.openAndGetStore(table);
		Index index = cfs.indexManager.getBestIndexFor(rowFilter);
		return null != index ? index.getIndexMetadata() : null;
	}

	public void maybeValidateIndex() {
		Index index = getIndex(Keyspace.openAndGetStore(metadata));
	}

	@SuppressWarnings("resource")
	public UnfilteredPartitionIterator executeLocally(ReadExecutionController executionController) {
		long startTimeNanos = System.nanoTime();
		ColumnFamilyStore cfs = Keyspace.openAndGetStore(metadata());
		Index index = getIndex(cfs);
		Index.Searcher searcher = null;
		if (index != null) {
			if (!(cfs.indexManager.isIndexQueryable(index)))
				throw new IndexNotAvailableException(index);

			Tracing.trace("Executing read on {}.{} using index {}", cfs.metadata.ksName, cfs.metadata.cfName, index.getIndexMetadata().name);
		}
		UnfilteredPartitionIterator resultIterator = (searcher == null) ? queryStorage(cfs, executionController) : searcher.search(executionController);
		try {
			resultIterator = withStateTracking(resultIterator);
			resultIterator = withMetricsRecording(withoutPurgeableTombstones(resultIterator, cfs), cfs.metric, startTimeNanos);
			RowFilter updatedFilter = (searcher == null) ? rowFilter() : index.getPostIndexQueryFilter(rowFilter());
			return limits().filter(updatedFilter.filter(resultIterator, nowInSec()), nowInSec(), selectsFullPartition());
		} catch (RuntimeException | Error e) {
			resultIterator.close();
			throw e;
		}
	}

	protected abstract void recordLatency(TableMetrics metric, long latencyNanos);

	public PartitionIterator executeInternal(ReadExecutionController controller) {
		return UnfilteredPartitionIterators.filter(executeLocally(controller), nowInSec());
	}

	public ReadExecutionController executionController() {
		return null;
	}

	private UnfilteredPartitionIterator withMetricsRecording(UnfilteredPartitionIterator iter, final TableMetrics metric, final long startTimeNanos) {
		class MetricRecording extends Transformation<UnfilteredRowIterator> {
			private final int failureThreshold = DatabaseDescriptor.getTombstoneFailureThreshold();

			private final int warningThreshold = DatabaseDescriptor.getTombstoneWarnThreshold();

			private final boolean respectTombstoneThresholds = !(SchemaConstants.isLocalSystemKeyspace(ReadCommand.this.metadata().ksName));

			private final boolean enforceStrictLiveness = metadata.enforceStrictLiveness();

			private int liveRows = 0;

			private int tombstones = 0;

			private DecoratedKey currentKey;

			@Override
			public UnfilteredRowIterator applyToPartition(UnfilteredRowIterator iter) {
				currentKey = iter.partitionKey();
				return Transformation.apply(iter, this);
			}

			@Override
			public Row applyToStatic(Row row) {
				return applyToRow(row);
			}

			@Override
			public Row applyToRow(Row row) {
				boolean hasTombstones = false;
				for (Cell cell : row.cells()) {
					if (!(cell.isLive(ReadCommand.this.nowInSec()))) {
						countTombstone(row.clustering());
						hasTombstones = true;
					}
				}
				if (row.hasLiveData(ReadCommand.this.nowInSec(), enforceStrictLiveness))
					++(liveRows);
				else
					if (((!(row.primaryKeyLivenessInfo().isLive(ReadCommand.this.nowInSec()))) && (row.hasDeletion(ReadCommand.this.nowInSec()))) && (!hasTombstones)) {
						countTombstone(row.clustering());
					}

				return row;
			}

			@Override
			public RangeTombstoneMarker applyToMarker(RangeTombstoneMarker marker) {
				countTombstone(marker.clustering());
				return marker;
			}

			private void countTombstone(ClusteringPrefix clustering) {
				++(tombstones);
				if (((tombstones) > (failureThreshold)) && (respectTombstoneThresholds)) {
					String query = ReadCommand.this.toCQLString();
					Tracing.trace("Scanned over {} tombstones for query {}; query aborted (see tombstone_failure_threshold)", failureThreshold, query);
					throw new TombstoneOverwhelmingException(tombstones, query, ReadCommand.this.metadata(), currentKey, clustering);
				}
			}

			@Override
			public void onClose() {
				recordLatency(metric, ((System.nanoTime()) - startTimeNanos));
				metric.tombstoneScannedHistogram.update(tombstones);
				metric.liveScannedHistogram.update(liveRows);
				boolean warnTombstones = ((tombstones) > (warningThreshold)) && (respectTombstoneThresholds);
				if (warnTombstones) {
					String msg = String.format("Read %d live rows and %d tombstone cells for query %1.512s (see tombstone_warn_threshold)", liveRows, tombstones, ReadCommand.this.toCQLString());
					ClientWarn.instance.warn(msg);
					ReadCommand.logger.warn(msg);
				}
				Tracing.trace("Read {} live rows and {} tombstone cells{}", liveRows, tombstones, (warnTombstones ? " (see tombstone_warn_threshold)" : ""));
			}
		}
		return Transformation.apply(iter, new MetricRecording());
	}

	protected class CheckForAbort extends StoppingTransformation<UnfilteredRowIterator> {
		long lastChecked = 0;

		protected UnfilteredRowIterator applyToPartition(UnfilteredRowIterator partition) {
			if (maybeAbort()) {
				partition.close();
				return null;
			}
			return Transformation.apply(partition, this);
		}

		protected Row applyToRow(Row row) {
			if ((ReadCommand.TEST_ITERATION_DELAY_MILLIS) > 0)
				maybeDelayForTesting();

			return maybeAbort() ? null : row;
		}

		private boolean maybeAbort() {
			if ((lastChecked) == (ApproximateTime.currentTimeMillis()))
				return false;

			lastChecked = ApproximateTime.currentTimeMillis();
			if (isAborted()) {
				stop();
				return true;
			}
			return false;
		}

		private void maybeDelayForTesting() {
			if (!(metadata.ksName.startsWith("system")))
				FBUtilities.sleepQuietly(ReadCommand.TEST_ITERATION_DELAY_MILLIS);

		}
	}

	protected UnfilteredPartitionIterator withStateTracking(UnfilteredPartitionIterator iter) {
		return Transformation.apply(iter, new ReadCommand.CheckForAbort());
	}

	public abstract MessageOut<ReadCommand> createMessage(int version);

	protected abstract void appendCQLWhereClause(StringBuilder sb);

	protected UnfilteredPartitionIterator withoutPurgeableTombstones(UnfilteredPartitionIterator iterator, ColumnFamilyStore cfs) {
		final boolean isForThrift = iterator.isForThrift();
		class WithoutPurgeableTombstones extends PurgeFunction {
			public WithoutPurgeableTombstones() {
				super(isForThrift, nowInSec(), cfs.gcBefore(nowInSec()), oldestUnrepairedTombstone(), cfs.getCompactionStrategyManager().onlyPurgeRepairedTombstones(), cfs.metadata.enforceStrictLiveness());
			}

			protected Predicate<Long> getPurgeEvaluator() {
				return ( time) -> true;
			}
		}
		return Transformation.apply(iterator, new WithoutPurgeableTombstones());
	}

	public String toCQLString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ").append(columnFilter());
		sb.append(" FROM ").append(metadata().ksName).append('.').append(metadata.cfName);
		appendCQLWhereClause(sb);
		if ((limits()) != (DataLimits.NONE))
			sb.append(' ').append(limits());

		return sb.toString();
	}

	public String name() {
		return toCQLString();
	}

	private static class Serializer implements IVersionedSerializer<ReadCommand> {
		private static int digestFlag(boolean isDigest) {
			return isDigest ? 1 : 0;
		}

		private static boolean isDigest(int flags) {
			return (flags & 1) != 0;
		}

		private static int thriftFlag(boolean isForThrift) {
			return isForThrift ? 2 : 0;
		}

		private static boolean isForThrift(int flags) {
			return (flags & 2) != 0;
		}

		private static int indexFlag(boolean hasIndex) {
			return hasIndex ? 4 : 0;
		}

		private static boolean hasIndex(int flags) {
			return (flags & 4) != 0;
		}

		public void serialize(ReadCommand command, DataOutputPlus out, int version) throws IOException {
			assert version >= (MessagingService.VERSION_30);
			out.writeByte(command.kind.ordinal());
			out.writeByte((((ReadCommand.Serializer.digestFlag(command.isDigestQuery())) | (ReadCommand.Serializer.thriftFlag(command.isForThrift()))) | (ReadCommand.Serializer.indexFlag((null != (command.index))))));
			if (command.isDigestQuery())
				out.writeUnsignedVInt(command.digestVersion());

			CFMetaData.serializer.serialize(command.metadata(), out, version);
			out.writeInt(command.nowInSec());
			ColumnFilter.serializer.serialize(command.columnFilter(), out, version);
			RowFilter.serializer.serialize(command.rowFilter(), out, version);
			DataLimits.serializer.serialize(command.limits(), out, version, command.metadata.comparator);
			if (null != (command.index))
				IndexMetadata.serializer.serialize(command.index, out, version);

			command.serializeSelection(out, version);
		}

		public ReadCommand deserialize(DataInputPlus in, int version) throws IOException {
			assert version >= (MessagingService.VERSION_30);
			ReadCommand.Kind kind = ReadCommand.Kind.values()[in.readByte()];
			int flags = in.readByte();
			boolean isDigest = ReadCommand.Serializer.isDigest(flags);
			boolean isForThrift = ReadCommand.Serializer.isForThrift(flags);
			boolean hasIndex = ReadCommand.Serializer.hasIndex(flags);
			int digestVersion = (isDigest) ? ((int) (in.readUnsignedVInt())) : 0;
			CFMetaData metadata = CFMetaData.serializer.deserialize(in, version);
			int nowInSec = in.readInt();
			ColumnFilter columnFilter = ColumnFilter.serializer.deserialize(in, version, metadata);
			RowFilter rowFilter = RowFilter.serializer.deserialize(in, version, metadata);
			DataLimits limits = DataLimits.serializer.deserialize(in, version, metadata.comparator);
			IndexMetadata index = (hasIndex) ? deserializeIndexMetadata(in, version, metadata) : null;
			return kind.selectionDeserializer.deserialize(in, version, isDigest, digestVersion, isForThrift, metadata, nowInSec, columnFilter, rowFilter, limits, index);
		}

		private IndexMetadata deserializeIndexMetadata(DataInputPlus in, int version, CFMetaData cfm) throws IOException {
			try {
				return IndexMetadata.serializer.deserialize(in, version, cfm);
			} catch (UnknownIndexException e) {
				ReadCommand.logger.info(("Couldn't find a defined index on {}.{} with the id {}. " + (("If an index was just created, this is likely due to the schema not " + "being fully propagated. Local read will proceed without using the ") + "index. Please wait for schema agreement after index creation.")), cfm.ksName, cfm.cfName, e.indexId);
				return null;
			}
		}

		public long serializedSize(ReadCommand command, int version) {
			assert version >= (MessagingService.VERSION_30);
			return (((((((2 + (command.isDigestQuery() ? TypeSizes.sizeofUnsignedVInt(command.digestVersion()) : 0)) + (CFMetaData.serializer.serializedSize(command.metadata(), version))) + (TypeSizes.sizeof(command.nowInSec()))) + (ColumnFilter.serializer.serializedSize(command.columnFilter(), version))) + (RowFilter.serializer.serializedSize(command.rowFilter(), version))) + (DataLimits.serializer.serializedSize(command.limits(), version, command.metadata.comparator))) + (command.selectionSerializedSize(version))) + (command.indexSerializedSize(version));
		}
	}

	private enum LegacyType {

		GET_BY_NAMES(((byte) (1))),
		GET_SLICES(((byte) (2)));
		public final byte serializedValue;

		LegacyType(byte b) {
			this.serializedValue = b;
		}

		public static ReadCommand.LegacyType fromPartitionFilterKind(ClusteringIndexFilter.Kind kind) {
			return kind == (SLICE) ? ReadCommand.LegacyType.GET_SLICES : ReadCommand.LegacyType.GET_BY_NAMES;
		}

		public static ReadCommand.LegacyType fromSerializedValue(byte b) {
			return b == 1 ? ReadCommand.LegacyType.GET_BY_NAMES : ReadCommand.LegacyType.GET_SLICES;
		}
	}

	private static class LegacyRangeSliceCommandSerializer implements IVersionedSerializer<ReadCommand> {
		public void serialize(ReadCommand command, DataOutputPlus out, int version) throws IOException {
			assert version < (MessagingService.VERSION_30);
			out.writeBoolean(false);
		}

		public ReadCommand deserialize(DataInputPlus in, int version) throws IOException {
			assert version < (MessagingService.VERSION_30);
			String keyspace = in.readUTF();
			String columnFamily = in.readUTF();
			CFMetaData metadata = Schema.instance.getCFMetaData(keyspace, columnFamily);
			if (metadata == null) {
				String message = String.format("Got legacy range command for nonexistent table %s.%s.", keyspace, columnFamily);
				throw new UnknownColumnFamilyException(message, null);
			}
			int nowInSec = ((int) ((in.readLong()) / 1000));
			ClusteringIndexFilter filter;
			ColumnFilter selection;
			int compositesToGroup = 0;
			int perPartitionLimit = -1;
			byte readType = in.readByte();
			if (readType == 1) {
				Pair<ColumnFilter, ClusteringIndexNamesFilter> selectionAndFilter = ReadCommand.LegacyReadCommandSerializer.deserializeNamesSelectionAndFilter(in, metadata);
				selection = selectionAndFilter.left;
				filter = selectionAndFilter.right;
			}else {
				Pair<ClusteringIndexSliceFilter, Boolean> p = ReadCommand.LegacyReadCommandSerializer.deserializeSlicePartitionFilter(in, metadata);
				filter = p.left;
				perPartitionLimit = in.readInt();
				compositesToGroup = in.readInt();
				selection = ReadCommand.LegacyRangeSliceCommandSerializer.getColumnSelectionForSlice(p.right, compositesToGroup, metadata);
			}
			RowFilter rowFilter = ReadCommand.LegacyRangeSliceCommandSerializer.deserializeRowFilter(in, metadata);
			AbstractBounds<PartitionPosition> keyRange = AbstractBounds.rowPositionSerializer.deserialize(in, metadata.partitioner, version);
			int maxResults = in.readInt();
			boolean countCQL3Rows = in.readBoolean();
			in.readBoolean();
			boolean selectsStatics = (!(selection.fetchedColumns().statics.isEmpty())) || (filter.selects(Clustering.STATIC_CLUSTERING));
			boolean isDistinct = (compositesToGroup == (-2)) || ((compositesToGroup != (-1)) && (!countCQL3Rows));
			DataLimits limits;
			if (isDistinct)
				limits = DataLimits.distinctLimits(maxResults);
			else
				if (compositesToGroup == (-1))
					limits = DataLimits.thriftLimits(maxResults, perPartitionLimit);
				else
					limits = DataLimits.cqlLimits(maxResults);


			return null;
		}

		static void serializeRowFilter(DataOutputPlus out, RowFilter rowFilter) throws IOException {
			ArrayList<RowFilter.Expression> indexExpressions = Lists.newArrayList(rowFilter.iterator());
			out.writeInt(indexExpressions.size());
			for (RowFilter.Expression expression : indexExpressions) {
				ByteBufferUtil.writeWithShortLength(expression.column().name.bytes, out);
				expression.operator().writeTo(out);
				ByteBufferUtil.writeWithShortLength(expression.getIndexValue(), out);
			}
		}

		static RowFilter deserializeRowFilter(DataInputPlus in, CFMetaData metadata) throws IOException {
			int numRowFilters = in.readInt();
			if (numRowFilters == 0)
				return RowFilter.NONE;

			RowFilter rowFilter = RowFilter.create(numRowFilters);
			for (int i = 0; i < numRowFilters; i++) {
				ByteBuffer columnName = ByteBufferUtil.readWithShortLength(in);
				ColumnDefinition column = metadata.getColumnDefinition(columnName);
				Operator op = Operator.readFrom(in);
				ByteBuffer indexValue = ByteBufferUtil.readWithShortLength(in);
				rowFilter.add(column, op, indexValue);
			}
			return rowFilter;
		}

		static long serializedRowFilterSize(RowFilter rowFilter) {
			long size = TypeSizes.sizeof(0);
			for (RowFilter.Expression expression : rowFilter) {
				size += ByteBufferUtil.serializedSizeWithShortLength(expression.column().name.bytes);
				size += TypeSizes.sizeof(0);
				size += ByteBufferUtil.serializedSizeWithShortLength(expression.getIndexValue());
			}
			return size;
		}

		public long serializedSize(ReadCommand command, int version) {
			assert version < (MessagingService.VERSION_30);
			return 0L;
		}

		static PartitionRangeReadCommand maybeConvertNamesToSlice(PartitionRangeReadCommand command) {
			if (!(command.dataRange().isNamesQuery()))
				return command;

			CFMetaData metadata = command.metadata();
			if (!(ReadCommand.LegacyReadCommandSerializer.shouldConvertNamesToSlice(metadata, command.columnFilter().fetchedColumns())))
				return command;

			return null;
		}

		static ColumnFilter getColumnSelectionForSlice(boolean selectsStatics, int compositesToGroup, CFMetaData metadata) {
			if (compositesToGroup == (-2))
				return ColumnFilter.all(metadata);

			PartitionColumns columns = (selectsStatics) ? metadata.partitionColumns() : metadata.partitionColumns().withoutStatics();
			return ColumnFilter.selectionBuilder().addAll(columns).build();
		}
	}

	private static class LegacyPagedRangeCommandSerializer implements IVersionedSerializer<ReadCommand> {
		public void serialize(ReadCommand command, DataOutputPlus out, int version) throws IOException {
			assert version < (MessagingService.VERSION_30);
			ClusteringIndexSliceFilter filter;
			filter = null;
			out.writeBoolean(filter.isReversed());
			int compositesToGroup;
			compositesToGroup = 0;
			out.writeInt(compositesToGroup);
			filter = null;
			Slice lastSlice = filter.requestedSlices().get(((filter.requestedSlices().size()) - 1));
		}

		public ReadCommand deserialize(DataInputPlus in, int version) throws IOException {
			assert version < (MessagingService.VERSION_30);
			String keyspace = in.readUTF();
			String columnFamily = in.readUTF();
			CFMetaData metadata = Schema.instance.getCFMetaData(keyspace, columnFamily);
			if (metadata == null) {
				String message = String.format("Got legacy paged range command for nonexistent table %s.%s.", keyspace, columnFamily);
				throw new UnknownColumnFamilyException(message, null);
			}
			int nowInSec = ((int) ((in.readLong()) / 1000));
			AbstractBounds<PartitionPosition> keyRange = AbstractBounds.rowPositionSerializer.deserialize(in, metadata.partitioner, version);
			Pair<ClusteringIndexSliceFilter, Boolean> p = ReadCommand.LegacyReadCommandSerializer.deserializeSlicePartitionFilter(in, metadata);
			ClusteringIndexSliceFilter filter = p.left;
			boolean selectsStatics = p.right;
			int perPartitionLimit = in.readInt();
			int compositesToGroup = in.readInt();
			LegacyLayout.LegacyBound startBound = LegacyLayout.decodeBound(metadata, ByteBufferUtil.readWithShortLength(in), true);
			ByteBufferUtil.readWithShortLength(in);
			ColumnFilter selection = ReadCommand.LegacyRangeSliceCommandSerializer.getColumnSelectionForSlice(selectsStatics, compositesToGroup, metadata);
			RowFilter rowFilter = ReadCommand.LegacyRangeSliceCommandSerializer.deserializeRowFilter(in, metadata);
			int maxResults = in.readInt();
			boolean countCQL3Rows = in.readBoolean();
			boolean isDistinct = (compositesToGroup == (-2)) || ((compositesToGroup != (-1)) && (!countCQL3Rows));
			DataLimits limits;
			if (isDistinct)
				limits = DataLimits.distinctLimits(maxResults);
			else
				limits = DataLimits.cqlLimits(maxResults);

			limits = limits.forPaging(maxResults);
			DataRange dataRange = new DataRange(keyRange, filter);
			Slices slices = filter.requestedSlices();
			if (((!isDistinct) && (startBound != (BOTTOM))) && (!(startBound.bound.equals(slices.get(0).start())))) {
				dataRange = dataRange.forPaging(keyRange, metadata.comparator, startBound.getAsClustering(metadata), false);
			}
			return null;
		}

		public long serializedSize(ReadCommand command, int version) {
			assert version < (MessagingService.VERSION_30);
			ClusteringIndexSliceFilter filter;
			filter = null;
			Slice lastSlice = filter.requestedSlices().get(((filter.requestedSlices().size()) - 1));
			return 0L;
		}
	}

	static class LegacyReadCommandSerializer implements IVersionedSerializer<ReadCommand> {
		public void serialize(ReadCommand command, DataOutputPlus out, int version) throws IOException {
			assert version < (MessagingService.VERSION_30);
		}

		public ReadCommand deserialize(DataInputPlus in, int version) throws IOException {
			assert version < (MessagingService.VERSION_30);
			ReadCommand.LegacyType msgType = ReadCommand.LegacyType.fromSerializedValue(in.readByte());
			boolean isDigest = in.readBoolean();
			String keyspaceName = in.readUTF();
			ByteBuffer key = ByteBufferUtil.readWithShortLength(in);
			String cfName = in.readUTF();
			long nowInMillis = in.readLong();
			int nowInSeconds = ((int) (nowInMillis / 1000));
			CFMetaData metadata = Schema.instance.getCFMetaData(keyspaceName, cfName);
			DecoratedKey dk = metadata.partitioner.decorateKey(key);
			return null;
		}

		public long serializedSize(ReadCommand command, int version) {
			assert version < (MessagingService.VERSION_30);
			long size = 1;
			size += TypeSizes.sizeof(command.isDigestQuery());
			size += TypeSizes.sizeof(((long) (command.nowInSec())));
			return 0L;
		}

		private void serializeNamesCommand(SinglePartitionReadCommand command, DataOutputPlus out) throws IOException {
		}

		private static void serializeNamesFilter(ReadCommand command, ClusteringIndexNamesFilter filter, DataOutputPlus out) throws IOException {
			PartitionColumns columns = command.columnFilter().fetchedColumns();
			CFMetaData metadata = command.metadata();
			SortedSet<Clustering> requestedRows = filter.requestedRows();
			if (requestedRows.isEmpty()) {
				out.writeInt(columns.size());
				for (ColumnDefinition column : columns)
					ByteBufferUtil.writeWithShortLength(column.name.bytes, out);

			}else {
				out.writeInt(((requestedRows.size()) * (columns.size())));
				for (Clustering clustering : requestedRows) {
					for (ColumnDefinition column : columns)
						ByteBufferUtil.writeWithShortLength(LegacyLayout.encodeCellName(metadata, clustering, column.name.bytes, null), out);

				}
			}
			if ((command.isForThrift()) || (((command.limits().kind()) == (CQL_LIMIT)) && ((command.limits().perPartitionCount()) == 1)))
				out.writeBoolean(false);
			else
				out.writeBoolean(true);

		}

		static long serializedNamesFilterSize(ClusteringIndexNamesFilter filter, CFMetaData metadata, PartitionColumns fetchedColumns) {
			SortedSet<Clustering> requestedRows = filter.requestedRows();
			long size = 0;
			if (requestedRows.isEmpty()) {
				size += TypeSizes.sizeof(fetchedColumns.size());
				for (ColumnDefinition column : fetchedColumns)
					size += ByteBufferUtil.serializedSizeWithShortLength(column.name.bytes);

			}else {
				size += TypeSizes.sizeof(((requestedRows.size()) * (fetchedColumns.size())));
				for (Clustering clustering : requestedRows) {
					for (ColumnDefinition column : fetchedColumns)
						size += ByteBufferUtil.serializedSizeWithShortLength(LegacyLayout.encodeCellName(metadata, clustering, column.name.bytes, null));

				}
			}
			return size + (TypeSizes.sizeof(true));
		}

		private SinglePartitionReadCommand deserializeNamesCommand(DataInputPlus in, boolean isDigest, CFMetaData metadata, DecoratedKey key, int nowInSeconds, int version) throws IOException {
			Pair<ColumnFilter, ClusteringIndexNamesFilter> selectionAndFilter = ReadCommand.LegacyReadCommandSerializer.deserializeNamesSelectionAndFilter(in, metadata);
			return null;
		}

		static Pair<ColumnFilter, ClusteringIndexNamesFilter> deserializeNamesSelectionAndFilter(DataInputPlus in, CFMetaData metadata) throws IOException {
			int numCellNames = in.readInt();
			NavigableSet<Clustering> clusterings = new TreeSet<>(metadata.comparator);
			ColumnFilter.Builder selectionBuilder = ColumnFilter.selectionBuilder();
			for (int i = 0; i < numCellNames; i++) {
				ByteBuffer buffer = ByteBufferUtil.readWithShortLength(in);
				LegacyLayout.LegacyCellName cellName;
				try {
					cellName = LegacyLayout.decodeCellName(metadata, buffer);
				} catch (UnknownColumnException exc) {
					throw new UnknownColumnFamilyException((("Received legacy range read command with names filter for unrecognized column name. " + "Fill name in filter (hex): ") + (ByteBufferUtil.bytesToHex(buffer))), metadata.cfId);
				}
				if ((metadata.isStaticCompactTable()) && (cellName.clustering.equals(Clustering.STATIC_CLUSTERING))) {
					clusterings.add(Clustering.make(cellName.column.name.bytes));
					selectionBuilder.add(metadata.compactValueColumn());
				}else {
					clusterings.add(cellName.clustering);
				}
				selectionBuilder.add(cellName.column);
			}
			if ((metadata.isStaticCompactTable()) && (clusterings.isEmpty()))
				selectionBuilder.addAll(metadata.partitionColumns());

			in.readBoolean();
			ClusteringIndexNamesFilter filter = new ClusteringIndexNamesFilter(clusterings, false);
			return Pair.create(selectionBuilder.build(), filter);
		}

		private long serializedNamesCommandSize(SinglePartitionReadCommand command) {
			ClusteringIndexNamesFilter filter = ((ClusteringIndexNamesFilter) (command.clusteringIndexFilter()));
			PartitionColumns columns = command.columnFilter().fetchedColumns();
			return ReadCommand.LegacyReadCommandSerializer.serializedNamesFilterSize(filter, command.metadata(), columns);
		}

		private void serializeSliceCommand(SinglePartitionReadCommand command, DataOutputPlus out) throws IOException {
			CFMetaData metadata = command.metadata();
			ClusteringIndexSliceFilter filter = ((ClusteringIndexSliceFilter) (command.clusteringIndexFilter()));
			Slices slices = filter.requestedSlices();
			boolean makeStaticSlice = (!(command.columnFilter().fetchedColumns().statics.isEmpty())) && (!(slices.selects(Clustering.STATIC_CLUSTERING)));
			ReadCommand.LegacyReadCommandSerializer.serializeSlices(out, slices, filter.isReversed(), makeStaticSlice, metadata);
			out.writeBoolean(filter.isReversed());
			boolean selectsStatics = (!(command.columnFilter().fetchedColumns().statics.isEmpty())) || (slices.selects(Clustering.STATIC_CLUSTERING));
			DataLimits limits = command.limits();
			if (limits.isDistinct())
				out.writeInt(1);
			else
				out.writeInt(ReadCommand.LegacyReadCommandSerializer.updateLimitForQuery(command.limits().count(), filter.requestedSlices()));

			int compositesToGroup;
			if (((limits.kind()) == (THRIFT_LIMIT)) || (metadata.isDense()))
				compositesToGroup = -1;
			else
				if ((limits.isDistinct()) && (!selectsStatics))
					compositesToGroup = -2;
				else
					compositesToGroup = metadata.clusteringColumns().size();


			out.writeInt(compositesToGroup);
		}

		private SinglePartitionReadCommand deserializeSliceCommand(DataInputPlus in, boolean isDigest, CFMetaData metadata, DecoratedKey key, int nowInSeconds, int version) throws IOException {
			Pair<ClusteringIndexSliceFilter, Boolean> p = ReadCommand.LegacyReadCommandSerializer.deserializeSlicePartitionFilter(in, metadata);
			ClusteringIndexSliceFilter filter = p.left;
			boolean selectsStatics = p.right;
			int count = in.readInt();
			int compositesToGroup = in.readInt();
			ColumnFilter columnFilter = ReadCommand.LegacyRangeSliceCommandSerializer.getColumnSelectionForSlice(selectsStatics, compositesToGroup, metadata);
			DataLimits limits;
			if (compositesToGroup == (-2))
				limits = DataLimits.distinctLimits(count);
			else
				if (compositesToGroup == (-1))
					limits = DataLimits.thriftLimits(1, count);
				else
					limits = DataLimits.cqlLimits(count);


			return null;
		}

		private long serializedSliceCommandSize(SinglePartitionReadCommand command) {
			CFMetaData metadata = command.metadata();
			ClusteringIndexSliceFilter filter = ((ClusteringIndexSliceFilter) (command.clusteringIndexFilter()));
			Slices slices = filter.requestedSlices();
			boolean makeStaticSlice = (!(command.columnFilter().fetchedColumns().statics.isEmpty())) && (!(slices.selects(Clustering.STATIC_CLUSTERING)));
			long size = ReadCommand.LegacyReadCommandSerializer.serializedSlicesSize(slices, makeStaticSlice, metadata);
			size += TypeSizes.sizeof(command.clusteringIndexFilter().isReversed());
			size += TypeSizes.sizeof(command.limits().count());
			return size + (TypeSizes.sizeof(0));
		}

		static void serializeSlices(DataOutputPlus out, Slices slices, boolean isReversed, boolean makeStaticSlice, CFMetaData metadata) throws IOException {
			out.writeInt(((slices.size()) + (makeStaticSlice ? 1 : 0)));
			if (isReversed) {
				for (int i = (slices.size()) - 1; i >= 0; i--)
					ReadCommand.LegacyReadCommandSerializer.serializeSlice(out, slices.get(i), true, metadata);

				if (makeStaticSlice)
					ReadCommand.LegacyReadCommandSerializer.serializeStaticSlice(out, true, metadata);

			}else {
				if (makeStaticSlice)
					ReadCommand.LegacyReadCommandSerializer.serializeStaticSlice(out, false, metadata);

				for (Slice slice : slices)
					ReadCommand.LegacyReadCommandSerializer.serializeSlice(out, slice, false, metadata);

			}
		}

		static long serializedSlicesSize(Slices slices, boolean makeStaticSlice, CFMetaData metadata) {
			long size = TypeSizes.sizeof(slices.size());
			for (Slice slice : slices) {
				ByteBuffer sliceStart = LegacyLayout.encodeBound(metadata, slice.start(), true);
				size += ByteBufferUtil.serializedSizeWithShortLength(sliceStart);
				ByteBuffer sliceEnd = LegacyLayout.encodeBound(metadata, slice.end(), false);
				size += ByteBufferUtil.serializedSizeWithShortLength(sliceEnd);
			}
			if (makeStaticSlice)
				size += ReadCommand.LegacyReadCommandSerializer.serializedStaticSliceSize(metadata);

			return size;
		}

		static long serializedStaticSliceSize(CFMetaData metadata) {
			ByteBuffer sliceStart = LegacyLayout.encodeBound(metadata, ClusteringBound.BOTTOM, false);
			long size = ByteBufferUtil.serializedSizeWithShortLength(sliceStart);
			size += TypeSizes.sizeof(((short) (((metadata.comparator.size()) * 3) + 2)));
			size += TypeSizes.sizeof(((short) (LegacyLayout.STATIC_PREFIX)));
			for (int i = 0; i < (metadata.comparator.size()); i++) {
				size += ByteBufferUtil.serializedSizeWithShortLength(ByteBufferUtil.EMPTY_BYTE_BUFFER);
				size += 1;
			}
			return size;
		}

		private static void serializeSlice(DataOutputPlus out, Slice slice, boolean isReversed, CFMetaData metadata) throws IOException {
			ByteBuffer sliceStart = LegacyLayout.encodeBound(metadata, (isReversed ? slice.end() : slice.start()), (!isReversed));
			ByteBufferUtil.writeWithShortLength(sliceStart, out);
			ByteBuffer sliceEnd = LegacyLayout.encodeBound(metadata, (isReversed ? slice.start() : slice.end()), isReversed);
			ByteBufferUtil.writeWithShortLength(sliceEnd, out);
		}

		private static void serializeStaticSlice(DataOutputPlus out, boolean isReversed, CFMetaData metadata) throws IOException {
			if (!isReversed) {
				ByteBuffer sliceStart = LegacyLayout.encodeBound(metadata, ClusteringBound.BOTTOM, false);
				ByteBufferUtil.writeWithShortLength(sliceStart, out);
			}
			out.writeShort((2 + ((metadata.comparator.size()) * 3)));
			out.writeShort(LegacyLayout.STATIC_PREFIX);
			for (int i = 0; i < (metadata.comparator.size()); i++) {
				ByteBufferUtil.writeWithShortLength(ByteBufferUtil.EMPTY_BYTE_BUFFER, out);
				out.writeByte((i == ((metadata.comparator.size()) - 1) ? 1 : 0));
			}
			if (isReversed) {
				ByteBuffer sliceStart = LegacyLayout.encodeBound(metadata, ClusteringBound.BOTTOM, false);
				ByteBufferUtil.writeWithShortLength(sliceStart, out);
			}
		}

		static Pair<ClusteringIndexSliceFilter, Boolean> deserializeSlicePartitionFilter(DataInputPlus in, CFMetaData metadata) throws IOException {
			int numSlices = in.readInt();
			ByteBuffer[] startBuffers = new ByteBuffer[numSlices];
			ByteBuffer[] finishBuffers = new ByteBuffer[numSlices];
			for (int i = 0; i < numSlices; i++) {
				startBuffers[i] = ByteBufferUtil.readWithShortLength(in);
				finishBuffers[i] = ByteBufferUtil.readWithShortLength(in);
			}
			boolean reversed = in.readBoolean();
			if (reversed) {
				ByteBuffer[] tmp = finishBuffers;
				finishBuffers = startBuffers;
				startBuffers = tmp;
			}
			boolean selectsStatics = false;
			Slices.Builder slicesBuilder = new Slices.Builder(metadata.comparator);
			for (int i = 0; i < numSlices; i++) {
				LegacyLayout.LegacyBound start = LegacyLayout.decodeBound(metadata, startBuffers[i], true);
				LegacyLayout.LegacyBound finish = LegacyLayout.decodeBound(metadata, finishBuffers[i], false);
				if (start.isStatic) {
					start = BOTTOM;
					if (start.bound.isInclusive())
						selectsStatics = true;

				}else
					if (start == (BOTTOM)) {
						selectsStatics = true;
					}

				if (finish.isStatic) {
					assert finish.bound.isInclusive();
					continue;
				}
				slicesBuilder.add(Slice.make(start.bound, finish.bound));
			}
			return Pair.create(new ClusteringIndexSliceFilter(slicesBuilder.build(), reversed), selectsStatics);
		}

		private static SinglePartitionReadCommand maybeConvertNamesToSlice(SinglePartitionReadCommand command) {
			if ((command.clusteringIndexFilter().kind()) != (NAMES))
				return command;

			CFMetaData metadata = command.metadata();
			if (!(ReadCommand.LegacyReadCommandSerializer.shouldConvertNamesToSlice(metadata, command.columnFilter().fetchedColumns())))
				return command;

			ClusteringIndexNamesFilter filter = ((ClusteringIndexNamesFilter) (command.clusteringIndexFilter()));
			ClusteringIndexSliceFilter sliceFilter = ReadCommand.LegacyReadCommandSerializer.convertNamesFilterToSliceFilter(filter, metadata);
			return command.withUpdatedClusteringIndexFilter(sliceFilter);
		}

		static boolean shouldConvertNamesToSlice(CFMetaData metadata, PartitionColumns columns) {
			if ((!(metadata.isDense())) && (metadata.isCompound()))
				return true;

			for (ColumnDefinition column : columns) {
				if (column.type.isMultiCell())
					return true;

			}
			return false;
		}

		private static ClusteringIndexSliceFilter convertNamesFilterToSliceFilter(ClusteringIndexNamesFilter filter, CFMetaData metadata) {
			SortedSet<Clustering> requestedRows = filter.requestedRows();
			Slices slices;
			if (requestedRows.isEmpty()) {
				slices = Slices.NONE;
			}else
				if (((requestedRows.size()) == 1) && ((requestedRows.first().size()) == 0)) {
					slices = Slices.ALL;
				}else {
					Slices.Builder slicesBuilder = new Slices.Builder(metadata.comparator);
					for (Clustering clustering : requestedRows)
						slicesBuilder.add(ClusteringBound.inclusiveStartOf(clustering), ClusteringBound.inclusiveEndOf(clustering));

					slices = slicesBuilder.build();
				}

			return new ClusteringIndexSliceFilter(slices, filter.isReversed());
		}

		static int updateLimitForQuery(int limit, Slices slices) {
			if ((!(slices.hasLowerBound())) && (!(slices.hasUpperBound())))
				return limit;

			for (Slice slice : slices) {
				if (limit == (Integer.MAX_VALUE))
					return limit;

				if (!(slice.start().isInclusive()))
					limit++;

				if (!(slice.end().isInclusive()))
					limit++;

			}
			return limit;
		}
	}
}

