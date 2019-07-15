

import com.google.common.primitives.Ints;
import java.io.DataInput;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.apache.cassandra.io.util.Memory;
import org.apache.cassandra.io.util.RebufferingInputStream;
import org.apache.cassandra.utils.memory.MemoryUtil;


public class MemoryInputStream extends RebufferingInputStream implements DataInput {
	private final Memory mem;

	private final int bufferSize;

	private long offset;

	@Override
	protected void reBuffer() throws IOException {
		buffer = MemoryInputStream.getByteBuffer(offset, Math.min(bufferSize, Ints.saturatedCast(memRemaining())));
		offset += buffer.capacity();
	}

	@Override
	public int available() {
		return Ints.saturatedCast(((buffer.remaining()) + (memRemaining())));
	}

	private long memRemaining() {
		return 0l;
	}

	private static ByteBuffer getByteBuffer(long offset, int length) {
		return MemoryUtil.getByteBuffer(offset, length, ByteOrder.BIG_ENDIAN);
	}
}

