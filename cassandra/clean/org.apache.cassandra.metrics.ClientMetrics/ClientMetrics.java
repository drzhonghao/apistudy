import org.apache.cassandra.metrics.DefaultNameFactory;
import org.apache.cassandra.metrics.*;


import java.util.concurrent.Callable;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;


public class ClientMetrics
{
    private static final MetricNameFactory factory = new DefaultNameFactory("Client");
    
    public static final ClientMetrics instance = new ClientMetrics();
    
    private ClientMetrics()
    {
    }

    public void addCounter(String name, final Callable<Integer> provider)
    {
        Metrics.register(factory.createMetricName(name), (Gauge<Integer>) () -> {
            try
            {
                return provider.call();
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    public Meter addMeter(String name)
    {
        return Metrics.meter(factory.createMetricName(name));
    }
}
