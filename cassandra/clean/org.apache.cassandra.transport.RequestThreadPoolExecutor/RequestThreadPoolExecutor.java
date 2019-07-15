import org.apache.cassandra.transport.*;


import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.util.concurrent.AbstractEventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import org.apache.cassandra.concurrent.LocalAwareExecutorService;
import org.apache.cassandra.config.DatabaseDescriptor;

import static org.apache.cassandra.concurrent.SharedExecutorPool.SHARED;

public class RequestThreadPoolExecutor extends AbstractEventExecutor
{
    private final static int MAX_QUEUED_REQUESTS = Integer.getInteger("cassandra.max_queued_native_transport_requests", 128);
    private final static String THREAD_FACTORY_ID = "Native-Transport-Requests";
    private final LocalAwareExecutorService wrapped = SHARED.newExecutor(DatabaseDescriptor.getNativeTransportMaxThreads(),
                                                                           MAX_QUEUED_REQUESTS,
                                                                           "transport",
                                                                           THREAD_FACTORY_ID);

    public boolean isShuttingDown()
    {
        return wrapped.isShutdown();
    }

    public Future<?> shutdownGracefully(long l, long l2, TimeUnit timeUnit)
    {
        throw new IllegalStateException();
    }

    public Future<?> terminationFuture()
    {
        throw new IllegalStateException();
    }

    @Override
    public void shutdown()
    {
        wrapped.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow()
    {
        return wrapped.shutdownNow();
    }

    public boolean isShutdown()
    {
        return wrapped.isShutdown();
    }

    public boolean isTerminated()
    {
        return wrapped.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
    {
        return wrapped.awaitTermination(timeout, unit);
    }

    public EventExecutorGroup parent()
    {
        return null;
    }

    public boolean inEventLoop(Thread thread)
    {
        return false;
    }

    public void execute(Runnable command)
    {
        wrapped.execute(command);
    }
}
