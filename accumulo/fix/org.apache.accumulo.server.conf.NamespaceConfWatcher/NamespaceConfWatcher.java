

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;


class NamespaceConfWatcher implements Watcher {
	static {
		Logger.getLogger("org.apache.zookeeper").setLevel(Level.WARN);
		Logger.getLogger("org.apache.hadoop.io.compress").setLevel(Level.WARN);
	}

	private static final Logger log = Logger.getLogger(NamespaceConfWatcher.class);

	private final Instance instance;

	private final String namespacesPrefix;

	private final int namespacesPrefixLength;

	private ServerConfigurationFactory scf;

	NamespaceConfWatcher(Instance instance) {
		this.instance = instance;
		namespacesPrefix = ((ZooUtil.getRoot(instance)) + (Constants.ZNAMESPACES)) + "/";
		namespacesPrefixLength = namespacesPrefix.length();
		scf = new ServerConfigurationFactory(instance);
	}

	static String toString(WatchedEvent event) {
		return new StringBuilder("{path=").append(event.getPath()).append(",state=").append(event.getState()).append(",type=").append(event.getType()).append("}").toString();
	}

	@Override
	public void process(WatchedEvent event) {
		String path = event.getPath();
		if (NamespaceConfWatcher.log.isTraceEnabled())
			NamespaceConfWatcher.log.trace(("WatchedEvent : " + (NamespaceConfWatcher.toString(event))));

		String namespaceId = null;
		String key = null;
		if (path != null) {
			if (path.startsWith(namespacesPrefix)) {
				namespaceId = path.substring(namespacesPrefixLength);
				if (namespaceId.contains("/")) {
					namespaceId = namespaceId.substring(0, namespaceId.indexOf('/'));
					if (path.startsWith(((((namespacesPrefix) + namespaceId) + (Constants.ZNAMESPACE_CONF)) + "/")))
						key = path.substring(((((namespacesPrefix) + namespaceId) + (Constants.ZNAMESPACE_CONF)) + "/").length());

				}
			}
			if (namespaceId == null) {
				NamespaceConfWatcher.log.warn(((("Zookeeper told me about a path I was not watching: " + path) + ", event ") + (NamespaceConfWatcher.toString(event))));
				return;
			}
		}
	}
}

