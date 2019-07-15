import org.apache.accumulo.server.master.state.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.List;

import org.apache.accumulo.core.master.thrift.DeadServer;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadServerList {
  private static final Logger log = LoggerFactory.getLogger(DeadServerList.class);
  private final String path;

  public DeadServerList(String path) {
    this.path = path;
    IZooReaderWriter zoo = ZooReaderWriter.getInstance();
    try {
      zoo.mkdirs(path);
    } catch (Exception ex) {
      log.error("Unable to make parent directories of " + path, ex);
    }
  }

  public List<DeadServer> getList() {
    List<DeadServer> result = new ArrayList<>();
    IZooReaderWriter zoo = ZooReaderWriter.getInstance();
    try {
      List<String> children = zoo.getChildren(path);
      if (children != null) {
        for (String child : children) {
          Stat stat = new Stat();
          byte[] data;
          try {
            data = zoo.getData(path + "/" + child, stat);
          } catch (NoNodeException nne) {
            // Another thread or process can delete child while this loop is running.
            // We ignore this error since it's harmless if we miss the deleted server
            // in the dead server list.
            continue;
          }
          DeadServer server = new DeadServer(child, stat.getMtime(), new String(data, UTF_8));
          result.add(server);
        }
      }
    } catch (Exception ex) {
      log.error("{}", ex.getMessage(), ex);
    }
    return result;
  }

  public void delete(String server) {
    IZooReaderWriter zoo = ZooReaderWriter.getInstance();
    try {
      zoo.recursiveDelete(path + "/" + server, NodeMissingPolicy.SKIP);
    } catch (Exception ex) {
      log.error("delete failed with exception", ex);
    }
  }

  public void post(String server, String cause) {
    IZooReaderWriter zoo = ZooReaderWriter.getInstance();
    try {
      zoo.putPersistentData(path + "/" + server, cause.getBytes(UTF_8), NodeExistsPolicy.SKIP);
    } catch (Exception ex) {
      log.error("post failed with exception", ex);
    }
  }
}
