import org.apache.cassandra.utils.memory.AbstractAllocator;
import org.apache.cassandra.utils.memory.*;


import java.nio.ByteBuffer;

public final class HeapAllocator extends AbstractAllocator
{
    public static final HeapAllocator instance = new HeapAllocator();

    /**
     * Normally you should use HeapAllocator.instance, since there is no per-Allocator state.
     * This is exposed so that the reflection done by Memtable works when SlabAllocator is disabled.
     */
    private HeapAllocator() {}

    public ByteBuffer allocate(int size)
    {
        return ByteBuffer.allocate(size);
    }

    public boolean allocatingOnHeap()
    {
        return true;
    }
}
