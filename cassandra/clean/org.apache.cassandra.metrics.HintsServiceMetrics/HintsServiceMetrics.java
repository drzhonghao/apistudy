import org.apache.cassandra.metrics.MetricNameFactory;
import org.apache.cassandra.metrics.*;


import com.codahale.metrics.Meter;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;

/**
 * Metrics for {@link org.apache.cassandra.hints.HintsService}.
 */
public final class HintsServiceMetrics
{
    private static final MetricNameFactory factory = new DefaultNameFactory("HintsService");

    public static final Meter hintsSucceeded = Metrics.meter(factory.createMetricName("HintsSucceeded"));
    public static final Meter hintsFailed    = Metrics.meter(factory.createMetricName("HintsFailed"));
    public static final Meter hintsTimedOut  = Metrics.meter(factory.createMetricName("HintsTimedOut"));
}
