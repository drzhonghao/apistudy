

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;


class PopulateZookeeperWithNamespace extends MasterRepo {
	private static final long serialVersionUID = 1L;

	@Override
	public long isReady(long id, Master environment) throws Exception {
		return 0L;
	}

	@Override
	public Repo<Master> call(long tid, Master master) throws Exception {
		return null;
	}

	@Override
	public void undo(long tid, Master master) throws Exception {
		Tables.clearCache(master.getInstance());
	}
}

