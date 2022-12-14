import org.apache.accumulo.server.util.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

public class CleanZookeeper {

  private static final Logger log = LoggerFactory.getLogger(CleanZookeeper.class);

  static class Opts extends Help {
    @Parameter(names = {"--password"},
        description = "The system secret, if different than instance.secret in accumulo-site.xml",
        password = true)
    String auth;
  }

  /**
   * @param args
   *          must contain one element: the address of a zookeeper node a second parameter provides
   *          an additional authentication value
   * @throws IOException
   *           error connecting to accumulo or zookeeper
   */
  public static void main(String[] args) throws IOException {
    Opts opts = new Opts();
    opts.parseArgs(CleanZookeeper.class.getName(), args);

    String root = Constants.ZROOT;
    IZooReaderWriter zk = ZooReaderWriter.getInstance();
    if (opts.auth != null) {
      zk.getZooKeeper().addAuthInfo("digest", ("accumulo:" + opts.auth).getBytes(UTF_8));
    }

    try {
      for (String child : zk.getChildren(root)) {
        if (Constants.ZINSTANCES.equals("/" + child)) {
          for (String instanceName : zk.getChildren(root + Constants.ZINSTANCES)) {
            String instanceNamePath = root + Constants.ZINSTANCES + "/" + instanceName;
            byte[] id = zk.getData(instanceNamePath, null);
            if (id != null
                && !new String(id, UTF_8).equals(HdfsZooInstance.getInstance().getInstanceID())) {
              try {
                zk.recursiveDelete(instanceNamePath, NodeMissingPolicy.SKIP);
              } catch (KeeperException.NoAuthException ex) {
                log.warn("Unable to delete " + instanceNamePath);
              }
            }
          }
        } else if (!child.equals(HdfsZooInstance.getInstance().getInstanceID())) {
          String path = root + "/" + child;
          try {
            zk.recursiveDelete(path, NodeMissingPolicy.SKIP);
          } catch (KeeperException.NoAuthException ex) {
            log.warn("Unable to delete " + path);
          }
        }
      }
    } catch (Exception ex) {
      System.out.println("Error Occurred: " + ex);
    }
  }

}
