

import com.codahale.metrics.Timer;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import org.apache.cassandra.metrics.CassandraMetricsRegistry;
import org.apache.cassandra.metrics.DefaultNameFactory;
import org.apache.cassandra.utils.concurrent.WaitQueue;
import org.apache.cassandra.utils.memory.MemtableAllocator;


public abstract class MemtablePool {
	public final MemtablePool.SubPool onHeap;

	public final MemtablePool.SubPool offHeap;

	public final Timer blockedOnAllocating;

	final WaitQueue hasRoom = new WaitQueue();

	MemtablePool(long maxOnHeapMemory, long maxOffHeapMemory, float cleanThreshold, Runnable cleaner) {
		this.onHeap = getSubPool(maxOnHeapMemory, cleanThreshold);
		this.offHeap = getSubPool(maxOffHeapMemory, cleanThreshold);
		blockedOnAllocating = CassandraMetricsRegistry.Metrics.timer(new DefaultNameFactory("MemtablePool").createMetricName("BlockedOnAllocation"));
	}

	MemtablePool.SubPool getSubPool(long limit, float cleanThreshold) {
		return new MemtablePool.SubPool(limit, cleanThreshold);
	}

	public abstract MemtableAllocator newAllocator();

	public class SubPool {
		public final long limit;

		public final float cleanThreshold;

		volatile long allocated;

		volatile long reclaiming;

		volatile long nextClean;

		public SubPool(long limit, float cleanThreshold) {
			this.limit = limit;
			this.cleanThreshold = cleanThreshold;
		}

		boolean needsCleaning() {
			return ((used()) > (nextClean)) && (updateNextClean());
		}

		void maybeClean() {
		}

		private boolean updateNextClean() {
			while (true) {
				long current = nextClean;
				long reclaiming = this.reclaiming;
				long next = reclaiming + ((long) ((this.limit) * (cleanThreshold)));
				if ((current == next) || (MemtablePool.nextCleanUpdater.compareAndSet(this, current, next)))
					return (used()) > next;

			} 
		}

		boolean tryAllocate(long size) {
			while (true) {
				long cur;
				if (((cur = allocated) + size) > (limit))
					return false;

				if (MemtablePool.allocatedUpdater.compareAndSet(this, cur, (cur + size)))
					return true;

			} 
		}

		private void adjustAllocated(long size) {
			while (true) {
				long cur = allocated;
				if (MemtablePool.allocatedUpdater.compareAndSet(this, cur, (cur + size)))
					return;

			} 
		}

		void allocated(long size) {
			assert size >= 0;
			if (size == 0)
				return;

			adjustAllocated(size);
			maybeClean();
		}

		void acquired(long size) {
			maybeClean();
		}

		void released(long size) {
			assert size >= 0;
			adjustAllocated((-size));
			hasRoom.signalAll();
		}

		void reclaiming(long size) {
			if (size == 0)
				return;

			MemtablePool.reclaimingUpdater.addAndGet(this, size);
		}

		void reclaimed(long size) {
			if (size == 0)
				return;

			MemtablePool.reclaimingUpdater.addAndGet(this, (-size));
		}

		public long used() {
			return allocated;
		}

		public float reclaimingRatio() {
			float r = (reclaiming) / ((float) (limit));
			if (Float.isNaN(r))
				return 0;

			return r;
		}

		public float usedRatio() {
			float r = (allocated) / ((float) (limit));
			if (Float.isNaN(r))
				return 0;

			return r;
		}

		public MemtableAllocator.SubAllocator newAllocator() {
			return null;
		}

		public WaitQueue hasRoom() {
			return hasRoom;
		}

		public Timer.Context blockedTimerContext() {
			return blockedOnAllocating.time();
		}
	}

	private static final AtomicLongFieldUpdater<MemtablePool.SubPool> reclaimingUpdater = AtomicLongFieldUpdater.newUpdater(MemtablePool.SubPool.class, "reclaiming");

	private static final AtomicLongFieldUpdater<MemtablePool.SubPool> allocatedUpdater = AtomicLongFieldUpdater.newUpdater(MemtablePool.SubPool.class, "allocated");

	private static final AtomicLongFieldUpdater<MemtablePool.SubPool> nextCleanUpdater = AtomicLongFieldUpdater.newUpdater(MemtablePool.SubPool.class, "nextClean");
}

