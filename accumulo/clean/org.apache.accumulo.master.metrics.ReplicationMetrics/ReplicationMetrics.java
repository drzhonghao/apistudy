import org.apache.accumulo.master.metrics.*;


import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.replication.ReplicationTable;
import org.apache.accumulo.core.replication.ReplicationTarget;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.server.metrics.AbstractMetricsImpl;
import org.apache.accumulo.server.replication.ReplicationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMX bindings to expose 'high-level' metrics about Replication
 */
public class ReplicationMetrics extends AbstractMetricsImpl implements ReplicationMetricsMBean {
  private static final Logger log = LoggerFactory.getLogger(ReplicationMetrics.class);
  private static final String METRICS_PREFIX = "replication";

  private Master master;
  private ObjectName objectName = null;
  private ReplicationUtil replicationUtil;

  ReplicationMetrics(Master master) {
    super();
    this.master = master;
    try {
      objectName = new ObjectName(
          "accumulo.server.metrics:service=Replication Metrics,name=ReplicationMBean,instance="
              + Thread.currentThread().getName());
    } catch (Exception e) {
      log.error("Exception setting MBean object name", e);
    }
    replicationUtil = new ReplicationUtil(master);
  }

  @Override
  public int getNumFilesPendingReplication() {

    if (TableState.ONLINE != Tables.getTableState(master.getInstance(), ReplicationTable.ID)) {
      return 0;
    }

    // Get all of the configured replication peers
    Map<String,String> peers = replicationUtil.getPeers();

    // A quick lookup to see if have any replication peer configured
    if (peers.isEmpty()) {
      return 0;
    }

    // The total set of configured targets
    Set<ReplicationTarget> allConfiguredTargets = replicationUtil.getReplicationTargets();

    // Number of files per target we have to replicate
    Map<ReplicationTarget,Long> targetCounts = replicationUtil.getPendingReplications();

    int filesPending = 0;

    // Sum pending replication over all targets
    for (ReplicationTarget configuredTarget : allConfiguredTargets) {
      Long numFiles = targetCounts.get(configuredTarget);

      if (null != numFiles) {
        filesPending += numFiles;
      }
    }

    return filesPending;
  }

  @Override
  public int getNumConfiguredPeers() {
    return replicationUtil.getPeers().size();
  }

  @Override
  public int getMaxReplicationThreads() {
    return replicationUtil.getMaxReplicationThreads(master.getMasterMonitorInfo());
  }

  @Override
  protected ObjectName getObjectName() {
    return objectName;
  }

  @Override
  protected String getMetricsPrefix() {
    return METRICS_PREFIX;
  }

}
