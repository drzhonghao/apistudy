import org.apache.cassandra.utils.concurrent.WaitQueue;
import org.apache.cassandra.utils.concurrent.*;


import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Condition;

// fulfils the Condition interface without spurious wakeup problems
// (or lost notify problems either: that is, even if you call await()
// _after_ signal(), it will work as desired.)
public class SimpleCondition implements Condition
{
    private static final AtomicReferenceFieldUpdater<SimpleCondition, WaitQueue> waitingUpdater = AtomicReferenceFieldUpdater.newUpdater(SimpleCondition.class, WaitQueue.class, "waiting");

    private volatile WaitQueue waiting;
    private volatile boolean signaled = false;

    public void await() throws InterruptedException
    {
        if (isSignaled())
            return;
        if (waiting == null)
            waitingUpdater.compareAndSet(this, null, new WaitQueue());
        WaitQueue.Signal s = waiting.register();
        if (isSignaled())
            s.cancel();
        else
            s.await();
        assert isSignaled();
    }

    public boolean await(long time, TimeUnit unit) throws InterruptedException
    {
        if (isSignaled())
            return true;
        long start = System.nanoTime();
        long until = start + unit.toNanos(time);
        if (waiting == null)
            waitingUpdater.compareAndSet(this, null, new WaitQueue());
        WaitQueue.Signal s = waiting.register();
        if (isSignaled())
        {
            s.cancel();
            return true;
        }
        return s.awaitUntil(until) || isSignaled();
    }

    public void signal()
    {
        throw new UnsupportedOperationException();
    }

    public boolean isSignaled()
    {
        return signaled;
    }

    public void signalAll()
    {
        signaled = true;
        if (waiting != null)
            waiting.signalAll();
    }

    public void awaitUninterruptibly()
    {
        throw new UnsupportedOperationException();
    }

    public long awaitNanos(long nanosTimeout) throws InterruptedException
    {
        throw new UnsupportedOperationException();
    }

    public boolean awaitUntil(Date deadline) throws InterruptedException
    {
        throw new UnsupportedOperationException();
    }
}
