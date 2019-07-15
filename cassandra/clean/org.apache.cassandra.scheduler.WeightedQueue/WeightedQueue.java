import org.apache.cassandra.scheduler.*;


import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

import org.apache.cassandra.metrics.LatencyMetrics;

class WeightedQueue
{
    private final LatencyMetrics metric;

    public final String key;
    public final int weight;
    private final SynchronousQueue<Entry> queue;
    public WeightedQueue(String key, int weight)
    {
        this.key = key;
        this.weight = weight;
        this.queue = new SynchronousQueue<Entry>(true);
        this.metric =  new LatencyMetrics("scheduler", "WeightedQueue", key);
    }

    public void put(Thread t, long timeoutMS) throws InterruptedException, TimeoutException
    {
        if (!queue.offer(new WeightedQueue.Entry(t), timeoutMS, TimeUnit.MILLISECONDS))
            throw new TimeoutException("Failed to acquire request scheduler slot for '" + key + "'");
    }

    public Thread poll()
    {
        Entry e = queue.poll();
        if (e == null)
            return null;
        metric.addNano(System.nanoTime() - e.creationTime);
        return e.thread;
    }

    @Override
    public String toString()
    {
        return "RoundRobinScheduler.WeightedQueue(key=" + key + " weight=" + weight + ")";
    }

    private final static class Entry
    {
        public final long creationTime = System.nanoTime();
        public final Thread thread;
        public Entry(Thread thread)
        {
            this.thread = thread;
        }
    }
}
