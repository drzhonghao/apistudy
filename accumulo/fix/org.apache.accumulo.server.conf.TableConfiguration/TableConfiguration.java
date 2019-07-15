

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigurationObserver;
import org.apache.accumulo.core.conf.ObservableConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.thrift.IterInfo;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooCacheFactory;
import org.apache.accumulo.server.conf.NamespaceConfiguration;
import org.apache.accumulo.server.conf.ZooCachePropertyAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope.values;


public class TableConfiguration extends ObservableConfiguration {
	private static final Logger log = LoggerFactory.getLogger(TableConfiguration.class);

	private ZooCachePropertyAccessor propCacheAccessor = null;

	private final Instance instance;

	private final NamespaceConfiguration parent;

	private ZooCacheFactory zcf = new ZooCacheFactory();

	private final String tableId;

	private EnumMap<IteratorUtil.IteratorScope, AtomicReference<TableConfiguration.ParsedIteratorConfig>> iteratorConfig;

	public TableConfiguration(Instance instance, String tableId, NamespaceConfiguration parent) {
		this.instance = instance;
		this.tableId = tableId;
		this.parent = parent;
		iteratorConfig = new EnumMap<>(IteratorUtil.IteratorScope.class);
		for (IteratorUtil.IteratorScope scope : values()) {
			iteratorConfig.put(scope, new AtomicReference<TableConfiguration.ParsedIteratorConfig>(null));
		}
	}

	void setZooCacheFactory(ZooCacheFactory zcf) {
		this.zcf = zcf;
	}

	private synchronized ZooCachePropertyAccessor getPropCacheAccessor() {
		if ((propCacheAccessor) == null) {
		}
		return propCacheAccessor;
	}

	@Override
	public void addObserver(ConfigurationObserver co) {
		if ((tableId) == null) {
			String err = "Attempt to add observer for non-table configuration";
			TableConfiguration.log.error(err);
			throw new RuntimeException(err);
		}
		iterator();
		super.addObserver(co);
	}

	@Override
	public void removeObserver(ConfigurationObserver co) {
		if ((tableId) == null) {
			String err = "Attempt to remove observer for non-table configuration";
			TableConfiguration.log.error(err);
			throw new RuntimeException(err);
		}
		super.removeObserver(co);
	}

	private String getPath() {
		return ((((ZooUtil.getRoot(instance.getInstanceID())) + (Constants.ZTABLES)) + "/") + (tableId)) + (Constants.ZTABLE_CONF);
	}

	@Override
	public String get(Property property) {
		return null;
	}

	@Override
	public void getProperties(Map<String, String> props, Predicate<String> filter) {
	}

	public String getTableId() {
		return tableId;
	}

	public NamespaceConfiguration getNamespaceConfiguration() {
		return null;
	}

	public NamespaceConfiguration getParentConfiguration() {
		return parent;
	}

	@Override
	public synchronized void invalidateCache() {
		if (null != (propCacheAccessor)) {
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	@Override
	public long getUpdateCount() {
		return 0l;
	}

	public static class ParsedIteratorConfig {
		private final List<IterInfo> tableIters;

		private final Map<String, Map<String, String>> tableOpts;

		private final String context;

		private final long updateCount;

		private ParsedIteratorConfig(List<IterInfo> ii, Map<String, Map<String, String>> opts, String context, long updateCount) {
			this.tableIters = ImmutableList.copyOf(ii);
			ImmutableMap.Builder<String, Map<String, String>> imb = ImmutableMap.builder();
			for (Map.Entry<String, Map<String, String>> entry : opts.entrySet()) {
				imb.put(entry.getKey(), ImmutableMap.copyOf(entry.getValue()));
			}
			tableOpts = imb.build();
			this.context = context;
			this.updateCount = updateCount;
		}

		public List<IterInfo> getIterInfo() {
			return tableIters;
		}

		public Map<String, Map<String, String>> getOpts() {
			return tableOpts;
		}

		public String getContext() {
			return context;
		}
	}

	public TableConfiguration.ParsedIteratorConfig getParsedIteratorConfig(IteratorUtil.IteratorScope scope) {
		long count = getUpdateCount();
		AtomicReference<TableConfiguration.ParsedIteratorConfig> ref = iteratorConfig.get(scope);
		TableConfiguration.ParsedIteratorConfig pic = ref.get();
		if ((pic == null) || ((pic.updateCount) != count)) {
			List<IterInfo> iters = new ArrayList<>();
			Map<String, Map<String, String>> allOptions = new HashMap<>();
			IteratorUtil.parseIterConf(scope, iters, allOptions, this);
			TableConfiguration.ParsedIteratorConfig newPic = new TableConfiguration.ParsedIteratorConfig(iters, allOptions, get(Property.TABLE_CLASSPATH), count);
			ref.compareAndSet(pic, newPic);
			pic = newPic;
		}
		return pic;
	}
}

