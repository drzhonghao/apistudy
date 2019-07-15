

import com.google.common.collect.ImmutableList;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.accumulo.fate.zookeeper.ZooReader;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.zookeeper.KeeperException.Code.CONNECTIONLOSS;
import static org.apache.zookeeper.KeeperException.Code.NONODE;
import static org.apache.zookeeper.KeeperException.Code.OPERATIONTIMEOUT;
import static org.apache.zookeeper.KeeperException.Code.SESSIONEXPIRED;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeChildrenChanged;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeCreated;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDataChanged;
import static org.apache.zookeeper.Watcher.Event.EventType.NodeDeleted;
import static org.apache.zookeeper.Watcher.Event.EventType.None;
import static org.apache.zookeeper.Watcher.Event.KeeperState.Disconnected;
import static org.apache.zookeeper.Watcher.Event.KeeperState.Expired;
import static org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected;


public class ZooCache {
	private static final Logger log = LoggerFactory.getLogger(ZooCache.class);

	private final ZooCache.ZCacheWatcher watcher = new ZooCache.ZCacheWatcher();

	private final Watcher externalWatcher;

	private final ReadWriteLock cacheLock = new ReentrantReadWriteLock(false);

	private final Lock cacheWriteLock = cacheLock.writeLock();

	private final Lock cacheReadLock = cacheLock.readLock();

	private final HashMap<String, byte[]> cache;

	private final HashMap<String, ZooCache.ZcStat> statCache;

	private final HashMap<String, List<String>> childrenCache;

	private final ZooReader zReader;

	public static class ZcStat {
		private long ephemeralOwner;

		public ZcStat() {
		}

		private ZcStat(Stat stat) {
			this.ephemeralOwner = stat.getEphemeralOwner();
		}

		public long getEphemeralOwner() {
			return ephemeralOwner;
		}

		private void set(ZooCache.ZcStat cachedStat) {
			this.ephemeralOwner = cachedStat.ephemeralOwner;
		}

		@com.google.common.annotations.VisibleForTesting
		public void setEphemeralOwner(long ephemeralOwner) {
			this.ephemeralOwner = ephemeralOwner;
		}
	}

	private static class ImmutableCacheCopies {
		final Map<String, byte[]> cache;

		final Map<String, ZooCache.ZcStat> statCache;

		final Map<String, List<String>> childrenCache;

		final long updateCount;

		ImmutableCacheCopies(long updateCount) {
			this.updateCount = updateCount;
			cache = Collections.emptyMap();
			statCache = Collections.emptyMap();
			childrenCache = Collections.emptyMap();
		}

		ImmutableCacheCopies(long updateCount, Map<String, byte[]> cache, Map<String, ZooCache.ZcStat> statCache, Map<String, List<String>> childrenCache) {
			this.updateCount = updateCount;
			this.cache = Collections.unmodifiableMap(new HashMap<>(cache));
			this.statCache = Collections.unmodifiableMap(new HashMap<>(statCache));
			this.childrenCache = Collections.unmodifiableMap(new HashMap<>(childrenCache));
		}

		ImmutableCacheCopies(long updateCount, ZooCache.ImmutableCacheCopies prev, Map<String, List<String>> childrenCache) {
			this.updateCount = updateCount;
			this.cache = prev.cache;
			this.statCache = prev.statCache;
			this.childrenCache = Collections.unmodifiableMap(new HashMap<>(childrenCache));
		}

		ImmutableCacheCopies(long updateCount, Map<String, byte[]> cache, Map<String, ZooCache.ZcStat> statCache, ZooCache.ImmutableCacheCopies prev) {
			this.updateCount = updateCount;
			this.cache = Collections.unmodifiableMap(new HashMap<>(cache));
			this.statCache = Collections.unmodifiableMap(new HashMap<>(statCache));
			this.childrenCache = prev.childrenCache;
		}
	}

	private volatile ZooCache.ImmutableCacheCopies immutableCache = new ZooCache.ImmutableCacheCopies(0);

	private long updateCount = 0;

	private ZooKeeper getZooKeeper() {
		return null;
	}

	private class ZCacheWatcher implements Watcher {
		@Override
		public void process(WatchedEvent event) {
			if (ZooCache.log.isTraceEnabled()) {
				ZooCache.log.trace("{}", event);
			}
			switch (event.getType()) {
				case NodeDataChanged :
				case NodeChildrenChanged :
				case NodeCreated :
				case NodeDeleted :
					remove(event.getPath());
					break;
				case None :
					switch (event.getState()) {
						case Disconnected :
							if (ZooCache.log.isTraceEnabled())
								ZooCache.log.trace("Zoo keeper connection disconnected, clearing cache");

							clear();
							break;
						case SyncConnected :
							break;
						case Expired :
							if (ZooCache.log.isTraceEnabled())
								ZooCache.log.trace("Zoo keeper connection expired, clearing cache");

							clear();
							break;
						default :
							ZooCache.log.warn(("Unhandled: " + event));
							break;
					}
					break;
				default :
					ZooCache.log.warn(("Unhandled: " + event));
					break;
			}
			if ((externalWatcher) != null) {
				externalWatcher.process(event);
			}
		}
	}

	public ZooCache(String zooKeepers, int sessionTimeout) {
		this(zooKeepers, sessionTimeout, null);
	}

	public ZooCache(String zooKeepers, int sessionTimeout, Watcher watcher) {
		this(new ZooReader(zooKeepers, sessionTimeout), watcher);
	}

	public ZooCache(ZooReader reader, Watcher watcher) {
		this.zReader = reader;
		this.cache = new HashMap<>();
		this.statCache = new HashMap<>();
		this.childrenCache = new HashMap<>();
		this.externalWatcher = watcher;
	}

	private abstract class ZooRunnable<T> {
		abstract T run() throws InterruptedException, KeeperException;

		public T retry() {
			int sleepTime = 100;
			while (true) {
				try {
					return run();
				} catch (KeeperException e) {
					final KeeperException.Code code = e.code();
					if (code == (NONODE)) {
						ZooCache.log.error(("Looked up non-existent node in cache " + (e.getPath())), e);
					}else
						if (((code == (CONNECTIONLOSS)) || (code == (OPERATIONTIMEOUT))) || (code == (SESSIONEXPIRED))) {
							ZooCache.log.warn("Saw (possibly) transient exception communicating with ZooKeeper, will retry", e);
						}else {
							ZooCache.log.warn("Zookeeper error, will retry", e);
						}

				} catch (InterruptedException e) {
					ZooCache.log.info("Zookeeper error, will retry", e);
				} catch (ConcurrentModificationException e) {
					ZooCache.log.debug("Zookeeper was modified, will retry");
				}
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					ZooCache.log.debug("Wait in retry() was interrupted.", e);
				}
				LockSupport.parkNanos(sleepTime);
				if (sleepTime < 10000) {
					sleepTime = ((int) (sleepTime + (sleepTime * (Math.random()))));
				}
			} 
		}
	}

	public List<String> getChildren(final String zPath) {
		ZooCache.ZooRunnable<List<String>> zr = new ZooCache.ZooRunnable<List<String>>() {
			@Override
			public List<String> run() throws InterruptedException, KeeperException {
				ZooCache.ImmutableCacheCopies lic = immutableCache;
				if (lic.childrenCache.containsKey(zPath)) {
					return lic.childrenCache.get(zPath);
				}
				cacheWriteLock.lock();
				try {
					if (childrenCache.containsKey(zPath)) {
						return childrenCache.get(zPath);
					}
					final ZooKeeper zooKeeper = getZooKeeper();
					List<String> children = zooKeeper.getChildren(zPath, watcher);
					if (children != null) {
						children = ImmutableList.copyOf(children);
					}
					childrenCache.put(zPath, children);
					immutableCache = new ZooCache.ImmutableCacheCopies((++(updateCount)), immutableCache, childrenCache);
					return children;
				} catch (KeeperException ke) {
					if ((ke.code()) != (NONODE)) {
						throw ke;
					}
				} finally {
					cacheWriteLock.unlock();
				}
				return null;
			}
		};
		return zr.retry();
	}

	public byte[] get(final String zPath) {
		return get(zPath, null);
	}

	public byte[] get(final String zPath, final ZooCache.ZcStat status) {
		ZooCache.ZooRunnable<byte[]> zr = new ZooCache.ZooRunnable<byte[]>() {
			@Override
			public byte[] run() throws InterruptedException, KeeperException {
				ZooCache.ZcStat zstat = null;
				ZooCache.ImmutableCacheCopies lic = immutableCache;
				byte[] val = lic.cache.get(zPath);
				if ((val != null) || (lic.cache.containsKey(zPath))) {
					if (status != null) {
						zstat = lic.statCache.get(zPath);
						copyStats(status, zstat);
					}
					return val;
				}
				cacheWriteLock.lock();
				try {
					final ZooKeeper zooKeeper = getZooKeeper();
					Stat stat = zooKeeper.exists(zPath, watcher);
					byte[] data = null;
					if (stat == null) {
						if (ZooCache.log.isTraceEnabled()) {
							ZooCache.log.trace(("zookeeper did not contain " + zPath));
						}
					}else {
						try {
							data = zooKeeper.getData(zPath, watcher, stat);
							zstat = new ZooCache.ZcStat(stat);
						} catch (KeeperException.BadVersionException e1) {
							throw new ConcurrentModificationException();
						} catch (KeeperException.NoNodeException e2) {
							throw new ConcurrentModificationException();
						}
						if (ZooCache.log.isTraceEnabled()) {
							ZooCache.log.trace(((("zookeeper contained " + zPath) + " ") + (data == null ? null : new String(data, StandardCharsets.UTF_8))));
						}
					}
					put(zPath, data, zstat);
					copyStats(status, zstat);
					return data;
				} finally {
					cacheWriteLock.unlock();
				}
			}
		};
		return zr.retry();
	}

	protected void copyStats(ZooCache.ZcStat userStat, ZooCache.ZcStat cachedStat) {
		if ((userStat != null) && (cachedStat != null)) {
			userStat.set(cachedStat);
		}
	}

	private void put(String zPath, byte[] data, ZooCache.ZcStat stat) {
		cacheWriteLock.lock();
		try {
			cache.put(zPath, data);
			statCache.put(zPath, stat);
			immutableCache = new ZooCache.ImmutableCacheCopies((++(updateCount)), cache, statCache, immutableCache);
		} finally {
			cacheWriteLock.unlock();
		}
	}

	private void remove(String zPath) {
		cacheWriteLock.lock();
		try {
			cache.remove(zPath);
			childrenCache.remove(zPath);
			statCache.remove(zPath);
			immutableCache = new ZooCache.ImmutableCacheCopies((++(updateCount)), cache, statCache, childrenCache);
		} finally {
			cacheWriteLock.unlock();
		}
	}

	public void clear() {
		cacheWriteLock.lock();
		try {
			cache.clear();
			childrenCache.clear();
			statCache.clear();
			immutableCache = new ZooCache.ImmutableCacheCopies((++(updateCount)));
		} finally {
			cacheWriteLock.unlock();
		}
	}

	public long getUpdateCount() {
		return immutableCache.updateCount;
	}

	@com.google.common.annotations.VisibleForTesting
	boolean dataCached(String zPath) {
		cacheReadLock.lock();
		try {
			return (immutableCache.cache.containsKey(zPath)) && (cache.containsKey(zPath));
		} finally {
			cacheReadLock.unlock();
		}
	}

	@com.google.common.annotations.VisibleForTesting
	boolean childrenCached(String zPath) {
		cacheReadLock.lock();
		try {
			return (immutableCache.childrenCache.containsKey(zPath)) && (childrenCache.containsKey(zPath));
		} finally {
			cacheReadLock.unlock();
		}
	}

	public void clear(String zPath) {
		cacheWriteLock.lock();
		try {
			for (Iterator<String> i = cache.keySet().iterator(); i.hasNext();) {
				String path = i.next();
				if (path.startsWith(zPath))
					i.remove();

			}
			for (Iterator<String> i = childrenCache.keySet().iterator(); i.hasNext();) {
				String path = i.next();
				if (path.startsWith(zPath))
					i.remove();

			}
			for (Iterator<String> i = statCache.keySet().iterator(); i.hasNext();) {
				String path = i.next();
				if (path.startsWith(zPath))
					i.remove();

			}
			immutableCache = new ZooCache.ImmutableCacheCopies((++(updateCount)), cache, statCache, childrenCache);
		} finally {
			cacheWriteLock.unlock();
		}
	}
}

