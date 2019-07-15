

import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;


class ChooseDir extends MasterRepo {
	private static final long serialVersionUID = 1L;

	@Override
	public long isReady(long tid, Master environment) throws Exception {
		return 0;
	}

	@Override
	public Repo<Master> call(long tid, Master master) throws Exception {
		return null;
	}

	@Override
	public void undo(long tid, Master master) throws Exception {
	}
}

