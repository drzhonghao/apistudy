

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterators;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.BatchWriter;
import org.apache.accumulo.core.client.BatchWriterConfig;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.MutationsRejectedException;
import org.apache.accumulo.core.client.RowIterator;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.data.ColumnUpdate;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.PartialKey;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.master.state.tables.TableState;
import org.apache.accumulo.core.master.thrift.MasterState;
import org.apache.accumulo.core.master.thrift.TabletServerStatus;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.RootTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.ColumnFQ;
import org.apache.accumulo.core.util.Daemon;
import org.apache.accumulo.fate.util.UtilWaitThread;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.state.MergeStats;
import org.apache.accumulo.master.state.TableCounts;
import org.apache.accumulo.master.state.TableStats;
import org.apache.accumulo.server.AccumuloServerContext;
import org.apache.accumulo.server.ServerConstants;
import org.apache.accumulo.server.conf.ServerConfigurationFactory;
import org.apache.accumulo.server.conf.TableConfiguration;
import org.apache.accumulo.server.fs.FileRef;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.log.WalStateManager;
import org.apache.accumulo.server.master.LiveTServerSet;
import org.apache.accumulo.server.master.state.Assignment;
import org.apache.accumulo.server.master.state.ClosableIterator;
import org.apache.accumulo.server.master.state.DistributedStoreException;
import org.apache.accumulo.server.master.state.MergeInfo;
import org.apache.accumulo.server.master.state.MergeState;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.master.state.TabletLocationState;
import org.apache.accumulo.server.master.state.TabletState;
import org.apache.accumulo.server.master.state.TabletStateStore;
import org.apache.accumulo.server.tables.TableManager;
import org.apache.accumulo.server.tablets.TabletTime;
import org.apache.accumulo.server.util.MetadataTableUtil;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.thrift.TException;

import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ChoppedColumnFamily.CHOPPED_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.CurrentLocationColumnFamily.NAME;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.DIRECTORY_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.ServerColumnFamily.TIME_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.TabletColumnFamily.PREV_ROW_COLUMN;


abstract class TabletGroupWatcher extends Daemon {
	private static final String ASSIGNMENT_BUFFER_SEPARATOR = ", ";

	private static final int ASSINGMENT_BUFFER_MAX_LENGTH = 4096;

	private final Master master;

	final TabletStateStore store;

	final TabletGroupWatcher dependentWatcher;

	private MasterState masterState;

	final TableStats stats = new TableStats();

	private SortedSet<TServerInstance> lastScanServers = ImmutableSortedSet.of();

	TabletGroupWatcher(Master master, TabletStateStore store, TabletGroupWatcher dependentWatcher) {
		this.master = master;
		this.store = store;
		this.dependentWatcher = dependentWatcher;
	}

	abstract boolean canSuspendTablets();

	Map<String, TableCounts> getStats() {
		return stats.getLast();
	}

	MasterState statsState() {
		return masterState;
	}

	TableCounts getStats(String tableId) {
		return stats.getLast(tableId);
	}

	public synchronized boolean isSameTserversAsLastScan(Set<TServerInstance> candidates) {
		return candidates.equals(lastScanServers);
	}

	@Override
	public void run() {
		Thread.currentThread().setName(("Watching " + (store.name())));
		int[] oldCounts = new int[TabletState.values().length];
		WalStateManager wals = new WalStateManager(master.getInstance(), ZooReaderWriter.getInstance());
		while (this.master.stillMaster()) {
			UtilWaitThread.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
			masterState = master.getMasterState();
			int totalUnloaded = 0;
			int unloaded = 0;
			ClosableIterator<TabletLocationState> iter = null;
			try {
				Map<String, MergeStats> mergeStatsCache = new HashMap<>();
				Map<String, MergeStats> currentMerges = new HashMap<>();
				for (MergeInfo merge : master.merges()) {
					if ((merge.getExtent()) != null) {
						currentMerges.put(merge.getExtent().getTableId(), new MergeStats(merge));
					}
				}
				SortedMap<TServerInstance, TabletServerStatus> currentTServers = new TreeMap<>();
				if ((currentTServers.size()) == 0) {
					synchronized(this) {
						lastScanServers = ImmutableSortedSet.of();
					}
					continue;
				}
				SortedMap<TServerInstance, TabletServerStatus> destinations = new TreeMap<>(currentTServers);
				List<Assignment> assignments = new ArrayList<>();
				List<Assignment> assigned = new ArrayList<>();
				List<TabletLocationState> assignedToDeadServers = new ArrayList<>();
				List<TabletLocationState> suspendedToGoneServers = new ArrayList<>();
				Map<KeyExtent, TServerInstance> unassigned = new HashMap<>();
				Map<TServerInstance, List<Path>> logsForDeadServers = new TreeMap<>();
				MasterState masterState = master.getMasterState();
				int[] counts = new int[TabletState.values().length];
				stats.begin();
				iter = store.iterator();
				while (iter.hasNext()) {
					TabletLocationState tls = iter.next();
					if (tls == null) {
						continue;
					}
					if ((TableManager.getInstance().getTableState(tls.extent.getTableId())) == null)
						continue;

					String tableId = tls.extent.getTableId();
					TableConfiguration tableConf = this.master.getConfigurationFactory().getTableConfiguration(tableId);
					MergeStats mergeStats = mergeStatsCache.get(tableId);
					if (mergeStats == null) {
						mergeStats = currentMerges.get(tableId);
						if (mergeStats == null) {
							mergeStats = new MergeStats(new MergeInfo());
						}
						mergeStatsCache.put(tableId, mergeStats);
					}
					TServerInstance server = tls.getServer();
					TabletState state = tls.getState(currentTServers.keySet());
					stats.update(tableId, state);
					mergeStats.update(tls.extent, state, tls.chopped, (!(tls.walogs.isEmpty())));
					sendChopRequest(mergeStats.getMergeInfo(), state, tls);
					sendSplitRequest(mergeStats.getMergeInfo(), state, tls);
					if (state == (TabletState.ASSIGNED)) {
					}
					(counts[state.ordinal()])++;
				} 
				flushChanges(destinations, assignments, assigned, assignedToDeadServers, logsForDeadServers, suspendedToGoneServers, unassigned);
				stats.end(masterState);
				for (TabletState state : TabletState.values()) {
					int i = state.ordinal();
					if (((counts[i]) > 0) && ((counts[i]) != (oldCounts[i]))) {
					}
				}
				oldCounts = counts;
				if (totalUnloaded > 0) {
				}
				updateMergeState(mergeStatsCache);
				synchronized(this) {
					lastScanServers = ImmutableSortedSet.copyOf(currentTServers.keySet());
				}
			} catch (Exception ex) {
				if (((ex.getCause()) != null) && ((ex.getCause()) instanceof TabletLocationState.BadLocationStateException)) {
					repairMetadata(((TabletLocationState.BadLocationStateException) (ex.getCause())).getEncodedEndRow());
				}else {
				}
			} finally {
				if (iter != null) {
					try {
						iter.close();
					} catch (IOException ex) {
					}
				}
			}
		} 
	}

	private void cancelOfflineTableMigrations(TabletLocationState tls) {
		TableState tableState = TableManager.getInstance().getTableState(tls.extent.getTableId());
	}

	private void repairMetadata(Text row) {
		try {
			Map<Key, Value> future = new HashMap<>();
			Map<Key, Value> assigned = new HashMap<>();
			KeyExtent extent = new KeyExtent(row, new Value(new byte[]{ 0 }));
			String table = MetadataTable.NAME;
			if (extent.isMeta())
				table = RootTable.NAME;

			Scanner scanner = this.master.getConnector().createScanner(table, Authorizations.EMPTY);
			scanner.fetchColumnFamily(NAME);
			scanner.fetchColumnFamily(MetadataSchema.TabletsSection.FutureLocationColumnFamily.NAME);
			scanner.setRange(new Range(row));
			for (Map.Entry<Key, Value> entry : scanner) {
				if (entry.getKey().getColumnFamily().equals(NAME)) {
					assigned.put(entry.getKey(), entry.getValue());
				}else
					if (entry.getKey().getColumnFamily().equals(MetadataSchema.TabletsSection.FutureLocationColumnFamily.NAME)) {
						future.put(entry.getKey(), entry.getValue());
					}

			}
			if (((future.size()) > 0) && ((assigned.size()) > 0)) {
			}else
				if (((future.size()) > 1) && ((assigned.size()) == 0)) {
				}else
					if (((future.size()) == 0) && ((assigned.size()) > 1)) {
					}else {
						return;
					}


			Iterator<Map.Entry<Key, Value>> iter = Iterators.concat(future.entrySet().iterator(), assigned.entrySet().iterator());
			while (iter.hasNext()) {
				Map.Entry<Key, Value> entry = iter.next();
			} 
		} catch (Throwable e) {
		}
	}

	private int assignedOrHosted() {
		int result = 0;
		for (TableCounts counts : stats.getLast().values()) {
			result += (counts.assigned()) + (counts.hosted());
		}
		return result;
	}

	private void sendSplitRequest(MergeInfo info, TabletState state, TabletLocationState tls) {
		if (!(info.getState().equals(MergeState.SPLITTING)))
			return;

		if (!(info.isDelete()))
			return;

		if (!(state.equals(TabletState.HOSTED)))
			return;

		KeyExtent range = info.getExtent();
		if (tls.extent.overlaps(range)) {
			for (Text splitPoint : new Text[]{ range.getPrevEndRow(), range.getEndRow() }) {
				if (splitPoint == null)
					continue;

				if (!(tls.extent.contains(splitPoint)))
					continue;

				if (splitPoint.equals(tls.extent.getEndRow()))
					continue;

				if (splitPoint.equals(tls.extent.getPrevEndRow()))
					continue;

				try {
					LiveTServerSet.TServerConnection conn;
					conn = null;
					if (conn != null) {
					}else {
					}
				} catch (Exception e) {
				}
			}
		}
	}

	private void sendChopRequest(MergeInfo info, TabletState state, TabletLocationState tls) {
		if (!(info.getState().equals(MergeState.WAITING_FOR_CHOPPED)))
			return;

		if (!(state.equals(TabletState.HOSTED)))
			return;

		if (tls.chopped)
			return;

		if (info.needsToBeChopped(tls.extent)) {
			LiveTServerSet.TServerConnection conn;
			conn = null;
			if (conn != null) {
			}else {
			}
		}
	}

	private void updateMergeState(Map<String, MergeStats> mergeStatsCache) {
		for (MergeStats stats : mergeStatsCache.values()) {
			try {
				MergeState update = stats.nextMergeState(this.master.getConnector(), this.master);
				if (update == (MergeState.COMPLETE))
					update = MergeState.NONE;

				if (update != (stats.getMergeInfo().getState())) {
					this.master.setMergeState(stats.getMergeInfo(), update);
				}
				if (update == (MergeState.MERGING)) {
					try {
						if (stats.getMergeInfo().isDelete()) {
							deleteTablets(stats.getMergeInfo());
						}else {
							mergeMetadataRecords(stats.getMergeInfo());
						}
						this.master.setMergeState(stats.getMergeInfo(), (update = MergeState.COMPLETE));
					} catch (Exception ex) {
					}
				}
			} catch (Exception ex) {
			}
		}
	}

	private void deleteTablets(MergeInfo info) throws AccumuloException {
		KeyExtent extent = info.getExtent();
		String targetSystemTable = (extent.isMeta()) ? RootTable.NAME : MetadataTable.NAME;
		char timeType = '\u0000';
		KeyExtent followingTablet = null;
		if ((extent.getEndRow()) != null) {
			Key nextExtent = new Key(extent.getEndRow()).followingKey(PartialKey.ROW);
			followingTablet = getHighTablet(new KeyExtent(extent.getTableId(), nextExtent.getRow(), extent.getEndRow()));
		}
		try {
			Connector conn = this.master.getConnector();
			Text start = extent.getPrevEndRow();
			if (start == null) {
				start = new Text();
			}
			Range deleteRange = new Range(KeyExtent.getMetadataEntry(extent.getTableId(), start), false, KeyExtent.getMetadataEntry(extent.getTableId(), extent.getEndRow()), true);
			Scanner scanner = conn.createScanner(targetSystemTable, Authorizations.EMPTY);
			scanner.setRange(deleteRange);
			DIRECTORY_COLUMN.fetch(scanner);
			TIME_COLUMN.fetch(scanner);
			scanner.fetchColumnFamily(MetadataSchema.TabletsSection.DataFileColumnFamily.NAME);
			scanner.fetchColumnFamily(NAME);
			Set<FileRef> datafiles = new TreeSet<>();
			for (Map.Entry<Key, Value> entry : scanner) {
				Key key = entry.getKey();
				if ((key.compareColumnFamily(MetadataSchema.TabletsSection.DataFileColumnFamily.NAME)) == 0) {
					if ((datafiles.size()) > 1000) {
						MetadataTableUtil.addDeleteEntries(extent, datafiles, master);
						datafiles.clear();
					}
				}else
					if (TIME_COLUMN.hasColumns(key)) {
						timeType = entry.getValue().toString().charAt(0);
					}else
						if ((key.compareColumnFamily(NAME)) == 0) {
							throw new IllegalStateException((("Tablet " + (key.getRow())) + " is assigned during a merge!"));
						}else
							if (DIRECTORY_COLUMN.hasColumns(key)) {
								String path = entry.getValue().toString();
								if (path.contains(":")) {
									datafiles.add(new FileRef(path));
								}else {
								}
								if ((datafiles.size()) > 1000) {
									MetadataTableUtil.addDeleteEntries(extent, datafiles, master);
									datafiles.clear();
								}
							}



			}
			MetadataTableUtil.addDeleteEntries(extent, datafiles, master);
			BatchWriter bw = conn.createBatchWriter(targetSystemTable, new BatchWriterConfig());
			try {
				deleteTablets(info, deleteRange, bw, conn);
			} finally {
				bw.close();
			}
			if (followingTablet != null) {
				bw = conn.createBatchWriter(targetSystemTable, new BatchWriterConfig());
				try {
					Mutation m = new Mutation(followingTablet.getMetadataEntry());
					PREV_ROW_COLUMN.put(m, KeyExtent.encodePrevEndRow(extent.getPrevEndRow()));
					CHOPPED_COLUMN.putDelete(m);
					bw.addMutation(m);
					bw.flush();
				} finally {
					bw.close();
				}
			}else {
				String tdir = ((((master.getFileSystem().choose(Optional.of(extent.getTableId()), ServerConstants.getBaseUris())) + (Constants.HDFS_TABLES_DIR)) + (Path.SEPARATOR)) + (extent.getTableId())) + (Constants.DEFAULT_TABLET_LOCATION);
			}
		} catch (RuntimeException | IOException | TableNotFoundException | AccumuloSecurityException ex) {
			throw new AccumuloException(ex);
		}
	}

	private void mergeMetadataRecords(MergeInfo info) throws AccumuloException {
		KeyExtent range = info.getExtent();
		KeyExtent stop = getHighTablet(range);
		Value firstPrevRowValue = null;
		Text stopRow = stop.getMetadataEntry();
		Text start = range.getPrevEndRow();
		if (start == null) {
			start = new Text();
		}
		Range scanRange = new Range(KeyExtent.getMetadataEntry(range.getTableId(), start), false, stopRow, false);
		String targetSystemTable = MetadataTable.NAME;
		if (range.isMeta()) {
			targetSystemTable = RootTable.NAME;
		}
		BatchWriter bw = null;
		try {
			long fileCount = 0;
			Connector conn = this.master.getConnector();
			bw = conn.createBatchWriter(targetSystemTable, new BatchWriterConfig());
			Scanner scanner = conn.createScanner(targetSystemTable, Authorizations.EMPTY);
			scanner.setRange(scanRange);
			PREV_ROW_COLUMN.fetch(scanner);
			TIME_COLUMN.fetch(scanner);
			DIRECTORY_COLUMN.fetch(scanner);
			scanner.fetchColumnFamily(MetadataSchema.TabletsSection.DataFileColumnFamily.NAME);
			Mutation m = new Mutation(stopRow);
			String maxLogicalTime = null;
			for (Map.Entry<Key, Value> entry : scanner) {
				Key key = entry.getKey();
				Value value = entry.getValue();
				if (key.getColumnFamily().equals(MetadataSchema.TabletsSection.DataFileColumnFamily.NAME)) {
					m.put(key.getColumnFamily(), key.getColumnQualifier(), value);
					fileCount++;
				}else
					if ((PREV_ROW_COLUMN.hasColumns(key)) && (firstPrevRowValue == null)) {
						firstPrevRowValue = new Value(value);
					}else
						if (TIME_COLUMN.hasColumns(key)) {
							maxLogicalTime = TabletTime.maxMetadataTime(maxLogicalTime, value.toString());
						}else
							if (DIRECTORY_COLUMN.hasColumns(key)) {
								bw.addMutation(MetadataTableUtil.createDeleteMutation(range.getTableId(), entry.getValue().toString()));
							}



			}
			scanner = conn.createScanner(targetSystemTable, Authorizations.EMPTY);
			scanner.setRange(new Range(stopRow));
			TIME_COLUMN.fetch(scanner);
			for (Map.Entry<Key, Value> entry : scanner) {
				if (TIME_COLUMN.hasColumns(entry.getKey())) {
					maxLogicalTime = TabletTime.maxMetadataTime(maxLogicalTime, entry.getValue().toString());
				}
			}
			if (maxLogicalTime != null)
				TIME_COLUMN.put(m, new Value(maxLogicalTime.getBytes()));

			if (!(m.getUpdates().isEmpty())) {
				bw.addMutation(m);
			}
			bw.flush();
			if (firstPrevRowValue == null) {
				return;
			}
			stop.setPrevEndRow(KeyExtent.decodePrevEndRow(firstPrevRowValue));
			Mutation updatePrevRow = stop.getPrevRowUpdateMutation();
			bw.addMutation(updatePrevRow);
			bw.flush();
			deleteTablets(info, scanRange, bw, conn);
			m = new Mutation(stopRow);
			CHOPPED_COLUMN.putDelete(m);
			bw.addMutation(m);
			bw.flush();
		} catch (Exception ex) {
			throw new AccumuloException(ex);
		} finally {
			if (bw != null)
				try {
					bw.close();
				} catch (Exception ex) {
					throw new AccumuloException(ex);
				}

		}
	}

	private void deleteTablets(MergeInfo info, Range scanRange, BatchWriter bw, Connector conn) throws MutationsRejectedException, TableNotFoundException {
		Scanner scanner;
		Mutation m;
		scanner = conn.createScanner((info.getExtent().isMeta() ? RootTable.NAME : MetadataTable.NAME), Authorizations.EMPTY);
		scanner.setRange(scanRange);
		RowIterator rowIter = new RowIterator(scanner);
		while (rowIter.hasNext()) {
			Iterator<Map.Entry<Key, Value>> row = rowIter.next();
			m = null;
			while (row.hasNext()) {
				Map.Entry<Key, Value> entry = row.next();
				Key key = entry.getKey();
				if (m == null)
					m = new Mutation(key.getRow());

				m.putDelete(key.getColumnFamily(), key.getColumnQualifier());
			} 
			bw.addMutation(m);
		} 
		bw.flush();
	}

	private KeyExtent getHighTablet(KeyExtent range) throws AccumuloException {
		try {
			Connector conn = this.master.getConnector();
			Scanner scanner = conn.createScanner((range.isMeta() ? RootTable.NAME : MetadataTable.NAME), Authorizations.EMPTY);
			PREV_ROW_COLUMN.fetch(scanner);
			KeyExtent start = new KeyExtent(range.getTableId(), range.getEndRow(), null);
			scanner.setRange(new Range(start.getMetadataEntry(), null));
			Iterator<Map.Entry<Key, Value>> iterator = scanner.iterator();
			if (!(iterator.hasNext())) {
				throw new AccumuloException(("No last tablet for a merge " + range));
			}
			Map.Entry<Key, Value> entry = iterator.next();
			KeyExtent highTablet = new KeyExtent(entry.getKey().getRow(), KeyExtent.decodePrevEndRow(entry.getValue()));
			if (!(highTablet.getTableId().equals(range.getTableId()))) {
				throw new AccumuloException(((("No last tablet for merge " + range) + " ") + highTablet));
			}
			return highTablet;
		} catch (Exception ex) {
			throw new AccumuloException(("Unexpected failure finding the last tablet for a merge " + range), ex);
		}
	}

	private void flushChanges(SortedMap<TServerInstance, TabletServerStatus> currentTServers, List<Assignment> assignments, List<Assignment> assigned, List<TabletLocationState> assignedToDeadServers, Map<TServerInstance, List<Path>> logsForDeadServers, List<TabletLocationState> suspendedToGoneServers, Map<KeyExtent, TServerInstance> unassigned) throws WalStateManager.WalMarkerException, DistributedStoreException, TException {
		boolean tabletsSuspendable = canSuspendTablets();
		if (!(assignedToDeadServers.isEmpty())) {
			int maxServersToShow = Math.min(assignedToDeadServers.size(), 100);
			if (tabletsSuspendable) {
				store.suspend(assignedToDeadServers, logsForDeadServers, master.getSteadyTime());
			}else {
				store.unassign(assignedToDeadServers, logsForDeadServers);
			}
			this.master.markDeadServerLogsAsClosed(logsForDeadServers);
		}
		if (!(suspendedToGoneServers.isEmpty())) {
			int maxServersToShow = Math.min(assignedToDeadServers.size(), 100);
			store.unsuspend(suspendedToGoneServers);
		}
		if (!(currentTServers.isEmpty())) {
			Map<KeyExtent, TServerInstance> assignedOut = new HashMap<>();
			final StringBuilder builder = new StringBuilder(64);
			for (Map.Entry<KeyExtent, TServerInstance> assignment : assignedOut.entrySet()) {
				if (unassigned.containsKey(assignment.getKey())) {
					if ((assignment.getValue()) != null) {
						if (!(currentTServers.containsKey(assignment.getValue()))) {
							continue;
						}
						if ((builder.length()) > 0) {
							builder.append(TabletGroupWatcher.ASSIGNMENT_BUFFER_SEPARATOR);
						}
						builder.append(assignment);
						if ((builder.length()) > (TabletGroupWatcher.ASSINGMENT_BUFFER_MAX_LENGTH)) {
							builder.append("]");
							builder.setLength(0);
						}
						assignments.add(new Assignment(assignment.getKey(), assignment.getValue()));
					}
				}else {
				}
			}
			if ((builder.length()) > 0) {
				builder.append("]");
			}
			if ((!(unassigned.isEmpty())) && (assignedOut.isEmpty())) {
			}
		}
		if ((assignments.size()) > 0) {
			store.setFutureLocations(assignments);
		}
		assignments.addAll(assigned);
		for (Assignment a : assignments) {
			master.assignedTablet(a.tablet);
		}
	}
}

