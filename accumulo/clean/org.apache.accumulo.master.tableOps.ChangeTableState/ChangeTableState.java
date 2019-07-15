import org.apache.accumulo.master.tableOps.*;


import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.server.tables.TableManager;
import org.slf4j.LoggerFactory;

public class ChangeTableState extends MasterRepo {

  private static final long serialVersionUID = 1L;
  private String tableId;
  private String namespaceId;
  private TableOperation top;

  private String getNamespaceId(Master env) throws Exception {
    return Utils.getNamespaceId(env.getInstance(), tableId, top, this.namespaceId);
  }

  public ChangeTableState(String namespaceId, String tableId, TableOperation top) {
    this.tableId = tableId;
    this.namespaceId = namespaceId;
    this.top = top;

    if (top != TableOperation.ONLINE && top != TableOperation.OFFLINE)
      throw new IllegalArgumentException(top.toString());
  }

  @Override
  public long isReady(long tid, Master env) throws Exception {
    // reserve the table so that this op does not run concurrently with create, clone, or delete
    // table
    return Utils.reserveNamespace(getNamespaceId(env), tid, false, true, top)
        + Utils.reserveTable(tableId, tid, true, true, top);
  }

  @Override
  public Repo<Master> call(long tid, Master env) throws Exception {
    TableState ts = TableState.ONLINE;
    if (top == TableOperation.OFFLINE)
      ts = TableState.OFFLINE;

    TableManager.getInstance().transitionTableState(tableId, ts);
    Utils.unreserveNamespace(getNamespaceId(env), tid, false);
    Utils.unreserveTable(tableId, tid, true);
    LoggerFactory.getLogger(ChangeTableState.class)
        .debug("Changed table state " + tableId + " " + ts);
    env.getEventCoordinator().event("Set table state of %s to %s", tableId, ts);
    return null;
  }

  @Override
  public void undo(long tid, Master env) throws Exception {
    Utils.unreserveNamespace(getNamespaceId(env), tid, false);
    Utils.unreserveTable(tableId, tid, true);
  }
}
