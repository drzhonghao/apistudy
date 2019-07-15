

import com.google.common.base.Preconditions;
import java.util.concurrent.TimeUnit;
import javax.security.auth.Destroyable;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchDeleter;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.ConditionalWriter;
import org.apache.accumulo.core.client.ConditionalWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.MultiTableBatchWriter;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.admin.InstanceOperations;
import org.apache.accumulo.core.client.admin.NamespaceOperations;
import org.apache.accumulo.core.client.admin.ReplicationOperations;
import org.apache.accumulo.core.client.admin.SecurityOperations;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.client.impl.BatchWriterImpl;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ClientExec;
import org.apache.accumulo.core.client.impl.Credentials;
import org.apache.accumulo.core.client.impl.InstanceOperationsImpl;
import org.apache.accumulo.core.client.impl.MultiTableBatchWriterImpl;
import org.apache.accumulo.core.client.impl.NamespaceOperationsImpl;
import org.apache.accumulo.core.client.impl.ReplicationOperationsImpl;
import org.apache.accumulo.core.client.impl.ScannerImpl;
import org.apache.accumulo.core.client.impl.SecurityOperationsImpl;
import org.apache.accumulo.core.client.impl.ServerClient;
import org.apache.accumulo.core.client.impl.TableOperationsImpl;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.TabletServerBatchDeleter;
import org.apache.accumulo.core.client.impl.TabletServerBatchReader;
import org.apache.accumulo.core.client.impl.thrift.ClientService;
import org.apache.accumulo.core.client.impl.thrift.ClientService.Client;
import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.accumulo.core.trace.thrift.TInfo;


public class ConnectorImpl extends Connector {
	private static final String SYSTEM_TOKEN_NAME = "org.apache.accumulo.server.security." + "SystemCredentials$SystemToken";

	private final ClientContext context;

	private SecurityOperations secops = null;

	private TableOperationsImpl tableops = null;

	private NamespaceOperations namespaceops = null;

	private InstanceOperations instanceops = null;

	private ReplicationOperations replicationops = null;

	public ConnectorImpl(final ClientContext context) throws AccumuloException, AccumuloSecurityException {
		Preconditions.checkArgument((context != null), "Context is null");
		Preconditions.checkArgument(((context.getCredentials()) != null), "Credentials are null");
		Preconditions.checkArgument(((context.getCredentials().getToken()) != null), "Authentication token is null");
		if (context.getCredentials().getToken().isDestroyed())
			throw new AccumuloSecurityException(context.getCredentials().getPrincipal(), SecurityErrorCode.TOKEN_EXPIRED);

		this.context = context;
		final String tokenClassName = context.getCredentials().getToken().getClass().getName();
		if (!(ConnectorImpl.SYSTEM_TOKEN_NAME.equals(tokenClassName))) {
			ServerClient.execute(context, new ClientExec<ClientService.Client>() {
				@Override
				public void execute(ClientService.Client iface) throws Exception {
					if (!(iface.authenticate(Tracer.traceInfo(), context.rpcCreds())))
						throw new AccumuloSecurityException("Authentication failed, access denied", SecurityErrorCode.BAD_CREDENTIALS);

				}
			});
		}
		this.tableops = new TableOperationsImpl(context);
		this.namespaceops = new NamespaceOperationsImpl(context, tableops);
	}

	private String getTableId(String tableName) throws TableNotFoundException {
		String tableId = Tables.getTableId(context.getInstance(), tableName);
		if ((Tables.getTableState(context.getInstance(), tableId)) == (TableState.OFFLINE))
			throw new TableOfflineException(context.getInstance(), tableId);

		return tableId;
	}

	@Override
	public Instance getInstance() {
		return context.getInstance();
	}

	@Override
	public BatchScanner createBatchScanner(String tableName, Authorizations authorizations, int numQueryThreads) throws TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((authorizations != null), "authorizations is null");
		return new TabletServerBatchReader(context, getTableId(tableName), authorizations, numQueryThreads);
	}

	@Deprecated
	@Override
	public BatchDeleter createBatchDeleter(String tableName, Authorizations authorizations, int numQueryThreads, long maxMemory, long maxLatency, int maxWriteThreads) throws TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((authorizations != null), "authorizations is null");
		return new TabletServerBatchDeleter(context, getTableId(tableName), authorizations, numQueryThreads, new BatchWriterConfig().setMaxMemory(maxMemory).setMaxLatency(maxLatency, TimeUnit.MILLISECONDS).setMaxWriteThreads(maxWriteThreads));
	}

	@Override
	public BatchDeleter createBatchDeleter(String tableName, Authorizations authorizations, int numQueryThreads, BatchWriterConfig config) throws TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((authorizations != null), "authorizations is null");
		return new TabletServerBatchDeleter(context, getTableId(tableName), authorizations, numQueryThreads, config);
	}

	@Deprecated
	@Override
	public BatchWriter createBatchWriter(String tableName, long maxMemory, long maxLatency, int maxWriteThreads) throws TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		return new BatchWriterImpl(context, getTableId(tableName), new BatchWriterConfig().setMaxMemory(maxMemory).setMaxLatency(maxLatency, TimeUnit.MILLISECONDS).setMaxWriteThreads(maxWriteThreads));
	}

	@Override
	public BatchWriter createBatchWriter(String tableName, BatchWriterConfig config) throws TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		return new BatchWriterImpl(context, getTableId(tableName), config);
	}

	@Deprecated
	@Override
	public MultiTableBatchWriter createMultiTableBatchWriter(long maxMemory, long maxLatency, int maxWriteThreads) {
		return new MultiTableBatchWriterImpl(context, new BatchWriterConfig().setMaxMemory(maxMemory).setMaxLatency(maxLatency, TimeUnit.MILLISECONDS).setMaxWriteThreads(maxWriteThreads));
	}

	@Override
	public MultiTableBatchWriter createMultiTableBatchWriter(BatchWriterConfig config) {
		return new MultiTableBatchWriterImpl(context, config);
	}

	@Override
	public ConditionalWriter createConditionalWriter(String tableName, ConditionalWriterConfig config) throws TableNotFoundException {
		return null;
	}

	@Override
	public Scanner createScanner(String tableName, Authorizations authorizations) throws TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((authorizations != null), "authorizations is null");
		return new ScannerImpl(context, getTableId(tableName), authorizations);
	}

	@Override
	public String whoami() {
		return context.getCredentials().getPrincipal();
	}

	@Override
	public synchronized TableOperations tableOperations() {
		return tableops;
	}

	@Override
	public synchronized NamespaceOperations namespaceOperations() {
		return namespaceops;
	}

	@Override
	public synchronized SecurityOperations securityOperations() {
		if ((secops) == null)
			secops = new SecurityOperationsImpl(context);

		return secops;
	}

	@Override
	public synchronized InstanceOperations instanceOperations() {
		if ((instanceops) == null)
			instanceops = new InstanceOperationsImpl(context);

		return instanceops;
	}

	@Override
	public synchronized ReplicationOperations replicationOperations() {
		if (null == (replicationops)) {
			replicationops = new ReplicationOperationsImpl(context);
		}
		return replicationops;
	}
}

