import org.apache.cassandra.io.util.Memory;
import org.apache.cassandra.io.util.*;


import net.nicoulaj.compilecommand.annotations.Inline;
import org.apache.cassandra.utils.concurrent.Ref;
import org.apache.cassandra.utils.concurrent.RefCounted;
import org.apache.cassandra.utils.concurrent.SharedCloseable;
import org.apache.cassandra.utils.memory.MemoryUtil;

public class SafeMemory extends Memory implements SharedCloseable
{
    private final Ref<?> ref;
    public SafeMemory(long size)
    {
        super(size);
        ref = new Ref<>(null, new MemoryTidy(peer, size));
    }

    private SafeMemory(SafeMemory copyOf)
    {
        super(copyOf);
        ref = copyOf.ref.ref();
        /** see {@link Memory#Memory(long)} re: null pointers*/
        if (peer == 0 && size != 0)
        {
            ref.ensureReleased();
            throw new IllegalStateException("Cannot create a sharedCopy of a SafeMemory object that has already been closed");
        }
    }

    public SafeMemory sharedCopy()
    {
        return new SafeMemory(this);
    }

    public void free()
    {
        ref.release();
        peer = 0;
    }

    public void close()
    {
        ref.ensureReleased();
        peer = 0;
    }

    public Throwable close(Throwable accumulate)
    {
        return ref.ensureReleased(accumulate);
    }

    public SafeMemory copy(long newSize)
    {
        SafeMemory copy = new SafeMemory(newSize);
        copy.put(0, this, 0, Math.min(size(), newSize));
        return copy;
    }

    private static final class MemoryTidy implements RefCounted.Tidy
    {
        final long peer;
        final long size;
        private MemoryTidy(long peer, long size)
        {
            this.peer = peer;
            this.size = size;
        }

        public void tidy()
        {
            /** see {@link Memory#Memory(long)} re: null pointers*/
            if (peer != 0)
                MemoryUtil.free(peer);
        }

        public String name()
        {
            return Memory.toString(peer, size);
        }
    }

    @Inline
    protected void checkBounds(long start, long end)
    {
        assert peer != 0 || size == 0 : ref.printDebugInfo();
        super.checkBounds(start, end);
    }

    public void addTo(Ref.IdentityCollection identities)
    {
        identities.add(ref);
    }
}
