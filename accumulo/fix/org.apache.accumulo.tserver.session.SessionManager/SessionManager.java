

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.thrift.MultiScanResult;
import org.apache.accumulo.core.tabletserver.thrift.ActiveScan;
import org.apache.accumulo.core.tabletserver.thrift.ScanState;
import org.apache.accumulo.core.util.MapCounter;
import org.apache.accumulo.server.util.time.SimpleTimer;
import org.apache.accumulo.tserver.scan.ScanRunState;
import org.apache.accumulo.tserver.scan.ScanTask;
import org.apache.accumulo.tserver.session.MultiScanSession;
import org.apache.accumulo.tserver.session.ScanSession;
import org.apache.accumulo.tserver.session.Session;
import org.apache.accumulo.tserver.tablet.ScanBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SessionManager {
	private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

	private final SecureRandom random;

	private final ConcurrentMap<Long, Session> sessions = new ConcurrentHashMap<>();

	private final long maxIdle;

	private final long maxUpdateIdle;

	private final List<Session> idleSessions = new ArrayList<>();

	private final Long expiredSessionMarker = Long.valueOf((-1));

	private final AccumuloConfiguration aconf;

	public SessionManager(AccumuloConfiguration conf) {
		aconf = conf;
		maxUpdateIdle = conf.getTimeInMillis(Property.TSERV_UPDATE_SESSION_MAXIDLE);
		maxIdle = conf.getTimeInMillis(Property.TSERV_SESSION_MAXIDLE);
		SecureRandom sr;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			SessionManager.log.debug("Unable to create SHA1PRNG secure random, using default");
			sr = new SecureRandom();
		}
		random = sr;
		Runnable r = new Runnable() {
			@Override
			public void run() {
				sweep(maxIdle, maxUpdateIdle);
			}
		};
		SimpleTimer.getInstance(conf).schedule(r, 0, Math.max(((maxIdle) / 2), 1000));
	}

	public long createSession(Session session, boolean reserve) {
		long sid = random.nextLong();
		synchronized(session) {
		}
		while ((sessions.putIfAbsent(sid, session)) != null) {
			sid = random.nextLong();
		} 
		return sid;
	}

	public long getMaxIdleTime() {
		return maxIdle;
	}

	public Session reserveSession(long sessionId) {
		Session session = sessions.get(sessionId);
		if (session != null) {
			synchronized(session) {
			}
		}
		return session;
	}

	public Session reserveSession(long sessionId, boolean wait) {
		Session session = sessions.get(sessionId);
		if (session != null) {
			synchronized(session) {
			}
		}
		return session;
	}

	public void unreserveSession(Session session) {
		synchronized(session) {
			session.notifyAll();
		}
	}

	public void unreserveSession(long sessionId) {
		Session session = getSession(sessionId);
		if (session != null) {
			unreserveSession(session);
		}
	}

	public Session getSession(long sessionId) {
		Session session = sessions.get(sessionId);
		if (session != null) {
			synchronized(session) {
			}
		}
		return session;
	}

	public Session removeSession(long sessionId) {
		return removeSession(sessionId, false);
	}

	public Session removeSession(long sessionId, boolean unreserve) {
		Session session = sessions.remove(sessionId);
		if (session != null) {
			boolean doCleanup = false;
			synchronized(session) {
			}
			if (doCleanup) {
				session.cleanup();
			}
		}
		return session;
	}

	private void sweep(final long maxIdle, final long maxUpdateIdle) {
		List<Session> sessionsToCleanup = new ArrayList<>();
		Iterator<Session> iter = sessions.values().iterator();
		while (iter.hasNext()) {
			Session session = iter.next();
			synchronized(session) {
			}
		} 
		synchronized(idleSessions) {
			sessionsToCleanup.addAll(idleSessions);
			idleSessions.clear();
		}
		for (Session session : sessionsToCleanup) {
			if (!(session.cleanup()))
				synchronized(idleSessions) {
					idleSessions.add(session);
				}

		}
	}

	public void removeIfNotAccessed(final long sessionId, final long delay) {
		Session session = sessions.get(sessionId);
		if (session != null) {
			long tmp;
			synchronized(session) {
			}
			tmp = 0l;
			final long removeTime = tmp;
			TimerTask r = new TimerTask() {
				@Override
				public void run() {
					Session session2 = sessions.get(sessionId);
					if (session2 != null) {
						boolean shouldRemove = false;
						synchronized(session2) {
						}
						if (shouldRemove) {
							SessionManager.log.info((((((("Closing not accessed session from user=" + (session2.getUser())) + ", client=") + (session2.client)) + ", duration=") + delay) + "ms"));
							sessions.remove(sessionId);
							session2.cleanup();
						}
					}
				}
			};
			SimpleTimer.getInstance(aconf).schedule(r, delay);
		}
	}

	public Map<String, MapCounter<ScanRunState>> getActiveScansPerTable() {
		Map<String, MapCounter<ScanRunState>> counts = new HashMap<>();
		Set<Map.Entry<Long, Session>> copiedIdleSessions = new HashSet<>();
		synchronized(idleSessions) {
			for (Session session : idleSessions) {
				copiedIdleSessions.add(Maps.immutableEntry(expiredSessionMarker, session));
			}
		}
		for (Map.Entry<Long, Session> entry : Iterables.concat(sessions.entrySet(), copiedIdleSessions)) {
			Session session = entry.getValue();
			@SuppressWarnings("rawtypes")
			ScanTask nbt = null;
			String tableID = null;
			if (session instanceof ScanSession) {
				ScanSession ss = ((ScanSession) (session));
				nbt = ss.nextBatchTask;
				tableID = ss.extent.getTableId();
			}else
				if (session instanceof MultiScanSession) {
					MultiScanSession mss = ((MultiScanSession) (session));
					nbt = mss.lookupTask;
					tableID = mss.threadPoolExtent.getTableId();
				}

			if (nbt == null)
				continue;

			ScanRunState srs = nbt.getScanRunState();
			if (srs == (ScanRunState.FINISHED))
				continue;

			MapCounter<ScanRunState> stateCounts = counts.get(tableID);
			if (stateCounts == null) {
				stateCounts = new MapCounter<>();
				counts.put(tableID, stateCounts);
			}
			stateCounts.increment(srs, 1);
		}
		return counts;
	}

	public List<ActiveScan> getActiveScans() {
		final List<ActiveScan> activeScans = new ArrayList<>();
		final long ct = System.currentTimeMillis();
		final Set<Map.Entry<Long, Session>> copiedIdleSessions = new HashSet<>();
		synchronized(idleSessions) {
			for (Session session : idleSessions) {
				copiedIdleSessions.add(Maps.immutableEntry(expiredSessionMarker, session));
			}
		}
		for (Map.Entry<Long, Session> entry : Iterables.concat(sessions.entrySet(), copiedIdleSessions)) {
			Session session = entry.getValue();
			if (session instanceof ScanSession) {
				ScanSession ss = ((ScanSession) (session));
				ScanState state = ScanState.RUNNING;
				ScanTask<ScanBatch> nbt = ss.nextBatchTask;
				if (nbt == null) {
					state = ScanState.IDLE;
				}else {
					switch (nbt.getScanRunState()) {
						case QUEUED :
							state = ScanState.QUEUED;
							break;
						case FINISHED :
							state = ScanState.IDLE;
							break;
						case RUNNING :
						default :
							break;
					}
				}
			}else
				if (session instanceof MultiScanSession) {
					MultiScanSession mss = ((MultiScanSession) (session));
					ScanState state = ScanState.RUNNING;
					ScanTask<MultiScanResult> nbt = mss.lookupTask;
					if (nbt == null) {
						state = ScanState.IDLE;
					}else {
						switch (nbt.getScanRunState()) {
							case QUEUED :
								state = ScanState.QUEUED;
								break;
							case FINISHED :
								state = ScanState.IDLE;
								break;
							case RUNNING :
							default :
								break;
						}
					}
				}

		}
		return activeScans;
	}
}

