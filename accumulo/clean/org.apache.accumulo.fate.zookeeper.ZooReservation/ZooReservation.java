import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.LoggerFactory;

public class ZooReservation {

  public static boolean attempt(IZooReaderWriter zk, String path, String reservationID,
      String debugInfo) throws KeeperException, InterruptedException {
    if (reservationID.contains(":"))
      throw new IllegalArgumentException();

    while (true) {
      try {
        zk.putPersistentData(path, (reservationID + ":" + debugInfo).getBytes(UTF_8),
            NodeExistsPolicy.FAIL);
        return true;
      } catch (NodeExistsException nee) {
        Stat stat = new Stat();
        byte[] zooData;
        try {
          zooData = zk.getData(path, stat);
        } catch (NoNodeException nne) {
          continue;
        }

        String idInZoo = new String(zooData, UTF_8).split(":")[0];

        return idInZoo.equals(reservationID);
      }
    }

  }

  public static void release(IZooReaderWriter zk, String path, String reservationID)
      throws KeeperException, InterruptedException {
    byte[] zooData;

    try {
      zooData = zk.getData(path, null);
    } catch (NoNodeException e) {
      // Just logging a warning, if data is gone then our work here is done.
      LoggerFactory.getLogger(ZooReservation.class).debug("Node does not exist " + path);
      return;
    }

    String zooDataStr = new String(zooData, UTF_8);
    String idInZoo = zooDataStr.split(":")[0];

    if (!idInZoo.equals(reservationID)) {
      throw new IllegalStateException("Tried to release reservation " + path
          + " with data mismatch " + reservationID + " " + zooDataStr);
    }

    zk.recursiveDelete(path, NodeMissingPolicy.SKIP);
  }

}
