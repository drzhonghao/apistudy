

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import org.apache.accumulo.core.client.BatchScanner;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.ScannerBase;
import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.iterators.user.WholeRowIterator;
import org.apache.accumulo.core.master.thrift.MasterState;
import org.apache.accumulo.core.metadata.MetadataTable;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.util.ColumnFQ;
import org.apache.accumulo.server.master.state.ClosableIterator;
import org.apache.accumulo.server.master.state.CurrentState;
import org.apache.accumulo.server.master.state.MergeInfo;
import org.apache.accumulo.server.master.state.SuspendingTServer;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.master.state.TabletLocationState;
import org.apache.accumulo.server.master.state.TabletStateChangeIterator;
import org.apache.hadoop.io.BinaryComparable;
import org.apache.hadoop.io.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.CurrentLocationColumnFamily.NAME;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.SuspendLocationColumn.SUSPEND_COLUMN;
import static org.apache.accumulo.core.metadata.schema.MetadataSchema.TabletsSection.TabletColumnFamily.PREV_ROW_COLUMN;


public class MetaDataTableScanner implements ClosableIterator<TabletLocationState> {
	private static final Logger log = LoggerFactory.getLogger(MetaDataTableScanner.class);

	BatchScanner mdScanner = null;

	Iterator<Map.Entry<Key, Value>> iter = null;

	public MetaDataTableScanner(ClientContext context, Range range, CurrentState state) {
		this(context, range, state, MetadataTable.NAME);
	}

	MetaDataTableScanner(ClientContext context, Range range, CurrentState state, String tableName) {
		try {
			Connector connector = context.getConnector();
			mdScanner = connector.createBatchScanner(tableName, Authorizations.EMPTY, 8);
			MetaDataTableScanner.configureScanner(mdScanner, state);
			mdScanner.setRanges(Collections.singletonList(range));
			iter = mdScanner.iterator();
		} catch (Exception ex) {
			if ((mdScanner) != null)
				mdScanner.close();

			iter = null;
			mdScanner = null;
			throw new RuntimeException(ex);
		}
	}

	public static void configureScanner(ScannerBase scanner, CurrentState state) {
		PREV_ROW_COLUMN.fetch(scanner);
		scanner.fetchColumnFamily(NAME);
		scanner.fetchColumnFamily(MetadataSchema.TabletsSection.FutureLocationColumnFamily.NAME);
		scanner.fetchColumnFamily(MetadataSchema.TabletsSection.LastLocationColumnFamily.NAME);
		scanner.fetchColumnFamily(SUSPEND_COLUMN.getColumnFamily());
		scanner.fetchColumnFamily(MetadataSchema.TabletsSection.LogColumnFamily.NAME);
		scanner.fetchColumnFamily(MetadataSchema.TabletsSection.ChoppedColumnFamily.NAME);
		scanner.addScanIterator(new IteratorSetting(1000, "wholeRows", WholeRowIterator.class));
		IteratorSetting tabletChange = new IteratorSetting(1001, "tabletChange", TabletStateChangeIterator.class);
		if (state != null) {
			TabletStateChangeIterator.setCurrentServers(tabletChange, state.onlineTabletServers());
			TabletStateChangeIterator.setOnlineTables(tabletChange, state.onlineTables());
			TabletStateChangeIterator.setMerges(tabletChange, state.merges());
			TabletStateChangeIterator.setMigrations(tabletChange, state.migrationsSnapshot());
			TabletStateChangeIterator.setMasterState(tabletChange, state.getMasterState());
			TabletStateChangeIterator.setShuttingDown(tabletChange, state.shutdownServers());
		}
		scanner.addScanIterator(tabletChange);
	}

	public MetaDataTableScanner(ClientContext context, Range range) {
		this(context, range, MetadataTable.NAME);
	}

	public MetaDataTableScanner(ClientContext context, Range range, String tableName) {
		this(context, range, null, tableName);
	}

	@Override
	public void close() {
		if ((iter) != null) {
			mdScanner.close();
			iter = null;
		}
	}

	@Override
	protected void finalize() {
		close();
	}

	@Override
	public boolean hasNext() {
		if ((iter) == null)
			return false;

		boolean result = iter.hasNext();
		if (!result) {
			close();
		}
		return result;
	}

	@Override
	public TabletLocationState next() {
		return fetch();
	}

	public static TabletLocationState createTabletLocationState(Key k, Value v) throws IOException, TabletLocationState.BadLocationStateException {
		final SortedMap<Key, Value> decodedRow = WholeRowIterator.decodeRow(k, v);
		KeyExtent extent = null;
		TServerInstance future = null;
		TServerInstance current = null;
		TServerInstance last = null;
		SuspendingTServer suspend = null;
		long lastTimestamp = 0;
		List<Collection<String>> walogs = new ArrayList<>();
		boolean chopped = false;
		for (Map.Entry<Key, Value> entry : decodedRow.entrySet()) {
			Key key = entry.getKey();
			Text row = key.getRow();
			Text cf = key.getColumnFamily();
			Text cq = key.getColumnQualifier();
			if ((cf.compareTo(MetadataSchema.TabletsSection.FutureLocationColumnFamily.NAME)) == 0) {
				TServerInstance location = new TServerInstance(entry.getValue(), cq);
				if (future != null) {
				}
				future = location;
			}else
				if ((cf.compareTo(NAME)) == 0) {
					TServerInstance location = new TServerInstance(entry.getValue(), cq);
					if (current != null) {
					}
					current = location;
				}else
					if ((cf.compareTo(MetadataSchema.TabletsSection.LogColumnFamily.NAME)) == 0) {
						String[] split = entry.getValue().toString().split("\\|")[0].split(";");
						walogs.add(Arrays.asList(split));
					}else
						if ((cf.compareTo(MetadataSchema.TabletsSection.LastLocationColumnFamily.NAME)) == 0) {
							if (lastTimestamp < (entry.getKey().getTimestamp()))
								last = new TServerInstance(entry.getValue(), cq);

						}else
							if ((cf.compareTo(MetadataSchema.TabletsSection.ChoppedColumnFamily.NAME)) == 0) {
								chopped = true;
							}else
								if (PREV_ROW_COLUMN.equals(cf, cq)) {
									extent = new KeyExtent(row, entry.getValue());
								}else
									if (SUSPEND_COLUMN.equals(cf, cq)) {
										suspend = SuspendingTServer.fromValue(entry.getValue());
									}






		}
		if (extent == null) {
			String msg = "No prev-row for key extent " + decodedRow;
			MetaDataTableScanner.log.error(msg);
		}
		return new TabletLocationState(extent, future, current, last, suspend, walogs, chopped);
	}

	private TabletLocationState fetch() {
		try {
			Map.Entry<Key, Value> e = iter.next();
			return MetaDataTableScanner.createTabletLocationState(e.getKey(), e.getValue());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} catch (TabletLocationState.BadLocationStateException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void remove() {
		throw new RuntimeException("Unimplemented");
	}
}

