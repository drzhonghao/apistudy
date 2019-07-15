

import java.util.Map;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;


public class CreateNamespace extends MasterRepo {
	private static final long serialVersionUID = 1L;

	public CreateNamespace(String user, String namespaceName, Map<String, String> props) {
	}

	@Override
	public long isReady(long tid, Master environment) throws Exception {
		return 0;
	}

	@Override
	public Repo<Master> call(long tid, Master master) throws Exception {
		try {
		} finally {
		}
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
	}
}

