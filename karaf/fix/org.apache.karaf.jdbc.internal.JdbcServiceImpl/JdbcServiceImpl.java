

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JdbcServiceImpl {
	private static final Logger LOGGER = LoggerFactory.getLogger(JdbcServiceImpl.class);

	private BundleContext bundleContext;

	private ConfigurationAdmin configAdmin;

	public void create(String name, String driverName, String driverClass, String databaseName, String url, String user, String password, String databaseType) throws Exception {
		if ((driverName == null) && (driverClass == null)) {
			throw new IllegalStateException("No driverName or driverClass supplied");
		}
		if (datasources().contains(name)) {
			throw new IllegalArgumentException(("There is already a DataSource with the name " + name));
		}
		Dictionary<String, String> properties = new Hashtable<>();
		properties.put(DataSourceFactory.JDBC_DATASOURCE_NAME, name);
		put(properties, DataSourceFactory.OSGI_JDBC_DRIVER_NAME, driverName);
		put(properties, DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, driverClass);
		put(properties, DataSourceFactory.JDBC_DATABASE_NAME, databaseName);
		put(properties, DataSourceFactory.JDBC_URL, url);
		put(properties, DataSourceFactory.JDBC_USER, user);
		put(properties, DataSourceFactory.JDBC_PASSWORD, password);
		put(properties, "dataSourceType", databaseType);
		Configuration config = configAdmin.createFactoryConfiguration("org.ops4j.datasource", null);
		config.update(properties);
	}

	private void put(Dictionary<String, String> properties, String key, String value) {
		if (value != null) {
			properties.put(key, value);
		}
	}

	public void delete(String name) throws Exception {
		String filter = String.format("(&(service.factoryPid=org.ops4j.datasource)(%s=%s))", DataSourceFactory.JDBC_DATASOURCE_NAME, name);
		Configuration[] configs = configAdmin.listConfigurations(filter);
		for (Configuration config : configs) {
			config.delete();
		}
	}

	public List<String> datasources() throws Exception {
		List<String> datasources = new ArrayList<>();
		ServiceReference<?>[] references = bundleContext.getServiceReferences(((String) (null)), (((((((("(|(" + (Constants.OBJECTCLASS)) + "=") + (DataSource.class.getName())) + ")(") + (Constants.OBJECTCLASS)) + "=") + (XADataSource.class.getName())) + "))"));
		if (references != null) {
			for (ServiceReference reference : references) {
				if ((reference.getProperty("osgi.jndi.service.name")) != null) {
					datasources.add(reference.getProperty("osgi.jndi.service.name").toString());
				}else
					if ((reference.getProperty("datasource")) != null) {
						datasources.add(reference.getProperty("datasource").toString());
					}else
						if ((reference.getProperty("name")) != null) {
							datasources.add(reference.getProperty("name").toString());
						}else
							if ((reference.getProperty(DataSourceFactory.JDBC_DATASOURCE_NAME)) != null) {
								datasources.add(reference.getProperty(DataSourceFactory.JDBC_DATASOURCE_NAME).toString());
							}else {
								datasources.add(reference.getProperty(Constants.SERVICE_ID).toString());
							}



			}
		}
		return datasources;
	}

	public List<Long> datasourceServiceIds() throws Exception {
		List<Long> datasources = new ArrayList<>();
		ServiceReference<?>[] references = bundleContext.getServiceReferences(((String) (null)), (((((((("(|(" + (Constants.OBJECTCLASS)) + "=") + (DataSource.class.getName())) + ")(") + (Constants.OBJECTCLASS)) + "=") + (XADataSource.class.getName())) + "))"));
		if (references != null) {
			for (ServiceReference reference : references) {
				datasources.add(((Long) (reference.getProperty(Constants.SERVICE_ID))));
			}
		}
		return datasources;
	}

	public Map<String, List<String>> query(String datasource, String query) throws Exception {
		return null;
	}

	public void execute(String datasource, String command) throws Exception {
	}

	public Map<String, List<String>> tables(String datasource) throws Exception {
		return null;
	}

	public Map<String, String> info(String datasource) throws Exception {
		ServiceReference<?> reference = lookupDataSource(datasource);
		String dsName = datasource;
		if ((reference.getProperty("osgi.jndi.service.name")) != null) {
			dsName = ((String) (reference.getProperty("osgi.jndi.service.name")));
		}else
			if ((reference.getProperty("datasource")) != null) {
				dsName = ((String) (reference.getProperty("datasource")));
			}else
				if ((reference.getProperty("name")) != null) {
					dsName = ((String) (reference.getProperty("name")));
				}


		return null;
	}

	private ServiceReference<?> lookupDataSource(String name) {
		ServiceReference<?>[] references;
		try {
			references = bundleContext.getServiceReferences(((String) (null)), (((((((((((((((((("(&(|(" + (Constants.OBJECTCLASS)) + "=") + (DataSource.class.getName())) + ")") + "(") + (Constants.OBJECTCLASS)) + "=") + (XADataSource.class.getName())) + "))") + "(|(osgi.jndi.service.name=") + name) + ")(datasource=") + name) + ")(name=") + name) + ")(service.id=") + name) + ")))"));
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException(("Error finding datasource with name " + name), e);
		}
		if ((references == null) || ((references.length) == 0)) {
			throw new IllegalArgumentException(("No JDBC datasource found for " + name));
		}
		if ((references.length) > 1) {
			Arrays.sort(references);
			if ((getRank(references[((references.length) - 1)])) == (getRank(references[((references.length) - 2)]))) {
				JdbcServiceImpl.LOGGER.warn(("Multiple JDBC datasources found with the same service ranking for " + name));
			}
		}
		return references[((references.length) - 1)];
	}

	private int getRank(ServiceReference<?> reference) {
		Object rankObj = reference.getProperty(Constants.SERVICE_RANKING);
		rankObj = (rankObj == null) ? new Integer(0) : rankObj;
		return rankObj instanceof Integer ? ((Integer) (rankObj)) : 0;
	}

	public List<String> factoryNames() throws Exception {
		List<String> factories = new ArrayList<>();
		Collection<ServiceReference<DataSourceFactory>> references = bundleContext.getServiceReferences(DataSourceFactory.class, null);
		if (references == null) {
			return factories;
		}
		for (ServiceReference<DataSourceFactory> reference : references) {
			String driverName = ((String) (reference.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_NAME)));
			if (driverName != null) {
				factories.add(driverName);
			}
		}
		return factories;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void setConfigAdmin(ConfigurationAdmin configAdmin) {
		this.configAdmin = configAdmin;
	}
}

