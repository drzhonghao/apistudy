

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.apache.cassandra.io.util.BufferedDataOutputStreamPlus;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.io.util.Memory;
import org.apache.cassandra.io.util.SafeMemory;


public class SafeMemoryWriter extends DataOutputBuffer {
	private SafeMemory memory;

	@SuppressWarnings("resource")
	public SafeMemoryWriter(long initialCapacity) {
		this(new SafeMemory(initialCapacity));
	}

	private SafeMemoryWriter(SafeMemory memory) {
		super(SafeMemoryWriter.tailBuffer(memory).order(ByteOrder.BIG_ENDIAN));
		this.memory = memory;
	}

	public SafeMemory currentBuffer() {
		return memory;
	}

	@Override
	protected void reallocate(long count) {
	}

	public void setCapacity(long newCapacity) {
		reallocate(newCapacity);
	}

	public void close() {
		memory.close();
	}

	public Throwable close(Throwable accumulate) {
		return memory.close(accumulate);
	}

	public long length() {
		return (SafeMemoryWriter.tailOffset(memory)) + (buffer.position());
	}

	public long capacity() {
		return memory.size();
	}

	@Override
	public SafeMemoryWriter order(ByteOrder order) {
		super.order(order);
		return this;
	}

	public long validateReallocation(long newSize) {
		return newSize;
	}

	private static long tailOffset(Memory memory) {
		return 0l;
	}

	private static ByteBuffer tailBuffer(Memory memory) {
		return null;
	}
}

