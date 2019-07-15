import org.apache.accumulo.tserver.metrics.*;


import javax.management.ObjectName;

import org.apache.accumulo.server.metrics.AbstractMetricsImpl;

public class TabletServerMinCMetrics extends AbstractMetricsImpl
    implements TabletServerMinCMetricsMBean {

  static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(TabletServerMinCMetrics.class);

  private static final String METRICS_PREFIX = "tserver.minc";

  private ObjectName OBJECT_NAME = null;

  TabletServerMinCMetrics() {
    super();
    reset();
    try {
      OBJECT_NAME = new ObjectName(
          "accumulo.server.metrics:service=TServerInfo,name=TabletServerMinCMetricsMBean,instance="
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
  public long getMinorCompactionMinTime() {
    return this.getMetricMin(MINC);
  }

  @Override
  public long getMinorCompactionAvgTime() {
    return this.getMetricAvg(MINC);
  }

  @Override
  public long getMinorCompactionCount() {
    return this.getMetricCount(MINC);
  }

  @Override
  public long getMinorCompactionMaxTime() {
    return this.getMetricMax(MINC);
  }

  @Override
  public long getMinorCompactionQueueAvgTime() {
    return this.getMetricAvg(QUEUE);
  }

  @Override
  public long getMinorCompactionQueueCount() {
    return this.getMetricCount(QUEUE);
  }

  @Override
  public long getMinorCompactionQueueMaxTime() {
    return this.getMetricMax(QUEUE);
  }

  @Override
  public long getMinorCompactionQueueMinTime() {
    return this.getMetricMin(MINC);
  }

  @Override
  public void reset() {
    createMetric("minc");
    createMetric("queue");
  }

}
