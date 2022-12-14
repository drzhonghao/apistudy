import org.apache.cassandra.concurrent.*;


import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.cassandra.metrics.ThreadPoolMetrics;

/**
 * This is a wrapper class for the <i>ScheduledThreadPoolExecutor</i>. It provides an implementation
 * for the <i>afterExecute()</i> found in the <i>ThreadPoolExecutor</i> class to log any unexpected
 * Runtime Exceptions.
 */

public class JMXEnabledThreadPoolExecutor extends DebuggableThreadPoolExecutor implements JMXEnabledThreadPoolExecutorMBean
{
    private final String mbeanName;
    public final ThreadPoolMetrics metrics;

    public JMXEnabledThreadPoolExecutor(String threadPoolName)
    {
        this(1, Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(threadPoolName), "internal");
    }

    public JMXEnabledThreadPoolExecutor(String threadPoolName, String jmxPath)
    {
        this(1, Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(threadPoolName), jmxPath);
    }

    public JMXEnabledThreadPoolExecutor(String threadPoolName, int priority)
    {
        this(1, Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory(threadPoolName, priority), "internal");
    }

    public JMXEnabledThreadPoolExecutor(NamedThreadFactory threadFactory, String jmxPath)
    {
        this(1, Integer.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory, jmxPath);
    }

    public JMXEnabledThreadPoolExecutor(int corePoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            NamedThreadFactory threadFactory,
            String jmxPath)
    {
        this(corePoolSize, corePoolSize, keepAliveTime, unit, workQueue, threadFactory, jmxPath);
    }

    public JMXEnabledThreadPoolExecutor(int corePoolSize,
                                        int maxPoolSize,
                                        long keepAliveTime,
                                        TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue,
                                        NamedThreadFactory threadFactory,
                                        String jmxPath)
    {
        super(corePoolSize, maxPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        super.prestartAllCoreThreads();
        metrics = new ThreadPoolMetrics(this, jmxPath, threadFactory.id);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        mbeanName = "org.apache.cassandra." + jmxPath + ":type=" + threadFactory.id;

        try
        {
            mbs.registerMBean(this, new ObjectName(mbeanName));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public JMXEnabledThreadPoolExecutor(Stage stage)
    {
        this(stage.getJmxName(), stage.getJmxType());
    }

    private void unregisterMBean()
    {
        try
        {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(mbeanName));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        // release metrics
        metrics.release();
    }

    @Override
    public synchronized void shutdown()
    {
        // synchronized, because there is no way to access super.mainLock, which would be
        // the preferred way to make this threadsafe
        if (!isShutdown())
        {
            unregisterMBean();
        }
        super.shutdown();
    }

    @Override
    public synchronized List<Runnable> shutdownNow()
    {
        // synchronized, because there is no way to access super.mainLock, which would be
        // the preferred way to make this threadsafe
        if (!isShutdown())
        {
            unregisterMBean();
        }
        return super.shutdownNow();
    }




    public int getTotalBlockedTasks()
    {
        return (int) metrics.totalBlocked.getCount();
    }

    public int getCurrentlyBlockedTasks()
    {
        return (int) metrics.currentBlocked.getCount();
    }

    public int getCoreThreads()
    {
        return getCorePoolSize();
    }

    public void setCoreThreads(int number)
    {
        setCorePoolSize(number);
    }

    public int getMaximumThreads()
    {
        return getMaximumPoolSize();
    }

    public void setMaximumThreads(int number)
    {
        setMaximumPoolSize(number);
    }

    @Override
    protected void onInitialRejection(Runnable task)
    {
        metrics.totalBlocked.inc();
        metrics.currentBlocked.inc();
    }

    @Override
    protected void onFinalAccept(Runnable task)
    {
        metrics.currentBlocked.dec();
    }

    @Override
    protected void onFinalRejection(Runnable task)
    {
        metrics.currentBlocked.dec();
    }
}
