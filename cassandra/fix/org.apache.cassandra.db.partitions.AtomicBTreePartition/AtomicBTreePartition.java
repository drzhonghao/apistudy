

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionInfo;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.RangeTombstone;
import org.apache.cassandra.db.Slices;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.partitions.AbstractBTreePartition;
import org.apache.cassandra.db.partitions.AbstractBTreePartition.Holder;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.Rows;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.index.transactions.IndexTransaction;
import org.apache.cassandra.index.transactions.UpdateTransaction;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.ObjectSizes;
import org.apache.cassandra.utils.SearchIterator;
import org.apache.cassandra.utils.btree.UpdateFunction;
import org.apache.cassandra.utils.concurrent.Locks;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.memory.EnsureOnHeap;
import org.apache.cassandra.utils.memory.MemtableAllocator;


public class AtomicBTreePartition extends AbstractBTreePartition {
	public static final long EMPTY_SIZE = ObjectSizes.measure(new AtomicBTreePartition(CFMetaData.createFake("keyspace", "table"), DatabaseDescriptor.getPartitioner().decorateKey(ByteBuffer.allocate(1)), null));

	private static final int TRACKER_NEVER_WASTED = 0;

	private static final int TRACKER_PESSIMISTIC_LOCKING = Integer.MAX_VALUE;

	private static final int ALLOCATION_GRANULARITY_BYTES = 1024;

	private static final long EXCESS_WASTE_BYTES = (10 * 1024) * 1024L;

	private static final int EXCESS_WASTE_OFFSET = ((int) ((AtomicBTreePartition.EXCESS_WASTE_BYTES) / (AtomicBTreePartition.ALLOCATION_GRANULARITY_BYTES)));

	private static final int CLOCK_SHIFT = 17;

	private static final AtomicIntegerFieldUpdater<AtomicBTreePartition> wasteTrackerUpdater = AtomicIntegerFieldUpdater.newUpdater(AtomicBTreePartition.class, "wasteTracker");

	private static final AtomicReferenceFieldUpdater<AtomicBTreePartition, AbstractBTreePartition.Holder> refUpdater = AtomicReferenceFieldUpdater.newUpdater(AtomicBTreePartition.class, AbstractBTreePartition.Holder.class, "ref");

	private volatile int wasteTracker = AtomicBTreePartition.TRACKER_NEVER_WASTED;

	private final MemtableAllocator allocator;

	private volatile AbstractBTreePartition.Holder ref;

	public AtomicBTreePartition(CFMetaData metadata, DecoratedKey partitionKey, MemtableAllocator allocator) {
		super(metadata, partitionKey);
		this.allocator = allocator;
		this.ref = AbstractBTreePartition.EMPTY;
	}

	protected AbstractBTreePartition.Holder holder() {
		return ref;
	}

	protected boolean canHaveShadowedData() {
		return true;
	}

	public long[] addAllWithSizeDelta(final PartitionUpdate update, OpOrder.Group writeOp, UpdateTransaction indexer) {
		AtomicBTreePartition.RowUpdater updater = new AtomicBTreePartition.RowUpdater(this, allocator, writeOp, indexer);
		DeletionInfo inputDeletionInfoCopy = null;
		boolean monitorOwned = false;
		try {
			if (usePessimisticLocking()) {
				Locks.monitorEnterUnsafe(this);
				monitorOwned = true;
			}
			indexer.start();
			while (true) {
				AbstractBTreePartition.Holder current = ref;
				updater.ref = current;
				updater.reset();
				if (!(update.deletionInfo().getPartitionDeletion().isLive()))
					indexer.onPartitionDeletion(update.deletionInfo().getPartitionDeletion());

				if (update.deletionInfo().hasRanges())
					update.deletionInfo().rangeIterator(false).forEachRemaining(indexer::onRangeTombstone);

				DeletionInfo deletionInfo;
				Row newStatic = update.staticRow();
			} 
		} finally {
			indexer.commit();
			if (monitorOwned)
				Locks.monitorExitUnsafe(this);

		}
	}

	@Override
	public DeletionInfo deletionInfo() {
		return allocator.ensureOnHeap().applyToDeletionInfo(super.deletionInfo());
	}

	@Override
	public Row staticRow() {
		return allocator.ensureOnHeap().applyToStatic(super.staticRow());
	}

	@Override
	public DecoratedKey partitionKey() {
		return allocator.ensureOnHeap().applyToPartitionKey(super.partitionKey());
	}

	@Override
	public Row getRow(Clustering clustering) {
		return allocator.ensureOnHeap().applyToRow(super.getRow(clustering));
	}

	@Override
	public Row lastRow() {
		return allocator.ensureOnHeap().applyToRow(super.lastRow());
	}

	@Override
	public SearchIterator<Clustering, Row> searchIterator(ColumnFilter columns, boolean reversed) {
		return allocator.ensureOnHeap().applyToPartition(super.searchIterator(columns, reversed));
	}

	@Override
	public UnfilteredRowIterator unfilteredIterator(ColumnFilter selection, Slices slices, boolean reversed) {
		return allocator.ensureOnHeap().applyToPartition(super.unfilteredIterator(selection, slices, reversed));
	}

	@Override
	public UnfilteredRowIterator unfilteredIterator() {
		return allocator.ensureOnHeap().applyToPartition(super.unfilteredIterator());
	}

	@Override
	public UnfilteredRowIterator unfilteredIterator(AbstractBTreePartition.Holder current, ColumnFilter selection, Slices slices, boolean reversed) {
		return allocator.ensureOnHeap().applyToPartition(super.unfilteredIterator(current, selection, slices, reversed));
	}

	@Override
	public Iterator<Row> iterator() {
		return allocator.ensureOnHeap().applyToPartition(super.iterator());
	}

	public boolean usePessimisticLocking() {
		return (wasteTracker) == (AtomicBTreePartition.TRACKER_PESSIMISTIC_LOCKING);
	}

	private boolean updateWastedAllocationTracker(long wastedBytes) {
		if (wastedBytes < (AtomicBTreePartition.EXCESS_WASTE_BYTES)) {
			int wastedAllocation = ((int) ((wastedBytes + (AtomicBTreePartition.ALLOCATION_GRANULARITY_BYTES)) - 1)) / (AtomicBTreePartition.ALLOCATION_GRANULARITY_BYTES);
			int oldTrackerValue;
			while ((AtomicBTreePartition.TRACKER_PESSIMISTIC_LOCKING) != (oldTrackerValue = wasteTracker)) {
				int time = ((int) ((System.nanoTime()) >>> (AtomicBTreePartition.CLOCK_SHIFT)));
				int delta = oldTrackerValue - time;
				if (((oldTrackerValue == (AtomicBTreePartition.TRACKER_NEVER_WASTED)) || (delta >= 0)) || (delta < (-(AtomicBTreePartition.EXCESS_WASTE_OFFSET))))
					delta = -(AtomicBTreePartition.EXCESS_WASTE_OFFSET);

				delta += wastedAllocation;
				if (delta >= 0)
					break;

				if (AtomicBTreePartition.wasteTrackerUpdater.compareAndSet(this, oldTrackerValue, AtomicBTreePartition.avoidReservedValues((time + delta))))
					return false;

			} 
		}
		AtomicBTreePartition.wasteTrackerUpdater.set(this, AtomicBTreePartition.TRACKER_PESSIMISTIC_LOCKING);
		return true;
	}

	private static int avoidReservedValues(int wasteTracker) {
		if ((wasteTracker == (AtomicBTreePartition.TRACKER_NEVER_WASTED)) || (wasteTracker == (AtomicBTreePartition.TRACKER_PESSIMISTIC_LOCKING)))
			return wasteTracker + 1;

		return wasteTracker;
	}

	private static final class RowUpdater implements UpdateFunction<Row, Row> {
		final AtomicBTreePartition updating;

		final MemtableAllocator allocator;

		final OpOrder.Group writeOp;

		final UpdateTransaction indexer;

		final int nowInSec;

		AbstractBTreePartition.Holder ref;

		Row.Builder regularBuilder;

		long dataSize;

		long heapSize;

		long colUpdateTimeDelta = Long.MAX_VALUE;

		List<Row> inserted;

		private RowUpdater(AtomicBTreePartition updating, MemtableAllocator allocator, OpOrder.Group writeOp, UpdateTransaction indexer) {
			this.updating = updating;
			this.allocator = allocator;
			this.writeOp = writeOp;
			this.indexer = indexer;
			this.nowInSec = FBUtilities.nowInSeconds();
		}

		private Row.Builder builder(Clustering clustering) {
			boolean isStatic = clustering == (Clustering.STATIC_CLUSTERING);
			if (isStatic)
				return allocator.rowBuilder(writeOp);

			if ((regularBuilder) == null)
				regularBuilder = allocator.rowBuilder(writeOp);

			return regularBuilder;
		}

		public Row apply(Row insert) {
			Row data = Rows.copy(insert, builder(insert.clustering())).build();
			indexer.onInserted(insert);
			this.dataSize += data.dataSize();
			this.heapSize += data.unsharedHeapSizeExcludingData();
			if ((inserted) == null)
				inserted = new ArrayList<>();

			inserted.add(data);
			return data;
		}

		public Row apply(Row existing, Row update) {
			Row.Builder builder = builder(existing.clustering());
			colUpdateTimeDelta = Math.min(colUpdateTimeDelta, Rows.merge(existing, update, builder, nowInSec));
			Row reconciled = builder.build();
			indexer.onUpdated(existing, reconciled);
			dataSize += (reconciled.dataSize()) - (existing.dataSize());
			heapSize += (reconciled.unsharedHeapSizeExcludingData()) - (existing.unsharedHeapSizeExcludingData());
			if ((inserted) == null)
				inserted = new ArrayList<>();

			inserted.add(reconciled);
			return reconciled;
		}

		protected void reset() {
			this.dataSize = 0;
			this.heapSize = 0;
			if ((inserted) != null)
				inserted.clear();

		}

		public boolean abortEarly() {
			return (updating.ref) != (ref);
		}

		public void allocated(long heapSize) {
			this.heapSize += heapSize;
		}

		protected void finish() {
			allocator.onHeap().adjust(heapSize, writeOp);
		}
	}
}

