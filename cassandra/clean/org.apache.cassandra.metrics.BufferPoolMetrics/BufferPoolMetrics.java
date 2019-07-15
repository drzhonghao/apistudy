import org.apache.cassandra.metrics.MetricNameFactory;
import org.apache.cassandra.metrics.*;


import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import org.apache.cassandra.utils.memory.BufferPool;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;

public class BufferPoolMetrics
{
    private static final MetricNameFactory factory = new DefaultNameFactory("BufferPool");

    /** Total number of misses */
    public final Meter misses;

    /** Total size of buffer pools, in bytes */
    public final Gauge<Long> size;

    public BufferPoolMetrics()
    {
        misses = Metrics.meter(factory.createMetricName("Misses"));

        size = Metrics.register(factory.createMetricName("Size"), new Gauge<Long>()
        {
            public Long getValue()
            {
                return BufferPool.sizeInBytes();
            }
        });
    }
}
