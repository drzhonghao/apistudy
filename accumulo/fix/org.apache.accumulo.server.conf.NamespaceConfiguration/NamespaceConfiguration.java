

import com.google.common.base.Predicate;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigurationObserver;
import org.apache.accumulo.core.conf.ObservableConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooCacheFactory;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.conf.ZooCachePropertyAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NamespaceConfiguration extends ObservableConfiguration {
	private static final Logger log = LoggerFactory.getLogger(NamespaceConfiguration.class);

	private final AccumuloConfiguration parent;

	private ZooCachePropertyAccessor propCacheAccessor = null;

	protected String namespaceId = null;

	protected Instance inst = null;

	private ZooCacheFactory zcf = new ZooCacheFactory();

	private final String path;

	public NamespaceConfiguration(String namespaceId, AccumuloConfiguration parent) {
		this(namespaceId, HdfsZooInstance.getInstance(), parent);
	}

	public NamespaceConfiguration(String namespaceId, Instance inst, AccumuloConfiguration parent) {
		this.inst = inst;
		this.parent = parent;
		this.namespaceId = namespaceId;
		this.path = ((((ZooUtil.getRoot(inst.getInstanceID())) + (Constants.ZNAMESPACES)) + "/") + namespaceId) + (Constants.ZNAMESPACE_CONF);
	}

	public AccumuloConfiguration getParentConfiguration() {
		return parent;
	}

	void setZooCacheFactory(ZooCacheFactory zcf) {
		this.zcf = zcf;
	}

	private synchronized ZooCachePropertyAccessor getPropCacheAccessor() {
		if ((propCacheAccessor) == null) {
		}
		return propCacheAccessor;
	}

	private String getPath() {
		return path;
	}

	@Override
	public String get(Property property) {
		String key = property.getKey();
		AccumuloConfiguration getParent;
		if (!((namespaceId.equals(Namespaces.ACCUMULO_NAMESPACE_ID)) && (NamespaceConfiguration.isIteratorOrConstraint(key)))) {
			getParent = parent;
		}else {
			getParent = null;
		}
		return null;
	}

	private class SystemNamespaceFilter implements Predicate<String> {
		private Predicate<String> userFilter;

		SystemNamespaceFilter(Predicate<String> userFilter) {
			this.userFilter = userFilter;
		}

		@Override
		public boolean apply(String key) {
			if (NamespaceConfiguration.isIteratorOrConstraint(key))
				return false;

			return userFilter.apply(key);
		}
	}

	@Override
	public void getProperties(Map<String, String> props, Predicate<String> filter) {
		Predicate<String> parentFilter = filter;
		if (getNamespaceId().equals(Namespaces.ACCUMULO_NAMESPACE_ID))
			parentFilter = new NamespaceConfiguration.SystemNamespaceFilter(filter);

	}

	protected String getNamespaceId() {
		return namespaceId;
	}

	@Override
	public void addObserver(ConfigurationObserver co) {
		if ((namespaceId) == null) {
			String err = "Attempt to add observer for non-namespace configuration";
			NamespaceConfiguration.log.error(err);
			throw new RuntimeException(err);
		}
		iterator();
		super.addObserver(co);
	}

	@Override
	public void removeObserver(ConfigurationObserver co) {
		if ((namespaceId) == null) {
			String err = "Attempt to remove observer for non-namespace configuration";
			NamespaceConfiguration.log.error(err);
			throw new RuntimeException(err);
		}
		super.removeObserver(co);
	}

	static boolean isIteratorOrConstraint(String key) {
		return (key.startsWith(Property.TABLE_ITERATOR_PREFIX.getKey())) || (key.startsWith(Property.TABLE_CONSTRAINT_PREFIX.getKey()));
	}

	@Override
	public synchronized void invalidateCache() {
		if (null != (propCacheAccessor)) {
		}
	}

	@Override
	public long getUpdateCount() {
		return 0l;
	}
}

