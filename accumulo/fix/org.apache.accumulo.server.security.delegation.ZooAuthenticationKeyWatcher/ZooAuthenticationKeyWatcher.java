

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.accumulo.fate.zookeeper.ZooReader;
import org.apache.accumulo.server.security.delegation.AuthenticationKey;
import org.apache.accumulo.server.security.delegation.AuthenticationTokenSecretManager;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.zookeeper.Watcher.Event.EventType.NodeChildrenChanged;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeCreated;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDataChanged;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDeleted;
import static org.apache.zookeeper.Watcher.Event.EventType.None;
import static org.apache.zookeeper.Watcher.Event.KeeperState.Disconnected;
import static org.apache.zookeeper.Watcher.Event.KeeperState.Expired;
import static org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected;


public class ZooAuthenticationKeyWatcher implements Watcher {
	private static final Logger log = LoggerFactory.getLogger(ZooAuthenticationKeyWatcher.class);

	private final AuthenticationTokenSecretManager secretManager;

	private final ZooReader zk;

	private final String baseNode;

	public ZooAuthenticationKeyWatcher(AuthenticationTokenSecretManager secretManager, ZooReader zk, String baseNode) {
		this.secretManager = secretManager;
		this.zk = zk;
		this.baseNode = baseNode;
	}

	@Override
	public void process(WatchedEvent event) {
		if ((None) == (event.getType())) {
			switch (event.getState()) {
				case Disconnected :
				case Expired :
					ZooAuthenticationKeyWatcher.log.debug("ZooKeeper connection disconnected, clearing secret manager");
					secretManager.removeAllKeys();
					break;
				case SyncConnected :
					ZooAuthenticationKeyWatcher.log.debug("ZooKeeper reconnected, updating secret manager");
					try {
						updateAuthKeys();
					} catch (KeeperException | InterruptedException e) {
						ZooAuthenticationKeyWatcher.log.error("Failed to update secret manager after ZooKeeper reconnect");
					}
					break;
				default :
					ZooAuthenticationKeyWatcher.log.warn(("Unhandled: " + event));
			}
			return;
		}
		String path = event.getPath();
		if (null == path) {
			return;
		}
		if (!(path.startsWith(baseNode))) {
			ZooAuthenticationKeyWatcher.log.info("Ignoring event for path: {}", path);
			return;
		}
		try {
			if (path.equals(baseNode)) {
				processBaseNode(event);
			}else {
				processChildNode(event);
			}
		} catch (KeeperException | InterruptedException e) {
			ZooAuthenticationKeyWatcher.log.error("Failed to communicate with ZooKeeper", e);
		}
	}

	void processBaseNode(WatchedEvent event) throws InterruptedException, KeeperException {
		switch (event.getType()) {
			case NodeDeleted :
				ZooAuthenticationKeyWatcher.log.debug("Parent ZNode was deleted, removing all AuthenticationKeys");
				secretManager.removeAllKeys();
				break;
			case None :
				break;
			case NodeCreated :
			case NodeChildrenChanged :
				updateAuthKeys(event.getPath());
				break;
			case NodeDataChanged :
				break;
			default :
				ZooAuthenticationKeyWatcher.log.warn("Unsupported event type: {}", event.getType());
				break;
		}
	}

	public void updateAuthKeys() throws InterruptedException, KeeperException {
		if (zk.exists(baseNode, this)) {
			ZooAuthenticationKeyWatcher.log.info("Added {} existing AuthenticationKeys to local cache from ZooKeeper", updateAuthKeys(baseNode));
		}
	}

	private int updateAuthKeys(String path) throws InterruptedException, KeeperException {
		int keysAdded = 0;
		for (String child : zk.getChildren(path, this)) {
			String childPath = (path + "/") + child;
			try {
				AuthenticationKey key = deserializeKey(zk.getData(childPath, this, null));
				secretManager.addKey(key);
				keysAdded++;
			} catch (KeeperException.NoNodeException e) {
				ZooAuthenticationKeyWatcher.log.trace("{} was deleted when we tried to access it", childPath);
			}
		}
		return keysAdded;
	}

	void processChildNode(WatchedEvent event) throws InterruptedException, KeeperException {
		final String path = event.getPath();
	}

	AuthenticationKey deserializeKey(byte[] serializedKey) {
		AuthenticationKey key = new AuthenticationKey();
		try {
			key.readFields(new DataInputStream(new ByteArrayInputStream(serializedKey)));
		} catch (IOException e) {
			throw new AssertionError("Failed to read from an in-memory buffer");
		}
		return key;
	}
}

