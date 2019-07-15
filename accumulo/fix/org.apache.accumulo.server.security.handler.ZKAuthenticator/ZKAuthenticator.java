

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.fate.zookeeper.IZooReader;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.ZooUtil;
import org.apache.accumulo.server.security.handler.Authenticator;
import org.apache.accumulo.server.security.handler.Authorizor;
import org.apache.accumulo.server.security.handler.PermissionHandler;
import org.apache.accumulo.server.zookeeper.ZooCache;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy.FAIL;
import static org.apache.accumulo.fate.zookeeper.ZooUtil.NodeMissingPolicy.SKIP;
import static org.apache.zookeeper.KeeperException.Code.NONODE;


public final class ZKAuthenticator implements Authenticator {
	private static final Logger log = LoggerFactory.getLogger(ZKAuthenticator.class);

	private static Authenticator zkAuthenticatorInstance = null;

	private String ZKUserPath;

	private final ZooCache zooCache;

	public static synchronized Authenticator getInstance() {
		if ((ZKAuthenticator.zkAuthenticatorInstance) == null)
			ZKAuthenticator.zkAuthenticatorInstance = new ZKAuthenticator();

		return ZKAuthenticator.zkAuthenticatorInstance;
	}

	public ZKAuthenticator() {
		zooCache = new ZooCache();
	}

	@Override
	public void initialize(String instanceId, boolean initialize) {
		ZKUserPath = (((Constants.ZROOT) + "/") + instanceId) + "/users";
	}

	@Override
	public void initializeSecurity(TCredentials credentials, String principal, byte[] token) throws AccumuloSecurityException {
		try {
			IZooReaderWriter zoo = ZooReaderWriter.getInstance();
			synchronized(zooCache) {
				zooCache.clear();
				if (zoo.exists(ZKUserPath)) {
					zoo.recursiveDelete(ZKUserPath, SKIP);
					ZKAuthenticator.log.info(((("Removed " + (ZKUserPath)) + "/") + " from zookeeper"));
				}
				zoo.putPersistentData(ZKUserPath, principal.getBytes(StandardCharsets.UTF_8), FAIL);
			}
		} catch (KeeperException e) {
			ZKAuthenticator.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			ZKAuthenticator.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private void constructUser(String user, byte[] pass) throws InterruptedException, KeeperException {
		synchronized(zooCache) {
			zooCache.clear();
			IZooReaderWriter zoo = ZooReaderWriter.getInstance();
			zoo.putPrivatePersistentData((((ZKUserPath) + "/") + user), pass, FAIL);
		}
	}

	@Override
	public Set<String> listUsers() {
		return new TreeSet<>(zooCache.getChildren(ZKUserPath));
	}

	@Override
	public void createUser(String principal, AuthenticationToken token) throws AccumuloSecurityException {
		if (!(token instanceof PasswordToken))
			throw new AccumuloSecurityException(principal, SecurityErrorCode.INVALID_TOKEN);

		PasswordToken pt = ((PasswordToken) (token));
	}

	@Override
	public void dropUser(String user) throws AccumuloSecurityException {
		try {
			synchronized(zooCache) {
				zooCache.clear();
				ZooReaderWriter.getInstance().recursiveDelete((((ZKUserPath) + "/") + user), ZooUtil.NodeMissingPolicy.FAIL);
			}
		} catch (InterruptedException e) {
			ZKAuthenticator.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (KeeperException e) {
			if (e.code().equals(NONODE)) {
				throw new AccumuloSecurityException(user, SecurityErrorCode.USER_DOESNT_EXIST, e);
			}
			ZKAuthenticator.log.error("{}", e.getMessage(), e);
			throw new AccumuloSecurityException(user, SecurityErrorCode.CONNECTION_ERROR, e);
		}
	}

	@Override
	public void changePassword(String principal, AuthenticationToken token) throws AccumuloSecurityException {
		if (!(token instanceof PasswordToken))
			throw new AccumuloSecurityException(principal, SecurityErrorCode.INVALID_TOKEN);

		PasswordToken pt = ((PasswordToken) (token));
		if (userExists(principal)) {
			synchronized(zooCache) {
				zooCache.clear((((ZKUserPath) + "/") + principal));
			}
		}else
			throw new AccumuloSecurityException(principal, SecurityErrorCode.USER_DOESNT_EXIST);

	}

	@Override
	public boolean userExists(String user) {
		return (zooCache.get((((ZKUserPath) + "/") + user))) != null;
	}

	@Override
	public boolean validSecurityHandlers(Authorizor auth, PermissionHandler pm) {
		return true;
	}

	@Override
	public boolean authenticateUser(String principal, AuthenticationToken token) throws AccumuloSecurityException {
		if (!(token instanceof PasswordToken))
			throw new AccumuloSecurityException(principal, SecurityErrorCode.INVALID_TOKEN);

		PasswordToken pt = ((PasswordToken) (token));
		byte[] pass;
		String zpath = ((ZKUserPath) + "/") + principal;
		pass = zooCache.get(zpath);
		return false;
	}

	@Override
	public Set<Class<? extends AuthenticationToken>> getSupportedTokenTypes() {
		Set<Class<? extends AuthenticationToken>> cs = new HashSet<>();
		cs.add(PasswordToken.class);
		return cs;
	}

	@Override
	public boolean validTokenClass(String tokenClass) {
		return tokenClass.equals(PasswordToken.class.getName());
	}
}

