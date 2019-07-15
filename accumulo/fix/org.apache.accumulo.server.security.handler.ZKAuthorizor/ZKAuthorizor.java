

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.SystemPermission;
import org.apache.accumulo.core.security.TablePermission;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.util.ByteBufferUtil;
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
import static org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy.SKIP;
import static org.apache.zookeeper.KeeperException.Code.NONODE;


public class ZKAuthorizor implements Authorizor {
	private static final Logger log = LoggerFactory.getLogger(ZKAuthorizor.class);

	private static Authorizor zkAuthorizorInstance = null;

	private final String ZKUserAuths = "/Authorizations";

	private String ZKUserPath;

	private final ZooCache zooCache;

	public static synchronized Authorizor getInstance() {
		if ((ZKAuthorizor.zkAuthorizorInstance) == null)
			ZKAuthorizor.zkAuthorizorInstance = new ZKAuthorizor();

		return ZKAuthorizor.zkAuthorizorInstance;
	}

	public ZKAuthorizor() {
		zooCache = new ZooCache();
	}

	@Override
	public void initialize(String instanceId, boolean initialize) {
	}

	@Override
	public Authorizations getCachedUserAuthorizations(String user) {
		byte[] authsBytes = zooCache.get(((((ZKUserPath) + "/") + user) + (ZKUserAuths)));
		if (authsBytes != null) {
		}
		return Authorizations.EMPTY;
	}

	@Override
	public boolean validSecurityHandlers(Authenticator auth, PermissionHandler pm) {
		return true;
	}

	@Override
	public void initializeSecurity(TCredentials itw, String rootuser) throws AccumuloSecurityException {
		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		Set<SystemPermission> rootPerms = new TreeSet<>();
		for (SystemPermission p : SystemPermission.values())
			rootPerms.add(p);

		Map<String, Set<TablePermission>> tablePerms = new HashMap<>();
		tablePerms.put(MetadataTable.ID, Collections.singleton(TablePermission.ALTER_TABLE));
		tablePerms.put(RootTable.ID, Collections.singleton(TablePermission.ALTER_TABLE));
		try {
			if (!(zoo.exists(ZKUserPath)))
				zoo.putPersistentData(ZKUserPath, rootuser.getBytes(StandardCharsets.UTF_8), FAIL);

			initUser(rootuser);
		} catch (KeeperException e) {
			ZKAuthorizor.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			ZKAuthorizor.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void initUser(String user) throws AccumuloSecurityException {
		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		try {
			zoo.putPersistentData((((ZKUserPath) + "/") + user), new byte[0], SKIP);
		} catch (KeeperException e) {
			ZKAuthorizor.log.error("{}", e.getMessage(), e);
			throw new AccumuloSecurityException(user, SecurityErrorCode.CONNECTION_ERROR, e);
		} catch (InterruptedException e) {
			ZKAuthorizor.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void dropUser(String user) throws AccumuloSecurityException {
		try {
			synchronized(zooCache) {
				IZooReaderWriter zoo = ZooReaderWriter.getInstance();
				zoo.recursiveDelete(((((ZKUserPath) + "/") + user) + (ZKUserAuths)), ZooUtil.NodeMissingPolicy.SKIP);
				zooCache.clear((((ZKUserPath) + "/") + user));
			}
		} catch (InterruptedException e) {
			ZKAuthorizor.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (KeeperException e) {
			ZKAuthorizor.log.error("{}", e.getMessage(), e);
			if (e.code().equals(NONODE))
				throw new AccumuloSecurityException(user, SecurityErrorCode.USER_DOESNT_EXIST, e);

			throw new AccumuloSecurityException(user, SecurityErrorCode.CONNECTION_ERROR, e);
		}
	}

	@Override
	public void changeAuthorizations(String user, Authorizations authorizations) throws AccumuloSecurityException {
		synchronized(zooCache) {
			zooCache.clear();
		}
	}

	@Override
	public boolean isValidAuthorizations(String user, List<ByteBuffer> auths) throws AccumuloSecurityException {
		if (auths.isEmpty()) {
			return true;
		}
		Authorizations userauths = getCachedUserAuthorizations(user);
		for (ByteBuffer auth : auths) {
			if (!(userauths.contains(ByteBufferUtil.toBytes(auth)))) {
				return false;
			}
		}
		return true;
	}
}

