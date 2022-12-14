import org.apache.accumulo.server.util.*;


import java.util.List;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy;
import org.apache.accumulo.server.cli.ClientOpts;
import org.apache.accumulo.server.security.SecurityUtil;
import org.apache.accumulo.server.zookeeper.ZooLock;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class ZooZap {
  private static final Logger log = LoggerFactory.getLogger(ZooZap.class);

  private static void message(String msg, Opts opts) {
    if (opts.verbose)
      System.out.println(msg);
  }

  static class Opts extends ClientOpts {
    @Parameter(names = "-master", description = "remove master locks")
    boolean zapMaster = false;
    @Parameter(names = "-tservers", description = "remove tablet server locks")
    boolean zapTservers = false;
    @Parameter(names = "-tracers", description = "remove tracer locks")
    boolean zapTracers = false;
    @Parameter(names = "-verbose", description = "print out messages about progress")
    boolean verbose = false;

    String getTraceZKPath() {
      return super.getClientConfiguration().get(ClientProperty.TRACE_ZK_PATH);
    }
  }

  public static void main(String[] args) {
    Opts opts = new Opts();
    opts.parseArgs(ZooZap.class.getName(), args);

    if (!opts.zapMaster && !opts.zapTservers && !opts.zapTracers) {
      new JCommander(opts).usage();
      return;
    }

    AccumuloConfiguration siteConf = SiteConfiguration.getInstance();
    // Login as the server on secure HDFS
    if (siteConf.getBoolean(Property.INSTANCE_RPC_SASL_ENABLED)) {
      SecurityUtil.serverLogin(siteConf);
    }

    String iid = opts.getInstance().getInstanceID();
    IZooReaderWriter zoo = ZooReaderWriter.getInstance();

    if (opts.zapMaster) {
      String masterLockPath = Constants.ZROOT + "/" + iid + Constants.ZMASTER_LOCK;

      try {
        zapDirectory(zoo, masterLockPath, opts);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (opts.zapTservers) {
      String tserversPath = Constants.ZROOT + "/" + iid + Constants.ZTSERVERS;
      try {
        List<String> children = zoo.getChildren(tserversPath);
        for (String child : children) {
          message("Deleting " + tserversPath + "/" + child + " from zookeeper", opts);

          if (opts.zapMaster)
            ZooReaderWriter.getInstance().recursiveDelete(tserversPath + "/" + child,
                NodeMissingPolicy.SKIP);
          else {
            String path = tserversPath + "/" + child;
            if (zoo.getChildren(path).size() > 0) {
              if (!ZooLock.deleteLock(path, "tserver")) {
                message("Did not delete " + tserversPath + "/" + child, opts);
              }
            }
          }
        }
      } catch (Exception e) {
        log.error("{}", e.getMessage(), e);
      }
    }

    if (opts.zapTracers) {
      String path = opts.getTraceZKPath();
      try {
        zapDirectory(zoo, path, opts);
      } catch (Exception e) {
        // do nothing if the /tracers node does not exist.
      }
    }

  }

  private static void zapDirectory(IZooReaderWriter zoo, String path, Opts opts)
      throws KeeperException, InterruptedException {
    List<String> children = zoo.getChildren(path);
    for (String child : children) {
      message("Deleting " + path + "/" + child + " from zookeeper", opts);
      zoo.recursiveDelete(path + "/" + child, NodeMissingPolicy.SKIP);
    }
  }
}
