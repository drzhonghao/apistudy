

import com.google.common.base.Preconditions;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.admin.ActiveCompaction;
import org.apache.accumulo.core.client.admin.ActiveScan;
import org.apache.accumulo.core.client.admin.InstanceOperations;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ClientExec;
import org.apache.accumulo.core.client.impl.ClientExecReturn;
import org.apache.accumulo.core.client.impl.MasterClient;
import org.apache.accumulo.core.client.impl.ServerClient;
import org.apache.accumulo.core.client.impl.thrift.ClientService;
import org.apache.accumulo.core.client.impl.thrift.ClientService.Client;
import org.apache.accumulo.core.client.impl.thrift.ConfigurationType;
import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.master.thrift.MasterClientService;
import org.apache.accumulo.core.master.thrift.MasterClientService.Client;
import org.apache.accumulo.core.master.thrift.TabletServerStatus;
import org.apache.accumulo.core.rpc.ThriftUtil;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.accumulo.core.trace.thrift.TInfo;
import org.apache.accumulo.core.util.AddressUtil;
import org.apache.accumulo.core.util.HostAndPort;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooCache;
import org.apache.accumulo.fate.zookeeper.ZooCacheFactory;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;


public class InstanceOperationsImpl implements InstanceOperations {
	private final ClientContext context;

	public InstanceOperationsImpl(ClientContext context) {
		Preconditions.checkArgument((context != null), "context is null");
		this.context = context;
	}

	@Override
	public void setProperty(final String property, final String value) throws IllegalArgumentException, AccumuloException, AccumuloSecurityException {
		Preconditions.checkArgument((property != null), "property is null");
		Preconditions.checkArgument((value != null), "value is null");
		MasterClient.execute(context, new ClientExec<MasterClientService.Client>() {
			@Override
			public void execute(MasterClientService.Client client) throws Exception {
				client.setSystemProperty(Tracer.traceInfo(), context.rpcCreds(), property, value);
			}
		});
	}

	@Override
	public void removeProperty(final String property) throws AccumuloException, AccumuloSecurityException {
		Preconditions.checkArgument((property != null), "property is null");
		MasterClient.execute(context, new ClientExec<MasterClientService.Client>() {
			@Override
			public void execute(MasterClientService.Client client) throws Exception {
				client.removeSystemProperty(Tracer.traceInfo(), context.rpcCreds(), property);
			}
		});
	}

	@Override
	public Map<String, String> getSystemConfiguration() throws AccumuloException, AccumuloSecurityException {
		return ServerClient.execute(context, new ClientExecReturn<Map<String, String>, ClientService.Client>() {
			@Override
			public Map<String, String> execute(ClientService.Client client) throws Exception {
				return client.getConfiguration(Tracer.traceInfo(), context.rpcCreds(), ConfigurationType.CURRENT);
			}
		});
	}

	@Override
	public Map<String, String> getSiteConfiguration() throws AccumuloException, AccumuloSecurityException {
		return ServerClient.execute(context, new ClientExecReturn<Map<String, String>, ClientService.Client>() {
			@Override
			public Map<String, String> execute(ClientService.Client client) throws Exception {
				return client.getConfiguration(Tracer.traceInfo(), context.rpcCreds(), ConfigurationType.SITE);
			}
		});
	}

	@Override
	public List<String> getTabletServers() {
		Instance instance = context.getInstance();
		ZooCache cache = new ZooCacheFactory().getZooCache(instance.getZooKeepers(), instance.getZooKeepersSessionTimeOut());
		String path = (ZooUtil.getRoot(instance)) + (Constants.ZTSERVERS);
		List<String> results = new ArrayList<>();
		for (String candidate : cache.getChildren(path)) {
			List<String> children = cache.getChildren(((path + "/") + candidate));
			if ((children != null) && ((children.size()) > 0)) {
				List<String> copy = new ArrayList<>(children);
				Collections.sort(copy);
				byte[] data = cache.get(((((path + "/") + candidate) + "/") + (copy.get(0))));
				if ((data != null) && (!("master".equals(new String(data, StandardCharsets.UTF_8))))) {
					results.add(candidate);
				}
			}
		}
		return results;
	}

	@Override
	public List<ActiveScan> getActiveScans(String tserver) throws AccumuloException, AccumuloSecurityException {
		final HostAndPort parsedTserver = HostAndPort.fromString(tserver);
		TabletClientService.Client client = null;
		try {
			client = ThriftUtil.getTServerClient(parsedTserver, context);
			List<ActiveScan> as = new ArrayList<>();
			for (org.apache.accumulo.core.tabletserver.thrift.ActiveScan activeScan : client.getActiveScans(Tracer.traceInfo(), context.rpcCreds())) {
			}
			return as;
		} catch (TTransportException e) {
			throw new AccumuloException(e);
		} catch (ThriftSecurityException e) {
			throw new AccumuloSecurityException(e.user, e.code, e);
		} catch (TException e) {
			throw new AccumuloException(e);
		} finally {
			if (client != null)
				ThriftUtil.returnClient(client);

		}
	}

	@Override
	public boolean testClassLoad(final String className, final String asTypeName) throws AccumuloException, AccumuloSecurityException {
		return ServerClient.execute(context, new ClientExecReturn<Boolean, ClientService.Client>() {
			@Override
			public Boolean execute(ClientService.Client client) throws Exception {
				return client.checkClass(Tracer.traceInfo(), context.rpcCreds(), className, asTypeName);
			}
		});
	}

	@Override
	public List<ActiveCompaction> getActiveCompactions(String tserver) throws AccumuloException, AccumuloSecurityException {
		final HostAndPort parsedTserver = HostAndPort.fromString(tserver);
		TabletClientService.Client client = null;
		try {
			client = ThriftUtil.getTServerClient(parsedTserver, context);
			List<ActiveCompaction> as = new ArrayList<>();
			for (org.apache.accumulo.core.tabletserver.thrift.ActiveCompaction activeCompaction : client.getActiveCompactions(Tracer.traceInfo(), context.rpcCreds())) {
			}
			return as;
		} catch (TTransportException e) {
			throw new AccumuloException(e);
		} catch (ThriftSecurityException e) {
			throw new AccumuloSecurityException(e.user, e.code, e);
		} catch (TException e) {
			throw new AccumuloException(e);
		} finally {
			if (client != null)
				ThriftUtil.returnClient(client);

		}
	}

	@Override
	public void ping(String tserver) throws AccumuloException {
		TTransport transport = null;
		try {
			transport = ThriftUtil.createTransport(AddressUtil.parseAddress(tserver, false), context);
			TabletClientService.Client client = ThriftUtil.createClient(new TabletClientService.Client.Factory(), transport);
			client.getTabletServerStatus(Tracer.traceInfo(), context.rpcCreds());
		} catch (TTransportException e) {
			throw new AccumuloException(e);
		} catch (ThriftSecurityException e) {
			throw new AccumuloException(e);
		} catch (TException e) {
			throw new AccumuloException(e);
		} finally {
			if (transport != null) {
				transport.close();
			}
		}
	}

	@Override
	public void waitForBalance() throws AccumuloException {
		try {
			MasterClient.execute(context, new ClientExec<MasterClientService.Client>() {
				@Override
				public void execute(MasterClientService.Client client) throws Exception {
					client.waitForBalance(Tracer.traceInfo());
				}
			});
		} catch (AccumuloSecurityException ex) {
			throw new RuntimeException("Unexpected exception thrown", ex);
		}
	}

	public static String lookupInstanceName(ZooCache zooCache, UUID instanceId) {
		Preconditions.checkArgument((zooCache != null), "zooCache is null");
		Preconditions.checkArgument((instanceId != null), "instanceId is null");
		for (String name : zooCache.getChildren(((Constants.ZROOT) + (Constants.ZINSTANCES)))) {
			String instanceNamePath = (((Constants.ZROOT) + (Constants.ZINSTANCES)) + "/") + name;
			byte[] bytes = zooCache.get(instanceNamePath);
			UUID iid = UUID.fromString(new String(bytes, StandardCharsets.UTF_8));
			if (iid.equals(instanceId)) {
				return name;
			}
		}
		return null;
	}
}

