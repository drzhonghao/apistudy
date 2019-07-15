

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;


class TableConfWatcher implements Watcher {
	static {
		Logger.getLogger("org.apache.zookeeper").setLevel(Level.WARN);
		Logger.getLogger("org.apache.hadoop.io.compress").setLevel(Level.WARN);
	}

	private static final Logger log = Logger.getLogger(TableConfWatcher.class);

	private final Instance instance;

	private final String tablesPrefix;

	private ServerConfigurationFactory scf;

	TableConfWatcher(Instance instance) {
		this.instance = instance;
		tablesPrefix = ((ZooUtil.getRoot(instance)) + (Constants.ZTABLES)) + "/";
		scf = new ServerConfigurationFactory(instance);
	}

	static String toString(WatchedEvent event) {
		return new StringBuilder("{path=").append(event.getPath()).append(",state=").append(event.getState()).append(",type=").append(event.getType()).append("}").toString();
	}

	@Override
	public void process(WatchedEvent event) {
		String path = event.getPath();
		if (TableConfWatcher.log.isTraceEnabled())
			TableConfWatcher.log.trace(("WatchedEvent : " + (TableConfWatcher.toString(event))));

		String tableId = null;
		String key = null;
		if (path != null) {
			if (path.startsWith(tablesPrefix)) {
				tableId = path.substring(tablesPrefix.length());
				if (tableId.contains("/")) {
					tableId = tableId.substring(0, tableId.indexOf('/'));
					if (path.startsWith(((((tablesPrefix) + tableId) + (Constants.ZTABLE_CONF)) + "/")))
						key = path.substring(((((tablesPrefix) + tableId) + (Constants.ZTABLE_CONF)) + "/").length());

				}
			}
			if (tableId == null) {
				TableConfWatcher.log.warn(((("Zookeeper told me about a path I was not watching: " + path) + ", event ") + (TableConfWatcher.toString(event))));
				return;
			}
		}
	}
}

