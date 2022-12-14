import org.apache.accumulo.server.util.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.List;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.ZooCache;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.zookeeper.ZooLock;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;

import com.beust.jcommander.Parameter;

public class TabletServerLocks {

  static class Opts extends Help {
    @Parameter(names = "-list")
    boolean list = false;
    @Parameter(names = "-delete")
    String delete = null;
  }

  public static void main(String[] args) throws Exception {

    Instance instance = HdfsZooInstance.getInstance();
    String tserverPath = ZooUtil.getRoot(instance) + Constants.ZTSERVERS;
    Opts opts = new Opts();
    opts.parseArgs(TabletServerLocks.class.getName(), args);

    ZooCache cache = new ZooCache(instance.getZooKeepers(), instance.getZooKeepersSessionTimeOut());

    if (opts.list) {
      IZooReaderWriter zoo = ZooReaderWriter.getInstance();

      List<String> tabletServers = zoo.getChildren(tserverPath);

      for (String tabletServer : tabletServers) {
        byte[] lockData = ZooLock.getLockData(cache, tserverPath + "/" + tabletServer, null);
        String holder = null;
        if (lockData != null) {
          holder = new String(lockData, UTF_8);
        }

        System.out.printf("%32s %16s%n", tabletServer, holder);
      }
    } else if (opts.delete != null) {
      ZooLock.deleteLock(tserverPath + "/" + args[1]);
    } else {
      System.out.println(
          "Usage : " + TabletServerLocks.class.getName() + " -list|-delete <tserver lock>");
    }

  }

}
