import org.apache.accumulo.server.metrics.*;


import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.metrics2.MetricsSystem;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
import org.apache.hadoop.metrics2.source.JvmMetrics;
import org.apache.hadoop.metrics2.source.JvmMetricsInfo;

/**
 *
 */
public class MetricsSystemHelper {

  private static String processName = "Unknown";

  public static void configure(String serviceName) {
    MetricsSystemHelper.processName = serviceName;
    String serviceInstance = System.getProperty("accumulo.metrics.service.instance", "");
    if (StringUtils.isNotBlank(serviceInstance)) {
      MetricsSystemHelper.processName += serviceInstance;
    }
  }

  public static String getProcessName() {
    return MetricsSystemHelper.processName;
  }

  private static class MetricsSystemHolder {
    // Singleton, rely on JVM to initialize the MetricsSystem only when it is accessed.
    private static final MetricsSystem metricsSystem = DefaultMetricsSystem
        .initialize(Metrics.PREFIX);
  }

  public static MetricsSystem getInstance() {
    if (MetricsSystemHolder.metricsSystem.getSource(JvmMetricsInfo.JvmMetrics.name()) == null) {
      JvmMetrics.create(getProcessName(), "", MetricsSystemHolder.metricsSystem);
    }
    return MetricsSystemHolder.metricsSystem;
  }

}
