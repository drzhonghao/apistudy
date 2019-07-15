

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.cassandra.metrics.CassandraMetricsRegistry;
import org.apache.cassandra.metrics.MetricNameFactory;


public class ThreadPoolMetrics {
	public final Gauge<Integer> activeTasks;

	public final Counter totalBlocked;

	public final Counter currentBlocked;

	public final Gauge<Long> completedTasks;

	public final Gauge<Long> pendingTasks;

	public final Gauge<Integer> maxPoolSize;

	private MetricNameFactory factory;

	public ThreadPoolMetrics(final ThreadPoolExecutor executor, String path, String poolName) {
		activeTasks = CassandraMetricsRegistry.Metrics.register(factory.createMetricName("ActiveTasks"), new Gauge<Integer>() {
			public Integer getValue() {
				return executor.getActiveCount();
			}
		});
		totalBlocked = CassandraMetricsRegistry.Metrics.counter(factory.createMetricName("TotalBlockedTasks"));
		currentBlocked = CassandraMetricsRegistry.Metrics.counter(factory.createMetricName("CurrentlyBlockedTasks"));
		completedTasks = CassandraMetricsRegistry.Metrics.register(factory.createMetricName("CompletedTasks"), new Gauge<Long>() {
			public Long getValue() {
				return executor.getCompletedTaskCount();
			}
		});
		pendingTasks = CassandraMetricsRegistry.Metrics.register(factory.createMetricName("PendingTasks"), new Gauge<Long>() {
			public Long getValue() {
				return (executor.getTaskCount()) - (executor.getCompletedTaskCount());
			}
		});
		maxPoolSize = CassandraMetricsRegistry.Metrics.register(factory.createMetricName("MaxPoolSize"), new Gauge<Integer>() {
			public Integer getValue() {
				return executor.getMaximumPoolSize();
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

	public static Object getJmxMetric(MBeanServerConnection mbeanServerConn, String jmxPath, String poolName, String metricName) {
		String name = String.format("org.apache.cassandra.metrics:type=ThreadPools,path=%s,scope=%s,name=%s", jmxPath, poolName, metricName);
		try {
			ObjectName oName = new ObjectName(name);
			if (!(mbeanServerConn.isRegistered(oName))) {
				return "N/A";
			}
			switch (metricName) {
				case "ActiveTasks" :
				case "PendingTasks" :
				case "CompletedTasks" :
					return JMX.newMBeanProxy(mbeanServerConn, oName, JmxReporter.JmxGaugeMBean.class).getValue();
				case "TotalBlockedTasks" :
				case "CurrentlyBlockedTasks" :
					return JMX.newMBeanProxy(mbeanServerConn, oName, JmxReporter.JmxCounterMBean.class).getCount();
				default :
					throw new AssertionError(("Unknown metric name " + metricName));
			}
		} catch (Exception e) {
			throw new RuntimeException(("Error reading: " + name), e);
		}
	}

	public static Multimap<String, String> getJmxThreadPools(MBeanServerConnection mbeanServerConn) {
		try {
			Multimap<String, String> threadPools = HashMultimap.create();
			Set<ObjectName> threadPoolObjectNames = mbeanServerConn.queryNames(new ObjectName("org.apache.cassandra.metrics:type=ThreadPools,*"), null);
			for (ObjectName oName : threadPoolObjectNames) {
				threadPools.put(oName.getKeyProperty("path"), oName.getKeyProperty("scope"));
			}
			return threadPools;
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException("Bad query to JMX server: ", e);
		} catch (IOException e) {
			throw new RuntimeException("Error getting threadpool names from JMX", e);
		}
	}
}

