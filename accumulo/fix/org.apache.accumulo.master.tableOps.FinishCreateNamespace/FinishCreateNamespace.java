

import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;


class FinishCreateNamespace extends MasterRepo {
	private static final long serialVersionUID = 1L;

	@Override
	public long isReady(long tid, Master environment) throws Exception {
		return 0;
	}

	@Override
	public Repo<Master> call(long id, Master env) throws Exception {
		return null;
	}

	@Override
	public String getReturn() {
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
	}
}

