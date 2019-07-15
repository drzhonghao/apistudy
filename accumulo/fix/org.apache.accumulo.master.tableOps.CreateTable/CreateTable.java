

import java.util.Map;
import org.apache.accumulo.core.client.admin.TimeType;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;


public class CreateTable extends MasterRepo {
	private static final long serialVersionUID = 1L;

	public CreateTable(String user, String tableName, TimeType timeType, Map<String, String> props, String namespaceId) {
	}

	@Override
	public long isReady(long tid, Master environment) throws Exception {
		return 0L;
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

