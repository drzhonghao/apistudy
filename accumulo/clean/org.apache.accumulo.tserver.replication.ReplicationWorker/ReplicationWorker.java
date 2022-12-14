import org.apache.accumulo.tserver.replication.*;


import java.util.concurrent.ThreadPoolExecutor;

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.replication.ReplicationConstants;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.zookeeper.DistributedWorkQueue;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches the {@link ReplicationProcessor}
 */
public class ReplicationWorker implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(ReplicationWorker.class);

  private ClientContext context;
  private Instance inst;
  private AccumuloConfiguration conf;
  private VolumeManager fs;
  private ThreadPoolExecutor executor;

  public ReplicationWorker(ClientContext clientCtx, VolumeManager fs) {
    this.context = clientCtx;
    this.inst = clientCtx.getInstance();
    this.fs = fs;
    this.conf = clientCtx.getConfiguration();
  }

  public void setExecutor(ThreadPoolExecutor executor) {
    this.executor = executor;
  }

  @Override
  public void run() {
    DefaultConfiguration defaultConf = DefaultConfiguration.getDefaultConfiguration();
    long defaultDelay = defaultConf.getTimeInMillis(Property.REPLICATION_WORK_PROCESSOR_DELAY);
    long defaultPeriod = defaultConf.getTimeInMillis(Property.REPLICATION_WORK_PROCESSOR_PERIOD);
    long delay = conf.getTimeInMillis(Property.REPLICATION_WORK_PROCESSOR_DELAY);
    long period = conf.getTimeInMillis(Property.REPLICATION_WORK_PROCESSOR_PERIOD);
    try {
      DistributedWorkQueue workQueue;
      if (defaultDelay != delay && defaultPeriod != period) {
        log.debug("Configuration DistributedWorkQueue with delay and period of {} and {}", delay,
            period);
        workQueue = new DistributedWorkQueue(
            ZooUtil.getRoot(inst) + ReplicationConstants.ZOO_WORK_QUEUE, conf, delay, period);
      } else {
        log.debug("Configuring DistributedWorkQueue with default delay and period");
        workQueue = new DistributedWorkQueue(
            ZooUtil.getRoot(inst) + ReplicationConstants.ZOO_WORK_QUEUE, conf);
      }

      workQueue.startProcessing(new ReplicationProcessor(context, conf, fs), executor);
    } catch (KeeperException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
