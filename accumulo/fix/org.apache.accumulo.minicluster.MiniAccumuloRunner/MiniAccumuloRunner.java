

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.util.Pair;
import org.apache.accumulo.minicluster.MemoryUnit;
import org.apache.accumulo.minicluster.MiniAccumuloCluster;
import org.apache.accumulo.minicluster.MiniAccumuloConfig;
import org.apache.accumulo.minicluster.ServerType;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MiniAccumuloRunner {
	private static final Logger log = LoggerFactory.getLogger(MiniAccumuloRunner.class);

	private static final String ROOT_PASSWORD_PROP = "rootPassword";

	private static final String SHUTDOWN_PORT_PROP = "shutdownPort";

	private static final String DEFAULT_MEMORY_PROP = "defaultMemory";

	private static final String MASTER_MEMORY_PROP = "masterMemory";

	private static final String TSERVER_MEMORY_PROP = "tserverMemory";

	private static final String ZOO_KEEPER_MEMORY_PROP = "zooKeeperMemory";

	private static final String JDWP_ENABLED_PROP = "jdwpEnabled";

	private static final String ZOO_KEEPER_PORT_PROP = "zooKeeperPort";

	private static final String ZOO_KEEPER_STARTUP_TIME_PROP = "zooKeeperStartupTime";

	private static final String NUM_T_SERVERS_PROP = "numTServers";

	private static final String DIRECTORY_PROP = "directory";

	private static final String INSTANCE_NAME_PROP = "instanceName";

	private static final String EXISTING_ZOO_KEEPERS_PROP = "existingZooKeepers";

	private static void printProperties() {
		System.out.println("#mini Accumulo cluster runner properties.");
		System.out.println("#");
		System.out.println(("#uncomment following propeties to use, propeties not" + " set will use default or random value"));
		System.out.println();
		System.out.println((("#" + (MiniAccumuloRunner.INSTANCE_NAME_PROP)) + "=devTest"));
		System.out.println((("#" + (MiniAccumuloRunner.DIRECTORY_PROP)) + "=/tmp/mac1"));
		System.out.println((("#" + (MiniAccumuloRunner.ROOT_PASSWORD_PROP)) + "=secret"));
		System.out.println((("#" + (MiniAccumuloRunner.NUM_T_SERVERS_PROP)) + "=2"));
		System.out.println((("#" + (MiniAccumuloRunner.ZOO_KEEPER_PORT_PROP)) + "=40404"));
		System.out.println((("#" + (MiniAccumuloRunner.ZOO_KEEPER_STARTUP_TIME_PROP)) + "=39000"));
		System.out.println((("#" + (MiniAccumuloRunner.SHUTDOWN_PORT_PROP)) + "=41414"));
		System.out.println((("#" + (MiniAccumuloRunner.DEFAULT_MEMORY_PROP)) + "=128M"));
		System.out.println((("#" + (MiniAccumuloRunner.MASTER_MEMORY_PROP)) + "=128M"));
		System.out.println((("#" + (MiniAccumuloRunner.TSERVER_MEMORY_PROP)) + "=128M"));
		System.out.println((("#" + (MiniAccumuloRunner.ZOO_KEEPER_MEMORY_PROP)) + "=128M"));
		System.out.println((("#" + (MiniAccumuloRunner.JDWP_ENABLED_PROP)) + "=false"));
		System.out.println((("#" + (MiniAccumuloRunner.EXISTING_ZOO_KEEPERS_PROP)) + "=localhost:2181"));
		System.out.println();
		System.out.println("# Configuration normally placed in accumulo-site.xml can be added using a site. prefix.");
		System.out.println("# For example the following line will set tserver.compaction.major.concurrent.max");
		System.out.println();
		System.out.println("#site.tserver.compaction.major.concurrent.max=4");
	}

	public static class PropertiesConverter implements IStringConverter<Properties> {
		@Override
		public Properties convert(String fileName) {
			Properties prop = new Properties();
			InputStream is;
			try {
				is = new FileInputStream(fileName);
				try {
					prop.load(is);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return prop;
		}
	}

	private static final String FORMAT_STRING = "  %-21s %s";

	public static class Opts extends Help {
		@Parameter(names = "-p", required = false, description = "properties file name", converter = MiniAccumuloRunner.PropertiesConverter.class)
		Properties prop = new Properties();

		@Parameter(names = { "-c", "--printProperties" }, required = false, description = "prints an example propeties file, redirect to file to use")
		boolean printProps = false;
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		MiniAccumuloRunner.Opts opts = new MiniAccumuloRunner.Opts();
		opts.parseArgs(MiniAccumuloRunner.class.getName(), args);
		if (opts.printProps) {
			MiniAccumuloRunner.printProperties();
			System.exit(0);
		}
		int shutdownPort = 4445;
		final File miniDir;
		if (opts.prop.containsKey(MiniAccumuloRunner.DIRECTORY_PROP))
			miniDir = new File(opts.prop.getProperty(MiniAccumuloRunner.DIRECTORY_PROP));
		else {
			miniDir = Files.createTempDirectory(((System.currentTimeMillis()) + "")).toFile();
		}
		String rootPass = (opts.prop.containsKey(MiniAccumuloRunner.ROOT_PASSWORD_PROP)) ? opts.prop.getProperty(MiniAccumuloRunner.ROOT_PASSWORD_PROP) : "secret";
		MiniAccumuloConfig config = new MiniAccumuloConfig(miniDir, rootPass);
		if (opts.prop.containsKey(MiniAccumuloRunner.INSTANCE_NAME_PROP))
			config.setInstanceName(opts.prop.getProperty(MiniAccumuloRunner.INSTANCE_NAME_PROP));

		if (opts.prop.containsKey(MiniAccumuloRunner.NUM_T_SERVERS_PROP))
			config.setNumTservers(Integer.parseInt(opts.prop.getProperty(MiniAccumuloRunner.NUM_T_SERVERS_PROP)));

		if (opts.prop.containsKey(MiniAccumuloRunner.ZOO_KEEPER_PORT_PROP))
			config.setZooKeeperPort(Integer.parseInt(opts.prop.getProperty(MiniAccumuloRunner.ZOO_KEEPER_PORT_PROP)));

		if (opts.prop.containsKey(MiniAccumuloRunner.ZOO_KEEPER_STARTUP_TIME_PROP))
			config.setZooKeeperStartupTime(Long.parseLong(opts.prop.getProperty(MiniAccumuloRunner.ZOO_KEEPER_STARTUP_TIME_PROP)));

		if (opts.prop.containsKey(MiniAccumuloRunner.EXISTING_ZOO_KEEPERS_PROP)) {
		}
		if (opts.prop.containsKey(MiniAccumuloRunner.JDWP_ENABLED_PROP))
			config.setJDWPEnabled(Boolean.parseBoolean(opts.prop.getProperty(MiniAccumuloRunner.JDWP_ENABLED_PROP)));

		if (opts.prop.containsKey(MiniAccumuloRunner.ZOO_KEEPER_MEMORY_PROP))
			MiniAccumuloRunner.setMemoryOnConfig(config, opts.prop.getProperty(MiniAccumuloRunner.ZOO_KEEPER_MEMORY_PROP), ServerType.ZOOKEEPER);

		if (opts.prop.containsKey(MiniAccumuloRunner.TSERVER_MEMORY_PROP))
			MiniAccumuloRunner.setMemoryOnConfig(config, opts.prop.getProperty(MiniAccumuloRunner.TSERVER_MEMORY_PROP), ServerType.TABLET_SERVER);

		if (opts.prop.containsKey(MiniAccumuloRunner.MASTER_MEMORY_PROP))
			MiniAccumuloRunner.setMemoryOnConfig(config, opts.prop.getProperty(MiniAccumuloRunner.MASTER_MEMORY_PROP), ServerType.MASTER);

		if (opts.prop.containsKey(MiniAccumuloRunner.DEFAULT_MEMORY_PROP))
			MiniAccumuloRunner.setMemoryOnConfig(config, opts.prop.getProperty(MiniAccumuloRunner.DEFAULT_MEMORY_PROP));

		if (opts.prop.containsKey(MiniAccumuloRunner.SHUTDOWN_PORT_PROP))
			shutdownPort = Integer.parseInt(opts.prop.getProperty(MiniAccumuloRunner.SHUTDOWN_PORT_PROP));

		Map<String, String> siteConfig = new HashMap<>();
		for (Map.Entry<Object, Object> entry : opts.prop.entrySet()) {
			String key = ((String) (entry.getKey()));
			if (key.startsWith("site."))
				siteConfig.put(key.replaceFirst("site.", ""), ((String) (entry.getValue())));

		}
		config.setSiteConfig(siteConfig);
		final MiniAccumuloCluster accumulo = new MiniAccumuloCluster(config);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					accumulo.stop();
				} catch (IOException e) {
					MiniAccumuloRunner.log.error("IOException attempting to stop Accumulo.", e);
					return;
				} catch (InterruptedException e) {
					MiniAccumuloRunner.log.error("InterruptedException attempting to stop Accumulo.", e);
					return;
				}
				try {
					FileUtils.deleteDirectory(miniDir);
				} catch (IOException e) {
					MiniAccumuloRunner.log.error("IOException attempting to clean up miniDir.", e);
					return;
				}
				System.out.println(("\nShut down gracefully on " + (new Date())));
			}
		});
		accumulo.start();
		MiniAccumuloRunner.printInfo(accumulo, shutdownPort);
		try (ServerSocket shutdownServer = new ServerSocket(shutdownPort)) {
			shutdownServer.accept().close();
		}
		System.exit(0);
	}

	private static boolean validateMemoryString(String memoryString) {
		String unitsRegex = "[";
		MemoryUnit[] units = MemoryUnit.values();
		for (int i = 0; i < (units.length); i++) {
			unitsRegex += units[i].suffix();
			if (i < ((units.length) - 1))
				unitsRegex += "|";

		}
		unitsRegex += "]";
		Pattern p = Pattern.compile(("\\d+" + unitsRegex));
		return p.matcher(memoryString).matches();
	}

	private static void setMemoryOnConfig(MiniAccumuloConfig config, String memoryString) {
		MiniAccumuloRunner.setMemoryOnConfig(config, memoryString, null);
	}

	private static void setMemoryOnConfig(MiniAccumuloConfig config, String memoryString, ServerType serverType) {
		if (!(MiniAccumuloRunner.validateMemoryString(memoryString)))
			throw new IllegalArgumentException((memoryString + " is not a valid memory string"));

		long memSize = Long.parseLong(memoryString.substring(0, ((memoryString.length()) - 1)));
		MemoryUnit memUnit = MemoryUnit.fromSuffix(memoryString.substring(((memoryString.length()) - 1)));
		if (serverType != null)
			config.setMemory(serverType, memSize, memUnit);
		else
			config.setDefaultMemory(memSize, memUnit);

	}

	private static void printInfo(MiniAccumuloCluster accumulo, int shutdownPort) {
		System.out.println("Mini Accumulo Cluster\n");
		System.out.println(String.format(MiniAccumuloRunner.FORMAT_STRING, "Directory:", accumulo.getConfig().getDir().getAbsoluteFile()));
		System.out.println(String.format(MiniAccumuloRunner.FORMAT_STRING, "Instance Name:", accumulo.getConfig().getInstanceName()));
		System.out.println(String.format(MiniAccumuloRunner.FORMAT_STRING, "Root Password:", accumulo.getConfig().getRootPassword()));
		System.out.println(String.format(MiniAccumuloRunner.FORMAT_STRING, "ZooKeeper:", accumulo.getZooKeepers()));
		for (Pair<ServerType, Integer> pair : accumulo.getDebugPorts()) {
			System.out.println(String.format(MiniAccumuloRunner.FORMAT_STRING, ((pair.getFirst().prettyPrint()) + " JDWP Host:"), ("localhost:" + (pair.getSecond()))));
		}
		System.out.println(String.format(MiniAccumuloRunner.FORMAT_STRING, "Shutdown Port:", shutdownPort));
		System.out.println();
		System.out.println("  To connect with shell, use the following command : ");
		System.out.println((((("    accumulo shell -zh " + (accumulo.getZooKeepers())) + " -zi ") + (accumulo.getConfig().getInstanceName())) + " -u root "));
		System.out.println(("\n\nSuccessfully started on " + (new Date())));
	}
}

