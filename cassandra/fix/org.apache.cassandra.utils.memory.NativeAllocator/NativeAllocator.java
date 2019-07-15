

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.rows.BTreeRow;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.memory.EnsureOnHeap;
import org.apache.cassandra.utils.memory.MemoryUtil;
import org.apache.cassandra.utils.memory.MemtableAllocator;


public class NativeAllocator extends MemtableAllocator {
	private static final int MAX_REGION_SIZE = (1 * 1024) * 1024;

	private static final int MAX_CLONED_SIZE = 128 * 1024;

	private static final int MIN_REGION_SIZE = 8 * 1024;

	private static final Map<Integer, NativeAllocator.RaceAllocated> RACE_ALLOCATED = new HashMap<>();

	static {
		for (int i = NativeAllocator.MIN_REGION_SIZE; i <= (NativeAllocator.MAX_REGION_SIZE); i *= 2)
			NativeAllocator.RACE_ALLOCATED.put(i, new NativeAllocator.RaceAllocated());

	}

	private final AtomicReference<NativeAllocator.Region> currentRegion = new AtomicReference<>();

	private final ConcurrentLinkedQueue<NativeAllocator.Region> regions = new ConcurrentLinkedQueue<>();

	private static class CloningBTreeRowBuilder extends BTreeRow.Builder {
		final OpOrder.Group writeOp;

		final NativeAllocator allocator;

		private CloningBTreeRowBuilder(OpOrder.Group writeOp, NativeAllocator allocator) {
			super(true);
			this.writeOp = writeOp;
			this.allocator = allocator;
		}

		@Override
		public void newRow(Clustering clustering) {
			if (clustering != (Clustering.STATIC_CLUSTERING)) {
			}
			super.newRow(clustering);
		}

		@Override
		public void addCell(Cell cell) {
		}
	}

	public Row.Builder rowBuilder(OpOrder.Group opGroup) {
		return new NativeAllocator.CloningBTreeRowBuilder(opGroup, this);
	}

	public DecoratedKey clone(DecoratedKey key, OpOrder.Group writeOp) {
		return null;
	}

	public EnsureOnHeap ensureOnHeap() {
		return null;
	}

	public long allocate(int size, OpOrder.Group opGroup) {
		assert size >= 0;
		offHeap().allocate(size, opGroup);
		if (size > (NativeAllocator.MAX_CLONED_SIZE))
			return allocateOversize(size);

		while (true) {
			NativeAllocator.Region region = currentRegion.get();
			long peer;
			if ((region != null) && ((peer = region.allocate(size)) > 0))
				return peer;

			trySwapRegion(region, size);
		} 
	}

	private void trySwapRegion(NativeAllocator.Region current, int minSize) {
		int size;
		if (current == null)
			size = NativeAllocator.MIN_REGION_SIZE;
		else
			size = (current.capacity) * 2;

		if (minSize > size)
			size = (Integer.highestOneBit(minSize)) << 3;

		size = Math.min(NativeAllocator.MAX_REGION_SIZE, size);
		NativeAllocator.RaceAllocated raceAllocated = NativeAllocator.RACE_ALLOCATED.get(size);
		NativeAllocator.Region next = raceAllocated.poll();
		if (next == null)
			next = new NativeAllocator.Region(MemoryUtil.allocate(size), size);

		if (currentRegion.compareAndSet(current, next))
			regions.add(next);
		else
			if (!(raceAllocated.stash(next)))
				MemoryUtil.free(next.peer);


	}

	private long allocateOversize(int size) {
		NativeAllocator.Region region = new NativeAllocator.Region(MemoryUtil.allocate(size), size);
		regions.add(region);
		long peer;
		if ((peer = region.allocate(size)) == (-1))
			throw new AssertionError();

		return peer;
	}

	public void setDiscarded() {
		for (NativeAllocator.Region region : regions)
			MemoryUtil.free(region.peer);

		super.setDiscarded();
	}

	private static class RaceAllocated {
		final ConcurrentLinkedQueue<NativeAllocator.Region> stash = new ConcurrentLinkedQueue<>();

		final Semaphore permits = new Semaphore(8);

		boolean stash(NativeAllocator.Region region) {
			if (!(permits.tryAcquire()))
				return false;

			stash.add(region);
			return true;
		}

		NativeAllocator.Region poll() {
			NativeAllocator.Region next = stash.poll();
			if (next != null)
				permits.release();

			return next;
		}
	}

	private static class Region {
		private final long peer;

		private final int capacity;

		private final AtomicInteger nextFreeOffset = new AtomicInteger(0);

		private final AtomicInteger allocCount = new AtomicInteger();

		private Region(long peer, int capacity) {
			this.peer = peer;
			this.capacity = capacity;
		}

		long allocate(int size) {
			while (true) {
				int oldOffset = nextFreeOffset.get();
				if ((oldOffset + size) > (capacity))
					return -1;

				if (nextFreeOffset.compareAndSet(oldOffset, (oldOffset + size))) {
					allocCount.incrementAndGet();
					return (peer) + oldOffset;
				}
			} 
		}

		@Override
		public String toString() {
			return (((("Region@" + (System.identityHashCode(this))) + " allocs=") + (allocCount.get())) + "waste=") + ((capacity) - (nextFreeOffset.get()));
		}
	}
}

