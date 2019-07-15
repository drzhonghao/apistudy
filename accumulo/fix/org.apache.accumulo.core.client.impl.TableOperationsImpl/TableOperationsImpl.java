

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IsolatedScanner;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.NamespaceExistsException;
import org.apache.accumulo.core.client.NamespaceNotFoundException;
import org.apache.accumulo.core.client.RowIterator;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.TableDeletedException;
import org.apache.accumulo.core.client.TableExistsException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.admin.CompactionConfig;
import org.apache.accumulo.core.client.admin.CompactionStrategyConfig;
import org.apache.accumulo.core.client.admin.DiskUsage;
import org.apache.accumulo.core.client.admin.FindMax;
import org.apache.accumulo.core.client.admin.InstanceOperations;
import org.apache.accumulo.core.client.admin.Locations;
import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.client.admin.TimeType;
import org.apache.accumulo.core.client.impl.AccumuloServerException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ClientExec;
import org.apache.accumulo.core.client.impl.ClientExecReturn;
import org.apache.accumulo.core.client.impl.CompactionStrategyConfigUtil;
import org.apache.accumulo.core.client.impl.MasterClient;
import org.apache.accumulo.core.client.impl.ServerClient;
import org.apache.accumulo.core.client.impl.TableOperationsHelper;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.TabletLocator;
import org.apache.accumulo.core.client.impl.thrift.ClientService;
import org.apache.accumulo.core.client.impl.thrift.ClientService.Client;
import org.apache.accumulo.core.client.impl.thrift.SecurityErrorCode;
import org.apache.accumulo.core.client.impl.thrift.TDiskUsage;
import org.apache.accumulo.core.client.impl.thrift.TableOperationExceptionType;
import org.apache.accumulo.core.client.impl.thrift.ThriftSecurityException;
import org.apache.accumulo.core.client.impl.thrift.ThriftTableOperationException;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.ConfigurationCopy;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.constraints.Constraint;
import org.apache.accumulo.core.data.ByteSequence;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.TabletId;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.impl.TabletIdImpl;
import org.apache.accumulo.core.data.thrift.TKeyExtent;
import org.apache.accumulo.core.iterators.IteratorUtil;
import org.apache.accumulo.core.iterators.IteratorUtil.IteratorScope;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.master.thrift.FateOperation;
import org.apache.accumulo.core.master.thrift.FateService;
import org.apache.accumulo.core.master.thrift.MasterClientService;
import org.apache.accumulo.core.master.thrift.MasterClientService.Client;
import org.apache.accumulo.core.metadata.MetadataServicer;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.rpc.ThriftUtil;
import org.apache.accumulo.core.sample.impl.SamplerConfigurationImpl;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.thrift.TCredentials;
import org.apache.accumulo.core.tabletserver.thrift.NotServingTabletException;
import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
import org.apache.accumulo.core.trace.Tracer;
import org.apache.accumulo.core.trace.thrift.TInfo;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.accumulo.core.util.ColumnFQ;
import org.apache.accumulo.core.util.HostAndPort;
import org.apache.accumulo.core.util.LocalityGroupUtil;
import org.apache.accumulo.core.util.MapCounter;
import org.apache.accumulo.core.util.NamingThreadFactory;
import org.apache.accumulo.core.util.OpTimer;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.core.util.TextUtil;
import org.apache.accumulo.core.volume.Volume;
import org.apache.accumulo.core.volume.VolumeConfiguration;
import org.apache.accumulo.fate.util.Retry;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.FutureLocationColumnFamily.NAME;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.TabletColumnFamily.PREV_ROW_COLUMN;


public class TableOperationsImpl extends TableOperationsHelper {
	public static final String CLONE_EXCLUDE_PREFIX = "!";

	private static final Logger log = LoggerFactory.getLogger(TableOperations.class);

	private final ClientContext context;

	public TableOperationsImpl(ClientContext context) {
		Preconditions.checkArgument((context != null), "context is null");
		this.context = context;
	}

	@Override
	public SortedSet<String> list() {
		OpTimer timer = null;
		if (TableOperationsImpl.log.isTraceEnabled()) {
			TableOperationsImpl.log.trace("tid={} Fetching list of tables...", Thread.currentThread().getId());
			timer = new OpTimer().start();
		}
		TreeSet<String> tableNames = new TreeSet<>(Tables.getNameToIdMap(context.getInstance()).keySet());
		if (timer != null) {
			timer.stop();
			TableOperationsImpl.log.trace("tid={} Fetched {} table names in {}", Thread.currentThread().getId(), tableNames.size());
		}
		return tableNames;
	}

	@Override
	public boolean exists(String tableName) {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		if ((tableName.equals(MetadataTable.NAME)) || (tableName.equals(RootTable.NAME)))
			return true;

		OpTimer timer = null;
		if (TableOperationsImpl.log.isTraceEnabled()) {
			TableOperationsImpl.log.trace("tid={} Checking if table {} exists...", Thread.currentThread().getId(), tableName);
			timer = new OpTimer().start();
		}
		boolean exists = Tables.getNameToIdMap(context.getInstance()).containsKey(tableName);
		if (timer != null) {
			timer.stop();
			TableOperationsImpl.log.trace("tid={} Checked existance of {} in {}", Thread.currentThread().getId(), exists);
		}
		return exists;
	}

	@Override
	public void create(String tableName) throws AccumuloException, AccumuloSecurityException, TableExistsException {
		create(tableName, new NewTableConfiguration());
	}

	@Override
	@Deprecated
	public void create(String tableName, boolean limitVersion) throws AccumuloException, AccumuloSecurityException, TableExistsException {
		create(tableName, limitVersion, TimeType.MILLIS);
	}

	@Override
	@Deprecated
	public void create(String tableName, boolean limitVersion, TimeType timeType) throws AccumuloException, AccumuloSecurityException, TableExistsException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((timeType != null), "timeType is null");
		NewTableConfiguration ntc = new NewTableConfiguration().setTimeType(timeType);
		if (limitVersion)
			create(tableName, ntc);
		else
			create(tableName, ntc.withoutDefaultIterators());

	}

	@Override
	public void create(String tableName, NewTableConfiguration ntc) throws AccumuloException, AccumuloSecurityException, TableExistsException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((ntc != null), "ntc is null");
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableName.getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap(ntc.getTimeType().name().getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = ntc.getProperties();
		try {
			doTableFateOperation(tableName, AccumuloException.class, FateOperation.TABLE_CREATE, args, opts);
		} catch (TableNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	private long beginFateOperation() throws ThriftSecurityException, TException {
		while (true) {
			MasterClientService.Iface client = null;
			try {
				client = MasterClient.getConnectionWithRetry(context);
				return client.beginFateOperation(Tracer.traceInfo(), context.rpcCreds());
			} catch (TTransportException tte) {
				TableOperationsImpl.log.debug("Failed to call beginFateOperation(), retrying ... ", tte);
				UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
			} finally {
				MasterClient.close(client);
			}
		} 
	}

	private void executeFateOperation(long opid, FateOperation op, List<ByteBuffer> args, Map<String, String> opts, boolean autoCleanUp) throws ThriftSecurityException, ThriftTableOperationException, TException {
		while (true) {
			MasterClientService.Iface client = null;
			try {
				client = MasterClient.getConnectionWithRetry(context);
				client.executeFateOperation(Tracer.traceInfo(), context.rpcCreds(), opid, op, args, opts, autoCleanUp);
				break;
			} catch (TTransportException tte) {
				TableOperationsImpl.log.debug("Failed to call executeFateOperation(), retrying ... ", tte);
				UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
			} finally {
				MasterClient.close(client);
			}
		} 
	}

	private String waitForFateOperation(long opid) throws ThriftSecurityException, ThriftTableOperationException, TException {
		while (true) {
			MasterClientService.Iface client = null;
			try {
				client = MasterClient.getConnectionWithRetry(context);
				return client.waitForFateOperation(Tracer.traceInfo(), context.rpcCreds(), opid);
			} catch (TTransportException tte) {
				TableOperationsImpl.log.debug("Failed to call waitForFateOperation(), retrying ... ", tte);
				UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
			} finally {
				MasterClient.close(client);
			}
		} 
	}

	private void finishFateOperation(long opid) throws ThriftSecurityException, TException {
		while (true) {
			MasterClientService.Iface client = null;
			try {
				client = MasterClient.getConnectionWithRetry(context);
				client.finishFateOperation(Tracer.traceInfo(), context.rpcCreds(), opid);
				break;
			} catch (TTransportException tte) {
				TableOperationsImpl.log.debug("Failed to call finishFateOperation(), retrying ... ", tte);
				UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
			} finally {
				MasterClient.close(client);
			}
		} 
	}

	String doFateOperation(FateOperation op, List<ByteBuffer> args, Map<String, String> opts, String tableOrNamespaceName) throws AccumuloException, AccumuloSecurityException, NamespaceExistsException, NamespaceNotFoundException, TableExistsException, TableNotFoundException {
		return doFateOperation(op, args, opts, tableOrNamespaceName, true);
	}

	String doFateOperation(FateOperation op, List<ByteBuffer> args, Map<String, String> opts, String tableOrNamespaceName, boolean wait) throws AccumuloException, AccumuloSecurityException, NamespaceExistsException, NamespaceNotFoundException, TableExistsException, TableNotFoundException {
		Long opid = null;
		try {
			opid = beginFateOperation();
			executeFateOperation(opid, op, args, opts, (!wait));
			if (!wait) {
				opid = null;
				return null;
			}
			String ret = waitForFateOperation(opid);
			return ret;
		} catch (ThriftSecurityException e) {
			switch (e.getCode()) {
				case TABLE_DOESNT_EXIST :
					throw new TableNotFoundException(null, tableOrNamespaceName, "Target table does not exist");
				case NAMESPACE_DOESNT_EXIST :
					throw new NamespaceNotFoundException(null, tableOrNamespaceName, "Target namespace does not exist");
				default :
					String tableInfo = Tables.getPrintableTableInfoFromName(context.getInstance(), tableOrNamespaceName);
					throw new AccumuloSecurityException(e.user, e.code, tableInfo, e);
			}
		} catch (ThriftTableOperationException e) {
			switch (e.getType()) {
				case EXISTS :
					throw new TableExistsException(e);
				case NOTFOUND :
					throw new TableNotFoundException(e);
				case NAMESPACE_EXISTS :
					throw new NamespaceExistsException(e);
				case NAMESPACE_NOTFOUND :
					throw new NamespaceNotFoundException(e);
				case OFFLINE :
					throw new TableOfflineException(context.getInstance(), Tables.getTableId(context.getInstance(), tableOrNamespaceName));
				default :
					throw new AccumuloException(e.description, e);
			}
		} catch (Exception e) {
			throw new AccumuloException(e.getMessage(), e);
		} finally {
			Tables.clearCache(context.getInstance());
			if (opid != null)
				try {
					finishFateOperation(opid);
				} catch (Exception e) {
					TableOperationsImpl.log.warn("Exception thrown while finishing fate table operation", e);
				}

		}
	}

	private static class SplitEnv {
		private String tableName;

		private String tableId;

		private ExecutorService executor;

		private CountDownLatch latch;

		private AtomicReference<Throwable> exception;

		SplitEnv(String tableName, String tableId, ExecutorService executor, CountDownLatch latch, AtomicReference<Throwable> exception) {
			this.tableName = tableName;
			this.tableId = tableId;
			this.executor = executor;
			this.latch = latch;
			this.exception = exception;
		}
	}

	private class SplitTask implements Runnable {
		private List<Text> splits;

		private TableOperationsImpl.SplitEnv env;

		SplitTask(TableOperationsImpl.SplitEnv env, List<Text> splits) {
			this.env = env;
			this.splits = splits;
		}

		@Override
		public void run() {
			try {
				if ((env.exception.get()) != null)
					return;

				if ((splits.size()) <= 2) {
					addSplits(env.tableName, new TreeSet<>(splits), env.tableId);
					for (int i = 0; i < (splits.size()); i++)
						env.latch.countDown();

					return;
				}
				int mid = (splits.size()) / 2;
				addSplits(env.tableName, new TreeSet<>(splits.subList(mid, (mid + 1))), env.tableId);
				env.latch.countDown();
				env.executor.execute(new TableOperationsImpl.SplitTask(env, splits.subList(0, mid)));
				env.executor.execute(new TableOperationsImpl.SplitTask(env, splits.subList((mid + 1), splits.size())));
			} catch (Throwable t) {
				env.exception.compareAndSet(null, t);
			}
		}
	}

	@Override
	public void addSplits(String tableName, SortedSet<Text> partitionKeys) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		String tableId = Tables.getTableId(context.getInstance(), tableName);
		List<Text> splits = new ArrayList<>(partitionKeys);
		Collections.sort(splits);
		CountDownLatch latch = new CountDownLatch(splits.size());
		AtomicReference<Throwable> exception = new AtomicReference<>(null);
		ExecutorService executor = Executors.newFixedThreadPool(16, new NamingThreadFactory("addSplits"));
		try {
			executor.execute(new TableOperationsImpl.SplitTask(new TableOperationsImpl.SplitEnv(tableName, tableId, executor, latch, exception), splits));
			while (!(latch.await(100, TimeUnit.MILLISECONDS))) {
				if ((exception.get()) != null) {
					executor.shutdownNow();
					Throwable excep = exception.get();
					if (excep instanceof TableNotFoundException) {
						TableNotFoundException tnfe = ((TableNotFoundException) (excep));
						throw new TableNotFoundException(tableId, tableName, "Table not found by background thread", tnfe);
					}else
						if (excep instanceof TableOfflineException) {
							TableOperationsImpl.log.debug("TableOfflineException occurred in background thread. Throwing new exception", excep);
							throw new TableOfflineException(context.getInstance(), tableId);
						}else
							if (excep instanceof AccumuloSecurityException) {
								AccumuloSecurityException base = ((AccumuloSecurityException) (excep));
								throw new AccumuloSecurityException(base.getUser(), base.asThriftException().getCode(), base.getTableInfo(), excep);
							}else
								if (excep instanceof AccumuloServerException) {
								}else
									if (excep instanceof Error) {
										throw new Error(excep);
									}else {
										throw new AccumuloException(excep);
									}




				}
			} 
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			executor.shutdown();
		}
	}

	private void addSplits(String tableName, SortedSet<Text> partitionKeys, String tableId) throws AccumuloException, AccumuloSecurityException, TableNotFoundException, AccumuloServerException {
		TabletLocator tabLocator = TabletLocator.getLocator(context, tableId);
		for (Text split : partitionKeys) {
			boolean successful = false;
			int attempt = 0;
			long locationFailures = 0;
			while (!successful) {
				if (attempt > 0)
					UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);

				attempt++;
				TabletLocator.TabletLocation tl = tabLocator.locateTablet(context, split, false, false);
				if (tl == null) {
					if (!(Tables.exists(context.getInstance(), tableId)))
						throw new TableNotFoundException(tableId, tableName, null);
					else
						if ((Tables.getTableState(context.getInstance(), tableId)) == (TableState.OFFLINE))
							throw new TableOfflineException(context.getInstance(), tableId);


					continue;
				}
				HostAndPort address = HostAndPort.fromString(tl.tablet_location);
				try {
					TabletClientService.Client client = ThriftUtil.getTServerClient(address, context);
					try {
						OpTimer timer = null;
						if (TableOperationsImpl.log.isTraceEnabled()) {
							TableOperationsImpl.log.trace("tid={} Splitting tablet {} on {} at {}");
							timer = new OpTimer().start();
						}
						client.splitTablet(Tracer.traceInfo(), context.rpcCreds(), tl.tablet_extent.toThrift(), TextUtil.getByteBuffer(split));
						tabLocator.invalidateCache(tl.tablet_extent);
						if (timer != null) {
							timer.stop();
							TableOperationsImpl.log.trace("Split tablet in {}", String.format("%.3f secs", timer.scale(TimeUnit.SECONDS)));
						}
					} finally {
						ThriftUtil.returnClient(client);
					}
				} catch (TApplicationException tae) {
					throw new AccumuloServerException(address.toString(), tae);
				} catch (TTransportException e) {
					tabLocator.invalidateCache(context.getInstance(), tl.tablet_location);
					continue;
				} catch (ThriftSecurityException e) {
					Tables.clearCache(context.getInstance());
					if (!(Tables.exists(context.getInstance(), tableId)))
						throw new TableNotFoundException(tableId, tableName, null);

					throw new AccumuloSecurityException(e.user, e.code, e);
				} catch (NotServingTabletException e) {
					locationFailures++;
					if ((5 == locationFailures) || (0 == (locationFailures % 50))) {
						TableOperationsImpl.log.warn(("Having difficulty locating hosting tabletserver for split {} on table {}." + " Seen {} failures."), split, tableName);
					}
					tabLocator.invalidateCache(tl.tablet_extent);
					continue;
				} catch (TException e) {
					tabLocator.invalidateCache(context.getInstance(), tl.tablet_location);
					continue;
				}
				successful = true;
			} 
		}
	}

	@Override
	public void merge(String tableName, Text start, Text end) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		ByteBuffer EMPTY = ByteBuffer.allocate(0);
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableName.getBytes(StandardCharsets.UTF_8)), (start == null ? EMPTY : TextUtil.getByteBuffer(start)), (end == null ? EMPTY : TextUtil.getByteBuffer(end)));
		Map<String, String> opts = new HashMap<>();
		try {
			doTableFateOperation(tableName, TableNotFoundException.class, FateOperation.TABLE_MERGE, args, opts);
		} catch (TableExistsException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public void deleteRows(String tableName, Text start, Text end) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		ByteBuffer EMPTY = ByteBuffer.allocate(0);
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableName.getBytes(StandardCharsets.UTF_8)), (start == null ? EMPTY : TextUtil.getByteBuffer(start)), (end == null ? EMPTY : TextUtil.getByteBuffer(end)));
		Map<String, String> opts = new HashMap<>();
		try {
			doTableFateOperation(tableName, TableNotFoundException.class, FateOperation.TABLE_DELETE_RANGE, args, opts);
		} catch (TableExistsException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public Collection<Text> listSplits(String tableName) throws AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		String tableId = Tables.getTableId(context.getInstance(), tableName);
		TreeMap<KeyExtent, String> tabletLocations = new TreeMap<>();
		while (true) {
			try {
				tabletLocations.clear();
				MetadataServicer.forTableId(context, tableId).getTabletLocations(tabletLocations);
				break;
			} catch (AccumuloSecurityException ase) {
				throw ase;
			} catch (Exception e) {
				if (!(Tables.exists(context.getInstance(), tableId))) {
					throw new TableNotFoundException(tableId, tableName, null);
				}
				if ((e instanceof RuntimeException) && ((e.getCause()) instanceof AccumuloSecurityException)) {
					throw ((AccumuloSecurityException) (e.getCause()));
				}
				TableOperationsImpl.log.info("{} ... retrying ...", e.getMessage());
				UtilWaitThread.sleepUninterruptibly(3, TimeUnit.SECONDS);
			}
		} 
		ArrayList<Text> endRows = new ArrayList<>(tabletLocations.size());
		for (KeyExtent ke : tabletLocations.keySet())
			if ((ke.getEndRow()) != null)
				endRows.add(ke.getEndRow());


		return endRows;
	}

	@Deprecated
	@Override
	public Collection<Text> getSplits(String tableName) throws TableNotFoundException {
		try {
			return listSplits(tableName);
		} catch (AccumuloSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<Text> listSplits(String tableName, int maxSplits) throws AccumuloSecurityException, TableNotFoundException {
		Collection<Text> endRows = listSplits(tableName);
		if ((endRows.size()) <= maxSplits)
			return endRows;

		double r = (maxSplits + 1) / ((double) (endRows.size()));
		double pos = 0;
		ArrayList<Text> subset = new ArrayList<>(maxSplits);
		int j = 0;
		for (int i = 0; (i < (endRows.size())) && (j < maxSplits); i++) {
			pos += r;
			while (pos > 1) {
				subset.add(((ArrayList<Text>) (endRows)).get(i));
				j++;
				pos -= 1;
			} 
		}
		return subset;
	}

	@Deprecated
	@Override
	public Collection<Text> getSplits(String tableName, int maxSplits) throws TableNotFoundException {
		try {
			return listSplits(tableName, maxSplits);
		} catch (AccumuloSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableName.getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = new HashMap<>();
		try {
			doTableFateOperation(tableName, TableNotFoundException.class, FateOperation.TABLE_DELETE, args, opts);
		} catch (TableExistsException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public void clone(String srcTableName, String newTableName, boolean flush, Map<String, String> propertiesToSet, Set<String> propertiesToExclude) throws AccumuloException, AccumuloSecurityException, TableExistsException, TableNotFoundException {
		Preconditions.checkArgument((srcTableName != null), "srcTableName is null");
		Preconditions.checkArgument((newTableName != null), "newTableName is null");
		String srcTableId = Tables.getTableId(context.getInstance(), srcTableName);
		if (flush)
			_flush(srcTableId, null, null, true);

		if (propertiesToExclude == null)
			propertiesToExclude = Collections.emptySet();

		if (propertiesToSet == null)
			propertiesToSet = Collections.emptyMap();

		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(srcTableId.getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap(newTableName.getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = new HashMap<>();
		for (Map.Entry<String, String> entry : propertiesToSet.entrySet()) {
			if (entry.getKey().startsWith(TableOperationsImpl.CLONE_EXCLUDE_PREFIX))
				throw new IllegalArgumentException(("Property can not start with " + (TableOperationsImpl.CLONE_EXCLUDE_PREFIX)));

			opts.put(entry.getKey(), entry.getValue());
		}
		for (String prop : propertiesToExclude) {
			opts.put(((TableOperationsImpl.CLONE_EXCLUDE_PREFIX) + prop), "");
		}
		doTableFateOperation(newTableName, AccumuloException.class, FateOperation.TABLE_CLONE, args, opts);
	}

	@Override
	public void rename(String oldTableName, String newTableName) throws AccumuloException, AccumuloSecurityException, TableExistsException, TableNotFoundException {
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(oldTableName.getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap(newTableName.getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = new HashMap<>();
		doTableFateOperation(oldTableName, TableNotFoundException.class, FateOperation.TABLE_RENAME, args, opts);
	}

	@Override
	@Deprecated
	public void flush(String tableName) throws AccumuloException, AccumuloSecurityException {
		try {
			flush(tableName, null, null, false);
		} catch (TableNotFoundException e) {
			throw new AccumuloException(e.getMessage(), e);
		}
	}

	@Override
	public void flush(String tableName, Text start, Text end, boolean wait) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		String tableId = Tables.getTableId(context.getInstance(), tableName);
		_flush(tableId, start, end, wait);
	}

	@Override
	public void compact(String tableName, Text start, Text end, boolean flush, boolean wait) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		compact(tableName, start, end, new ArrayList<IteratorSetting>(), flush, wait);
	}

	@Override
	public void compact(String tableName, Text start, Text end, List<IteratorSetting> iterators, boolean flush, boolean wait) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		compact(tableName, new CompactionConfig().setStartRow(start).setEndRow(end).setIterators(iterators).setFlush(flush).setWait(wait));
	}

	@Override
	public void compact(String tableName, CompactionConfig config) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		ByteBuffer EMPTY = ByteBuffer.allocate(0);
		final String skviName = SortedKeyValueIterator.class.getName();
		for (IteratorSetting setting : config.getIterators()) {
			String iteratorClass = setting.getIteratorClass();
			if (!(testClassLoad(tableName, iteratorClass, skviName))) {
				throw new AccumuloException(("TabletServer could not load iterator class " + iteratorClass));
			}
		}
		final String compactionStrategyName = config.getCompactionStrategy().getClassName();
		if (!(CompactionStrategyConfigUtil.DEFAULT_STRATEGY.getClassName().equals(compactionStrategyName))) {
			if (!(testClassLoad(tableName, compactionStrategyName, "org.apache.accumulo.tserver.compaction.CompactionStrategy"))) {
				throw new AccumuloException(("TabletServer could not load CompactionStrategy class " + compactionStrategyName));
			}
		}
		String tableId = Tables.getTableId(context.getInstance(), tableName);
		Text start = config.getStartRow();
		Text end = config.getEndRow();
		if (config.getFlush())
			_flush(tableId, start, end, true);

		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableId.getBytes(StandardCharsets.UTF_8)), (start == null ? EMPTY : TextUtil.getByteBuffer(start)), (end == null ? EMPTY : TextUtil.getByteBuffer(end)), ByteBuffer.wrap(IteratorUtil.encodeIteratorSettings(config.getIterators())), ByteBuffer.wrap(CompactionStrategyConfigUtil.encode(config.getCompactionStrategy())));
		Map<String, String> opts = new HashMap<>();
		try {
			doFateOperation(FateOperation.TABLE_COMPACT, args, opts, tableName, config.getWait());
		} catch (TableExistsException e) {
			throw new AssertionError(e);
		} catch (NamespaceExistsException e) {
			throw new AssertionError(e);
		} catch (NamespaceNotFoundException e) {
			throw new TableNotFoundException(null, tableName, "Namespace not found", e);
		}
	}

	@Override
	public void cancelCompaction(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		String tableId = Tables.getTableId(context.getInstance(), tableName);
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableId.getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = new HashMap<>();
		try {
			doTableFateOperation(tableName, TableNotFoundException.class, FateOperation.TABLE_CANCEL_COMPACT, args, opts);
		} catch (TableExistsException e) {
			throw new AssertionError(e);
		}
	}

	private void _flush(String tableId, Text start, Text end, boolean wait) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		try {
			long flushID;
			while (true) {
				MasterClientService.Iface client = null;
				try {
					client = MasterClient.getConnectionWithRetry(context);
					flushID = client.initiateFlush(Tracer.traceInfo(), context.rpcCreds(), tableId);
					break;
				} catch (TTransportException tte) {
					TableOperationsImpl.log.debug("Failed to call initiateFlush, retrying ... ", tte);
					UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
				} finally {
					MasterClient.close(client);
				}
			} 
			while (true) {
				MasterClientService.Iface client = null;
				try {
					client = MasterClient.getConnectionWithRetry(context);
					client.waitForFlush(Tracer.traceInfo(), context.rpcCreds(), tableId, TextUtil.getByteBuffer(start), TextUtil.getByteBuffer(end), flushID, (wait ? Long.MAX_VALUE : 1));
					break;
				} catch (TTransportException tte) {
					TableOperationsImpl.log.debug("Failed to call initiateFlush, retrying ... ", tte);
					UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
				} finally {
					MasterClient.close(client);
				}
			} 
		} catch (ThriftSecurityException e) {
			switch (e.getCode()) {
				case TABLE_DOESNT_EXIST :
					throw new TableNotFoundException(tableId, null, e.getMessage(), e);
				default :
					TableOperationsImpl.log.debug("flush security exception on table id {}", tableId);
					throw new AccumuloSecurityException(e.user, e.code, e);
			}
		} catch (ThriftTableOperationException e) {
			switch (e.getType()) {
				case NOTFOUND :
					throw new TableNotFoundException(e);
				default :
					throw new AccumuloException(e.description, e);
			}
		} catch (Exception e) {
			throw new AccumuloException(e);
		}
	}

	@Override
	public void setProperty(final String tableName, final String property, final String value) throws AccumuloException, AccumuloSecurityException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((property != null), "property is null");
		Preconditions.checkArgument((value != null), "value is null");
		try {
			MasterClient.executeTable(context, new ClientExec<MasterClientService.Client>() {
				@Override
				public void execute(MasterClientService.Client client) throws Exception {
					client.setTableProperty(Tracer.traceInfo(), context.rpcCreds(), tableName, property, value);
				}
			});
		} catch (TableNotFoundException e) {
			throw new AccumuloException(e);
		}
	}

	@Override
	public void removeProperty(final String tableName, final String property) throws AccumuloException, AccumuloSecurityException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((property != null), "property is null");
		try {
			MasterClient.executeTable(context, new ClientExec<MasterClientService.Client>() {
				@Override
				public void execute(MasterClientService.Client client) throws Exception {
					client.removeTableProperty(Tracer.traceInfo(), context.rpcCreds(), tableName, property);
				}
			});
		} catch (TableNotFoundException e) {
			throw new AccumuloException(e);
		}
	}

	@Override
	public Iterable<Map.Entry<String, String>> getProperties(final String tableName) throws AccumuloException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		try {
			return ServerClient.executeRaw(context, new ClientExecReturn<Map<String, String>, ClientService.Client>() {
				@Override
				public Map<String, String> execute(ClientService.Client client) throws Exception {
					return client.getTableConfiguration(Tracer.traceInfo(), context.rpcCreds(), tableName);
				}
			}).entrySet();
		} catch (ThriftTableOperationException e) {
			switch (e.getType()) {
				case NOTFOUND :
					throw new TableNotFoundException(e);
				case NAMESPACE_NOTFOUND :
					throw new TableNotFoundException(tableName, new NamespaceNotFoundException(e));
				default :
					throw new AccumuloException(e.description, e);
			}
		} catch (AccumuloException e) {
			throw e;
		} catch (Exception e) {
			throw new AccumuloException(e);
		}
	}

	@Override
	public void setLocalityGroups(String tableName, Map<String, Set<Text>> groups) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		HashSet<Text> all = new HashSet<>();
		for (Map.Entry<String, Set<Text>> entry : groups.entrySet()) {
			if (!(Collections.disjoint(all, entry.getValue()))) {
				throw new IllegalArgumentException((("Group " + (entry.getKey())) + " overlaps with another group"));
			}
			all.addAll(entry.getValue());
		}
		for (Map.Entry<String, Set<Text>> entry : groups.entrySet()) {
			Set<Text> colFams = entry.getValue();
			String value = LocalityGroupUtil.encodeColumnFamilies(colFams);
			setProperty(tableName, ((Property.TABLE_LOCALITY_GROUP_PREFIX) + (entry.getKey())), value);
		}
		try {
			setProperty(tableName, Property.TABLE_LOCALITY_GROUPS.getKey(), Joiner.on(",").join(groups.keySet()));
		} catch (AccumuloException e) {
			if ((e.getCause()) instanceof TableNotFoundException)
				throw ((TableNotFoundException) (e.getCause()));

			throw e;
		}
		String prefix = Property.TABLE_LOCALITY_GROUP_PREFIX.getKey();
		for (Map.Entry<String, String> entry : getProperties(tableName)) {
			String property = entry.getKey();
			if (property.startsWith(prefix)) {
				String[] parts = property.split("\\.");
				String group = parts[((parts.length) - 1)];
				if (!(groups.containsKey(group))) {
					removeProperty(tableName, property);
				}
			}
		}
	}

	@Override
	public Map<String, Set<Text>> getLocalityGroups(String tableName) throws AccumuloException, TableNotFoundException {
		AccumuloConfiguration conf = new ConfigurationCopy(this.getProperties(tableName));
		Map<String, Set<ByteSequence>> groups = LocalityGroupUtil.getLocalityGroups(conf);
		Map<String, Set<Text>> groups2 = new HashMap<>();
		for (Map.Entry<String, Set<ByteSequence>> entry : groups.entrySet()) {
			HashSet<Text> colFams = new HashSet<>();
			for (ByteSequence bs : entry.getValue()) {
				colFams.add(new Text(bs.toArray()));
			}
			groups2.put(entry.getKey(), colFams);
		}
		return groups2;
	}

	@Override
	public Set<Range> splitRangeByTablets(String tableName, Range range, int maxSplits) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((range != null), "range is null");
		if (maxSplits < 1)
			throw new IllegalArgumentException("maximum splits must be >= 1");

		if (maxSplits == 1)
			return Collections.singleton(range);

		Random random = new Random();
		Map<String, Map<KeyExtent, List<Range>>> binnedRanges = new HashMap<>();
		String tableId = Tables.getTableId(context.getInstance(), tableName);
		TabletLocator tl = TabletLocator.getLocator(context, tableId);
		tl.invalidateCache();
		while (!(tl.binRanges(context, Collections.singletonList(range), binnedRanges).isEmpty())) {
			if (!(Tables.exists(context.getInstance(), tableId)))
				throw new TableDeletedException(tableId);

			if ((Tables.getTableState(context.getInstance(), tableId)) == (TableState.OFFLINE))
				throw new TableOfflineException(context.getInstance(), tableId);

			TableOperationsImpl.log.warn("Unable to locate bins for specified range. Retrying.");
			UtilWaitThread.sleepUninterruptibly((100 + (random.nextInt(100))), TimeUnit.MILLISECONDS);
			binnedRanges.clear();
			tl.invalidateCache();
		} 
		LinkedList<KeyExtent> unmergedExtents = new LinkedList<>();
		List<KeyExtent> mergedExtents = new ArrayList<>();
		for (Map<KeyExtent, List<Range>> map : binnedRanges.values())
			unmergedExtents.addAll(map.keySet());

		Collections.sort(unmergedExtents);
		while (((unmergedExtents.size()) + (mergedExtents.size())) > maxSplits) {
			if ((unmergedExtents.size()) >= 2) {
				KeyExtent first = unmergedExtents.removeFirst();
				KeyExtent second = unmergedExtents.removeFirst();
				first.setEndRow(second.getEndRow());
				mergedExtents.add(first);
			}else {
				mergedExtents.addAll(unmergedExtents);
				unmergedExtents.clear();
				unmergedExtents.addAll(mergedExtents);
				mergedExtents.clear();
			}
		} 
		mergedExtents.addAll(unmergedExtents);
		Set<Range> ranges = new HashSet<>();
		for (KeyExtent k : mergedExtents)
			ranges.add(k.toDataRange().clip(range));

		return ranges;
	}

	private Path checkPath(String dir, String kind, String type) throws IOException, AccumuloException, AccumuloSecurityException {
		Path ret;
		Map<String, String> props = context.getConnector().instanceOperations().getSystemConfiguration();
		AccumuloConfiguration conf = new ConfigurationCopy(props);
		FileSystem fs = VolumeConfiguration.getVolume(dir, CachedConfiguration.getInstance(), conf).getFileSystem();
		if (dir.contains(":")) {
			ret = new Path(dir);
		}else {
			ret = fs.makeQualified(new Path(dir));
		}
		try {
			if (!(fs.getFileStatus(ret).isDirectory())) {
				throw new AccumuloException((((((kind + " import ") + type) + " directory ") + dir) + " is not a directory!"));
			}
		} catch (FileNotFoundException fnf) {
			throw new AccumuloException((((((kind + " import ") + type) + " directory ") + dir) + " does not exist!"));
		}
		if (type.equals("failure")) {
			FileStatus[] listStatus = fs.listStatus(ret);
			if ((listStatus != null) && ((listStatus.length) != 0)) {
				throw new AccumuloException((("Bulk import failure directory " + ret) + " is not empty"));
			}
		}
		return ret;
	}

	@Override
	public void importDirectory(String tableName, String dir, String failureDir, boolean setTime) throws IOException, AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((dir != null), "dir is null");
		Preconditions.checkArgument((failureDir != null), "failureDir is null");
		Tables.getTableId(context.getInstance(), tableName);
		Path dirPath = checkPath(dir, "Bulk", "");
		Path failPath = checkPath(failureDir, "Bulk", "failure");
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableName.getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap(dirPath.toString().getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap(failPath.toString().getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap((setTime + "").getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = new HashMap<>();
		try {
			doTableFateOperation(tableName, TableNotFoundException.class, FateOperation.TABLE_BULK_IMPORT, args, opts);
		} catch (TableExistsException e) {
			throw new AssertionError(e);
		}
	}

	private void waitForTableStateTransition(String tableId, TableState expectedState) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Text startRow = null;
		Text lastRow = null;
		while (true) {
			if ((Tables.getTableState(context.getInstance(), tableId)) != expectedState) {
				Tables.clearCache(context.getInstance());
				TableState currentState = Tables.getTableState(context.getInstance(), tableId);
				if (currentState != expectedState) {
					if (!(Tables.exists(context.getInstance(), tableId)))
						throw new TableDeletedException(tableId);

					if (currentState == (TableState.DELETING))
						throw new TableNotFoundException(tableId, "", "Table is being deleted.");

					throw new AccumuloException(((((("Unexpected table state " + tableId) + " ") + (Tables.getTableState(context.getInstance(), tableId))) + " != ") + expectedState));
				}
			}
			Range range;
			if ((startRow == null) || (lastRow == null))
				range = new KeyExtent(tableId, null, null).toMetadataRange();
			else
				range = new Range(startRow, lastRow);

			String metaTable = MetadataTable.NAME;
			if (tableId.equals(MetadataTable.ID))
				metaTable = RootTable.NAME;

			Scanner scanner = createMetadataScanner(metaTable, range);
			RowIterator rowIter = new RowIterator(scanner);
			KeyExtent lastExtent = null;
			int total = 0;
			int waitFor = 0;
			int holes = 0;
			Text continueRow = null;
			MapCounter<String> serverCounts = new MapCounter<>();
			while (rowIter.hasNext()) {
				Iterator<Map.Entry<Key, Value>> row = rowIter.next();
				total++;
				KeyExtent extent = null;
				String future = null;
				String current = null;
				while (row.hasNext()) {
					Map.Entry<Key, Value> entry = row.next();
					Key key = entry.getKey();
					if (key.getColumnFamily().equals(NAME))
						future = entry.getValue().toString();

					if (key.getColumnFamily().equals(MetadataSchema.TabletsSection.CurrentLocationColumnFamily.NAME))
						current = entry.getValue().toString();

					if (PREV_ROW_COLUMN.hasColumns(key))
						extent = new KeyExtent(key.getRow(), entry.getValue());

				} 
				if (((expectedState == (TableState.ONLINE)) && (current == null)) || ((expectedState == (TableState.OFFLINE)) && ((future != null) || (current != null)))) {
					if (continueRow == null)
						continueRow = extent.getMetadataEntry();

					waitFor++;
					lastRow = extent.getMetadataEntry();
					if (current != null)
						serverCounts.increment(current, 1);

					if (future != null)
						serverCounts.increment(future, 1);

				}
				if (!(extent.getTableId().equals(tableId))) {
					throw new AccumuloException(((("Saw unexpected table Id " + tableId) + " ") + extent));
				}
				if ((lastExtent != null) && (!(extent.isPreviousExtent(lastExtent)))) {
					holes++;
				}
				lastExtent = extent;
			} 
			if (continueRow != null) {
				startRow = continueRow;
			}
			if ((holes > 0) || (total == 0)) {
				startRow = null;
				lastRow = null;
			}
			if (((waitFor > 0) || (holes > 0)) || (total == 0)) {
				long waitTime;
				long maxPerServer = 0;
				if ((serverCounts.size()) > 0) {
					maxPerServer = Collections.max(serverCounts.values());
					waitTime = maxPerServer * 10;
				}else
					waitTime = waitFor * 10;

				waitTime = Math.max(100, waitTime);
				waitTime = Math.min(5000, waitTime);
				TableOperationsImpl.log.trace("Waiting for {}({}) tablets, startRow = {} lastRow = {}, holes={} sleeping:{}ms");
				UtilWaitThread.sleepUninterruptibly(waitTime, TimeUnit.MILLISECONDS);
			}else {
				break;
			}
		} 
	}

	protected IsolatedScanner createMetadataScanner(String metaTable, Range range) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Scanner scanner = context.getConnector().createScanner(metaTable, Authorizations.EMPTY);
		PREV_ROW_COLUMN.fetch(scanner);
		scanner.fetchColumnFamily(NAME);
		scanner.fetchColumnFamily(MetadataSchema.TabletsSection.CurrentLocationColumnFamily.NAME);
		scanner.setRange(range);
		return new IsolatedScanner(scanner);
	}

	@Override
	public void offline(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		offline(tableName, false);
	}

	@Override
	public void offline(String tableName, boolean wait) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		String tableId = Tables.getTableId(context.getInstance(), tableName);
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableId.getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = new HashMap<>();
		try {
			doTableFateOperation(tableName, TableNotFoundException.class, FateOperation.TABLE_OFFLINE, args, opts);
		} catch (TableExistsException e) {
			throw new AssertionError(e);
		}
		if (wait)
			waitForTableStateTransition(tableId, TableState.OFFLINE);

	}

	@Override
	public void online(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		online(tableName, false);
	}

	@Override
	public void online(String tableName, boolean wait) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		String tableId = Tables.getTableId(context.getInstance(), tableName);
		TableState expectedState = Tables.getTableState(context.getInstance(), tableId, true);
		if (expectedState == (TableState.ONLINE)) {
			if (wait)
				waitForTableStateTransition(tableId, TableState.ONLINE);

			return;
		}
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableId.getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = new HashMap<>();
		try {
			doTableFateOperation(tableName, TableNotFoundException.class, FateOperation.TABLE_ONLINE, args, opts);
		} catch (TableExistsException e) {
			throw new AssertionError(e);
		}
		if (wait)
			waitForTableStateTransition(tableId, TableState.ONLINE);

	}

	@Override
	public void clearLocatorCache(String tableName) throws TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		TabletLocator tabLocator = TabletLocator.getLocator(context, Tables.getTableId(context.getInstance(), tableName));
		tabLocator.invalidateCache();
	}

	@Override
	public Map<String, String> tableIdMap() {
		return Tables.getNameToIdMap(context.getInstance());
	}

	@Override
	public Text getMaxRow(String tableName, Authorizations auths, Text startRow, boolean startInclusive, Text endRow, boolean endInclusive) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((auths != null), "auths is null");
		Scanner scanner = context.getConnector().createScanner(tableName, auths);
		return FindMax.findMax(scanner, startRow, startInclusive, endRow, endInclusive);
	}

	@Override
	public List<DiskUsage> getDiskUsage(Set<String> tableNames) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		List<TDiskUsage> diskUsages = null;
		while (diskUsages == null) {
			Pair<String, ClientService.Client> pair = null;
			try {
				pair = ServerClient.getConnection(context, false);
				diskUsages = pair.getSecond().getDiskUsage(tableNames, context.rpcCreds());
			} catch (ThriftTableOperationException e) {
				switch (e.getType()) {
					case NOTFOUND :
						throw new TableNotFoundException(e);
					case NAMESPACE_NOTFOUND :
						throw new TableNotFoundException(e.getTableName(), new NamespaceNotFoundException(e));
					default :
						throw new AccumuloException(e.description, e);
				}
			} catch (ThriftSecurityException e) {
				throw new AccumuloSecurityException(e.getUser(), e.getCode());
			} catch (TTransportException e) {
				if (pair == null) {
					TableOperationsImpl.log.debug("Disk usage request failed.  Pair is null.  Retrying request...", e);
				}else {
					TableOperationsImpl.log.debug("Disk usage request failed {}, retrying ... ", pair.getFirst(), e);
				}
				UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
			} catch (TException e) {
				throw new AccumuloException(e);
			} finally {
				if (pair != null)
					ServerClient.close(pair.getSecond());

			}
		} 
		List<DiskUsage> finalUsages = new ArrayList<>();
		for (TDiskUsage diskUsage : diskUsages) {
			finalUsages.add(new DiskUsage(new TreeSet<>(diskUsage.getTables()), diskUsage.getUsage()));
		}
		return finalUsages;
	}

	public static Map<String, String> getExportedProps(FileSystem fs, Path path) throws IOException {
		HashMap<String, String> props = new HashMap<>();
		ZipInputStream zis = new ZipInputStream(fs.open(path));
		try {
			ZipEntry zipEntry;
			while ((zipEntry = zis.getNextEntry()) != null) {
				if (zipEntry.getName().equals(Constants.EXPORT_TABLE_CONFIG_FILE)) {
					BufferedReader in = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
					try {
						String line;
						while ((line = in.readLine()) != null) {
							String[] sa = line.split("=", 2);
							props.put(sa[0], sa[1]);
						} 
					} finally {
						in.close();
					}
					break;
				}
			} 
		} finally {
			zis.close();
		}
		return props;
	}

	@Override
	public void importTable(String tableName, String importDir) throws AccumuloException, AccumuloSecurityException, TableExistsException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((importDir != null), "importDir is null");
		try {
			importDir = checkPath(importDir, "Table", "").toString();
		} catch (IOException e) {
			throw new AccumuloException(e);
		}
		try {
			FileSystem fs = new Path(importDir).getFileSystem(CachedConfiguration.getInstance());
			Map<String, String> props = TableOperationsImpl.getExportedProps(fs, new Path(importDir, Constants.EXPORT_FILE));
			for (Map.Entry<String, String> entry : props.entrySet()) {
				if ((Property.isClassProperty(entry.getKey())) && (!(entry.getValue().contains(Constants.CORE_PACKAGE_NAME)))) {
					LoggerFactory.getLogger(this.getClass()).info("Imported table sets '{}' to '{}'.  Ensure this class is on Accumulo classpath.", entry.getKey(), entry.getValue());
				}
			}
		} catch (IOException ioe) {
			LoggerFactory.getLogger(this.getClass()).warn("Failed to check if imported table references external java classes : {}", ioe.getMessage());
		}
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableName.getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap(importDir.getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = Collections.emptyMap();
		try {
			doTableFateOperation(tableName, AccumuloException.class, FateOperation.TABLE_IMPORT, args, opts);
		} catch (TableNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public void exportTable(String tableName, String exportDir) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((exportDir != null), "exportDir is null");
		List<ByteBuffer> args = Arrays.asList(ByteBuffer.wrap(tableName.getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap(exportDir.getBytes(StandardCharsets.UTF_8)));
		Map<String, String> opts = Collections.emptyMap();
		try {
			doTableFateOperation(tableName, TableNotFoundException.class, FateOperation.TABLE_EXPORT, args, opts);
		} catch (TableExistsException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public boolean testClassLoad(final String tableName, final String className, final String asTypeName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Preconditions.checkArgument((tableName != null), "tableName is null");
		Preconditions.checkArgument((className != null), "className is null");
		Preconditions.checkArgument((asTypeName != null), "asTypeName is null");
		try {
			return ServerClient.executeRaw(context, new ClientExecReturn<Boolean, ClientService.Client>() {
				@Override
				public Boolean execute(ClientService.Client client) throws Exception {
					return client.checkTableClass(Tracer.traceInfo(), context.rpcCreds(), tableName, className, asTypeName);
				}
			});
		} catch (ThriftTableOperationException e) {
			switch (e.getType()) {
				case NOTFOUND :
					throw new TableNotFoundException(e);
				case NAMESPACE_NOTFOUND :
					throw new TableNotFoundException(tableName, new NamespaceNotFoundException(e));
				default :
					throw new AccumuloException(e.description, e);
			}
		} catch (ThriftSecurityException e) {
			throw new AccumuloSecurityException(e.user, e.code, e);
		} catch (AccumuloException e) {
			throw e;
		} catch (Exception e) {
			throw new AccumuloException(e);
		}
	}

	@Override
	public void attachIterator(String tableName, IteratorSetting setting, EnumSet<IteratorUtil.IteratorScope> scopes) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		testClassLoad(tableName, setting.getIteratorClass(), SortedKeyValueIterator.class.getName());
		super.attachIterator(tableName, setting, scopes);
	}

	@Override
	public int addConstraint(String tableName, String constraintClassName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		testClassLoad(tableName, constraintClassName, Constraint.class.getName());
		return super.addConstraint(tableName, constraintClassName);
	}

	private void doTableFateOperation(String tableOrNamespaceName, Class<? extends Exception> namespaceNotFoundExceptionClass, FateOperation op, List<ByteBuffer> args, Map<String, String> opts) throws AccumuloException, AccumuloSecurityException, TableExistsException, TableNotFoundException {
		try {
			doFateOperation(op, args, opts, tableOrNamespaceName);
		} catch (NamespaceExistsException e) {
			throw new AssertionError(e);
		} catch (NamespaceNotFoundException e) {
			if (namespaceNotFoundExceptionClass == null) {
				throw new AssertionError(e);
			}else
				if (AccumuloException.class.isAssignableFrom(namespaceNotFoundExceptionClass)) {
					throw new AccumuloException("Cannot create table in non-existent namespace", e);
				}else
					if (TableNotFoundException.class.isAssignableFrom(namespaceNotFoundExceptionClass)) {
						throw new TableNotFoundException(null, tableOrNamespaceName, "Namespace not found", e);
					}else {
						throw new AssertionError(e);
					}


		}
	}

	private void clearSamplerOptions(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		String prefix = Property.TABLE_SAMPLER_OPTS.getKey();
		for (Map.Entry<String, String> entry : getProperties(tableName)) {
			String property = entry.getKey();
			if (property.startsWith(prefix)) {
				removeProperty(tableName, property);
			}
		}
	}

	@Override
	public void setSamplerConfiguration(String tableName, SamplerConfiguration samplerConfiguration) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		clearSamplerOptions(tableName);
		List<Pair<String, String>> props = new SamplerConfigurationImpl(samplerConfiguration).toTableProperties();
		for (Pair<String, String> pair : props) {
			setProperty(tableName, pair.getFirst(), pair.getSecond());
		}
	}

	@Override
	public void clearSamplerConfiguration(String tableName) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		removeProperty(tableName, Property.TABLE_SAMPLER.getKey());
		clearSamplerOptions(tableName);
	}

	@Override
	public SamplerConfiguration getSamplerConfiguration(String tableName) throws AccumuloException, TableNotFoundException {
		AccumuloConfiguration conf = new ConfigurationCopy(this.getProperties(tableName));
		SamplerConfigurationImpl sci = SamplerConfigurationImpl.newSamplerConfig(conf);
		if (sci == null) {
			return null;
		}
		return sci.toSamplerConfiguration();
	}

	private static class LoctionsImpl implements Locations {
		private Map<Range, List<TabletId>> groupedByRanges;

		private Map<TabletId, List<Range>> groupedByTablets;

		private Map<TabletId, String> tabletLocations;

		public LoctionsImpl(Map<String, Map<KeyExtent, List<Range>>> binnedRanges) {
			groupedByTablets = new HashMap<>();
			groupedByRanges = null;
			tabletLocations = new HashMap<>();
			for (Map.Entry<String, Map<KeyExtent, List<Range>>> entry : binnedRanges.entrySet()) {
				String location = entry.getKey();
				for (Map.Entry<KeyExtent, List<Range>> entry2 : entry.getValue().entrySet()) {
					TabletIdImpl tabletId = new TabletIdImpl(entry2.getKey());
					tabletLocations.put(tabletId, location);
					List<Range> prev = groupedByTablets.put(tabletId, Collections.unmodifiableList(entry2.getValue()));
					if (prev != null) {
						throw new RuntimeException(((("Unexpected : tablet at multiple locations : " + location) + " ") + tabletId));
					}
				}
			}
			groupedByTablets = Collections.unmodifiableMap(groupedByTablets);
		}

		@Override
		public String getTabletLocation(TabletId tabletId) {
			return tabletLocations.get(tabletId);
		}

		@Override
		public Map<Range, List<TabletId>> groupByRange() {
			if ((groupedByRanges) == null) {
				Map<Range, List<TabletId>> tmp = new HashMap<>();
				for (Map.Entry<TabletId, List<Range>> entry : groupedByTablets.entrySet()) {
					for (Range range : entry.getValue()) {
						List<TabletId> tablets = tmp.get(range);
						if (tablets == null) {
							tablets = new ArrayList<>();
							tmp.put(range, tablets);
						}
						tablets.add(entry.getKey());
					}
				}
				Map<Range, List<TabletId>> tmp2 = new HashMap<>();
				for (Map.Entry<Range, List<TabletId>> entry : tmp.entrySet()) {
					tmp2.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
				}
				groupedByRanges = Collections.unmodifiableMap(tmp2);
			}
			return groupedByRanges;
		}

		@Override
		public Map<TabletId, List<Range>> groupByTablet() {
			return groupedByTablets;
		}
	}

	@Override
	public Locations locate(String tableName, Collection<Range> ranges) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Objects.requireNonNull(tableName, "tableName must be non null");
		Objects.requireNonNull(ranges, "ranges must be non null");
		String tableId = Tables.getTableId(context.getInstance(), tableName);
		TabletLocator locator = TabletLocator.getLocator(context, tableId);
		List<Range> rangeList = null;
		if (ranges instanceof List) {
			rangeList = ((List<Range>) (ranges));
		}else {
			rangeList = new ArrayList<>(ranges);
		}
		Map<String, Map<KeyExtent, List<Range>>> binnedRanges = new HashMap<>();
		locator.invalidateCache();
		Retry retry = Retry.builder().infiniteRetries().retryAfter(100, TimeUnit.MILLISECONDS).incrementBy(100, TimeUnit.MILLISECONDS).maxWait(2, TimeUnit.SECONDS).logInterval(3, TimeUnit.MINUTES).createRetry();
		while (!(locator.binRanges(context, rangeList, binnedRanges).isEmpty())) {
			if (!(Tables.exists(context.getInstance(), tableId)))
				throw new TableNotFoundException(tableId, tableName, null);

			if ((Tables.getTableState(context.getInstance(), tableId)) == (TableState.OFFLINE))
				throw new TableOfflineException(context.getInstance(), tableId);

			binnedRanges.clear();
			try {
				retry.waitForNextAttempt();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			locator.invalidateCache();
		} 
		return new TableOperationsImpl.LoctionsImpl(binnedRanges);
	}
}

