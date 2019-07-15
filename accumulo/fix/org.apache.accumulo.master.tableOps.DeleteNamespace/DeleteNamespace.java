

import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.EventCoordinator;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.master.tableOps.Utils;


public class DeleteNamespace extends MasterRepo {
	private static final long serialVersionUID = 1L;

	private String namespaceId;

	public DeleteNamespace(String namespaceId) {
		this.namespaceId = namespaceId;
	}

	@Override
	public long isReady(long id, Master environment) throws Exception {
		return Utils.reserveNamespace(namespaceId, id, true, true, TableOperation.DELETE);
	}

	@Override
	public Repo<Master> call(long tid, Master environment) throws Exception {
		environment.getEventCoordinator().event("deleting namespace %s ", namespaceId);
		return null;
	}

	@Override
	public void undo(long id, Master environment) throws Exception {
		Utils.unreserveNamespace(namespaceId, id, true);
	}
}

