

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.apache.cassandra.utils.Architecture;
import org.apache.cassandra.utils.FastByteOperations;
import org.apache.cassandra.utils.concurrent.Ref;
import org.apache.cassandra.utils.memory.MemoryUtil;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;


public class Memory implements AutoCloseable {
	private static final Unsafe unsafe;

	static {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			unsafe = ((Unsafe) (field.get(null)));
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private static final long BYTE_ARRAY_BASE_OFFSET = Memory.unsafe.arrayBaseOffset(byte[].class);

	private static final boolean bigEndian = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

	public static final ByteBuffer[] NO_BYTE_BUFFERS = new ByteBuffer[0];

	protected long peer;

	protected final long size;

	protected Memory(long bytes) {
		if (bytes <= 0)
			throw new AssertionError();

		size = bytes;
		peer = MemoryUtil.allocate(size);
		if ((peer) == 0)
			throw new OutOfMemoryError();

	}

	protected Memory(Memory copyOf) {
		size = copyOf.size;
		peer = copyOf.peer;
	}

	public static Memory allocate(long bytes) {
		if (bytes < 0)
			throw new IllegalArgumentException();

		if (Ref.DEBUG_ENABLED) {
		}
		return new Memory(bytes);
	}

	public void setByte(long offset, byte b) {
		checkBounds(offset, (offset + 1));
		Memory.unsafe.putByte(((peer) + offset), b);
	}

	public void setMemory(long offset, long bytes, byte b) {
		checkBounds(offset, (offset + bytes));
		Memory.unsafe.setMemory(((peer) + offset), bytes, b);
	}

	public void setLong(long offset, long l) {
		checkBounds(offset, (offset + 8));
		if (Architecture.IS_UNALIGNED) {
			Memory.unsafe.putLong(((peer) + offset), l);
		}else {
			putLongByByte(((peer) + offset), l);
		}
	}

	private void putLongByByte(long address, long value) {
		if (Memory.bigEndian) {
			Memory.unsafe.putByte(address, ((byte) (value >> 56)));
			Memory.unsafe.putByte((address + 1), ((byte) (value >> 48)));
			Memory.unsafe.putByte((address + 2), ((byte) (value >> 40)));
			Memory.unsafe.putByte((address + 3), ((byte) (value >> 32)));
			Memory.unsafe.putByte((address + 4), ((byte) (value >> 24)));
			Memory.unsafe.putByte((address + 5), ((byte) (value >> 16)));
			Memory.unsafe.putByte((address + 6), ((byte) (value >> 8)));
			Memory.unsafe.putByte((address + 7), ((byte) (value)));
		}else {
			Memory.unsafe.putByte((address + 7), ((byte) (value >> 56)));
			Memory.unsafe.putByte((address + 6), ((byte) (value >> 48)));
			Memory.unsafe.putByte((address + 5), ((byte) (value >> 40)));
			Memory.unsafe.putByte((address + 4), ((byte) (value >> 32)));
			Memory.unsafe.putByte((address + 3), ((byte) (value >> 24)));
			Memory.unsafe.putByte((address + 2), ((byte) (value >> 16)));
			Memory.unsafe.putByte((address + 1), ((byte) (value >> 8)));
			Memory.unsafe.putByte(address, ((byte) (value)));
		}
	}

	public void setInt(long offset, int l) {
		checkBounds(offset, (offset + 4));
		if (Architecture.IS_UNALIGNED) {
			Memory.unsafe.putInt(((peer) + offset), l);
		}else {
			putIntByByte(((peer) + offset), l);
		}
	}

	private void putIntByByte(long address, int value) {
		if (Memory.bigEndian) {
			Memory.unsafe.putByte(address, ((byte) (value >> 24)));
			Memory.unsafe.putByte((address + 1), ((byte) (value >> 16)));
			Memory.unsafe.putByte((address + 2), ((byte) (value >> 8)));
			Memory.unsafe.putByte((address + 3), ((byte) (value)));
		}else {
			Memory.unsafe.putByte((address + 3), ((byte) (value >> 24)));
			Memory.unsafe.putByte((address + 2), ((byte) (value >> 16)));
			Memory.unsafe.putByte((address + 1), ((byte) (value >> 8)));
			Memory.unsafe.putByte(address, ((byte) (value)));
		}
	}

	public void setShort(long offset, short l) {
		checkBounds(offset, (offset + 2));
		if (Architecture.IS_UNALIGNED) {
			Memory.unsafe.putShort(((peer) + offset), l);
		}else {
			putShortByByte(((peer) + offset), l);
		}
	}

	private void putShortByByte(long address, short value) {
		if (Memory.bigEndian) {
			Memory.unsafe.putByte(address, ((byte) (value >> 8)));
			Memory.unsafe.putByte((address + 1), ((byte) (value)));
		}else {
			Memory.unsafe.putByte((address + 1), ((byte) (value >> 8)));
			Memory.unsafe.putByte(address, ((byte) (value)));
		}
	}

	public void setBytes(long memoryOffset, ByteBuffer buffer) {
		if (buffer == null)
			throw new NullPointerException();
		else
			if ((buffer.remaining()) == 0)
				return;


		checkBounds(memoryOffset, (memoryOffset + (buffer.remaining())));
		if (buffer.hasArray()) {
			setBytes(memoryOffset, buffer.array(), ((buffer.arrayOffset()) + (buffer.position())), buffer.remaining());
		}else
			if (buffer instanceof DirectBuffer) {
				Memory.unsafe.copyMemory(((((DirectBuffer) (buffer)).address()) + (buffer.position())), ((peer) + memoryOffset), buffer.remaining());
			}else
				throw new IllegalStateException();


	}

	public void setBytes(long memoryOffset, byte[] buffer, int bufferOffset, int count) {
		if (buffer == null)
			throw new NullPointerException();
		else
			if (((bufferOffset < 0) || (count < 0)) || ((bufferOffset + count) > (buffer.length)))
				throw new IndexOutOfBoundsException();
			else
				if (count == 0)
					return;



		checkBounds(memoryOffset, (memoryOffset + count));
		Memory.unsafe.copyMemory(buffer, ((Memory.BYTE_ARRAY_BASE_OFFSET) + bufferOffset), null, ((peer) + memoryOffset), count);
	}

	public byte getByte(long offset) {
		checkBounds(offset, (offset + 1));
		return Memory.unsafe.getByte(((peer) + offset));
	}

	public long getLong(long offset) {
		checkBounds(offset, (offset + 8));
		if (Architecture.IS_UNALIGNED) {
			return Memory.unsafe.getLong(((peer) + offset));
		}else {
			return getLongByByte(((peer) + offset));
		}
	}

	private long getLongByByte(long address) {
		if (Memory.bigEndian) {
			return (((((((((long) (Memory.unsafe.getByte(address))) << 56) | ((((long) (Memory.unsafe.getByte((address + 1)))) & 255) << 48)) | ((((long) (Memory.unsafe.getByte((address + 2)))) & 255) << 40)) | ((((long) (Memory.unsafe.getByte((address + 3)))) & 255) << 32)) | ((((long) (Memory.unsafe.getByte((address + 4)))) & 255) << 24)) | ((((long) (Memory.unsafe.getByte((address + 5)))) & 255) << 16)) | ((((long) (Memory.unsafe.getByte((address + 6)))) & 255) << 8)) | (((long) (Memory.unsafe.getByte((address + 7)))) & 255);
		}else {
			return (((((((((long) (Memory.unsafe.getByte((address + 7)))) << 56) | ((((long) (Memory.unsafe.getByte((address + 6)))) & 255) << 48)) | ((((long) (Memory.unsafe.getByte((address + 5)))) & 255) << 40)) | ((((long) (Memory.unsafe.getByte((address + 4)))) & 255) << 32)) | ((((long) (Memory.unsafe.getByte((address + 3)))) & 255) << 24)) | ((((long) (Memory.unsafe.getByte((address + 2)))) & 255) << 16)) | ((((long) (Memory.unsafe.getByte((address + 1)))) & 255) << 8)) | (((long) (Memory.unsafe.getByte(address))) & 255);
		}
	}

	public int getInt(long offset) {
		checkBounds(offset, (offset + 4));
		if (Architecture.IS_UNALIGNED) {
			return Memory.unsafe.getInt(((peer) + offset));
		}else {
			return getIntByByte(((peer) + offset));
		}
	}

	private int getIntByByte(long address) {
		if (Memory.bigEndian) {
			return ((((Memory.unsafe.getByte(address)) << 24) | (((Memory.unsafe.getByte((address + 1))) & 255) << 16)) | (((Memory.unsafe.getByte((address + 2))) & 255) << 8)) | ((Memory.unsafe.getByte((address + 3))) & 255);
		}else {
			return ((((Memory.unsafe.getByte((address + 3))) << 24) | (((Memory.unsafe.getByte((address + 2))) & 255) << 16)) | (((Memory.unsafe.getByte((address + 1))) & 255) << 8)) | ((Memory.unsafe.getByte(address)) & 255);
		}
	}

	public void getBytes(long memoryOffset, byte[] buffer, int bufferOffset, int count) {
		if (buffer == null)
			throw new NullPointerException();
		else
			if (((bufferOffset < 0) || (count < 0)) || (count > ((buffer.length) - bufferOffset)))
				throw new IndexOutOfBoundsException();
			else
				if (count == 0)
					return;



		checkBounds(memoryOffset, (memoryOffset + count));
		FastByteOperations.UnsafeOperations.copy(null, ((peer) + memoryOffset), buffer, bufferOffset, count);
	}

	@net.nicoulaj.compilecommand.annotations.Inline
	protected void checkBounds(long start, long end) {
		assert (peer) != 0 : "Memory was freed";
		assert ((start >= 0) && (end <= (size))) && (start <= end) : (((("Illegal bounds [" + start) + "..") + end) + "); size: ") + (size);
	}

	public void put(long trgOffset, Memory memory, long srcOffset, long size) {
		checkBounds(trgOffset, (trgOffset + size));
		memory.checkBounds(srcOffset, (srcOffset + size));
		Memory.unsafe.copyMemory(((memory.peer) + srcOffset), ((peer) + trgOffset), size);
	}

	public Memory copy(long newSize) {
		Memory copy = Memory.allocate(newSize);
		copy.put(0, this, 0, Math.min(size(), newSize));
		return copy;
	}

	public void free() {
		if ((peer) != 0)
			MemoryUtil.free(peer);
		else
			assert (size) == 0;

		peer = 0;
	}

	public void close() {
		free();
	}

	public long size() {
		assert (peer) != 0;
		return size;
	}

	@Override
	public boolean equals(Object o) {
		if ((this) == o)
			return true;

		if (!(o instanceof Memory))
			return false;

		Memory b = ((Memory) (o));
		if (((peer) == (b.peer)) && ((size) == (b.size)))
			return true;

		return false;
	}

	public ByteBuffer[] asByteBuffers(long offset, long length) {
		checkBounds(offset, (offset + length));
		if ((size()) == 0)
			return Memory.NO_BYTE_BUFFERS;

		ByteBuffer[] result = new ByteBuffer[((int) (length / (Integer.MAX_VALUE))) + 1];
		int size = ((int) ((size()) / (result.length)));
		for (int i = 0; i < ((result.length) - 1); i++) {
			result[i] = MemoryUtil.getByteBuffer(((peer) + offset), size);
			offset += size;
			length -= size;
		}
		result[((result.length) - 1)] = MemoryUtil.getByteBuffer(((peer) + offset), ((int) (length)));
		return result;
	}

	public ByteBuffer asByteBuffer(long offset, int length) {
		checkBounds(offset, (offset + length));
		return MemoryUtil.getByteBuffer(((peer) + offset), length);
	}

	public void setByteBuffer(ByteBuffer buffer, long offset, int length) {
		checkBounds(offset, (offset + length));
		MemoryUtil.setByteBuffer(buffer, ((peer) + offset), length);
	}

	public String toString() {
		return Memory.toString(peer, size);
	}

	protected static String toString(long peer, long size) {
		return String.format("Memory@[%x..%x)", peer, (peer + size));
	}
}

