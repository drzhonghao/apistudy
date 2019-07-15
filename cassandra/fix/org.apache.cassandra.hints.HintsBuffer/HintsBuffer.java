

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.zip.CRC32;
import org.apache.cassandra.hints.Hint;
import org.apache.cassandra.io.util.BufferedDataOutputStreamPlus;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.DataOutputBufferFixed;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.AbstractIterator;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.concurrent.OpOrder;


final class HintsBuffer {
	static final int ENTRY_OVERHEAD_SIZE = 12;

	static final int CLOSED = -1;

	private final ByteBuffer slab;

	private final AtomicInteger position;

	private final ConcurrentMap<UUID, Queue<Integer>> offsets;

	private final OpOrder appendOrder;

	private HintsBuffer(ByteBuffer slab) {
		this.slab = slab;
		position = new AtomicInteger();
		offsets = new ConcurrentHashMap<>();
		appendOrder = new OpOrder();
	}

	static HintsBuffer create(int slabSize) {
		return new HintsBuffer(ByteBuffer.allocateDirect(slabSize));
	}

	boolean isClosed() {
		return (position.get()) == (HintsBuffer.CLOSED);
	}

	int capacity() {
		return slab.capacity();
	}

	int remaining() {
		int pos = position.get();
		return pos == (HintsBuffer.CLOSED) ? 0 : (capacity()) - pos;
	}

	HintsBuffer recycle() {
		slab.clear();
		return new HintsBuffer(slab);
	}

	void free() {
		FileUtils.clean(slab);
	}

	void waitForModifications() {
		appendOrder.awaitNewBarrier();
	}

	Set<UUID> hostIds() {
		return offsets.keySet();
	}

	Iterator<ByteBuffer> consumingHintsIterator(UUID hostId) {
		final Queue<Integer> bufferOffsets = offsets.get(hostId);
		if (bufferOffsets == null)
			return Collections.emptyIterator();

		return new AbstractIterator<ByteBuffer>() {
			private final ByteBuffer flyweight = slab.duplicate();

			protected ByteBuffer computeNext() {
				Integer offset = bufferOffsets.poll();
				if (offset == null)
					return endOfData();

				int totalSize = (slab.getInt(offset)) + (HintsBuffer.ENTRY_OVERHEAD_SIZE);
				return ((ByteBuffer) (flyweight.clear().position(offset).limit((offset + totalSize))));
			}
		};
	}

	@SuppressWarnings("resource")
	HintsBuffer.Allocation allocate(int hintSize) {
		int totalSize = hintSize + (HintsBuffer.ENTRY_OVERHEAD_SIZE);
		if (totalSize > ((slab.capacity()) / 2)) {
			throw new IllegalArgumentException(String.format("Hint of %s bytes is too large - the maximum size is %s", hintSize, ((slab.capacity()) / 2)));
		}
		OpOrder.Group opGroup = appendOrder.start();
		try {
			return allocate(totalSize, opGroup);
		} catch (Throwable t) {
			opGroup.close();
			throw t;
		}
	}

	private HintsBuffer.Allocation allocate(int totalSize, OpOrder.Group opGroup) {
		int offset = allocateBytes(totalSize);
		if (offset < 0) {
			opGroup.close();
			return null;
		}
		return new HintsBuffer.Allocation(offset, totalSize, opGroup);
	}

	private int allocateBytes(int totalSize) {
		while (true) {
			int prev = position.get();
			int next = prev + totalSize;
			if (prev == (HintsBuffer.CLOSED))
				return HintsBuffer.CLOSED;

			if (next > (slab.capacity())) {
				position.set(HintsBuffer.CLOSED);
				return HintsBuffer.CLOSED;
			}
			if (position.compareAndSet(prev, next))
				return prev;

		} 
	}

	private void put(UUID hostId, int offset) {
		Queue<Integer> queue = offsets.get(hostId);
		if (queue == null)
			queue = offsets.computeIfAbsent(hostId, ( id) -> new ConcurrentLinkedQueue<>());

		queue.offer(offset);
	}

	final class Allocation implements AutoCloseable {
		private final Integer offset;

		private final int totalSize;

		private final OpOrder.Group opGroup;

		Allocation(int offset, int totalSize, OpOrder.Group opGroup) {
			this.offset = offset;
			this.totalSize = totalSize;
			this.opGroup = opGroup;
		}

		void write(Iterable<UUID> hostIds, Hint hint) {
			write(hint);
			for (UUID hostId : hostIds)
				put(hostId, offset);

		}

		public void close() {
			opGroup.close();
		}

		private void write(Hint hint) {
			ByteBuffer buffer = ((ByteBuffer) (slab.duplicate().position(offset).limit(((offset) + (totalSize)))));
			CRC32 crc = new CRC32();
			int hintSize = (totalSize) - (HintsBuffer.ENTRY_OVERHEAD_SIZE);
			try (DataOutputBuffer dop = new DataOutputBufferFixed(buffer)) {
				dop.writeInt(hintSize);
				FBUtilities.updateChecksumInt(crc, hintSize);
				dop.writeInt(((int) (crc.getValue())));
				FBUtilities.updateChecksum(crc, buffer, ((buffer.position()) - hintSize), hintSize);
				dop.writeInt(((int) (crc.getValue())));
			} catch (IOException e) {
				throw new AssertionError();
			}
		}
	}
}

