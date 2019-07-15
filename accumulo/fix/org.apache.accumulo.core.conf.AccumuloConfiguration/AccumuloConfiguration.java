

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.conf.DefaultConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.PropertyType;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.core.conf.PropertyType.PortRange.parse;


public abstract class AccumuloConfiguration implements Iterable<Map.Entry<String, String>> {
	private static class PrefixProps {
		final long updateCount;

		final Map<String, String> props;

		PrefixProps(Map<String, String> props, long updateCount) {
			this.updateCount = updateCount;
			this.props = props;
		}
	}

	private volatile EnumMap<Property, AccumuloConfiguration.PrefixProps> cachedPrefixProps = new EnumMap<>(Property.class);

	private Lock prefixCacheUpdateLock = new ReentrantLock();

	@Deprecated
	public interface PropertyFilter {
		boolean accept(String key);
	}

	public static class MatchFilter implements Predicate<String> {
		private String match;

		public MatchFilter(String match) {
			this.match = match;
		}

		@Override
		public boolean apply(String key) {
			return Objects.equals(match, key);
		}
	}

	public static class PrefixFilter implements Predicate<String> {
		private String prefix;

		public PrefixFilter(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public boolean apply(String key) {
			return key.startsWith(prefix);
		}
	}

	private static final Logger log = LoggerFactory.getLogger(AccumuloConfiguration.class);

	public String get(String property) {
		Map<String, String> propMap = new HashMap<>(1);
		getProperties(propMap, new AccumuloConfiguration.MatchFilter(property));
		return propMap.get(property);
	}

	public abstract String get(Property property);

	public abstract void getProperties(Map<String, String> props, Predicate<String> filter);

	@Override
	public Iterator<Map.Entry<String, String>> iterator() {
		Predicate<String> all = Predicates.alwaysTrue();
		TreeMap<String, String> entries = new TreeMap<>();
		getProperties(entries, all);
		return entries.entrySet().iterator();
	}

	private void checkType(Property property, PropertyType type) {
		if (!(property.getType().equals(type))) {
			String msg = ((((("Configuration method intended for type " + type) + " called with a ") + (property.getType())) + " argument (") + (property.getKey())) + ")";
			IllegalArgumentException err = new IllegalArgumentException(msg);
			AccumuloConfiguration.log.error(msg, err);
			throw err;
		}
	}

	public long getUpdateCount() {
		return 0;
	}

	public Map<String, String> getAllPropertiesWithPrefix(Property property) {
		checkType(property, PropertyType.PREFIX);
		AccumuloConfiguration.PrefixProps prefixProps = cachedPrefixProps.get(property);
		if ((prefixProps == null) || ((prefixProps.updateCount) != (getUpdateCount()))) {
			prefixCacheUpdateLock.lock();
			try {
				long updateCount = getUpdateCount();
				prefixProps = cachedPrefixProps.get(property);
				if ((prefixProps == null) || ((prefixProps.updateCount) != updateCount)) {
					Map<String, String> propMap = new HashMap<>();
					getProperties(propMap, new AccumuloConfiguration.PrefixFilter(property.getKey()));
					propMap = ImmutableMap.copyOf(propMap);
					EnumMap<Property, AccumuloConfiguration.PrefixProps> localPrefixes = new EnumMap<>(Property.class);
					localPrefixes.putAll(cachedPrefixProps);
					prefixProps = new AccumuloConfiguration.PrefixProps(propMap, updateCount);
					localPrefixes.put(property, prefixProps);
					cachedPrefixProps = localPrefixes;
				}
			} finally {
				prefixCacheUpdateLock.unlock();
			}
		}
		return prefixProps.props;
	}

	public long getMemoryInBytes(Property property) {
		checkType(property, PropertyType.MEMORY);
		String memString = get(property);
		return AccumuloConfiguration.getMemoryInBytes(memString);
	}

	public static long getMemoryInBytes(String str) {
		char lastChar = str.charAt(((str.length()) - 1));
		if (lastChar == 'b') {
			AccumuloConfiguration.log.warn(((("The 'b' in " + str) + " is being considered as bytes. ") + "Setting memory by bits is not supported"));
		}
		try {
			int multiplier;
			switch (Character.toUpperCase(lastChar)) {
				case 'G' :
					multiplier = 30;
					break;
				case 'M' :
					multiplier = 20;
					break;
				case 'K' :
					multiplier = 10;
					break;
				case 'B' :
					multiplier = 0;
					break;
				default :
					return Long.parseLong(str);
			}
			return (Long.parseLong(str.substring(0, ((str.length()) - 1)))) << multiplier;
		} catch (Exception ex) {
			throw new IllegalArgumentException(((("The value '" + str) + "' is not a valid memory setting. A valid value would a number ") + "possibily followed by an optional 'G', 'M', 'K', or 'B'."));
		}
	}

	public long getTimeInMillis(Property property) {
		checkType(property, PropertyType.TIMEDURATION);
		return AccumuloConfiguration.getTimeInMillis(get(property));
	}

	public static long getTimeInMillis(String str) {
		TimeUnit timeUnit;
		int unitsLen = 1;
		switch (str.charAt(((str.length()) - 1))) {
			case 'd' :
				timeUnit = TimeUnit.DAYS;
				break;
			case 'h' :
				timeUnit = TimeUnit.HOURS;
				break;
			case 'm' :
				timeUnit = TimeUnit.MINUTES;
				break;
			case 's' :
				timeUnit = TimeUnit.SECONDS;
				if (str.endsWith("ms")) {
					timeUnit = TimeUnit.MILLISECONDS;
					unitsLen = 2;
				}
				break;
			default :
				timeUnit = TimeUnit.SECONDS;
				unitsLen = 0;
				break;
		}
		return timeUnit.toMillis(Long.parseLong(str.substring(0, ((str.length()) - unitsLen))));
	}

	public boolean getBoolean(Property property) {
		checkType(property, PropertyType.BOOLEAN);
		return Boolean.parseBoolean(get(property));
	}

	public double getFraction(Property property) {
		checkType(property, PropertyType.FRACTION);
		return getFraction(get(property));
	}

	public double getFraction(String str) {
		if (((str.length()) > 0) && ((str.charAt(((str.length()) - 1))) == '%'))
			return (Double.parseDouble(str.substring(0, ((str.length()) - 1)))) / 100.0;

		return Double.parseDouble(str);
	}

	public int[] getPort(Property property) {
		checkType(property, PropertyType.PORT);
		String portString = get(property);
		int[] ports = null;
		try {
			Pair<Integer, Integer> portRange = parse(portString);
			int low = portRange.getFirst();
			int high = portRange.getSecond();
			ports = new int[(high - low) + 1];
			for (int i = 0, j = low; j <= high; i++ , j++) {
				ports[i] = j;
			}
		} catch (IllegalArgumentException e) {
			ports = new int[1];
			try {
				int port = Integer.parseInt(portString);
				if (port != 0) {
					if ((port < 1024) || (port > 65535)) {
						AccumuloConfiguration.log.error(((("Invalid port number " + port) + "; Using default ") + (property.getDefaultValue())));
						ports[0] = Integer.parseInt(property.getDefaultValue());
					}else {
						ports[0] = port;
					}
				}else {
					ports[0] = port;
				}
			} catch (NumberFormatException e1) {
				throw new IllegalArgumentException(("Invalid port syntax. Must be a single positive " + "integers or a range (M-N) of positive integers"));
			}
		}
		return ports;
	}

	public int getCount(Property property) {
		checkType(property, PropertyType.COUNT);
		String countString = get(property);
		return Integer.parseInt(countString);
	}

	public String getPath(Property property) {
		checkType(property, PropertyType.PATH);
		String pathString = get(property);
		if (pathString == null)
			return null;

		for (String replaceableEnvVar : Constants.PATH_PROPERTY_ENV_VARS) {
			String envValue = System.getenv(replaceableEnvVar);
			if (envValue != null)
				pathString = pathString.replace(("$" + replaceableEnvVar), envValue);

		}
		return pathString;
	}

	public static synchronized DefaultConfiguration getDefaultConfiguration() {
		return DefaultConfiguration.getInstance();
	}

	public static AccumuloConfiguration getTableConfiguration(Connector conn, String tableId) throws AccumuloException, TableNotFoundException {
		String tableName = Tables.getTableName(conn.getInstance(), tableId);
		return null;
	}

	public int getMaxFilesPerTablet() {
		int maxFilesPerTablet = getCount(Property.TABLE_FILE_MAX);
		if (maxFilesPerTablet <= 0) {
			maxFilesPerTablet = (getCount(Property.TSERV_SCAN_MAX_OPENFILES)) - 1;
			AccumuloConfiguration.log.debug(("Max files per tablet " + maxFilesPerTablet));
		}
		return maxFilesPerTablet;
	}

	public void invalidateCache() {
	}

	public <T> T instantiateClassProperty(Property property, Class<T> base, T defaultInstance) {
		String clazzName = get(property);
		T instance = null;
		try {
			Class<? extends T> clazz = AccumuloVFSClassLoader.loadClass(clazzName, base);
			instance = clazz.newInstance();
			AccumuloConfiguration.log.info(("Loaded class : " + clazzName));
		} catch (Exception e) {
			AccumuloConfiguration.log.warn("Failed to load class ", e);
		}
		if (instance == null) {
			AccumuloConfiguration.log.info(("Using " + (defaultInstance.getClass().getName())));
			instance = defaultInstance;
		}
		return instance;
	}
}

