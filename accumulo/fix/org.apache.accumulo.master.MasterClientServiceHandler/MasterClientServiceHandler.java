

import com.google.protobuf.InvalidProtocolBufferException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.RowIterator;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.DelegationTokenConfigSerializer;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.thrift.TableOperation;
import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.client.impl.thrift.ThriftTableOperationException;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.thrift.TKeyExtent;
import org.apache.accumulo.core.master.thrift.MasterClientService;
import org.apache.accumulo.core.master.thrift.MasterGoalState;
import org.apache.accumulo.core.master.thrift.MasterMonitorInfo;
import org.apache.accumulo.core.master.thrift.TabletLoadState;
import org.apache.accumulo.core.master.thrift.TabletSplit;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.protobuf.ProtobufUtil;
import org.apache.accumulo.core.replication.ReplicationSchema;
import org.apache.accumulo.core.replication.ReplicationTable;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.security.thrift.TDelegationToken;
import org.apache.accumulo.core.security.thrift.TDelegationTokenConfig;
import org.apache.accumulo.core.trace.thrift.TInfo;
import org.apache.accumulo.core.util.ByteBufferUtil;
import org.apache.accumulo.core.util.ColumnFQ;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.server.client.ClientServiceHandler;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.replication.StatusUtil;
import org.apache.accumulo.server.replication.proto.Replication;
import org.apache.accumulo.server.util.NamespacePropUtil;
import org.apache.accumulo.server.util.SystemPropUtil;
import org.apache.accumulo.server.util.TabletIterator;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.hadoop.io.BinaryComparable;
import org.apache.hadoop.io.Text;
import org.apache.thrift.TException;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.core.metadata.schema.MetadataSchema.ReplicationSection.COLF;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.ReplicationSection.getRange;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.ReplicationSection.getRowPrefix;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.CurrentLocationColumnFamily.NAME;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.FLUSH_COLUMN;
import static org.apache.accumulo.core.replication.ReplicationSchema.OrderSection.getFile;
import static org.apache.accumulo.core.replication.ReplicationSchema.OrderSection.getTimeClosed;
import static org.apache.accumulo.server.replication.proto.Replication.Status.parseFrom;


public class MasterClientServiceHandler implements MasterClientService.Iface {
	private static final Logger drainLog = LoggerFactory.getLogger("org.apache.accumulo.master.MasterDrainImpl");

	private Instance instance;

	protected MasterClientServiceHandler(Master master) {
		this.instance = master.getInstance();
	}

	@Override
	public long initiateFlush(TInfo tinfo, TCredentials c, String tableId) throws ThriftSecurityException, ThriftTableOperationException {
		String namespaceId = getNamespaceIdFromTableId(TableOperation.FLUSH, tableId);
		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		byte[] fid;
		try {
		} catch (Exception e) {
			throw new ThriftTableOperationException(tableId, null, TableOperation.FLUSH, TableOperationExceptionType.OTHER, null);
		}
		fid = null;
		return Long.parseLong(new String(fid));
	}

	@Override
	public void waitForFlush(TInfo tinfo, TCredentials c, String tableId, ByteBuffer startRow, ByteBuffer endRow, long flushID, long maxLoops) throws ThriftSecurityException, ThriftTableOperationException {
		String namespaceId = getNamespaceIdFromTableId(TableOperation.FLUSH, tableId);
		if (((endRow != null) && (startRow != null)) && ((ByteBufferUtil.toText(startRow).compareTo(ByteBufferUtil.toText(endRow))) >= 0))
			throw new ThriftTableOperationException(tableId, null, TableOperation.FLUSH, TableOperationExceptionType.BAD_RANGE, "start row must be less than end row");

		for (long l = 0; l < maxLoops; l++) {
			if (l == (maxLoops - 1))
				break;

			UtilWaitThread.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
			try {
				Scanner scanner;
				if (tableId.equals(MetadataTable.ID)) {
					scanner = null;
					scanner.setRange(MetadataSchema.TabletsSection.getRange());
				}else {
					Range range = new KeyExtent(tableId, null, ByteBufferUtil.toText(startRow)).toMetadataRange();
					scanner = null;
					scanner.setRange(range.clip(MetadataSchema.TabletsSection.getRange()));
				}
				scanner = null;
				FLUSH_COLUMN.fetch(scanner);
				DIRECTORY_COLUMN.fetch(scanner);
				scanner.fetchColumnFamily(NAME);
				scanner.fetchColumnFamily(MetadataSchema.TabletsSection.LogColumnFamily.NAME);
				RowIterator ri = new RowIterator(scanner);
				int tabletsToWaitFor = 0;
				int tabletCount = 0;
				Text ert = ByteBufferUtil.toText(endRow);
				while (ri.hasNext()) {
					Iterator<Map.Entry<Key, Value>> row = ri.next();
					long tabletFlushID = -1;
					int logs = 0;
					boolean online = false;
					TServerInstance server = null;
					Map.Entry<Key, Value> entry = null;
					while (row.hasNext()) {
						entry = row.next();
						Key key = entry.getKey();
						if (FLUSH_COLUMN.equals(key.getColumnFamily(), key.getColumnQualifier())) {
							tabletFlushID = Long.parseLong(entry.getValue().toString());
						}
						if (MetadataSchema.TabletsSection.LogColumnFamily.NAME.equals(key.getColumnFamily()))
							logs++;

						if (NAME.equals(key.getColumnFamily())) {
							online = true;
							server = new TServerInstance(entry.getValue(), key.getColumnQualifier());
						}
					} 
					if ((online || (logs > 0)) && (tabletFlushID < flushID)) {
						tabletsToWaitFor++;
						if (server != null) {
						}
					}
					tabletCount++;
					Text tabletEndRow = new KeyExtent(entry.getKey().getRow(), ((Text) (null))).getEndRow();
					if ((tabletEndRow == null) || ((ert != null) && ((tabletEndRow.compareTo(ert)) >= 0)))
						break;

				} 
				if (tabletsToWaitFor == 0)
					break;

			} catch (TabletIterator.TabletDeletedException tde) {
			}
		}
	}

	private String getNamespaceIdFromTableId(TableOperation tableOp, String tableId) throws ThriftTableOperationException {
		String namespaceId;
		try {
			namespaceId = Tables.getNamespaceId(instance, tableId);
		} catch (TableNotFoundException e) {
			throw new ThriftTableOperationException(tableId, null, tableOp, TableOperationExceptionType.NOTFOUND, e.getMessage());
		}
		return namespaceId;
	}

	@Override
	public MasterMonitorInfo getMasterStats(TInfo info, TCredentials credentials) throws ThriftSecurityException {
		return null;
	}

	@Override
	public void removeTableProperty(TInfo info, TCredentials credentials, String tableName, String property) throws ThriftSecurityException, ThriftTableOperationException {
		alterTableProperty(credentials, tableName, property, null, TableOperation.REMOVE_PROPERTY);
	}

	@Override
	public void setTableProperty(TInfo info, TCredentials credentials, String tableName, String property, String value) throws ThriftSecurityException, ThriftTableOperationException {
		alterTableProperty(credentials, tableName, property, value, TableOperation.SET_PROPERTY);
	}

	@Override
	public void shutdown(TInfo info, TCredentials c, boolean stopTabletServers) throws ThriftSecurityException {
		if (stopTabletServers) {
		}
	}

	@Override
	public void shutdownTabletServer(TInfo info, TCredentials c, String tabletServer, boolean force) throws ThriftSecurityException {
		if (!force) {
		}
	}

	@Override
	public void reportSplitExtent(TInfo info, TCredentials credentials, String serverName, TabletSplit split) {
		KeyExtent oldTablet = new KeyExtent(split.oldTablet);
	}

	@Override
	public void reportTabletStatus(TInfo info, TCredentials credentials, String serverName, TabletLoadState status, TKeyExtent ttablet) {
		KeyExtent tablet = new KeyExtent(ttablet);
	}

	@Override
	public void setMasterGoalState(TInfo info, TCredentials c, MasterGoalState state) throws ThriftSecurityException {
	}

	@Override
	public void removeSystemProperty(TInfo info, TCredentials c, String property) throws ThriftSecurityException {
		try {
			SystemPropUtil.removeSystemProperty(property);
			updatePlugins(property);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public void setSystemProperty(TInfo info, TCredentials c, String property, String value) throws ThriftSecurityException, TException {
		try {
			SystemPropUtil.setSystemProperty(property, value);
			updatePlugins(property);
		} catch (IllegalArgumentException iae) {
			throw iae;
		} catch (Exception e) {
			throw new TException(e.getMessage());
		}
	}

	@Override
	public void setNamespaceProperty(TInfo tinfo, TCredentials credentials, String ns, String property, String value) throws ThriftSecurityException, ThriftTableOperationException {
		alterNamespaceProperty(credentials, ns, property, value, TableOperation.SET_PROPERTY);
	}

	@Override
	public void removeNamespaceProperty(TInfo tinfo, TCredentials credentials, String ns, String property) throws ThriftSecurityException, ThriftTableOperationException {
		alterNamespaceProperty(credentials, ns, property, null, TableOperation.REMOVE_PROPERTY);
	}

	private void alterNamespaceProperty(TCredentials c, String namespace, String property, String value, TableOperation op) throws ThriftSecurityException, ThriftTableOperationException {
		String namespaceId = null;
		try {
			if (value == null) {
				NamespacePropUtil.removeNamespaceProperty(namespaceId, property);
			}else {
				NamespacePropUtil.setNamespaceProperty(namespaceId, property, value);
			}
		} catch (KeeperException.NoNodeException e) {
			throw new ThriftTableOperationException(namespaceId, namespace, op, TableOperationExceptionType.OTHER, "Problem altering namespaceproperty");
		} catch (Exception e) {
			throw new ThriftTableOperationException(namespaceId, namespace, op, TableOperationExceptionType.OTHER, "Problem altering namespace property");
		}
	}

	private void alterTableProperty(TCredentials c, String tableName, String property, String value, TableOperation op) throws ThriftSecurityException, ThriftTableOperationException {
		try {
			if ((value == null) || (value.isEmpty())) {
			}else {
			}
		} catch (Exception e) {
		}
	}

	private void updatePlugins(String property) {
		if (property.equals(Property.MASTER_TABLET_BALANCER.getKey())) {
		}
	}

	@Override
	public void waitForBalance(TInfo tinfo) throws TException {
	}

	@Override
	public List<String> getActiveTservers(TInfo tinfo, TCredentials credentials) throws TException {
		List<String> servers = new ArrayList<>();
		return servers;
	}

	@Override
	public TDelegationToken getDelegationToken(TInfo tinfo, TCredentials credentials, TDelegationTokenConfig tConfig) throws ThriftSecurityException, TException {
		final DelegationTokenConfig config = DelegationTokenConfigSerializer.deserialize(tConfig);
		try {
		} catch (Exception e) {
			throw new TException(e.getMessage());
		}
		return null;
	}

	@Override
	public boolean drainReplicationTable(TInfo tfino, TCredentials credentials, String tableName, Set<String> logsToWatch) throws TException {
		Connector conn;
		MasterClientServiceHandler.drainLog.trace("Reading from metadata table");
		final Set<Range> range = Collections.singleton(new Range(getRange()));
		BatchScanner bs;
		try {
			conn = null;
			bs = conn.createBatchScanner(MetadataTable.NAME, Authorizations.EMPTY, 4);
		} catch (TableNotFoundException e) {
			throw new RuntimeException("Could not read metadata table", e);
		}
		bs.setRanges(range);
		bs.fetchColumnFamily(COLF);
		try {
		} finally {
			bs.close();
		}
		MasterClientServiceHandler.drainLog.trace("reading from replication table");
		try {
			conn = null;
			bs = conn.createBatchScanner(ReplicationTable.NAME, Authorizations.EMPTY, 4);
		} catch (TableNotFoundException e) {
			throw new RuntimeException("Replication table was not found", e);
		}
		bs.setRanges(Collections.singleton(new Range()));
		try {
		} finally {
			bs.close();
		}
		return false;
	}

	protected String getTableId(Instance instance, String tableName) throws ThriftTableOperationException {
		return ClientServiceHandler.checkTableId(instance, tableName, null);
	}

	protected boolean allReferencesReplicated(BatchScanner bs, Text tableId, Set<String> relevantLogs) {
		Text rowHolder = new Text();
		Text colfHolder = new Text();
		for (Map.Entry<Key, Value> entry : bs) {
			MasterClientServiceHandler.drainLog.trace("Got key {}", entry.getKey().toStringNoTruncate());
			entry.getKey().getColumnQualifier(rowHolder);
			if (tableId.equals(rowHolder)) {
				entry.getKey().getRow(rowHolder);
				entry.getKey().getColumnFamily(colfHolder);
				String file;
				if (colfHolder.equals(COLF)) {
					file = rowHolder.toString();
					file = file.substring(getRowPrefix().length());
				}else
					if (colfHolder.equals(ReplicationSchema.OrderSection.NAME)) {
						file = getFile(entry.getKey(), rowHolder);
						long timeClosed = getTimeClosed(entry.getKey(), rowHolder);
						MasterClientServiceHandler.drainLog.trace("Order section: {} and {}", timeClosed, file);
					}else {
						file = rowHolder.toString();
					}

				if (!(relevantLogs.contains(file))) {
					MasterClientServiceHandler.drainLog.trace("Found file that we didn't care about {}", file);
					continue;
				}else {
					MasterClientServiceHandler.drainLog.trace("Found file that we *do* care about {}", file);
				}
				try {
					Replication.Status stat = parseFrom(entry.getValue().get());
					if (!(StatusUtil.isFullyReplicated(stat))) {
						MasterClientServiceHandler.drainLog.trace("{} and {} is not replicated", file, ProtobufUtil.toString(stat));
						return false;
					}
					MasterClientServiceHandler.drainLog.trace("{} and {} is replicated", file, ProtobufUtil.toString(stat));
				} catch (InvalidProtocolBufferException e) {
					MasterClientServiceHandler.drainLog.trace("Could not parse protobuf for {}", entry.getKey(), e);
				}
			}
		}
		return true;
	}
}

