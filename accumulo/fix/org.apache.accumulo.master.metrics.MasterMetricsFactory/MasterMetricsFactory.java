

import java.util.Objects;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.server.metrics.Metrics;
import org.apache.accumulo.server.metrics.MetricsSystemHelper;
import org.apache.hadoop.metrics2.MetricsSystem;


public class MasterMetricsFactory {
	private final boolean useOldMetrics;

	private final MetricsSystem metricsSystem;

	private final Master master;

	public MasterMetricsFactory(AccumuloConfiguration conf, Master master) {
		Objects.requireNonNull(conf);
		useOldMetrics = conf.getBoolean(Property.GENERAL_LEGACY_METRICS);
		this.master = master;
		if (useOldMetrics) {
			metricsSystem = null;
		}else {
			metricsSystem = MetricsSystemHelper.getInstance();
		}
	}

	public Metrics createReplicationMetrics() {
		if (useOldMetrics) {
		}
		return null;
	}
}

