

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.NamespaceExistsException;
import org.apache.accumulo.core.client.NamespaceNotEmptyException;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ClientExec;
import org.apache.accumulo.core.client.impl.ClientExecReturn;
import org.apache.accumulo.core.client.impl.Credentials;
import org.apache.accumulo.core.client.impl.MasterClient;
import org.apache.accumulo.core.client.impl.NamespaceOperationsHelper;
import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.core.client.impl.ServerClient;
import org.apache.accumulo.core.client.impl.TableOperationsImpl;
import org.apache.accumulo.core.client.impl.thrift.ClientService;
import org.apache.accumulo.core.client.impl.thrift.ClientService.Client;
import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.client.impl.thrift.ThriftTableOperationException;
import org.apache.accumulo.core.constraints.Constraint;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.master.thrift.FateOperation;
import org.apache.accumulo.core.master.thrift.MasterClientService;
import org.apache.accumulo.core.master.thrift.MasterClientService.Client;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.accumulo.core.trace.thrift.TInfo;
import org.apache.accumulo.core.util.OpTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NamespaceOperationsImpl extends NamespaceOperationsHelper {
	private final ClientContext context;

	private TableOperationsImpl tableOps;

	private static final Logger log = LoggerFactory.getLogger(TableOperations.class);

	public NamespaceOperationsImpl(ClientContext context, TableOperationsImpl tableOps) {
		Preconditions.checkArgument((context != null), "context is null");
		this.context = context;
		this.tableOps = tableOps;
	}

	@Override
	public SortedSet<String> list() {
		OpTimer timer = null;
		if (NamespaceOperationsImpl.log.isTraceEnabled()) {
			NamespaceOperationsImpl.log.trace("tid={} Fetching list of namespaces...", Thread.currentThread().getId());
			timer = new OpTimer().start();
		}
		TreeSet<String> namespaces = new TreeSet<>(Namespaces.getNameToIdMap(context.getInstance()).keySet());
		if (timer != null) {
			timer.stop();
			NamespaceOperationsImpl.log.trace("tid={} Fetched {} namespaces in {}", Thread.currentThread().getId(), namespaces.size());
		}
		return namespaces;
	}

	@Override
	public boolean exists(String namespace) {
		Preconditions.checkArgument((namespace != null), "namespace is null");
		OpTimer timer = null;
		if (NamespaceOperationsImpl.log.isTraceEnabled()) {
			NamespaceOperationsImpl.log.trace("tid={} Checking if namespace {} exists", Thread.currentThread().getId(), namespace);
			timer = new OpTimer().start();
		}
		boolean exists = Namespaces.getNameToIdMap(context.getInstance()).containsKey(namespace);
		if (timer != null) {
			timer.stop();
			NamespaceOperationsImpl.log.trace("tid={} Checked existance of {} in {}", Thread.currentThread().getId(), exists);
		}
		return exists;
	}

	@Override
	public void create(String namespace) throws AccumuloException, AccumuloSecurityException, NamespaceExistsException {
		Preconditions.checkArgument((namespace != null), "namespace is null");
		try {
			doNamespaceFateOperation(FateOperation.NAMESPACE_CREATE, Arrays.asList(ByteBuffer.wrap(namespace.getBytes(StandardCharsets.UTF_8))), Collections.<String, String>emptyMap(), namespace);
		} catch (NamespaceNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public void delete(String namespace) throws AccumuloException, AccumuloSecurityException, NamespaceNotEmptyException, NamespaceNotFoundException {
		Preconditions.checkArgument((namespace != null), "namespace is null");
		String namespaceId = Namespaces.getNamespaceId(context.getInstance(), namespace);
		if ((namespaceId.equals(Namespaces.ACCUMULO_NAMESPACE_ID)) || (namespaceId.equals(Namespaces.DEFAULT_NAMESPACE_ID))) {
			Credentials credentials = context.getCredentials();
			NamespaceOperationsImpl.log.debug("{} attempted to delete the {} namespace", credentials.getPrincipal(), namespaceId);
			throw new AccumuloSecurityException(credentials.getPrincipal(), SecurityErrorCode.UNSUPPORTED_OPERATION);
		}
		if ((Namespaces.getTableIds(context.getInstance(), namespaceId).size()) > 0) {
			throw new NamespaceNotEmptyException(namespaceId, namespace, null);
		}
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(namespace.getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = new HashMap<>();
		try {
			doNamespaceFateOperation(FateOperation.NAMESPACE_DELETE, args, opts, namespace);
		} catch (NamespaceExistsException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public void rename(String oldNamespaceName, String newNamespaceName) throws AccumuloException, AccumuloSecurityException, NamespaceExistsException, NamespaceNotFoundException {
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(oldNamespaceName.getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap(newNamespaceName.getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = new HashMap<>();
		doNamespaceFateOperation(FateOperation.NAMESPACE_RENAME, args, opts, oldNamespaceName);
	}

	@Override
	public void setProperty(final String namespace, final String property, final String value) throws AccumuloException, AccumuloSecurityException, NamespaceNotFoundException {
		Preconditions.checkArgument((namespace != null), "namespace is null");
		Preconditions.checkArgument((property != null), "property is null");
		Preconditions.checkArgument((value != null), "value is null");
		MasterClient.executeNamespace(context, new ClientExec<MasterClientService.Client>() {
			@Override
			public void execute(MasterClientService.Client client) throws Exception {
				client.setNamespaceProperty(Tracer.traceInfo(), context.rpcCreds(), namespace, property, value);
			}
		});
	}

	@Override
	public void removeProperty(final String namespace, final String property) throws AccumuloException, AccumuloSecurityException, NamespaceNotFoundException {
		Preconditions.checkArgument((namespace != null), "namespace is null");
		Preconditions.checkArgument((property != null), "property is null");
		MasterClient.executeNamespace(context, new ClientExec<MasterClientService.Client>() {
			@Override
			public void execute(MasterClientService.Client client) throws Exception {
				client.removeNamespaceProperty(Tracer.traceInfo(), context.rpcCreds(), namespace, property);
			}
		});
	}

	@Override
	public Iterable<Map.Entry<String, String>> getProperties(final String namespace) throws AccumuloException, NamespaceNotFoundException {
		Preconditions.checkArgument((namespace != null), "namespace is null");
		try {
			return ServerClient.executeRaw(context, new ClientExecReturn<Map<String, String>, ClientService.Client>() {
				@Override
				public Map<String, String> execute(ClientService.Client client) throws Exception {
					return client.getNamespaceConfiguration(Tracer.traceInfo(), context.rpcCreds(), namespace);
				}
			}).entrySet();
		} catch (ThriftTableOperationException e) {
			switch (e.getType()) {
				case NAMESPACE_NOTFOUND :
					throw new NamespaceNotFoundException(e);
				case OTHER :
				default :
					throw new AccumuloException(e.description, e);
			}
		} catch (AccumuloException e) {
			throw e;
		} catch (Exception e) {
			throw new AccumuloException(e);
		}
	}

	@Override
	public Map<String, String> namespaceIdMap() {
		return Namespaces.getNameToIdMap(context.getInstance());
	}

	@Override
	public boolean testClassLoad(final String namespace, final String className, final String asTypeName) throws AccumuloException, AccumuloSecurityException, NamespaceNotFoundException {
		Preconditions.checkArgument((namespace != null), "namespace is null");
		Preconditions.checkArgument((className != null), "className is null");
		Preconditions.checkArgument((asTypeName != null), "asTypeName is null");
		try {
			return ServerClient.executeRaw(context, new ClientExecReturn<Boolean, ClientService.Client>() {
				@Override
				public Boolean execute(ClientService.Client client) throws Exception {
					return client.checkNamespaceClass(Tracer.traceInfo(), context.rpcCreds(), namespace, className, asTypeName);
				}
			});
		} catch (ThriftTableOperationException e) {
			switch (e.getType()) {
				case NAMESPACE_NOTFOUND :
					throw new NamespaceNotFoundException(e);
				default :
					throw new AccumuloException(e.description, e);
			}
		} catch (ThriftSecurityException e) {
			throw new AccumuloSecurityException(e.user, e.code, e);
		} catch (AccumuloException e) {
			throw e;
		} catch (Exception e) {
			throw new AccumuloException(e);
		}
	}

	@Override
	public void attachIterator(String namespace, IteratorSetting setting, EnumSet<IteratorUtil.IteratorScope> scopes) throws AccumuloException, AccumuloSecurityException, NamespaceNotFoundException {
		testClassLoad(namespace, setting.getIteratorClass(), SortedKeyValueIterator.class.getName());
		super.attachIterator(namespace, setting, scopes);
	}

	@Override
	public int addConstraint(String namespace, String constraintClassName) throws AccumuloException, AccumuloSecurityException, NamespaceNotFoundException {
		testClassLoad(namespace, constraintClassName, Constraint.class.getName());
		return super.addConstraint(namespace, constraintClassName);
	}

	private String doNamespaceFateOperation(FateOperation op, List<ByteBuffer> args, Map<String, String> opts, String namespace) throws AccumuloException, AccumuloSecurityException, NamespaceExistsException, NamespaceNotFoundException {
		return null;
	}
}

