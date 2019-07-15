

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.Namespaces;
import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.core.security.NamespacePermission;
import org.apache.accumulo.core.security.SystemPermission;
import org.apache.accumulo.core.security.TablePermission;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.fate.zookeeper.IZooReader;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.ZooReader;
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


public class ZKPermHandler implements PermissionHandler {
	private static final Logger log = LoggerFactory.getLogger(ZKPermHandler.class);

	private static PermissionHandler zkPermHandlerInstance = null;

	private String ZKUserPath;

	private String ZKTablePath;

	private String ZKNamespacePath;

	private final ZooCache zooCache;

	private final String ZKUserSysPerms = "/System";

	private final String ZKUserTablePerms = "/Tables";

	private final String ZKUserNamespacePerms = "/Namespaces";

	public static synchronized PermissionHandler getInstance() {
		if ((ZKPermHandler.zkPermHandlerInstance) == null)
			ZKPermHandler.zkPermHandlerInstance = new ZKPermHandler();

		return ZKPermHandler.zkPermHandlerInstance;
	}

	@Override
	public void initialize(String instanceId, boolean initialize) {
	}

	public ZKPermHandler() {
		zooCache = new ZooCache();
	}

	@Override
	public boolean hasTablePermission(String user, String table, TablePermission permission) throws TableNotFoundException {
		byte[] serializedPerms;
		final ZooReaderWriter zrw = ZooReaderWriter.getInstance();
		try {
			String path = (((((ZKUserPath) + "/") + user) + (ZKUserTablePerms)) + "/") + table;
			zrw.sync(path);
			serializedPerms = zrw.getData(path, null);
		} catch (KeeperException e) {
			if ((e.code()) == (NONODE)) {
				try {
					zrw.getData((((ZKTablePath) + "/") + table), null);
					return false;
				} catch (InterruptedException ex) {
					ZKPermHandler.log.warn("Unhandled InterruptedException, failing closed for table permission check", e);
					return false;
				} catch (KeeperException ex) {
					if ((e.code()) == (NONODE)) {
						throw new TableNotFoundException(null, table, "while checking permissions");
					}
					ZKPermHandler.log.warn("Unhandled InterruptedException, failing closed for table permission check", e);
				}
				return false;
			}
			ZKPermHandler.log.warn("Unhandled KeeperException, failing closed for table permission check", e);
			return false;
		} catch (InterruptedException e) {
			ZKPermHandler.log.warn("Unhandled InterruptedException, failing closed for table permission check", e);
			return false;
		}
		if (serializedPerms != null) {
		}
		return false;
	}

	@Override
	public boolean hasCachedTablePermission(String user, String table, TablePermission permission) throws AccumuloSecurityException, TableNotFoundException {
		byte[] serializedPerms = zooCache.get(((((((ZKUserPath) + "/") + user) + (ZKUserTablePerms)) + "/") + table));
		if (serializedPerms != null) {
		}
		return false;
	}

	@Override
	public boolean hasNamespacePermission(String user, String namespace, NamespacePermission permission) throws NamespaceNotFoundException {
		byte[] serializedPerms;
		final ZooReaderWriter zrw = ZooReaderWriter.getInstance();
		try {
			String path = (((((ZKUserPath) + "/") + user) + (ZKUserNamespacePerms)) + "/") + namespace;
			zrw.sync(path);
			serializedPerms = zrw.getData(path, null);
		} catch (KeeperException e) {
			if ((e.code()) == (NONODE)) {
				try {
					zrw.getData((((ZKNamespacePath) + "/") + namespace), null);
					return false;
				} catch (InterruptedException ex) {
					ZKPermHandler.log.warn("Unhandled InterruptedException, failing closed for namespace permission check", e);
					return false;
				} catch (KeeperException ex) {
					if ((e.code()) == (NONODE)) {
						throw new NamespaceNotFoundException(null, namespace, "while checking permissions");
					}
					ZKPermHandler.log.warn("Unhandled InterruptedException, failing closed for table permission check", e);
				}
				return false;
			}
			ZKPermHandler.log.warn("Unhandled KeeperException, failing closed for table permission check", e);
			return false;
		} catch (InterruptedException e) {
			ZKPermHandler.log.warn("Unhandled InterruptedException, failing closed for table permission check", e);
			return false;
		}
		if (serializedPerms != null) {
		}
		return false;
	}

	@Override
	public boolean hasCachedNamespacePermission(String user, String namespace, NamespacePermission permission) throws AccumuloSecurityException, NamespaceNotFoundException {
		byte[] serializedPerms = zooCache.get(((((((ZKUserPath) + "/") + user) + (ZKUserNamespacePerms)) + "/") + namespace));
		if (serializedPerms != null) {
		}
		return false;
	}

	@Override
	public void grantSystemPermission(String user, SystemPermission permission) throws AccumuloSecurityException {
		byte[] permBytes = zooCache.get(((((ZKUserPath) + "/") + user) + (ZKUserSysPerms)));
		Set<SystemPermission> perms;
		if (permBytes == null) {
			perms = new TreeSet<>();
		}else {
		}
		perms = null;
		if (perms.add(permission)) {
			synchronized(zooCache) {
				zooCache.clear();
			}
		}
	}

	@Override
	public void grantTablePermission(String user, String table, TablePermission permission) throws AccumuloSecurityException {
		Set<TablePermission> tablePerms;
		byte[] serializedPerms = zooCache.get(((((((ZKUserPath) + "/") + user) + (ZKUserTablePerms)) + "/") + table));
		if (serializedPerms != null) {
		}else
			tablePerms = new TreeSet<>();

		tablePerms = null;
		if (tablePerms.add(permission)) {
			synchronized(zooCache) {
				zooCache.clear(((((((ZKUserPath) + "/") + user) + (ZKUserTablePerms)) + "/") + table));
			}
		}
	}

	@Override
	public void grantNamespacePermission(String user, String namespace, NamespacePermission permission) throws AccumuloSecurityException {
		Set<NamespacePermission> namespacePerms;
		byte[] serializedPerms = zooCache.get(((((((ZKUserPath) + "/") + user) + (ZKUserNamespacePerms)) + "/") + namespace));
		if (serializedPerms != null) {
		}else
			namespacePerms = new TreeSet<>();

		namespacePerms = null;
		if (namespacePerms.add(permission)) {
			synchronized(zooCache) {
				zooCache.clear(((((((ZKUserPath) + "/") + user) + (ZKUserNamespacePerms)) + "/") + namespace));
			}
		}
	}

	@Override
	public void revokeSystemPermission(String user, SystemPermission permission) throws AccumuloSecurityException {
		byte[] sysPermBytes = zooCache.get(((((ZKUserPath) + "/") + user) + (ZKUserSysPerms)));
		if (sysPermBytes == null)
			return;

	}

	@Override
	public void revokeTablePermission(String user, String table, TablePermission permission) throws AccumuloSecurityException {
		byte[] serializedPerms = zooCache.get(((((((ZKUserPath) + "/") + user) + (ZKUserTablePerms)) + "/") + table));
		if (serializedPerms == null)
			return;

	}

	@Override
	public void revokeNamespacePermission(String user, String namespace, NamespacePermission permission) throws AccumuloSecurityException {
		byte[] serializedPerms = zooCache.get(((((((ZKUserPath) + "/") + user) + (ZKUserNamespacePerms)) + "/") + namespace));
		if (serializedPerms == null)
			return;

	}

	@Override
	public void cleanTablePermissions(String table) throws AccumuloSecurityException {
		try {
			synchronized(zooCache) {
				zooCache.clear();
				IZooReaderWriter zoo = ZooReaderWriter.getInstance();
				for (String user : zooCache.getChildren(ZKUserPath))
					zoo.recursiveDelete(((((((ZKUserPath) + "/") + user) + (ZKUserTablePerms)) + "/") + table), SKIP);

			}
		} catch (KeeperException e) {
			ZKPermHandler.log.error("{}", e.getMessage(), e);
			throw new AccumuloSecurityException("unknownUser", SecurityErrorCode.CONNECTION_ERROR, e);
		} catch (InterruptedException e) {
			ZKPermHandler.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void cleanNamespacePermissions(String namespace) throws AccumuloSecurityException {
		try {
			synchronized(zooCache) {
				zooCache.clear();
				IZooReaderWriter zoo = ZooReaderWriter.getInstance();
				for (String user : zooCache.getChildren(ZKUserPath))
					zoo.recursiveDelete(((((((ZKUserPath) + "/") + user) + (ZKUserNamespacePerms)) + "/") + namespace), SKIP);

			}
		} catch (KeeperException e) {
			ZKPermHandler.log.error("{}", e.getMessage(), e);
			throw new AccumuloSecurityException("unknownUser", SecurityErrorCode.CONNECTION_ERROR, e);
		} catch (InterruptedException e) {
			ZKPermHandler.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void initializeSecurity(TCredentials itw, String rootuser) throws AccumuloSecurityException {
		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		Set<SystemPermission> rootPerms = new TreeSet<>();
		for (SystemPermission p : SystemPermission.values())
			rootPerms.add(p);

		Map<String, Set<TablePermission>> tablePerms = new HashMap<>();
		tablePerms.put(RootTable.ID, Collections.singleton(TablePermission.ALTER_TABLE));
		tablePerms.put(MetadataTable.ID, Collections.singleton(TablePermission.ALTER_TABLE));
		Map<String, Set<NamespacePermission>> namespacePerms = new HashMap<>();
		namespacePerms.put(Namespaces.ACCUMULO_NAMESPACE_ID, Collections.singleton(NamespacePermission.ALTER_NAMESPACE));
		namespacePerms.put(Namespaces.ACCUMULO_NAMESPACE_ID, Collections.singleton(NamespacePermission.ALTER_TABLE));
		try {
			if (!(zoo.exists(ZKUserPath)))
				zoo.putPersistentData(ZKUserPath, rootuser.getBytes(StandardCharsets.UTF_8), FAIL);

			initUser(rootuser);
			for (Map.Entry<String, Set<TablePermission>> entry : tablePerms.entrySet())
				createTablePerm(rootuser, entry.getKey(), entry.getValue());

			for (Map.Entry<String, Set<NamespacePermission>> entry : namespacePerms.entrySet())
				createNamespacePerm(rootuser, entry.getKey(), entry.getValue());

		} catch (KeeperException e) {
			ZKPermHandler.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			ZKPermHandler.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void initUser(String user) throws AccumuloSecurityException {
		IZooReaderWriter zoo = ZooReaderWriter.getInstance();
		try {
			zoo.putPersistentData((((ZKUserPath) + "/") + user), new byte[0], ZooUtil.NodeExistsPolicy.SKIP);
			zoo.putPersistentData(((((ZKUserPath) + "/") + user) + (ZKUserTablePerms)), new byte[0], ZooUtil.NodeExistsPolicy.SKIP);
			zoo.putPersistentData(((((ZKUserPath) + "/") + user) + (ZKUserNamespacePerms)), new byte[0], ZooUtil.NodeExistsPolicy.SKIP);
		} catch (KeeperException e) {
			ZKPermHandler.log.error("{}", e.getMessage(), e);
			throw new AccumuloSecurityException(user, SecurityErrorCode.CONNECTION_ERROR, e);
		} catch (InterruptedException e) {
			ZKPermHandler.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private void createTablePerm(String user, String table, Set<TablePermission> perms) throws InterruptedException, KeeperException {
		synchronized(zooCache) {
			zooCache.clear();
		}
	}

	private void createNamespacePerm(String user, String namespace, Set<NamespacePermission> perms) throws InterruptedException, KeeperException {
		synchronized(zooCache) {
			zooCache.clear();
		}
	}

	@Override
	public void cleanUser(String user) throws AccumuloSecurityException {
		try {
			synchronized(zooCache) {
				IZooReaderWriter zoo = ZooReaderWriter.getInstance();
				zoo.recursiveDelete(((((ZKUserPath) + "/") + user) + (ZKUserSysPerms)), SKIP);
				zoo.recursiveDelete(((((ZKUserPath) + "/") + user) + (ZKUserTablePerms)), SKIP);
				zoo.recursiveDelete(((((ZKUserPath) + "/") + user) + (ZKUserNamespacePerms)), SKIP);
				zooCache.clear((((ZKUserPath) + "/") + user));
			}
		} catch (InterruptedException e) {
			ZKPermHandler.log.error("{}", e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (KeeperException e) {
			ZKPermHandler.log.error("{}", e.getMessage(), e);
			if (e.code().equals(NONODE))
				throw new AccumuloSecurityException(user, SecurityErrorCode.USER_DOESNT_EXIST, e);

			throw new AccumuloSecurityException(user, SecurityErrorCode.CONNECTION_ERROR, e);
		}
	}

	@Override
	public boolean hasSystemPermission(String user, SystemPermission permission) throws AccumuloSecurityException {
		byte[] perms;
		try {
			String path = (((ZKUserPath) + "/") + user) + (ZKUserSysPerms);
			ZooReaderWriter.getInstance().sync(path);
			perms = ZooReaderWriter.getInstance().getData(path, null);
		} catch (KeeperException e) {
			if ((e.code()) == (NONODE)) {
				return false;
			}
			ZKPermHandler.log.warn("Unhandled KeeperException, failing closed for table permission check", e);
			return false;
		} catch (InterruptedException e) {
			ZKPermHandler.log.warn("Unhandled InterruptedException, failing closed for table permission check", e);
			return false;
		}
		if (perms == null)
			return false;

		return false;
	}

	@Override
	public boolean hasCachedSystemPermission(String user, SystemPermission permission) throws AccumuloSecurityException {
		byte[] perms = zooCache.get(((((ZKUserPath) + "/") + user) + (ZKUserSysPerms)));
		if (perms == null)
			return false;

		return false;
	}

	@Override
	public boolean validSecurityHandlers(Authenticator authent, Authorizor author) {
		return true;
	}

	@Override
	public void initTable(String table) throws AccumuloSecurityException {
	}
}

