import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.*;


import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.accumulo.fate.zookeeper.DistributedReadWriteLock.QueueLock;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NotEmptyException;

public class ZooQueueLock implements QueueLock {

  private static final String PREFIX = "lock-";

  private IZooReaderWriter zoo;
  private String path;
  private boolean ephemeral;

  protected ZooQueueLock(IZooReaderWriter zrw, String path, boolean ephemeral) {
    this.zoo = zrw;
    this.path = path;
    this.ephemeral = ephemeral;
  }

  @Override
  public long addEntry(byte[] data) {
    String newPath;
    try {
      while (true) {
        try {
          if (ephemeral) {
            newPath = zoo.putEphemeralSequential(path + "/" + PREFIX, data);
          } else {
            newPath = zoo.putPersistentSequential(path + "/" + PREFIX, data);
          }
          String[] parts = newPath.split("/");
          String last = parts[parts.length - 1];
          return Long.parseLong(last.substring(PREFIX.length()));
        } catch (NoNodeException nne) {
          // the parent does not exist so try to create it
          zoo.putPersistentData(path, new byte[] {}, NodeExistsPolicy.SKIP);
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public SortedMap<Long,byte[]> getEarlierEntries(long entry) {
    SortedMap<Long,byte[]> result = new TreeMap<>();
    try {
      List<String> children = Collections.emptyList();
      try {
        children = zoo.getChildren(path);
      } catch (KeeperException.NoNodeException ex) {
        // the path does not exist (it was deleted or not created yet), that is ok there are no
        // earlier entries then
      }

      for (String name : children) {
        // this try catch must be done inside the loop because some subset of the children may exist
        try {
          byte[] data = zoo.getData(path + "/" + name, null);
          long order = Long.parseLong(name.substring(PREFIX.length()));
          if (order <= entry)
            result.put(order, data);
        } catch (KeeperException.NoNodeException ex) {
          // ignored
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    return result;
  }

  @Override
  public void removeEntry(long entry) {
    try {
      zoo.recursiveDelete(path + String.format("/%s%010d", PREFIX, entry), NodeMissingPolicy.SKIP);
      try {
        // try to delete the parent if it has no children
        zoo.delete(path, -1);
      } catch (NotEmptyException nee) {
        // the path had other lock nodes, no big deal
      } catch (NoNodeException nne) {
        // someone else deleted the lock path
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
