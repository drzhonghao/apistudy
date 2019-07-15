

import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;


class FinishCloneTable extends MasterRepo {
	private static final long serialVersionUID = 1L;

	@Override
	public long isReady(long tid, Master environment) throws Exception {
		return 0;
	}

	@Override
	public void undo(long tid, Master environment) throws Exception {
	}
}

