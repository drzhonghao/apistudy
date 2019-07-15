

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.fate.zookeeper.IZooReader;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.master.tableOps.Utils;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;


public class CancelCompactions extends MasterRepo {
	private static final long serialVersionUID = 1L;

	private String tableId;

	private String namespaceId;

	private String getNamespaceId(Master env) throws Exception {
		return Utils.getNamespaceId(env.getInstance(), tableId, TableOperation.COMPACT_CANCEL, this.namespaceId);
	}

	public CancelCompactions(String namespaceId, String tableId) {
		this.tableId = tableId;
		this.namespaceId = namespaceId;
	}

	@Override
	public long isReady(long tid, Master env) throws Exception {
		return (Utils.reserveNamespace(getNamespaceId(env), tid, false, true, TableOperation.COMPACT_CANCEL)) + (Utils.reserveTable(tableId, tid, false, true, TableOperation.COMPACT_CANCEL));
	}

	@Override
	public Repo<Master> call(long tid, Master environment) throws Exception {
		String zCompactID = ((((((Constants.ZROOT) + "/") + (environment.getInstance().getInstanceID())) + (Constants.ZTABLES)) + "/") + (tableId)) + (Constants.ZTABLE_COMPACT_ID);
		String zCancelID = ((((((Constants.ZROOT) + "/") + (environment.getInstance().getInstanceID())) + (Constants.ZTABLES)) + "/") + (tableId)) + (Constants.ZTABLE_COMPACT_CANCEL_ID);
		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		byte[] currentValue = zoo.getData(zCompactID, null);
		String cvs = new String(currentValue, StandardCharsets.UTF_8);
		String[] tokens = cvs.split(",");
		final long flushID = Long.parseLong(tokens[0]);
		zoo.mutate(zCancelID, null, null, new IZooReaderWriter.Mutator() {
			@Override
			public byte[] mutate(byte[] currentValue) throws Exception {
				long cid = Long.parseLong(new String(currentValue, StandardCharsets.UTF_8));
				if (cid < flushID)
					return Long.toString(flushID).getBytes(StandardCharsets.UTF_8);
				else
					return Long.toString(cid).getBytes(StandardCharsets.UTF_8);

			}
		});
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
		Utils.unreserveTable(tableId, tid, false);
		Utils.unreserveNamespace(getNamespaceId(env), tid, false);
	}
}

