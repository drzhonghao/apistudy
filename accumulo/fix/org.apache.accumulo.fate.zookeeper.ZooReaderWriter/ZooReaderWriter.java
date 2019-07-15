

import java.security.SecurityPermission;
import java.util.List;
import org.apache.accumulo.fate.util.Retry;
import org.apache.accumulo.fate.zookeeper.IZooReaderWriter;
import org.apache.accumulo.fate.zookeeper.ZooReader;
import org.apache.accumulo.fate.zookeeper.ZooUtil;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy.SKIP;
import static org.apache.zookeeper.KeeperException.Code.BADVERSION;
import static org.apache.zookeeper.KeeperException.Code.CONNECTIONLOSS;
import static org.apache.zookeeper.KeeperException.Code.NODEEXISTS;
import static org.apache.zookeeper.KeeperException.Code.NONODE;
import static org.apache.zookeeper.KeeperException.Code.OPERATIONTIMEOUT;
import static org.apache.zookeeper.KeeperException.Code.SESSIONEXPIRED;


public class ZooReaderWriter extends ZooReader implements IZooReaderWriter {
	private static final Logger log = LoggerFactory.getLogger(ZooReaderWriter.class);

	private static SecurityPermission ZOOWRITER_PERMISSION = new SecurityPermission("zookeeperWriterPermission");

	private static ZooReaderWriter instance = null;

	private final String scheme;

	private final byte[] auth;

	@Override
	public ZooKeeper getZooKeeper() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			sm.checkPermission(ZooReaderWriter.ZOOWRITER_PERMISSION);
		}
		return getSession(keepers, timeout, scheme, auth);
	}

	@Override
	public boolean putPersistentData(String zPath, byte[] data, ZooUtil.NodeExistsPolicy policy) throws InterruptedException, KeeperException {
		return false;
	}

	@Override
	public boolean putPrivatePersistentData(String zPath, byte[] data, ZooUtil.NodeExistsPolicy policy) throws InterruptedException, KeeperException {
		return false;
	}

	@Override
	public void putPersistentData(String zPath, byte[] data, int version, ZooUtil.NodeExistsPolicy policy) throws InterruptedException, KeeperException {
	}

	@Override
	public String putPersistentSequential(String zPath, byte[] data) throws InterruptedException, KeeperException {
		return null;
	}

	@Override
	public String putEphemeralData(String zPath, byte[] data) throws InterruptedException, KeeperException {
		return null;
	}

	@Override
	public void recursiveCopyPersistent(String source, String destination, ZooUtil.NodeExistsPolicy policy) throws InterruptedException, KeeperException {
	}

	@Override
	public void delete(String path, int version) throws InterruptedException, KeeperException {
		final Retry retry = getRetryFactory().createRetry();
		while (true) {
			try {
				getZooKeeper().delete(path, version);
				return;
			} catch (KeeperException e) {
				final KeeperException.Code code = e.code();
				if (code == (NONODE)) {
					if (retry.hasRetried()) {
						ZooReaderWriter.log.debug("Delete saw no node on a retry. Assuming node was deleted");
						return;
					}
					throw e;
				}else
					if (((code == (CONNECTIONLOSS)) || (code == (OPERATIONTIMEOUT))) || (code == (SESSIONEXPIRED))) {
						retryOrThrow(retry, e);
					}else {
						throw e;
					}

			}
			retry.waitForNextAttempt();
		} 
	}

	@Override
	public byte[] mutate(String zPath, byte[] createValue, List<ACL> acl, IZooReaderWriter.Mutator mutator) throws Exception {
		if (createValue != null) {
			while (true) {
				final Retry retry = getRetryFactory().createRetry();
				try {
					getZooKeeper().create(zPath, createValue, acl, CreateMode.PERSISTENT);
					return createValue;
				} catch (KeeperException ex) {
					final KeeperException.Code code = ex.code();
					if (code == (NODEEXISTS)) {
						break;
					}else
						if (((code == (OPERATIONTIMEOUT)) || (code == (CONNECTIONLOSS))) || (code == (SESSIONEXPIRED))) {
							retryOrThrow(retry, ex);
						}else {
							throw ex;
						}

				}
				retry.waitForNextAttempt();
			} 
		}
		do {
			final Retry retry = getRetryFactory().createRetry();
			Stat stat = new Stat();
			byte[] data = getData(zPath, false, stat);
			data = mutator.mutate(data);
			if (data == null)
				return data;

			try {
				getZooKeeper().setData(zPath, data, stat.getVersion());
				return data;
			} catch (KeeperException ex) {
				final KeeperException.Code code = ex.code();
				if (code == (BADVERSION)) {
				}else
					if (((code == (OPERATIONTIMEOUT)) || (code == (CONNECTIONLOSS))) || (code == (SESSIONEXPIRED))) {
						retryOrThrow(retry, ex);
						retry.waitForNextAttempt();
					}else {
						throw ex;
					}

			}
		} while (true );
	}

	public static synchronized ZooReaderWriter getInstance(String zookeepers, int timeInMillis, String scheme, byte[] auth) {
		if ((ZooReaderWriter.instance) == null) {
		}
		return ZooReaderWriter.instance;
	}

	@Override
	public boolean isLockHeld(ZooUtil.LockID lockID) throws InterruptedException, KeeperException {
		return false;
	}

	@Override
	public void mkdirs(String path) throws InterruptedException, KeeperException {
		if (path.equals(""))
			return;

		if (!(path.startsWith("/")))
			throw new IllegalArgumentException((path + "does not start with /"));

		if (exists(path))
			return;

		String parent = path.substring(0, path.lastIndexOf("/"));
		mkdirs(parent);
		putPersistentData(path, new byte[]{  }, SKIP);
	}
}

