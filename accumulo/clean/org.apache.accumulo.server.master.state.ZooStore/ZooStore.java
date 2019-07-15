import org.apache.accumulo.fate.ZooStore;
import org.apache.accumulo.server.master.state.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.List;

import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.zookeeper.ZooCache;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooStore implements DistributedStore {

  private static final Logger log = LoggerFactory.getLogger(ZooStore.class);

  String basePath;

  ZooCache cache = new ZooCache();

  public ZooStore(String basePath) throws IOException {
    if (basePath.endsWith("/"))
      basePath = basePath.substring(0, basePath.length() - 1);
    this.basePath = basePath;
  }

  public ZooStore() throws IOException {
    this(ZooUtil.getRoot(HdfsZooInstance.getInstance().getInstanceID()));
  }

  @Override
  public byte[] get(String path) throws DistributedStoreException {
    try {
      return cache.get(relative(path));
    } catch (Exception ex) {
      throw new DistributedStoreException(ex);
    }
  }

  private String relative(String path) {
    return basePath + path;
  }

  @Override
  public List<String> getChildren(String path) throws DistributedStoreException {
    try {
      return cache.getChildren(relative(path));
    } catch (Exception ex) {
      throw new DistributedStoreException(ex);
    }
  }

  @Override
  public void put(String path, byte[] bs) throws DistributedStoreException {
    try {
      path = relative(path);
      ZooReaderWriter.getInstance().putPersistentData(path, bs, NodeExistsPolicy.OVERWRITE);
      cache.clear();
      log.debug("Wrote " + new String(bs, UTF_8) + " to " + path);
    } catch (Exception ex) {
      throw new DistributedStoreException(ex);
    }
  }

  @Override
  public void remove(String path) throws DistributedStoreException {
    try {
      log.debug("Removing " + path);
      path = relative(path);
      IZooReaderWriter zoo = ZooReaderWriter.getInstance();
      if (zoo.exists(path))
        zoo.recursiveDelete(path, NodeMissingPolicy.SKIP);
      cache.clear();
    } catch (Exception ex) {
      throw new DistributedStoreException(ex);
    }
  }
}
