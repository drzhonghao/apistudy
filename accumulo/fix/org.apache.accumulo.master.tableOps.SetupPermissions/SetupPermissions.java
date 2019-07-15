

import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;


class SetupPermissions extends MasterRepo {
	private static final long serialVersionUID = 1L;

	@Override
	public void undo(long tid, Master env) throws Exception {
	}
}

