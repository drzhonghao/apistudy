import org.apache.accumulo.tserver.metrics.*;


import org.apache.accumulo.server.metrics.Metrics;
import org.apache.accumulo.server.metrics.MetricsSystemHelper;
import org.apache.hadoop.metrics2.MetricsCollector;
import org.apache.hadoop.metrics2.MetricsRecordBuilder;
import org.apache.hadoop.metrics2.MetricsSource;
import org.apache.hadoop.metrics2.MetricsSystem;
import org.apache.hadoop.metrics2.impl.MsInfo;
import org.apache.hadoop.metrics2.lib.Interns;
import org.apache.hadoop.metrics2.lib.MetricsRegistry;
import org.apache.hadoop.metrics2.lib.MutableStat;

/**
 *
 */
public class Metrics2TabletServerMinCMetrics
    implements Metrics, MetricsSource, TabletServerMinCMetricsKeys {
  public static final String NAME = TSERVER_NAME + ",sub=MinorCompactions",
      DESCRIPTION = "TabletServer Minor Compaction Metrics", CONTEXT = "tserver",
      RECORD = "MinorCompactions";

  private final MetricsSystem system;
  private final MetricsRegistry registry;
  private final MutableStat activeMinc, queuedMinc;

  // Use TabletServerMetricsFactory
  Metrics2TabletServerMinCMetrics(MetricsSystem system) {
    this.system = system;
    this.registry = new MetricsRegistry(Interns.info(NAME, DESCRIPTION));
    this.registry.tag(MsInfo.ProcessName, MetricsSystemHelper.getProcessName());

    activeMinc = registry.newStat(MINC, "Minor compactions", "Ops", "Count", true);
    queuedMinc = registry.newStat(QUEUE, "Queued minor compactions", "Ops", "Count", true);
  }

  @Override
  public void add(String name, long value) {
    if (MINC.equals(name)) {
      activeMinc.add(value);
    } else if (QUEUE.equals(name)) {
      queuedMinc.add(value);
    }
  }

  @Override
  public void register() {
    system.register(NAME, DESCRIPTION, this);
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void getMetrics(MetricsCollector collector, boolean all) {
    MetricsRecordBuilder builder = collector.addRecord(RECORD).setContext(CONTEXT);

    registry.snapshot(builder, all);
  }

}
