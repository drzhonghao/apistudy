

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.EventCoordinator;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.master.tableOps.Utils;
import org.apache.accumulo.server.tables.TableManager;


public class DeleteTable extends MasterRepo {
	private static final long serialVersionUID = 1L;

	private String tableId;

	private String namespaceId;

	private String getNamespaceId(Master env) throws Exception {
		return Utils.getNamespaceId(env.getInstance(), tableId, TableOperation.DELETE, this.namespaceId);
	}

	public DeleteTable(String namespaceId, String tableId) {
		this.namespaceId = namespaceId;
		this.tableId = tableId;
	}

	@Override
	public long isReady(long tid, Master env) throws Exception {
		return (Utils.reserveNamespace(getNamespaceId(env), tid, false, false, TableOperation.DELETE)) + (Utils.reserveTable(tableId, tid, true, true, TableOperation.DELETE));
	}

	@Override
	public Repo<Master> call(long tid, Master env) throws Exception {
		TableManager.getInstance().transitionTableState(tableId, TableState.DELETING);
		env.getEventCoordinator().event("deleting table %s ", tableId);
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
		Utils.unreserveTable(tableId, tid, true);
		Utils.unreserveNamespace(getNamespaceId(env), tid, false);
	}
}

