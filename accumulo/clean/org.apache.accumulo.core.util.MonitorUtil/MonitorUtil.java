import org.apache.accumulo.core.util.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooReader;
import org.apache.zookeeper.KeeperException;

public class MonitorUtil {
  public static String getLocation(Instance instance) throws KeeperException, InterruptedException {
    ZooReader zr = new ZooReader(instance.getZooKeepers(), 30000);
    byte[] loc = zr.getData(ZooUtil.getRoot(instance) + Constants.ZMONITOR_HTTP_ADDR, null);
    return loc == null ? null : new String(loc, UTF_8);
  }
}
