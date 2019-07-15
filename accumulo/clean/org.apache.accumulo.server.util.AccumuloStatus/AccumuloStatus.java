import org.apache.accumulo.server.util.*;


import java.io.IOException;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.fate.zookeeper.IZooReader;
import org.apache.zookeeper.KeeperException;

public class AccumuloStatus {
  /**
   * Determines if there could be an accumulo instance running via zookeeper lock checking
   *
   * @return true iff all servers show no indication of being registered in zookeeper, otherwise
   *         false
   * @throws IOException
   *           if there are issues connecting to ZooKeeper to determine service status
   */
  public static boolean isAccumuloOffline(IZooReader reader, String rootPath) throws IOException {
    try {
      for (String child : reader.getChildren(rootPath + Constants.ZTSERVERS)) {
        if (!reader.getChildren(rootPath + Constants.ZTSERVERS + "/" + child).isEmpty())
          return false;
      }
      if (!reader.getChildren(rootPath + Constants.ZMASTER_LOCK).isEmpty())
        return false;
      if (!reader.getChildren(rootPath + Constants.ZMONITOR_LOCK).isEmpty())
        return false;
      if (!reader.getChildren(rootPath + Constants.ZGC_LOCK).isEmpty())
        return false;
    } catch (KeeperException e) {
      throw new IOException("Issues contacting ZooKeeper to get Accumulo status.", e);
    } catch (InterruptedException e) {
      throw new IOException("Issues contacting ZooKeeper to get Accumulo status.", e);
    }
    return true;
  }

}
