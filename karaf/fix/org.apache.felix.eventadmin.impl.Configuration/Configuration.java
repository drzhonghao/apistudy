

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.apache.felix.eventadmin.impl.MetaTypeProviderImpl;
import org.apache.felix.eventadmin.impl.adapter.AbstractAdapter;
import org.apache.felix.eventadmin.impl.adapter.BundleEventAdapter;
import org.apache.felix.eventadmin.impl.adapter.FrameworkEventAdapter;
import org.apache.felix.eventadmin.impl.adapter.ServiceEventAdapter;
import org.apache.felix.eventadmin.impl.handler.EventAdminImpl;
import org.apache.felix.eventadmin.impl.security.SecureEventAdminFactory;
import org.apache.felix.eventadmin.impl.tasks.DefaultThreadPool;
import org.apache.felix.eventadmin.impl.util.LogWrapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.MetaTypeProvider;


public class Configuration {
	static final String PID = "org.apache.felix.eventadmin.impl.EventAdmin";

	static final String PROP_THREAD_POOL_SIZE = "org.apache.felix.eventadmin.ThreadPoolSize";

	static final String PROP_ASYNC_TO_SYNC_THREAD_RATIO = "org.apache.felix.eventadmin.AsyncToSyncThreadRatio";

	static final String PROP_TIMEOUT = "org.apache.felix.eventadmin.Timeout";

	static final String PROP_REQUIRE_TOPIC = "org.apache.felix.eventadmin.RequireTopic";

	static final String PROP_IGNORE_TIMEOUT = "org.apache.felix.eventadmin.IgnoreTimeout";

	static final String PROP_IGNORE_TOPIC = "org.apache.felix.eventadmin.IgnoreTopic";

	static final String PROP_LOG_LEVEL = "org.apache.felix.eventadmin.LogLevel";

	static final String PROP_ADD_TIMESTAMP = "org.apache.felix.eventadmin.AddTimestamp";

	static final String PROP_ADD_SUBJECT = "org.apache.felix.eventadmin.AddSubject";

	private final BundleContext m_bundleContext;

	private int m_threadPoolSize;

	private double m_asyncToSyncThreadRatio;

	private int m_asyncThreadPoolSize;

	private int m_timeout;

	private boolean m_requireTopic;

	private String[] m_ignoreTimeout;

	private String[] m_ignoreTopics;

	private int m_logLevel;

	private boolean m_addTimestamp;

	private boolean m_addSubject;

	private volatile DefaultThreadPool m_sync_pool;

	private volatile DefaultThreadPool m_async_pool;

	private volatile EventAdminImpl m_admin;

	private volatile ServiceRegistration m_registration;

	private AbstractAdapter[] m_adapters;

	private ServiceRegistration m_managedServiceReg;

	public Configuration(BundleContext bundleContext) {
		m_bundleContext = bundleContext;
		configure(null);
		startOrUpdate();
		try {
			Object service = tryToCreateManagedService();
			if (service != null) {
				Object enhancedService = tryToCreateMetaTypeProvider(service);
				final String[] interfaceNames;
				if (enhancedService == null) {
					interfaceNames = new String[]{ ManagedService.class.getName() };
				}else {
					interfaceNames = new String[]{ ManagedService.class.getName(), MetaTypeProvider.class.getName() };
					service = enhancedService;
				}
				Dictionary<String, Object> props = new Hashtable<>();
				props.put(Constants.SERVICE_PID, Configuration.PID);
				m_managedServiceReg = m_bundleContext.registerService(interfaceNames, service, props);
			}
		} catch (Throwable t) {
		}
	}

	void updateFromConfigAdmin(final Dictionary<String, ?> config) {
		new Thread(() -> {
			synchronized(this) {
				this.configure(config);
				this.startOrUpdate();
			}
		}).start();
	}

	void configure(Dictionary<String, ?> config) {
		if (config == null) {
			m_threadPoolSize = getIntProperty(Configuration.PROP_THREAD_POOL_SIZE, m_bundleContext.getProperty(Configuration.PROP_THREAD_POOL_SIZE), 20, 2);
			m_asyncToSyncThreadRatio = getDoubleProperty(Configuration.PROP_ASYNC_TO_SYNC_THREAD_RATIO, m_bundleContext.getProperty(Configuration.PROP_ASYNC_TO_SYNC_THREAD_RATIO), 0.5, 0.0);
			m_timeout = getIntProperty(Configuration.PROP_TIMEOUT, m_bundleContext.getProperty(Configuration.PROP_TIMEOUT), 5000, Integer.MIN_VALUE);
			m_requireTopic = getBooleanProperty(m_bundleContext.getProperty(Configuration.PROP_REQUIRE_TOPIC), true);
			final String value = m_bundleContext.getProperty(Configuration.PROP_IGNORE_TIMEOUT);
			if (value == null) {
				m_ignoreTimeout = null;
			}else {
				final StringTokenizer st = new StringTokenizer(value, ",");
				m_ignoreTimeout = new String[st.countTokens()];
				for (int i = 0; i < (m_ignoreTimeout.length); i++) {
					m_ignoreTimeout[i] = st.nextToken();
				}
			}
			final String valueIgnoreTopic = m_bundleContext.getProperty(Configuration.PROP_IGNORE_TOPIC);
			if (valueIgnoreTopic == null) {
				m_ignoreTopics = null;
			}else {
				final StringTokenizer st = new StringTokenizer(valueIgnoreTopic, ",");
				m_ignoreTopics = new String[st.countTokens()];
				for (int i = 0; i < (m_ignoreTopics.length); i++) {
					m_ignoreTopics[i] = st.nextToken();
				}
			}
			m_logLevel = getIntProperty(Configuration.PROP_LOG_LEVEL, m_bundleContext.getProperty(Configuration.PROP_LOG_LEVEL), LogWrapper.LOG_WARNING, LogWrapper.LOG_ERROR);
			m_addTimestamp = getBooleanProperty(m_bundleContext.getProperty(Configuration.PROP_ADD_TIMESTAMP), false);
			m_addSubject = getBooleanProperty(m_bundleContext.getProperty(Configuration.PROP_ADD_SUBJECT), false);
		}else {
			m_threadPoolSize = getIntProperty(Configuration.PROP_THREAD_POOL_SIZE, config.get(Configuration.PROP_THREAD_POOL_SIZE), 20, 2);
			m_asyncToSyncThreadRatio = getDoubleProperty(Configuration.PROP_ASYNC_TO_SYNC_THREAD_RATIO, m_bundleContext.getProperty(Configuration.PROP_ASYNC_TO_SYNC_THREAD_RATIO), 0.5, 0.0);
			m_timeout = getIntProperty(Configuration.PROP_TIMEOUT, config.get(Configuration.PROP_TIMEOUT), 5000, Integer.MIN_VALUE);
			m_requireTopic = getBooleanProperty(config.get(Configuration.PROP_REQUIRE_TOPIC), true);
			m_ignoreTimeout = null;
			final Object value = config.get(Configuration.PROP_IGNORE_TIMEOUT);
			if (value instanceof String) {
				m_ignoreTimeout = new String[]{ ((String) (value)) };
			}else
				if (value instanceof String[]) {
					m_ignoreTimeout = ((String[]) (value));
				}else
					if (value != null) {
						LogWrapper.getLogger().log(LogWrapper.LOG_WARNING, (("Value for property: " + (Configuration.PROP_IGNORE_TIMEOUT)) + " is neither a string nor a string array - Using default"));
					}


			m_ignoreTopics = null;
			final Object valueIT = config.get(Configuration.PROP_IGNORE_TOPIC);
			if (valueIT instanceof String) {
				m_ignoreTopics = new String[]{ ((String) (valueIT)) };
			}else
				if (valueIT instanceof String[]) {
					m_ignoreTopics = ((String[]) (valueIT));
				}else
					if (valueIT != null) {
						LogWrapper.getLogger().log(LogWrapper.LOG_WARNING, (("Value for property: " + (Configuration.PROP_IGNORE_TOPIC)) + " is neither a string nor a string array - Using default"));
					}


			m_logLevel = getIntProperty(Configuration.PROP_LOG_LEVEL, config.get(Configuration.PROP_LOG_LEVEL), LogWrapper.LOG_WARNING, LogWrapper.LOG_ERROR);
			m_addTimestamp = getBooleanProperty(config.get(Configuration.PROP_ADD_TIMESTAMP), false);
			m_addSubject = getBooleanProperty(config.get(Configuration.PROP_ADD_SUBJECT), false);
		}
		if ((m_timeout) <= 100) {
			m_timeout = 0;
		}
		m_asyncThreadPoolSize = ((m_threadPoolSize) > 5) ? ((int) (Math.floor(((m_threadPoolSize) * (m_asyncToSyncThreadRatio))))) : 2;
	}

	private void startOrUpdate() {
		LogWrapper.getLogger().setLogLevel(m_logLevel);
		LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG, (((Configuration.PROP_LOG_LEVEL) + "=") + (m_logLevel)));
		LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG, (((Configuration.PROP_THREAD_POOL_SIZE) + "=") + (m_threadPoolSize)));
		LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG, (((Configuration.PROP_ASYNC_TO_SYNC_THREAD_RATIO) + "=") + (m_asyncToSyncThreadRatio)));
		LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG, ("Async Pool Size=" + (m_asyncThreadPoolSize)));
		LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG, (((Configuration.PROP_TIMEOUT) + "=") + (m_timeout)));
		LogWrapper.getLogger().log(LogWrapper.LOG_DEBUG, (((Configuration.PROP_REQUIRE_TOPIC) + "=") + (m_requireTopic)));
		if ((m_sync_pool) == null) {
			m_sync_pool = new DefaultThreadPool(m_threadPoolSize, true);
		}else {
			m_sync_pool.configure(m_threadPoolSize);
		}
		final int asyncThreadPoolSize = m_asyncThreadPoolSize;
		if ((m_async_pool) == null) {
			m_async_pool = new DefaultThreadPool(asyncThreadPoolSize, false);
		}else {
			m_async_pool.configure(asyncThreadPoolSize);
		}
		if ((m_admin) == null) {
			adaptEvents(m_admin);
			m_registration = m_bundleContext.registerService(EventAdmin.class.getName(), new SecureEventAdminFactory(m_admin), null);
		}else {
		}
	}

	public void destroy() {
		synchronized(this) {
			if ((m_adapters) != null) {
				for (AbstractAdapter adapter : m_adapters) {
					adapter.destroy(m_bundleContext);
				}
				m_adapters = null;
			}
			if ((m_managedServiceReg) != null) {
				m_managedServiceReg.unregister();
				m_managedServiceReg = null;
			}
			if ((m_registration) != null) {
				m_registration.unregister();
				m_registration = null;
			}
			if ((m_admin) != null) {
				m_admin.stop();
				m_admin = null;
			}
			if ((m_async_pool) != null) {
				m_async_pool.close();
				m_async_pool = null;
			}
			if ((m_sync_pool) != null) {
				m_sync_pool.close();
				m_sync_pool = null;
			}
		}
	}

	private void adaptEvents(final EventAdmin admin) {
		m_adapters = new AbstractAdapter[3];
		m_adapters[0] = new FrameworkEventAdapter(m_bundleContext, admin);
		m_adapters[1] = new BundleEventAdapter(m_bundleContext, admin);
		m_adapters[2] = new ServiceEventAdapter(m_bundleContext, admin);
	}

	private Object tryToCreateMetaTypeProvider(final Object managedService) {
		try {
			return new MetaTypeProviderImpl(((ManagedService) (managedService)), m_threadPoolSize, m_timeout, m_requireTopic, m_ignoreTimeout, m_ignoreTopics, m_asyncToSyncThreadRatio);
		} catch (final Throwable t) {
		}
		return null;
	}

	private Object tryToCreateManagedService() {
		try {
			return ((ManagedService) (this::updateFromConfigAdmin));
		} catch (Throwable t) {
		}
		return null;
	}

	private int getIntProperty(final String key, final Object value, final int defaultValue, final int min) {
		if (null != value) {
			final int result;
			if (value instanceof Integer) {
				result = ((Integer) (value)).intValue();
			}else {
				try {
					result = Integer.parseInt(value.toString());
				} catch (NumberFormatException e) {
					LogWrapper.getLogger().log(LogWrapper.LOG_WARNING, (("Unable to parse property: " + key) + " - Using default"), e);
					return defaultValue;
				}
			}
			if (result >= min) {
				return result;
			}
			LogWrapper.getLogger().log(LogWrapper.LOG_WARNING, (("Value for property: " + key) + " is to low - Using default"));
		}
		return defaultValue;
	}

	private double getDoubleProperty(final String key, final Object value, final double defaultValue, final double min) {
		if (null != value) {
			final double result;
			if (value instanceof Double) {
				result = ((Double) (value)).doubleValue();
			}else {
				try {
					result = Double.parseDouble(value.toString());
				} catch (NumberFormatException e) {
					LogWrapper.getLogger().log(LogWrapper.LOG_WARNING, (("Unable to parse property: " + key) + " - Using default"), e);
					return defaultValue;
				}
			}
			if (result >= min) {
				return result;
			}
			LogWrapper.getLogger().log(LogWrapper.LOG_WARNING, (("Value for property: " + key) + " is to low - Using default"));
		}
		return defaultValue;
	}

	private boolean getBooleanProperty(final Object obj, final boolean defaultValue) {
		if (null != obj) {
			if (obj instanceof Boolean) {
				return ((Boolean) (obj)).booleanValue();
			}
			String value = obj.toString().trim().toLowerCase();
			if ((0 < (value.length())) && ((("0".equals(value)) || ("false".equals(value))) || ("no".equals(value)))) {
				return false;
			}
			if ((0 < (value.length())) && ((("1".equals(value)) || ("true".equals(value))) || ("yes".equals(value)))) {
				return true;
			}
		}
		return defaultValue;
	}
}

