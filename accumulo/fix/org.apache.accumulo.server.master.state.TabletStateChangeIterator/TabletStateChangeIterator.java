

import com.google.common.base.Joiner;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.impl.KeyExtent;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SkippingIterator;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.iterators.WrappingIterator;
import org.apache.accumulo.core.master.thrift.MasterState;
import org.apache.accumulo.core.util.AddressUtil;
import org.apache.accumulo.core.util.Base64;
import org.apache.accumulo.core.util.HostAndPort;
import org.apache.accumulo.server.master.state.MergeInfo;
import org.apache.accumulo.server.master.state.MergeState;
import org.apache.accumulo.server.master.state.MetaDataTableScanner;
import org.apache.accumulo.server.master.state.TServerInstance;
import org.apache.accumulo.server.master.state.TabletLocationState;
import org.apache.accumulo.server.master.state.TabletState;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TabletStateChangeIterator extends SkippingIterator {
	private static final String SERVERS_OPTION = "servers";

	private static final String TABLES_OPTION = "tables";

	private static final String MERGES_OPTION = "merges";

	private static final String DEBUG_OPTION = "debug";

	private static final String MIGRATIONS_OPTION = "migrations";

	private static final String MASTER_STATE_OPTION = "masterState";

	private static final String SHUTTING_DOWN_OPTION = "shuttingDown";

	private static final Logger log = LoggerFactory.getLogger(TabletStateChangeIterator.class);

	private Set<TServerInstance> current;

	private Set<String> onlineTables;

	private Map<String, MergeInfo> merges;

	private boolean debug = false;

	private Set<KeyExtent> migrations;

	private MasterState masterState = MasterState.NORMAL;

	@Override
	public void init(SortedKeyValueIterator<Key, Value> source, Map<String, String> options, IteratorEnvironment env) throws IOException {
		super.init(source, options, env);
		current = parseServers(options.get(TabletStateChangeIterator.SERVERS_OPTION));
		onlineTables = parseTables(options.get(TabletStateChangeIterator.TABLES_OPTION));
		merges = parseMerges(options.get(TabletStateChangeIterator.MERGES_OPTION));
		debug = options.containsKey(TabletStateChangeIterator.DEBUG_OPTION);
		migrations = parseMigrations(options.get(TabletStateChangeIterator.MIGRATIONS_OPTION));
		try {
			masterState = MasterState.valueOf(options.get(TabletStateChangeIterator.MASTER_STATE_OPTION));
		} catch (Exception ex) {
			if ((options.get(TabletStateChangeIterator.MASTER_STATE_OPTION)) != null) {
				TabletStateChangeIterator.log.error(("Unable to decode masterState " + (options.get(TabletStateChangeIterator.MASTER_STATE_OPTION))));
			}
		}
		Set<TServerInstance> shuttingDown = parseServers(options.get(TabletStateChangeIterator.SHUTTING_DOWN_OPTION));
		if (((current) != null) && (shuttingDown != null)) {
			current.removeAll(shuttingDown);
		}
	}

	private Set<KeyExtent> parseMigrations(String migrations) {
		if (migrations == null)
			return Collections.emptySet();

		try {
			Set<KeyExtent> result = new HashSet<>();
			DataInputBuffer buffer = new DataInputBuffer();
			byte[] data = Base64.decodeBase64(migrations.getBytes(StandardCharsets.UTF_8));
			buffer.reset(data, data.length);
			while ((buffer.available()) > 0) {
				KeyExtent extent = new KeyExtent();
				extent.readFields(buffer);
				result.add(extent);
			} 
			return result;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private Set<String> parseTables(String tables) {
		if (tables == null)
			return null;

		Set<String> result = new HashSet<>();
		for (String table : tables.split(","))
			result.add(table);

		return result;
	}

	private Set<TServerInstance> parseServers(String servers) {
		if (servers == null)
			return null;

		Set<TServerInstance> result = new HashSet<>();
		if ((servers.length()) > 0) {
			for (String part : servers.split(",")) {
				String[] parts = part.split("\\[", 2);
				String hostport = parts[0];
				String instance = parts[1];
				if ((instance != null) && (instance.endsWith("]")))
					instance = instance.substring(0, ((instance.length()) - 1));

				result.add(new TServerInstance(AddressUtil.parseAddress(hostport, false), instance));
			}
		}
		return result;
	}

	private Map<String, MergeInfo> parseMerges(String merges) {
		if (merges == null)
			return null;

		try {
			Map<String, MergeInfo> result = new HashMap<>();
			DataInputBuffer buffer = new DataInputBuffer();
			byte[] data = Base64.decodeBase64(merges.getBytes(StandardCharsets.UTF_8));
			buffer.reset(data, data.length);
			while ((buffer.available()) > 0) {
				MergeInfo mergeInfo = new MergeInfo();
				mergeInfo.readFields(buffer);
			} 
			return result;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	protected void consume() throws IOException {
		while (getSource().hasTop()) {
			Key k = getSource().getTopKey();
			Value v = getSource().getTopValue();
			if ((((onlineTables) == null) || ((current) == null)) || ((masterState) != (MasterState.NORMAL)))
				return;

			TabletLocationState tls;
			try {
				tls = MetaDataTableScanner.createTabletLocationState(k, v);
				if (tls == null)
					return;

			} catch (TabletLocationState.BadLocationStateException e) {
				return;
			}
			MergeInfo merge = merges.get(tls.extent.getTableId());
			if (merge != null) {
				return;
			}
			if (migrations.contains(tls.extent)) {
				return;
			}
			boolean shouldBeOnline = onlineTables.contains(tls.extent.getTableId());
			if (debug) {
				TabletStateChangeIterator.log.debug(((((((tls.extent) + " is ") + (tls.getState(current))) + " and should be ") + (shouldBeOnline ? "on" : "off")) + "line"));
			}
			switch (tls.getState(current)) {
				case ASSIGNED :
					return;
				case HOSTED :
					if (!shouldBeOnline)
						return;

					break;
				case ASSIGNED_TO_DEAD_SERVER :
					return;
				case SUSPENDED :
				case UNASSIGNED :
					if (shouldBeOnline)
						return;

					break;
				default :
					throw new AssertionError(("Inconceivable! The tablet is an unrecognized state: " + (tls.getState(current))));
			}
			getSource().next();
		} 
	}

	@Override
	public SortedKeyValueIterator<Key, Value> deepCopy(IteratorEnvironment env) {
		throw new UnsupportedOperationException();
	}

	public static void setCurrentServers(IteratorSetting cfg, Set<TServerInstance> goodServers) {
		if (goodServers != null) {
			List<String> servers = new ArrayList<>();
			for (TServerInstance server : goodServers)
				servers.add(server.toString());

			cfg.addOption(TabletStateChangeIterator.SERVERS_OPTION, Joiner.on(",").join(servers));
		}
	}

	public static void setOnlineTables(IteratorSetting cfg, Set<String> onlineTables) {
		if (onlineTables != null)
			cfg.addOption(TabletStateChangeIterator.TABLES_OPTION, Joiner.on(",").join(onlineTables));

	}

	public static void setMerges(IteratorSetting cfg, Collection<MergeInfo> merges) {
		DataOutputBuffer buffer = new DataOutputBuffer();
		try {
			for (MergeInfo info : merges) {
				KeyExtent extent = info.getExtent();
				if ((extent != null) && (!(info.getState().equals(MergeState.NONE)))) {
					info.write(buffer);
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		String encoded = Base64.encodeBase64String(Arrays.copyOf(buffer.getData(), buffer.getLength()));
		cfg.addOption(TabletStateChangeIterator.MERGES_OPTION, encoded);
	}

	public static void setMigrations(IteratorSetting cfg, Collection<KeyExtent> migrations) {
		DataOutputBuffer buffer = new DataOutputBuffer();
		try {
			for (KeyExtent extent : migrations) {
				extent.write(buffer);
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		String encoded = Base64.encodeBase64String(Arrays.copyOf(buffer.getData(), buffer.getLength()));
		cfg.addOption(TabletStateChangeIterator.MIGRATIONS_OPTION, encoded);
	}

	public static void setMasterState(IteratorSetting cfg, MasterState state) {
		cfg.addOption(TabletStateChangeIterator.MASTER_STATE_OPTION, state.toString());
	}

	public static void setShuttingDown(IteratorSetting cfg, Set<TServerInstance> servers) {
		if (servers != null) {
			List<String> serverList = new ArrayList<>();
			for (TServerInstance server : servers) {
				serverList.add(server.toString());
			}
			cfg.addOption(TabletStateChangeIterator.SHUTTING_DOWN_OPTION, Joiner.on(",").join(servers));
		}
	}
}

