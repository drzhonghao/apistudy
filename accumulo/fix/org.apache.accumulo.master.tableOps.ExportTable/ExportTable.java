

import org.apache.accumulo.core.client.impl.thrift.ThriftTableOperationException;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;


public class ExportTable extends MasterRepo {
	private static final long serialVersionUID = 1L;

	public ExportTable(String namespaceId, String tableName, String tableId, String exportDir) throws ThriftTableOperationException {
	}

	@Override
	public long isReady(long tid, Master environment) throws Exception {
		return 0L;
	}

	@Override
	public Repo<Master> call(long tid, Master env) throws Exception {
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
	}

	public static final int VERSION = 1;

	public static final String DATA_VERSION_PROP = "srcDataVersion";

	public static final String EXPORT_VERSION_PROP = "exportVersion";
}

