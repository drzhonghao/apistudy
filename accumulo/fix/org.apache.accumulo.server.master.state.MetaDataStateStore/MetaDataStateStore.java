

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.tabletserver.log.LogEntry;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.master.state.Assignment;
import org.apache.accumulo.server.master.state.ClosableIterator;
import org.apache.accumulo.server.master.state.CurrentState;
import org.apache.accumulo.server.master.state.DistributedStoreException;
import org.apache.accumulo.server.master.state.SuspendingTServer;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.master.state.TabletLocationState;
import org.apache.accumulo.server.master.state.TabletStateStore;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;


public class MetaDataStateStore extends TabletStateStore {
	private static final int THREADS = 4;

	private static final int LATENCY = 1000;

	private static final int MAX_MEMORY = (200 * 1024) * 1024;

	protected final ClientContext context;

	protected final CurrentState state;

	private final String targetTableName;

	protected MetaDataStateStore(ClientContext context, CurrentState state, String targetTableName) {
		this.context = context;
		this.state = state;
		this.targetTableName = targetTableName;
	}

	public MetaDataStateStore(ClientContext context, CurrentState state) {
		this(context, state, MetadataTable.NAME);
	}

	protected MetaDataStateStore(AccumuloServerContext context, String tableName) {
		this(context, null, tableName);
	}

	public MetaDataStateStore(AccumuloServerContext context) {
		this(context, MetadataTable.NAME);
	}

	@Override
	public ClosableIterator<TabletLocationState> iterator() {
		return null;
	}

	@Override
	public void setLocations(Collection<Assignment> assignments) throws DistributedStoreException {
		BatchWriter writer = createBatchWriter();
		try {
			for (Assignment assignment : assignments) {
				Mutation m = new Mutation(assignment.tablet.getMetadataEntry());
				assignment.server.putLocation(m);
				assignment.server.clearFutureLocation(m);
				SuspendingTServer.clearSuspension(m);
				writer.addMutation(m);
			}
		} catch (Exception ex) {
			throw new DistributedStoreException(ex);
		} finally {
			try {
				writer.close();
			} catch (MutationsRejectedException e) {
				throw new DistributedStoreException(e);
			}
		}
	}

	BatchWriter createBatchWriter() {
		try {
			return context.getConnector().createBatchWriter(targetTableName, new BatchWriterConfig().setMaxMemory(MetaDataStateStore.MAX_MEMORY).setMaxLatency(MetaDataStateStore.LATENCY, TimeUnit.MILLISECONDS).setMaxWriteThreads(MetaDataStateStore.THREADS));
		} catch (TableNotFoundException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setFutureLocations(Collection<Assignment> assignments) throws DistributedStoreException {
		BatchWriter writer = createBatchWriter();
		try {
			for (Assignment assignment : assignments) {
				Mutation m = new Mutation(assignment.tablet.getMetadataEntry());
				SuspendingTServer.clearSuspension(m);
				assignment.server.putFutureLocation(m);
				writer.addMutation(m);
			}
		} catch (Exception ex) {
			throw new DistributedStoreException(ex);
		} finally {
			try {
				writer.close();
			} catch (MutationsRejectedException e) {
				throw new DistributedStoreException(e);
			}
		}
	}

	@Override
	public void unassign(Collection<TabletLocationState> tablets, Map<TServerInstance, List<Path>> logsForDeadServers) throws DistributedStoreException {
		suspend(tablets, logsForDeadServers, (-1));
	}

	@Override
	public void suspend(Collection<TabletLocationState> tablets, Map<TServerInstance, List<Path>> logsForDeadServers, long suspensionTimestamp) throws DistributedStoreException {
		BatchWriter writer = createBatchWriter();
		try {
			for (TabletLocationState tls : tablets) {
				Mutation m = new Mutation(tls.extent.getMetadataEntry());
				if ((tls.current) != null) {
					tls.current.clearLocation(m);
					if (logsForDeadServers != null) {
						List<Path> logs = logsForDeadServers.get(tls.current);
						if (logs != null) {
							for (Path log : logs) {
								LogEntry entry = new LogEntry(tls.extent, 0, tls.current.hostPort(), log.toString());
								m.put(entry.getColumnFamily(), entry.getColumnQualifier(), entry.getValue());
							}
						}
					}
					if (suspensionTimestamp >= 0) {
					}
				}
				if (((tls.suspend) != null) && (suspensionTimestamp < 0)) {
					SuspendingTServer.clearSuspension(m);
				}
				if ((tls.future) != null) {
					tls.future.clearFutureLocation(m);
				}
				writer.addMutation(m);
			}
		} catch (Exception ex) {
			throw new DistributedStoreException(ex);
		} finally {
			try {
				writer.close();
			} catch (MutationsRejectedException e) {
				throw new DistributedStoreException(e);
			}
		}
	}

	@Override
	public void unsuspend(Collection<TabletLocationState> tablets) throws DistributedStoreException {
		BatchWriter writer = createBatchWriter();
		try {
			for (TabletLocationState tls : tablets) {
				if ((tls.suspend) != null) {
					continue;
				}
				Mutation m = new Mutation(tls.extent.getMetadataEntry());
				SuspendingTServer.clearSuspension(m);
				writer.addMutation(m);
			}
		} catch (Exception ex) {
			throw new DistributedStoreException(ex);
		} finally {
			try {
				writer.close();
			} catch (MutationsRejectedException e) {
				throw new DistributedStoreException(e);
			}
		}
	}

	@Override
	public String name() {
		return "Normal Tablets";
	}
}

