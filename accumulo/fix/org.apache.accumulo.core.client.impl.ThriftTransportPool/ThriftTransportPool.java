

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.security.SecurityPermission;
import java.util.AbstractCollection;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ThriftTransportKey;
import org.apache.accumulo.core.util.Daemon;
import org.apache.accumulo.core.util.HostAndPort;
import org.apache.accumulo.core.util.Pair;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ThriftTransportPool {
	private static SecurityPermission TRANSPORT_POOL_PERMISSION = new SecurityPermission("transportPoolPermission");

	private static final Random random = new Random();

	private long killTime = 1000 * 3;

	private static class CachedConnections {
		LinkedList<ThriftTransportPool.CachedConnection> unreserved = new LinkedList<>();

		Map<ThriftTransportPool.CachedTTransport, ThriftTransportPool.CachedConnection> reserved = new HashMap<>();

		public ThriftTransportPool.CachedConnection reserveAny() {
			if ((unreserved.size()) > 0) {
				ThriftTransportPool.CachedConnection cachedConnection = unreserved.removeFirst();
				cachedConnection.reserve();
				reserved.put(cachedConnection.transport, cachedConnection);
				if (ThriftTransportPool.log.isTraceEnabled()) {
					ThriftTransportPool.log.trace("Using existing connection to {}", cachedConnection.transport.cacheKey);
				}
				return cachedConnection;
			}
			return null;
		}
	}

	private Map<ThriftTransportKey, ThriftTransportPool.CachedConnections> cache = new HashMap<>();

	private Map<ThriftTransportKey, Long> errorCount = new HashMap<>();

	private Map<ThriftTransportKey, Long> errorTime = new HashMap<>();

	private Set<ThriftTransportKey> serversWarnedAbout = new HashSet<>();

	private CountDownLatch closerExitLatch;

	private static final Logger log = LoggerFactory.getLogger(ThriftTransportPool.class);

	private static final Long ERROR_THRESHOLD = 20L;

	private static final int STUCK_THRESHOLD = (2 * 60) * 1000;

	private static class CachedConnection {
		public CachedConnection(ThriftTransportPool.CachedTTransport t) {
			this.transport = t;
		}

		void reserve() {
			Preconditions.checkState((!(this.transport.reserved)));
			this.transport.setReserved(true);
		}

		void unreserve() {
			Preconditions.checkState(this.transport.reserved);
			this.transport.setReserved(false);
		}

		final ThriftTransportPool.CachedTTransport transport;

		long lastReturnTime;
	}

	public static class TransportPoolShutdownException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	private static class Closer implements Runnable {
		final ThriftTransportPool pool;

		private CountDownLatch closerExitLatch;

		public Closer(ThriftTransportPool pool, CountDownLatch closerExitLatch) {
			this.pool = pool;
			this.closerExitLatch = closerExitLatch;
		}

		private void closeConnections() {
			while (true) {
				ArrayList<ThriftTransportPool.CachedConnection> connectionsToClose = new ArrayList<>();
				synchronized(pool) {
					for (ThriftTransportPool.CachedConnections cachedConns : pool.getCache().values()) {
						Iterator<ThriftTransportPool.CachedConnection> iter = cachedConns.unreserved.iterator();
						while (iter.hasNext()) {
							ThriftTransportPool.CachedConnection cachedConnection = iter.next();
							if (((System.currentTimeMillis()) - (cachedConnection.lastReturnTime)) > (pool.killTime)) {
								connectionsToClose.add(cachedConnection);
								iter.remove();
							}
						} 
						for (ThriftTransportPool.CachedConnection cachedConnection : cachedConns.reserved.values()) {
							cachedConnection.transport.checkForStuckIO(ThriftTransportPool.STUCK_THRESHOLD);
						}
					}
					Iterator<Map.Entry<ThriftTransportKey, Long>> iter = pool.errorTime.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry<ThriftTransportKey, Long> entry = iter.next();
						long delta = (System.currentTimeMillis()) - (entry.getValue());
						if (delta >= (ThriftTransportPool.STUCK_THRESHOLD)) {
							pool.errorCount.remove(entry.getKey());
							iter.remove();
						}
					} 
				}
				for (ThriftTransportPool.CachedConnection cachedConnection : connectionsToClose) {
					cachedConnection.transport.close();
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					ThriftTransportPool.log.debug("Sleep interrupted in closeConnections()", e);
				}
			} 
		}

		@Override
		public void run() {
			try {
				closeConnections();
			} catch (ThriftTransportPool.TransportPoolShutdownException e) {
			} finally {
				closerExitLatch.countDown();
			}
		}
	}

	static class CachedTTransport extends TTransport {
		private ThriftTransportKey cacheKey;

		private TTransport wrappedTransport;

		private boolean sawError = false;

		private volatile String ioThreadName = null;

		private volatile long ioStartTime = 0;

		private volatile boolean reserved = false;

		private String stuckThreadName = null;

		int ioCount = 0;

		int lastIoCount = -1;

		private void sawError(Exception e) {
			sawError = true;
		}

		final void setReserved(boolean reserved) {
			this.reserved = reserved;
			if (reserved) {
				ioThreadName = Thread.currentThread().getName();
				ioCount = 0;
				lastIoCount = -1;
			}else {
				if (((ioCount) & 1) == 1) {
					ThriftTransportPool.log.warn("Connection returned to thrift connection pool that may still be in use {} {}", ioThreadName, Thread.currentThread().getName());
				}
				ioCount = 0;
				lastIoCount = -1;
				ioThreadName = null;
			}
			checkForStuckIO(ThriftTransportPool.STUCK_THRESHOLD);
		}

		final void checkForStuckIO(long threshold) {
			if (((ioCount) & 1) == 1) {
				if ((ioCount) == (lastIoCount)) {
					long delta = (System.currentTimeMillis()) - (ioStartTime);
					if ((delta >= threshold) && ((stuckThreadName) == null)) {
						stuckThreadName = ioThreadName;
						ThriftTransportPool.log.warn("Thread \"{}\" stuck on IO to {} for at least {} ms", ioThreadName, cacheKey);
					}
				}else {
					lastIoCount = ioCount;
					ioStartTime = System.currentTimeMillis();
					if ((stuckThreadName) != null) {
						ThriftTransportPool.log.info("Thread \"{}\" no longer stuck on IO to {} sawError = {}", stuckThreadName, cacheKey);
						stuckThreadName = null;
					}
				}
			}else {
				if ((stuckThreadName) != null) {
					ThriftTransportPool.log.info("Thread \"{}\" no longer stuck on IO to {} sawError = {}", stuckThreadName, cacheKey);
					stuckThreadName = null;
				}
			}
		}

		public CachedTTransport(TTransport transport, ThriftTransportKey cacheKey2) {
			this.wrappedTransport = transport;
			this.cacheKey = cacheKey2;
		}

		@Override
		public boolean isOpen() {
			return wrappedTransport.isOpen();
		}

		@Override
		public void open() throws TTransportException {
			try {
				(ioCount)++;
				wrappedTransport.open();
			} catch (TTransportException tte) {
				sawError(tte);
				throw tte;
			} finally {
				(ioCount)++;
			}
		}

		@Override
		public int read(byte[] arg0, int arg1, int arg2) throws TTransportException {
			try {
				(ioCount)++;
				return wrappedTransport.read(arg0, arg1, arg2);
			} catch (TTransportException tte) {
				sawError(tte);
				throw tte;
			} finally {
				(ioCount)++;
			}
		}

		@Override
		public int readAll(byte[] arg0, int arg1, int arg2) throws TTransportException {
			try {
				(ioCount)++;
				return wrappedTransport.readAll(arg0, arg1, arg2);
			} catch (TTransportException tte) {
				sawError(tte);
				throw tte;
			} finally {
				(ioCount)++;
			}
		}

		@Override
		public void write(byte[] arg0, int arg1, int arg2) throws TTransportException {
			try {
				(ioCount)++;
				wrappedTransport.write(arg0, arg1, arg2);
			} catch (TTransportException tte) {
				sawError(tte);
				throw tte;
			} finally {
				(ioCount)++;
			}
		}

		@Override
		public void write(byte[] arg0) throws TTransportException {
			try {
				(ioCount)++;
				wrappedTransport.write(arg0);
			} catch (TTransportException tte) {
				sawError(tte);
				throw tte;
			} finally {
				(ioCount)++;
			}
		}

		@Override
		public void close() {
			try {
				(ioCount)++;
				wrappedTransport.close();
			} finally {
				(ioCount)++;
			}
		}

		@Override
		public void flush() throws TTransportException {
			try {
				(ioCount)++;
				wrappedTransport.flush();
			} catch (TTransportException tte) {
				sawError(tte);
				throw tte;
			} finally {
				(ioCount)++;
			}
		}

		@Override
		public boolean peek() {
			try {
				(ioCount)++;
				return wrappedTransport.peek();
			} finally {
				(ioCount)++;
			}
		}

		@Override
		public byte[] getBuffer() {
			try {
				(ioCount)++;
				return wrappedTransport.getBuffer();
			} finally {
				(ioCount)++;
			}
		}

		@Override
		public int getBufferPosition() {
			try {
				(ioCount)++;
				return wrappedTransport.getBufferPosition();
			} finally {
				(ioCount)++;
			}
		}

		@Override
		public int getBytesRemainingInBuffer() {
			try {
				(ioCount)++;
				return wrappedTransport.getBytesRemainingInBuffer();
			} finally {
				(ioCount)++;
			}
		}

		@Override
		public void consumeBuffer(int len) {
			try {
				(ioCount)++;
				wrappedTransport.consumeBuffer(len);
			} finally {
				(ioCount)++;
			}
		}

		public ThriftTransportKey getCacheKey() {
			return cacheKey;
		}
	}

	private ThriftTransportPool() {
	}

	public TTransport getTransport(HostAndPort location, long milliseconds, ClientContext context) throws TTransportException {
		return getTransport(new ThriftTransportKey(location, milliseconds, context));
	}

	private TTransport getTransport(ThriftTransportKey cacheKey) throws TTransportException {
		cacheKey.precomputeHashCode();
		synchronized(this) {
			ThriftTransportPool.CachedConnections ccl = getCache().get(cacheKey);
			if (ccl == null) {
				ccl = new ThriftTransportPool.CachedConnections();
				getCache().put(cacheKey, ccl);
			}
			ThriftTransportPool.CachedConnection cachedConnection = ccl.reserveAny();
			if (cachedConnection != null) {
				return cachedConnection.transport;
			}
		}
		return null;
	}

	@com.google.common.annotations.VisibleForTesting
	public Pair<String, TTransport> getAnyTransport(List<ThriftTransportKey> servers, boolean preferCachedConnection) throws TTransportException {
		servers = new ArrayList<>(servers);
		if (preferCachedConnection) {
			HashSet<ThriftTransportKey> serversSet = new HashSet<>(servers);
			synchronized(this) {
				serversSet.retainAll(getCache().keySet());
				if ((serversSet.size()) > 0) {
					ArrayList<ThriftTransportKey> cachedServers = new ArrayList<>(serversSet);
					Collections.shuffle(cachedServers, ThriftTransportPool.random);
					for (ThriftTransportKey ttk : cachedServers) {
						ThriftTransportPool.CachedConnection cachedConnection = getCache().get(ttk).reserveAny();
						if (cachedConnection != null) {
						}
					}
				}
			}
		}
		int retryCount = 0;
		while (((servers.size()) > 0) && (retryCount < 10)) {
			int index = ThriftTransportPool.random.nextInt(servers.size());
			ThriftTransportKey ttk = servers.get(index);
			if (preferCachedConnection) {
				synchronized(this) {
					ThriftTransportPool.CachedConnections cachedConns = getCache().get(ttk);
					if (cachedConns != null) {
						ThriftTransportPool.CachedConnection cachedConnection = cachedConns.reserveAny();
						if (cachedConnection != null) {
						}
					}
				}
			}
		} 
		throw new TTransportException("Failed to connect to a server");
	}

	public void returnTransport(TTransport tsc) {
		if (tsc == null) {
			return;
		}
		boolean existInCache = false;
		ThriftTransportPool.CachedTTransport ctsc = ((ThriftTransportPool.CachedTTransport) (tsc));
		ArrayList<ThriftTransportPool.CachedConnection> closeList = new ArrayList<>();
		synchronized(this) {
			ThriftTransportPool.CachedConnections cachedConns = getCache().get(ctsc.getCacheKey());
			if (cachedConns != null) {
				ThriftTransportPool.CachedConnection cachedConnection = cachedConns.reserved.remove(ctsc);
				if (cachedConnection != null) {
					if (ctsc.sawError) {
						closeList.add(cachedConnection);
						ThriftTransportPool.log.trace("Returned connection had error {}", ctsc.getCacheKey());
						Long ecount = errorCount.get(ctsc.getCacheKey());
						if (ecount == null)
							ecount = 0L;

						ecount++;
						errorCount.put(ctsc.getCacheKey(), ecount);
						Long etime = errorTime.get(ctsc.getCacheKey());
						if (etime == null) {
							errorTime.put(ctsc.getCacheKey(), System.currentTimeMillis());
						}
						if ((ecount >= (ThriftTransportPool.ERROR_THRESHOLD)) && (!(serversWarnedAbout.contains(ctsc.getCacheKey())))) {
							ThriftTransportPool.log.warn("Server {} had {} failures in a short time period, will not complain anymore", ctsc.getCacheKey(), ecount);
							serversWarnedAbout.add(ctsc.getCacheKey());
						}
						cachedConnection.unreserve();
						closeList.addAll(cachedConns.unreserved);
						cachedConns.unreserved.clear();
					}else {
						ThriftTransportPool.log.trace("Returned connection {} ioCount: {}", ctsc.getCacheKey(), cachedConnection.transport.ioCount);
						cachedConnection.lastReturnTime = System.currentTimeMillis();
						cachedConnection.unreserve();
						cachedConns.unreserved.addFirst(cachedConnection);
					}
					existInCache = true;
				}
			}
		}
		for (ThriftTransportPool.CachedConnection cachedConnection : closeList) {
			try {
				cachedConnection.transport.close();
			} catch (Exception e) {
				ThriftTransportPool.log.debug("Failed to close connection w/ errors", e);
			}
		}
		if (!existInCache) {
			ThriftTransportPool.log.warn("Returned tablet server connection to cache that did not come from cache");
			tsc.close();
		}
	}

	public synchronized void setIdleTime(long time) {
		this.killTime = time;
		ThriftTransportPool.log.debug("Set thrift transport pool idle time to {}", time);
	}

	private static ThriftTransportPool instance = new ThriftTransportPool();

	private static final AtomicBoolean daemonStarted = new AtomicBoolean(false);

	public static ThriftTransportPool getInstance() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			sm.checkPermission(ThriftTransportPool.TRANSPORT_POOL_PERMISSION);
		}
		if (ThriftTransportPool.daemonStarted.compareAndSet(false, true)) {
			CountDownLatch closerExitLatch = new CountDownLatch(1);
			new Daemon(new ThriftTransportPool.Closer(ThriftTransportPool.instance, closerExitLatch), "Thrift Connection Pool Checker").start();
			ThriftTransportPool.instance.setCloserExitLatch(closerExitLatch);
		}
		return ThriftTransportPool.instance;
	}

	private synchronized void setCloserExitLatch(CountDownLatch closerExitLatch) {
		this.closerExitLatch = closerExitLatch;
	}

	public void shutdown() {
		synchronized(this) {
			if ((cache) == null)
				return;

			for (ThriftTransportPool.CachedConnections cachedConn : getCache().values()) {
				for (ThriftTransportPool.CachedConnection cc : Iterables.concat(cachedConn.reserved.values(), cachedConn.unreserved)) {
					try {
						cc.transport.close();
					} catch (Exception e) {
						ThriftTransportPool.log.debug("Error closing transport during shutdown", e);
					}
				}
			}
			this.cache = null;
		}
		try {
			closerExitLatch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<ThriftTransportKey, ThriftTransportPool.CachedConnections> getCache() {
		if ((cache) == null)
			throw new ThriftTransportPool.TransportPoolShutdownException();

		return cache;
	}
}

