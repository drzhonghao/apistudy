import org.apache.cassandra.utils.memory.AbstractAllocator;
import org.apache.cassandra.utils.memory.MemtableBufferAllocator;
import org.apache.cassandra.utils.memory.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.concurrent.OpOrder;

/**
 * Wraps calls to a PoolAllocator with the provided writeOp. Also doubles as a Function that clones Cells
 * using itself
 */
public final class ContextAllocator extends AbstractAllocator
{
    private final OpOrder.Group opGroup;
    private final MemtableBufferAllocator allocator;

    public ContextAllocator(OpOrder.Group opGroup, MemtableBufferAllocator allocator)
    {
        this.opGroup = opGroup;
        this.allocator = allocator;
    }

    @Override
    public ByteBuffer clone(ByteBuffer buffer)
    {
        assert buffer != null;
        if (buffer.remaining() == 0)
            return ByteBufferUtil.EMPTY_BYTE_BUFFER;
        ByteBuffer cloned = allocate(buffer.remaining());

        cloned.mark();
        cloned.put(buffer.duplicate());
        cloned.reset();
        return cloned;
    }

    public ByteBuffer allocate(int size)
    {
        return allocator.allocate(size, opGroup);
    }
}
