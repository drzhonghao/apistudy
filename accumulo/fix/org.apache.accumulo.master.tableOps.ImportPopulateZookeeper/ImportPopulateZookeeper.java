

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.server.fs.VolumeManager;


class ImportPopulateZookeeper extends MasterRepo {
	private static final long serialVersionUID = 1L;

	@Override
	public long isReady(long tid, Master environment) throws Exception {
		return 0L;
	}

	private Map<String, String> getExportedProps(VolumeManager fs) throws Exception {
		return null;
	}

	@Override
	public Repo<Master> call(long tid, Master env) throws Exception {
		try {
			Instance instance = env.getInstance();
			Tables.clearCache(instance);
		} finally {
		}
		for (Map.Entry<String, String> entry : getExportedProps(env.getFileSystem()).entrySet()) {
		}
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
		Instance instance = env.getInstance();
		Tables.clearCache(instance);
	}
}

