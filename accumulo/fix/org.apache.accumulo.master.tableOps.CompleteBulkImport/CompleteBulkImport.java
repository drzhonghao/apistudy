

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.server.zookeeper.TransactionWatcher;

import static org.apache.accumulo.server.zookeeper.TransactionWatcher.ZooArbitrator.stop;


class CompleteBulkImport extends MasterRepo {
	private static final long serialVersionUID = 1L;

	private String tableId;

	private String source;

	private String bulk;

	private String error;

	public CompleteBulkImport(String tableId, String source, String bulk, String error) {
		this.tableId = tableId;
		this.source = source;
		this.bulk = bulk;
		this.error = error;
	}

	@Override
	public Repo<Master> call(long tid, Master master) throws Exception {
		stop(Constants.BULK_ARBITRATOR_TYPE, tid);
		return null;
	}
}

