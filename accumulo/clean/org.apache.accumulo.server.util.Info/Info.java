import org.apache.accumulo.server.util.*;


import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.util.MonitorUtil;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.start.spi.KeywordExecutable;
import org.apache.zookeeper.KeeperException;

import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class Info implements KeywordExecutable {

  @Override
  public String keyword() {
    return "info";
  }

  @Override
  public void execute(final String[] args) throws KeeperException, InterruptedException {
    Instance instance = HdfsZooInstance.getInstance();
    System.out.println("monitor: " + MonitorUtil.getLocation(instance));
    System.out.println("masters: " + instance.getMasterLocations());
    System.out.println("zookeepers: " + instance.getZooKeepers());
  }

  public static void main(String[] args) throws Exception {
    new Info().execute(args);
  }
}
