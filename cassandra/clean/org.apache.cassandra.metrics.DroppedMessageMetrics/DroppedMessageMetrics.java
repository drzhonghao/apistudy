import org.apache.cassandra.metrics.MetricNameFactory;
import org.apache.cassandra.metrics.*;


import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import org.apache.cassandra.net.MessagingService;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;

/**
 * Metrics for dropped messages by verb.
 */
public class DroppedMessageMetrics
{
    /** Number of dropped messages */
    public final Meter dropped;

    /** The dropped latency within node */
    public final Timer internalDroppedLatency;

    /** The cross node dropped latency */
    public final Timer crossNodeDroppedLatency;

    public DroppedMessageMetrics(MessagingService.Verb verb)
    {
        this(new DefaultNameFactory("DroppedMessage", verb.toString()));
    }

    public DroppedMessageMetrics(MetricNameFactory factory)
    {
        dropped = Metrics.meter(factory.createMetricName("Dropped"));
        internalDroppedLatency = Metrics.timer(factory.createMetricName("InternalDroppedLatency"));
        crossNodeDroppedLatency = Metrics.timer(factory.createMetricName("CrossNodeDroppedLatency"));
    }
}
