

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;


class CloneZookeeper extends MasterRepo {
	private static final long serialVersionUID = 1L;

	@Override
	public Repo<Master> call(long tid, Master environment) throws Exception {
		try {
			Tables.clearCache(environment.getInstance());
		} finally {
		}
		return null;
	}

	@Override
	public void undo(long tid, Master environment) throws Exception {
		Tables.clearCache(environment.getInstance());
	}
}

