import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.accumulo.server.zookeeper.*;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.IZooReader;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.ZooReader;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.zookeeper.KeeperException;

public class TransactionWatcher extends org.apache.accumulo.fate.zookeeper.TransactionWatcher {
  public static class ZooArbitrator implements Arbitrator {

    Instance instance = HdfsZooInstance.getInstance();
    ZooReader rdr = new ZooReader(instance.getZooKeepers(), instance.getZooKeepersSessionTimeOut());

    @Override
    public boolean transactionAlive(String type, long tid) throws Exception {
      String path = ZooUtil.getRoot(instance) + "/" + type + "/" + tid;
      rdr.sync(path);
      return rdr.exists(path);
    }

    public static void start(String type, long tid) throws KeeperException, InterruptedException {
      Instance instance = HdfsZooInstance.getInstance();
      IZooReaderWriter writer = ZooReaderWriter.getInstance();
      writer.putPersistentData(ZooUtil.getRoot(instance) + "/" + type, new byte[] {},
          NodeExistsPolicy.OVERWRITE);
      writer.putPersistentData(ZooUtil.getRoot(instance) + "/" + type + "/" + tid, new byte[] {},
          NodeExistsPolicy.OVERWRITE);
      writer.putPersistentData(ZooUtil.getRoot(instance) + "/" + type + "/" + tid + "-running",
          new byte[] {}, NodeExistsPolicy.OVERWRITE);
    }

    public static void stop(String type, long tid) throws KeeperException, InterruptedException {
      Instance instance = HdfsZooInstance.getInstance();
      IZooReaderWriter writer = ZooReaderWriter.getInstance();
      writer.recursiveDelete(ZooUtil.getRoot(instance) + "/" + type + "/" + tid,
          NodeMissingPolicy.SKIP);
    }

    public static void cleanup(String type, long tid) throws KeeperException, InterruptedException {
      Instance instance = HdfsZooInstance.getInstance();
      IZooReaderWriter writer = ZooReaderWriter.getInstance();
      writer.recursiveDelete(ZooUtil.getRoot(instance) + "/" + type + "/" + tid,
          NodeMissingPolicy.SKIP);
      writer.recursiveDelete(ZooUtil.getRoot(instance) + "/" + type + "/" + tid + "-running",
          NodeMissingPolicy.SKIP);
    }

    public static Set<Long> allTransactionsAlive(String type)
        throws KeeperException, InterruptedException {
      final Instance instance = HdfsZooInstance.getInstance();
      final IZooReader reader = ZooReaderWriter.getInstance();
      final Set<Long> result = new HashSet<>();
      final String parent = ZooUtil.getRoot(instance) + "/" + type;
      reader.sync(parent);
      List<String> children = reader.getChildren(parent);
      for (String child : children) {
        if (child.endsWith("-running")) {
          continue;
        }
        result.add(Long.parseLong(child));
      }
      return result;
    }

    @Override
    public boolean transactionComplete(String type, long tid) throws Exception {
      String path = ZooUtil.getRoot(instance) + "/" + type + "/" + tid + "-running";
      rdr.sync(path);
      return !rdr.exists(path);
    }
  }

  public TransactionWatcher() {
    super(new ZooArbitrator());
  }
}
