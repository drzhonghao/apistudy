

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
import org.apache.accumulo.core.client.mapred.AccumuloInputFormat;
import org.apache.accumulo.core.client.mapred.RangeInputSplit;
import org.apache.accumulo.core.client.mapred.impl.BatchInputSplit;
import org.apache.accumulo.core.client.mapreduce.InputTableConfig;
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
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public abstract class AbstractInputFormat<K, V> implements InputFormat<K, V> {
	protected static final Class<?> CLASS = AccumuloInputFormat.class;

	protected static final Logger log = Logger.getLogger(AbstractInputFormat.CLASS);

	public static void setClassLoaderContext(JobConf job, String context) {
		InputConfigurator.setClassLoaderContext(AbstractInputFormat.CLASS, job, context);
	}

	public static String getClassLoaderContext(JobConf job) {
		return InputConfigurator.getClassLoaderContext(AbstractInputFormat.CLASS, job);
	}

	public static void setConnectorInfo(JobConf job, String principal, AuthenticationToken token) throws AccumuloSecurityException {
		if (token instanceof KerberosToken) {
			AbstractInputFormat.log.info("Received KerberosToken, attempting to fetch DelegationToken");
			try {
				Instance instance = AbstractInputFormat.getInstance(job);
				Connector conn = instance.getConnector(principal, token);
				token = conn.securityOperations().getDelegationToken(new DelegationTokenConfig());
			} catch (Exception e) {
				AbstractInputFormat.log.warn(("Failed to automatically obtain DelegationToken, Mappers/Reducers will likely" + " fail to communicate with Accumulo"), e);
			}
		}
		if (token instanceof DelegationTokenImpl) {
			DelegationTokenImpl delegationToken = ((DelegationTokenImpl) (token));
			AuthenticationTokenIdentifier identifier = delegationToken.getIdentifier();
			Token<AuthenticationTokenIdentifier> hadoopToken = new Token<>(identifier.getBytes(), delegationToken.getPassword(), identifier.getKind(), delegationToken.getServiceName());
			job.getCredentials().addToken(hadoopToken.getService(), hadoopToken);
		}
		InputConfigurator.setConnectorInfo(AbstractInputFormat.CLASS, job, principal, token);
	}

	public static void setConnectorInfo(JobConf job, String principal, String tokenFile) throws AccumuloSecurityException {
		InputConfigurator.setConnectorInfo(AbstractInputFormat.CLASS, job, principal, tokenFile);
	}

	protected static Boolean isConnectorInfoSet(JobConf job) {
		return InputConfigurator.isConnectorInfoSet(AbstractInputFormat.CLASS, job);
	}

	protected static String getPrincipal(JobConf job) {
		return InputConfigurator.getPrincipal(AbstractInputFormat.CLASS, job);
	}

	protected static AuthenticationToken getAuthenticationToken(JobConf job) {
		AuthenticationToken token = InputConfigurator.getAuthenticationToken(AbstractInputFormat.CLASS, job);
		return ConfiguratorBase.unwrapAuthenticationToken(job, token);
	}

	@Deprecated
	public static void setZooKeeperInstance(JobConf job, String instanceName, String zooKeepers) {
		AbstractInputFormat.setZooKeeperInstance(job, new ClientConfiguration().withInstance(instanceName).withZkHosts(zooKeepers));
	}

	public static void setZooKeeperInstance(JobConf job, ClientConfiguration clientConfig) {
		InputConfigurator.setZooKeeperInstance(AbstractInputFormat.CLASS, job, clientConfig);
	}

	@Deprecated
	public static void setMockInstance(JobConf job, String instanceName) {
		InputConfigurator.setMockInstance(AbstractInputFormat.CLASS, job, instanceName);
	}

	protected static Instance getInstance(JobConf job) {
		return InputConfigurator.getInstance(AbstractInputFormat.CLASS, job);
	}

	public static void setLogLevel(JobConf job, Level level) {
		InputConfigurator.setLogLevel(AbstractInputFormat.CLASS, job, level);
	}

	protected static Level getLogLevel(JobConf job) {
		return InputConfigurator.getLogLevel(AbstractInputFormat.CLASS, job);
	}

	public static void setScanAuthorizations(JobConf job, Authorizations auths) {
		InputConfigurator.setScanAuthorizations(AbstractInputFormat.CLASS, job, auths);
	}

	protected static Authorizations getScanAuthorizations(JobConf job) {
		return InputConfigurator.getScanAuthorizations(AbstractInputFormat.CLASS, job);
	}

	@Deprecated
	protected static TabletLocator getTabletLocator(JobConf job, String tableId) throws TableNotFoundException {
		return InputConfigurator.getTabletLocator(AbstractInputFormat.CLASS, job, tableId);
	}

	protected static ClientConfiguration getClientConfiguration(JobConf job) {
		return InputConfigurator.getClientConfiguration(AbstractInputFormat.CLASS, job);
	}

	protected static void validateOptions(JobConf job) throws IOException {
		final Instance inst = InputConfigurator.validateInstance(AbstractInputFormat.CLASS, job);
		String principal = InputConfigurator.getPrincipal(AbstractInputFormat.CLASS, job);
		AuthenticationToken token = InputConfigurator.getAuthenticationToken(AbstractInputFormat.CLASS, job);
		token = ConfiguratorBase.unwrapAuthenticationToken(job, token);
		Connector conn;
		try {
			conn = inst.getConnector(principal, token);
		} catch (Exception e) {
			throw new IOException(e);
		}
		InputConfigurator.validatePermissions(AbstractInputFormat.CLASS, job, conn);
	}

	public static Map<String, InputTableConfig> getInputTableConfigs(JobConf job) {
		return InputConfigurator.getInputTableConfigs(AbstractInputFormat.CLASS, job);
	}

	public static InputTableConfig getInputTableConfig(JobConf job, String tableName) {
		return InputConfigurator.getInputTableConfig(AbstractInputFormat.CLASS, job, tableName);
	}

	protected static abstract class AbstractRecordReader<K, V> implements RecordReader<K, V> {
		protected long numKeysRead;

		protected Iterator<Map.Entry<Key, Value>> scannerIterator;

		protected RangeInputSplit split;

		private org.apache.accumulo.core.client.mapreduce.RangeInputSplit baseSplit;

		protected ScannerBase scannerBase;

		protected abstract List<IteratorSetting> jobIterators(JobConf job, String tableName);

		private void setupIterators(JobConf job, ScannerBase scanner, String tableName, org.apache.accumulo.core.client.mapreduce.RangeInputSplit split) {
			List<IteratorSetting> iterators = null;
			if (null == split) {
				iterators = jobIterators(job, tableName);
			}else {
				iterators = split.getIterators();
				if (null == iterators) {
					iterators = jobIterators(job, tableName);
				}
			}
			for (IteratorSetting iterator : iterators)
				scanner.addScanIterator(iterator);

		}

		@Deprecated
		protected void setupIterators(JobConf job, Scanner scanner, String tableName, RangeInputSplit split) {
			setupIterators(job, ((ScannerBase) (scanner)), tableName, split);
		}

		public void initialize(InputSplit inSplit, JobConf job) throws IOException {
			baseSplit = ((org.apache.accumulo.core.client.mapreduce.RangeInputSplit) (inSplit));
			AbstractInputFormat.log.debug(("Initializing input split: " + (baseSplit.toString())));
			Instance instance = baseSplit.getInstance(AbstractInputFormat.getClientConfiguration(job));
			if (null == instance) {
				instance = AbstractInputFormat.getInstance(job);
			}
			String principal = baseSplit.getPrincipal();
			if (null == principal) {
				principal = AbstractInputFormat.getPrincipal(job);
			}
			AuthenticationToken token = baseSplit.getToken();
			if (null == token) {
				token = AbstractInputFormat.getAuthenticationToken(job);
			}
			Authorizations authorizations = baseSplit.getAuths();
			if (null == authorizations) {
				authorizations = AbstractInputFormat.getScanAuthorizations(job);
			}
			String classLoaderContext = AbstractInputFormat.getClassLoaderContext(job);
			String table = baseSplit.getTableName();
			InputTableConfig tableConfig = AbstractInputFormat.getInputTableConfig(job, baseSplit.getTableName());
			AbstractInputFormat.log.debug(("Creating connector with user: " + principal));
			AbstractInputFormat.log.debug(("Creating scanner for table: " + table));
			AbstractInputFormat.log.debug(("Authorizations are: " + authorizations));
			if ((baseSplit) instanceof BatchInputSplit) {
				BatchScanner scanner;
				BatchInputSplit multiRangeSplit = ((BatchInputSplit) (baseSplit));
				try {
					int scanThreads = 1;
					scanner = instance.getConnector(principal, token).createBatchScanner(baseSplit.getTableName(), authorizations, scanThreads);
					setupIterators(job, scanner, baseSplit.getTableName(), baseSplit);
					if (null != classLoaderContext) {
						scanner.setClassLoaderContext(classLoaderContext);
					}
				} catch (Exception e) {
					throw new IOException(e);
				}
				scanner.setRanges(multiRangeSplit.getRanges());
				scannerBase = scanner;
			}else
				if ((baseSplit) instanceof RangeInputSplit) {
					split = ((RangeInputSplit) (baseSplit));
					Boolean isOffline = baseSplit.isOffline();
					if (null == isOffline) {
						isOffline = tableConfig.isOfflineScan();
					}
					Boolean isIsolated = baseSplit.isIsolatedScan();
					if (null == isIsolated) {
						isIsolated = tableConfig.shouldUseIsolatedScanners();
					}
					Boolean usesLocalIterators = baseSplit.usesLocalIterators();
					if (null == usesLocalIterators) {
						usesLocalIterators = tableConfig.shouldUseLocalIterators();
					}
					Scanner scanner;
					try {
						if (isOffline) {
							scanner = new OfflineScanner(instance, new org.apache.accumulo.core.client.impl.Credentials(principal, token), baseSplit.getTableId(), authorizations);
						}else
							if (DeprecationUtil.isMockInstance(instance)) {
								scanner = instance.getConnector(principal, token).createScanner(baseSplit.getTableName(), authorizations);
							}else {
								ClientConfiguration clientConf = AbstractInputFormat.getClientConfiguration(job);
								ClientContext context = new ClientContext(instance, new org.apache.accumulo.core.client.impl.Credentials(principal, token), clientConf);
								scanner = new ScannerImpl(context, baseSplit.getTableId(), authorizations);
							}

						if (isIsolated) {
							AbstractInputFormat.log.info("Creating isolated scanner");
							scanner = new IsolatedScanner(scanner);
						}
						if (usesLocalIterators) {
							AbstractInputFormat.log.info("Using local iterators");
							scanner = new ClientSideIteratorScanner(scanner);
						}
						setupIterators(job, scanner, baseSplit.getTableName(), baseSplit);
					} catch (Exception e) {
						throw new IOException(e);
					}
					scanner.setRange(baseSplit.getRange());
					scannerBase = scanner;
				}else {
					throw new IllegalArgumentException(("Can not initialize from " + (baseSplit.getClass().toString())));
				}

			Collection<Pair<Text, Text>> columns = baseSplit.getFetchedColumns();
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
			SamplerConfiguration samplerConfig = baseSplit.getSamplerConfiguration();
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
		public long getPos() throws IOException {
			return numKeysRead;
		}

		@Override
		public float getProgress() throws IOException {
			if (((numKeysRead) > 0) && ((currentKey) == null))
				return 1.0F;

			return baseSplit.getProgress(currentKey);
		}

		protected Key currentKey = null;
	}

	Map<String, Map<KeyExtent, List<Range>>> binOfflineTable(JobConf job, String tableId, List<Range> ranges) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		Instance instance = AbstractInputFormat.getInstance(job);
		Connector conn = instance.getConnector(AbstractInputFormat.getPrincipal(job), AbstractInputFormat.getAuthenticationToken(job));
		return InputConfigurator.binOffline(tableId, ranges, instance, conn);
	}

	@Override
	public InputSplit[] getSplits(JobConf job, int numSplits) throws IOException {
		Level logLevel = AbstractInputFormat.getLogLevel(job);
		AbstractInputFormat.log.setLevel(logLevel);
		AbstractInputFormat.validateOptions(job);
		Random random = new Random();
		LinkedList<InputSplit> splits = new LinkedList<>();
		Map<String, InputTableConfig> tableConfigs = AbstractInputFormat.getInputTableConfigs(job);
		for (Map.Entry<String, InputTableConfig> tableConfigEntry : tableConfigs.entrySet()) {
			String tableName = tableConfigEntry.getKey();
			InputTableConfig tableConfig = tableConfigEntry.getValue();
			Instance instance = AbstractInputFormat.getInstance(job);
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
			Authorizations auths = AbstractInputFormat.getScanAuthorizations(job);
			String principal = AbstractInputFormat.getPrincipal(job);
			AuthenticationToken token = AbstractInputFormat.getAuthenticationToken(job);
			boolean batchScan = InputConfigurator.isBatchScan(AbstractInputFormat.CLASS, job);
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
					binnedRanges = binOfflineTable(job, tableId, ranges);
					while (binnedRanges == null) {
						UtilWaitThread.sleepUninterruptibly((100 + (random.nextInt(100))), TimeUnit.MILLISECONDS);
						binnedRanges = binOfflineTable(job, tableId, ranges);
					} 
				}else {
					tl = InputConfigurator.getTabletLocator(AbstractInputFormat.CLASS, job, tableId);
					tl.invalidateCache();
					ClientContext context = new ClientContext(AbstractInputFormat.getInstance(job), new org.apache.accumulo.core.client.impl.Credentials(AbstractInputFormat.getPrincipal(job), AbstractInputFormat.getAuthenticationToken(job)), AbstractInputFormat.getClientConfiguration(job));
					while (!(tl.binRanges(context, ranges, binnedRanges).isEmpty())) {
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
		return splits.toArray(new InputSplit[splits.size()]);
	}
}

