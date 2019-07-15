

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringComparator;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.Columns;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionInfo;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LegacyLayout;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.MutableDeletionInfo;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.RangeTombstone;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.BaseRowIterator;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.ColumnData;
import org.apache.cassandra.db.rows.ComplexColumnData;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.rows.RangeTombstoneMarker;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.rows.RowIterators;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.rows.SerializationHelper;
import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.UnfilteredRowIteratorSerializer;
import org.apache.cassandra.db.rows.UnfilteredRowIterators;
import org.apache.cassandra.io.util.DataInputBuffer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.btree.BTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.db.rows.SerializationHelper.Flag.LOCAL;
import static org.apache.cassandra.db.rows.Unfiltered.Kind.ROW;


public class PartitionUpdate extends AbstractBTreePartition {
	protected static final Logger logger = LoggerFactory.getLogger(PartitionUpdate.class);

	public static final PartitionUpdate.PartitionUpdateSerializer serializer = new PartitionUpdate.PartitionUpdateSerializer();

	private final int createdAtInSec = FBUtilities.nowInSeconds();

	private volatile boolean isBuilt;

	private boolean canReOpen = true;

	private AbstractBTreePartition.Holder holder;

	private BTree.Builder<Row> rowBuilder;

	private MutableDeletionInfo deletionInfo;

	private final boolean canHaveShadowedData;

	private PartitionUpdate(CFMetaData metadata, DecoratedKey key, PartitionColumns columns, MutableDeletionInfo deletionInfo, int initialRowCapacity, boolean canHaveShadowedData) {
		super(metadata, key);
		this.deletionInfo = deletionInfo;
		this.canHaveShadowedData = canHaveShadowedData;
		rowBuilder = builder(initialRowCapacity);
	}

	private PartitionUpdate(CFMetaData metadata, DecoratedKey key, AbstractBTreePartition.Holder holder, MutableDeletionInfo deletionInfo, boolean canHaveShadowedData) {
		super(metadata, key);
		this.holder = holder;
		this.deletionInfo = deletionInfo;
		this.isBuilt = true;
		this.canHaveShadowedData = canHaveShadowedData;
	}

	public PartitionUpdate(CFMetaData metadata, DecoratedKey key, PartitionColumns columns, int initialRowCapacity) {
		this(metadata, key, columns, MutableDeletionInfo.live(), initialRowCapacity, true);
	}

	public PartitionUpdate(CFMetaData metadata, ByteBuffer key, PartitionColumns columns, int initialRowCapacity) {
		this(metadata, metadata.decorateKey(key), columns, initialRowCapacity);
	}

	public static PartitionUpdate emptyUpdate(CFMetaData metadata, DecoratedKey key) {
		MutableDeletionInfo deletionInfo = MutableDeletionInfo.live();
		return null;
	}

	public static PartitionUpdate fullPartitionDelete(CFMetaData metadata, DecoratedKey key, long timestamp, int nowInSec) {
		MutableDeletionInfo deletionInfo = new MutableDeletionInfo(timestamp, nowInSec);
		return null;
	}

	public static PartitionUpdate singleRowUpdate(CFMetaData metadata, DecoratedKey key, Row row) {
		MutableDeletionInfo deletionInfo = MutableDeletionInfo.live();
		if (row.isStatic()) {
		}else {
		}
		return null;
	}

	public static PartitionUpdate singleRowUpdate(CFMetaData metadata, ByteBuffer key, Row row) {
		return PartitionUpdate.singleRowUpdate(metadata, metadata.decorateKey(key), row);
	}

	public static PartitionUpdate fromIterator(UnfilteredRowIterator iterator, ColumnFilter filter) {
		iterator = UnfilteredRowIterators.withOnlyQueriedData(iterator, filter);
		AbstractBTreePartition.Holder holder = AbstractBTreePartition.build(iterator, 16);
		return null;
	}

	public static PartitionUpdate fromIterator(RowIterator iterator, ColumnFilter filter) {
		iterator = RowIterators.withOnlyQueriedData(iterator, filter);
		MutableDeletionInfo deletionInfo = MutableDeletionInfo.live();
		AbstractBTreePartition.Holder holder = AbstractBTreePartition.build(iterator, deletionInfo, true, 16);
		return new PartitionUpdate(iterator.metadata(), iterator.partitionKey(), holder, deletionInfo, false);
	}

	protected boolean canHaveShadowedData() {
		return canHaveShadowedData;
	}

	public static PartitionUpdate fromBytes(ByteBuffer bytes, int version, DecoratedKey key) {
		if (bytes == null)
			return null;

		try {
			return PartitionUpdate.serializer.deserialize(new DataInputBuffer(bytes, true), version, LOCAL, (version < (MessagingService.VERSION_30) ? key : null));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static ByteBuffer toBytes(PartitionUpdate update, int version) {
		try (DataOutputBuffer out = new DataOutputBuffer()) {
			PartitionUpdate.serializer.serialize(update, out, version);
			return out.buffer();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static PartitionUpdate fullPartitionDelete(CFMetaData metadata, ByteBuffer key, long timestamp, int nowInSec) {
		return PartitionUpdate.fullPartitionDelete(metadata, metadata.decorateKey(key), timestamp, nowInSec);
	}

	public static PartitionUpdate merge(List<PartitionUpdate> updates) {
		assert !(updates.isEmpty());
		final int size = updates.size();
		if (size == 1)
			return Iterables.getOnlyElement(updates);

		int nowInSecs = FBUtilities.nowInSeconds();
		List<UnfilteredRowIterator> asIterators = Lists.transform(updates, AbstractBTreePartition::unfilteredIterator);
		return PartitionUpdate.fromIterator(UnfilteredRowIterators.merge(asIterators, nowInSecs), ColumnFilter.all(updates.get(0).metadata()));
	}

	@Override
	public DeletionInfo deletionInfo() {
		return deletionInfo;
	}

	public void updateAllTimestamp(long newTimestamp) {
		AbstractBTreePartition.Holder holder = holder();
		deletionInfo.updateAllTimestamp((newTimestamp - 1));
	}

	public int operationCount() {
		return (((rowCount()) + (staticRow().isEmpty() ? 0 : 1)) + (deletionInfo.rangeCount())) + (deletionInfo.getPartitionDeletion().isLive() ? 0 : 1);
	}

	public int dataSize() {
		int size = 0;
		for (Row row : this) {
			size += row.clustering().dataSize();
			for (ColumnData cd : row)
				size += cd.dataSize();

		}
		return size;
	}

	@Override
	public PartitionColumns columns() {
		return null;
	}

	protected AbstractBTreePartition.Holder holder() {
		maybeBuild();
		return holder;
	}

	public EncodingStats stats() {
		return null;
	}

	public synchronized void allowNewUpdates() {
		if (!(canReOpen))
			throw new IllegalStateException("You cannot do more updates on collectCounterMarks has been called");

		isBuilt = false;
		if ((rowBuilder) == null)
			rowBuilder = builder(16);

	}

	private BTree.Builder<Row> builder(int initialCapacity) {
		return BTree.<Row>builder(metadata.comparator, initialCapacity).setQuickResolver(( a, b) -> Rows.merge(a, b, createdAtInSec));
	}

	@Override
	public Iterator<Row> iterator() {
		maybeBuild();
		return super.iterator();
	}

	public void validate() {
		for (Row row : this) {
			metadata().comparator.validate(row.clustering());
			for (ColumnData cd : row)
				cd.validate();

		}
	}

	public long maxTimestamp() {
		maybeBuild();
		long maxTimestamp = deletionInfo.maxTimestamp();
		for (Row row : this) {
			maxTimestamp = Math.max(maxTimestamp, row.primaryKeyLivenessInfo().timestamp());
			for (ColumnData cd : row) {
				if (cd.column().isSimple()) {
					maxTimestamp = Math.max(maxTimestamp, ((Cell) (cd)).timestamp());
				}else {
					ComplexColumnData complexData = ((ComplexColumnData) (cd));
					maxTimestamp = Math.max(maxTimestamp, complexData.complexDeletion().markedForDeleteAt());
					for (Cell cell : complexData)
						maxTimestamp = Math.max(maxTimestamp, cell.timestamp());

				}
			}
		}
		return maxTimestamp;
	}

	public List<PartitionUpdate.CounterMark> collectCounterMarks() {
		assert metadata().isCounter();
		maybeBuild();
		canReOpen = false;
		List<PartitionUpdate.CounterMark> marks = new ArrayList<>();
		addMarksForRow(staticRow(), marks);
		for (Row row : this)
			addMarksForRow(row, marks);

		return marks;
	}

	private void addMarksForRow(Row row, List<PartitionUpdate.CounterMark> marks) {
		for (Cell cell : row.cells()) {
			if (cell.isCounterCell())
				marks.add(new PartitionUpdate.CounterMark(row, cell.column(), cell.path()));

		}
	}

	private void assertNotBuilt() {
		if (isBuilt)
			throw new IllegalStateException("An update should not be written again once it has been read");

	}

	public void addPartitionDeletion(DeletionTime deletionTime) {
		assertNotBuilt();
		deletionInfo.add(deletionTime);
	}

	public void add(RangeTombstone range) {
		assertNotBuilt();
		deletionInfo.add(range, metadata.comparator);
	}

	public void add(Row row) {
		if (row.isEmpty())
			return;

		assertNotBuilt();
		if (row.isStatic()) {
			assert columns().statics.containsAll(row.columns()) : ((columns().statics) + " is not superset of ") + (row.columns());
		}else {
			assert columns().regulars.containsAll(row.columns()) : ((columns().regulars) + " is not superset of ") + (row.columns());
			rowBuilder.add(row);
		}
	}

	private void maybeBuild() {
		if (isBuilt)
			return;

		build();
	}

	private synchronized void build() {
		if (isBuilt)
			return;

		AbstractBTreePartition.Holder holder = this.holder;
		Object[] add = rowBuilder.build();
		rowBuilder = null;
		isBuilt = true;
	}

	@Override
	public String toString() {
		if (isBuilt)
			return super.toString();

		StringBuilder sb = new StringBuilder();
		sb.append(String.format("[%s.%s] key=%s columns=%s", metadata.ksName, metadata.cfName, metadata.getKeyValidator().getString(partitionKey().getKey()), columns()));
		sb.append("\n    deletionInfo=").append(deletionInfo);
		sb.append(" (not built)");
		return sb.toString();
	}

	public static PartitionUpdate.SimpleBuilder simpleBuilder(CFMetaData metadata, Object... partitionKeyValues) {
		return null;
	}

	public interface SimpleBuilder {
		public CFMetaData metadata();

		public PartitionUpdate.SimpleBuilder timestamp(long timestamp);

		public PartitionUpdate.SimpleBuilder ttl(int ttl);

		public PartitionUpdate.SimpleBuilder nowInSec(int nowInSec);

		public Row.SimpleBuilder row(Object... clusteringValues);

		public PartitionUpdate.SimpleBuilder delete();

		public PartitionUpdate.SimpleBuilder.RangeTombstoneBuilder addRangeTombstone();

		public PartitionUpdate build();

		public Mutation buildAsMutation();

		public interface RangeTombstoneBuilder {
			public PartitionUpdate.SimpleBuilder.RangeTombstoneBuilder start(Object... values);

			public PartitionUpdate.SimpleBuilder.RangeTombstoneBuilder end(Object... values);

			public PartitionUpdate.SimpleBuilder.RangeTombstoneBuilder inclStart();

			public PartitionUpdate.SimpleBuilder.RangeTombstoneBuilder exclStart();

			public PartitionUpdate.SimpleBuilder.RangeTombstoneBuilder inclEnd();

			public PartitionUpdate.SimpleBuilder.RangeTombstoneBuilder exclEnd();
		}
	}

	public static class PartitionUpdateSerializer {
		public void serialize(PartitionUpdate update, DataOutputPlus out, int version) throws IOException {
			try (UnfilteredRowIterator iter = update.unfilteredIterator()) {
				assert !(iter.isReverseOrder());
				if (version < (MessagingService.VERSION_30)) {
					LegacyLayout.serializeAsLegacyPartition(null, iter, out, version);
				}else {
					CFMetaData.serializer.serialize(update.metadata(), out, version);
					UnfilteredRowIteratorSerializer.serializer.serialize(iter, null, out, version, update.rowCount());
				}
			}
		}

		public PartitionUpdate deserialize(DataInputPlus in, int version, SerializationHelper.Flag flag, ByteBuffer key) throws IOException {
			if (version >= (MessagingService.VERSION_30)) {
				assert key == null;
				return PartitionUpdate.PartitionUpdateSerializer.deserialize30(in, version, flag);
			}else {
				assert key != null;
				return PartitionUpdate.PartitionUpdateSerializer.deserializePre30(in, version, flag, key);
			}
		}

		public PartitionUpdate deserialize(DataInputPlus in, int version, SerializationHelper.Flag flag, DecoratedKey key) throws IOException {
			if (version >= (MessagingService.VERSION_30)) {
				return PartitionUpdate.PartitionUpdateSerializer.deserialize30(in, version, flag);
			}else {
				assert key != null;
				return PartitionUpdate.PartitionUpdateSerializer.deserializePre30(in, version, flag, key.getKey());
			}
		}

		private static PartitionUpdate deserialize30(DataInputPlus in, int version, SerializationHelper.Flag flag) throws IOException {
			CFMetaData metadata = CFMetaData.serializer.deserialize(in, version);
			UnfilteredRowIteratorSerializer.Header header = UnfilteredRowIteratorSerializer.serializer.deserializeHeader(metadata, null, in, version, flag);
			if (header.isEmpty)
				return PartitionUpdate.emptyUpdate(metadata, header.key);

			assert !(header.isReversed);
			assert (header.rowEstimate) >= 0;
			MutableDeletionInfo.Builder deletionBuilder = MutableDeletionInfo.builder(header.partitionDeletion, metadata.comparator, false);
			BTree.Builder<Row> rows = BTree.builder(metadata.comparator, header.rowEstimate);
			rows.auto(false);
			try (final UnfilteredRowIterator partition = UnfilteredRowIteratorSerializer.serializer.deserialize(in, version, metadata, flag, header)) {
				while (partition.hasNext()) {
					Unfiltered unfiltered = partition.next();
					if ((unfiltered.kind()) == (ROW))
						rows.add(((Row) (unfiltered)));
					else
						deletionBuilder.add(((RangeTombstoneMarker) (unfiltered)));

				} 
			}
			MutableDeletionInfo deletionInfo = deletionBuilder.build();
			return null;
		}

		private static PartitionUpdate deserializePre30(DataInputPlus in, int version, SerializationHelper.Flag flag, ByteBuffer key) throws IOException {
			try (UnfilteredRowIterator iterator = LegacyLayout.deserializeLegacyPartition(in, version, flag, key)) {
				assert iterator != null;
				return PartitionUpdate.fromIterator(iterator, ColumnFilter.all(iterator.metadata()));
			}
		}

		public long serializedSize(PartitionUpdate update, int version) {
			try (UnfilteredRowIterator iter = update.unfilteredIterator()) {
				if (version < (MessagingService.VERSION_30))
					return LegacyLayout.serializedSizeAsLegacyPartition(null, iter, version);

				return (CFMetaData.serializer.serializedSize(update.metadata(), version)) + (UnfilteredRowIteratorSerializer.serializer.serializedSize(iter, null, version, update.rowCount()));
			}
		}
	}

	public static class CounterMark {
		private final Row row;

		private final ColumnDefinition column;

		private final CellPath path;

		private CounterMark(Row row, ColumnDefinition column, CellPath path) {
			this.row = row;
			this.column = column;
			this.path = path;
		}

		public Clustering clustering() {
			return row.clustering();
		}

		public ColumnDefinition column() {
			return column;
		}

		public CellPath path() {
			return path;
		}

		public ByteBuffer value() {
			return (path) == null ? row.getCell(column).value() : row.getCell(column, path).value();
		}

		public void setValue(ByteBuffer value) {
			assert (row) instanceof BTreeRow;
			((BTreeRow) (row)).setValue(column, path, value);
		}
	}
}

