import org.apache.cassandra.db.*;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.cassandra.dht.Token;
import org.apache.cassandra.utils.concurrent.OpOrder;
import org.apache.cassandra.utils.memory.MemoryUtil;
import org.apache.cassandra.utils.memory.NativeAllocator;

public class NativeDecoratedKey extends DecoratedKey
{
    final long peer;

    public NativeDecoratedKey(Token token, NativeAllocator allocator, OpOrder.Group writeOp, ByteBuffer key)
    {
        super(token);
        assert key != null;
        assert key.order() == ByteOrder.BIG_ENDIAN;

        int size = key.remaining();
        this.peer = allocator.allocate(4 + size, writeOp);
        MemoryUtil.setInt(peer, size);
        MemoryUtil.setBytes(peer + 4, key);
    }

    public ByteBuffer getKey()
    {
        return MemoryUtil.getByteBuffer(peer + 4, MemoryUtil.getInt(peer), ByteOrder.BIG_ENDIAN);
    }
}
