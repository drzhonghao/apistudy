import org.apache.cassandra.metrics.*;


import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;

/**
 * Metrics for messages
 */
public class MessagingMetrics
{
    private static Logger logger = LoggerFactory.getLogger(MessagingMetrics.class);
    private static final MetricNameFactory factory = new DefaultNameFactory("Messaging");
    public final Timer crossNodeLatency;
    public final ConcurrentHashMap<String, Timer> dcLatency;

    public MessagingMetrics()
    {
        crossNodeLatency = Metrics.timer(factory.createMetricName("CrossNodeLatency"));
        dcLatency = new ConcurrentHashMap<>();
    }

    public void addTimeTaken(InetAddress from, long timeTaken)
    {
        String dc = DatabaseDescriptor.getEndpointSnitch().getDatacenter(from);
        Timer timer = dcLatency.get(dc);
        if (timer == null)
        {
            timer = dcLatency.computeIfAbsent(dc, k -> Metrics.timer(factory.createMetricName(dc + "-Latency")));
        }
        timer.update(timeTaken, TimeUnit.MILLISECONDS);
        crossNodeLatency.update(timeTaken, TimeUnit.MILLISECONDS);
    }
}
