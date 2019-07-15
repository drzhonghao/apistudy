

import java.util.List;
import org.apache.accumulo.core.util.Daemon;
import org.apache.accumulo.server.security.delegation.AuthenticationKey;
import org.apache.accumulo.server.security.delegation.AuthenticationTokenSecretManager;
import org.apache.accumulo.server.security.delegation.ZooAuthenticationKeyDistributor;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuthenticationTokenKeyManager extends Daemon {
	private static final Logger log = LoggerFactory.getLogger(AuthenticationTokenKeyManager.class);

	private final AuthenticationTokenSecretManager secretManager;

	private final ZooAuthenticationKeyDistributor keyDistributor;

	private long lastKeyUpdate = 0;

	private long keyUpdateInterval;

	private long tokenMaxLifetime;

	private int idSeq = 0;

	private volatile boolean keepRunning = true;

	private volatile boolean initialized = false;

	public AuthenticationTokenKeyManager(AuthenticationTokenSecretManager mgr, ZooAuthenticationKeyDistributor dist, long keyUpdateInterval, long tokenMaxLifetime) {
		super("Delegation Token Key Manager");
		this.secretManager = mgr;
		this.keyDistributor = dist;
		this.keyUpdateInterval = keyUpdateInterval;
		this.tokenMaxLifetime = tokenMaxLifetime;
	}

	@com.google.common.annotations.VisibleForTesting
	void setKeepRunning(boolean keepRunning) {
		this.keepRunning = keepRunning;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void gracefulStop() {
		keepRunning = false;
	}

	@Override
	public void run() {
		updateStateFromCurrentKeys();
		initialized = true;
		while (keepRunning) {
			long now = System.currentTimeMillis();
			_run(now);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ie) {
				AuthenticationTokenKeyManager.log.debug("Interrupted waiting for next update", ie);
			}
		} 
	}

	@com.google.common.annotations.VisibleForTesting
	void updateStateFromCurrentKeys() {
		try {
			List<AuthenticationKey> currentKeys = keyDistributor.getCurrentKeys();
			if (!(currentKeys.isEmpty())) {
				for (AuthenticationKey key : currentKeys) {
					if ((key.getKeyId()) > (idSeq)) {
						idSeq = key.getKeyId();
					}
					secretManager.addKey(key);
				}
				AuthenticationTokenKeyManager.log.info("Added {} existing AuthenticationKeys into the local cache from ZooKeeper", currentKeys.size());
			}
		} catch (KeeperException | InterruptedException e) {
			AuthenticationTokenKeyManager.log.warn("Failed to fetch existing AuthenticationKeys from ZooKeeper");
		}
	}

	@com.google.common.annotations.VisibleForTesting
	long getLastKeyUpdate() {
		return lastKeyUpdate;
	}

	@com.google.common.annotations.VisibleForTesting
	int getIdSeq() {
		return idSeq;
	}

	void _run(long now) {
		if (((lastKeyUpdate) + (keyUpdateInterval)) < now) {
			AuthenticationTokenKeyManager.log.debug("Key update interval passed, creating new authentication key");
			lastKeyUpdate = now;
		}
	}
}

