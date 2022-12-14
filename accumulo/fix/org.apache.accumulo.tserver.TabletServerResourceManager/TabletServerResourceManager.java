

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.file.blockfile.cache.LruBlockCache;
import org.apache.accumulo.core.metadata.schema.DataFileValue;
import org.apache.accumulo.core.util.Daemon;
import org.apache.accumulo.core.util.NamingThreadFactory;
import org.apache.accumulo.fate.util.LoggingRunnable;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.server.fs.FileRef;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.tabletserver.LargestFirstMemoryManager;
import org.apache.accumulo.server.tabletserver.MemoryManagementActions;
import org.apache.accumulo.server.tabletserver.MemoryManager;
import org.apache.accumulo.server.tabletserver.TabletState;
import org.apache.accumulo.server.util.time.SimpleTimer;
import org.apache.accumulo.tserver.ActiveAssignmentRunnable;
import org.apache.accumulo.tserver.FileManager;
import org.apache.accumulo.tserver.HoldTimeoutException;
import org.apache.accumulo.tserver.MinorCompactionReason;
import org.apache.accumulo.tserver.NativeMap;
import org.apache.accumulo.tserver.RunnableStartedAt;
import org.apache.accumulo.tserver.TabletServer;
import org.apache.accumulo.tserver.compaction.CompactionStrategy;
import org.apache.accumulo.tserver.compaction.DefaultCompactionStrategy;
import org.apache.accumulo.tserver.compaction.MajorCompactionReason;
import org.apache.accumulo.tserver.compaction.MajorCompactionRequest;
import org.apache.accumulo.tserver.tablet.Tablet;
import org.apache.htrace.wrappers.TraceExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TabletServerResourceManager {
	private static final Logger log = LoggerFactory.getLogger(TabletServerResourceManager.class);

	private final ExecutorService minorCompactionThreadPool;

	private final ExecutorService majorCompactionThreadPool;

	private final ExecutorService rootMajorCompactionThreadPool;

	private final ExecutorService defaultMajorCompactionThreadPool;

	private final ExecutorService splitThreadPool;

	private final ExecutorService defaultSplitThreadPool;

	private final ExecutorService defaultMigrationPool;

	private final ExecutorService migrationPool;

	private final ExecutorService assignmentPool;

	private final ExecutorService assignMetaDataPool;

	private final ExecutorService readAheadThreadPool;

	private final ExecutorService defaultReadAheadThreadPool;

	private final Map<String, ExecutorService> threadPools = new TreeMap<>();

	private final ConcurrentHashMap<KeyExtent, RunnableStartedAt> activeAssignments;

	private final VolumeManager fs;

	private final FileManager fileManager;

	private final MemoryManager memoryManager;

	private final TabletServerResourceManager.MemoryManagementFramework memMgmt;

	private final LruBlockCache _dCache;

	private final LruBlockCache _iCache;

	private final TabletServer tserver;

	private final ServerConfigurationFactory conf;

	private ExecutorService addEs(String name, ExecutorService tp) {
		if (threadPools.containsKey(name)) {
			throw new IllegalArgumentException(("Cannot create two executor services with same name " + name));
		}
		tp = new TraceExecutorService(tp);
		threadPools.put(name, tp);
		return tp;
	}

	private ExecutorService addEs(final Property maxThreads, String name, final ThreadPoolExecutor tp) {
		ExecutorService result = addEs(name, tp);
		SimpleTimer.getInstance(tserver.getConfiguration()).schedule(new Runnable() {
			@Override
			public void run() {
				try {
					int max = tserver.getConfiguration().getCount(maxThreads);
					if ((tp.getMaximumPoolSize()) != max) {
						TabletServerResourceManager.log.info(((("Changing " + (maxThreads.getKey())) + " to ") + max));
						tp.setCorePoolSize(max);
						tp.setMaximumPoolSize(max);
					}
				} catch (Throwable t) {
					TabletServerResourceManager.log.error("Failed to change thread pool size", t);
				}
			}
		}, 1000, (10 * 1000));
		return result;
	}

	private ExecutorService createEs(int max, String name) {
		return addEs(name, Executors.newFixedThreadPool(max, new NamingThreadFactory(name)));
	}

	private ExecutorService createEs(Property max, String name) {
		return createEs(max, name, new LinkedBlockingQueue<Runnable>());
	}

	private ExecutorService createEs(Property max, String name, BlockingQueue<Runnable> queue) {
		int maxThreads = conf.getConfiguration().getCount(max);
		ThreadPoolExecutor tp = new ThreadPoolExecutor(maxThreads, maxThreads, 0L, TimeUnit.MILLISECONDS, queue, new NamingThreadFactory(name));
		return addEs(max, name, tp);
	}

	private ExecutorService createEs(int min, int max, int timeout, String name) {
		return addEs(name, new ThreadPoolExecutor(min, max, timeout, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamingThreadFactory(name)));
	}

	public TabletServerResourceManager(TabletServer tserver, VolumeManager fs) {
		this.tserver = tserver;
		this.conf = tserver.getServerConfigurationFactory();
		this.fs = fs;
		final AccumuloConfiguration acuConf = conf.getConfiguration();
		long maxMemory = acuConf.getMemoryInBytes(Property.TSERV_MAXMEM);
		boolean usingNativeMap = (acuConf.getBoolean(Property.TSERV_NATIVEMAP_ENABLED)) && (NativeMap.isLoaded());
		long blockSize = acuConf.getMemoryInBytes(Property.TSERV_DEFAULT_BLOCKSIZE);
		long dCacheSize = acuConf.getMemoryInBytes(Property.TSERV_DATACACHE_SIZE);
		long iCacheSize = acuConf.getMemoryInBytes(Property.TSERV_INDEXCACHE_SIZE);
		long totalQueueSize = acuConf.getMemoryInBytes(Property.TSERV_TOTAL_MUTATION_QUEUE_MAX);
		_iCache = new LruBlockCache(iCacheSize, blockSize);
		_dCache = new LruBlockCache(dCacheSize, blockSize);
		Runtime runtime = Runtime.getRuntime();
		if (usingNativeMap) {
			if (((dCacheSize + iCacheSize) + totalQueueSize) > (runtime.maxMemory())) {
				throw new IllegalArgumentException(String.format(("Block cache sizes %,d" + " and mutation queue size %,d is too large for this JVM configuration %,d"), (dCacheSize + iCacheSize), totalQueueSize, runtime.maxMemory()));
			}
		}else
			if ((((maxMemory + dCacheSize) + iCacheSize) + totalQueueSize) > (runtime.maxMemory())) {
				throw new IllegalArgumentException(String.format(("Maximum tablet server" + (" map memory %,d block cache sizes %,d and mutation queue size %,d is" + " too large for this JVM configuration %,d")), maxMemory, (dCacheSize + iCacheSize), totalQueueSize, runtime.maxMemory()));
			}

		runtime.gc();
		if ((!usingNativeMap) && (maxMemory > ((runtime.maxMemory()) - ((runtime.totalMemory()) - (runtime.freeMemory()))))) {
			TabletServerResourceManager.log.warn("In-memory map may not fit into local memory space.");
		}
		minorCompactionThreadPool = createEs(Property.TSERV_MINC_MAXCONCURRENT, "minor compactor");
		rootMajorCompactionThreadPool = createEs(0, 1, 300, "md root major compactor");
		defaultMajorCompactionThreadPool = createEs(0, 1, 300, "md major compactor");
		splitThreadPool = createEs(1, "splitter");
		defaultSplitThreadPool = createEs(0, 1, 60, "md splitter");
		defaultMigrationPool = createEs(0, 1, 60, "metadata tablet migration");
		migrationPool = createEs(Property.TSERV_MIGRATE_MAXCONCURRENT, "tablet migration");
		assignmentPool = createEs(Property.TSERV_ASSIGNMENT_MAXCONCURRENT, "tablet assignment");
		assignMetaDataPool = createEs(0, 1, 60, "metadata tablet assignment");
		activeAssignments = new ConcurrentHashMap<>();
		readAheadThreadPool = createEs(Property.TSERV_READ_AHEAD_MAXCONCURRENT, "tablet read ahead");
		defaultReadAheadThreadPool = createEs(Property.TSERV_METADATA_READ_AHEAD_MAXCONCURRENT, "metadata tablets read ahead");
		int maxOpenFiles = acuConf.getCount(Property.TSERV_SCAN_MAX_OPENFILES);
		Cache<String, Long> fileLenCache = CacheBuilder.newBuilder().maximumSize(Math.min((maxOpenFiles * 1000L), 100000)).build();
		fileManager = new FileManager(tserver, fs, maxOpenFiles, fileLenCache, _dCache, _iCache);
		memoryManager = Property.createInstanceFromPropertyName(acuConf, Property.TSERV_MEM_MGMT, MemoryManager.class, new LargestFirstMemoryManager());
		memoryManager.init(tserver.getServerConfigurationFactory());
		memMgmt = new TabletServerResourceManager.MemoryManagementFramework();
		memMgmt.startThreads();
		SimpleTimer timer = SimpleTimer.getInstance(tserver.getConfiguration());
		timer.schedule(new TabletServerResourceManager.AssignmentWatcher(acuConf, activeAssignments, timer), 5000);
		majorCompactionThreadPool = null;
	}

	protected static class AssignmentWatcher implements Runnable {
		private static final Logger log = LoggerFactory.getLogger(TabletServerResourceManager.AssignmentWatcher.class);

		private final Map<KeyExtent, RunnableStartedAt> activeAssignments;

		private final AccumuloConfiguration conf;

		private final SimpleTimer timer;

		public AssignmentWatcher(AccumuloConfiguration conf, Map<KeyExtent, RunnableStartedAt> activeAssignments, SimpleTimer timer) {
			this.conf = conf;
			this.activeAssignments = activeAssignments;
			this.timer = timer;
		}

		@Override
		public void run() {
			final long millisBeforeWarning = conf.getTimeInMillis(Property.TSERV_ASSIGNMENT_DURATION_WARNING);
			try {
				long now = System.currentTimeMillis();
				KeyExtent extent;
				RunnableStartedAt runnable;
				for (Map.Entry<KeyExtent, RunnableStartedAt> entry : activeAssignments.entrySet()) {
					extent = entry.getKey();
					runnable = entry.getValue();
					final long duration = now - (runnable.getStartTime());
					if (duration > millisBeforeWarning) {
						TabletServerResourceManager.AssignmentWatcher.log.warn((((("Assignment for " + extent) + " has been running for at least ") + duration) + "ms"), runnable.getTask().getException());
					}else
						if (TabletServerResourceManager.AssignmentWatcher.log.isTraceEnabled()) {
							TabletServerResourceManager.AssignmentWatcher.log.trace((((("Assignment for " + extent) + " only running for ") + duration) + "ms"));
						}

				}
			} catch (Exception e) {
				TabletServerResourceManager.AssignmentWatcher.log.warn("Caught exception checking active assignments", e);
			} finally {
				long delay = Math.max(((long) (millisBeforeWarning * 0.5)), 5000L);
				if (TabletServerResourceManager.AssignmentWatcher.log.isTraceEnabled()) {
					TabletServerResourceManager.AssignmentWatcher.log.trace((("Rescheduling assignment watcher to run in " + delay) + "ms"));
				}
				timer.schedule(this, delay);
			}
		}
	}

	private static class TabletStateImpl implements Cloneable , TabletState {
		private final long lct;

		private final Tablet tablet;

		private final long mts;

		private final long mcmts;

		public TabletStateImpl(Tablet t, long mts, long lct, long mcmts) {
			this.tablet = t;
			this.mts = mts;
			this.lct = lct;
			this.mcmts = mcmts;
		}

		@Override
		public KeyExtent getExtent() {
			return tablet.getExtent();
		}

		Tablet getTablet() {
			return tablet;
		}

		@Override
		public long getLastCommitTime() {
			return lct;
		}

		@Override
		public long getMemTableSize() {
			return mts;
		}

		@Override
		public long getMinorCompactingMemTableSize() {
			return mcmts;
		}

		@Override
		public TabletServerResourceManager.TabletStateImpl clone() throws CloneNotSupportedException {
			return ((TabletServerResourceManager.TabletStateImpl) (super.clone()));
		}
	}

	private class MemoryManagementFramework {
		private final Map<KeyExtent, TabletServerResourceManager.TabletStateImpl> tabletReports;

		private final LinkedBlockingQueue<TabletServerResourceManager.TabletStateImpl> memUsageReports;

		private long lastMemCheckTime = System.currentTimeMillis();

		private long maxMem;

		private long lastMemTotal = 0;

		private final Thread memoryGuardThread;

		private final Thread minorCompactionInitiatorThread;

		MemoryManagementFramework() {
			tabletReports = Collections.synchronizedMap(new HashMap<KeyExtent, TabletServerResourceManager.TabletStateImpl>());
			memUsageReports = new LinkedBlockingQueue<>();
			maxMem = conf.getConfiguration().getMemoryInBytes(Property.TSERV_MAXMEM);
			Runnable r1 = new Runnable() {
				@Override
				public void run() {
					processTabletMemStats();
				}
			};
			memoryGuardThread = new Daemon(new LoggingRunnable(TabletServerResourceManager.log, r1));
			memoryGuardThread.setPriority(((Thread.NORM_PRIORITY) + 1));
			memoryGuardThread.setName("Accumulo Memory Guard");
			Runnable r2 = new Runnable() {
				@Override
				public void run() {
					manageMemory();
				}
			};
			minorCompactionInitiatorThread = new Daemon(new LoggingRunnable(TabletServerResourceManager.log, r2));
			minorCompactionInitiatorThread.setName("Accumulo Minor Compaction Initiator");
		}

		void startThreads() {
			memoryGuardThread.start();
			minorCompactionInitiatorThread.start();
		}

		private void processTabletMemStats() {
			while (true) {
				try {
					TabletServerResourceManager.TabletStateImpl report = memUsageReports.take();
					while (report != null) {
						tabletReports.put(report.getExtent(), report);
						report = memUsageReports.poll();
					} 
					long delta = (System.currentTimeMillis()) - (lastMemCheckTime);
					if (((holdCommits) || (delta > 50)) || ((lastMemTotal) > (0.9 * (maxMem)))) {
						lastMemCheckTime = System.currentTimeMillis();
						long totalMemUsed = 0;
						synchronized(tabletReports) {
							for (TabletServerResourceManager.TabletStateImpl tsi : tabletReports.values()) {
								totalMemUsed += tsi.getMemTableSize();
								totalMemUsed += tsi.getMinorCompactingMemTableSize();
							}
						}
						if (totalMemUsed > (0.95 * (maxMem))) {
							holdAllCommits(true);
						}else {
							holdAllCommits(false);
						}
						lastMemTotal = totalMemUsed;
					}
				} catch (InterruptedException e) {
					TabletServerResourceManager.log.warn("Interrupted processing tablet memory statistics", e);
				}
			} 
		}

		private void manageMemory() {
			while (true) {
				MemoryManagementActions mma = null;
				Map<KeyExtent, TabletServerResourceManager.TabletStateImpl> tabletReportsCopy = null;
				try {
					synchronized(tabletReports) {
						tabletReportsCopy = new HashMap<>(tabletReports);
					}
					ArrayList<TabletState> tabletStates = new ArrayList<TabletState>(tabletReportsCopy.values());
					mma = memoryManager.getMemoryManagementActions(tabletStates);
				} catch (Throwable t) {
					TabletServerResourceManager.log.error("Memory manager failed {}", t.getMessage(), t);
				}
				try {
					if (((mma != null) && ((mma.tabletsToMinorCompact) != null)) && ((mma.tabletsToMinorCompact.size()) > 0)) {
						for (KeyExtent keyExtent : mma.tabletsToMinorCompact) {
							TabletServerResourceManager.TabletStateImpl tabletReport = tabletReportsCopy.get(keyExtent);
							if (tabletReport == null) {
								TabletServerResourceManager.log.warn((("Memory manager asked to compact nonexistent tablet " + keyExtent) + "; manager implementation might be misbehaving"));
								continue;
							}
							Tablet tablet = tabletReport.getTablet();
							if (!(tablet.initiateMinorCompaction(MinorCompactionReason.SYSTEM))) {
								if (tablet.isClosed()) {
									synchronized(tabletReports) {
										TabletServerResourceManager.TabletStateImpl latestReport = tabletReports.remove(keyExtent);
										if (latestReport != null) {
											if ((latestReport.getTablet()) != tablet) {
												tabletReports.put(keyExtent, latestReport);
											}else {
												TabletServerResourceManager.log.debug(("Cleaned up report for closed tablet " + keyExtent));
											}
										}
									}
									TabletServerResourceManager.log.debug(("Ignoring memory manager recommendation: not minor compacting closed tablet " + keyExtent));
								}else {
									TabletServerResourceManager.log.info(("Ignoring memory manager recommendation: not minor compacting " + keyExtent));
								}
							}
						}
					}
				} catch (Throwable t) {
					TabletServerResourceManager.log.error("Minor compactions for memory managment failed", t);
				}
				UtilWaitThread.sleepUninterruptibly(250, TimeUnit.MILLISECONDS);
			} 
		}

		public void updateMemoryUsageStats(Tablet tablet, long size, long lastCommitTime, long mincSize) {
			memUsageReports.add(new TabletServerResourceManager.TabletStateImpl(tablet, size, lastCommitTime, mincSize));
		}

		public void tabletClosed(KeyExtent extent) {
			tabletReports.remove(extent);
		}
	}

	private final Object commitHold = new Object();

	private volatile boolean holdCommits = false;

	private long holdStartTime;

	protected void holdAllCommits(boolean holdAllCommits) {
		synchronized(commitHold) {
			if ((holdCommits) != holdAllCommits) {
				holdCommits = holdAllCommits;
				if (holdCommits) {
					holdStartTime = System.currentTimeMillis();
				}
				if (!(holdCommits)) {
					TabletServerResourceManager.log.debug(String.format("Commits held for %6.2f secs", (((System.currentTimeMillis()) - (holdStartTime)) / 1000.0)));
					commitHold.notifyAll();
				}
			}
		}
	}

	void waitUntilCommitsAreEnabled() {
		if (holdCommits) {
			long timeout = (System.currentTimeMillis()) + (conf.getConfiguration().getTimeInMillis(Property.GENERAL_RPC_TIMEOUT));
			synchronized(commitHold) {
				while (holdCommits) {
					try {
						if ((System.currentTimeMillis()) > timeout)
							throw new HoldTimeoutException("Commits are held");

						commitHold.wait(1000);
					} catch (InterruptedException e) {
					}
				} 
			}
		}
	}

	public long holdTime() {
		if (!(holdCommits))
			return 0;

		synchronized(commitHold) {
			return (System.currentTimeMillis()) - (holdStartTime);
		}
	}

	public void close() {
		for (ExecutorService executorService : threadPools.values()) {
			executorService.shutdown();
		}
		for (Map.Entry<String, ExecutorService> entry : threadPools.entrySet()) {
			while (true) {
				try {
					if (entry.getValue().awaitTermination(60, TimeUnit.SECONDS))
						break;

					TabletServerResourceManager.log.info((("Waiting for thread pool " + (entry.getKey())) + " to shutdown"));
				} catch (InterruptedException e) {
					TabletServerResourceManager.log.warn("Interrupted waiting for executor to terminate", e);
				}
			} 
		}
	}

	public synchronized TabletServerResourceManager.TabletResourceManager createTabletResourceManager(KeyExtent extent, AccumuloConfiguration conf) {
		TabletServerResourceManager.TabletResourceManager trm = new TabletServerResourceManager.TabletResourceManager(extent, conf);
		return trm;
	}

	public class TabletResourceManager {
		private final long creationTime = System.currentTimeMillis();

		private volatile boolean openFilesReserved = false;

		private volatile boolean closed = false;

		private final KeyExtent extent;

		private final AccumuloConfiguration tableConf;

		TabletResourceManager(KeyExtent extent, AccumuloConfiguration tableConf) {
			Objects.requireNonNull(extent, "extent is null");
			Objects.requireNonNull(tableConf, "tableConf is null");
			this.extent = extent;
			this.tableConf = tableConf;
		}

		@com.google.common.annotations.VisibleForTesting
		KeyExtent getExtent() {
			return extent;
		}

		@com.google.common.annotations.VisibleForTesting
		AccumuloConfiguration getTableConfiguration() {
			return tableConf;
		}

		public void importedMapFiles() {
			lastReportedCommitTime = System.currentTimeMillis();
		}

		public synchronized FileManager.ScanFileManager newScanFileManager() {
			if (closed)
				throw new IllegalStateException("closed");

			return fileManager.newScanFileManager(extent);
		}

		private final AtomicLong lastReportedSize = new AtomicLong();

		private final AtomicLong lastReportedMincSize = new AtomicLong();

		private volatile long lastReportedCommitTime = 0;

		public void updateMemoryUsageStats(Tablet tablet, long size, long mincSize) {
			long totalSize = size + mincSize;
			long lrs = lastReportedSize.get();
			long delta = totalSize - lrs;
			long lrms = lastReportedMincSize.get();
			boolean report = false;
			if ((((lrms > 0) && (mincSize == 0)) || ((lrms == 0) && (mincSize > 0))) && (lastReportedMincSize.compareAndSet(lrms, mincSize))) {
				report = true;
			}
			long currentTime = System.currentTimeMillis();
			if ((((delta > 32000) || (delta < 0)) || ((currentTime - (lastReportedCommitTime)) > 1000)) && (lastReportedSize.compareAndSet(lrs, totalSize))) {
				if (delta > 0)
					lastReportedCommitTime = currentTime;

				report = true;
			}
			if (report)
				memMgmt.updateMemoryUsageStats(tablet, size, lastReportedCommitTime, mincSize);

		}

		public boolean needsMajorCompaction(SortedMap<FileRef, DataFileValue> tabletFiles, MajorCompactionReason reason) {
			if (closed)
				return false;

			if (reason == (MajorCompactionReason.USER))
				return true;

			if (reason == (MajorCompactionReason.IDLE)) {
				long idleTime;
				if ((lastReportedCommitTime) == 0) {
					idleTime = (System.currentTimeMillis()) - (creationTime);
				}else {
					idleTime = (System.currentTimeMillis()) - (lastReportedCommitTime);
				}
				if (idleTime < (tableConf.getTimeInMillis(Property.TABLE_MAJC_COMPACTALL_IDLETIME))) {
					return false;
				}
			}
			CompactionStrategy strategy = Property.createTableInstanceFromPropertyName(tableConf, Property.TABLE_COMPACTION_STRATEGY, CompactionStrategy.class, new DefaultCompactionStrategy());
			strategy.init(Property.getCompactionStrategyOptions(tableConf));
			MajorCompactionRequest request = new MajorCompactionRequest(extent, reason, TabletServerResourceManager.this.fs, tableConf);
			request.setFiles(tabletFiles);
			try {
				return strategy.shouldCompact(request);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void executeMinorCompaction(final Runnable r) {
			minorCompactionThreadPool.execute(new LoggingRunnable(TabletServerResourceManager.log, r));
		}

		public void close() throws IOException {
			synchronized(TabletServerResourceManager.this) {
				synchronized(this) {
					if (closed)
						throw new IOException("closed");

					if (openFilesReserved)
						throw new IOException("tired to close files while open files reserved");

					memMgmt.tabletClosed(extent);
					memoryManager.tabletClosed(extent);
					closed = true;
				}
			}
		}

		public TabletServerResourceManager getTabletServerResourceManager() {
			return TabletServerResourceManager.this;
		}

		public void executeMajorCompaction(KeyExtent tablet, Runnable compactionTask) {
			TabletServerResourceManager.this.executeMajorCompaction(tablet, compactionTask);
		}
	}

	public void executeSplit(KeyExtent tablet, Runnable splitTask) {
		if (tablet.isMeta()) {
			if (tablet.isRootTablet()) {
				TabletServerResourceManager.log.warn("Saw request to split root tablet, ignoring");
				return;
			}
			defaultSplitThreadPool.execute(splitTask);
		}else {
			splitThreadPool.execute(splitTask);
		}
	}

	public void executeMajorCompaction(KeyExtent tablet, Runnable compactionTask) {
		if (tablet.isRootTablet()) {
			rootMajorCompactionThreadPool.execute(compactionTask);
		}else
			if (tablet.isMeta()) {
				defaultMajorCompactionThreadPool.execute(compactionTask);
			}else {
				majorCompactionThreadPool.execute(compactionTask);
			}

	}

	public void executeReadAhead(KeyExtent tablet, Runnable task) {
		if (tablet.isRootTablet()) {
			task.run();
		}else
			if (tablet.isMeta()) {
				defaultReadAheadThreadPool.execute(task);
			}else {
				readAheadThreadPool.execute(task);
			}

	}

	public void addMigration(KeyExtent tablet, Runnable migrationHandler) {
		if (tablet.isRootTablet()) {
			migrationHandler.run();
		}else
			if (tablet.isMeta()) {
				defaultMigrationPool.execute(migrationHandler);
			}else {
				migrationPool.execute(migrationHandler);
			}

	}

	public LruBlockCache getIndexCache() {
		return _iCache;
	}

	public LruBlockCache getDataCache() {
		return _dCache;
	}
}

