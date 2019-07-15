

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.auto.service.AutoService;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import jline.Terminal;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.history.FileHistory;
import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.admin.InstanceOperations;
import org.apache.accumulo.core.client.admin.NamespaceOperations;
import org.apache.accumulo.core.client.admin.SecurityOperations;
import org.apache.accumulo.core.client.admin.TableOperations;
import org.apache.accumulo.core.client.impl.Tables;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.data.thrift.TConstraintViolationSummary;
import org.apache.accumulo.core.tabletserver.thrift.ConstraintViolationException;
import org.apache.accumulo.core.trace.DistributedTrace;
import org.apache.accumulo.core.util.BadArgumentException;
import org.apache.accumulo.core.util.DeprecationUtil;
import org.apache.accumulo.core.util.format.DefaultFormatter;
import org.apache.accumulo.core.util.format.Formatter;
import org.apache.accumulo.core.util.format.FormatterConfig;
import org.apache.accumulo.core.util.format.FormatterFactory;
import org.apache.accumulo.core.volume.VolumeConfiguration;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.shell.ShellCompletor;
import org.apache.accumulo.shell.ShellOptions;
import org.apache.accumulo.shell.ShellOptionsJC;
import org.apache.accumulo.shell.Token;
import org.apache.accumulo.shell.commands.OptUtil;
import org.apache.accumulo.shell.commands.QuotedStringTokenizer;
import org.apache.accumulo.start.classloader.vfs.AccumuloVFSClassLoader;
import org.apache.accumulo.start.classloader.vfs.ContextManager;
import org.apache.accumulo.start.spi.KeywordExecutable;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static org.apache.accumulo.core.client.ClientConfiguration.ClientProperty.INSTANCE_NAME;
import static org.apache.accumulo.core.client.ClientConfiguration.ClientProperty.INSTANCE_RPC_SASL_ENABLED;
import static org.apache.accumulo.core.client.ClientConfiguration.ClientProperty.INSTANCE_ZK_HOST;


@AutoService(KeywordExecutable.class)
public class Shell extends ShellOptions implements KeywordExecutable {
	public static final Logger log = Logger.getLogger(Shell.class);

	private static final Logger audit = Logger.getLogger(((Shell.class.getName()) + ".audit"));

	public static final Charset CHARSET = StandardCharsets.ISO_8859_1;

	public static final int NO_FIXED_ARG_LENGTH_CHECK = -1;

	public static final String COMMENT_PREFIX = "#";

	public static final String HISTORY_DIR_NAME = ".accumulo";

	public static final String HISTORY_FILE_NAME = "shell_history.txt";

	private static final String SHELL_DESCRIPTION = "Shell - Apache Accumulo Interactive Shell";

	protected int exitCode = 0;

	private String tableName;

	protected Instance instance;

	private Connector connector;

	protected ConsoleReader reader;

	private AuthenticationToken token;

	private final Class<? extends Formatter> defaultFormatterClass = DefaultFormatter.class;

	public Map<String, List<IteratorSetting>> scanIteratorOptions = new HashMap<>();

	public Map<String, List<IteratorSetting>> iteratorProfiles = new HashMap<>();

	private Token rootToken;

	public final Map<String, Shell.Command> commandFactory = new TreeMap<>();

	public final Map<String, Shell.Command[]> commandGrouping = new TreeMap<>();

	private boolean exit = false;

	protected File execFile = null;

	protected String execCommand = null;

	protected boolean verbose = true;

	private boolean tabCompletion;

	private boolean disableAuthTimeout;

	private long authTimeout;

	private long lastUserActivity = System.nanoTime();

	private boolean logErrorsToConsole = false;

	private boolean masking = false;

	{
		String prop = "input.encoding";
		if ((System.getProperty(prop)) == null) {
			String value = System.getProperty("jline.WindowsTerminal.output.encoding");
			if (value == null) {
				value = System.getProperty("file.encoding");
			}
			if (value != null) {
				System.setProperty(prop, value);
			}
		}
	}

	public Shell() {
	}

	public Shell(ConsoleReader reader) {
		super();
		this.reader = reader;
	}

	public boolean config(String... args) throws IOException {
		if ((this.reader) == null)
			this.reader = new ConsoleReader();

		ShellOptionsJC options = new ShellOptionsJC();
		JCommander jc = new JCommander();
		jc.setProgramName("accumulo shell");
		jc.addObject(options);
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			jc.usage();
			exitCode = 1;
			return false;
		}
		if (options.isHelpEnabled()) {
			jc.usage();
			exitCode = 0;
			return false;
		}
		if ((options.getUnrecognizedOptions()) != null) {
			logError(("Unrecognized Options: " + (options.getUnrecognizedOptions().toString())));
			jc.usage();
			exitCode = 1;
			return false;
		}
		Shell.setDebugging(options.isDebugEnabled());
		authTimeout = TimeUnit.MINUTES.toNanos(options.getAuthTimeout());
		disableAuthTimeout = options.isAuthTimeoutDisabled();
		ClientConfiguration clientConf;
		try {
			clientConf = options.getClientConfiguration();
		} catch (Exception e) {
			printException(e);
			return true;
		}
		if (Boolean.parseBoolean(clientConf.get(INSTANCE_RPC_SASL_ENABLED))) {
			Shell.log.debug("SASL is enabled, disabling authorization timeout");
			disableAuthTimeout = true;
		}
		final String user;
		try {
			user = options.getUsername();
		} catch (Exception e) {
			printException(e);
			return true;
		}
		String password = options.getPassword();
		tabCompletion = !(options.isTabCompletionDisabled());
		setInstance(options);
		try {
			token = options.getAuthenticationToken();
		} catch (Exception e) {
			printException(e);
			return true;
		}
		Map<String, String> loginOptions = options.getTokenProperties();
		try {
			final boolean hasToken = (token) != null;
			if (hasToken && (password != null)) {
				throw new ParameterException("Can not supply '--pass' option with '--tokenClass' option");
			}
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					reader.getTerminal().setEchoEnabled(true);
				}
			});
			if (hasToken) {
				AuthenticationToken.Properties props = new AuthenticationToken.Properties();
				if (!(loginOptions.isEmpty())) {
					props.putAllStrings(loginOptions);
				}
				token.init(props);
			}else {
				if (("stdin".equals(password)) || (password == null)) {
					password = reader.readLine("Password: ", '*');
				}
				if (password == null) {
					throw new ParameterException("No password or token option supplied");
				}else {
					this.token = new PasswordToken(password);
				}
			}
			if (!(options.isFake())) {
				DistributedTrace.enable(InetAddress.getLocalHost().getHostName(), "shell", clientConf);
			}
			this.setTableName("");
			connector = instance.getConnector(user, token);
		} catch (Exception e) {
			printException(e);
			exitCode = 1;
			return false;
		}
		if ((options.getExecFile()) != null) {
			execFile = options.getExecFile();
			verbose = false;
		}else
			if ((options.getExecFileVerbose()) != null) {
				execFile = options.getExecFileVerbose();
				verbose = true;
			}

		execCommand = options.getExecCommand();
		if ((execCommand) != null) {
			verbose = false;
		}
		rootToken = new Token();
		for (Shell.Command[] cmds : commandGrouping.values()) {
			for (Shell.Command cmd : cmds)
				commandFactory.put(cmd.getName(), cmd);

		}
		return true;
	}

	protected void setInstance(ShellOptionsJC options) {
		instance = null;
		if (options.isFake()) {
			instance = DeprecationUtil.makeMockInstance("fake");
		}else {
			String instanceName;
			String hosts;
			if (options.isHdfsZooInstance()) {
				instanceName = hosts = null;
			}else
				if ((options.getZooKeeperInstance().size()) > 0) {
					List<String> zkOpts = options.getZooKeeperInstance();
					instanceName = zkOpts.get(0);
					hosts = zkOpts.get(1);
				}else {
					instanceName = options.getZooKeeperInstanceName();
					hosts = options.getZooKeeperHosts();
				}

			final ClientConfiguration clientConf;
			try {
				clientConf = options.getClientConfiguration();
			} catch (ConfigurationException | FileNotFoundException e) {
				throw new IllegalArgumentException(("Unable to load client config from " + (options.getClientConfigFile())), e);
			}
			instance = Shell.getZooInstance(instanceName, hosts, clientConf);
		}
	}

	static String getZooKeepers(String keepers, ClientConfiguration clientConfig) {
		if (null != keepers) {
			return keepers;
		}
		if (clientConfig.containsKey(INSTANCE_ZK_HOST.getKey())) {
			return clientConfig.get(INSTANCE_ZK_HOST);
		}
		return SiteConfiguration.getInstance().get(Property.INSTANCE_ZK_HOST);
	}

	private static Instance getZooInstance(String instanceName, String keepersOption, ClientConfiguration clientConfig) {
		UUID instanceId = null;
		if (instanceName == null) {
			instanceName = clientConfig.get(INSTANCE_NAME);
		}
		String keepers = Shell.getZooKeepers(keepersOption, clientConfig);
		if (instanceName == null) {
			AccumuloConfiguration conf = SiteConfiguration.getInstance();
			Path instanceDir = new Path(VolumeConfiguration.getVolumeUris(conf)[0], "instance_id");
			instanceId = UUID.fromString(ZooUtil.getInstanceIDFromHdfs(instanceDir, conf));
		}
		if (instanceId != null) {
			return new ZooKeeperInstance(clientConfig.withInstance(instanceId).withZkHosts(keepers));
		}else {
			return new ZooKeeperInstance(clientConfig.withInstance(instanceName).withZkHosts(keepers));
		}
	}

	public Connector getConnector() {
		return connector;
	}

	public Instance getInstance() {
		return instance;
	}

	public ClassLoader getClassLoader(final CommandLine cl, final Shell shellState) throws IOException, AccumuloException, AccumuloSecurityException, TableNotFoundException, FileSystemException {
		boolean tables = (cl.hasOption(OptUtil.tableOpt().getOpt())) || (!(shellState.getTableName().isEmpty()));
		boolean namespaces = cl.hasOption(OptUtil.namespaceOpt().getOpt());
		String classpath = null;
		Iterable<Map.Entry<String, String>> tableProps;
		if (namespaces) {
		}else
			if (tables) {
			}else {
				throw new IllegalArgumentException("No table or namespace specified");
			}

		tableProps = null;
		for (Map.Entry<String, String> entry : tableProps) {
			if (entry.getKey().equals(Property.TABLE_CLASSPATH.getKey())) {
				classpath = entry.getValue();
			}
		}
		ClassLoader classloader;
		if ((classpath != null) && (!(classpath.equals("")))) {
			shellState.getConnector().instanceOperations().getSystemConfiguration().get(((Property.VFS_CONTEXT_CLASSPATH_PROPERTY.getKey()) + classpath));
			try {
				final Map<String, String> systemConfig = shellState.getConnector().instanceOperations().getSystemConfiguration();
				AccumuloVFSClassLoader.getContextManager().setContextConfig(new ContextManager.DefaultContextsConfig() {
					@Override
					public Map<String, String> getVfsContextClasspathProperties() {
						Map<String, String> filteredMap = new HashMap<>();
						for (Map.Entry<String, String> entry : systemConfig.entrySet()) {
							if (entry.getKey().startsWith(Property.VFS_CONTEXT_CLASSPATH_PROPERTY.getKey())) {
								filteredMap.put(entry.getKey(), entry.getValue());
							}
						}
						return filteredMap;
					}
				});
			} catch (IllegalStateException ise) {
			}
			classloader = AccumuloVFSClassLoader.getContextManager().getClassLoader(classpath);
		}else {
			classloader = AccumuloVFSClassLoader.getClassLoader();
		}
		return classloader;
	}

	@Override
	public String keyword() {
		return "shell";
	}

	@Override
	public void execute(final String[] args) throws IOException {
		try {
			if (!(config(args))) {
				System.exit(getExitCode());
			}
			System.exit(start());
		} finally {
			shutdown();
			DistributedTrace.disable();
		}
	}

	public static void main(String[] args) throws IOException {
		new Shell(new ConsoleReader()).execute(args);
	}

	public int start() throws IOException {
		String input;
		if (isVerbose())
			printInfo();

		String home = System.getProperty("HOME");
		if (home == null)
			home = System.getenv("HOME");

		String configDir = (home + "/") + (Shell.HISTORY_DIR_NAME);
		String historyPath = (configDir + "/") + (Shell.HISTORY_FILE_NAME);
		File accumuloDir = new File(configDir);
		if ((!(accumuloDir.exists())) && (!(accumuloDir.mkdirs())))
			Shell.log.warn(("Unable to make directory for history at " + accumuloDir));

		try {
			final FileHistory history = new FileHistory(new File(historyPath));
			reader.setHistory(history);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						history.flush();
					} catch (IOException e) {
						Shell.log.warn("Could not flush history to file.");
					}
				}
			});
		} catch (IOException e) {
			Shell.log.warn(("Unable to load history file at " + historyPath));
		}
		reader.setHandleUserInterrupt(true);
		ShellCompletor userCompletor = null;
		if ((execFile) != null) {
			Scanner scanner = new Scanner(execFile, StandardCharsets.UTF_8.name());
			try {
				while ((scanner.hasNextLine()) && (!(hasExited()))) {
					execCommand(scanner.nextLine(), true, isVerbose());
				} 
			} finally {
				scanner.close();
			}
		}else
			if ((execCommand) != null) {
				for (String command : execCommand.split("\n")) {
					execCommand(command, true, isVerbose());
				}
				return exitCode;
			}

		while (true) {
			try {
				if (hasExited())
					return exitCode;

				if (tabCompletion) {
					if (userCompletor != null)
						reader.removeCompleter(userCompletor);

					userCompletor = setupCompletion();
					reader.addCompleter(userCompletor);
				}
				reader.setPrompt(getDefaultPrompt());
				input = reader.readLine();
				if (input == null) {
					reader.println();
					return exitCode;
				}
				execCommand(input, disableAuthTimeout, false);
			} catch (UserInterruptException uie) {
				reader.println();
				String partialLine = uie.getPartialLine();
				if ((partialLine == null) || ("".equals(uie.getPartialLine().trim()))) {
					return exitCode;
				}
			} finally {
				reader.flush();
			}
		} 
	}

	public void shutdown() {
		if ((reader) != null) {
			reader.shutdown();
		}
	}

	public void printInfo() throws IOException {
		reader.print(((((((((((((((("\n" + (Shell.SHELL_DESCRIPTION)) + "\n") + "- \n") + "- version: ") + (Constants.VERSION)) + "\n") + "- instance name: ") + (connector.getInstance().getInstanceName())) + "\n") + "- instance id: ") + (connector.getInstance().getInstanceID())) + "\n") + "- \n") + "- type \'help\' for a list of available commands\n") + "- \n"));
		reader.flush();
	}

	public void printVerboseInfo() throws IOException {
		StringBuilder sb = new StringBuilder("-\n");
		sb.append("- Current user: ").append(connector.whoami()).append("\n");
		if ((execFile) != null)
			sb.append("- Executing commands from: ").append(execFile).append("\n");

		if (disableAuthTimeout)
			sb.append("- Authorization timeout: disabled\n");
		else
			sb.append("- Authorization timeout: ").append(String.format("%ds%n", TimeUnit.NANOSECONDS.toSeconds(authTimeout)));

		sb.append("- Debug: ").append((Shell.isDebuggingEnabled() ? "on" : "off")).append("\n");
		if (!(scanIteratorOptions.isEmpty())) {
			for (Map.Entry<String, List<IteratorSetting>> entry : scanIteratorOptions.entrySet()) {
				sb.append("- Session scan iterators for table ").append(entry.getKey()).append(":\n");
				for (IteratorSetting setting : entry.getValue()) {
					sb.append("-    Iterator ").append(setting.getName()).append(" options:\n");
					sb.append("-        ").append("iteratorPriority").append(" = ").append(setting.getPriority()).append("\n");
					sb.append("-        ").append("iteratorClassName").append(" = ").append(setting.getIteratorClass()).append("\n");
					for (Map.Entry<String, String> optEntry : setting.getOptions().entrySet()) {
						sb.append("-        ").append(optEntry.getKey()).append(" = ").append(optEntry.getValue()).append("\n");
					}
				}
			}
		}
		sb.append("-\n");
		reader.print(sb.toString());
	}

	public String getDefaultPrompt() {
		return (((((connector.whoami()) + "@") + (connector.getInstance().getInstanceName())) + (getTableName().isEmpty() ? "" : " ")) + (getTableName())) + "> ";
	}

	public void execCommand(String input, boolean ignoreAuthTimeout, boolean echoPrompt) throws IOException {
		Shell.audit.log(Level.INFO, ((getDefaultPrompt()) + input));
		if (echoPrompt) {
			reader.print(getDefaultPrompt());
			reader.println(input);
		}
		if (input.startsWith(Shell.COMMENT_PREFIX)) {
			return;
		}
		String[] fields;
		try {
			fields = new QuotedStringTokenizer(input).getTokens();
		} catch (BadArgumentException e) {
			printException(e);
			++(exitCode);
			return;
		}
		if ((fields.length) == 0)
			return;

		String command = fields[0];
		fields = ((fields.length) > 1) ? Arrays.copyOfRange(fields, 1, fields.length) : new String[]{  };
		Shell.Command sc = null;
		if ((command.length()) > 0) {
			try {
				sc = commandFactory.get(command);
				if (sc == null) {
					reader.println(String.format("Unknown command \"%s\".  Enter \"help\" for a list possible commands.", command));
					reader.flush();
					return;
				}
				long duration = (System.nanoTime()) - (lastUserActivity);
				Options parseOpts = sc.getOptionsWithHelp();
				CommandLine cl = new BasicParser().parse(parseOpts, fields);
				int actualArgLen = cl.getArgs().length;
				int expectedArgLen = sc.numArgs();
				if (cl.hasOption(ShellOptions.helpOption)) {
					sc.printHelp(this);
				}else
					if ((expectedArgLen != (Shell.NO_FIXED_ARG_LENGTH_CHECK)) && (actualArgLen != expectedArgLen)) {
						++(exitCode);
						printException(new IllegalArgumentException(String.format("Expected %d argument%s. There %s %d.", expectedArgLen, (expectedArgLen == 1 ? "" : "s"), (actualArgLen == 1 ? "was" : "were"), actualArgLen)));
						sc.printHelp(this);
					}else {
						int tmpCode = sc.execute(input, cl, this);
						exitCode += tmpCode;
						reader.flush();
					}

			} catch (ConstraintViolationException e) {
				++(exitCode);
				printConstraintViolationException(e);
			} catch (TableNotFoundException e) {
				++(exitCode);
				if (getTableName().equals(e.getTableName()))
					setTableName("");

				printException(e);
			} catch (ParseException e) {
				if (!((e instanceof MissingOptionException) && ((Arrays.asList(fields).contains(("-" + (ShellOptions.helpOption)))) || (Arrays.asList(fields).contains(("--" + (ShellOptions.helpLongOption))))))) {
					++(exitCode);
					printException(e);
				}
				if (sc != null)
					sc.printHelp(this);

			} catch (UserInterruptException e) {
				++(exitCode);
			} catch (Exception e) {
				++(exitCode);
				printException(e);
			}
		}else {
			++(exitCode);
			printException(new BadArgumentException("Unrecognized empty command", command, (-1)));
		}
		reader.flush();
	}

	private ShellCompletor setupCompletion() {
		rootToken = new Token();
		Set<String> tableNames = null;
		try {
			tableNames = connector.tableOperations().list();
		} catch (Exception e) {
			Shell.log.debug("Unable to obtain list of tables", e);
			tableNames = Collections.emptySet();
		}
		Set<String> userlist = null;
		try {
			userlist = connector.securityOperations().listLocalUsers();
		} catch (Exception e) {
			Shell.log.debug("Unable to obtain list of users", e);
			userlist = Collections.emptySet();
		}
		Set<String> namespaces = null;
		try {
			namespaces = connector.namespaceOperations().list();
		} catch (Exception e) {
			Shell.log.debug("Unable to obtain list of namespaces", e);
			namespaces = Collections.emptySet();
		}
		Map<Shell.Command.CompletionSet, Set<String>> options = new HashMap<>();
		Set<String> commands = new HashSet<>();
		for (String a : commandFactory.keySet())
			commands.add(a);

		Set<String> modifiedUserlist = new HashSet<>();
		Set<String> modifiedTablenames = new HashSet<>();
		Set<String> modifiedNamespaces = new HashSet<>();
		for (String a : tableNames)
			modifiedTablenames.add(a.replaceAll("([\\s\'\"])", "\\\\$1"));

		for (String a : userlist)
			modifiedUserlist.add(a.replaceAll("([\\s\'\"])", "\\\\$1"));

		for (String a : namespaces) {
			String b = a.replaceAll("([\\s\'\"])", "\\\\$1");
			modifiedNamespaces.add((b.isEmpty() ? "\"\"" : b));
		}
		options.put(Shell.Command.CompletionSet.USERNAMES, modifiedUserlist);
		options.put(Shell.Command.CompletionSet.TABLENAMES, modifiedTablenames);
		options.put(Shell.Command.CompletionSet.NAMESPACES, modifiedNamespaces);
		options.put(Shell.Command.CompletionSet.COMMANDS, commands);
		for (Shell.Command[] cmdGroup : commandGrouping.values()) {
			for (Shell.Command c : cmdGroup) {
				c.getOptionsWithHelp();
				c.registerCompletion(rootToken, options);
			}
		}
		return null;
	}

	public static abstract class Command {
		public enum CompletionSet {

			TABLENAMES,
			USERNAMES,
			COMMANDS,
			NAMESPACES;}

		public void registerCompletionGeneral(Token root, Set<String> args, boolean caseSens) {
			Token t = new Token(args);
			t.setCaseSensitive(caseSens);
			Token command = new Token(getName());
			command.addSubcommand(t);
			root.addSubcommand(command);
		}

		public void registerCompletionForTables(Token root, Map<Shell.Command.CompletionSet, Set<String>> completionSet) {
			registerCompletionGeneral(root, completionSet.get(Shell.Command.CompletionSet.TABLENAMES), true);
		}

		public void registerCompletionForUsers(Token root, Map<Shell.Command.CompletionSet, Set<String>> completionSet) {
			registerCompletionGeneral(root, completionSet.get(Shell.Command.CompletionSet.USERNAMES), true);
		}

		public void registerCompletionForCommands(Token root, Map<Shell.Command.CompletionSet, Set<String>> completionSet) {
			registerCompletionGeneral(root, completionSet.get(Shell.Command.CompletionSet.COMMANDS), false);
		}

		public void registerCompletionForNamespaces(Token root, Map<Shell.Command.CompletionSet, Set<String>> completionSet) {
			registerCompletionGeneral(root, completionSet.get(Shell.Command.CompletionSet.NAMESPACES), true);
		}

		public abstract int execute(String fullCommand, CommandLine cl, Shell shellState) throws Exception;

		public abstract String description();

		public abstract int numArgs();

		public String getName() {
			String s = this.getClass().getName();
			int st = Math.max(s.lastIndexOf('$'), s.lastIndexOf('.'));
			int i = s.indexOf("Command");
			return i > 0 ? s.substring((st + 1), i).toLowerCase(Locale.ENGLISH) : null;
		}

		public void registerCompletion(Token root, Map<Shell.Command.CompletionSet, Set<String>> completion_set) {
			root.addSubcommand(new Token(getName()));
		}

		public final void printHelp(Shell shellState) throws IOException {
			shellState.printHelp(usage(), ("description: " + (this.description())), getOptionsWithHelp());
		}

		public final void printHelp(Shell shellState, int width) throws IOException {
			shellState.printHelp(usage(), ("description: " + (this.description())), getOptionsWithHelp(), width);
		}

		public final Options getOptionsWithHelp() {
			Options opts = getOptions();
			opts.addOption(new Option(ShellOptions.helpOption, ShellOptions.helpLongOption, false, "display this help"));
			return opts;
		}

		public String usage() {
			return getName();
		}

		public Options getOptions() {
			return new Options();
		}
	}

	public interface PrintLine extends AutoCloseable {
		void print(String s);

		@Override
		void close();
	}

	public static class PrintShell implements Shell.PrintLine {
		ConsoleReader reader;

		public PrintShell(ConsoleReader reader) {
			this.reader = reader;
		}

		@Override
		public void print(String s) {
			try {
				reader.println(s);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public void close() {
		}
	}

	public static class PrintFile implements Shell.PrintLine {
		PrintWriter writer;

		public PrintFile(String filename) throws FileNotFoundException {
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8)));
		}

		@Override
		public void print(String s) {
			writer.println(s);
		}

		@Override
		public void close() {
			writer.close();
		}
	}

	public final void printLines(Iterator<String> lines, boolean paginate) throws IOException {
		printLines(lines, paginate, null);
	}

	public final void printLines(Iterator<String> lines, boolean paginate, Shell.PrintLine out) throws IOException {
		int linesPrinted = 0;
		String prompt = "-- hit any key to continue or 'q' to quit --";
		int lastPromptLength = prompt.length();
		int termWidth = reader.getTerminal().getWidth();
		int maxLines = reader.getTerminal().getHeight();
		String peek = null;
		while (lines.hasNext()) {
			String nextLine = lines.next();
			if (nextLine == null)
				continue;

			for (String line : nextLine.split("\\n")) {
				if (out == null) {
					if (peek != null) {
						reader.println(peek);
						if (paginate) {
							linesPrinted += ((peek.length()) == 0) ? 0 : Math.ceil((((peek.length()) * 1.0) / termWidth));
							if ((((linesPrinted + (Math.ceil(((lastPromptLength * 1.0) / termWidth)))) + (Math.ceil((((prompt.length()) * 1.0) / termWidth)))) + (Math.ceil((((line.length()) * 1.0) / termWidth)))) > maxLines) {
								linesPrinted = 0;
								int numdashes = (termWidth - (prompt.length())) / 2;
								String nextPrompt = ((Shell.repeat("-", numdashes)) + prompt) + (Shell.repeat("-", numdashes));
								lastPromptLength = nextPrompt.length();
								reader.print(nextPrompt);
								reader.flush();
								if ((Character.toUpperCase(((char) (reader.readCharacter())))) == 'Q') {
									reader.println();
									return;
								}
								reader.println();
								termWidth = reader.getTerminal().getWidth();
								maxLines = reader.getTerminal().getHeight();
							}
						}
					}
					peek = line;
				}else {
					out.print(line);
				}
			}
		} 
		if ((out == null) && (peek != null)) {
			reader.println(peek);
		}
	}

	public final void printRecords(Iterable<Map.Entry<Key, Value>> scanner, FormatterConfig config, boolean paginate, Class<? extends Formatter> formatterClass, Shell.PrintLine outFile) throws IOException {
		printLines(FormatterFactory.getFormatter(formatterClass, scanner, config), paginate, outFile);
	}

	public final void printRecords(Iterable<Map.Entry<Key, Value>> scanner, FormatterConfig config, boolean paginate, Class<? extends Formatter> formatterClass) throws IOException {
		printLines(FormatterFactory.getFormatter(formatterClass, scanner, config), paginate);
	}

	public static String repeat(String s, int c) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < c; i++)
			sb.append(s);

		return sb.toString();
	}

	public void checkTableState() {
		if (getTableName().isEmpty())
			throw new IllegalStateException(("Not in a table context. Please use" + (" 'table <tableName>' to switch to a table, or use '-t' to specify a" + " table if option is available.")));

	}

	private final void printConstraintViolationException(ConstraintViolationException cve) {
		printException(cve, "");
		int COL1 = 50;
		int COL2 = 14;
		int col3 = Math.max(1, Math.min(Integer.MAX_VALUE, ((((reader.getTerminal().getWidth()) - COL1) - COL2) - 6)));
		logError(String.format((((((("%" + COL1) + "s-+-%") + COL2) + "s-+-%") + col3) + "s%n"), Shell.repeat("-", COL1), Shell.repeat("-", COL2), Shell.repeat("-", col3)));
		logError(String.format((((((("%-" + COL1) + "s | %") + COL2) + "s | %-") + col3) + "s%n"), "Constraint class", "Violation code", "Violation Description"));
		logError(String.format((((((("%" + COL1) + "s-+-%") + COL2) + "s-+-%") + col3) + "s%n"), Shell.repeat("-", COL1), Shell.repeat("-", COL2), Shell.repeat("-", col3)));
		for (TConstraintViolationSummary cvs : cve.violationSummaries)
			logError(String.format((((((("%-" + COL1) + "s | %") + COL2) + "d | %-") + col3) + "s%n"), cvs.constrainClass, cvs.violationCode, cvs.violationDescription));

		logError(String.format((((((("%" + COL1) + "s-+-%") + COL2) + "s-+-%") + col3) + "s%n"), Shell.repeat("-", COL1), Shell.repeat("-", COL2), Shell.repeat("-", col3)));
	}

	public final void printException(Exception e) {
		printException(e, e.getMessage());
	}

	private final void printException(Exception e, String msg) {
		logError(((e.getClass().getName()) + (msg != null ? ": " + msg : "")));
		Shell.log.debug(((e.getClass().getName()) + (msg != null ? ": " + msg : "")), e);
	}

	public static final void setDebugging(boolean debuggingEnabled) {
		Logger.getLogger(Constants.CORE_PACKAGE_NAME).setLevel((debuggingEnabled ? Level.TRACE : Level.INFO));
		Logger.getLogger(Shell.class.getPackage().getName()).setLevel((debuggingEnabled ? Level.TRACE : Level.INFO));
	}

	public static final boolean isDebuggingEnabled() {
		return Logger.getLogger(Constants.CORE_PACKAGE_NAME).isTraceEnabled();
	}

	private final void printHelp(String usage, String description, Options opts) throws IOException {
		printHelp(usage, description, opts, Integer.MAX_VALUE);
	}

	private final void printHelp(String usage, String description, Options opts, int width) throws IOException {
		new HelpFormatter().printHelp(new PrintWriter(reader.getOutput()), width, usage, description, opts, 2, 5, null, true);
		reader.getOutput().flush();
	}

	public int getExitCode() {
		return exitCode;
	}

	public void resetExitCode() {
		exitCode = 0;
	}

	public void setExit(boolean exit) {
		this.exit = exit;
	}

	public boolean getExit() {
		return this.exit;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setTableName(String tableName) {
		this.tableName = ((tableName == null) || (tableName.isEmpty())) ? "" : Tables.qualified(tableName);
	}

	public String getTableName() {
		return tableName;
	}

	public ConsoleReader getReader() {
		return reader;
	}

	public void updateUser(String principal, AuthenticationToken token) throws AccumuloException, AccumuloSecurityException {
		connector = instance.getConnector(principal, token);
		this.token = token;
	}

	public String getPrincipal() {
		return connector.whoami();
	}

	public AuthenticationToken getToken() {
		return token;
	}

	public Class<? extends Formatter> getFormatter() {
		return getFormatter(this.tableName);
	}

	public Class<? extends Formatter> getFormatter(String tableName) {
		return null;
	}

	public void setLogErrorsToConsole() {
		this.logErrorsToConsole = true;
	}

	private void logError(String s) {
		Shell.log.error(s);
		if (logErrorsToConsole) {
			try {
				reader.println(("ERROR: " + s));
				reader.flush();
			} catch (IOException e) {
			}
		}
	}

	public String readMaskedLine(String prompt, Character mask) throws IOException {
		this.masking = true;
		String s = reader.readLine(prompt, mask);
		this.masking = false;
		return s;
	}

	public boolean isMasking() {
		return masking;
	}

	public boolean hasExited() {
		return exit;
	}

	public boolean isTabCompletion() {
		return tabCompletion;
	}
}

