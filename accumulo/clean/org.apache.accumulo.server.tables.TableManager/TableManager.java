import org.apache.accumulo.server.tables.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.SecurityPermission;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter.Mutator;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.util.TablePropUtil;
import org.apache.accumulo.server.zookeeper.ZooCache;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableManager {
  private static SecurityPermission TABLE_MANAGER_PERMISSION = new SecurityPermission(
      "tableManagerPermission");

  private static final Logger log = LoggerFactory.getLogger(TableManager.class);
  private static final Set<TableObserver> observers = Collections
      .synchronizedSet(new HashSet<TableObserver>());
  private static final Map<String,TableState> tableStateCache = Collections
      .synchronizedMap(new HashMap<String,TableState>());
  private static final byte[] ZERO_BYTE = new byte[] {'0'};

  private static TableManager tableManager = null;

  private final Instance instance;
  private ZooCache zooStateCache;

  public static void prepareNewNamespaceState(String instanceId, String namespaceId,
      String namespace, NodeExistsPolicy existsPolicy)
      throws KeeperException, InterruptedException {
    log.debug(
        "Creating ZooKeeper entries for new namespace " + namespace + " (ID: " + namespaceId + ")");
    String zPath = Constants.ZROOT + "/" + instanceId + Constants.ZNAMESPACES + "/" + namespaceId;

    IZooReaderWriter zoo = ZooReaderWriter.getInstance();
    zoo.putPersistentData(zPath, new byte[0], existsPolicy);
    zoo.putPersistentData(zPath + Constants.ZNAMESPACE_NAME, namespace.getBytes(UTF_8),
        existsPolicy);
    zoo.putPersistentData(zPath + Constants.ZNAMESPACE_CONF, new byte[0], existsPolicy);
  }

  public static void prepareNewTableState(String instanceId, String tableId, String namespaceId,
      String tableName, TableState state, NodeExistsPolicy existsPolicy)
      throws KeeperException, InterruptedException {
    // state gets created last
    log.debug("Creating ZooKeeper entries for new table " + tableName + " (ID: " + tableId
        + ") in namespace (ID: " + namespaceId + ")");
    Pair<String,String> qualifiedTableName = Tables.qualify(tableName);
    tableName = qualifiedTableName.getSecond();
    String zTablePath = Constants.ZROOT + "/" + instanceId + Constants.ZTABLES + "/" + tableId;
    IZooReaderWriter zoo = ZooReaderWriter.getInstance();
    zoo.putPersistentData(zTablePath, new byte[0], existsPolicy);
    zoo.putPersistentData(zTablePath + Constants.ZTABLE_CONF, new byte[0], existsPolicy);
    zoo.putPersistentData(zTablePath + Constants.ZTABLE_NAMESPACE, namespaceId.getBytes(UTF_8),
        existsPolicy);
    zoo.putPersistentData(zTablePath + Constants.ZTABLE_NAME, tableName.getBytes(UTF_8),
        existsPolicy);
    zoo.putPersistentData(zTablePath + Constants.ZTABLE_FLUSH_ID, ZERO_BYTE, existsPolicy);
    zoo.putPersistentData(zTablePath + Constants.ZTABLE_COMPACT_ID, ZERO_BYTE, existsPolicy);
    zoo.putPersistentData(zTablePath + Constants.ZTABLE_COMPACT_CANCEL_ID, ZERO_BYTE, existsPolicy);
    zoo.putPersistentData(zTablePath + Constants.ZTABLE_STATE, state.name().getBytes(UTF_8),
        existsPolicy);
  }

  public synchronized static TableManager getInstance() {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(TABLE_MANAGER_PERMISSION);
    }
    if (tableManager == null)
      tableManager = new TableManager();
    return tableManager;
  }

  private TableManager() {
    instance = HdfsZooInstance.getInstance();
    zooStateCache = new ZooCache(new TableStateWatcher());
    updateTableStateCache();
  }

  public TableState getTableState(String tableId) {
    return tableStateCache.get(tableId);
  }

  public static class IllegalTableTransitionException extends Exception {
    private static final long serialVersionUID = 1L;

    final TableState oldState;
    final TableState newState;
    final String message;

    public IllegalTableTransitionException(TableState oldState, TableState newState) {
      this(oldState, newState, "");
    }

    public IllegalTableTransitionException(TableState oldState, TableState newState,
        String message) {
      this.oldState = oldState;
      this.newState = newState;

      if (StringUtils.isNotEmpty(message))
        this.message = message;
      else {
        String defaultMessage = "Error transitioning from " + oldState + " state to " + newState
            + " state";
        this.message = defaultMessage;
      }
    }

    public TableState getOldState() {
      return oldState;
    }

    public TableState getNewState() {
      return newState;
    }

    @Override
    public String getMessage() {
      return message;
    }

  }

  public synchronized void transitionTableState(final String tableId, final TableState newState) {
    String statePath = ZooUtil.getRoot(HdfsZooInstance.getInstance()) + Constants.ZTABLES + "/"
        + tableId + Constants.ZTABLE_STATE;

    try {
      ZooReaderWriter.getInstance().mutate(statePath, newState.name().getBytes(UTF_8),
          ZooUtil.PUBLIC, new Mutator() {
            @Override
            public byte[] mutate(byte[] oldData) throws Exception {
              TableState oldState = TableState.UNKNOWN;
              if (oldData != null)
                oldState = TableState.valueOf(new String(oldData, UTF_8));
              boolean transition = true;
              // +--------+
              // v |
              // NEW -> (ONLINE|OFFLINE)+--- DELETING
              switch (oldState) {
                case NEW:
                  transition = (newState == TableState.OFFLINE || newState == TableState.ONLINE);
                  break;
                case ONLINE: // fall-through intended
                case UNKNOWN:// fall through intended
                case OFFLINE:
                  transition = (newState != TableState.NEW);
                  break;
                case DELETING:
                  // Can't transition to any state from DELETING
                  transition = false;
                  break;
              }
              if (!transition)
                throw new IllegalTableTransitionException(oldState, newState);
              log.debug("Transitioning state for table " + tableId + " from " + oldState + " to "
                  + newState);
              return newState.name().getBytes(UTF_8);
            }
          });
    } catch (Exception e) {
      // ACCUMULO-3651 Changed level to error and added FATAL to message for slf4j compatibility
      log.error("FATAL Failed to transition table to state " + newState);
      throw new RuntimeException(e);
    }
  }

  private void updateTableStateCache() {
    synchronized (tableStateCache) {
      for (String tableId : zooStateCache
          .getChildren(ZooUtil.getRoot(instance) + Constants.ZTABLES))
        if (zooStateCache.get(ZooUtil.getRoot(instance) + Constants.ZTABLES + "/" + tableId
            + Constants.ZTABLE_STATE) != null)
          updateTableStateCache(tableId);
    }
  }

  public TableState updateTableStateCache(String tableId) {
    synchronized (tableStateCache) {
      TableState tState = TableState.UNKNOWN;
      byte[] data = zooStateCache.get(
          ZooUtil.getRoot(instance) + Constants.ZTABLES + "/" + tableId + Constants.ZTABLE_STATE);
      if (data != null) {
        String sState = new String(data, UTF_8);
        try {
          tState = TableState.valueOf(sState);
        } catch (IllegalArgumentException e) {
          log.error("Unrecognized state for table with tableId=" + tableId + ": " + sState);
        }
        tableStateCache.put(tableId, tState);
      }
      return tState;
    }
  }

  public void addTable(String tableId, String namespaceId, String tableName,
      NodeExistsPolicy existsPolicy)
      throws KeeperException, InterruptedException, NamespaceNotFoundException {
    prepareNewTableState(instance.getInstanceID(), tableId, namespaceId, tableName, TableState.NEW,
        existsPolicy);
    updateTableStateCache(tableId);
  }

  public void cloneTable(String srcTable, String tableId, String tableName, String namespaceId,
      Map<String,String> propertiesToSet, Set<String> propertiesToExclude,
      NodeExistsPolicy existsPolicy) throws KeeperException, InterruptedException {
    prepareNewTableState(instance.getInstanceID(), tableId, namespaceId, tableName, TableState.NEW,
        existsPolicy);

    String srcTablePath = Constants.ZROOT + "/" + instance.getInstanceID() + Constants.ZTABLES + "/"
        + srcTable + Constants.ZTABLE_CONF;
    String newTablePath = Constants.ZROOT + "/" + instance.getInstanceID() + Constants.ZTABLES + "/"
        + tableId + Constants.ZTABLE_CONF;
    ZooReaderWriter.getInstance().recursiveCopyPersistent(srcTablePath, newTablePath,
        NodeExistsPolicy.OVERWRITE);

    for (Entry<String,String> entry : propertiesToSet.entrySet())
      TablePropUtil.setTableProperty(tableId, entry.getKey(), entry.getValue());

    for (String prop : propertiesToExclude)
      ZooReaderWriter.getInstance().recursiveDelete(Constants.ZROOT + "/" + instance.getInstanceID()
          + Constants.ZTABLES + "/" + tableId + Constants.ZTABLE_CONF + "/" + prop,
          NodeMissingPolicy.SKIP);

    updateTableStateCache(tableId);
  }

  public void removeTable(String tableId) throws KeeperException, InterruptedException {
    synchronized (tableStateCache) {
      tableStateCache.remove(tableId);
      ZooReaderWriter.getInstance().recursiveDelete(
          ZooUtil.getRoot(instance) + Constants.ZTABLES + "/" + tableId + Constants.ZTABLE_STATE,
          NodeMissingPolicy.SKIP);
      ZooReaderWriter.getInstance().recursiveDelete(
          ZooUtil.getRoot(instance) + Constants.ZTABLES + "/" + tableId, NodeMissingPolicy.SKIP);
    }
  }

  public boolean addObserver(TableObserver to) {
    synchronized (observers) {
      synchronized (tableStateCache) {
        to.initialize(Collections.unmodifiableMap(tableStateCache));
        return observers.add(to);
      }
    }
  }

  private class TableStateWatcher implements Watcher {
    @Override
    public void process(WatchedEvent event) {
      if (log.isTraceEnabled()) {
        log.trace("{}", event);
      }
      final String zPath = event.getPath();
      final EventType zType = event.getType();

      String tablesPrefix = ZooUtil.getRoot(instance) + Constants.ZTABLES;
      String tableId = null;

      if (zPath != null && zPath.startsWith(tablesPrefix + "/")) {
        String suffix = zPath.substring(tablesPrefix.length() + 1);
        if (suffix.contains("/")) {
          String[] sa = suffix.split("/", 2);
          if (Constants.ZTABLE_STATE.equals("/" + sa[1]))
            tableId = sa[0];
        }
        if (tableId == null) {
          log.warn("Unknown path in " + event);
          return;
        }
      }

      switch (zType) {
        case NodeChildrenChanged:
          if (zPath != null && zPath.equals(tablesPrefix)) {
            updateTableStateCache();
          } else {
            log.warn("Unexpected path " + zPath);
          }
          break;
        case NodeCreated:
        case NodeDataChanged:
          // state transition
          TableState tState = updateTableStateCache(tableId);
          log.debug("State transition to " + tState + " @ " + event);
          synchronized (observers) {
            for (TableObserver to : observers)
              to.stateChanged(tableId, tState);
          }
          break;
        case NodeDeleted:
          if (zPath != null && tableId != null
              && (zPath.equals(tablesPrefix + "/" + tableId + Constants.ZTABLE_STATE)
                  || zPath.equals(tablesPrefix + "/" + tableId + Constants.ZTABLE_CONF)
                  || zPath.equals(tablesPrefix + "/" + tableId + Constants.ZTABLE_NAME)))
            tableStateCache.remove(tableId);
          break;
        case None:
          switch (event.getState()) {
            case Expired:
              if (log.isTraceEnabled())
                log.trace("Session expired " + event);
              synchronized (observers) {
                for (TableObserver to : observers)
                  to.sessionExpired();
              }
              break;
            case SyncConnected:
            default:
              if (log.isTraceEnabled())
                log.trace("Ignored " + event);
          }
          break;
        default:
          log.warn("Unandled " + event);
      }
    }
  }

  public void removeNamespace(String namespaceId) throws KeeperException, InterruptedException {
    ZooReaderWriter.getInstance().recursiveDelete(
        ZooUtil.getRoot(instance) + Constants.ZNAMESPACES + "/" + namespaceId,
        NodeMissingPolicy.SKIP);
  }

}
