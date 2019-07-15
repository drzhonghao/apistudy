import org.apache.cassandra.metrics.MetricNameFactory;
import org.apache.cassandra.metrics.*;


import com.codahale.metrics.Counter;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;

/**
 * Metrics related to Storage.
 */
public class StorageMetrics
{
    private static final MetricNameFactory factory = new DefaultNameFactory("Storage");

    public static final Counter load = Metrics.counter(factory.createMetricName("Load"));
    public static final Counter exceptions = Metrics.counter(factory.createMetricName("Exceptions"));
    public static final Counter totalHintsInProgress  = Metrics.counter(factory.createMetricName("TotalHintsInProgress"));
    public static final Counter totalHints = Metrics.counter(factory.createMetricName("TotalHints"));
}
