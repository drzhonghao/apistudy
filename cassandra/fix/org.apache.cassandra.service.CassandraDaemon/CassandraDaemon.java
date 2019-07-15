

import com.addthis.metrics3.reporter.config.ReporterConfig;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerMBean;
import org.apache.cassandra.batchlog.LegacyBatchlogMigrator;
import org.apache.cassandra.cache.AutoSavingCache;
import org.apache.cassandra.cache.IRowCacheEntry;
import org.apache.cassandra.cache.KeyCacheKey;
import org.apache.cassandra.cache.RowCacheKey;
import org.apache.cassandra.concurrent.DebuggableScheduledThreadPoolExecutor;
import org.apache.cassandra.concurrent.ScheduledExecutors;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.cql3.QueryProcessor;
import org.apache.cassandra.cql3.functions.ThreadAwareSecurityManager;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.SizeEstimatesRecorder;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.WindowsFailedSnapshotTracker;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.compaction.CompactionStrategyManager;
import org.apache.cassandra.db.view.ViewManager;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.StartupException;
import org.apache.cassandra.gms.Gossiper;
import org.apache.cassandra.hints.LegacyHintsMigrator;
import org.apache.cassandra.io.FSError;
import org.apache.cassandra.io.sstable.CorruptSSTableException;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.metrics.CassandraMetricsRegistry;
import org.apache.cassandra.metrics.DefaultNameFactory;
import org.apache.cassandra.metrics.StorageMetrics;
import org.apache.cassandra.schema.LegacySchemaMigrator;
import org.apache.cassandra.service.CacheService;
import org.apache.cassandra.service.DefaultFSErrorHandler;
import org.apache.cassandra.service.GCInspector;
import org.apache.cassandra.service.NativeAccessMBean;
import org.apache.cassandra.service.NativeTransportService;
import org.apache.cassandra.service.StartupChecks;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.JMXServerUtils;
import org.apache.cassandra.utils.JVMStabilityInspector;
import org.apache.cassandra.utils.Mx4jTool;
import org.apache.cassandra.utils.NativeLibrary;
import org.apache.cassandra.utils.WindowsTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CassandraDaemon {
	public static final String MBEAN_NAME = "org.apache.cassandra.db:type=NativeAccess";

	private static final Logger logger;

	static {
		SharedMetricRegistries.getOrCreate("logback-metrics").addListener(new MetricRegistryListener.Base() {
			@Override
			public void onMeterAdded(String metricName, Meter meter) {
				int separator = metricName.lastIndexOf('.');
				String appenderName = metricName.substring(0, separator);
				String metric = metricName.substring((separator + 1));
				ObjectName name = DefaultNameFactory.createMetricName(appenderName, metric, null).getMBeanName();
				CassandraMetricsRegistry.Metrics.registerMBean(meter, name);
			}
		});
		logger = LoggerFactory.getLogger(CassandraDaemon.class);
	}

	private void maybeInitJmx() {
		if ((System.getProperty("com.sun.management.jmxremote.port")) != null) {
			CassandraDaemon.logger.warn(("JMX settings in cassandra-env.sh have been bypassed as the JMX connector server is " + "already initialized. Please refer to cassandra-env.(sh|ps1) for JMX configuration info"));
			return;
		}
		System.setProperty("java.rmi.server.randomIDs", "true");
		boolean localOnly = false;
		String jmxPort = System.getProperty("cassandra.jmx.remote.port");
		if (jmxPort == null) {
			localOnly = true;
			jmxPort = System.getProperty("cassandra.jmx.local.port");
		}
		if (jmxPort == null)
			return;

		try {
			jmxServer = JMXServerUtils.createJMXServer(Integer.parseInt(jmxPort), localOnly);
			if ((jmxServer) == null)
				return;

		} catch (IOException e) {
			exitOrFail(1, e.getMessage(), e.getCause());
		}
	}

	static final CassandraDaemon instance = new CassandraDaemon();

	public CassandraDaemon.Server thriftServer;

	private NativeTransportService nativeTransportService;

	private JMXConnectorServer jmxServer;

	private final boolean runManaged;

	protected final StartupChecks startupChecks;

	private boolean setupCompleted;

	public CassandraDaemon() {
		this(false);
	}

	public CassandraDaemon(boolean runManaged) {
		this.runManaged = runManaged;
		this.startupChecks = new StartupChecks().withDefaultTests();
		this.setupCompleted = false;
	}

	protected void setup() {
		FileUtils.setFSErrorHandler(new DefaultFSErrorHandler());
		if (FBUtilities.isWindows)
			WindowsFailedSnapshotTracker.deleteOldSnapshots();

		maybeInitJmx();
		Mx4jTool.maybeLoad();
		ThreadAwareSecurityManager.install();
		logSystemInfo();
		NativeLibrary.tryMlockall();
		try {
			startupChecks.verify();
		} catch (StartupException e) {
			exitOrFail(e.returnCode, e.getMessage(), e.getCause());
		}
		try {
			if (SystemKeyspace.snapshotOnVersionChange()) {
				SystemKeyspace.migrateDataDirs();
			}
		} catch (IOException e) {
			exitOrFail(3, e.getMessage(), e.getCause());
		}
		SystemKeyspace.persistLocalMetadata();
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				StorageMetrics.exceptions.inc();
				CassandraDaemon.logger.error(("Exception in thread " + t), e);
				Tracing.trace("Exception in thread {}", t, e);
				for (Throwable e2 = e; e2 != null; e2 = e2.getCause()) {
					JVMStabilityInspector.inspectThrowable(e2);
					if (e2 instanceof FSError) {
						if (e2 != e)
							CassandraDaemon.logger.error(("Exception in thread " + t), e2);

						FileUtils.handleFSError(((FSError) (e2)));
					}
					if (e2 instanceof CorruptSSTableException) {
						if (e2 != e)
							CassandraDaemon.logger.error(("Exception in thread " + t), e2);

						FileUtils.handleCorruptSSTable(((CorruptSSTableException) (e2)));
					}
				}
			}
		});
		LegacySchemaMigrator.migrate();
		StorageService.instance.populateTokenMetadata();
		Schema.instance.loadFromDisk();
		for (String keyspaceName : Schema.instance.getKeyspaces()) {
			if (keyspaceName.equals(SchemaConstants.SYSTEM_KEYSPACE_NAME))
				continue;

			for (CFMetaData cfm : Schema.instance.getTablesAndViews(keyspaceName)) {
				try {
					ColumnFamilyStore.scrubDataDirectories(cfm);
				} catch (StartupException e) {
					exitOrFail(e.returnCode, e.getMessage(), e.getCause());
				}
			}
		}
		Keyspace.setInitialized();
		for (String keyspaceName : Schema.instance.getKeyspaces()) {
			if (CassandraDaemon.logger.isDebugEnabled())
				CassandraDaemon.logger.debug("opening keyspace {}", keyspaceName);

			for (ColumnFamilyStore cfs : Keyspace.open(keyspaceName).getColumnFamilyStores()) {
				for (ColumnFamilyStore store : cfs.concatWithIndexes()) {
					store.disableAutoCompaction();
				}
			}
		}
		try {
			loadRowAndKeyCacheAsync().get();
		} catch (Throwable t) {
			JVMStabilityInspector.inspectThrowable(t);
			CassandraDaemon.logger.warn("Error loading key or row cache", t);
		}
		try {
			GCInspector.register();
		} catch (Throwable t) {
			JVMStabilityInspector.inspectThrowable(t);
			CassandraDaemon.logger.warn("Unable to start GCInspector (currently only supported on the Sun JVM)");
		}
		try {
			CommitLog.instance.recoverSegmentsOnDisk();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		StorageService.instance.populateTokenMetadata();
		new LegacyHintsMigrator(DatabaseDescriptor.getHintsDirectory(), DatabaseDescriptor.getMaxHintsFileSize()).migrate();
		LegacyBatchlogMigrator.migrate();
		SystemKeyspace.finishStartup();
		QueryProcessor.preloadPreparedStatement();
		String metricsReporterConfigFile = System.getProperty("cassandra.metricsReporterConfigFile");
		if (metricsReporterConfigFile != null) {
			CassandraDaemon.logger.info("Trying to load metrics-reporter-config from file: {}", metricsReporterConfigFile);
			try {
				CassandraMetricsRegistry.Metrics.register("jvm.buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
				CassandraMetricsRegistry.Metrics.register("jvm.gc", new GarbageCollectorMetricSet());
				CassandraMetricsRegistry.Metrics.register("jvm.memory", new MemoryUsageGaugeSet());
				CassandraMetricsRegistry.Metrics.register("jvm.fd.usage", new FileDescriptorRatioGauge());
				URL resource = CassandraDaemon.class.getClassLoader().getResource(metricsReporterConfigFile);
				if (resource == null) {
					CassandraDaemon.logger.warn("Failed to load metrics-reporter-config, file does not exist: {}", metricsReporterConfigFile);
				}else {
					String reportFileLocation = resource.getFile();
					ReporterConfig.loadFromFile(reportFileLocation).enableAll(CassandraMetricsRegistry.Metrics);
				}
			} catch (Exception e) {
				CassandraDaemon.logger.warn("Failed to load metrics-reporter-config, metric sinks will not be activated", e);
			}
		}
		try {
			StorageService.instance.initServer();
		} catch (ConfigurationException e) {
			System.err.println(((e.getMessage()) + "\nFatal configuration error; unable to start server.  See log for stacktrace."));
			exitOrFail(1, "Fatal configuration error", e);
		}
		Runnable viewRebuild = () -> {
			for (Keyspace keyspace : Keyspace.all()) {
				keyspace.viewManager.buildAllViews();
			}
			CassandraDaemon.logger.debug("Completed submission of build tasks for any materialized views defined at startup");
		};
		ScheduledExecutors.optionalTasks.schedule(viewRebuild, StorageService.RING_DELAY, TimeUnit.MILLISECONDS);
		if (!(FBUtilities.getBroadcastAddress().equals(InetAddress.getLoopbackAddress())))
			Gossiper.waitToSettle();

		for (Keyspace keyspace : Keyspace.all()) {
			for (ColumnFamilyStore cfs : keyspace.getColumnFamilyStores()) {
				for (final ColumnFamilyStore store : cfs.concatWithIndexes()) {
					store.reload();
					if (store.getCompactionStrategyManager().shouldBeEnabled()) {
						store.enableAutoCompaction();
					}
				}
			}
		}
		ScheduledExecutors.optionalTasks.scheduleWithFixedDelay(ColumnFamilyStore.getBackgroundCompactionTaskSubmitter(), 5, 1, TimeUnit.MINUTES);
		int sizeRecorderInterval = Integer.getInteger("cassandra.size_recorder_interval", (5 * 60));
		if (sizeRecorderInterval > 0)
			ScheduledExecutors.optionalTasks.scheduleWithFixedDelay(SizeEstimatesRecorder.instance, 30, sizeRecorderInterval, TimeUnit.SECONDS);

		InetAddress rpcAddr = DatabaseDescriptor.getRpcAddress();
		int rpcPort = DatabaseDescriptor.getRpcPort();
		int listenBacklog = DatabaseDescriptor.getRpcListenBacklog();
		nativeTransportService = new NativeTransportService();
		completeSetup();
	}

	private ListenableFuture<?> loadRowAndKeyCacheAsync() {
		final ListenableFuture<Integer> keyCacheLoad = CacheService.instance.keyCache.loadSavedAsync();
		final ListenableFuture<Integer> rowCacheLoad = CacheService.instance.rowCache.loadSavedAsync();
		@SuppressWarnings("unchecked")
		ListenableFuture<List<Integer>> retval = Futures.successfulAsList(keyCacheLoad, rowCacheLoad);
		return retval;
	}

	@com.google.common.annotations.VisibleForTesting
	public void completeSetup() {
		setupCompleted = true;
	}

	public boolean setupCompleted() {
		return setupCompleted;
	}

	private void logSystemInfo() {
		if (CassandraDaemon.logger.isInfoEnabled()) {
			try {
				CassandraDaemon.logger.info("Hostname: {}", InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e1) {
				CassandraDaemon.logger.info("Could not resolve local host");
			}
			CassandraDaemon.logger.info("JVM vendor/version: {}/{}", System.getProperty("java.vm.name"), System.getProperty("java.version"));
			CassandraDaemon.logger.info("Heap size: {}/{}", FBUtilities.prettyPrintMemory(Runtime.getRuntime().totalMemory()), FBUtilities.prettyPrintMemory(Runtime.getRuntime().maxMemory()));
			for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans())
				CassandraDaemon.logger.info("{} {}: {}", pool.getName(), pool.getType(), pool.getPeakUsage());

			CassandraDaemon.logger.info("Classpath: {}", System.getProperty("java.class.path"));
			CassandraDaemon.logger.info("JVM Arguments: {}", ManagementFactory.getRuntimeMXBean().getInputArguments());
		}
	}

	public void init(String[] arguments) throws IOException {
		setup();
	}

	public void start() {
		String nativeFlag = System.getProperty("cassandra.start_native_transport");
		if (((nativeFlag != null) && (Boolean.parseBoolean(nativeFlag))) || ((nativeFlag == null) && (DatabaseDescriptor.startNativeTransport()))) {
			startNativeTransport();
			StorageService.instance.setRpcReady(true);
		}else
			CassandraDaemon.logger.info("Not starting native transport as requested. Use JMX (StorageService->startNativeTransport()) or nodetool (enablebinary) to start it");

		String rpcFlag = System.getProperty("cassandra.start_rpc");
		if (((rpcFlag != null) && (Boolean.parseBoolean(rpcFlag))) || ((rpcFlag == null) && (DatabaseDescriptor.startRpc())))
			thriftServer.start();
		else
			CassandraDaemon.logger.info("Not starting RPC server as requested. Use JMX (StorageService->startRPCServer()) or nodetool (enablethrift) to start it");

	}

	public void stop() {
		CassandraDaemon.logger.info("Cassandra shutting down...");
		if ((thriftServer) != null)
			thriftServer.stop();

		if ((nativeTransportService) != null)
			nativeTransportService.destroy();

		StorageService.instance.setRpcReady(false);
		if (FBUtilities.isWindows)
			System.exit(0);

		if ((jmxServer) != null) {
			try {
				jmxServer.stop();
			} catch (IOException e) {
				CassandraDaemon.logger.error("Error shutting down local JMX server: ", e);
			}
		}
	}

	public void destroy() {
	}

	public void activate() {
		try {
			applyConfig();
			try {
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				mbs.registerMBean(new StandardMBean(new CassandraDaemon.NativeAccess(), NativeAccessMBean.class), new ObjectName(CassandraDaemon.MBEAN_NAME));
			} catch (Exception e) {
				CassandraDaemon.logger.error("error registering MBean {}", CassandraDaemon.MBEAN_NAME, e);
			}
			if (FBUtilities.isWindows) {
				WindowsTimer.startTimerPeriod(DatabaseDescriptor.getWindowsTimerInterval());
			}
			setup();
			String pidFile = System.getProperty("cassandra-pidfile");
			if (pidFile != null) {
				new File(pidFile).deleteOnExit();
			}
			if ((System.getProperty("cassandra-foreground")) == null) {
				System.out.close();
				System.err.close();
			}
			start();
		} catch (Throwable e) {
			boolean logStackTrace = (e instanceof ConfigurationException) ? ((ConfigurationException) (e)).logStackTrace : true;
			System.out.println(((("Exception (" + (e.getClass().getName())) + ") encountered during startup: ") + (e.getMessage())));
			if (logStackTrace) {
				if (runManaged)
					CassandraDaemon.logger.error("Exception encountered during startup", e);

				e.printStackTrace();
				exitOrFail(3, "Exception encountered during startup", e);
			}else {
				if (runManaged)
					CassandraDaemon.logger.error("Exception encountered during startup: {}", e.getMessage());

				System.err.println(e.getMessage());
				exitOrFail(3, ("Exception encountered during startup: " + (e.getMessage())));
			}
		}
	}

	public void applyConfig() {
		DatabaseDescriptor.daemonInitialization();
	}

	public void startNativeTransport() {
		if ((nativeTransportService) == null)
			throw new IllegalStateException("setup() must be called first for CassandraDaemon");
		else
			nativeTransportService.start();

	}

	public void stopNativeTransport() {
		if ((nativeTransportService) != null)
			nativeTransportService.stop();

	}

	public boolean isNativeTransportRunning() {
		return (nativeTransportService) != null ? nativeTransportService.isRunning() : false;
	}

	public void deactivate() {
		stop();
		destroy();
		if (!(runManaged)) {
			System.exit(0);
		}
	}

	public static void stop(String[] args) {
		CassandraDaemon.instance.deactivate();
	}

	public static void main(String[] args) {
		CassandraDaemon.instance.activate();
	}

	private void exitOrFail(int code, String message) {
		exitOrFail(code, message, null);
	}

	private void exitOrFail(int code, String message, Throwable cause) {
		if (runManaged) {
			RuntimeException t = (cause != null) ? new RuntimeException(message, cause) : new RuntimeException(message);
			throw t;
		}else {
			CassandraDaemon.logger.error(message, cause);
			System.exit(code);
		}
	}

	static class NativeAccess implements NativeAccessMBean {
		public boolean isAvailable() {
			return NativeLibrary.isAvailable();
		}

		public boolean isMemoryLockable() {
			return NativeLibrary.jnaMemoryLockable();
		}
	}

	public interface Server {
		public void start();

		public void stop();

		public boolean isRunning();
	}
}

