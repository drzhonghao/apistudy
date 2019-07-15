

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.impl.AcceptableThriftTableOperationException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.master.tableOps.Utils;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RenameTable extends MasterRepo {
	private static final long serialVersionUID = 1L;

	private String tableId;

	private String namespaceId;

	private String oldTableName;

	private String newTableName;

	private String getNamespaceId(Master env) throws Exception {
		return Utils.getNamespaceId(env.getInstance(), tableId, TableOperation.RENAME, this.namespaceId);
	}

	@Override
	public long isReady(long tid, Master env) throws Exception {
		return (Utils.reserveNamespace(getNamespaceId(env), tid, false, true, TableOperation.RENAME)) + (Utils.reserveTable(tableId, tid, true, true, TableOperation.RENAME));
	}

	public RenameTable(String namespaceId, String tableId, String oldTableName, String newTableName) throws NamespaceNotFoundException {
		this.namespaceId = namespaceId;
		this.tableId = tableId;
		this.oldTableName = oldTableName;
		this.newTableName = newTableName;
	}

	@Override
	public Repo<Master> call(long tid, Master master) throws Exception {
		Instance instance = master.getInstance();
		String namespaceId = getNamespaceId(master);
		Pair<String, String> qualifiedOldTableName = Tables.qualify(oldTableName);
		Pair<String, String> qualifiedNewTableName = Tables.qualify(newTableName);
		if ((newTableName.contains(".")) && (!(namespaceId.equals(Namespaces.getNamespaceId(instance, qualifiedNewTableName.getFirst())))))
			throw new AcceptableThriftTableOperationException(tableId, oldTableName, TableOperation.RENAME, TableOperationExceptionType.INVALID_NAME, "Namespace in new table name does not match the old table name");

		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		try {
			final String newName = qualifiedNewTableName.getSecond();
			final String oldName = qualifiedOldTableName.getSecond();
			final String tap = ((((ZooUtil.getRoot(instance)) + (Constants.ZTABLES)) + "/") + (tableId)) + (Constants.ZTABLE_NAME);
			zoo.mutate(tap, null, null, new IZooReaderWriter.Mutator() {
				@Override
				public byte[] mutate(byte[] current) throws Exception {
					final String currentName = new String(current, StandardCharsets.UTF_8);
					if (currentName.equals(newName))
						return null;

					if (!(currentName.equals(oldName))) {
						throw new AcceptableThriftTableOperationException(null, oldTableName, TableOperation.RENAME, TableOperationExceptionType.NOTFOUND, "Name changed while processing");
					}
					return newName.getBytes(StandardCharsets.UTF_8);
				}
			});
			Tables.clearCache(instance);
		} finally {
			Utils.unreserveTable(tableId, tid, true);
			Utils.unreserveNamespace(namespaceId, tid, false);
		}
		LoggerFactory.getLogger(RenameTable.class).debug(((((("Renamed table " + (tableId)) + " ") + (oldTableName)) + " ") + (newTableName)));
		return null;
	}

	@Override
	public void undo(long tid, Master env) throws Exception {
		Utils.unreserveTable(tableId, tid, true);
		Utils.unreserveNamespace(getNamespaceId(env), tid, false);
	}
}

