

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.accumulo.fate.util.Retry;
import org.apache.accumulo.fate.zookeeper.IZooReader;
import org.apache.accumulo.fate.zookeeper.ZooSession;
import org.apache.accumulo.fate.zookeeper.ZooUtil;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.zookeeper.KeeperException.Code.CONNECTIONLOSS;
import static org.apache.zookeeper.KeeperException.Code.OK;
import static org.apache.zookeeper.KeeperException.Code.OPERATIONTIMEOUT;
import static org.apache.zookeeper.KeeperException.Code.SESSIONEXPIRED;
import static org.apache.zookeeper.KeeperException.Code.get;


public class ZooReader implements IZooReader {
	private static final Logger log = LoggerFactory.getLogger(ZooReader.class);

	protected String keepers;

	protected int timeout;

	private final Retry.RetryFactory retryFactory;

	protected ZooKeeper getSession(String keepers, int timeout, String scheme, byte[] auth) {
		return ZooSession.getSession(keepers, timeout, scheme, auth);
	}

	protected ZooKeeper getZooKeeper() {
		return getSession(keepers, timeout, null, null);
	}

	protected Retry.RetryFactory getRetryFactory() {
		return retryFactory;
	}

	protected void retryOrThrow(Retry retry, KeeperException e) throws KeeperException {
		ZooReader.log.warn("Saw (possibly) transient exception communicating with ZooKeeper", e);
		if (retry.canRetry()) {
			retry.useRetry();
			return;
		}
		ZooReader.log.error((("Retry attempts (" + (retry.retriesCompleted())) + ") exceeded trying to communicate with ZooKeeper"));
		throw e;
	}

	@Override
	public byte[] getData(String zPath, Stat stat) throws InterruptedException, KeeperException {
		return getData(zPath, false, stat);
	}

	@Override
	public byte[] getData(String zPath, boolean watch, Stat stat) throws InterruptedException, KeeperException {
		final Retry retry = getRetryFactory().createRetry();
		while (true) {
			try {
				return getZooKeeper().getData(zPath, watch, stat);
			} catch (KeeperException e) {
				final KeeperException.Code code = e.code();
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
	public byte[] getData(String zPath, Watcher watcher, Stat stat) throws InterruptedException, KeeperException {
		final Retry retry = getRetryFactory().createRetry();
		while (true) {
			try {
				return getZooKeeper().getData(zPath, watcher, stat);
			} catch (KeeperException e) {
				final KeeperException.Code code = e.code();
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
	public Stat getStatus(String zPath) throws InterruptedException, KeeperException {
		final Retry retry = getRetryFactory().createRetry();
		while (true) {
			try {
				return getZooKeeper().exists(zPath, false);
			} catch (KeeperException e) {
				final KeeperException.Code code = e.code();
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
	public Stat getStatus(String zPath, Watcher watcher) throws InterruptedException, KeeperException {
		final Retry retry = getRetryFactory().createRetry();
		while (true) {
			try {
				return getZooKeeper().exists(zPath, watcher);
			} catch (KeeperException e) {
				final KeeperException.Code code = e.code();
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
	public List<String> getChildren(String zPath) throws InterruptedException, KeeperException {
		final Retry retry = getRetryFactory().createRetry();
		while (true) {
			try {
				return getZooKeeper().getChildren(zPath, false);
			} catch (KeeperException e) {
				final KeeperException.Code code = e.code();
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
	public List<String> getChildren(String zPath, Watcher watcher) throws InterruptedException, KeeperException {
		final Retry retry = getRetryFactory().createRetry();
		while (true) {
			try {
				return getZooKeeper().getChildren(zPath, watcher);
			} catch (KeeperException e) {
				final KeeperException.Code code = e.code();
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
	public boolean exists(String zPath) throws InterruptedException, KeeperException {
		final Retry retry = getRetryFactory().createRetry();
		while (true) {
			try {
				return (getZooKeeper().exists(zPath, false)) != null;
			} catch (KeeperException e) {
				final KeeperException.Code code = e.code();
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
	public boolean exists(String zPath, Watcher watcher) throws InterruptedException, KeeperException {
		final Retry retry = getRetryFactory().createRetry();
		while (true) {
			try {
				return (getZooKeeper().exists(zPath, watcher)) != null;
			} catch (KeeperException e) {
				final KeeperException.Code code = e.code();
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
	public void sync(final String path) throws InterruptedException, KeeperException {
		final AtomicInteger rc = new AtomicInteger();
		final CountDownLatch waiter = new CountDownLatch(1);
		getZooKeeper().sync(path, new AsyncCallback.VoidCallback() {
			@Override
			public void processResult(int code, String arg1, Object arg2) {
				rc.set(code);
				waiter.countDown();
			}
		}, null);
		waiter.await();
		KeeperException.Code code = get(rc.get());
		if (code != (OK)) {
			throw KeeperException.create(code);
		}
	}

	@Override
	public List<ACL> getACL(String zPath, Stat stat) throws InterruptedException, KeeperException {
		return null;
	}

	public ZooReader(String keepers, int timeout) {
		this.keepers = keepers;
		this.timeout = timeout;
		this.retryFactory = ZooUtil.DEFAULT_RETRY;
	}
}

