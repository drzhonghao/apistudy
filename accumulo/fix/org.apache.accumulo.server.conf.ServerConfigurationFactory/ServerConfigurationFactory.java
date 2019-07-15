

import java.security.SecurityPermission;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigSanityCheck;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.conf.ObservableConfiguration;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.fate.zookeeper.ZooCacheFactory;
import org.apache.accumulo.server.conf.NamespaceConfiguration;
import org.apache.accumulo.server.conf.ServerConfiguration;
import org.apache.accumulo.server.conf.TableConfiguration;


public class ServerConfigurationFactory extends ServerConfiguration {
	private static final Map<String, Map<String, TableConfiguration>> tableConfigs = new HashMap<>(1);

	private static final Map<String, Map<String, NamespaceConfiguration>> namespaceConfigs = new HashMap<>(1);

	private static final Map<String, Map<String, NamespaceConfiguration>> tableParentConfigs = new HashMap<>(1);

	private static void addInstanceToCaches(String iid) {
		synchronized(ServerConfigurationFactory.tableConfigs) {
			if (!(ServerConfigurationFactory.tableConfigs.containsKey(iid))) {
				ServerConfigurationFactory.tableConfigs.put(iid, new HashMap<String, TableConfiguration>());
			}
		}
		synchronized(ServerConfigurationFactory.namespaceConfigs) {
			if (!(ServerConfigurationFactory.namespaceConfigs.containsKey(iid))) {
				ServerConfigurationFactory.namespaceConfigs.put(iid, new HashMap<String, NamespaceConfiguration>());
			}
		}
		synchronized(ServerConfigurationFactory.tableParentConfigs) {
			if (!(ServerConfigurationFactory.tableParentConfigs.containsKey(iid))) {
				ServerConfigurationFactory.tableParentConfigs.put(iid, new HashMap<String, NamespaceConfiguration>());
			}
		}
	}

	private static final SecurityPermission CONFIGURATION_PERMISSION = new SecurityPermission("configurationPermission");

	private static final SecurityManager SM = System.getSecurityManager();

	private static void checkPermissions() {
		if ((ServerConfigurationFactory.SM) != null) {
			ServerConfigurationFactory.SM.checkPermission(ServerConfigurationFactory.CONFIGURATION_PERMISSION);
		}
	}

	static boolean removeCachedTableConfiguration(String instanceId, String tableId) {
		synchronized(ServerConfigurationFactory.tableConfigs) {
			return (ServerConfigurationFactory.tableConfigs.get(instanceId).remove(tableId)) != null;
		}
	}

	static boolean removeCachedNamespaceConfiguration(String instanceId, String namespaceId) {
		synchronized(ServerConfigurationFactory.namespaceConfigs) {
			return (ServerConfigurationFactory.namespaceConfigs.get(instanceId).remove(namespaceId)) != null;
		}
	}

	static void clearCachedConfigurations() {
		synchronized(ServerConfigurationFactory.tableConfigs) {
			ServerConfigurationFactory.tableConfigs.clear();
		}
		synchronized(ServerConfigurationFactory.namespaceConfigs) {
			ServerConfigurationFactory.namespaceConfigs.clear();
		}
		synchronized(ServerConfigurationFactory.tableParentConfigs) {
			ServerConfigurationFactory.tableParentConfigs.clear();
		}
	}

	static void expireAllTableObservers() {
		synchronized(ServerConfigurationFactory.tableConfigs) {
			for (Map<String, TableConfiguration> instanceMap : ServerConfigurationFactory.tableConfigs.values()) {
				for (TableConfiguration c : instanceMap.values()) {
					c.expireAllObservers();
				}
			}
		}
	}

	private final Instance instance;

	private final String instanceID;

	private ZooCacheFactory zcf = new ZooCacheFactory();

	public ServerConfigurationFactory(Instance instance) {
		this.instance = instance;
		instanceID = instance.getInstanceID();
		ServerConfigurationFactory.addInstanceToCaches(instanceID);
	}

	void setZooCacheFactory(ZooCacheFactory zcf) {
		this.zcf = zcf;
	}

	private SiteConfiguration siteConfig = null;

	private DefaultConfiguration defaultConfig = null;

	private AccumuloConfiguration systemConfig = null;

	public synchronized SiteConfiguration getSiteConfiguration() {
		if ((siteConfig) == null) {
			ServerConfigurationFactory.checkPermissions();
			siteConfig = SiteConfiguration.getInstance();
		}
		return siteConfig;
	}

	public synchronized DefaultConfiguration getDefaultConfiguration() {
		if ((defaultConfig) == null) {
			ServerConfigurationFactory.checkPermissions();
			defaultConfig = DefaultConfiguration.getInstance();
		}
		return defaultConfig;
	}

	@Override
	public synchronized AccumuloConfiguration getConfiguration() {
		if ((systemConfig) == null) {
			ServerConfigurationFactory.checkPermissions();
		}
		return systemConfig;
	}

	@Override
	public TableConfiguration getTableConfiguration(String tableId) {
		ServerConfigurationFactory.checkPermissions();
		TableConfiguration conf;
		synchronized(ServerConfigurationFactory.tableConfigs) {
			conf = ServerConfigurationFactory.tableConfigs.get(instanceID).get(tableId);
		}
		if ((conf == null) && (Tables.exists(instance, tableId))) {
			conf = new TableConfiguration(instance, tableId, getNamespaceConfigurationForTable(tableId));
			ConfigSanityCheck.validate(conf);
			synchronized(ServerConfigurationFactory.tableConfigs) {
				Map<String, TableConfiguration> configs = ServerConfigurationFactory.tableConfigs.get(instanceID);
				TableConfiguration existingConf = configs.get(tableId);
				if (null == existingConf) {
					configs.put(tableId, conf);
				}else {
					conf = existingConf;
				}
			}
		}
		return conf;
	}

	@Override
	public TableConfiguration getTableConfiguration(KeyExtent extent) {
		return getTableConfiguration(extent.getTableId());
	}

	public NamespaceConfiguration getNamespaceConfigurationForTable(String tableId) {
		ServerConfigurationFactory.checkPermissions();
		NamespaceConfiguration conf;
		synchronized(ServerConfigurationFactory.tableParentConfigs) {
			conf = ServerConfigurationFactory.tableParentConfigs.get(instanceID).get(tableId);
		}
		if (conf == null) {
			String namespaceId;
			try {
				namespaceId = Tables.getNamespaceId(instance, tableId);
			} catch (TableNotFoundException e) {
				throw new RuntimeException(e);
			}
			conf = new NamespaceConfiguration(namespaceId, instance, getConfiguration());
			ConfigSanityCheck.validate(conf);
			synchronized(ServerConfigurationFactory.tableParentConfigs) {
				ServerConfigurationFactory.tableParentConfigs.get(instanceID).put(tableId, conf);
			}
		}
		return conf;
	}

	@Override
	public NamespaceConfiguration getNamespaceConfiguration(String namespaceId) {
		ServerConfigurationFactory.checkPermissions();
		NamespaceConfiguration conf;
		synchronized(ServerConfigurationFactory.namespaceConfigs) {
			conf = ServerConfigurationFactory.namespaceConfigs.get(instanceID).get(namespaceId);
		}
		if (conf == null) {
			conf = new NamespaceConfiguration(namespaceId, instance, getConfiguration());
			ConfigSanityCheck.validate(conf);
			synchronized(ServerConfigurationFactory.namespaceConfigs) {
				ServerConfigurationFactory.namespaceConfigs.get(instanceID).put(namespaceId, conf);
			}
		}
		return conf;
	}

	@Override
	public Instance getInstance() {
		return instance;
	}
}

