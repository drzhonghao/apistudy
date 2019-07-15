

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.MergeRateLimiter;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.index.MergeTrigger;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RateLimitedIndexOutput;
import org.apache.lucene.store.RateLimiter;
import org.apache.lucene.util.CollectionUtil;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.ThreadInterruptedException;

import static org.apache.lucene.store.IOContext.Context.MERGE;


public class ConcurrentMergeScheduler extends MergeScheduler {
	public static final int AUTO_DETECT_MERGES_AND_THREADS = -1;

	public static final String DEFAULT_CPU_CORE_COUNT_PROPERTY = "lucene.cms.override_core_count";

	public static final String DEFAULT_SPINS_PROPERTY = "lucene.cms.override_spins";

	protected final List<ConcurrentMergeScheduler.MergeThread> mergeThreads = new ArrayList<>();

	private int maxThreadCount = ConcurrentMergeScheduler.AUTO_DETECT_MERGES_AND_THREADS;

	private int maxMergeCount = ConcurrentMergeScheduler.AUTO_DETECT_MERGES_AND_THREADS;

	protected int mergeThreadCount;

	private static final double MIN_MERGE_MB_PER_SEC = 5.0;

	private static final double MAX_MERGE_MB_PER_SEC = 10240.0;

	private static final double START_MB_PER_SEC = 20.0;

	private static final double MIN_BIG_MERGE_MB = 50.0;

	protected double targetMBPerSec = ConcurrentMergeScheduler.START_MB_PER_SEC;

	private boolean doAutoIOThrottle = true;

	private double forceMergeMBPerSec = Double.POSITIVE_INFINITY;

	public ConcurrentMergeScheduler() {
	}

	public synchronized void setMaxMergesAndThreads(int maxMergeCount, int maxThreadCount) {
		if ((maxMergeCount == (ConcurrentMergeScheduler.AUTO_DETECT_MERGES_AND_THREADS)) && (maxThreadCount == (ConcurrentMergeScheduler.AUTO_DETECT_MERGES_AND_THREADS))) {
			this.maxMergeCount = ConcurrentMergeScheduler.AUTO_DETECT_MERGES_AND_THREADS;
			this.maxThreadCount = ConcurrentMergeScheduler.AUTO_DETECT_MERGES_AND_THREADS;
		}else
			if (maxMergeCount == (ConcurrentMergeScheduler.AUTO_DETECT_MERGES_AND_THREADS)) {
				throw new IllegalArgumentException("both maxMergeCount and maxThreadCount must be AUTO_DETECT_MERGES_AND_THREADS");
			}else
				if (maxThreadCount == (ConcurrentMergeScheduler.AUTO_DETECT_MERGES_AND_THREADS)) {
					throw new IllegalArgumentException("both maxMergeCount and maxThreadCount must be AUTO_DETECT_MERGES_AND_THREADS");
				}else {
					if (maxThreadCount < 1) {
						throw new IllegalArgumentException("maxThreadCount should be at least 1");
					}
					if (maxMergeCount < 1) {
						throw new IllegalArgumentException("maxMergeCount should be at least 1");
					}
					if (maxThreadCount > maxMergeCount) {
						throw new IllegalArgumentException((("maxThreadCount should be <= maxMergeCount (= " + maxMergeCount) + ")"));
					}
					this.maxThreadCount = maxThreadCount;
					this.maxMergeCount = maxMergeCount;
				}


	}

	public synchronized void setDefaultMaxMergesAndThreads(boolean spins) {
		if (spins) {
			maxThreadCount = 1;
			maxMergeCount = 6;
		}else {
			int coreCount = Runtime.getRuntime().availableProcessors();
			try {
				String value = System.getProperty(ConcurrentMergeScheduler.DEFAULT_CPU_CORE_COUNT_PROPERTY);
				if (value != null) {
					coreCount = Integer.parseInt(value);
				}
			} catch (Throwable ignored) {
			}
			maxThreadCount = Math.max(1, Math.min(4, (coreCount / 2)));
			maxMergeCount = (maxThreadCount) + 5;
		}
	}

	public synchronized void setForceMergeMBPerSec(double v) {
		forceMergeMBPerSec = v;
		updateMergeThreads();
	}

	public synchronized double getForceMergeMBPerSec() {
		return forceMergeMBPerSec;
	}

	public synchronized void enableAutoIOThrottle() {
		doAutoIOThrottle = true;
		targetMBPerSec = ConcurrentMergeScheduler.START_MB_PER_SEC;
		updateMergeThreads();
	}

	public synchronized void disableAutoIOThrottle() {
		doAutoIOThrottle = false;
		updateMergeThreads();
	}

	public synchronized boolean getAutoIOThrottle() {
		return doAutoIOThrottle;
	}

	public synchronized double getIORateLimitMBPerSec() {
		if (doAutoIOThrottle) {
			return targetMBPerSec;
		}else {
			return Double.POSITIVE_INFINITY;
		}
	}

	public synchronized int getMaxThreadCount() {
		return maxThreadCount;
	}

	public synchronized int getMaxMergeCount() {
		return maxMergeCount;
	}

	synchronized void removeMergeThread() {
		Thread currentThread = Thread.currentThread();
		for (int i = 0; i < (mergeThreads.size()); i++) {
			if ((mergeThreads.get(i)) == currentThread) {
				mergeThreads.remove(i);
				return;
			}
		}
		assert false : ("merge thread " + currentThread) + " was not found";
	}

	@Override
	public Directory wrapForMerge(MergePolicy.OneMerge merge, Directory in) {
		Thread mergeThread = Thread.currentThread();
		if (!(ConcurrentMergeScheduler.MergeThread.class.isInstance(mergeThread))) {
			throw new AssertionError(("wrapForMerge should be called from MergeThread. Current thread: " + mergeThread));
		}
		RateLimiter rateLimiter = ((ConcurrentMergeScheduler.MergeThread) (mergeThread)).rateLimiter;
		return new FilterDirectory(in) {
			@Override
			public IndexOutput createOutput(String name, IOContext context) throws IOException {
				ensureOpen();
				assert (context.context) == (MERGE) : "got context=" + (context.context);
				assert mergeThread == (Thread.currentThread()) : (("Not the same merge thread, current=" + (Thread.currentThread())) + ", expected=") + mergeThread;
				return new RateLimitedIndexOutput(rateLimiter, in.createOutput(name, context));
			}
		};
	}

	protected synchronized void updateMergeThreads() {
		final List<ConcurrentMergeScheduler.MergeThread> activeMerges = new ArrayList<>();
		int threadIdx = 0;
		while (threadIdx < (mergeThreads.size())) {
			final ConcurrentMergeScheduler.MergeThread mergeThread = mergeThreads.get(threadIdx);
			if (!(mergeThread.isAlive())) {
				mergeThreads.remove(threadIdx);
				continue;
			}
			activeMerges.add(mergeThread);
			threadIdx++;
		} 
		CollectionUtil.timSort(activeMerges);
		final int activeMergeCount = activeMerges.size();
		int bigMergeCount = 0;
		for (threadIdx = activeMergeCount - 1; threadIdx >= 0; threadIdx--) {
			ConcurrentMergeScheduler.MergeThread mergeThread = activeMerges.get(threadIdx);
			if ((mergeThread.merge.estimatedMergeBytes) > (((ConcurrentMergeScheduler.MIN_BIG_MERGE_MB) * 1024) * 1024)) {
				bigMergeCount = 1 + threadIdx;
				break;
			}
		}
		long now = System.nanoTime();
		StringBuilder message;
		if (verbose()) {
			message = new StringBuilder();
			message.append(String.format(Locale.ROOT, "updateMergeThreads ioThrottle=%s targetMBPerSec=%.1f MB/sec", doAutoIOThrottle, targetMBPerSec));
		}else {
			message = null;
		}
		for (threadIdx = 0; threadIdx < activeMergeCount; threadIdx++) {
			ConcurrentMergeScheduler.MergeThread mergeThread = activeMerges.get(threadIdx);
			MergePolicy.OneMerge merge = mergeThread.merge;
			final boolean doPause = threadIdx < (bigMergeCount - (maxThreadCount));
			double newMBPerSec;
			if (doPause) {
				newMBPerSec = 0.0;
			}else {
			}
			MergeRateLimiter rateLimiter = mergeThread.rateLimiter;
			double curMBPerSec = rateLimiter.getMBPerSec();
			if (verbose()) {
				message.append('\n');
				newMBPerSec = 0d;
				if (newMBPerSec != curMBPerSec) {
					if (newMBPerSec == 0.0) {
						message.append("  now stop");
					}else
						if (curMBPerSec == 0.0) {
							if (newMBPerSec == (Double.POSITIVE_INFINITY)) {
								message.append("  now resume");
							}else {
								message.append(String.format(Locale.ROOT, "  now resume to %.1f MB/sec", newMBPerSec));
							}
						}else {
							message.append(String.format(Locale.ROOT, "  now change from %.1f MB/sec to %.1f MB/sec", curMBPerSec, newMBPerSec));
						}

				}else
					if (curMBPerSec == 0.0) {
						message.append("  leave stopped");
					}else {
						message.append(String.format(Locale.ROOT, "  leave running at %.1f MB/sec", curMBPerSec));
					}

			}
			newMBPerSec = 0.0;
			rateLimiter.setMBPerSec(newMBPerSec);
		}
		if (verbose()) {
			message(message.toString());
		}
	}

	private synchronized void initDynamicDefaults(IndexWriter writer) throws IOException {
		if ((maxThreadCount) == (ConcurrentMergeScheduler.AUTO_DETECT_MERGES_AND_THREADS)) {
			boolean spins = IOUtils.spins(writer.getDirectory());
			try {
				String value = System.getProperty(ConcurrentMergeScheduler.DEFAULT_SPINS_PROPERTY);
				if (value != null) {
					spins = Boolean.parseBoolean(value);
				}
			} catch (Exception ignored) {
			}
			setDefaultMaxMergesAndThreads(spins);
			if (verbose()) {
				message(((((("initDynamicDefaults spins=" + spins) + " maxThreadCount=") + (maxThreadCount)) + " maxMergeCount=") + (maxMergeCount)));
			}
		}
	}

	private static String rateToString(double mbPerSec) {
		if (mbPerSec == 0.0) {
			return "stopped";
		}else
			if (mbPerSec == (Double.POSITIVE_INFINITY)) {
				return "unlimited";
			}else {
				return String.format(Locale.ROOT, "%.1f MB/sec", mbPerSec);
			}

	}

	@Override
	public void close() {
		sync();
	}

	public void sync() {
		boolean interrupted = false;
		try {
			while (true) {
				ConcurrentMergeScheduler.MergeThread toSync = null;
				synchronized(this) {
					for (ConcurrentMergeScheduler.MergeThread t : mergeThreads) {
						if ((t.isAlive()) && (t != (Thread.currentThread()))) {
							toSync = t;
							break;
						}
					}
				}
				if (toSync != null) {
					try {
						toSync.join();
					} catch (InterruptedException ie) {
						interrupted = true;
					}
				}else {
					break;
				}
			} 
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();

		}
	}

	public synchronized int mergeThreadCount() {
		Thread currentThread = Thread.currentThread();
		int count = 0;
		for (ConcurrentMergeScheduler.MergeThread mergeThread : mergeThreads) {
			if (((currentThread != mergeThread) && (mergeThread.isAlive())) && ((mergeThread.merge.isAborted()) == false)) {
				count++;
			}
		}
		return count;
	}

	@Override
	public synchronized void merge(IndexWriter writer, MergeTrigger trigger, boolean newMergesFound) throws IOException {
		assert !(Thread.holdsLock(writer));
		initDynamicDefaults(writer);
		if (trigger == (MergeTrigger.CLOSING)) {
			targetMBPerSec = ConcurrentMergeScheduler.MAX_MERGE_MB_PER_SEC;
			updateMergeThreads();
		}
		if (verbose()) {
			message("now merge");
		}
		while (true) {
			if ((maybeStall(writer)) == false) {
				break;
			}
			MergePolicy.OneMerge merge = writer.getNextMerge();
			if (merge == null) {
				if (verbose()) {
					message("  no more merges pending; now return");
				}
				return;
			}
			boolean success = false;
			try {
				if (verbose()) {
				}
				final ConcurrentMergeScheduler.MergeThread newMergeThread = getMergeThread(writer, merge);
				mergeThreads.add(newMergeThread);
				updateIOThrottle(newMergeThread.merge, newMergeThread.rateLimiter);
				if (verbose()) {
					message((("    launch new thread [" + (newMergeThread.getName())) + "]"));
				}
				newMergeThread.start();
				updateMergeThreads();
				success = true;
			} finally {
				if (!success) {
				}
			}
		} 
	}

	protected synchronized boolean maybeStall(IndexWriter writer) {
		long startStallTime = 0;
		while ((writer.hasPendingMerges()) && ((mergeThreadCount()) >= (maxMergeCount))) {
			if (mergeThreads.contains(Thread.currentThread())) {
				return false;
			}
			if ((verbose()) && (startStallTime == 0)) {
				message("    too many merges; stalling...");
			}
			startStallTime = System.currentTimeMillis();
			doStall();
		} 
		if ((verbose()) && (startStallTime != 0)) {
			message((("  stalled for " + ((System.currentTimeMillis()) - startStallTime)) + " msec"));
		}
		return true;
	}

	protected synchronized void doStall() {
		try {
			wait(250);
		} catch (InterruptedException ie) {
			throw new ThreadInterruptedException(ie);
		}
	}

	protected void doMerge(IndexWriter writer, MergePolicy.OneMerge merge) throws IOException {
		writer.merge(merge);
	}

	protected synchronized ConcurrentMergeScheduler.MergeThread getMergeThread(IndexWriter writer, MergePolicy.OneMerge merge) throws IOException {
		final ConcurrentMergeScheduler.MergeThread thread = new ConcurrentMergeScheduler.MergeThread(writer, merge);
		thread.setDaemon(true);
		thread.setName(("Lucene Merge Thread #" + ((mergeThreadCount)++)));
		return thread;
	}

	protected class MergeThread extends Thread implements Comparable<ConcurrentMergeScheduler.MergeThread> {
		final IndexWriter writer;

		final MergePolicy.OneMerge merge;

		final MergeRateLimiter rateLimiter;

		public MergeThread(IndexWriter writer, MergePolicy.OneMerge merge) {
			this.writer = writer;
			this.merge = merge;
			this.rateLimiter = new MergeRateLimiter(merge.getMergeProgress());
		}

		@Override
		public int compareTo(ConcurrentMergeScheduler.MergeThread other) {
			return Long.compare(other.merge.estimatedMergeBytes, merge.estimatedMergeBytes);
		}

		@Override
		public void run() {
			try {
				if (verbose()) {
					message("  merge thread: start");
				}
				doMerge(writer, merge);
				if (verbose()) {
					message("  merge thread: done");
				}
				try {
					merge(writer, MergeTrigger.MERGE_FINISHED, true);
				} catch (AlreadyClosedException ace) {
				} catch (IOException ioe) {
					throw new RuntimeException(ioe);
				}
			} catch (Throwable exc) {
				if (exc instanceof MergePolicy.MergeAbortedException) {
				}else
					if ((suppressExceptions) == false) {
						handleMergeException(writer.getDirectory(), exc);
					}

			} finally {
				synchronized(ConcurrentMergeScheduler.this) {
					removeMergeThread();
					updateMergeThreads();
					ConcurrentMergeScheduler.this.notifyAll();
				}
			}
		}
	}

	protected void handleMergeException(Directory dir, Throwable exc) {
		throw new MergePolicy.MergeException(exc, dir);
	}

	private boolean suppressExceptions;

	void setSuppressExceptions() {
		if (verbose()) {
			message("will suppress merge exceptions");
		}
		suppressExceptions = true;
	}

	void clearSuppressExceptions() {
		if (verbose()) {
			message("will not suppress merge exceptions");
		}
		suppressExceptions = false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(((getClass().getSimpleName()) + ": "));
		sb.append("maxThreadCount=").append(maxThreadCount).append(", ");
		sb.append("maxMergeCount=").append(maxMergeCount).append(", ");
		sb.append("ioThrottle=").append(doAutoIOThrottle);
		return sb.toString();
	}

	private boolean isBacklog(long now, MergePolicy.OneMerge merge) {
		double mergeMB = ConcurrentMergeScheduler.bytesToMB(merge.estimatedMergeBytes);
		for (ConcurrentMergeScheduler.MergeThread mergeThread : mergeThreads) {
		}
		return false;
	}

	private synchronized void updateIOThrottle(MergePolicy.OneMerge newMerge, MergeRateLimiter rateLimiter) throws IOException {
		if ((doAutoIOThrottle) == false) {
			return;
		}
		double mergeMB = ConcurrentMergeScheduler.bytesToMB(newMerge.estimatedMergeBytes);
		if (mergeMB < (ConcurrentMergeScheduler.MIN_BIG_MERGE_MB)) {
			return;
		}
		long now = System.nanoTime();
		boolean newBacklog = isBacklog(now, newMerge);
		boolean curBacklog = false;
		if (newBacklog == false) {
			if ((mergeThreads.size()) > (maxThreadCount)) {
				curBacklog = true;
			}else {
				for (ConcurrentMergeScheduler.MergeThread mergeThread : mergeThreads) {
					if (isBacklog(now, mergeThread.merge)) {
						curBacklog = true;
						break;
					}
				}
			}
		}
		double curMBPerSec = targetMBPerSec;
		if (newBacklog) {
			targetMBPerSec *= 1.2;
			if ((targetMBPerSec) > (ConcurrentMergeScheduler.MAX_MERGE_MB_PER_SEC)) {
				targetMBPerSec = ConcurrentMergeScheduler.MAX_MERGE_MB_PER_SEC;
			}
			if (verbose()) {
				if (curMBPerSec == (targetMBPerSec)) {
					message(String.format(Locale.ROOT, "io throttle: new merge backlog; leave IO rate at ceiling %.1f MB/sec", targetMBPerSec));
				}else {
					message(String.format(Locale.ROOT, "io throttle: new merge backlog; increase IO rate to %.1f MB/sec", targetMBPerSec));
				}
			}
		}else
			if (curBacklog) {
				if (verbose()) {
					message(String.format(Locale.ROOT, "io throttle: current merge backlog; leave IO rate at %.1f MB/sec", targetMBPerSec));
				}
			}else {
				targetMBPerSec /= 1.1;
				if ((targetMBPerSec) < (ConcurrentMergeScheduler.MIN_MERGE_MB_PER_SEC)) {
					targetMBPerSec = ConcurrentMergeScheduler.MIN_MERGE_MB_PER_SEC;
				}
				if (verbose()) {
					if (curMBPerSec == (targetMBPerSec)) {
						message(String.format(Locale.ROOT, "io throttle: no merge backlog; leave IO rate at floor %.1f MB/sec", targetMBPerSec));
					}else {
						message(String.format(Locale.ROOT, "io throttle: no merge backlog; decrease IO rate to %.1f MB/sec", targetMBPerSec));
					}
				}
			}

		double rate;
		rate = 0.0;
		rateLimiter.setMBPerSec(rate);
		targetMBPerSecChanged();
	}

	protected void targetMBPerSecChanged() {
	}

	private static double nsToSec(long ns) {
		return ns / 1.0E9;
	}

	private static double bytesToMB(long bytes) {
		return (bytes / 1024.0) / 1024.0;
	}
}

