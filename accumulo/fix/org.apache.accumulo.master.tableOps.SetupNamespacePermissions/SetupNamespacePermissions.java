

import org.apache.accumulo.core.security.NamespacePermission;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.server.security.AuditedSecurityOperation;
import org.apache.accumulo.server.security.SecurityOperation;


class SetupNamespacePermissions extends MasterRepo {
	private static final long serialVersionUID = 1L;

	@Override
	public Repo<Master> call(long tid, Master env) throws Exception {
		SecurityOperation security = AuditedSecurityOperation.getInstance(env);
		for (NamespacePermission permission : NamespacePermission.values()) {
		}
		return null;
	}
}

