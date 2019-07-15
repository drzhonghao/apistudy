import org.apache.cassandra.metrics.MetricNameFactory;
import org.apache.cassandra.metrics.*;


import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.RatioGauge;
import org.apache.cassandra.cql3.QueryProcessor;

import static org.apache.cassandra.metrics.CassandraMetricsRegistry.Metrics;

public class CQLMetrics
{
    private static final MetricNameFactory factory = new DefaultNameFactory("CQL");

    public final Counter regularStatementsExecuted;
    public final Counter preparedStatementsExecuted;
    public final Counter preparedStatementsEvicted;

    public final Gauge<Integer> preparedStatementsCount;
    public final Gauge<Double> preparedStatementsRatio;

    public CQLMetrics()
    {
        regularStatementsExecuted = Metrics.counter(factory.createMetricName("RegularStatementsExecuted"));
        preparedStatementsExecuted = Metrics.counter(factory.createMetricName("PreparedStatementsExecuted"));
        preparedStatementsEvicted = Metrics.counter(factory.createMetricName("PreparedStatementsEvicted"));

        preparedStatementsCount = Metrics.register(factory.createMetricName("PreparedStatementsCount"), new Gauge<Integer>()
        {
            public Integer getValue()
            {
                return QueryProcessor.preparedStatementsCount();
            }
        });
        preparedStatementsRatio = Metrics.register(factory.createMetricName("PreparedStatementsRatio"), new RatioGauge()
        {
            public Ratio getRatio()
            {
                return Ratio.of(getNumerator(), getDenominator());
            }

            public double getNumerator()
            {
                return preparedStatementsExecuted.getCount();
            }

            public double getDenominator()
            {
                return regularStatementsExecuted.getCount() + preparedStatementsExecuted.getCount();
            }
        });
    }
}
