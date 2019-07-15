

import org.apache.accumulo.core.security.TablePermission;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.server.security.AuditedSecurityOperation;
import org.apache.accumulo.server.security.SecurityOperation;


class ImportSetupPermissions extends MasterRepo {
	private static final long serialVersionUID = 1L;

	@Override
	public long isReady(long tid, Master environment) throws Exception {
		return 0;
	}

	@Override
	public Repo<Master> call(long tid, Master env) throws Exception {
		SecurityOperation security = AuditedSecurityOperation.getInstance(env);
		for (TablePermission permission : TablePermission.values()) {
		}
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
	}
}

