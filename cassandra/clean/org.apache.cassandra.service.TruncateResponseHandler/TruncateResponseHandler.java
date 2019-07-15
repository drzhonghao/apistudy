import org.apache.cassandra.service.*;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.net.IAsyncCallback;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.utils.concurrent.SimpleCondition;

public class TruncateResponseHandler implements IAsyncCallback
{
    protected static final Logger logger = LoggerFactory.getLogger(TruncateResponseHandler.class);
    protected final SimpleCondition condition = new SimpleCondition();
    private final int responseCount;
    protected final AtomicInteger responses = new AtomicInteger(0);
    private final long start;

    public TruncateResponseHandler(int responseCount)
    {
        // at most one node per range can bootstrap at a time, and these will be added to the write until
        // bootstrap finishes (at which point we no longer need to write to the old ones).
        assert 1 <= responseCount: "invalid response count " + responseCount;

        this.responseCount = responseCount;
        start = System.nanoTime();
    }

    public void get() throws TimeoutException
    {
        long timeout = TimeUnit.MILLISECONDS.toNanos(DatabaseDescriptor.getTruncateRpcTimeout()) - (System.nanoTime() - start);
        boolean success;
        try
        {
            success = condition.await(timeout, TimeUnit.NANOSECONDS); // TODO truncate needs a much longer timeout
        }
        catch (InterruptedException ex)
        {
            throw new AssertionError(ex);
        }

        if (!success)
        {
            throw new TimeoutException("Truncate timed out - received only " + responses.get() + " responses");
        }
    }

    public void response(MessageIn message)
    {
        responses.incrementAndGet();
        if (responses.get() >= responseCount)
            condition.signalAll();
    }

    public boolean isLatencyForSnitch()
    {
        return false;
    }
}
