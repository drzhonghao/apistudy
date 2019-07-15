import org.apache.cassandra.metrics.MetricNameFactory;
import org.apache.cassandra.metrics.*;


import com.codahale.metrics.Meter;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;

/**
 * Metrics related to Read Repair.
 */
public class ReadRepairMetrics
{
    private static final MetricNameFactory factory = new DefaultNameFactory("ReadRepair");

    public static final Meter repairedBlocking = Metrics.meter(factory.createMetricName("RepairedBlocking"));
    public static final Meter repairedBackground = Metrics.meter(factory.createMetricName("RepairedBackground"));
    public static final Meter attempted = Metrics.meter(factory.createMetricName("Attempted"));
}
