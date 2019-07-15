

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.impl.AuthenticationTokenIdentifier;
import org.apache.accumulo.core.client.impl.DelegationTokenImpl;
import org.apache.accumulo.server.security.delegation.AuthenticationKey;
import org.apache.accumulo.server.security.delegation.ZooAuthenticationKeyDistributor;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.token.SecretManager;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuthenticationTokenSecretManager extends SecretManager<AuthenticationTokenIdentifier> {
	private static final Logger log = LoggerFactory.getLogger(AuthenticationTokenSecretManager.class);

	private final Instance instance;

	private final long tokenMaxLifetime;

	private final ConcurrentHashMap<Integer, AuthenticationKey> allKeys = new ConcurrentHashMap<>();

	private AuthenticationKey currentKey;

	public AuthenticationTokenSecretManager(Instance instance, long tokenMaxLifetime) {
		Objects.requireNonNull(instance);
		Preconditions.checkArgument((tokenMaxLifetime > 0), "Max lifetime must be positive");
		this.instance = instance;
		this.tokenMaxLifetime = tokenMaxLifetime;
	}

	@Override
	protected byte[] createPassword(AuthenticationTokenIdentifier identifier) {
		DelegationTokenConfig cfg = identifier.getConfig();
		long now = System.currentTimeMillis();
		final AuthenticationKey secretKey;
		synchronized(this) {
			secretKey = currentKey;
		}
		identifier.setKeyId(secretKey.getKeyId());
		identifier.setIssueDate(now);
		long expiration = now + (tokenMaxLifetime);
		if (expiration < now) {
			expiration = Long.MAX_VALUE;
		}
		identifier.setExpirationDate(expiration);
		if (null != cfg) {
			long requestedLifetime = cfg.getTokenLifetime(TimeUnit.MILLISECONDS);
			if (0 < requestedLifetime) {
				long requestedExpirationDate = (identifier.getIssueDate()) + requestedLifetime;
				if (requestedExpirationDate < (identifier.getIssueDate())) {
					requestedExpirationDate = Long.MAX_VALUE;
				}
				if (requestedExpirationDate > (identifier.getExpirationDate())) {
					throw new RuntimeException("Requested token lifetime exceeds configured maximum");
				}
				AuthenticationTokenSecretManager.log.trace("Overriding token expiration date from {} to {}", identifier.getExpirationDate(), requestedExpirationDate);
				identifier.setExpirationDate(requestedExpirationDate);
			}
		}
		identifier.setInstanceId(instance.getInstanceID());
		return null;
	}

	@Override
	public byte[] retrievePassword(AuthenticationTokenIdentifier identifier) throws SecretManager.InvalidToken {
		long now = System.currentTimeMillis();
		if ((identifier.getExpirationDate()) < now) {
			throw new SecretManager.InvalidToken("Token has expired");
		}
		if ((identifier.getIssueDate()) > now) {
			throw new SecretManager.InvalidToken("Token issued in the future");
		}
		AuthenticationKey masterKey = allKeys.get(identifier.getKeyId());
		if (masterKey == null) {
			throw new SecretManager.InvalidToken((("Unknown master key for token (id=" + (identifier.getKeyId())) + ")"));
		}
		return null;
	}

	@Override
	public AuthenticationTokenIdentifier createIdentifier() {
		return new AuthenticationTokenIdentifier();
	}

	public Map.Entry<Token<AuthenticationTokenIdentifier>, AuthenticationTokenIdentifier> generateToken(String username, DelegationTokenConfig cfg) throws AccumuloException {
		Objects.requireNonNull(username);
		Objects.requireNonNull(cfg);
		final AuthenticationTokenIdentifier id = new AuthenticationTokenIdentifier(username, cfg);
		final StringBuilder svcName = new StringBuilder(DelegationTokenImpl.SERVICE_NAME);
		if (null != (id.getInstanceId())) {
			svcName.append("-").append(id.getInstanceId());
		}
		byte[] password;
		try {
			password = createPassword(id);
		} catch (RuntimeException e) {
			throw new AccumuloException(e.getMessage());
		}
		Token<AuthenticationTokenIdentifier> token = new Token<>(id.getBytes(), password, id.getKind(), new Text(svcName.toString()));
		return Maps.immutableEntry(token, id);
	}

	public synchronized void addKey(AuthenticationKey key) {
		Objects.requireNonNull(key);
		AuthenticationTokenSecretManager.log.debug("Adding AuthenticationKey with keyId {}", key.getKeyId());
		allKeys.put(key.getKeyId(), key);
		if (((currentKey) == null) || ((key.getKeyId()) > (currentKey.getKeyId()))) {
			currentKey = key;
		}
	}

	synchronized boolean removeKey(Integer keyId) {
		Objects.requireNonNull(keyId);
		AuthenticationTokenSecretManager.log.debug("Removing AuthenticatioKey with keyId {}", keyId);
		return null != (allKeys.remove(keyId));
	}

	@com.google.common.annotations.VisibleForTesting
	AuthenticationKey getCurrentKey() {
		return currentKey;
	}

	@com.google.common.annotations.VisibleForTesting
	Map<Integer, AuthenticationKey> getKeys() {
		return allKeys;
	}

	synchronized int removeExpiredKeys(ZooAuthenticationKeyDistributor keyDistributor) {
		long now = System.currentTimeMillis();
		int keysRemoved = 0;
		Iterator<Map.Entry<Integer, AuthenticationKey>> iter = allKeys.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Integer, AuthenticationKey> entry = iter.next();
			AuthenticationKey key = entry.getValue();
			if ((key.getExpirationDate()) < now) {
				AuthenticationTokenSecretManager.log.debug("Removing expired delegation token key {}", key.getKeyId());
				iter.remove();
				keysRemoved++;
				try {
					keyDistributor.remove(key);
				} catch (KeeperException | InterruptedException e) {
					AuthenticationTokenSecretManager.log.error("Failed to remove AuthenticationKey from ZooKeeper. Exiting", e);
					throw new RuntimeException(e);
				}
			}
		} 
		return keysRemoved;
	}

	synchronized boolean isCurrentKeySet() {
		return null != (currentKey);
	}

	public synchronized void removeAllKeys() {
		allKeys.clear();
		currentKey = null;
	}

	@Override
	protected SecretKey generateSecret() {
		return super.generateSecret();
	}

	public static SecretKey createSecretKey(byte[] raw) {
		return SecretManager.createSecretKey(raw);
	}
}

