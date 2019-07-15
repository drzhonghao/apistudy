

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Metadata;
import com.google.common.collect.ImmutableMap;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.cassandra.config.EncryptionOptions;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.stress.settings.Command;
import org.apache.cassandra.stress.settings.ConnectionAPI;
import org.apache.cassandra.stress.settings.CqlVersion;
import org.apache.cassandra.stress.settings.SettingsColumn;
import org.apache.cassandra.stress.settings.SettingsCommand;
import org.apache.cassandra.stress.settings.SettingsErrors;
import org.apache.cassandra.stress.settings.SettingsGraph;
import org.apache.cassandra.stress.settings.SettingsInsert;
import org.apache.cassandra.stress.settings.SettingsLog;
import org.apache.cassandra.stress.settings.SettingsMode;
import org.apache.cassandra.stress.settings.SettingsNode;
import org.apache.cassandra.stress.settings.SettingsPopulation;
import org.apache.cassandra.stress.settings.SettingsPort;
import org.apache.cassandra.stress.settings.SettingsRate;
import org.apache.cassandra.stress.settings.SettingsSchema;
import org.apache.cassandra.stress.settings.SettingsTokenRange;
import org.apache.cassandra.stress.settings.SettingsTransport;
import org.apache.cassandra.stress.util.JavaDriverClient;
import org.apache.cassandra.stress.util.ResultLogger;
import org.apache.cassandra.stress.util.SimpleThriftClient;
import org.apache.cassandra.stress.util.SmartThriftClient;
import org.apache.cassandra.stress.util.ThriftClient;
import org.apache.cassandra.thrift.AuthenticationRequest;
import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.ITransportFactory;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.transport.SimpleClient;
import org.apache.cassandra.transport.messages.ResultMessage;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;


public class StressSettings implements Serializable {
	public final SettingsCommand command;

	public final SettingsRate rate;

	public final SettingsPopulation generate;

	public final SettingsInsert insert;

	public final SettingsColumn columns;

	public final SettingsErrors errors;

	public final SettingsLog log;

	public final SettingsMode mode;

	public final SettingsNode node;

	public final SettingsSchema schema;

	public final SettingsTransport transport;

	public final SettingsPort port;

	public final String sendToDaemon;

	public final SettingsGraph graph;

	public final SettingsTokenRange tokenRange;

	public StressSettings(SettingsCommand command, SettingsRate rate, SettingsPopulation generate, SettingsInsert insert, SettingsColumn columns, SettingsErrors errors, SettingsLog log, SettingsMode mode, SettingsNode node, SettingsSchema schema, SettingsTransport transport, SettingsPort port, String sendToDaemon, SettingsGraph graph, SettingsTokenRange tokenRange) {
		this.command = command;
		this.rate = rate;
		this.insert = insert;
		this.generate = generate;
		this.columns = columns;
		this.errors = errors;
		this.log = log;
		this.mode = mode;
		this.node = node;
		this.schema = schema;
		this.transport = transport;
		this.port = port;
		this.sendToDaemon = sendToDaemon;
		this.graph = graph;
		this.tokenRange = tokenRange;
	}

	private SmartThriftClient tclient;

	public synchronized ThriftClient getThriftClient() {
		if ((mode.api) != (ConnectionAPI.THRIFT_SMART))
			return getSimpleThriftClient();

		if ((tclient) == null)
			tclient = getSmartThriftClient();

		return tclient;
	}

	private SmartThriftClient getSmartThriftClient() {
		Metadata metadata = getJavaDriverClient().getCluster().getMetadata();
		return null;
	}

	private SimpleThriftClient getSimpleThriftClient() {
		return new SimpleThriftClient(getRawThriftClient(node.randomNode(), true));
	}

	public Cassandra.Client getRawThriftClient(boolean setKeyspace) {
		return getRawThriftClient(node.randomNode(), setKeyspace);
	}

	public Cassandra.Client getRawThriftClient(String host) {
		return getRawThriftClient(host, true);
	}

	public Cassandra.Client getRawThriftClient(String host, boolean setKeyspace) {
		Cassandra.Client client;
		try {
			TTransport transport = this.transport.getFactory().openTransport(host, port.thriftPort);
			client = new Cassandra.Client(new TBinaryProtocol(transport));
			if (mode.cqlVersion.isCql())
				client.set_cql_version(mode.cqlVersion.connectVersion);

			if (setKeyspace)
				client.set_keyspace(schema.keyspace);

			if ((mode.username) != null)
				client.login(new AuthenticationRequest(ImmutableMap.of("username", mode.username, "password", mode.password)));

		} catch (InvalidRequestException e) {
			throw new RuntimeException(e.getWhy());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return client;
	}

	public SimpleClient getSimpleNativeClient() {
		try {
			String currentNode = node.randomNode();
			SimpleClient client = new SimpleClient(currentNode, port.nativePort);
			client.connect(false);
			client.execute((("USE \"" + (schema.keyspace)) + "\";"), ConsistencyLevel.ONE);
			return client;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	private static volatile JavaDriverClient client;

	private static volatile int numFailures;

	private static int MAX_NUM_FAILURES = 10;

	public JavaDriverClient getJavaDriverClient() {
		return getJavaDriverClient(true);
	}

	public JavaDriverClient getJavaDriverClient(boolean setKeyspace) {
		if ((StressSettings.client) != null)
			return StressSettings.client;

		synchronized(this) {
			if ((StressSettings.numFailures) >= (StressSettings.MAX_NUM_FAILURES))
				throw new RuntimeException("Failed to create client too many times");

			try {
				String currentNode = node.randomNode();
				if ((StressSettings.client) != null)
					return StressSettings.client;

				EncryptionOptions.ClientEncryptionOptions encOptions = transport.getEncryptionOptions();
				if (setKeyspace) {
				}
			} catch (Exception e) {
				StressSettings.numFailures += 1;
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	public void maybeCreateKeyspaces() {
	}

	public static StressSettings parse(String[] args) {
		args = StressSettings.repairParams(args);
		final Map<String, String[]> clArgs = StressSettings.parseMap(args);
		if (clArgs.containsKey("legacy")) {
		}
		return StressSettings.get(clArgs);
	}

	private static String[] repairParams(String[] args) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String arg : args) {
			if (!first)
				sb.append(" ");

			sb.append(arg);
			first = false;
		}
		return sb.toString().replaceAll("\\s+([,=()])", "$1").replaceAll("([,=(])\\s+", "$1").split(" +");
	}

	public static StressSettings get(Map<String, String[]> clArgs) {
		SettingsPort port = SettingsPort.get(clArgs);
		SettingsTokenRange tokenRange = SettingsTokenRange.get(clArgs);
		SettingsInsert insert = SettingsInsert.get(clArgs);
		SettingsErrors errors = SettingsErrors.get(clArgs);
		SettingsLog log = SettingsLog.get(clArgs);
		SettingsMode mode = SettingsMode.get(clArgs);
		SettingsNode node = SettingsNode.get(clArgs);
		SettingsTransport transport = SettingsTransport.get(clArgs);
		if (!(clArgs.isEmpty())) {
			StressSettings.printHelp();
			System.out.println("Error processing command line arguments. The following were ignored:");
			for (Map.Entry<String, String[]> e : clArgs.entrySet()) {
				System.out.print(e.getKey());
				for (String v : e.getValue()) {
					System.out.print(" ");
					System.out.print(v);
				}
				System.out.println();
			}
			System.exit(1);
		}
		return null;
	}

	private static Map<String, String[]> parseMap(String[] args) {
		if ((args.length) == 0) {
			System.out.println("No command provided");
			StressSettings.printHelp();
			System.exit(1);
		}
		final LinkedHashMap<String, String[]> r = new LinkedHashMap<>();
		String key = null;
		List<String> params = new ArrayList<>();
		for (int i = 0; i < (args.length); i++) {
			if ((i == 0) || (args[i].startsWith("-"))) {
				if (i > 0)
					StressSettings.putParam(key, params.toArray(new String[0]), r);

				key = args[i].toLowerCase();
				params.clear();
			}else
				params.add(args[i]);

		}
		StressSettings.putParam(key, params.toArray(new String[0]), r);
		return r;
	}

	private static void putParam(String key, String[] args, Map<String, String[]> clArgs) {
		String[] prev = clArgs.put(key, args);
		if (prev != null)
			throw new IllegalArgumentException((key + " is defined multiple times. Each option/command can be specified at most once."));

	}

	public static void printHelp() {
	}

	public void printSettings(ResultLogger out) {
		out.println("******************** Stress Settings ********************");
		out.println("Command:");
		command.printSettings(out);
		out.println("Rate:");
		rate.printSettings(out);
		out.println("Population:");
		generate.printSettings(out);
		out.println("Insert:");
		insert.printSettings(out);
		if ((command.type) != (Command.USER)) {
			out.println("Columns:");
			columns.printSettings(out);
		}
		out.println("Errors:");
		errors.printSettings(out);
		out.println("Log:");
		log.printSettings(out);
		out.println("Mode:");
		mode.printSettings(out);
		out.println("Node:");
		node.printSettings(out);
		out.println("Schema:");
		schema.printSettings(out);
		out.println("Transport:");
		transport.printSettings(out);
		out.println("Port:");
		port.printSettings(out);
		out.println("Send To Daemon:");
		out.printf((("  " + ((sendToDaemon) != null ? sendToDaemon : "*not set*")) + "%n"));
		out.println("Graph:");
		graph.printSettings(out);
		out.println("TokenRange:");
		tokenRange.printSettings(out);
		if ((command.type) == (Command.USER)) {
			out.println();
			out.println("******************** Profile ********************");
		}
		out.println();
	}

	public synchronized void disconnect() {
		if ((StressSettings.client) == null)
			return;

		StressSettings.client.disconnect();
		StressSettings.client = null;
	}
}

