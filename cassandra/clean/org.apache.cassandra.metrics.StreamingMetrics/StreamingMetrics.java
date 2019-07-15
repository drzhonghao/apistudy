import org.apache.cassandra.metrics.DefaultNameFactory;
import org.apache.cassandra.metrics.*;


import java.net.InetAddress;
import java.util.concurrent.ConcurrentMap;


import com.codahale.metrics.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMap;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;

/**
 * Metrics for streaming.
 */
public class StreamingMetrics
{
    public static final String TYPE_NAME = "Streaming";

    private static final ConcurrentMap<InetAddress, StreamingMetrics> instances = new NonBlockingHashMap<InetAddress, StreamingMetrics>();

    public static final Counter activeStreamsOutbound = Metrics.counter(DefaultNameFactory.createMetricName(TYPE_NAME, "ActiveOutboundStreams", null));
    public static final Counter totalIncomingBytes = Metrics.counter(DefaultNameFactory.createMetricName(TYPE_NAME, "TotalIncomingBytes", null));
    public static final Counter totalOutgoingBytes = Metrics.counter(DefaultNameFactory.createMetricName(TYPE_NAME, "TotalOutgoingBytes", null));
    public final Counter incomingBytes;
    public final Counter outgoingBytes;

    public static StreamingMetrics get(InetAddress ip)
    {
       StreamingMetrics metrics = instances.get(ip);
       if (metrics == null)
       {
           metrics = new StreamingMetrics(ip);
           instances.put(ip, metrics);
       }
       return metrics;
    }

    public StreamingMetrics(final InetAddress peer)
    {
        MetricNameFactory factory = new DefaultNameFactory("Streaming", peer.getHostAddress().replace(':', '.'));
        incomingBytes = Metrics.counter(factory.createMetricName("IncomingBytes"));
        outgoingBytes= Metrics.counter(factory.createMetricName("OutgoingBytes"));
    }
}
