

import java.util.Objects;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.server.metrics.Metrics;
import org.apache.accumulo.server.metrics.MetricsSystemHelper;
import org.apache.accumulo.tserver.TabletServer;
import org.apache.hadoop.metrics2.MetricsSystem;


public class TabletServerMetricsFactory {
	private final boolean useOldMetrics;

	private final MetricsSystem metricsSystem;

	public TabletServerMetricsFactory(AccumuloConfiguration conf) {
		Objects.requireNonNull(conf);
		useOldMetrics = conf.getBoolean(Property.GENERAL_LEGACY_METRICS);
		if (useOldMetrics) {
			metricsSystem = null;
		}else {
			metricsSystem = MetricsSystemHelper.getInstance();
		}
	}

	public Metrics createMincMetrics() {
		if (useOldMetrics) {
		}
		return null;
	}

	public Metrics createTabletServerMetrics(TabletServer tserver) {
		if (useOldMetrics) {
		}
		return null;
	}

	public Metrics createScanMetrics() {
		if (useOldMetrics) {
		}
		return null;
	}

	public Metrics createUpdateMetrics() {
		if (useOldMetrics) {
		}
		return null;
	}
}

