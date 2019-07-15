

import com.codahale.metrics.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import org.apache.cassandra.concurrent.NamedThreadFactory;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.utils.Clock;
import org.apache.cassandra.utils.NoSpamLogger;
import org.apache.cassandra.utils.concurrent.WaitQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cassandra.utils.NoSpamLogger.Level.WARN;


public abstract class AbstractCommitLogService {
	static final long DEFAULT_MARKER_INTERVAL_MILLIS = 100;

	private Thread thread;

	private volatile boolean shutdown = false;

	protected volatile long lastSyncedAt = System.currentTimeMillis();

	private final AtomicLong written = new AtomicLong(0);

	protected final AtomicLong pending = new AtomicLong(0);

	protected final WaitQueue syncComplete = new WaitQueue();

	final CommitLog commitLog;

	private final String name;

	final long syncIntervalNanos;

	final long markerIntervalNanos;

	private volatile boolean syncRequested;

	private static final Logger logger = LoggerFactory.getLogger(AbstractCommitLogService.class);

	AbstractCommitLogService(final CommitLog commitLog, final String name, long syncIntervalMillis) {
		this(commitLog, name, syncIntervalMillis, false);
	}

	AbstractCommitLogService(final CommitLog commitLog, final String name, long syncIntervalMillis, boolean markHeadersFaster) {
		this.commitLog = commitLog;
		this.name = name;
		final long markerIntervalMillis;
		if (markHeadersFaster && (syncIntervalMillis > (AbstractCommitLogService.DEFAULT_MARKER_INTERVAL_MILLIS))) {
			markerIntervalMillis = AbstractCommitLogService.DEFAULT_MARKER_INTERVAL_MILLIS;
			long modulo = syncIntervalMillis % markerIntervalMillis;
			if (modulo != 0) {
				syncIntervalMillis -= modulo;
				if (modulo >= (markerIntervalMillis / 2))
					syncIntervalMillis += markerIntervalMillis;

			}
			AbstractCommitLogService.logger.debug("Will update the commitlog markers every {}ms and flush every {}ms", markerIntervalMillis, syncIntervalMillis);
		}else {
			markerIntervalMillis = syncIntervalMillis;
		}
		assert (syncIntervalMillis % markerIntervalMillis) == 0;
		this.markerIntervalNanos = TimeUnit.NANOSECONDS.convert(markerIntervalMillis, TimeUnit.MILLISECONDS);
		this.syncIntervalNanos = TimeUnit.NANOSECONDS.convert(syncIntervalMillis, TimeUnit.MILLISECONDS);
	}

	void start() {
		if ((syncIntervalNanos) < 1)
			throw new IllegalArgumentException(String.format("Commit log flush interval must be positive: %fms", ((syncIntervalNanos) * 1.0E-6)));

		shutdown = false;
		Runnable runnable = new AbstractCommitLogService.SyncRunnable(new Clock());
		thread = NamedThreadFactory.createThread(runnable, name);
		thread.start();
	}

	class SyncRunnable implements Runnable {
		private final Clock clock;

		private long firstLagAt = 0;

		private long totalSyncDuration = 0;

		private long syncExceededIntervalBy = 0;

		private int lagCount = 0;

		private int syncCount = 0;

		SyncRunnable(Clock clock) {
			this.clock = clock;
		}

		public void run() {
			while (true) {
				if (!(sync()))
					break;

			} 
		}

		boolean sync() {
			boolean shutdownRequested = shutdown;
			try {
				long pollStarted = clock.nanoTime();
				if (((((lastSyncedAt) + (syncIntervalNanos)) <= pollStarted) || shutdownRequested) || (syncRequested)) {
					commitLog.sync(true);
					syncRequested = false;
					lastSyncedAt = pollStarted;
					syncComplete.signalAll();
					(syncCount)++;
				}else {
					commitLog.sync(false);
				}
				long now = clock.nanoTime();
				long wakeUpAt = pollStarted + (markerIntervalNanos);
				if (wakeUpAt < now) {
					if ((firstLagAt) == 0) {
						firstLagAt = now;
						totalSyncDuration = syncExceededIntervalBy = syncCount = lagCount = 0;
					}
					syncExceededIntervalBy += now - wakeUpAt;
					(lagCount)++;
				}
				totalSyncDuration += now - pollStarted;
				if ((firstLagAt) > 0) {
					boolean logged = NoSpamLogger.log(AbstractCommitLogService.logger, WARN, 5, TimeUnit.MINUTES, "Out of {} commit log syncs over the past {}s with average duration of {}ms, {} have exceeded the configured commit interval by an average of {}ms", syncCount, String.format("%.2f", ((now - (firstLagAt)) * 1.0E-9)), String.format("%.2f", (((totalSyncDuration) * 1.0E-6) / (syncCount))), lagCount, String.format("%.2f", (((syncExceededIntervalBy) * 1.0E-6) / (lagCount))));
					if (logged)
						firstLagAt = 0;

				}
				if (shutdownRequested)
					return false;

				if (wakeUpAt > now)
					LockSupport.parkNanos((wakeUpAt - now));

			} catch (Throwable t) {
				if (!(CommitLog.handleCommitError("Failed to persist commits to disk", t)))
					return false;

				LockSupport.parkNanos(markerIntervalNanos);
			}
			return true;
		}
	}

	void requestExtraSync() {
		syncRequested = true;
		LockSupport.unpark(thread);
	}

	public void shutdown() {
		shutdown = true;
		requestExtraSync();
	}

	public void syncBlocking() {
		long requestTime = System.nanoTime();
		requestExtraSync();
		awaitSyncAt(requestTime, null);
	}

	void awaitSyncAt(long syncTime, Timer.Context context) {
		do {
			WaitQueue.Signal signal = (context != null) ? syncComplete.register(context) : syncComplete.register();
			if ((lastSyncedAt) < syncTime)
				signal.awaitUninterruptibly();
			else
				signal.cancel();

		} while ((lastSyncedAt) < syncTime );
	}

	public void awaitTermination() throws InterruptedException {
		thread.join();
	}

	public long getCompletedTasks() {
		return written.get();
	}

	public long getPendingTasks() {
		return pending.get();
	}
}

