

import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.AcceptableThriftTableOperationException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.ExportTable;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.server.ServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ImportTable extends MasterRepo {
	private static final Logger log = LoggerFactory.getLogger(ImportTable.class);

	private static final long serialVersionUID = 1L;

	public ImportTable(String user, String tableName, String exportDir, String namespaceId) {
	}

	@Override
	public long isReady(long tid, Master environment) throws Exception {
		return 0L;
	}

	@Override
	public Repo<Master> call(long tid, Master env) throws Exception {
		checkVersions(env);
		try {
			Instance instance = env.getInstance();
		} finally {
		}
		return null;
	}

	public void checkVersions(Master env) throws AcceptableThriftTableOperationException {
		Integer exportVersion = null;
		Integer dataVersion = null;
		if ((exportVersion == null) || (exportVersion > (ExportTable.VERSION))) {
		}
		if ((dataVersion == null) || (dataVersion > (ServerConstants.DATA_VERSION))) {
		}
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
	}
}

