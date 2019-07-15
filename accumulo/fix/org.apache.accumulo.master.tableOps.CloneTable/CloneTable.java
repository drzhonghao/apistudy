

import java.util.Map;
import java.util.Set;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;


public class CloneTable extends MasterRepo {
	private static final long serialVersionUID = 1L;

	public CloneTable(String user, String namespaceId, String srcTableId, String tableName, Map<String, String> propertiesToSet, Set<String> propertiesToExclude) {
	}

	@Override
	public long isReady(long tid, Master environment) throws Exception {
		return 0L;
	}

	@Override
	public Repo<Master> call(long tid, Master environment) throws Exception {
		try {
		} finally {
		}
		return null;
	}

	@Override
	public void undo(long tid, Master environment) throws Exception {
	}
}

