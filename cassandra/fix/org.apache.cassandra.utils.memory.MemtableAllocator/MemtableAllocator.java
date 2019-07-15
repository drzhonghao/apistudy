

import com.codahale.metrics.Timer;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.concurrent.WaitQueue;
import org.apache.cassandra.utils.memory.EnsureOnHeap;
import org.apache.cassandra.utils.memory.MemtablePool;


public abstract class MemtableAllocator {
	private final MemtableAllocator.SubAllocator onHeap;

	private final MemtableAllocator.SubAllocator offHeap;

	volatile MemtableAllocator.LifeCycle state = MemtableAllocator.LifeCycle.LIVE;

	enum LifeCycle {

		LIVE,
		DISCARDING,
		DISCARDED;
		MemtableAllocator.LifeCycle transition(MemtableAllocator.LifeCycle targetState) {
			switch (targetState) {
				case DISCARDING :
					assert (this) == (MemtableAllocator.LifeCycle.LIVE);
					return MemtableAllocator.LifeCycle.DISCARDING;
				case DISCARDED :
					assert (this) == (MemtableAllocator.LifeCycle.DISCARDING);
					return MemtableAllocator.LifeCycle.DISCARDED;
				default :
					throw new IllegalStateException();
			}
		}
	}

	MemtableAllocator(MemtableAllocator.SubAllocator onHeap, MemtableAllocator.SubAllocator offHeap) {
		this.onHeap = onHeap;
		this.offHeap = offHeap;
	}

	public abstract Row.Builder rowBuilder(OpOrder.Group opGroup);

	public abstract DecoratedKey clone(DecoratedKey key, OpOrder.Group opGroup);

	public abstract EnsureOnHeap ensureOnHeap();

	public MemtableAllocator.SubAllocator onHeap() {
		return onHeap;
	}

	public MemtableAllocator.SubAllocator offHeap() {
		return offHeap;
	}

	public void setDiscarding() {
		state = state.transition(MemtableAllocator.LifeCycle.DISCARDING);
		onHeap.markAllReclaiming();
		offHeap.markAllReclaiming();
	}

	public void setDiscarded() {
		state = state.transition(MemtableAllocator.LifeCycle.DISCARDED);
		onHeap.releaseAll();
		offHeap.releaseAll();
	}

	public boolean isLive() {
		return (state) == (MemtableAllocator.LifeCycle.LIVE);
	}

	public static final class SubAllocator {
		private final MemtablePool.SubPool parent;

		private volatile long owns;

		private volatile long reclaiming;

		SubAllocator(MemtablePool.SubPool parent) {
			this.parent = parent;
		}

		void releaseAll() {
		}

		public void adjust(long size, OpOrder.Group opGroup) {
			if (size <= 0)
				released((-size));
			else
				allocate(size, opGroup);

		}

		public void allocate(long size, OpOrder.Group opGroup) {
			assert size >= 0;
			while (true) {
				WaitQueue.Signal signal = opGroup.isBlockingSignal(parent.hasRoom().register(parent.blockedTimerContext()));
			} 
		}

		private void allocated(long size) {
			MemtableAllocator.SubAllocator.ownsUpdater.addAndGet(this, size);
		}

		private void acquired(long size) {
			MemtableAllocator.SubAllocator.ownsUpdater.addAndGet(this, size);
		}

		void released(long size) {
			MemtableAllocator.SubAllocator.ownsUpdater.addAndGet(this, (-size));
		}

		void markAllReclaiming() {
			while (true) {
				long cur = owns;
				long prev = reclaiming;
				if (!(MemtableAllocator.SubAllocator.reclaimingUpdater.compareAndSet(this, prev, cur)))
					continue;

				return;
			} 
		}

		public long owns() {
			return owns;
		}

		public float ownershipRatio() {
			float r = (owns) / ((float) (parent.limit));
			if (Float.isNaN(r))
				return 0;

			return r;
		}

		private static final AtomicLongFieldUpdater<MemtableAllocator.SubAllocator> ownsUpdater = AtomicLongFieldUpdater.newUpdater(MemtableAllocator.SubAllocator.class, "owns");

		private static final AtomicLongFieldUpdater<MemtableAllocator.SubAllocator> reclaimingUpdater = AtomicLongFieldUpdater.newUpdater(MemtableAllocator.SubAllocator.class, "reclaiming");
	}
}

