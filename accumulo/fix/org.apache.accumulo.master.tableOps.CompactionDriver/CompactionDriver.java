

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IsolatedScanner;
import org.apache.accumulo.core.client.RowIterator;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.impl.AcceptableThriftTableOperationException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.ColumnFQ;
import org.apache.accumulo.core.util.MapCounter;
import org.apache.accumulo.fate.Repo;
import org.apache.accumulo.fate.zookeeper.IZooReader;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.tableOps.MasterRepo;
import org.apache.accumulo.master.tableOps.Utils;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.master.LiveTServerSet;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.zookeeper.ZooLock;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.hadoop.io.BinaryComparable;
import org.apache.hadoop.io.Text;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.CurrentLocationColumnFamily.NAME;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.COMPACT_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.getRange;


class CompactionDriver extends MasterRepo {
	private static final long serialVersionUID = 1L;

	private long compactId;

	private final String tableId;

	private final String namespaceId;

	private byte[] startRow;

	private byte[] endRow;

	private String getNamespaceId(Master env) throws Exception {
		return Utils.getNamespaceId(env.getInstance(), tableId, TableOperation.COMPACT, this.namespaceId);
	}

	public CompactionDriver(long compactId, String namespaceId, String tableId, byte[] startRow, byte[] endRow) {
		this.compactId = compactId;
		this.tableId = tableId;
		this.namespaceId = namespaceId;
		this.startRow = startRow;
		this.endRow = endRow;
	}

	@Override
	public long isReady(long tid, Master master) throws Exception {
		String zCancelID = ((((((Constants.ZROOT) + "/") + (master.getInstance().getInstanceID())) + (Constants.ZTABLES)) + "/") + (tableId)) + (Constants.ZTABLE_COMPACT_CANCEL_ID);
		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		if ((Long.parseLong(new String(zoo.getData(zCancelID, null)))) >= (compactId)) {
			throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.COMPACT, TableOperationExceptionType.OTHER, "Compaction canceled");
		}
		MapCounter<TServerInstance> serversToFlush = new MapCounter<>();
		Connector conn = master.getConnector();
		Scanner scanner;
		if (tableId.equals(MetadataTable.ID)) {
			scanner = new IsolatedScanner(conn.createScanner(RootTable.NAME, Authorizations.EMPTY));
			scanner.setRange(getRange());
		}else {
			scanner = new IsolatedScanner(conn.createScanner(MetadataTable.NAME, Authorizations.EMPTY));
			Range range = new KeyExtent(tableId, null, ((startRow) == null ? null : new Text(startRow))).toMetadataRange();
			scanner.setRange(range);
		}
		COMPACT_COLUMN.fetch(scanner);
		DIRECTORY_COLUMN.fetch(scanner);
		scanner.fetchColumnFamily(NAME);
		long t1 = System.currentTimeMillis();
		RowIterator ri = new RowIterator(scanner);
		int tabletsToWaitFor = 0;
		int tabletCount = 0;
		while (ri.hasNext()) {
			Iterator<Map.Entry<Key, Value>> row = ri.next();
			long tabletCompactID = -1;
			TServerInstance server = null;
			Map.Entry<Key, Value> entry = null;
			while (row.hasNext()) {
				entry = row.next();
				Key key = entry.getKey();
				if (COMPACT_COLUMN.equals(key.getColumnFamily(), key.getColumnQualifier()))
					tabletCompactID = Long.parseLong(entry.getValue().toString());

				if (NAME.equals(key.getColumnFamily()))
					server = new TServerInstance(entry.getValue(), key.getColumnQualifier());

			} 
			if (tabletCompactID < (compactId)) {
				tabletsToWaitFor++;
				if (server != null)
					serversToFlush.increment(server, 1);

			}
			tabletCount++;
			Text tabletEndRow = new KeyExtent(entry.getKey().getRow(), ((Text) (null))).getEndRow();
			if ((tabletEndRow == null) || (((endRow) != null) && ((tabletEndRow.compareTo(new Text(endRow))) >= 0)))
				break;

		} 
		long scanTime = (System.currentTimeMillis()) - t1;
		Instance instance = master.getInstance();
		Tables.clearCache(instance);
		if ((tabletCount == 0) && (!(Tables.exists(instance, tableId))))
			throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.COMPACT, TableOperationExceptionType.NOTFOUND, null);

		if (((serversToFlush.size()) == 0) && ((Tables.getTableState(instance, tableId)) == (TableState.OFFLINE)))
			throw new AcceptableThriftTableOperationException(tableId, null, TableOperation.COMPACT, TableOperationExceptionType.OFFLINE, null);

		if (tabletsToWaitFor == 0)
			return 0;

		for (TServerInstance tsi : serversToFlush.keySet()) {
			try {
				final LiveTServerSet.TServerConnection server = master.getConnection(tsi);
				if (server != null)
					server.compact(master.getMasterLock(), tableId, startRow, endRow);

			} catch (TException ex) {
				LoggerFactory.getLogger(CompactionDriver.class).error(ex.toString());
			}
		}
		long sleepTime = 500;
		if ((serversToFlush.size()) > 0)
			sleepTime = (Collections.max(serversToFlush.values())) * sleepTime;

		sleepTime = Math.max((2 * scanTime), sleepTime);
		sleepTime = Math.min(sleepTime, 30000);
		return sleepTime;
	}

	@Override
	public Repo<Master> call(long tid, Master env) throws Exception {
		Utils.getReadLock(tableId, tid).unlock();
		Utils.getReadLock(getNamespaceId(env), tid).unlock();
		return null;
	}

	@Override
	public void undo(long tid, Master environment) throws Exception {
	}
}

