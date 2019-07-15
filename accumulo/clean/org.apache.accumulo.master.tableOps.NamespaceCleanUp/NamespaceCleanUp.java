import org.apache.accumulo.master.tableOps.*;


import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.server.security.AuditedSecurityOperation;
import org.apache.accumulo.server.tables.TableManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NamespaceCleanUp extends MasterRepo {

  private static final Logger log = LoggerFactory.getLogger(NamespaceCleanUp.class);

  private static final long serialVersionUID = 1L;

  private String namespaceId;

  public NamespaceCleanUp(String namespaceId) {
    this.namespaceId = namespaceId;
  }

  @Override
  public long isReady(long tid, Master master) throws Exception {
    return 0;
  }

  @Override
  public Repo<Master> call(long id, Master master) throws Exception {

    // remove from zookeeper
    try {
      TableManager.getInstance().removeNamespace(namespaceId);
    } catch (Exception e) {
      log.error("Failed to find namespace in zookeeper", e);
    }
    Tables.clearCache(master.getInstance());

    // remove any permissions associated with this namespace
    try {
      AuditedSecurityOperation.getInstance(master).deleteNamespace(master.rpcCreds(), namespaceId);
    } catch (ThriftSecurityException e) {
      log.error("{}", e.getMessage(), e);
    }

    Utils.unreserveNamespace(namespaceId, id, true);

    log.debug("Deleted namespace " + namespaceId);

    return null;
  }

  @Override
  public void undo(long tid, Master environment) throws Exception {
    // nothing to do
  }

}
