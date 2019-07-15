import org.apache.cassandra.utils.concurrent.*;


import java.util.Iterator;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A simple append-only collection supporting an unbounded number of concurrent readers/writers,
 * but a bounded number of items.
 *
 * @param <E>
 */
public class Accumulator<E> implements Iterable<E>
{
    private volatile int nextIndex;
    private volatile int presentCount;
    private final Object[] values;
    private static final AtomicIntegerFieldUpdater<Accumulator> nextIndexUpdater = AtomicIntegerFieldUpdater.newUpdater(Accumulator.class, "nextIndex");
    private static final AtomicIntegerFieldUpdater<Accumulator> presentCountUpdater = AtomicIntegerFieldUpdater.newUpdater(Accumulator.class, "presentCount");

    public Accumulator(int size)
    {
        values = new Object[size];
    }

    /**
     * Adds an item to the collection.
     *
     * Note it is not guaranteed to be visible on exiting the method, if another add was happening concurrently;
     * it will be visible once all concurrent adds (which are non-blocking) complete, but it is not guaranteed
     * that any size change occurs during the execution of any specific call.
     *
     * @param item add to collection
     */
    public void add(E item)
    {
        int insertPos;
        while (true)
        {
            insertPos = nextIndex;
            if (insertPos >= values.length)
                throw new IllegalStateException();
            if (nextIndexUpdater.compareAndSet(this, insertPos, insertPos + 1))
                break;
        }
        values[insertPos] = item;
        // we then try to increase presentCount for each consecutive value that is visible after the current size;
        // this should hopefully extend past us, but if it doesn't this behaviour means the lagging write will fix up
        // our state for us.
        //
        // we piggyback off presentCountUpdater to get volatile write semantics for our update to values
        boolean volatileWrite = false;
        while (true)
        {
            int cur = presentCount;
            if (cur != insertPos && (cur == values.length || values[cur] == null))
            {
                // ensure our item has been made visible before aborting
                if (!volatileWrite && cur < insertPos && !presentCountUpdater.compareAndSet(this, cur, cur))
                {
                    // if we fail to CAS it means an older write has completed, and may have not fixed us up
                    // due to our write not being visible
                    volatileWrite = true;
                    continue;
                }
                return;
            }
            presentCountUpdater.compareAndSet(this, cur, cur + 1);
            volatileWrite = true;
        }
    }

    public boolean isEmpty()
    {
        return presentCount == 0;
    }

    /**
     * @return the size of guaranteed-to-be-visible portion of the list
     */
    public int size()
    {
        return presentCount;
    }

    public int capacity()
    {
        return values.length;
    }

    public Iterator<E> iterator()
    {
        return new Iterator<E>()
        {
            int p = 0;

            public boolean hasNext()
            {
                return p < presentCount;
            }

            public E next()
            {
                return (E) values[p++];
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    public E get(int i)
    {
        // we read presentCount to guarantee a volatile read of values
        if (i >= presentCount)
            throw new IndexOutOfBoundsException();
        return (E) values[i];
    }
}
