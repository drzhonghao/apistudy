

import com.beust.jcommander.Parameter;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IsolatedScanner;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.PartialKey;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.gc.thrift.GCMonitorService;
import org.apache.accumulo.core.gc.thrift.GCStatus;
import org.apache.accumulo.core.gc.thrift.GcCycleStats;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.replication.ReplicationSchema;
import org.apache.accumulo.core.replication.ReplicationTable;
import org.apache.accumulo.core.replication.ReplicationTableOfflineException;
import org.apache.accumulo.core.rpc.SslConnectionParams;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.trace.DistributedTrace;
import org.apache.accumulo.core.trace.ProbabilitySampler;
import org.apache.accumulo.core.trace.Span;
import org.apache.accumulo.core.trace.Trace;
import org.apache.accumulo.core.trace.thrift.TInfo;
import org.apache.accumulo.core.util.ColumnFQ;
import org.apache.accumulo.core.util.HostAndPort;
import org.apache.accumulo.core.util.NamingThreadFactory;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.core.util.ServerServices;
import org.apache.accumulo.core.volume.Volume;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.gc.GarbageCollectionAlgorithm;
import org.apache.accumulo.gc.GarbageCollectionEnvironment;
import org.apache.accumulo.gc.replication.CloseWriteAheadLogReferences;
import org.apache.accumulo.server.Accumulo;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.ServerConstants;
import org.apache.accumulo.server.ServerOpts;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.fs.VolumeManagerImpl;
import org.apache.accumulo.server.fs.VolumeUtil;
import org.apache.accumulo.server.metrics.MetricsSystemHelper;
import org.apache.accumulo.server.replication.proto.Replication;
import org.apache.accumulo.server.rpc.RpcWrapper;
import org.apache.accumulo.server.rpc.SaslServerConnectionParams;
import org.apache.accumulo.server.rpc.ServerAddress;
import org.apache.accumulo.server.rpc.TCredentialsUpdatingWrapper;
import org.apache.accumulo.server.rpc.TServerUtils;
import org.apache.accumulo.server.rpc.ThriftServerType;
import org.apache.accumulo.server.security.SecurityUtil;
import org.apache.accumulo.server.tables.TableManager;
import org.apache.accumulo.server.util.Halt;
import org.apache.accumulo.server.util.TabletIterator;
import org.apache.accumulo.server.zookeeper.ZooLock;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.core.metadata.schema.MetadataSchema.DeletesSection.getRowPrefix;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.DataFileColumnFamily.NAME;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.getRange;
import static org.apache.accumulo.core.replication.ReplicationSchema.StatusSection.limit;
import static org.apache.accumulo.core.util.ServerServices.Service.GC_CLIENT;
import static org.apache.accumulo.server.fs.VolumeManager.FileType.TABLE;
import static org.apache.accumulo.server.replication.proto.Replication.Status.parseFrom;


public class SimpleGarbageCollector extends AccumuloServerContext implements GCMonitorService.Iface {
	private static final Text EMPTY_TEXT = new Text();

	static class Opts extends ServerOpts {
		@Parameter(names = { "-v", "--verbose" }, description = "extra information will get printed to stdout also")
		boolean verbose = false;

		@Parameter(names = { "-s", "--safemode" }, description = "safe mode will not delete files")
		boolean safeMode = false;
	}

	static final float CANDIDATE_MEMORY_PERCENTAGE = 0.5F;

	private static final Logger log = LoggerFactory.getLogger(SimpleGarbageCollector.class);

	private VolumeManager fs;

	private SimpleGarbageCollector.Opts opts = new SimpleGarbageCollector.Opts();

	private ZooLock lock;

	private GCStatus status = new GCStatus(new GcCycleStats(), new GcCycleStats(), new GcCycleStats(), new GcCycleStats());

	public static void main(String[] args) throws IOException, UnknownHostException {
		final String app = "gc";
		Accumulo.setupLogging(app);
		SecurityUtil.serverLogin(SiteConfiguration.getInstance());
		Instance instance = HdfsZooInstance.getInstance();
		ServerConfigurationFactory conf = new ServerConfigurationFactory(instance);
		SimpleGarbageCollector.log.info(("Version " + (Constants.VERSION)));
		SimpleGarbageCollector.log.info(("Instance " + (instance.getInstanceID())));
		final VolumeManager fs = VolumeManagerImpl.get();
		MetricsSystemHelper.configure(SimpleGarbageCollector.class.getSimpleName());
		Accumulo.init(fs, conf, app);
		SimpleGarbageCollector.Opts opts = new SimpleGarbageCollector.Opts();
		opts.parseArgs(app, args);
		SimpleGarbageCollector gc = new SimpleGarbageCollector(opts, fs, conf);
		DistributedTrace.enable(opts.getAddress(), app, conf.getConfiguration());
		try {
			gc.run();
		} finally {
			DistributedTrace.disable();
		}
	}

	public SimpleGarbageCollector(SimpleGarbageCollector.Opts opts, VolumeManager fs, ServerConfigurationFactory confFactory) {
		super(confFactory);
		this.opts = opts;
		this.fs = fs;
		long gcDelay = getConfiguration().getTimeInMillis(Property.GC_CYCLE_DELAY);
		SimpleGarbageCollector.log.info((("start delay: " + (getStartDelay())) + " milliseconds"));
		SimpleGarbageCollector.log.info((("time delay: " + gcDelay) + " milliseconds"));
		SimpleGarbageCollector.log.info(("safemode: " + (opts.safeMode)));
		SimpleGarbageCollector.log.info(("verbose: " + (opts.verbose)));
		SimpleGarbageCollector.log.info((((("memory threshold: " + (SimpleGarbageCollector.CANDIDATE_MEMORY_PERCENTAGE)) + " of ") + (Runtime.getRuntime().maxMemory())) + " bytes"));
		SimpleGarbageCollector.log.info(("delete threads: " + (getNumDeleteThreads())));
	}

	long getStartDelay() {
		return getConfiguration().getTimeInMillis(Property.GC_CYCLE_START);
	}

	VolumeManager getVolumeManager() {
		return fs;
	}

	boolean isUsingTrash() {
		return !(getConfiguration().getBoolean(Property.GC_TRASH_IGNORE));
	}

	SimpleGarbageCollector.Opts getOpts() {
		return opts;
	}

	int getNumDeleteThreads() {
		return getConfiguration().getCount(Property.GC_DELETE_THREADS);
	}

	boolean shouldArchiveFiles() {
		return getConfiguration().getBoolean(Property.GC_FILE_ARCHIVE);
	}

	private class GCEnv implements GarbageCollectionEnvironment {
		private String tableName;

		GCEnv(String tableName) {
			this.tableName = tableName;
		}

		@Override
		public boolean getCandidates(String continuePoint, List<String> result) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
			Range range = MetadataSchema.DeletesSection.getRange();
			if ((continuePoint != null) && (!(continuePoint.isEmpty()))) {
				String continueRow = (getRowPrefix()) + continuePoint;
				range = new Range(new Key(continueRow).followingKey(PartialKey.ROW), true, range.getEndKey(), range.isEndKeyInclusive());
			}
			Scanner scanner = getConnector().createScanner(tableName, Authorizations.EMPTY);
			scanner.setRange(range);
			result.clear();
			for (Map.Entry<Key, Value> entry : scanner) {
				String cand = entry.getKey().getRow().toString().substring(getRowPrefix().length());
				result.add(cand);
				if (SimpleGarbageCollector.almostOutOfMemory(Runtime.getRuntime())) {
					SimpleGarbageCollector.log.info(("List of delete candidates has exceeded the memory" + " threshold. Attempting to delete what has been gathered so far."));
					return true;
				}
			}
			return false;
		}

		@Override
		public Iterator<String> getBlipIterator() throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
			@SuppressWarnings("resource")
			IsolatedScanner scanner = new IsolatedScanner(getConnector().createScanner(tableName, Authorizations.EMPTY));
			scanner.setRange(MetadataSchema.BlipSection.getRange());
			return Iterators.transform(scanner.iterator(), new Function<Map.Entry<Key, Value>, String>() {
				@Override
				public String apply(Map.Entry<Key, Value> entry) {
					return entry.getKey().getRow().toString().substring(MetadataSchema.BlipSection.getRowPrefix().length());
				}
			});
		}

		@Override
		public Iterator<Map.Entry<Key, Value>> getReferenceIterator() throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
			IsolatedScanner scanner = new IsolatedScanner(getConnector().createScanner(tableName, Authorizations.EMPTY));
			scanner.fetchColumnFamily(NAME);
			scanner.fetchColumnFamily(MetadataSchema.TabletsSection.ScanFileColumnFamily.NAME);
			DIRECTORY_COLUMN.fetch(scanner);
			TabletIterator tabletIterator = new TabletIterator(scanner, getRange(), false, true);
			return Iterators.concat(Iterators.transform(tabletIterator, new Function<Map<Key, Value>, Iterator<Map.Entry<Key, Value>>>() {
				@Override
				public Iterator<Map.Entry<Key, Value>> apply(Map<Key, Value> input) {
					return input.entrySet().iterator();
				}
			}));
		}

		@Override
		public Set<String> getTableIDs() {
			return Tables.getIdToNameMap(getInstance()).keySet();
		}

		@Override
		public void delete(SortedMap<String, String> confirmedDeletes) throws IOException, AccumuloException, AccumuloSecurityException, TableNotFoundException {
			if (opts.safeMode) {
				if (opts.verbose)
					System.out.println(((("SAFEMODE: There are " + (confirmedDeletes.size())) + " data file candidates marked for deletion.%n") + "          Examine the log files to identify them.%n"));

				SimpleGarbageCollector.log.info("SAFEMODE: Listing all data file candidates for deletion");
				for (String s : confirmedDeletes.values())
					SimpleGarbageCollector.log.info(("SAFEMODE: " + s));

				SimpleGarbageCollector.log.info("SAFEMODE: End candidates for deletion");
				return;
			}
			Connector c = getConnector();
			BatchWriter writer = c.createBatchWriter(tableName, new BatchWriterConfig());
			Iterator<Map.Entry<String, String>> cdIter = confirmedDeletes.entrySet().iterator();
			String lastDir = null;
			while (cdIter.hasNext()) {
				Map.Entry<String, String> entry = cdIter.next();
				String relPath = entry.getKey();
				String absPath = fs.getFullPath(TABLE, entry.getValue()).toString();
				if (SimpleGarbageCollector.isDir(relPath)) {
					lastDir = absPath;
				}else
					if (lastDir != null) {
						if (absPath.startsWith(lastDir)) {
							SimpleGarbageCollector.log.debug((((("Ignoring " + (entry.getValue())) + " because ") + lastDir) + " exist"));
							try {
								SimpleGarbageCollector.putMarkerDeleteMutation(entry.getValue(), writer);
							} catch (MutationsRejectedException e) {
								throw new RuntimeException(e);
							}
							cdIter.remove();
						}else {
							lastDir = null;
						}
					}

			} 
			final BatchWriter finalWriter = writer;
			ExecutorService deleteThreadPool = Executors.newFixedThreadPool(getNumDeleteThreads(), new NamingThreadFactory("deleting"));
			final List<Pair<Path, Path>> replacements = ServerConstants.getVolumeReplacements();
			for (final String delete : confirmedDeletes.values()) {
				Runnable deleteTask = new Runnable() {
					@Override
					public void run() {
						boolean removeFlag;
						try {
							Path fullPath;
							String switchedDelete = VolumeUtil.switchVolume(delete, TABLE, replacements);
							if (switchedDelete != null) {
								SimpleGarbageCollector.log.debug(((("Volume replaced " + delete) + " -> ") + switchedDelete));
								fullPath = fs.getFullPath(TABLE, switchedDelete);
							}else {
								fullPath = fs.getFullPath(TABLE, delete);
							}
							SimpleGarbageCollector.log.debug(("Deleting " + fullPath));
							if ((archiveOrMoveToTrash(fullPath)) || (fs.deleteRecursively(fullPath))) {
								removeFlag = true;
								synchronized(SimpleGarbageCollector.this) {
									++(status.current.deleted);
								}
							}else
								if (fs.exists(fullPath)) {
									removeFlag = false;
									synchronized(SimpleGarbageCollector.this) {
										++(status.current.errors);
									}
									SimpleGarbageCollector.log.warn(("File exists, but was not deleted for an unknown reason: " + fullPath));
								}else {
									removeFlag = true;
									synchronized(SimpleGarbageCollector.this) {
										++(status.current.errors);
									}
									String[] parts = fullPath.toString().split(Constants.ZTABLES)[1].split("/");
									if ((parts.length) > 2) {
										String tableId = parts[1];
										String tabletDir = parts[2];
										TableManager.getInstance().updateTableStateCache(tableId);
										TableState tableState = TableManager.getInstance().getTableState(tableId);
										if ((tableState != null) && (tableState != (TableState.DELETING))) {
											if (!(tabletDir.startsWith(Constants.CLONE_PREFIX)))
												SimpleGarbageCollector.log.debug(("File doesn't exist: " + fullPath));

										}
									}else {
										SimpleGarbageCollector.log.warn(("Very strange path name: " + delete));
									}
								}

							if (removeFlag && (finalWriter != null)) {
								SimpleGarbageCollector.putMarkerDeleteMutation(delete, finalWriter);
							}
						} catch (Exception e) {
							SimpleGarbageCollector.log.error("{}", e.getMessage(), e);
						}
					}
				};
				deleteThreadPool.execute(deleteTask);
			}
			deleteThreadPool.shutdown();
			try {
				while (!(deleteThreadPool.awaitTermination(1000, TimeUnit.MILLISECONDS))) {
				} 
			} catch (InterruptedException e1) {
				SimpleGarbageCollector.log.error("{}", e1.getMessage(), e1);
			}
			if (writer != null) {
				try {
					writer.close();
				} catch (MutationsRejectedException e) {
					SimpleGarbageCollector.log.error("Problem removing entries from the metadata table: ", e);
				}
			}
		}

		@Override
		public void deleteTableDirIfEmpty(String tableID) throws IOException {
			for (String dir : ServerConstants.getTablesDirs()) {
				FileStatus[] tabletDirs = null;
				try {
					tabletDirs = fs.listStatus(new Path(((dir + "/") + tableID)));
				} catch (FileNotFoundException ex) {
					continue;
				}
				if ((tabletDirs.length) == 0) {
					Path p = new Path(((dir + "/") + tableID));
					SimpleGarbageCollector.log.debug(("Removing table dir " + p));
					if (!(archiveOrMoveToTrash(p)))
						fs.delete(p);

				}
			}
		}

		@Override
		public void incrementCandidatesStat(long i) {
			status.current.candidates += i;
		}

		@Override
		public void incrementInUseStat(long i) {
			status.current.inUse += i;
		}

		@Override
		public Iterator<Map.Entry<String, Replication.Status>> getReplicationNeededIterator() throws AccumuloException, AccumuloSecurityException {
			Connector conn = getConnector();
			try {
				Scanner s = ReplicationTable.getScanner(conn);
				limit(s);
				return Iterators.transform(s.iterator(), new Function<Map.Entry<Key, Value>, Map.Entry<String, Replication.Status>>() {
					@Override
					public Map.Entry<String, Replication.Status> apply(Map.Entry<Key, Value> input) {
						String file = input.getKey().getRow().toString();
						Replication.Status stat;
						try {
							stat = parseFrom(input.getValue().get());
						} catch (InvalidProtocolBufferException e) {
							SimpleGarbageCollector.log.warn(("Could not deserialize protobuf for: " + (input.getKey())));
							stat = null;
						}
						return Maps.immutableEntry(file, stat);
					}
				});
			} catch (ReplicationTableOfflineException e) {
				return Collections.emptyIterator();
			}
		}
	}

	private void run() {
		long tStart;
		long tStop;
		SimpleGarbageCollector.log.info("Trying to acquire ZooKeeper lock for garbage collector");
		try {
			getZooLock(startStatsService());
		} catch (Exception ex) {
			SimpleGarbageCollector.log.error("{}", ex.getMessage(), ex);
			System.exit(1);
		}
		try {
			long delay = getStartDelay();
			SimpleGarbageCollector.log.debug((("Sleeping for " + delay) + " milliseconds before beginning garbage collection cycles"));
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			SimpleGarbageCollector.log.warn("{}", e.getMessage(), e);
			return;
		}
		ProbabilitySampler sampler = new ProbabilitySampler(getConfiguration().getFraction(Property.GC_TRACE_PERCENT));
		while (true) {
			Trace.on("gc", sampler);
			Span gcSpan = Trace.start("loop");
			tStart = System.currentTimeMillis();
			try {
				System.gc();
				status.current.started = System.currentTimeMillis();
				new GarbageCollectionAlgorithm().collect(new SimpleGarbageCollector.GCEnv(RootTable.NAME));
				new GarbageCollectionAlgorithm().collect(new SimpleGarbageCollector.GCEnv(MetadataTable.NAME));
				SimpleGarbageCollector.log.info(("Number of data file candidates for deletion: " + (status.current.candidates)));
				SimpleGarbageCollector.log.info(("Number of data file candidates still in use: " + (status.current.inUse)));
				SimpleGarbageCollector.log.info(("Number of successfully deleted data files: " + (status.current.deleted)));
				SimpleGarbageCollector.log.info(("Number of data files delete failures: " + (status.current.errors)));
				status.current.finished = System.currentTimeMillis();
				status.last = status.current;
				status.current = new GcCycleStats();
			} catch (Exception e) {
				SimpleGarbageCollector.log.error("{}", e.getMessage(), e);
			}
			tStop = System.currentTimeMillis();
			SimpleGarbageCollector.log.info(String.format("Collect cycle took %.2f seconds", ((tStop - tStart) / 1000.0)));
			Span replSpan = Trace.start("replicationClose");
			try {
				CloseWriteAheadLogReferences closeWals = new CloseWriteAheadLogReferences(this);
				closeWals.run();
			} catch (Exception e) {
				SimpleGarbageCollector.log.error("Error trying to close write-ahead logs for replication table", e);
			} finally {
				replSpan.stop();
			}
			Span waLogs = Trace.start("walogs");
			try {
				SimpleGarbageCollector.log.info("Beginning garbage collection of write-ahead logs");
			} catch (Exception e) {
				SimpleGarbageCollector.log.error("{}", e.getMessage(), e);
			} finally {
				waLogs.stop();
			}
			gcSpan.stop();
			try {
				Connector connector = getConnector();
				connector.tableOperations().compact(MetadataTable.NAME, null, null, true, true);
				connector.tableOperations().compact(RootTable.NAME, null, null, true, true);
			} catch (Exception e) {
				SimpleGarbageCollector.log.warn("{}", e.getMessage(), e);
			}
			Trace.off();
			try {
				long gcDelay = getConfiguration().getTimeInMillis(Property.GC_CYCLE_DELAY);
				SimpleGarbageCollector.log.debug((("Sleeping for " + gcDelay) + " milliseconds"));
				Thread.sleep(gcDelay);
			} catch (InterruptedException e) {
				SimpleGarbageCollector.log.warn("{}", e.getMessage(), e);
				return;
			}
		} 
	}

	boolean archiveOrMoveToTrash(Path path) throws IOException {
		if (shouldArchiveFiles()) {
			return archiveFile(path);
		}else {
			if (!(isUsingTrash()))
				return false;

			try {
				return fs.moveToTrash(path);
			} catch (FileNotFoundException ex) {
				return false;
			}
		}
	}

	boolean archiveFile(Path fileToArchive) throws IOException {
		Volume sourceVolume = fs.getVolumeByPath(fileToArchive);
		String sourceVolumeBasePath = sourceVolume.getBasePath();
		SimpleGarbageCollector.log.debug(("Base path for volume: " + sourceVolumeBasePath));
		String sourcePathBasePath = fileToArchive.toUri().getPath();
		String relativeVolumePath = sourcePathBasePath.substring(sourceVolumeBasePath.length());
		if ((Path.SEPARATOR_CHAR) == (relativeVolumePath.charAt(0))) {
			if ((relativeVolumePath.length()) > 1) {
				relativeVolumePath = relativeVolumePath.substring(1);
			}else {
				relativeVolumePath = "";
			}
		}
		SimpleGarbageCollector.log.debug(("Computed relative path for file to archive: " + relativeVolumePath));
		Path archivePath = new Path(sourceVolumeBasePath, ServerConstants.FILE_ARCHIVE_DIR);
		SimpleGarbageCollector.log.debug(("File archive path: " + archivePath));
		fs.mkdirs(archivePath);
		Path fileArchivePath = new Path(archivePath, relativeVolumePath);
		SimpleGarbageCollector.log.debug(((((("Create full path of " + fileArchivePath) + " from ") + archivePath) + " and ") + relativeVolumePath));
		if (fs.exists(fileArchivePath)) {
			SimpleGarbageCollector.log.warn(("Tried to archive file, but it already exists: " + fileArchivePath));
			return false;
		}
		SimpleGarbageCollector.log.debug(((("Moving " + fileToArchive) + " to ") + fileArchivePath));
		return fs.rename(fileToArchive, fileArchivePath);
	}

	private void getZooLock(HostAndPort addr) throws InterruptedException, KeeperException {
		String path = (ZooUtil.getRoot(getInstance())) + (Constants.ZGC_LOCK);
		org.apache.accumulo.fate.zookeeper.ZooLock.LockWatcher lockWatcher = new org.apache.accumulo.fate.zookeeper.ZooLock.LockWatcher() {
			@Override
			public void lostLock(org.apache.accumulo.fate.zookeeper.ZooLock.LockLossReason reason) {
				Halt.halt((("GC lock in zookeeper lost (reason = " + reason) + "), exiting!"), 1);
			}

			@Override
			public void unableToMonitorLockNode(final Throwable e) {
				Halt.halt((-1), new Runnable() {
					@Override
					public void run() {
						SimpleGarbageCollector.log.error("FATAL: No longer able to monitor lock node ", e);
					}
				});
			}
		};
		while (true) {
			lock = new ZooLock(path);
			if (lock.tryLock(lockWatcher, new ServerServices(addr.toString(), GC_CLIENT).toString().getBytes())) {
				SimpleGarbageCollector.log.debug("Got GC ZooKeeper lock");
				return;
			}
			SimpleGarbageCollector.log.debug("Failed to get GC ZooKeeper lock, will retry");
			UtilWaitThread.sleepUninterruptibly(1, TimeUnit.SECONDS);
		} 
	}

	private HostAndPort startStatsService() throws UnknownHostException {
		GCMonitorService.Iface rpcProxy = RpcWrapper.service(this, new GCMonitorService.Processor<GCMonitorService.Iface>(this));
		final GCMonitorService.Processor<GCMonitorService.Iface> processor;
		if ((ThriftServerType.SASL) == (getThriftServerType())) {
			GCMonitorService.Iface tcProxy = TCredentialsUpdatingWrapper.service(rpcProxy, getClass(), getConfiguration());
			processor = new GCMonitorService.Processor<>(tcProxy);
		}else {
			processor = new GCMonitorService.Processor<>(rpcProxy);
		}
		int[] port = getConfiguration().getPort(Property.GC_PORT);
		HostAndPort[] addresses = TServerUtils.getHostAndPorts(this.opts.getAddress(), port);
		long maxMessageSize = getConfiguration().getMemoryInBytes(Property.GENERAL_MAX_MESSAGE_SIZE);
		try {
			ServerAddress server = TServerUtils.startTServer(getConfiguration(), getThriftServerType(), processor, this.getClass().getSimpleName(), "GC Monitor Service", 2, getConfiguration().getCount(Property.GENERAL_SIMPLETIMER_THREADPOOL_SIZE), 1000, maxMessageSize, getServerSslParams(), getSaslParams(), 0, addresses);
			SimpleGarbageCollector.log.debug(("Starting garbage collector listening on " + (server.address)));
			return server.address;
		} catch (Exception ex) {
			SimpleGarbageCollector.log.error("FATAL:", ex);
			throw new RuntimeException(ex);
		}
	}

	static boolean almostOutOfMemory(Runtime runtime) {
		return ((runtime.totalMemory()) - (runtime.freeMemory())) > ((SimpleGarbageCollector.CANDIDATE_MEMORY_PERCENTAGE) * (runtime.maxMemory()));
	}

	private static void putMarkerDeleteMutation(final String delete, final BatchWriter writer) throws MutationsRejectedException {
		Mutation m = new Mutation(((getRowPrefix()) + delete));
		m.putDelete(SimpleGarbageCollector.EMPTY_TEXT, SimpleGarbageCollector.EMPTY_TEXT);
		writer.addMutation(m);
	}

	static boolean isDir(String delete) {
		if (delete == null) {
			return false;
		}
		int slashCount = 0;
		for (int i = 0; i < (delete.length()); i++)
			if ((delete.charAt(i)) == '/')
				slashCount++;


		return slashCount == 1;
	}

	@Override
	public GCStatus getStatus(TInfo info, TCredentials credentials) {
		return status;
	}
}

