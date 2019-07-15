import org.apache.cassandra.db.commitlog.*;


import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.concurrent.FastThreadLocal;
import org.apache.cassandra.io.compress.BufferType;
import org.apache.cassandra.io.util.FileUtils;

/**
 * A very simple Bytebuffer pool with a fixed allocation size and a cached max allocation count. Will allow
 * you to go past the "max", freeing all buffers allocated beyond the max buffer count on release.
 *
 * Has a reusable thread local ByteBuffer that users can make use of.
 */
public class SimpleCachedBufferPool
{
    protected static final FastThreadLocal<ByteBuffer> reusableBufferHolder = new FastThreadLocal<ByteBuffer>()
    {
        protected ByteBuffer initialValue()
        {
            return ByteBuffer.allocate(0);
        }
    };

    private Queue<ByteBuffer> bufferPool = new ConcurrentLinkedQueue<>();
    private AtomicInteger usedBuffers = new AtomicInteger(0);

    /**
     * Maximum number of buffers in the compression pool. Any buffers above this count that are allocated will be cleaned
     * upon release rather than held and re-used.
     */
    private final int maxBufferPoolSize;

    /**
     * Size of individual buffer segments on allocation.
     */
    private final int bufferSize;

    private BufferType preferredReusableBufferType = BufferType.ON_HEAP;

    public SimpleCachedBufferPool(int maxBufferPoolSize, int bufferSize)
    {
        this.maxBufferPoolSize = maxBufferPoolSize;
        this.bufferSize = bufferSize;
    }

    public ByteBuffer createBuffer(BufferType bufferType)
    {
        usedBuffers.incrementAndGet();
        ByteBuffer buf = bufferPool.poll();
        if (buf != null)
        {
            buf.clear();
            return buf;
        }
        return bufferType.allocate(bufferSize);
    }

    public ByteBuffer getThreadLocalReusableBuffer(int size)
    {
        ByteBuffer result = reusableBufferHolder.get();
        if (result.capacity() < size || BufferType.typeOf(result) != preferredReusableBufferType)
        {
            FileUtils.clean(result);
            result = preferredReusableBufferType.allocate(size);
            reusableBufferHolder.set(result);
        }
        return result;
    }

    public void setPreferredReusableBufferType(BufferType type)
    {
        preferredReusableBufferType = type;
    }

    public void releaseBuffer(ByteBuffer buffer)
    {
        usedBuffers.decrementAndGet();

        if (bufferPool.size() < maxBufferPoolSize)
            bufferPool.add(buffer);
        else
            FileUtils.clean(buffer);
    }

    public void shutdown()
    {
        bufferPool.clear();
    }

    public boolean atLimit()
    {
        return usedBuffers.get() >= maxBufferPoolSize;
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
               .append("SimpleBufferPool:")
               .append(" bufferCount:").append(usedBuffers.get())
               .append(", bufferSize:").append(maxBufferPoolSize)
               .append(", buffer size:").append(bufferSize)
               .toString();
    }
}
