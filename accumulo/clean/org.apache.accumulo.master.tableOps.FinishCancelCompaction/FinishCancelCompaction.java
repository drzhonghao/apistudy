import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.master.tableOps.Utils;
import org.apache.accumulo.master.tableOps.*;


import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;

class FinishCancelCompaction extends MasterRepo {
  private static final long serialVersionUID = 1L;
  private String tableId;
  private String namespaceId;

  private String getNamespaceId(Master env) throws Exception {
    return Utils.getNamespaceId(env.getInstance(), tableId, TableOperation.COMPACT_CANCEL,
        this.namespaceId);
  }

  public FinishCancelCompaction(String namespaceId, String tableId) {
    this.tableId = tableId;
    this.namespaceId = namespaceId;
  }

  @Override
  public Repo<Master> call(long tid, Master environment) throws Exception {
    Utils.unreserveTable(tableId, tid, false);
    Utils.unreserveNamespace(getNamespaceId(environment), tid, false);
    return null;
  }

  @Override
  public void undo(long tid, Master environment) throws Exception {

  }
}
