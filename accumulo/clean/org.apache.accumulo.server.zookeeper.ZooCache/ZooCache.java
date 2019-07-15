import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.accumulo.server.zookeeper.*;


import org.apache.zookeeper.Watcher;

public class ZooCache extends org.apache.accumulo.fate.zookeeper.ZooCache {
  public ZooCache() {
    this(null);
  }

  public ZooCache(Watcher watcher) {
    super(ZooReaderWriter.getInstance(), watcher);
  }

}
