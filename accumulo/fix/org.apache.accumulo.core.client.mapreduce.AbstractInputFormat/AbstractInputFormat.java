

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.ClientSideIteratorScanner;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IsolatedScanner;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.TableDeletedException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.admin.DelegationTokenConfig;
import org.apache.accumulo.core.client.admin.SecurityOperations;
import org.apache.accumulo.core.client.impl.AuthenticationTokenIdentifier;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.DelegationTokenImpl;
import org.apache.accumulo.core.client.impl.OfflineScanner;
import org.apache.accumulo.core.client.impl.ScannerImpl;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.TabletLocator;
import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.client.mapreduce.InputTableConfig;
import org.apache.accumulo.core.client.mapreduce.RangeInputSplit;
import org.apache.accumulo.core.client.mapreduce.impl.BatchInputSplit;
import org.apache.accumulo.core.client.mapreduce.impl.SplitUtils;
import org.apache.accumulo.core.client.mapreduce.lib.impl.ConfiguratorBase;
import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
import org.apache.accumulo.core.client.sample.SamplerConfiguration;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.DelegationToken;
import org.apache.accumulo.core.client.security.tokens.KerberosToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.DeprecationUtil;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.task.JobContextImpl;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static org.apache.accumulo.core.client.security.tokens.AuthenticationToken.AuthenticationTokenSerializer.serialize;


public abstract class AbstractInputFormat<K, V> extends InputFormat<K, V> {
	protected static final Class<?> CLASS = AccumuloInputFormat.class;

	protected static final Logger log = Logger.getLogger(AbstractInputFormat.CLASS);

	public static void setClassLoaderContext(Job job, String context) {
		InputConfigurator.setClassLoaderContext(AbstractInputFormat.CLASS, job.getConfiguration(), context);
	}

	public static String getClassLoaderContext(JobContext job) {
		return InputConfigurator.getClassLoaderContext(AbstractInputFormat.CLASS, job.getConfiguration());
	}

	public static void setConnectorInfo(Job job, String principal, AuthenticationToken token) throws AccumuloSecurityException {
		if (token instanceof KerberosToken) {
			AbstractInputFormat.log.info("Received KerberosToken, attempting to fetch DelegationToken");
			try {
				Instance instance = AbstractInputFormat.getInstance(job);
				Connector conn = instance.getConnector(principal, token);
				token = conn.securityOperations().getDelegationToken(new DelegationTokenConfig());
			} catch (Exception e) {
				AbstractInputFormat.log.warn(("Failed to automatically obtain DelegationToken, " + "Mappers/Reducers will likely fail to communicate with Accumulo"), e);
			}
		}
		if (token instanceof DelegationTokenImpl) {
			DelegationTokenImpl delegationToken = ((DelegationTokenImpl) (token));
			AuthenticationTokenIdentifier identifier = delegationToken.getIdentifier();
			Token<AuthenticationTokenIdentifier> hadoopToken = new Token<>(identifier.getBytes(), delegationToken.getPassword(), identifier.getKind(), delegationToken.getServiceName());
			job.getCredentials().addToken(hadoopToken.getService(), hadoopToken);
		}
		InputConfigurator.setConnectorInfo(AbstractInputFormat.CLASS, job.getConfiguration(), principal, token);
	}

	public static void setConnectorInfo(Job job, String principal, String tokenFile) throws AccumuloSecurityException {
		InputConfigurator.setConnectorInfo(AbstractInputFormat.CLASS, job.getConfiguration(), principal, tokenFile);
	}

	protected static Boolean isConnectorInfoSet(JobContext context) {
		return InputConfigurator.isConnectorInfoSet(AbstractInputFormat.CLASS, context.getConfiguration());
	}

	protected static String getPrincipal(JobContext context) {
		return InputConfigurator.getPrincipal(AbstractInputFormat.CLASS, context.getConfiguration());
	}

	@Deprecated
	protected static String getTokenClass(JobContext context) {
		return AbstractInputFormat.getAuthenticationToken(context).getClass().getName();
	}

	@Deprecated
	protected static byte[] getToken(JobContext context) {
		return serialize(AbstractInputFormat.getAuthenticationToken(context));
	}

	protected static AuthenticationToken getAuthenticationToken(JobContext context) {
		AuthenticationToken token = InputConfigurator.getAuthenticationToken(AbstractInputFormat.CLASS, context.getConfiguration());
		return ConfiguratorBase.unwrapAuthenticationToken(context, token);
	}

	@Deprecated
	public static void setZooKeeperInstance(Job job, String instanceName, String zooKeepers) {
		AbstractInputFormat.setZooKeeperInstance(job, new ClientConfiguration().withInstance(instanceName).withZkHosts(zooKeepers));
	}

	public static void setZooKeeperInstance(Job job, ClientConfiguration clientConfig) {
		InputConfigurator.setZooKeeperInstance(AbstractInputFormat.CLASS, job.getConfiguration(), clientConfig);
	}

	@Deprecated
	public static void setMockInstance(Job job, String instanceName) {
		InputConfigurator.setMockInstance(AbstractInputFormat.CLASS, job.getConfiguration(), instanceName);
	}

	protected static Instance getInstance(JobContext context) {
		return InputConfigurator.getInstance(AbstractInputFormat.CLASS, context.getConfiguration());
	}

	public static void setLogLevel(Job job, Level level) {
		InputConfigurator.setLogLevel(AbstractInputFormat.CLASS, job.getConfiguration(), level);
	}

	protected static Level getLogLevel(JobContext context) {
		return InputConfigurator.getLogLevel(AbstractInputFormat.CLASS, context.getConfiguration());
	}

	public static void setScanAuthorizations(Job job, Authorizations auths) {
		InputConfigurator.setScanAuthorizations(AbstractInputFormat.CLASS, job.getConfiguration(), auths);
	}

	protected static Authorizations getScanAuthorizations(JobContext context) {
		return InputConfigurator.getScanAuthorizations(AbstractInputFormat.CLASS, context.getConfiguration());
	}

	protected static Map<String, InputTableConfig> getInputTableConfigs(JobContext context) {
		return InputConfigurator.getInputTableConfigs(AbstractInputFormat.CLASS, context.getConfiguration());
	}

	protected static InputTableConfig getInputTableConfig(JobContext context, String tableName) {
		return InputConfigurator.getInputTableConfig(AbstractInputFormat.CLASS, context.getConfiguration(), tableName);
	}

	@Deprecated
	protected static TabletLocator getTabletLocator(JobContext context, String table) throws TableNotFoundException {
		return InputConfigurator.getTabletLocator(AbstractInputFormat.CLASS, context.getConfiguration(), table);
	}

	protected static void validateOptions(JobContext context) throws IOException {
		final Configuration conf = context.getConfiguration();
		final Instance inst = InputConfigurator.validateInstance(AbstractInputFormat.CLASS, conf);
		String principal = InputConfigurator.getPrincipal(AbstractInputFormat.CLASS, conf);
		AuthenticationToken token = InputConfigurator.getAuthenticationToken(AbstractInputFormat.CLASS, conf);
		token = ConfiguratorBase.unwrapAuthenticationToken(context, token);
		Connector conn;
		try {
			conn = inst.getConnector(principal, token);
		} catch (Exception e) {
			throw new IOException(e);
		}
		InputConfigurator.validatePermissions(AbstractInputFormat.CLASS, conf, conn);
	}

	protected static ClientConfiguration getClientConfiguration(JobContext context) {
		return InputConfigurator.getClientConfiguration(AbstractInputFormat.CLASS, context.getConfiguration());
	}

	protected static abstract class AbstractRecordReader<K, V> extends RecordReader<K, V> {
		protected long numKeysRead;

		protected Iterator<Map.Entry<Key, Value>> scannerIterator;

		protected ScannerBase scannerBase;

		protected RangeInputSplit split;

		protected abstract List<IteratorSetting> contextIterators(TaskAttemptContext context, String tableName);

		private void setupIterators(TaskAttemptContext context, ScannerBase scanner, String tableName, RangeInputSplit split) {
			List<IteratorSetting> iterators = null;
			if (null == split) {
				iterators = contextIterators(context, tableName);
			}else {
				iterators = split.getIterators();
				if (null == iterators) {
					iterators = contextIterators(context, tableName);
				}
			}
			for (IteratorSetting iterator : iterators)
				scanner.addScanIterator(iterator);

		}

		@Deprecated
		protected void setupIterators(TaskAttemptContext context, Scanner scanner, String tableName, RangeInputSplit split) {
			setupIterators(context, ((ScannerBase) (scanner)), tableName, split);
		}

		@Override
		public void initialize(InputSplit inSplit, TaskAttemptContext attempt) throws IOException {
			split = ((RangeInputSplit) (inSplit));
			AbstractInputFormat.log.debug(("Initializing input split: " + (split.toString())));
			Instance instance = split.getInstance(AbstractInputFormat.getClientConfiguration(attempt));
			if (null == instance) {
				instance = AbstractInputFormat.getInstance(attempt);
			}
			String principal = split.getPrincipal();
			if (null == principal) {
				principal = AbstractInputFormat.getPrincipal(attempt);
			}
			AuthenticationToken token = split.getToken();
			if (null == token) {
				token = AbstractInputFormat.getAuthenticationToken(attempt);
			}
			Authorizations authorizations = split.getAuths();
			if (null == authorizations) {
				authorizations = AbstractInputFormat.getScanAuthorizations(attempt);
			}
			String classLoaderContext = AbstractInputFormat.getClassLoaderContext(attempt);
			String table = split.getTableName();
			InputTableConfig tableConfig = AbstractInputFormat.getInputTableConfig(attempt, split.getTableName());
			AbstractInputFormat.log.debug(("Creating connector with user: " + principal));
			AbstractInputFormat.log.debug(("Creating scanner for table: " + table));
			AbstractInputFormat.log.debug(("Authorizations are: " + authorizations));
			if ((split) instanceof BatchInputSplit) {
				BatchInputSplit batchSplit = ((BatchInputSplit) (split));
				BatchScanner scanner;
				try {
					int scanThreads = 1;
					scanner = instance.getConnector(principal, token).createBatchScanner(split.getTableName(), authorizations, scanThreads);
					setupIterators(attempt, scanner, split.getTableName(), split);
					if (null != classLoaderContext) {
						scanner.setClassLoaderContext(classLoaderContext);
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new IOException(e);
				}
				scanner.setRanges(batchSplit.getRanges());
				scannerBase = scanner;
			}else {
				Scanner scanner;
				Boolean isOffline = split.isOffline();
				if (null == isOffline) {
					isOffline = tableConfig.isOfflineScan();
				}
				Boolean isIsolated = split.isIsolatedScan();
				if (null == isIsolated) {
					isIsolated = tableConfig.shouldUseIsolatedScanners();
				}
				Boolean usesLocalIterators = split.usesLocalIterators();
				if (null == usesLocalIterators) {
					usesLocalIterators = tableConfig.shouldUseLocalIterators();
				}
				try {
					if (isOffline) {
						scanner = new OfflineScanner(instance, new org.apache.accumulo.core.client.impl.Credentials(principal, token), split.getTableId(), authorizations);
					}else
						if (DeprecationUtil.isMockInstance(instance)) {
							scanner = instance.getConnector(principal, token).createScanner(split.getTableName(), authorizations);
						}else {
							ClientConfiguration clientConf = AbstractInputFormat.getClientConfiguration(attempt);
							ClientContext context = new ClientContext(instance, new org.apache.accumulo.core.client.impl.Credentials(principal, token), clientConf);
							scanner = new ScannerImpl(context, split.getTableId(), authorizations);
						}

					if (isIsolated) {
						AbstractInputFormat.log.info("Creating isolated scanner");
						scanner = new IsolatedScanner(scanner);
					}
					if (usesLocalIterators) {
						AbstractInputFormat.log.info("Using local iterators");
						scanner = new ClientSideIteratorScanner(scanner);
					}
					setupIterators(attempt, scanner, split.getTableName(), split);
				} catch (Exception e) {
					throw new IOException(e);
				}
				scanner.setRange(split.getRange());
				scannerBase = scanner;
			}
			Collection<Pair<Text, Text>> columns = split.getFetchedColumns();
			if (null == columns) {
				columns = tableConfig.getFetchedColumns();
			}
			for (Pair<Text, Text> c : columns) {
				if ((c.getSecond()) != null) {
					AbstractInputFormat.log.debug(((("Fetching column " + (c.getFirst())) + ":") + (c.getSecond())));
					scannerBase.fetchColumn(c.getFirst(), c.getSecond());
				}else {
					AbstractInputFormat.log.debug(("Fetching column family " + (c.getFirst())));
					scannerBase.fetchColumnFamily(c.getFirst());
				}
			}
			SamplerConfiguration samplerConfig = split.getSamplerConfiguration();
			if (null == samplerConfig) {
				samplerConfig = tableConfig.getSamplerConfiguration();
			}
			if (samplerConfig != null) {
				scannerBase.setSamplerConfiguration(samplerConfig);
			}
			scannerIterator = scannerBase.iterator();
			numKeysRead = 0;
		}

		@Override
		public void close() {
			if (null != (scannerBase)) {
				scannerBase.close();
			}
		}

		@Override
		public float getProgress() throws IOException {
			if (((numKeysRead) > 0) && ((currentKey) == null))
				return 1.0F;

			return split.getProgress(currentKey);
		}

		protected K currentK = null;

		protected V currentV = null;

		protected Key currentKey = null;

		@Override
		public K getCurrentKey() throws IOException, InterruptedException {
			return currentK;
		}

		@Override
		public V getCurrentValue() throws IOException, InterruptedException {
			return currentV;
		}
	}

	Map<String, Map<KeyExtent, List<Range>>> binOfflineTable(JobContext context, String tableId, List<Range> ranges) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Instance instance = AbstractInputFormat.getInstance(context);
		Connector conn = instance.getConnector(AbstractInputFormat.getPrincipal(context), AbstractInputFormat.getAuthenticationToken(context));
		return InputConfigurator.binOffline(tableId, ranges, instance, conn);
	}

	@Override
	public List<InputSplit> getSplits(JobContext context) throws IOException {
		Level logLevel = AbstractInputFormat.getLogLevel(context);
		AbstractInputFormat.log.setLevel(logLevel);
		AbstractInputFormat.validateOptions(context);
		Random random = new Random();
		LinkedList<InputSplit> splits = new LinkedList<>();
		Map<String, InputTableConfig> tableConfigs = AbstractInputFormat.getInputTableConfigs(context);
		for (Map.Entry<String, InputTableConfig> tableConfigEntry : tableConfigs.entrySet()) {
			String tableName = tableConfigEntry.getKey();
			InputTableConfig tableConfig = tableConfigEntry.getValue();
			Instance instance = AbstractInputFormat.getInstance(context);
			String tableId;
			if (DeprecationUtil.isMockInstance(instance)) {
				tableId = "";
			}else {
				try {
					tableId = Tables.getTableId(instance, tableName);
				} catch (TableNotFoundException e) {
					throw new IOException(e);
				}
			}
			Authorizations auths = AbstractInputFormat.getScanAuthorizations(context);
			String principal = AbstractInputFormat.getPrincipal(context);
			AuthenticationToken token = AbstractInputFormat.getAuthenticationToken(context);
			boolean batchScan = InputConfigurator.isBatchScan(AbstractInputFormat.CLASS, context.getConfiguration());
			boolean supportBatchScan = !(((tableConfig.isOfflineScan()) || (tableConfig.shouldUseIsolatedScanners())) || (tableConfig.shouldUseLocalIterators()));
			if (batchScan && (!supportBatchScan))
				throw new IllegalArgumentException(("BatchScanner optimization not available for offline" + " scan, isolated, or local iterators"));

			boolean autoAdjust = tableConfig.shouldAutoAdjustRanges();
			if (batchScan && (!autoAdjust))
				throw new IllegalArgumentException("AutoAdjustRanges must be enabled when using BatchScanner optimization");

			List<Range> ranges = (autoAdjust) ? Range.mergeOverlapping(tableConfig.getRanges()) : tableConfig.getRanges();
			if (ranges.isEmpty()) {
				ranges = new ArrayList<>(1);
				ranges.add(new Range());
			}
			Map<String, Map<KeyExtent, List<Range>>> binnedRanges = new HashMap<>();
			TabletLocator tl;
			try {
				if (tableConfig.isOfflineScan()) {
					binnedRanges = binOfflineTable(context, tableId, ranges);
					while (binnedRanges == null) {
						UtilWaitThread.sleepUninterruptibly((100 + (random.nextInt(100))), TimeUnit.MILLISECONDS);
						binnedRanges = binOfflineTable(context, tableId, ranges);
					} 
				}else {
					tl = InputConfigurator.getTabletLocator(AbstractInputFormat.CLASS, context.getConfiguration(), tableId);
					tl.invalidateCache();
					ClientContext clientContext = new ClientContext(AbstractInputFormat.getInstance(context), new org.apache.accumulo.core.client.impl.Credentials(AbstractInputFormat.getPrincipal(context), AbstractInputFormat.getAuthenticationToken(context)), AbstractInputFormat.getClientConfiguration(context));
					while (!(tl.binRanges(clientContext, ranges, binnedRanges).isEmpty())) {
						if (!(DeprecationUtil.isMockInstance(instance))) {
							if (!(Tables.exists(instance, tableId)))
								throw new TableDeletedException(tableId);

							if ((Tables.getTableState(instance, tableId)) == (TableState.OFFLINE))
								throw new TableOfflineException(instance, tableId);

						}
						binnedRanges.clear();
						AbstractInputFormat.log.warn("Unable to locate bins for specified ranges. Retrying.");
						UtilWaitThread.sleepUninterruptibly((100 + (random.nextInt(100))), TimeUnit.MILLISECONDS);
						tl.invalidateCache();
					} 
				}
			} catch (Exception e) {
				throw new IOException(e);
			}
			HashMap<Range, ArrayList<String>> splitsToAdd = null;
			if (!autoAdjust)
				splitsToAdd = new HashMap<>();

			HashMap<String, String> hostNameCache = new HashMap<>();
			for (Map.Entry<String, Map<KeyExtent, List<Range>>> tserverBin : binnedRanges.entrySet()) {
				String ip = tserverBin.getKey().split(":", 2)[0];
				String location = hostNameCache.get(ip);
				if (location == null) {
					InetAddress inetAddress = InetAddress.getByName(ip);
					location = inetAddress.getCanonicalHostName();
					hostNameCache.put(ip, location);
				}
				for (Map.Entry<KeyExtent, List<Range>> extentRanges : tserverBin.getValue().entrySet()) {
					Range ke = extentRanges.getKey().toDataRange();
					if (batchScan) {
						ArrayList<Range> clippedRanges = new ArrayList<>();
						for (Range r : extentRanges.getValue())
							clippedRanges.add(ke.clip(r));

						BatchInputSplit split = new BatchInputSplit(tableName, tableId, clippedRanges, new String[]{ location });
						SplitUtils.updateSplit(split, instance, tableConfig, principal, token, auths, logLevel);
						splits.add(split);
					}else {
						for (Range r : extentRanges.getValue()) {
							if (autoAdjust) {
							}else {
								ArrayList<String> locations = splitsToAdd.get(r);
								if (locations == null)
									locations = new ArrayList<>(1);

								locations.add(location);
								splitsToAdd.put(r, locations);
							}
						}
					}
				}
			}
			if (!autoAdjust)
				for (Map.Entry<Range, ArrayList<String>> entry : splitsToAdd.entrySet()) {
				}

		}
		return splits;
	}
}

