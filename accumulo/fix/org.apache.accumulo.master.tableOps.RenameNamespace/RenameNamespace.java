

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.AcceptableThriftTableOperationException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.master.tableOps.Utils;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RenameNamespace extends MasterRepo {
	private static final long serialVersionUID = 1L;

	private String namespaceId;

	private String oldName;

	private String newName;

	@Override
	public long isReady(long id, Master environment) throws Exception {
		return Utils.reserveNamespace(namespaceId, id, true, true, TableOperation.RENAME);
	}

	public RenameNamespace(String namespaceId, String oldName, String newName) {
		this.namespaceId = namespaceId;
		this.oldName = oldName;
		this.newName = newName;
	}

	@Override
	public Repo<Master> call(long id, Master master) throws Exception {
		Instance instance = master.getInstance();
		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		try {
			final String tap = ((((ZooUtil.getRoot(instance)) + (Constants.ZNAMESPACES)) + "/") + (namespaceId)) + (Constants.ZNAMESPACE_NAME);
			zoo.mutate(tap, null, null, new IZooReaderWriter.Mutator() {
				@Override
				public byte[] mutate(byte[] current) throws Exception {
					final String currentName = new String(current);
					if (currentName.equals(newName))
						return null;

					if (!(currentName.equals(oldName))) {
						throw new AcceptableThriftTableOperationException(null, oldName, TableOperation.RENAME, TableOperationExceptionType.NAMESPACE_NOTFOUND, "Name changed while processing");
					}
					return newName.getBytes();
				}
			});
			Tables.clearCache(instance);
		} finally {
			Utils.unreserveNamespace(namespaceId, id, true);
		}
		LoggerFactory.getLogger(RenameNamespace.class).debug(((((("Renamed namespace " + (namespaceId)) + " ") + (oldName)) + " ") + (newName)));
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
		Utils.unreserveNamespace(namespaceId, tid, true);
	}
}

