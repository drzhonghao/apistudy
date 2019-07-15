import org.apache.accumulo.server.metrics.ThriftMetricsKeys;
import org.apache.accumulo.server.metrics.MetricsSystemHelper;
import org.apache.accumulo.server.metrics.*;


import org.apache.hadoop.metrics2.MetricsCollector;
import org.apache.hadoop.metrics2.MetricsRecordBuilder;
import org.apache.hadoop.metrics2.MetricsSource;
import org.apache.hadoop.metrics2.MetricsSystem;
import org.apache.hadoop.metrics2.impl.MsInfo;
import org.apache.hadoop.metrics2.lib.Interns;
import org.apache.hadoop.metrics2.lib.MetricsRegistry;

/**
 *
 */
public class Metrics2ThriftMetrics implements Metrics, MetricsSource, ThriftMetricsKeys {
  public static final String CONTEXT = "thrift";

  private final MetricsSystem system;
  private final MetricsRegistry registry;
  private final String record, name, desc;

  Metrics2ThriftMetrics(MetricsSystem system, String serverName, String threadName) {
    this.system = system;
    this.record = serverName;
    this.name = THRIFT_NAME + ",sub=" + serverName;
    this.desc = "Thrift Server Metrics - " + serverName + " " + threadName;
    this.registry = new MetricsRegistry(Interns.info(name, desc));
    this.registry.tag(MsInfo.ProcessName, MetricsSystemHelper.getProcessName());
  }

  @Override
  public void add(String name, long time) {
    registry.add(name, time);
  }

  @Override
  public void register() {
    system.register(name, desc, this);
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void getMetrics(MetricsCollector collector, boolean all) {
    MetricsRecordBuilder builder = collector.addRecord(record).setContext(CONTEXT);

    registry.snapshot(builder, all);
  }
}
