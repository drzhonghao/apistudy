import org.apache.accumulo.server.metrics.AbstractMetricsImpl;
import org.apache.accumulo.server.metrics.ThriftMetricsMBean;
import org.apache.accumulo.server.metrics.*;


import javax.management.ObjectName;

public class ThriftMetrics extends AbstractMetricsImpl implements ThriftMetricsMBean {

  static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ThriftMetrics.class);

  private static final String METRICS_PREFIX = "thrift";

  private ObjectName OBJECT_NAME = null;

  ThriftMetrics(String serverName, String threadName) {
    super();
    reset();
    try {
      OBJECT_NAME = new ObjectName("accumulo.server.metrics:service=" + serverName
          + ",name=ThriftMetricsMBean,instance=" + threadName);
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
  public void reset() {
    createMetric(idle);
    createMetric(execute);
  }

  @Override
  public long getExecutionAvgTime() {
    return this.getMetricAvg(execute);
  }

  @Override
  public long getExecutionCount() {
    return this.getMetricCount(execute);
  }

  @Override
  public long getExecutionMaxTime() {
    return this.getMetricMax(execute);
  }

  @Override
  public long getExecutionMinTime() {
    return this.getMetricMin(execute);
  }

  @Override
  public long getIdleAvgTime() {
    return this.getMetricAvg(idle);
  }

  @Override
  public long getIdleCount() {
    return this.getMetricCount(idle);
  }

  @Override
  public long getIdleMaxTime() {
    return this.getMetricMax(idle);
  }

  @Override
  public long getIdleMinTime() {
    return this.getMetricMin(idle);
  }

}
