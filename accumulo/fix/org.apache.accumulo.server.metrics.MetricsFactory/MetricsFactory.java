

import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.server.metrics.Metrics;
import org.apache.accumulo.server.metrics.MetricsSystemHelper;
import org.apache.hadoop.metrics2.MetricsSystem;


public class MetricsFactory {
	private final boolean useOldMetrics;

	private final MetricsSystem metricsSystem;

	public MetricsFactory(AccumuloConfiguration conf) {
		this(conf.getBoolean(Property.GENERAL_LEGACY_METRICS));
	}

	public MetricsFactory(boolean useOldMetrics) {
		this.useOldMetrics = useOldMetrics;
		if (useOldMetrics) {
			metricsSystem = null;
		}else {
			metricsSystem = MetricsSystemHelper.getInstance();
		}
	}

	public Metrics createThriftMetrics(String serverName, String threadName) {
		if (useOldMetrics) {
		}
		return null;
	}
}

