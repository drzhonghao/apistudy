

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.CFMetaData.DroppedColumn;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.Columns;
import org.apache.cassandra.db.DeletionPurger;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.rows.AbstractRow;
import org.apache.cassandra.db.rows.BufferCell;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.ColumnData;
import org.apache.cassandra.db.rows.ComplexColumnData;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.utils.AbstractIterator;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.ObjectSizes;
import org.apache.cassandra.utils.WrappedBoolean;
import org.apache.cassandra.utils.WrappedInt;
import org.apache.cassandra.utils.btree.BTree;
import org.apache.cassandra.utils.btree.BTreeSearchIterator;
import org.apache.cassandra.utils.btree.UpdateFunction;

import static org.apache.cassandra.config.ColumnDefinition.Kind.REGULAR;
import static org.apache.cassandra.config.ColumnDefinition.Kind.STATIC;
import static org.apache.cassandra.db.rows.Row.Deletion.LIVE;
import static org.apache.cassandra.db.rows.Row.Deletion.regular;
import static org.apache.cassandra.utils.btree.BTree.Dir.ASC;
import static org.apache.cassandra.utils.btree.BTree.Dir.DESC;


public class BTreeRow extends AbstractRow {
	private static final long EMPTY_SIZE = ObjectSizes.measure(BTreeRow.emptyRow(Clustering.EMPTY));

	private final Clustering clustering;

	private final LivenessInfo primaryKeyLivenessInfo;

	private final Row.Deletion deletion;

	private final Object[] btree;

	private final int minLocalDeletionTime;

	private BTreeRow(Clustering clustering, LivenessInfo primaryKeyLivenessInfo, Row.Deletion deletion, Object[] btree, int minLocalDeletionTime) {
		assert !(deletion.isShadowedBy(primaryKeyLivenessInfo));
		this.clustering = clustering;
		this.primaryKeyLivenessInfo = primaryKeyLivenessInfo;
		this.deletion = deletion;
		this.btree = btree;
		this.minLocalDeletionTime = minLocalDeletionTime;
	}

	private BTreeRow(Clustering clustering, Object[] btree, int minLocalDeletionTime) {
		this(clustering, LivenessInfo.EMPTY, LIVE, btree, minLocalDeletionTime);
	}

	public static BTreeRow create(Clustering clustering, LivenessInfo primaryKeyLivenessInfo, Row.Deletion deletion, Object[] btree) {
		int minDeletionTime = Math.min(BTreeRow.minDeletionTime(primaryKeyLivenessInfo), BTreeRow.minDeletionTime(deletion.time()));
		if (minDeletionTime != (Integer.MIN_VALUE)) {
			for (ColumnData cd : BTree.<ColumnData>iterable(btree))
				minDeletionTime = Math.min(minDeletionTime, BTreeRow.minDeletionTime(cd));

		}
		return BTreeRow.create(clustering, primaryKeyLivenessInfo, deletion, btree, minDeletionTime);
	}

	public static BTreeRow create(Clustering clustering, LivenessInfo primaryKeyLivenessInfo, Row.Deletion deletion, Object[] btree, int minDeletionTime) {
		return new BTreeRow(clustering, primaryKeyLivenessInfo, deletion, btree, minDeletionTime);
	}

	public static BTreeRow emptyRow(Clustering clustering) {
		return new BTreeRow(clustering, BTree.empty(), Integer.MAX_VALUE);
	}

	public static BTreeRow singleCellRow(Clustering clustering, Cell cell) {
		if (cell.column().isSimple())
			return new BTreeRow(clustering, BTree.singleton(cell), BTreeRow.minDeletionTime(cell));

		return null;
	}

	public static BTreeRow emptyDeletedRow(Clustering clustering, Row.Deletion deletion) {
		assert !(deletion.isLive());
		return new BTreeRow(clustering, LivenessInfo.EMPTY, deletion, BTree.empty(), Integer.MIN_VALUE);
	}

	public static BTreeRow noCellLiveRow(Clustering clustering, LivenessInfo primaryKeyLivenessInfo) {
		assert !(primaryKeyLivenessInfo.isEmpty());
		return new BTreeRow(clustering, primaryKeyLivenessInfo, LIVE, BTree.empty(), BTreeRow.minDeletionTime(primaryKeyLivenessInfo));
	}

	private static int minDeletionTime(Cell cell) {
		return cell.isTombstone() ? Integer.MIN_VALUE : cell.localDeletionTime();
	}

	private static int minDeletionTime(LivenessInfo info) {
		return info.isExpiring() ? info.localExpirationTime() : Integer.MAX_VALUE;
	}

	private static int minDeletionTime(DeletionTime dt) {
		return dt.isLive() ? Integer.MAX_VALUE : Integer.MIN_VALUE;
	}

	private static int minDeletionTime(ComplexColumnData cd) {
		int min = BTreeRow.minDeletionTime(cd.complexDeletion());
		for (Cell cell : cd) {
			min = Math.min(min, BTreeRow.minDeletionTime(cell));
			if (min == (Integer.MIN_VALUE))
				break;

		}
		return min;
	}

	private static int minDeletionTime(ColumnData cd) {
		return cd.column().isSimple() ? BTreeRow.minDeletionTime(((Cell) (cd))) : BTreeRow.minDeletionTime(((ComplexColumnData) (cd)));
	}

	public void apply(Consumer<ColumnData> function, boolean reversed) {
		BTree.apply(btree, function, reversed);
	}

	public void apply(Consumer<ColumnData> funtion, Predicate<ColumnData> stopCondition, boolean reversed) {
		BTree.apply(btree, funtion, stopCondition, reversed);
	}

	private static int minDeletionTime(Object[] btree, LivenessInfo info, DeletionTime rowDeletion) {
		final WrappedInt min = new WrappedInt(Math.min(BTreeRow.minDeletionTime(info), BTreeRow.minDeletionTime(rowDeletion)));
		BTree.<ColumnData>apply(btree, ( cd) -> min.set(Math.min(min.get(), BTreeRow.minDeletionTime(cd))), ( cd) -> (min.get()) == (Integer.MIN_VALUE), false);
		return min.get();
	}

	public Clustering clustering() {
		return clustering;
	}

	public Collection<ColumnDefinition> columns() {
		return Collections2.transform(this, ColumnData::column);
	}

	public LivenessInfo primaryKeyLivenessInfo() {
		return primaryKeyLivenessInfo;
	}

	public boolean isEmpty() {
		return ((primaryKeyLivenessInfo().isEmpty()) && (deletion().isLive())) && (BTree.isEmpty(btree));
	}

	public Row.Deletion deletion() {
		return deletion;
	}

	public Cell getCell(ColumnDefinition c) {
		assert !(c.isComplex());
		return ((Cell) (BTree.<Object>find(btree, ColumnDefinition.asymmetricColumnDataComparator, c)));
	}

	public Cell getCell(ColumnDefinition c, CellPath path) {
		assert c.isComplex();
		ComplexColumnData cd = getComplexColumnData(c);
		if (cd == null)
			return null;

		return cd.getCell(path);
	}

	public ComplexColumnData getComplexColumnData(ColumnDefinition c) {
		assert c.isComplex();
		return ((ComplexColumnData) (BTree.<Object>find(btree, ColumnDefinition.asymmetricColumnDataComparator, c)));
	}

	public int size() {
		return BTree.size(btree);
	}

	public Iterator<ColumnData> iterator() {
		return searchIterator();
	}

	public Iterable<Cell> cells() {
		return BTreeRow.CellIterator::new;
	}

	public BTreeSearchIterator<ColumnDefinition, ColumnData> searchIterator() {
		return BTree.slice(btree, ColumnDefinition.asymmetricColumnDataComparator, ASC);
	}

	public Row filter(ColumnFilter filter, CFMetaData metadata) {
		return filter(filter, DeletionTime.LIVE, false, metadata);
	}

	public Row filter(ColumnFilter filter, DeletionTime activeDeletion, boolean setActiveDeletionToRow, CFMetaData metadata) {
		Map<ByteBuffer, CFMetaData.DroppedColumn> droppedColumns = metadata.getDroppedColumns();
		boolean mayFilterColumns = (!(filter.fetchesAllColumns())) || (!(filter.allFetchedColumnsAreQueried()));
		boolean mayHaveShadowed = activeDeletion.supersedes(deletion.time());
		if (((!mayFilterColumns) && (!mayHaveShadowed)) && (droppedColumns.isEmpty()))
			return this;

		LivenessInfo newInfo = primaryKeyLivenessInfo;
		Row.Deletion newDeletion = deletion;
		if (mayHaveShadowed) {
			if (activeDeletion.deletes(newInfo.timestamp()))
				newInfo = LivenessInfo.EMPTY;

			newDeletion = (setActiveDeletionToRow) ? regular(activeDeletion) : LIVE;
		}
		Columns columns = filter.fetchedColumns().columns(isStatic());
		java.util.function.Predicate<ColumnDefinition> inclusionTester = columns.inOrderInclusionTester();
		java.util.function.Predicate<ColumnDefinition> queriedByUserTester = filter.queriedColumns().columns(isStatic()).inOrderInclusionTester();
		final LivenessInfo rowLiveness = newInfo;
		return transformAndFilter(newInfo, newDeletion, ( cd) -> {
			ColumnDefinition column = cd.column();
			if (!(inclusionTester.test(column)))
				return null;

			CFMetaData.DroppedColumn dropped = droppedColumns.get(column.name.bytes);
			if (column.isComplex())
				return ((ComplexColumnData) (cd)).filter(filter, (mayHaveShadowed ? activeDeletion : DeletionTime.LIVE), dropped, rowLiveness);

			Cell cell = ((Cell) (cd));
			boolean isForDropped = (dropped != null) && ((cell.timestamp()) <= (dropped.droppedTime));
			boolean isShadowed = mayHaveShadowed && (activeDeletion.deletes(cell));
			boolean isSkippable = (!(queriedByUserTester.test(column))) && ((cell.timestamp()) < (rowLiveness.timestamp()));
			return (isForDropped || isShadowed) || isSkippable ? null : cell;
		});
	}

	public Row withOnlyQueriedData(ColumnFilter filter) {
		if (filter.allFetchedColumnsAreQueried())
			return this;

		return transformAndFilter(primaryKeyLivenessInfo, deletion, ( cd) -> {
			ColumnDefinition column = cd.column();
			if (column.isComplex())
				return ((ComplexColumnData) (cd)).withOnlyQueriedData(filter);

			return filter.fetchedColumnIsQueried(column) ? cd : null;
		});
	}

	public boolean hasComplex() {
		ColumnData cd = Iterables.getFirst(BTree.<ColumnData>iterable(btree, DESC), null);
		return false;
	}

	public boolean hasComplexDeletion() {
		final WrappedBoolean result = new WrappedBoolean(false);
		apply(( c) -> {
		}, ( cd) -> {
			if (!(((ComplexColumnData) (cd)).complexDeletion().isLive())) {
				result.set(true);
				return true;
			}
			return false;
		}, true);
		return result.get();
	}

	public Row markCounterLocalToBeCleared() {
		return transformAndFilter(primaryKeyLivenessInfo, deletion, ( cd) -> cd.column().isCounterColumn() ? cd.markCounterLocalToBeCleared() : cd);
	}

	public boolean hasDeletion(int nowInSec) {
		return nowInSec >= (minLocalDeletionTime);
	}

	public Row updateAllTimestamp(long newTimestamp) {
		LivenessInfo newInfo = (primaryKeyLivenessInfo.isEmpty()) ? primaryKeyLivenessInfo : primaryKeyLivenessInfo.withUpdatedTimestamp(newTimestamp);
		Row.Deletion newDeletion = ((deletion.isLive()) || ((deletion.isShadowable()) && (!(primaryKeyLivenessInfo.isEmpty())))) ? LIVE : new Row.Deletion(new DeletionTime((newTimestamp - 1), deletion.time().localDeletionTime()), deletion.isShadowable());
		return transformAndFilter(newInfo, newDeletion, ( cd) -> cd.updateAllTimestamp(newTimestamp));
	}

	public Row withRowDeletion(DeletionTime newDeletion) {
		return (newDeletion.isLive()) || (!(deletion.isLive())) ? this : new BTreeRow(clustering, primaryKeyLivenessInfo, regular(newDeletion), btree, Integer.MIN_VALUE);
	}

	public Row purge(DeletionPurger purger, int nowInSec, boolean enforceStrictLiveness) {
		if (!(hasDeletion(nowInSec)))
			return this;

		LivenessInfo newInfo = (purger.shouldPurge(primaryKeyLivenessInfo, nowInSec)) ? LivenessInfo.EMPTY : primaryKeyLivenessInfo;
		Row.Deletion newDeletion = (purger.shouldPurge(deletion.time())) ? LIVE : deletion;
		if ((enforceStrictLiveness && (newDeletion.isLive())) && (newInfo.isEmpty()))
			return null;

		return transformAndFilter(newInfo, newDeletion, ( cd) -> cd.purge(purger, nowInSec));
	}

	private Row transformAndFilter(LivenessInfo info, Row.Deletion deletion, Function<ColumnData, ColumnData> function) {
		Object[] transformed = BTree.transformAndFilter(btree, function);
		if ((((btree) == transformed) && (info == (this.primaryKeyLivenessInfo))) && (deletion == (this.deletion)))
			return this;

		if (((info.isEmpty()) && (deletion.isLive())) && (BTree.isEmpty(transformed)))
			return null;

		int minDeletionTime = BTreeRow.minDeletionTime(transformed, info, deletion.time());
		return BTreeRow.create(clustering, info, deletion, transformed, minDeletionTime);
	}

	public int dataSize() {
		int dataSize = ((clustering.dataSize()) + (primaryKeyLivenessInfo.dataSize())) + (deletion.dataSize());
		for (ColumnData cd : this)
			dataSize += cd.dataSize();

		return dataSize;
	}

	public long unsharedHeapSizeExcludingData() {
		long heapSize = ((BTreeRow.EMPTY_SIZE) + (clustering.unsharedHeapSizeExcludingData())) + (BTree.sizeOfStructureOnHeap(btree));
		for (ColumnData cd : this)
			heapSize += cd.unsharedHeapSizeExcludingData();

		return heapSize;
	}

	public static Row.Builder sortedBuilder() {
		return new BTreeRow.Builder(true);
	}

	public static Row.Builder unsortedBuilder(int nowInSec) {
		return new BTreeRow.Builder(false, nowInSec);
	}

	public void setValue(ColumnDefinition column, CellPath path, ByteBuffer value) {
		ColumnData current = ((ColumnData) (BTree.<Object>find(btree, ColumnDefinition.asymmetricColumnDataComparator, column)));
	}

	public Iterable<Cell> cellsInLegacyOrder(CFMetaData metadata, boolean reversed) {
		return () -> new BTreeRow.CellInLegacyOrderIterator(metadata, reversed);
	}

	private class CellIterator extends AbstractIterator<Cell> {
		private Iterator<ColumnData> columnData = iterator();

		private Iterator<Cell> complexCells;

		protected Cell computeNext() {
			while (true) {
				if ((complexCells) != null) {
					if (complexCells.hasNext())
						return complexCells.next();

					complexCells = null;
				}
				if (!(columnData.hasNext()))
					return endOfData();

				ColumnData cd = columnData.next();
				if (cd.column().isComplex())
					complexCells = ((ComplexColumnData) (cd)).iterator();
				else
					return ((Cell) (cd));

			} 
		}
	}

	private class CellInLegacyOrderIterator extends AbstractIterator<Cell> {
		private final Comparator<ByteBuffer> comparator;

		private final boolean reversed;

		private final int firstComplexIdx;

		private int simpleIdx;

		private int complexIdx;

		private Iterator<Cell> complexCells;

		private final Object[] data;

		private CellInLegacyOrderIterator(CFMetaData metadata, boolean reversed) {
			AbstractType<?> nameComparator = metadata.getColumnDefinitionNameComparator((isStatic() ? STATIC : REGULAR));
			this.comparator = (reversed) ? Collections.reverseOrder(nameComparator) : nameComparator;
			this.reversed = reversed;
			this.data = new Object[BTree.size(btree)];
			BTree.toArray(btree, data, 0);
			int idx = Iterators.indexOf(Iterators.forArray(data), ( cd) -> cd instanceof ComplexColumnData);
			this.firstComplexIdx = (idx < 0) ? data.length : idx;
			this.complexIdx = firstComplexIdx;
		}

		private int getSimpleIdx() {
			return reversed ? ((firstComplexIdx) - (simpleIdx)) - 1 : simpleIdx;
		}

		private int getSimpleIdxAndIncrement() {
			int idx = getSimpleIdx();
			++(simpleIdx);
			return idx;
		}

		private int getComplexIdx() {
			return reversed ? (((data.length) + (firstComplexIdx)) - (complexIdx)) - 1 : complexIdx;
		}

		private int getComplexIdxAndIncrement() {
			int idx = getComplexIdx();
			++(complexIdx);
			return idx;
		}

		private Iterator<Cell> makeComplexIterator(Object complexData) {
			ComplexColumnData ccd = ((ComplexColumnData) (complexData));
			return reversed ? ccd.reverseIterator() : ccd.iterator();
		}

		protected Cell computeNext() {
			while (true) {
				if ((complexCells) != null) {
					if (complexCells.hasNext())
						return complexCells.next();

					complexCells = null;
				}
				if ((simpleIdx) >= (firstComplexIdx)) {
					if ((complexIdx) >= (data.length))
						return endOfData();

					complexCells = makeComplexIterator(data[getComplexIdxAndIncrement()]);
				}else {
					if ((complexIdx) >= (data.length))
						return ((Cell) (data[getSimpleIdxAndIncrement()]));

					if ((comparator.compare(((ColumnData) (data[getSimpleIdx()])).column().name.bytes, ((ColumnData) (data[getComplexIdx()])).column().name.bytes)) < 0)
						return ((Cell) (data[getSimpleIdxAndIncrement()]));
					else
						complexCells = makeComplexIterator(data[getComplexIdxAndIncrement()]);

				}
			} 
		}
	}

	public static class Builder implements Row.Builder {
		private static class ComplexColumnDeletion extends BufferCell {
			public ComplexColumnDeletion(ColumnDefinition column, DeletionTime deletionTime) {
				super(column, deletionTime.markedForDeleteAt(), 0, deletionTime.localDeletionTime(), ByteBufferUtil.EMPTY_BYTE_BUFFER, CellPath.BOTTOM);
			}
		}

		private static class CellResolver implements BTree.Builder.Resolver {
			final int nowInSec;

			private CellResolver(int nowInSec) {
				this.nowInSec = nowInSec;
			}

			public ColumnData resolve(Object[] cells, int lb, int ub) {
				Cell cell = ((Cell) (cells[lb]));
				DeletionTime deletion = DeletionTime.LIVE;
				while (lb < ub) {
					cell = ((Cell) (cells[lb]));
					if (!(cell instanceof BTreeRow.Builder.ComplexColumnDeletion))
						break;

					if ((cell.timestamp()) > (deletion.markedForDeleteAt()))
						deletion = new DeletionTime(cell.timestamp(), cell.localDeletionTime());

					lb++;
				} 
				List<Object> buildFrom = new ArrayList<>((ub - lb));
				Cell previous = null;
				for (int i = lb; i < ub; i++) {
					Cell c = ((Cell) (cells[i]));
					if ((deletion == (DeletionTime.LIVE)) || ((c.timestamp()) >= (deletion.markedForDeleteAt()))) {
						previous = c;
					}
				}
				Object[] btree = BTree.build(buildFrom, UpdateFunction.noOp());
				return null;
			}
		}

		protected Clustering clustering;

		protected LivenessInfo primaryKeyLivenessInfo = LivenessInfo.EMPTY;

		protected Row.Deletion deletion = LIVE;

		private final boolean isSorted;

		private BTree.Builder<Cell> cells_;

		private final BTreeRow.Builder.CellResolver resolver;

		private boolean hasComplex = false;

		protected Builder(boolean isSorted) {
			this(isSorted, Integer.MIN_VALUE);
		}

		protected Builder(boolean isSorted, int nowInSecs) {
			cells_ = null;
			resolver = new BTreeRow.Builder.CellResolver(nowInSecs);
			this.isSorted = isSorted;
		}

		private BTree.Builder<Cell> getCells() {
			if ((cells_) == null) {
				cells_ = BTree.builder(ColumnData.comparator);
				cells_.auto(false);
			}
			return cells_;
		}

		protected Builder(BTreeRow.Builder builder) {
			clustering = builder.clustering;
			primaryKeyLivenessInfo = builder.primaryKeyLivenessInfo;
			deletion = builder.deletion;
			cells_ = ((builder.cells_) == null) ? null : builder.cells_.copy();
			resolver = builder.resolver;
			isSorted = builder.isSorted;
			hasComplex = builder.hasComplex;
		}

		@Override
		public BTreeRow.Builder copy() {
			return new BTreeRow.Builder(this);
		}

		public boolean isSorted() {
			return isSorted;
		}

		public void newRow(Clustering clustering) {
			assert (this.clustering) == null;
			this.clustering = clustering;
		}

		public Clustering clustering() {
			return clustering;
		}

		protected void reset() {
			this.clustering = null;
			this.primaryKeyLivenessInfo = LivenessInfo.EMPTY;
			this.deletion = LIVE;
			this.cells_ = null;
			this.hasComplex = false;
		}

		public void addPrimaryKeyLivenessInfo(LivenessInfo info) {
			if (!(deletion.deletes(info)))
				this.primaryKeyLivenessInfo = info;

		}

		public void addRowDeletion(Row.Deletion deletion) {
			this.deletion = deletion;
			if (deletion.deletes(primaryKeyLivenessInfo))
				this.primaryKeyLivenessInfo = LivenessInfo.EMPTY;

		}

		public void addCell(Cell cell) {
			assert (cell.column().isStatic()) == ((clustering) == (Clustering.STATIC_CLUSTERING)) : (("Column is " + (cell.column())) + ", clustering = ") + (clustering);
			if (deletion.deletes(cell))
				return;

			getCells().add(cell);
		}

		public void addComplexDeletion(ColumnDefinition column, DeletionTime complexDeletion) {
			getCells().add(new BTreeRow.Builder.ComplexColumnDeletion(column, complexDeletion));
			hasComplex = true;
		}

		public Row build() {
			if (!(isSorted))
				getCells().sort();

			if ((!(isSorted)) | (hasComplex))
				getCells().resolve(resolver);

			Object[] btree = getCells().build();
			if (deletion.isShadowedBy(primaryKeyLivenessInfo))
				deletion = LIVE;

			int minDeletionTime = BTreeRow.minDeletionTime(btree, primaryKeyLivenessInfo, deletion.time());
			Row row = BTreeRow.create(clustering, primaryKeyLivenessInfo, deletion, btree, minDeletionTime);
			reset();
			return row;
		}
	}
}

