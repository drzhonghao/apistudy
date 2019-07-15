import org.apache.cassandra.metrics.MetricNameFactory;
import org.apache.cassandra.metrics.*;


import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.RatioGauge;
import org.apache.cassandra.cache.ICache;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;

/**
 * Metrics for {@code ICache}.
 */
public class CacheMetrics
{
    /** Cache capacity in bytes */
    public final Gauge<Long> capacity;
    /** Total number of cache hits */
    public final Meter hits;
    /** Total number of cache requests */
    public final Meter requests;
    /** all time cache hit rate */
    public final Gauge<Double> hitRate;
    /** 1m hit rate */
    public final Gauge<Double> oneMinuteHitRate;
    /** 5m hit rate */
    public final Gauge<Double> fiveMinuteHitRate;
    /** 15m hit rate */
    public final Gauge<Double> fifteenMinuteHitRate;
    /** Total size of cache, in bytes */
    public final Gauge<Long> size;
    /** Total number of cache entries */
    public final Gauge<Integer> entries;

    /**
     * Create metrics for given cache.
     *
     * @param type Type of Cache to identify metrics.
     * @param cache Cache to measure metrics
     */
    public CacheMetrics(String type, final ICache<?, ?> cache)
    {
        MetricNameFactory factory = new DefaultNameFactory("Cache", type);

        capacity = Metrics.register(factory.createMetricName("Capacity"), new Gauge<Long>()
        {
            public Long getValue()
            {
                return cache.capacity();
            }
        });
        hits = Metrics.meter(factory.createMetricName("Hits"));
        requests = Metrics.meter(factory.createMetricName("Requests"));
        hitRate = Metrics.register(factory.createMetricName("HitRate"), new RatioGauge()
        {
            @Override
            public Ratio getRatio()
            {
                return Ratio.of(hits.getCount(), requests.getCount());
            }
        });
        oneMinuteHitRate = Metrics.register(factory.createMetricName("OneMinuteHitRate"), new RatioGauge()
        {
            protected Ratio getRatio()
            {
                return Ratio.of(hits.getOneMinuteRate(), requests.getOneMinuteRate());
            }
        });
        fiveMinuteHitRate = Metrics.register(factory.createMetricName("FiveMinuteHitRate"), new RatioGauge()
        {
            protected Ratio getRatio()
            {
                return Ratio.of(hits.getFiveMinuteRate(), requests.getFiveMinuteRate());
            }
        });
        fifteenMinuteHitRate = Metrics.register(factory.createMetricName("FifteenMinuteHitRate"), new RatioGauge()
        {
            protected Ratio getRatio()
            {
                return Ratio.of(hits.getFifteenMinuteRate(), requests.getFifteenMinuteRate());
            }
        });
        size = Metrics.register(factory.createMetricName("Size"), new Gauge<Long>()
        {
            public Long getValue()
            {
                return cache.weightedSize();
            }
        });
        entries = Metrics.register(factory.createMetricName("Entries"), new Gauge<Integer>()
        {
            public Integer getValue()
            {
                return cache.size();
            }
        });
    }
}
