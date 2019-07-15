import org.apache.accumulo.tserver.metrics.*;


import javax.management.ObjectName;

import org.apache.accumulo.server.metrics.AbstractMetricsImpl;

public class TabletServerScanMetrics extends AbstractMetricsImpl
    implements TabletServerScanMetricsMBean {

  static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(TabletServerScanMetrics.class);

  public static final String METRICS_PREFIX = "tserver.scan";

  ObjectName OBJECT_NAME = null;

  TabletServerScanMetrics() {
    super();
    reset();
    try {
      OBJECT_NAME = new ObjectName(
          "accumulo.server.metrics:service=TServerInfo,name=TabletServerScanMetricsMBean,instance="
              + Thread.currentThread().getName());
    } catch (Exception e) {
      log.error("Exception setting MBean object name", e);
    }
  }

  @Override
  protected ObjectName getObjectName() {
    return OBJECT_NAME;
  }

  @Override
  protected String getMetricsPrefix() {
    return METRICS_PREFIX;
  }

  @Override
  public long getResultAvgSize() {
    return this.getMetricAvg(RESULT_SIZE);
  }

  @Override
  public long getResultCount() {
    return this.getMetricCount(RESULT_SIZE);
  }

  @Override
  public long getResultMaxSize() {
    return this.getMetricMax(RESULT_SIZE);
  }

  @Override
  public long getResultMinSize() {
    return this.getMetricMin(RESULT_SIZE);
  }

  @Override
  public long getScanAvgTime() {
    return this.getMetricAvg(SCAN);
  }

  @Override
  public long getScanCount() {
    return this.getMetricCount(SCAN);
  }

  @Override
  public long getScanMaxTime() {
    return this.getMetricMax(SCAN);
  }

  @Override
  public long getScanMinTime() {
    return this.getMetricMin(SCAN);
  }

  @Override
  public void reset() {
    createMetric(SCAN);
    createMetric(RESULT_SIZE);
  }

}
