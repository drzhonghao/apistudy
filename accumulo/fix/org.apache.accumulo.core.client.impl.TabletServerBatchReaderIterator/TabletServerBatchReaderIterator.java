

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.SampleNotPresentException;
import org.apache.accumulo.core.client.TableDeletedException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.TableOfflineException;
import org.apache.accumulo.core.client.TimedOutException;
import org.apache.accumulo.core.client.impl.AccumuloServerException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.ScannerOptions;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.impl.TabletLocator;
import org.apache.accumulo.core.client.impl.ThriftTransportPool;
import org.apache.accumulo.core.client.impl.TimeoutTabletLocator;
import org.apache.accumulo.core.client.impl.Translator;
import org.apache.accumulo.core.client.impl.Translators;
import org.apache.accumulo.core.data.Column;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.data.thrift.MultiScanResult;
import org.apache.accumulo.core.data.thrift.TKey;
import org.apache.accumulo.core.data.thrift.TKeyExtent;
import org.apache.accumulo.core.data.thrift.TRange;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.rpc.ThriftUtil;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.tabletserver.thrift.TabletClientService;
import org.apache.accumulo.core.util.HostAndPort;
import org.apache.htrace.wrappers.TraceRunnable;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TabletServerBatchReaderIterator implements Iterator<Map.Entry<Key, Value>> {
	private static final Logger log = LoggerFactory.getLogger(TabletServerBatchReaderIterator.class);

	private final ClientContext context;

	private final Instance instance;

	private final String tableId;

	private Authorizations authorizations = Authorizations.EMPTY;

	private final int numThreads;

	private final ExecutorService queryThreadPool;

	private final ScannerOptions options;

	private ArrayBlockingQueue<List<Map.Entry<Key, Value>>> resultsQueue;

	private Iterator<Map.Entry<Key, Value>> batchIterator;

	private List<Map.Entry<Key, Value>> batch;

	private static final List<Map.Entry<Key, Value>> LAST_BATCH = new ArrayList<>();

	private final Object nextLock = new Object();

	private long failSleepTime = 100;

	private volatile Throwable fatalException = null;

	private Map<String, TabletServerBatchReaderIterator.TimeoutTracker> timeoutTrackers;

	private Set<String> timedoutServers;

	private final long timeout;

	private TabletLocator locator;

	public interface ResultReceiver {
		void receive(List<Map.Entry<Key, Value>> entries);
	}

	public TabletServerBatchReaderIterator(ClientContext context, String tableId, Authorizations authorizations, ArrayList<Range> ranges, int numThreads, ExecutorService queryThreadPool, ScannerOptions scannerOptions, long timeout) {
		this.context = context;
		this.instance = context.getInstance();
		this.tableId = tableId;
		this.authorizations = authorizations;
		this.numThreads = numThreads;
		this.queryThreadPool = queryThreadPool;
		this.options = new ScannerOptions(scannerOptions);
		resultsQueue = new ArrayBlockingQueue<>(numThreads);
		this.locator = new TimeoutTabletLocator(timeout, context, tableId);
		timeoutTrackers = Collections.synchronizedMap(new HashMap<String, TabletServerBatchReaderIterator.TimeoutTracker>());
		timedoutServers = Collections.synchronizedSet(new HashSet<String>());
		this.timeout = timeout;
		TabletServerBatchReaderIterator.ResultReceiver rr = new TabletServerBatchReaderIterator.ResultReceiver() {
			@Override
			public void receive(List<Map.Entry<Key, Value>> entries) {
				try {
					resultsQueue.put(entries);
				} catch (InterruptedException e) {
					if (TabletServerBatchReaderIterator.this.queryThreadPool.isShutdown())
						TabletServerBatchReaderIterator.log.debug("Failed to add Batch Scan result", e);
					else
						TabletServerBatchReaderIterator.log.warn("Failed to add Batch Scan result", e);

					fatalException = e;
					throw new RuntimeException(e);
				}
			}
		};
		try {
			lookup(ranges, rr);
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create iterator", e);
		}
	}

	@Override
	public boolean hasNext() {
		synchronized(nextLock) {
			if ((batch) == (TabletServerBatchReaderIterator.LAST_BATCH))
				return false;

			if (((batch) != null) && (batchIterator.hasNext()))
				return true;

			try {
				batch = null;
				while ((((batch) == null) && ((fatalException) == null)) && (!(queryThreadPool.isShutdown())))
					batch = resultsQueue.poll(1, TimeUnit.SECONDS);

				if ((fatalException) != null)
					if ((fatalException) instanceof RuntimeException)
						throw ((RuntimeException) (fatalException));
					else
						throw new RuntimeException(fatalException);


				if (queryThreadPool.isShutdown()) {
					String shortMsg = "The BatchScanner was unexpectedly closed while this Iterator " + "was still in use.";
					TabletServerBatchReaderIterator.log.error((((shortMsg + " Ensure that a reference to the BatchScanner is retained so that it can be closed") + " when this Iterator is exhausted. Not retaining a reference to the BatchScanner") + " guarantees that you are leaking threads in your client JVM."));
					throw new RuntimeException((shortMsg + " Ensure proper handling of the BatchScanner."));
				}
				batchIterator = batch.iterator();
				return (batch) != (TabletServerBatchReaderIterator.LAST_BATCH);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public Map.Entry<Key, Value> next() {
		synchronized(nextLock) {
			if (hasNext())
				return batchIterator.next();
			else
				throw new NoSuchElementException();

		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private synchronized void lookup(List<Range> ranges, TabletServerBatchReaderIterator.ResultReceiver receiver) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		ranges = Range.mergeOverlapping(ranges);
		Map<String, Map<KeyExtent, List<Range>>> binnedRanges = new HashMap<>();
		binRanges(locator, ranges, binnedRanges);
	}

	private void binRanges(TabletLocator tabletLocator, List<Range> ranges, Map<String, Map<KeyExtent, List<Range>>> binnedRanges) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		int lastFailureSize = Integer.MAX_VALUE;
		while (true) {
			binnedRanges.clear();
			List<Range> failures = tabletLocator.binRanges(context, ranges, binnedRanges);
			if ((failures.size()) > 0) {
				if ((failures.size()) >= lastFailureSize)
					if (!(Tables.exists(instance, tableId)))
						throw new TableDeletedException(tableId);
					else
						if ((Tables.getTableState(instance, tableId)) == (TableState.OFFLINE))
							throw new TableOfflineException(instance, tableId);



				lastFailureSize = failures.size();
				if (TabletServerBatchReaderIterator.log.isTraceEnabled())
					TabletServerBatchReaderIterator.log.trace("Failed to bin {} ranges, tablet locations were null, retrying in 100ms", failures.size());

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}else {
				break;
			}
		} 
		Map<String, Map<KeyExtent, List<Range>>> binnedRanges2 = new HashMap<>();
		for (Map.Entry<String, Map<KeyExtent, List<Range>>> entry : binnedRanges.entrySet()) {
			Map<KeyExtent, List<Range>> tabletMap = new HashMap<>();
			binnedRanges2.put(entry.getKey(), tabletMap);
			for (Map.Entry<KeyExtent, List<Range>> tabletRanges : entry.getValue().entrySet()) {
				Range tabletRange = tabletRanges.getKey().toDataRange();
				List<Range> clippedRanges = new ArrayList<>();
				tabletMap.put(tabletRanges.getKey(), clippedRanges);
				for (Range range : tabletRanges.getValue())
					clippedRanges.add(tabletRange.clip(range));

			}
		}
		binnedRanges.clear();
		binnedRanges.putAll(binnedRanges2);
	}

	private void processFailures(Map<KeyExtent, List<Range>> failures, TabletServerBatchReaderIterator.ResultReceiver receiver, List<Column> columns) throws AccumuloException, AccumuloSecurityException, TableNotFoundException {
		if (TabletServerBatchReaderIterator.log.isTraceEnabled())
			TabletServerBatchReaderIterator.log.trace("Failed to execute multiscans against {} tablets, retrying...", failures.size());

		try {
			Thread.sleep(failSleepTime);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			TabletServerBatchReaderIterator.log.debug("Exiting failure processing on interrupt");
			return;
		}
		failSleepTime = Math.min(5000, ((failSleepTime) * 2));
		Map<String, Map<KeyExtent, List<Range>>> binnedRanges = new HashMap<>();
		List<Range> allRanges = new ArrayList<>();
		for (List<Range> ranges : failures.values())
			allRanges.addAll(ranges);

		binRanges(locator, allRanges, binnedRanges);
		doLookups(binnedRanges, receiver, columns);
	}

	private String getTableInfo() {
		return Tables.getPrintableTableInfoFromId(instance, tableId);
	}

	private class QueryTask implements Runnable {
		private String tsLocation;

		private Map<KeyExtent, List<Range>> tabletsRanges;

		private TabletServerBatchReaderIterator.ResultReceiver receiver;

		private Semaphore semaphore = null;

		private final Map<KeyExtent, List<Range>> failures;

		private List<Column> columns;

		private int semaphoreSize;

		QueryTask(String tsLocation, Map<KeyExtent, List<Range>> tabletsRanges, Map<KeyExtent, List<Range>> failures, TabletServerBatchReaderIterator.ResultReceiver receiver, List<Column> columns) {
			this.tsLocation = tsLocation;
			this.tabletsRanges = tabletsRanges;
			this.receiver = receiver;
			this.columns = columns;
			this.failures = failures;
		}

		void setSemaphore(Semaphore semaphore, int semaphoreSize) {
			this.semaphore = semaphore;
			this.semaphoreSize = semaphoreSize;
		}

		@Override
		public void run() {
			String threadName = Thread.currentThread().getName();
			Thread.currentThread().setName(((((threadName + " looking up ") + (tabletsRanges.size())) + " ranges at ") + (tsLocation)));
			Map<KeyExtent, List<Range>> unscanned = new HashMap<>();
			Map<KeyExtent, List<Range>> tsFailures = new HashMap<>();
			try {
				TabletServerBatchReaderIterator.TimeoutTracker timeoutTracker = timeoutTrackers.get(tsLocation);
				if (timeoutTracker == null) {
					timeoutTracker = new TabletServerBatchReaderIterator.TimeoutTracker(tsLocation, timedoutServers, timeout);
					timeoutTrackers.put(tsLocation, timeoutTracker);
				}
				TabletServerBatchReaderIterator.doLookup(context, tsLocation, tabletsRanges, tsFailures, unscanned, receiver, columns, options, authorizations, timeoutTracker);
				if ((tsFailures.size()) > 0) {
					locator.invalidateCache(tsFailures.keySet());
					synchronized(failures) {
						failures.putAll(tsFailures);
					}
				}
			} catch (IOException e) {
				if (!(TabletServerBatchReaderIterator.this.queryThreadPool.isShutdown())) {
					synchronized(failures) {
						failures.putAll(tsFailures);
						failures.putAll(unscanned);
					}
					locator.invalidateCache(context.getInstance(), tsLocation);
				}
				TabletServerBatchReaderIterator.log.debug("IOException thrown", e);
			} catch (AccumuloSecurityException e) {
				e.setTableInfo(getTableInfo());
				TabletServerBatchReaderIterator.log.debug("AccumuloSecurityException thrown", e);
				Tables.clearCache(instance);
				if (!(Tables.exists(instance, tableId)))
					fatalException = new TableDeletedException(tableId);
				else
					fatalException = e;

			} catch (SampleNotPresentException e) {
				fatalException = e;
			} catch (Throwable t) {
				if (queryThreadPool.isShutdown())
					TabletServerBatchReaderIterator.log.debug("Caught exception, but queryThreadPool is shutdown", t);
				else
					TabletServerBatchReaderIterator.log.warn("Caught exception, but queryThreadPool is not shutdown", t);

				fatalException = t;
			} finally {
				semaphore.release();
				Thread.currentThread().setName(threadName);
				if (semaphore.tryAcquire(semaphoreSize)) {
					if (((fatalException) == null) && ((failures.size()) > 0)) {
						try {
							processFailures(failures, receiver, columns);
						} catch (TableNotFoundException e) {
							TabletServerBatchReaderIterator.log.debug("{}", e.getMessage(), e);
							fatalException = e;
						} catch (AccumuloException e) {
							TabletServerBatchReaderIterator.log.debug("{}", e.getMessage(), e);
							fatalException = e;
						} catch (AccumuloSecurityException e) {
							e.setTableInfo(getTableInfo());
							TabletServerBatchReaderIterator.log.debug("{}", e.getMessage(), e);
							fatalException = e;
						} catch (Throwable t) {
							TabletServerBatchReaderIterator.log.debug("{}", t.getMessage(), t);
							fatalException = t;
						}
						if ((fatalException) != null) {
							if (!(resultsQueue.offer(TabletServerBatchReaderIterator.LAST_BATCH))) {
								TabletServerBatchReaderIterator.log.debug("Could not add to result queue after seeing fatalException in processFailures", fatalException);
							}
						}
					}else {
						if ((fatalException) != null) {
							if (!(resultsQueue.offer(TabletServerBatchReaderIterator.LAST_BATCH))) {
								TabletServerBatchReaderIterator.log.debug("Could not add to result queue after seeing fatalException", fatalException);
							}
						}else {
							try {
								resultsQueue.put(TabletServerBatchReaderIterator.LAST_BATCH);
							} catch (InterruptedException e) {
								fatalException = e;
								if (!(resultsQueue.offer(TabletServerBatchReaderIterator.LAST_BATCH))) {
									TabletServerBatchReaderIterator.log.debug("Could not add to result queue after seeing fatalException", fatalException);
								}
							}
						}
					}
				}
			}
		}
	}

	private void doLookups(Map<String, Map<KeyExtent, List<Range>>> binnedRanges, final TabletServerBatchReaderIterator.ResultReceiver receiver, List<Column> columns) {
		if (timedoutServers.containsAll(binnedRanges.keySet())) {
			throw new TimedOutException(timedoutServers);
		}
		int maxTabletsPerRequest = Integer.MAX_VALUE;
		if (((numThreads) / (binnedRanges.size())) > 1) {
			int totalNumberOfTablets = 0;
			for (Map.Entry<String, Map<KeyExtent, List<Range>>> entry : binnedRanges.entrySet()) {
				totalNumberOfTablets += entry.getValue().size();
			}
			maxTabletsPerRequest = totalNumberOfTablets / (numThreads);
			if (maxTabletsPerRequest == 0) {
				maxTabletsPerRequest = 1;
			}
		}
		Map<KeyExtent, List<Range>> failures = new HashMap<>();
		if ((timedoutServers.size()) > 0) {
			for (Iterator<Map.Entry<String, Map<KeyExtent, List<Range>>>> iterator = binnedRanges.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry<String, Map<KeyExtent, List<Range>>> entry = iterator.next();
				if (timedoutServers.contains(entry.getKey())) {
					failures.putAll(entry.getValue());
					iterator.remove();
				}
			}
		}
		List<String> locations = new ArrayList<>(binnedRanges.keySet());
		Collections.shuffle(locations);
		List<TabletServerBatchReaderIterator.QueryTask> queryTasks = new ArrayList<>();
		for (final String tsLocation : locations) {
			final Map<KeyExtent, List<Range>> tabletsRanges = binnedRanges.get(tsLocation);
			if ((maxTabletsPerRequest == (Integer.MAX_VALUE)) || ((tabletsRanges.size()) == 1)) {
				TabletServerBatchReaderIterator.QueryTask queryTask = new TabletServerBatchReaderIterator.QueryTask(tsLocation, tabletsRanges, failures, receiver, columns);
				queryTasks.add(queryTask);
			}else {
				HashMap<KeyExtent, List<Range>> tabletSubset = new HashMap<>();
				for (Map.Entry<KeyExtent, List<Range>> entry : tabletsRanges.entrySet()) {
					tabletSubset.put(entry.getKey(), entry.getValue());
					if ((tabletSubset.size()) >= maxTabletsPerRequest) {
						TabletServerBatchReaderIterator.QueryTask queryTask = new TabletServerBatchReaderIterator.QueryTask(tsLocation, tabletSubset, failures, receiver, columns);
						queryTasks.add(queryTask);
						tabletSubset = new HashMap<>();
					}
				}
				if ((tabletSubset.size()) > 0) {
					TabletServerBatchReaderIterator.QueryTask queryTask = new TabletServerBatchReaderIterator.QueryTask(tsLocation, tabletSubset, failures, receiver, columns);
					queryTasks.add(queryTask);
				}
			}
		}
		final Semaphore semaphore = new Semaphore(queryTasks.size());
		semaphore.acquireUninterruptibly(queryTasks.size());
		for (TabletServerBatchReaderIterator.QueryTask queryTask : queryTasks) {
			queryTask.setSemaphore(semaphore, queryTasks.size());
			queryThreadPool.execute(new TraceRunnable(queryTask));
		}
	}

	static void trackScanning(Map<KeyExtent, List<Range>> failures, Map<KeyExtent, List<Range>> unscanned, MultiScanResult scanResult) {
		Map<KeyExtent, List<Range>> retFailures = Translator.translate(scanResult.failures, Translators.TKET, new Translator.ListTranslator<>(Translators.TRT));
		unscanned.keySet().removeAll(retFailures.keySet());
		failures.putAll(retFailures);
		HashSet<KeyExtent> fullScans = new HashSet<>(Translator.translate(scanResult.fullScans, Translators.TKET));
		unscanned.keySet().removeAll(fullScans);
		if ((scanResult.partScan) != null) {
			KeyExtent ke = new KeyExtent(scanResult.partScan);
			Key nextKey = new Key(scanResult.partNextKey);
			ListIterator<Range> iterator = unscanned.get(ke).listIterator();
			while (iterator.hasNext()) {
				Range range = iterator.next();
				if ((range.afterEndKey(nextKey)) || ((nextKey.equals(range.getEndKey())) && ((scanResult.partNextKeyInclusive) != (range.isEndKeyInclusive())))) {
					iterator.remove();
				}else
					if (range.contains(nextKey)) {
						iterator.remove();
						Range partRange = new Range(nextKey, scanResult.partNextKeyInclusive, range.getEndKey(), range.isEndKeyInclusive());
						iterator.add(partRange);
					}

			} 
		}
	}

	private static class TimeoutTracker {
		String server;

		Set<String> badServers;

		long timeOut;

		long activityTime;

		Long firstErrorTime = null;

		TimeoutTracker(String server, Set<String> badServers, long timeOut) {
			this(timeOut);
			this.server = server;
			this.badServers = badServers;
		}

		TimeoutTracker(long timeOut) {
			this.timeOut = timeOut;
		}

		void startingScan() {
			activityTime = System.currentTimeMillis();
		}

		void check() throws IOException {
			if (((System.currentTimeMillis()) - (activityTime)) > (timeOut)) {
				badServers.add(server);
				throw new IOException(((("Time exceeded " + ((System.currentTimeMillis()) - (activityTime))) + " ") + (server)));
			}
		}

		void madeProgress() {
			activityTime = System.currentTimeMillis();
			firstErrorTime = null;
		}

		void errorOccured(Exception e) {
			if ((firstErrorTime) == null) {
				firstErrorTime = activityTime;
			}else
				if (((System.currentTimeMillis()) - (firstErrorTime)) > (timeOut)) {
					badServers.add(server);
				}

		}

		public long getTimeOut() {
			return timeOut;
		}
	}

	public static void doLookup(ClientContext context, String server, Map<KeyExtent, List<Range>> requested, Map<KeyExtent, List<Range>> failures, Map<KeyExtent, List<Range>> unscanned, TabletServerBatchReaderIterator.ResultReceiver receiver, List<Column> columns, ScannerOptions options, Authorizations authorizations) throws IOException, AccumuloSecurityException, AccumuloServerException {
		TabletServerBatchReaderIterator.doLookup(context, server, requested, failures, unscanned, receiver, columns, options, authorizations, new TabletServerBatchReaderIterator.TimeoutTracker(Long.MAX_VALUE));
	}

	static void doLookup(ClientContext context, String server, Map<KeyExtent, List<Range>> requested, Map<KeyExtent, List<Range>> failures, Map<KeyExtent, List<Range>> unscanned, TabletServerBatchReaderIterator.ResultReceiver receiver, List<Column> columns, ScannerOptions options, Authorizations authorizations, TabletServerBatchReaderIterator.TimeoutTracker timeoutTracker) throws IOException, AccumuloSecurityException, AccumuloServerException {
		if ((requested.size()) == 0) {
			return;
		}
		for (Map.Entry<KeyExtent, List<Range>> entry : requested.entrySet()) {
			ArrayList<Range> ranges = new ArrayList<>();
			for (Range range : entry.getValue()) {
				ranges.add(new Range(range));
			}
			unscanned.put(new KeyExtent(entry.getKey()), ranges);
		}
		timeoutTracker.startingScan();
		TTransport transport = null;
		try {
			final HostAndPort parsedServer = HostAndPort.fromString(server);
			final TabletClientService.Client client;
			if ((timeoutTracker.getTimeOut()) < (context.getClientTimeoutInMillis()))
				client = ThriftUtil.getTServerClient(parsedServer, context, timeoutTracker.getTimeOut());
			else
				client = ThriftUtil.getTServerClient(parsedServer, context);

		} catch (TTransportException e) {
			TabletServerBatchReaderIterator.log.debug("Server : {} msg : {}", server, e.getMessage());
			timeoutTracker.errorOccured(e);
			throw new IOException(e);
		} catch (TException e) {
			TabletServerBatchReaderIterator.log.debug("Server : {} msg : {}", server, e.getMessage());
			timeoutTracker.errorOccured(e);
			throw new IOException(e);
		} finally {
			ThriftTransportPool.getInstance().returnTransport(transport);
		}
	}

	static int sumSizes(Collection<List<Range>> values) {
		int sum = 0;
		for (List<Range> list : values) {
			sum += list.size();
		}
		return sum;
	}
}

