

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import org.apache.cassandra.concurrent.SEPExecutor;
import org.apache.cassandra.metrics.CassandraMetricsRegistry;
import org.apache.cassandra.metrics.MetricNameFactory;


public class SEPMetrics {
	public final Gauge<Integer> activeTasks;

	public final Counter totalBlocked;

	public final Counter currentBlocked;

	public final Gauge<Long> completedTasks;

	public final Gauge<Long> pendingTasks;

	public final Gauge<Integer> maxPoolSize;

	private MetricNameFactory factory;

	public SEPMetrics(final SEPExecutor executor, String path, String poolName) {
		activeTasks = CassandraMetricsRegistry.Metrics.register(factory.createMetricName("ActiveTasks"), new Gauge<Integer>() {
			public Integer getValue() {
				return executor.getActiveCount();
			}
		});
		pendingTasks = CassandraMetricsRegistry.Metrics.register(factory.createMetricName("PendingTasks"), new Gauge<Long>() {
			public Long getValue() {
				return executor.getPendingTasks();
			}
		});
		totalBlocked = CassandraMetricsRegistry.Metrics.counter(factory.createMetricName("TotalBlockedTasks"));
		currentBlocked = CassandraMetricsRegistry.Metrics.counter(factory.createMetricName("CurrentlyBlockedTasks"));
		completedTasks = CassandraMetricsRegistry.Metrics.register(factory.createMetricName("CompletedTasks"), new Gauge<Long>() {
			public Long getValue() {
				return executor.getCompletedTasks();
			}
		});
		maxPoolSize = CassandraMetricsRegistry.Metrics.register(factory.createMetricName("MaxPoolSize"), new Gauge<Integer>() {
			public Integer getValue() {
				return executor.maxWorkers;
			}
		});
	}

	public void release() {
		CassandraMetricsRegistry.Metrics.remove(factory.createMetricName("ActiveTasks"));
		CassandraMetricsRegistry.Metrics.remove(factory.createMetricName("PendingTasks"));
		CassandraMetricsRegistry.Metrics.remove(factory.createMetricName("CompletedTasks"));
		CassandraMetricsRegistry.Metrics.remove(factory.createMetricName("TotalBlockedTasks"));
		CassandraMetricsRegistry.Metrics.remove(factory.createMetricName("CurrentlyBlockedTasks"));
		CassandraMetricsRegistry.Metrics.remove(factory.createMetricName("MaxPoolSize"));
	}
}

