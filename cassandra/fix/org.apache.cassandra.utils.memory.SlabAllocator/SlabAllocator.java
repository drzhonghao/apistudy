

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.memory.AbstractAllocator;
import org.apache.cassandra.utils.memory.ContextAllocator;
import org.apache.cassandra.utils.memory.EnsureOnHeap;
import org.apache.cassandra.utils.memory.MemtableAllocator;
import org.apache.cassandra.utils.memory.MemtableBufferAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;


public class SlabAllocator extends MemtableBufferAllocator {
	private static final Logger logger = LoggerFactory.getLogger(SlabAllocator.class);

	private static final int REGION_SIZE = 1024 * 1024;

	private static final int MAX_CLONED_SIZE = 128 * 1024;

	private static final ConcurrentLinkedQueue<SlabAllocator.Region> RACE_ALLOCATED = new ConcurrentLinkedQueue<>();

	private final AtomicReference<SlabAllocator.Region> currentRegion = new AtomicReference<>();

	private final AtomicInteger regionCount = new AtomicInteger(0);

	private final ConcurrentLinkedQueue<SlabAllocator.Region> offHeapRegions = new ConcurrentLinkedQueue<>();

	private final AtomicLong unslabbedSize = new AtomicLong(0);

	private final boolean allocateOnHeapOnly;

	private final EnsureOnHeap ensureOnHeap;

	SlabAllocator(MemtableAllocator.SubAllocator onHeap, MemtableAllocator.SubAllocator offHeap, boolean allocateOnHeapOnly) {
		super(onHeap, offHeap);
		this.allocateOnHeapOnly = allocateOnHeapOnly;
		ensureOnHeap = null;
	}

	public EnsureOnHeap ensureOnHeap() {
		return ensureOnHeap;
	}

	public ByteBuffer allocate(int size) {
		return allocate(size, null);
	}

	public ByteBuffer allocate(int size, OpOrder.Group opGroup) {
		assert size >= 0;
		if (size == 0)
			return ByteBufferUtil.EMPTY_BYTE_BUFFER;

		(allocateOnHeapOnly ? onHeap() : offHeap()).allocate(size, opGroup);
		if (size > (SlabAllocator.MAX_CLONED_SIZE)) {
			unslabbedSize.addAndGet(size);
			if (allocateOnHeapOnly)
				return ByteBuffer.allocate(size);

			SlabAllocator.Region region = new SlabAllocator.Region(ByteBuffer.allocateDirect(size));
			offHeapRegions.add(region);
			return region.allocate(size);
		}
		while (true) {
			SlabAllocator.Region region = getRegion();
			ByteBuffer cloned = region.allocate(size);
			if (cloned != null)
				return cloned;

			currentRegion.compareAndSet(region, null);
		} 
	}

	public void setDiscarded() {
		for (SlabAllocator.Region region : offHeapRegions)
			((DirectBuffer) (region.data)).cleaner().clean();

		super.setDiscarded();
	}

	private SlabAllocator.Region getRegion() {
		while (true) {
			SlabAllocator.Region region = currentRegion.get();
			if (region != null)
				return region;

			region = SlabAllocator.RACE_ALLOCATED.poll();
			if (region == null)
				region = new SlabAllocator.Region((allocateOnHeapOnly ? ByteBuffer.allocate(SlabAllocator.REGION_SIZE) : ByteBuffer.allocateDirect(SlabAllocator.REGION_SIZE)));

			if (currentRegion.compareAndSet(null, region)) {
				if (!(allocateOnHeapOnly))
					offHeapRegions.add(region);

				regionCount.incrementAndGet();
				SlabAllocator.logger.trace("{} regions now allocated in {}", regionCount, this);
				return region;
			}
			SlabAllocator.RACE_ALLOCATED.add(region);
		} 
	}

	protected AbstractAllocator allocator(OpOrder.Group writeOp) {
		return new ContextAllocator(writeOp, this);
	}

	private static class Region {
		private final ByteBuffer data;

		private final AtomicInteger nextFreeOffset = new AtomicInteger(0);

		private final AtomicInteger allocCount = new AtomicInteger();

		private Region(ByteBuffer buffer) {
			data = buffer;
		}

		public ByteBuffer allocate(int size) {
			while (true) {
				int oldOffset = nextFreeOffset.get();
				if ((oldOffset + size) > (data.capacity()))
					return null;

				if (nextFreeOffset.compareAndSet(oldOffset, (oldOffset + size))) {
					allocCount.incrementAndGet();
					return ((ByteBuffer) (data.duplicate().position(oldOffset).limit((oldOffset + size))));
				}
			} 
		}

		@Override
		public String toString() {
			return (((("Region@" + (System.identityHashCode(this))) + " allocs=") + (allocCount.get())) + "waste=") + ((data.capacity()) - (nextFreeOffset.get()));
		}
	}
}

