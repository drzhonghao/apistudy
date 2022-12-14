import org.apache.accumulo.server.util.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.PropertyType;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemPropUtil {
  private static final Logger log = LoggerFactory.getLogger(SystemPropUtil.class);

  public static boolean setSystemProperty(String property, String value)
      throws KeeperException, InterruptedException {
    if (!Property.isValidZooPropertyKey(property)) {
      IllegalArgumentException iae = new IllegalArgumentException(
          "Zookeeper property is not mutable: " + property);
      log.debug("Attempted to set zookeeper property.  It is not mutable", iae);
      throw iae;
    }

    // Find the property taking prefix into account
    Property foundProp = null;
    for (Property prop : Property.values()) {
      if (PropertyType.PREFIX == prop.getType() && property.startsWith(prop.getKey())
          || prop.getKey().equals(property)) {
        foundProp = prop;
        break;
      }
    }

    if ((foundProp == null || (foundProp.getType() != PropertyType.PREFIX
        && !foundProp.getType().isValidFormat(value)))) {
      IllegalArgumentException iae = new IllegalArgumentException(
          "Ignoring property " + property + " it is either null or in an invalid format");
      log.debug("Attempted to set zookeeper property.  Value is either null or invalid", iae);
      throw iae;
    }

    // create the zk node for this property and set it's data to the specified value
    String zPath = ZooUtil.getRoot(HdfsZooInstance.getInstance()) + Constants.ZCONFIG + "/"
        + property;
    boolean result = ZooReaderWriter.getInstance().putPersistentData(zPath, value.getBytes(UTF_8),
        NodeExistsPolicy.OVERWRITE);

    return result;
  }

  public static void removeSystemProperty(String property)
      throws InterruptedException, KeeperException {
    String zPath = ZooUtil.getRoot(HdfsZooInstance.getInstance()) + Constants.ZCONFIG + "/"
        + property;
    ZooReaderWriter.getInstance().recursiveDelete(zPath, NodeMissingPolicy.FAIL);
  }
}
