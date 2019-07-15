import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.core.client.impl.*;


import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.accumulo.core.client.impl.Tables.qualified;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * Used for thread safe caching of immutable table ID maps. See ACCUMULO-4778.
 */
public class TableMap {
  private static final Logger log = LoggerFactory.getLogger(TableMap.class);

  private final Map<String,String> tableNameToIdMap;
  private final Map<String,String> tableIdToNameMap;

  public TableMap(Instance instance, ZooCache zooCache) {
    List<String> tableIds = zooCache.getChildren(ZooUtil.getRoot(instance) + Constants.ZTABLES);
    Map<String,String> namespaceIdToNameMap = new HashMap<>();
    ImmutableMap.Builder<String,String> tableNameToIdBuilder = new ImmutableMap.Builder<>();
    ImmutableMap.Builder<String,String> tableIdToNameBuilder = new ImmutableMap.Builder<>();
    // use StringBuilder to construct zPath string efficiently across many tables
    StringBuilder zPathBuilder = new StringBuilder();
    zPathBuilder.append(ZooUtil.getRoot(instance)).append(Constants.ZTABLES).append("/");
    int prefixLength = zPathBuilder.length();

    for (String tableId : tableIds) {
      // reset StringBuilder to prefix length before appending ID and suffix
      zPathBuilder.setLength(prefixLength);
      zPathBuilder.append(tableId).append(Constants.ZTABLE_NAME);
      byte[] tableName = zooCache.get(zPathBuilder.toString());
      zPathBuilder.setLength(prefixLength);
      zPathBuilder.append(tableId).append(Constants.ZTABLE_NAMESPACE);
      byte[] nId = zooCache.get(zPathBuilder.toString());

      String namespaceName = Namespaces.DEFAULT_NAMESPACE;
      // create fully qualified table name
      if (nId == null) {
        namespaceName = null;
      } else {
        String namespaceId = new String(nId, UTF_8);
        if (!namespaceId.equals(Namespaces.DEFAULT_NAMESPACE_ID)) {
          try {
            namespaceName = namespaceIdToNameMap.get(namespaceId);
            if (namespaceName == null) {
              namespaceName = Namespaces.getNamespaceName(instance, namespaceId);
              namespaceIdToNameMap.put(namespaceId, namespaceName);
            }
          } catch (NamespaceNotFoundException e) {
            log.error("Table (" + tableId + ") contains reference to namespace (" + namespaceId
                + ") that doesn't exist", e);
            continue;
          }
        }
      }
      if (tableName != null && namespaceName != null) {
        String tableNameStr = qualified(new String(tableName, UTF_8), namespaceName);
        tableNameToIdBuilder.put(tableNameStr, tableId);
        tableIdToNameBuilder.put(tableId, tableNameStr);
      }
    }
    tableNameToIdMap = tableNameToIdBuilder.build();
    tableIdToNameMap = tableIdToNameBuilder.build();
  }

  public Map<String,String> getNameToIdMap() {
    return tableNameToIdMap;
  }

  public Map<String,String> getIdtoNameMap() {
    return tableIdToNameMap;
  }
}
