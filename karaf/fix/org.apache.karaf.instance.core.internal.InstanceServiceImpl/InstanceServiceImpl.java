

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import org.apache.felix.utils.properties.InterpolationHelper;
import org.apache.felix.utils.properties.Properties;
import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstanceService;
import org.apache.karaf.instance.core.InstanceSettings;
import org.apache.karaf.instance.core.internal.InstanceImpl;
import org.apache.karaf.instance.main.Execute;
import org.apache.karaf.jpm.Process;
import org.apache.karaf.jpm.ProcessBuilder;
import org.apache.karaf.jpm.impl.ProcessBuilderFactoryImpl;
import org.apache.karaf.jpm.impl.ScriptUtils;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.apache.karaf.util.StreamUtils;
import org.apache.karaf.util.config.PropertiesLoader;
import org.apache.karaf.util.locks.FileLockUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InstanceServiceImpl implements InstanceService {
	public static final String STORAGE_FILE = "instance.properties";

	public static final String BACKUP_EXTENSION = ".bak";

	private static final String FEATURES_CFG = "etc/org.apache.karaf.features.cfg";

	private static final String RESOURCE_BASE = "org/apache/karaf/instance/resources/";

	private static final Logger LOGGER = LoggerFactory.getLogger(InstanceServiceImpl.class);

	private static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";

	private static final String KARAF_SHUTDOWN_PORT = "karaf.shutdown.port";

	private static final String KARAF_SHUTDOWN_HOST = "karaf.shutdown.host";

	private static final String KARAF_SHUTDOWN_PORT_FILE = "karaf.shutdown.port.file";

	private static final String KARAF_SHUTDOWN_COMMAND = "karaf.shutdown.command";

	private static final String KARAF_SHUTDOWN_TIMEOUT = "karaf.shutdown.timeout";

	private static final String DEFAULT_SHUTDOWN_COMMAND = "SHUTDOWN";

	public static final String DEFAULT_JAVA_OPTS = "-server -Xmx512M -Dcom.sun.management.jmxremote -XX:+UnlockDiagnosticVMOptions";

	private LinkedHashMap<String, InstanceImpl> proxies = new LinkedHashMap<>();

	private File storageLocation;

	private long stopTimeout = 30000;

	static class InstanceState {
		String name;

		String loc;

		String opts;

		int pid;

		boolean root;
	}

	static class State {
		int defaultSshPortStart = 8101;

		int defaultRmiRegistryPortStart = 1099;

		int defaultRmiServerPortStart = 44444;

		Map<String, InstanceServiceImpl.InstanceState> instances;

		public State() {
			try {
				TypedProperties shellProperty = new TypedProperties();
				shellProperty.load(new File(System.getProperty("karaf.etc"), "org.apache.karaf.shell.cfg"));
				defaultSshPortStart = InstanceServiceImpl.getInt(shellProperty, "sshPort", 8101);
				TypedProperties managementProperty = new TypedProperties();
				managementProperty.load(new File(System.getProperty("karaf.etc"), "org.apache.karaf.management.cfg"));
				defaultRmiRegistryPortStart = InstanceServiceImpl.getInt(managementProperty, "rmiRegistryPort", 1099);
				defaultRmiServerPortStart = InstanceServiceImpl.getInt(managementProperty, "rmiServerPort", 1099);
			} catch (Exception e) {
				InstanceServiceImpl.LOGGER.debug("Could not read port start value from the root instance configuration.", e);
			}
		}
	}

	public InstanceServiceImpl() {
		String prop = System.getProperty("karaf.instances");
		if (prop != null) {
			storageLocation = new File(prop);
		}
	}

	public File getStorageLocation() {
		return storageLocation;
	}

	public void setStorageLocation(File storage) {
		this.storageLocation = storage;
	}

	public long getStopTimeout() {
		return stopTimeout;
	}

	public void setStopTimeout(long stopTimeout) {
		this.stopTimeout = stopTimeout;
	}

	private InstanceServiceImpl.State loadData(TypedProperties storage) {
		InstanceServiceImpl.State state = new InstanceServiceImpl.State();
		int count = InstanceServiceImpl.getInt(storage, "count", 0);
		state.defaultSshPortStart = InstanceServiceImpl.getInt(storage, "ssh.port", state.defaultSshPortStart);
		state.defaultRmiRegistryPortStart = InstanceServiceImpl.getInt(storage, "rmi.registry.port", state.defaultRmiRegistryPortStart);
		state.defaultRmiServerPortStart = InstanceServiceImpl.getInt(storage, "rmi.server.port", state.defaultRmiServerPortStart);
		state.instances = new LinkedHashMap<>();
		for (int i = 0; i < count; i++) {
			InstanceServiceImpl.InstanceState instance = new InstanceServiceImpl.InstanceState();
			instance.name = InstanceServiceImpl.getString(storage, (("item." + i) + ".name"), null);
			instance.loc = InstanceServiceImpl.getString(storage, (("item." + i) + ".loc"), null);
			instance.opts = InstanceServiceImpl.getString(storage, (("item." + i) + ".opts"), null);
			instance.pid = InstanceServiceImpl.getInt(storage, (("item." + i) + ".pid"), 0);
			instance.root = InstanceServiceImpl.getBool(storage, (("item." + i) + ".root"), false);
			state.instances.put(instance.name, instance);
		}
		for (InstanceServiceImpl.InstanceState instance : state.instances.values()) {
			if (!(this.proxies.containsKey(instance.name))) {
			}
		}
		List<String> names = new ArrayList<>(this.proxies.keySet());
		for (String name : names) {
			if (!(state.instances.containsKey(name))) {
				this.proxies.remove(name);
			}
		}
		return state;
	}

	private void saveData(InstanceServiceImpl.State state, TypedProperties storage) {
		storage.put("ssh.port", Integer.toString(state.defaultSshPortStart));
		storage.put("rmi.registry.port", Integer.toString(state.defaultRmiRegistryPortStart));
		storage.put("rmi.server.port", Integer.toString(state.defaultRmiServerPortStart));
		storage.put("count", Integer.toString(state.instances.size()));
		int i = 0;
		for (InstanceServiceImpl.InstanceState instance : state.instances.values()) {
			storage.put((("item." + i) + ".name"), instance.name);
			storage.put((("item." + i) + ".root"), Boolean.toString(instance.root));
			storage.put((("item." + i) + ".loc"), instance.loc);
			storage.put((("item." + i) + ".pid"), Integer.toString(instance.pid));
			storage.put((("item." + i) + ".opts"), ((instance.opts) != null ? instance.opts : ""));
			i++;
		}
		while (storage.containsKey((("item." + i) + ".name"))) {
			storage.remove((("item." + i) + ".name"));
			storage.remove((("item." + i) + ".root"));
			storage.remove((("item." + i) + ".loc"));
			storage.remove((("item." + i) + ".pid"));
			storage.remove((("item." + i) + ".opts"));
			i++;
		} 
	}

	private static boolean getBool(TypedProperties storage, String name, boolean def) {
		Object value = storage.get(name);
		if (value instanceof Boolean) {
			return ((Boolean) (value));
		}else
			if (value != null) {
				return Boolean.parseBoolean(value.toString());
			}else {
				return def;
			}

	}

	private static int getInt(TypedProperties storage, String name, int def) {
		Object value = storage.get(name);
		if (value instanceof Number) {
			return ((Number) (value)).intValue();
		}else
			if (value != null) {
				return Integer.parseInt(value.toString());
			}else {
				return def;
			}

	}

	private static String getString(TypedProperties storage, String name, String def) {
		Object value = storage.get(name);
		return value != null ? value.toString() : def;
	}

	interface Task<U, T> {
		T call(U state) throws IOException;
	}

	synchronized <T> T execute(final InstanceServiceImpl.Task<InstanceServiceImpl.State, T> callback, final boolean writeToFile) {
		final File storageFile = new File(storageLocation, InstanceServiceImpl.STORAGE_FILE);
		if (!(storageFile.exists())) {
			storageFile.getParentFile().mkdirs();
			try {
				storageFile.createNewFile();
			} catch (IOException e) {
			}
		}
		if (storageFile.exists()) {
			if (!(storageFile.isFile())) {
				throw new IllegalStateException(("Instance storage location should be a file: " + storageFile));
			}
			try {
				return FileLockUtils.execute(storageFile, ( properties) -> {
					InstanceServiceImpl.State state = loadData(properties);
					T t = callback.call(state);
					if (writeToFile) {
						saveData(state, properties);
					}
					return t;
				}, writeToFile);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}else {
			throw new IllegalStateException(("Instance storage location does not exist: " + storageFile));
		}
	}

	private static void logInfo(String message, boolean printOutput, Object... args) {
		if ((InstanceServiceImpl.LOGGER.isInfoEnabled()) || printOutput) {
			String formatted = String.format(message, args);
			InstanceServiceImpl.LOGGER.info(formatted);
			if (printOutput) {
				InstanceServiceImpl.println(formatted);
			}
		}
	}

	private static void logDebug(String message, boolean printOutput, Object... args) {
		if ((InstanceServiceImpl.LOGGER.isDebugEnabled()) || printOutput) {
			String formatted = String.format(message, args);
			InstanceServiceImpl.LOGGER.debug(formatted);
			if (printOutput) {
				InstanceServiceImpl.println(formatted);
			}
		}
	}

	public synchronized Instance createInstance(final String name, final InstanceSettings settings, final boolean printOutput) throws Exception {
		return null;
	}

	void addFeaturesFromSettings(File featuresCfg, final InstanceSettings settings) throws IOException {
		FileLockUtils.execute(featuresCfg, ( properties) -> {
			InstanceServiceImpl.appendToPropList(properties, "featuresBoot", settings.getFeatures());
			InstanceServiceImpl.appendToPropList(properties, "featuresRepositories", settings.getFeatureURLs());
		}, true);
	}

	private static void appendToPropList(TypedProperties p, String key, List<String> elements) {
		if (elements == null) {
			return;
		}
		StringBuilder sb = new StringBuilder(p.get(key).toString().trim());
		for (String f : elements) {
			if ((sb.length()) > 0) {
				sb.append(',');
			}
			sb.append(f);
		}
		p.put(key, sb.toString());
	}

	public Instance[] getInstances() {
		return execute(( state) -> proxies.values().toArray(new Instance[proxies.size()]), false);
	}

	public Instance getInstance(final String name) {
		return execute(( state) -> proxies.get(name), false);
	}

	public void startInstance(final String name, final String javaOpts) {
		execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			checkPid(instance);
			if ((instance.pid) != 0) {
				throw new IllegalStateException("Instance already started");
			}
			doStart(instance, name, javaOpts);
			return null;
		}, true);
	}

	private void doStart(InstanceServiceImpl.InstanceState instance, String name, String javaOpts) throws IOException {
		String opts = javaOpts;
		if ((opts == null) || ((opts.length()) == 0)) {
			opts = instance.opts;
		}
		if ((opts == null) || ((opts.length()) == 0)) {
			opts = InstanceServiceImpl.DEFAULT_JAVA_OPTS;
		}
		String karafOptsEnv = System.getenv("KARAF_OPTS");
		String karafOpts = System.getProperty("karaf.opts", (karafOptsEnv != null ? karafOptsEnv : ""));
		String location = instance.loc;
		File libDir = new File(System.getProperty("karaf.home"), "lib");
		File bootLibDir = new File(libDir, "boot");
		File childLibDir = new File(location, "lib");
		StringBuilder classpath = classpathFromLibDir(bootLibDir);
		StringBuilder childClasspath = classpathFromLibDir(childLibDir);
		if (((childClasspath.length()) > 0) && (!(bootLibDir.equals(childLibDir)))) {
			classpath.append(System.getProperty("path.separator"));
			classpath.append(childClasspath);
		}
		String jdkOpts;
		if (!(System.getProperty("java.version").startsWith("1."))) {
			jdkOpts = " --add-opens java.base/java.security=ALL-UNNAMED" + (((((((((((" --add-opens java.base/java.net=ALL-UNNAMED" + " --add-opens java.base/java.lang=ALL-UNNAMED") + " --add-opens java.base/java.util=ALL-UNNAMED") + " --add-opens java.naming/javax.naming.spi=ALL-UNNAMED") + " --add-opens java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED") + " --add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED") + " --add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED") + " --add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED") + " --add-exports=java.xml.bind/com.sun.xml.internal.bind.v2.runtime=ALL-UNNAMED") + " --add-exports=jdk.xml.dom/org.w3c.dom.html=ALL-UNNAMED") + " --add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED") + " --add-modules java.xml.ws.annotation,java.corba,java.transaction,java.xml.bind,java.xml.ws");
		}else {
			jdkOpts = ((((((((((((" -Djava.endorsed.dirs=\"" + (new File(new File(new File(System.getProperty("java.home"), "jre"), "lib"), "endorsed"))) + (System.getProperty("path.separator"))) + (new File(new File(System.getProperty("java.home"), "lib"), "endorsed"))) + (System.getProperty("path.separator"))) + (new File(libDir, "endorsed").getCanonicalPath())) + "\"") + " -Djava.ext.dirs=\"") + (new File(new File(new File(System.getProperty("java.home"), "jre"), "lib"), "ext"))) + (System.getProperty("path.separator"))) + (new File(new File(System.getProperty("java.home"), "lib"), "ext"))) + (System.getProperty("path.separator"))) + (new File(libDir, "ext").getCanonicalPath())) + "\"";
		}
		String command = ((((((((((((((((((((((((((((((((("\"" + (new File(System.getProperty("java.home"), (ScriptUtils.isWindows() ? "bin\\java.exe" : "bin/java")).getCanonicalPath())) + "\" ") + opts) + " ") + karafOpts) + " ") + jdkOpts) + " -Djava.util.logging.config.file=\"") + (new File(location, "etc/java.util.logging.properties").getCanonicalPath())) + "\"") + " -Dkaraf.home=\"") + (System.getProperty("karaf.home"))) + "\"") + " -Dkaraf.base=\"") + (new File(location).getCanonicalPath())) + "\"") + " -Dkaraf.data=\"") + (new File(new File(location).getCanonicalPath(), "data"))) + "\"") + " -Dkaraf.etc=\"") + (new File(new File(location).getCanonicalPath(), "etc"))) + "\"") + " -Dkaraf.log=\"") + (new File(new File(new File(location).getCanonicalFile(), "data"), "log"))) + "\"") + " -Djava.io.tmpdir=\"") + (new File(new File(location).getCanonicalPath(), (("data" + (File.separator)) + "tmp")))) + "\"") + " -Dkaraf.startLocalConsole=false") + " -Dkaraf.startRemoteShell=true") + " -classpath \"") + (classpath.toString())) + "\"") + " org.apache.karaf.main.Main server";
		if (((System.getenv("KARAF_REDIRECT")) != null) && (!(System.getenv("KARAF_REDIRECT").isEmpty()))) {
			command = (command + " >> ") + (System.getenv("KARAF_REDIRECT"));
		}
		InstanceServiceImpl.LOGGER.debug(((("Starting instance " + name) + " with command: ") + command));
		Process process = new ProcessBuilderFactoryImpl().newBuilder().directory(new File(location)).command(command).start();
		instance.pid = process.getPid();
	}

	private StringBuilder classpathFromLibDir(File libDir) throws IOException {
		File[] jars = libDir.listFiles(( dir, name) -> name.endsWith(".jar"));
		StringBuilder classpath = new StringBuilder();
		if (jars != null) {
			for (File jar : jars) {
				if ((classpath.length()) > 0) {
					classpath.append(System.getProperty("path.separator"));
				}
				classpath.append(jar.getCanonicalPath());
			}
		}
		return classpath;
	}

	private void addJar(StringBuilder sb, String groupId, String artifactId) {
		File artifactDir = new File(((((((((System.getProperty("karaf.home")) + (File.separator)) + "system") + (File.separator)) + (groupId.replaceAll("\\.", File.separator))) + (File.separator)) + artifactId) + (File.separator)));
		TreeMap<String, File> jars = new TreeMap<>();
		String[] versions = artifactDir.list();
		if (versions != null) {
			for (String version : versions) {
				File jar = new File(artifactDir, (((((version + (File.separator)) + artifactId) + "-") + version) + ".jar"));
				if (jar.exists()) {
					jars.put(version, jar);
				}
			}
		}
		if (jars.isEmpty()) {
			throw new IllegalStateException(((("Cound not find jar for " + groupId) + "/") + artifactId));
		}
		if ((sb.length()) > 0) {
			sb.append(File.pathSeparator);
		}
		sb.append(jars.lastEntry().getValue().getAbsolutePath());
	}

	public void restartInstance(final String name, final String javaOpts) {
		execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			String current = System.getProperty("karaf.name");
			if (name.equals(current)) {
				String location = System.getProperty("karaf.home");
				StringBuilder classpath = new StringBuilder();
				addJar(classpath, "org.apache.karaf.instance", "org.apache.karaf.instance.core");
				addJar(classpath, "org.apache.karaf.shell", "org.apache.karaf.shell.core");
				addJar(classpath, "org.ops4j.pax.logging", "pax-logging-api");
				addJar(classpath, "jline", "jline");
				String command = ((((((((((((((((((((((((((((((("\"" + (new File(System.getProperty("java.home"), (ScriptUtils.isWindows() ? "bin\\java.exe" : "bin/java")).getCanonicalPath())) + "\" ") + " -Djava.util.logging.config.file=\"") + (new File(location, "etc/java.util.logging.properties").getCanonicalPath())) + "\"") + " -Dkaraf.home=\"") + (System.getProperty("karaf.home"))) + "\"") + " -Dkaraf.base=\"") + (new File(location).getCanonicalPath())) + "\"") + " -Dkaraf.data=\"") + (new File(new File(location).getCanonicalPath(), "data"))) + "\"") + " -Dkaraf.etc=\"") + (new File(new File(location).getCanonicalPath(), "etc"))) + "\"") + " -Dkaraf.log=\"") + (new File(new File(new File(location).getCanonicalFile(), "data"), "log"))) + "\"") + " -Dkaraf.instances=\"") + (System.getProperty("karaf.instances"))) + "\"") + " -classpath \"") + (classpath.toString())) + "\"") + " ") + (Execute.class.getName())) + " restart --java-opts \"") + javaOpts) + "\" ") + name;
				new ProcessBuilderFactoryImpl().newBuilder().directory(new File(System.getProperty("karaf.home"))).command(command).start();
			}else {
				checkPid(instance);
				if ((instance.pid) != 0) {
					cleanShutdown(instance);
				}
				doStart(instance, name, javaOpts);
			}
			return null;
		}, true);
	}

	public void stopInstance(final String name) {
		Integer pid = execute(( state) -> {
			int rootInstancePID = 0;
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			checkPid(instance);
			if ((instance.pid) == 0) {
				throw new IllegalStateException("Instance already stopped");
			}
			cleanShutdown(instance);
			if ((instance.pid) > 0) {
				if (!(instance.root)) {
					Process process = new ProcessBuilderFactoryImpl().newBuilder().attach(instance.pid);
					process.destroy();
				}else {
					rootInstancePID = instance.pid;
				}
				instance.pid = 0;
			}
			return rootInstancePID;
		}, true);
		if ((pid != 0) && (isInstanceRoot(name))) {
			Process process;
			try {
				process = new ProcessBuilderFactoryImpl().newBuilder().attach(pid);
				process.destroy();
			} catch (IOException e) {
				InstanceServiceImpl.LOGGER.debug("Unable to cleanly shutdown root instance ", e);
			}
		}
	}

	public void destroyInstance(final String name) {
		execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			checkPid(instance);
			if ((instance.pid) != 0) {
				throw new IllegalStateException("Instance not stopped");
			}
			deleteFile(new File(instance.loc));
			state.instances.remove(name);
			this.proxies.remove(name);
			return null;
		}, true);
	}

	public void renameInstance(final String oldName, final String newName, final boolean printOutput) throws Exception {
		execute(( state) -> {
			if ((state.instances.get(newName)) != null) {
				throw new IllegalArgumentException((("Instance " + newName) + " already exists"));
			}
			InstanceServiceImpl.InstanceState instance = state.instances.get(oldName);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + oldName) + " not found"));
			}
			if (instance.root) {
				throw new IllegalArgumentException("Root instance cannot be renamed");
			}
			checkPid(instance);
			if ((instance.pid) != 0) {
				throw new IllegalStateException("Instance not stopped");
			}
			InstanceServiceImpl.println(((((((("Renaming instance " + (SimpleAnsi.INTENSITY_BOLD)) + oldName) + (SimpleAnsi.INTENSITY_NORMAL)) + " to ") + (SimpleAnsi.INTENSITY_BOLD)) + newName) + (SimpleAnsi.INTENSITY_NORMAL)));
			String oldLocationPath = instance.loc;
			File oldLocation = new File(oldLocationPath);
			String basedir = oldLocation.getParent();
			File newLocation = new File(basedir, newName);
			oldLocation.renameTo(newLocation);
			HashMap<String, String> props = new HashMap<>();
			props.put(oldName, newName);
			props.put(oldLocationPath, newLocation.getPath());
			filterResource(newLocation, "etc/system.properties", props);
			filterResource(newLocation, "bin/karaf", props);
			filterResource(newLocation, "bin/start", props);
			filterResource(newLocation, "bin/stop", props);
			filterResource(newLocation, "bin/karaf.bat", props);
			filterResource(newLocation, "bin/start.bat", props);
			filterResource(newLocation, "bin/stop.bat", props);
			instance.name = newName;
			instance.loc = newLocation.getPath();
			state.instances.put(newName, instance);
			state.instances.remove(oldName);
			InstanceImpl proxy = this.proxies.remove(oldName);
			if (proxy == null) {
			}else {
			}
			this.proxies.put(newName, proxy);
			return null;
		}, true);
	}

	public synchronized Instance cloneInstance(final String name, final String cloneName, final InstanceSettings settings, final boolean printOutput) throws Exception {
		final int instanceSshPort = getInstanceSshPort(name);
		final int instanceRmiRegistryPort = getInstanceRmiRegistryPort(name);
		final int instanceRmiServerPort = getInstanceRmiServerPort(name);
		return null;
	}

	private void checkPid(InstanceServiceImpl.InstanceState instance) throws IOException {
		if ((instance.pid) != 0) {
			Process process = new ProcessBuilderFactoryImpl().newBuilder().attach(instance.pid);
			if (!(process.isRunning())) {
				instance.pid = 0;
			}
		}
	}

	protected void cleanShutdown(InstanceServiceImpl.InstanceState instance) {
		try {
			File file = new File(new File(instance.loc, "etc"), InstanceServiceImpl.CONFIG_PROPERTIES_FILE_NAME);
			Properties props = PropertiesLoader.loadPropertiesFile(file.toURI().toURL(), false);
			props.put("karaf.base", new File(instance.loc).getCanonicalPath());
			props.put("karaf.home", System.getProperty("karaf.home"));
			props.put("karaf.data", new File(new File(instance.loc), "data").getCanonicalPath());
			props.put("karaf.etc", new File(new File(instance.loc), "etc").getCanonicalPath());
			props.put("karaf.log", new File(new File(new File(instance.loc), "data"), "log").getCanonicalPath());
			InterpolationHelper.performSubstitution(props, null, true, false, true);
			int port = Integer.parseInt(props.getProperty(InstanceServiceImpl.KARAF_SHUTDOWN_PORT, "0"));
			String host = props.getProperty(InstanceServiceImpl.KARAF_SHUTDOWN_HOST, "localhost");
			String portFile = props.getProperty(InstanceServiceImpl.KARAF_SHUTDOWN_PORT_FILE);
			String shutdown = props.getProperty(InstanceServiceImpl.KARAF_SHUTDOWN_COMMAND, InstanceServiceImpl.DEFAULT_SHUTDOWN_COMMAND);
			if ((port == 0) && (portFile != null)) {
				BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(portFile)));
				String portStr = r.readLine();
				port = Integer.parseInt(portStr);
				r.close();
			}
			if (port > 0) {
				Socket s = new Socket(host, port);
				s.getOutputStream().write(shutdown.getBytes());
				s.close();
				long stopTimeout = Long.parseLong(props.getProperty(InstanceServiceImpl.KARAF_SHUTDOWN_TIMEOUT, Long.toString(getStopTimeout())));
				long t = (System.currentTimeMillis()) + stopTimeout;
				do {
					Thread.sleep(100);
					checkPid(instance);
				} while (((System.currentTimeMillis()) < t) && ((instance.pid) > 0) );
			}
		} catch (Exception e) {
			InstanceServiceImpl.LOGGER.debug(("Unable to cleanly shutdown instance " + (instance.name)), e);
		}
	}

	int getInstanceSshPort(String name) {
		return getKarafPort(name, "etc/org.apache.karaf.shell.cfg", "sshPort");
	}

	void changeInstanceSshPort(String name, final int port) throws Exception {
		setKarafPort(name, "etc/org.apache.karaf.shell.cfg", "sshPort", port);
	}

	String getInstanceSshHost(String name) {
		return getKarafHost(name, "etc/org.apache.karaf.shell.cfg", "sshHost");
	}

	int getInstanceRmiRegistryPort(String name) {
		return getKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiRegistryPort");
	}

	void changeInstanceRmiRegistryPort(String name, final int port) throws Exception {
		setKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiRegistryPort", port);
	}

	String getInstanceRmiRegistryHost(String name) {
		return getKarafHost(name, "etc/org.apache.karaf.management.cfg", "rmiRegistryHost");
	}

	int getInstanceRmiServerPort(String name) {
		return getKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiServerPort");
	}

	void changeInstanceRmiServerPort(String name, int port) throws Exception {
		setKarafPort(name, "etc/org.apache.karaf.management.cfg", "rmiServerPort", port);
	}

	String getInstanceRmiServerHost(String name) {
		return getKarafHost(name, "etc/org.apache.karaf.management.cfg", "rmiServerHost");
	}

	private int getKarafPort(final String name, final String path, final String key) {
		return execute(( state) -> getKarafPort(state, name, path, key), false);
	}

	private Integer getKarafPort(InstanceServiceImpl.State state, String name, String path, final String key) {
		InstanceServiceImpl.InstanceState instance = state.instances.get(name);
		if (instance == null) {
			throw new IllegalArgumentException((("Instance " + name) + " not found"));
		}
		File f = new File(instance.loc, path);
		try {
			return FileLockUtils.execute(f, ( properties) -> {
				Object obj = properties.get(key);
				return obj instanceof Number ? ((Number) (obj)).intValue() : Integer.parseInt(obj.toString());
			}, false);
		} catch (IOException e) {
			return 0;
		}
	}

	private void setKarafPort(final String name, final String path, final String key, final int port) throws IOException {
		execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			checkPid(instance);
			if ((instance.pid) != 0) {
				throw new IllegalStateException("Instance is not stopped");
			}
			File f = new File(instance.loc, path);
			FileLockUtils.execute(f, ( properties) -> {
				properties.put(key, port);
			}, true);
			return null;
		}, true);
	}

	private String getKarafHost(final String name, final String path, final String key) {
		return execute(( state) -> this.getKarafHost(state, name, path, key), false);
	}

	private String getKarafHost(InstanceServiceImpl.State state, String name, String path, final String key) {
		InstanceServiceImpl.InstanceState instance = state.instances.get(name);
		if (instance == null) {
			throw new IllegalArgumentException((("Instance " + name) + " not found"));
		}
		File f = new File(instance.loc, path);
		try {
			return FileLockUtils.execute(f, (TypedProperties properties) -> properties.get(key).toString(), false);
		} catch (IOException e) {
			return "0.0.0.0";
		}
	}

	boolean isInstanceRoot(final String name) {
		return execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			return instance.root;
		}, false);
	}

	String getInstanceLocation(final String name) {
		return execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			return instance.loc;
		}, true);
	}

	int getInstancePid(final String name) {
		boolean updateInstanceProperties = isInstancePidNeedUpdate(name);
		return execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			checkPid(instance);
			return instance.pid;
		}, updateInstanceProperties);
	}

	String getInstanceJavaOpts(final String name) {
		return execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			return instance.opts;
		}, false);
	}

	void changeInstanceJavaOpts(final String name, final String opts) {
		execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			instance.opts = opts;
			return null;
		}, true);
	}

	String getInstanceState(final String name) {
		boolean updateInstanceProperties = isInstancePidNeedUpdate(name);
		return execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			int port = getKarafPort(state, name, "etc/org.apache.karaf.shell.cfg", "sshPort");
			String host = getKarafHost(state, name, "etc/org.apache.karaf.shell.cfg", "sshHost");
			if (host.equals("0.0.0.0")) {
				host = "localhost";
			}
			if ((!(new File(instance.loc).isDirectory())) || (port <= 0)) {
				return Instance.ERROR;
			}
			checkPid(instance);
			if ((instance.pid) == 0) {
				return Instance.STOPPED;
			}else {
				try {
					Socket s = new Socket(host, port);
					s.close();
					return Instance.STARTED;
				} catch (Exception e) {
				}
				return Instance.STARTING;
			}
		}, updateInstanceProperties);
	}

	private boolean deleteFile(File fileToDelete) {
		if ((fileToDelete == null) || (!(fileToDelete.exists()))) {
			return true;
		}
		boolean result = true;
		if (fileToDelete.isDirectory()) {
			File[] files = fileToDelete.listFiles();
			if (files == null) {
				result = false;
			}else {
				for (File file : files) {
					if ((file.getName().equals(".")) || (file.getName().equals(".."))) {
						continue;
					}
					if (file.isDirectory()) {
						result &= deleteFile(file);
					}else {
						result &= file.delete();
					}
				}
			}
		}
		result &= fileToDelete.delete();
		return result;
	}

	private void copyResourcesToDir(String[] resourcesToCopy, File target, Map<String, URL> resources, boolean printOutput) throws IOException {
		for (String resource : resourcesToCopy) {
			copyResourceToDir(resource, target, resources, printOutput);
		}
	}

	private void copyResourceToDir(String resource, File target, Map<String, URL> resources, boolean printOutput) throws IOException {
		File outFile = new File(target, resource);
		if (!(outFile.exists())) {
			InstanceServiceImpl.logDebug("Creating file: %s", printOutput, outFile.getPath());
			try (InputStream is = getResourceStream(resource, resources);OutputStream os = new FileOutputStream(outFile)) {
				if (is == null) {
					InstanceServiceImpl.logInfo("\tWARNING: unable to find %s", true, resource);
				}else {
					copyStream(is, os);
				}
			}
		}
	}

	private InputStream getResourceStream(String resource, Map<String, URL> resources) throws IOException {
		return resources.containsKey(resource) ? resources.remove(resource).openStream() : getClass().getClassLoader().getResourceAsStream(((InstanceServiceImpl.RESOURCE_BASE) + resource));
	}

	private static void println(String st) {
		System.out.println(st);
	}

	protected static Properties loadPropertiesFile(URL configPropURL) throws Exception {
		try (InputStream is = configPropURL.openConnection().getInputStream()) {
			Properties configProps = new Properties();
			configProps.load(is);
			return configProps;
		} catch (Exception ex) {
			System.err.println(("Error loading config properties from " + configPropURL));
			System.err.println(("Main: " + ex));
			return null;
		}
	}

	private void copyStream(InputStream is, OutputStream os) {
		PrintStream out = new PrintStream(os);
		Scanner scanner = new Scanner(is);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			out.println(line);
		} 
		scanner.close();
	}

	private void filterResource(File basedir, String path, HashMap<String, String> props) throws IOException {
		File file = new File(basedir, path);
		File bak = new File(basedir, (path + (InstanceServiceImpl.BACKUP_EXTENSION)));
		if (!(file.exists())) {
			return;
		}
		file.renameTo(bak);
		copyAndFilterResource(new FileInputStream(bak), new FileOutputStream(file), props);
		bak.delete();
	}

	private void copyFilteredResourcesToDir(String[] resourcesToCopy, File target, Map<String, URL> resources, Map<String, String> props, boolean printOutput) throws IOException {
		for (String resource : resourcesToCopy) {
			copyFilteredResourceToDir(resource, target, resources, props, printOutput);
		}
	}

	private void copyFilteredResourceToDir(String resource, File target, Map<String, URL> resources, Map<String, String> props, boolean printOutput) throws IOException {
		File outFile = new File(target, resource);
		if (!(outFile.exists())) {
			InstanceServiceImpl.logDebug("Creating file: %s", printOutput, outFile.getPath());
			try (InputStream is = getResourceStream(resource, resources);OutputStream os = new FileOutputStream(outFile)) {
				copyAndFilterResource(is, os, props);
			}
		}
	}

	private void copyAndFilterResource(InputStream source, OutputStream target, Map<String, String> props) throws IOException {
		PrintStream out = new PrintStream(target);
		Scanner scanner = new Scanner(source);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = filter(line, props);
			out.println(line);
		} 
		scanner.close();
	}

	private void copyBinaryResourceToDir(String resource, File target, Map<String, URL> resources, boolean printOutput) throws IOException {
		File outFile = new File(target, resource);
		if (!(outFile.exists())) {
			InstanceServiceImpl.logDebug("Creating file: %s", printOutput, outFile.getPath());
			try (InputStream is = getResourceStream(resource, resources);OutputStream os = new FileOutputStream(outFile)) {
				StreamUtils.copy(is, os);
			}
		}
	}

	private String filter(String line, Map<String, String> props) {
		for (Map.Entry<String, String> i : props.entrySet()) {
			int p1 = line.indexOf(i.getKey());
			if (p1 >= 0) {
				String l1 = line.substring(0, p1);
				String l2 = line.substring((p1 + (i.getKey().length())));
				line = (l1 + (i.getValue())) + l2;
			}
		}
		return line;
	}

	private void mkdir(File karafBase, String path, boolean printOutput) {
		File file = new File(karafBase, path);
		if (!(file.exists())) {
			InstanceServiceImpl.logDebug("Creating dir: %s", printOutput, file.getPath());
			file.mkdirs();
		}
	}

	private void makeFileExecutable(File serviceFile) throws IOException {
		try {
			Set<PosixFilePermission> permissions = new HashSet<>();
			permissions.add(PosixFilePermission.OWNER_EXECUTE);
			permissions.add(PosixFilePermission.GROUP_EXECUTE);
			permissions.add(PosixFilePermission.OTHERS_EXECUTE);
			Set<PosixFilePermission> filePermissions = Files.getPosixFilePermissions(serviceFile.toPath());
			filePermissions.addAll(permissions);
			Files.setPosixFilePermissions(serviceFile.toPath(), filePermissions);
		} catch (UnsupportedOperationException ex) {
			serviceFile.setExecutable(true, false);
		}
	}

	private void copy(File source, File destination) throws IOException {
		if (source.getName().equals("cache.lock")) {
			return;
		}
		if (source.getName().equals("lock")) {
			return;
		}
		if (source.getName().matches("transaction_\\d+\\.log")) {
			return;
		}
		if (source.getName().endsWith(".instance")) {
			return;
		}
		if (source.isDirectory()) {
			if (!(destination.exists())) {
				destination.mkdirs();
			}
			String[] children = source.list();
			for (String child : children) {
				if ((!(child.contains("instances"))) && (!(child.contains("lib"))))
					copy(new File(source, child), new File(destination, child));

			}
		}else {
			try (InputStream in = new FileInputStream(source);OutputStream out = new FileOutputStream(destination)) {
				StreamUtils.copy(in, out);
			}
		}
	}

	public void changeInstanceSshHost(String name, String host) throws Exception {
		setKarafHost(name, "etc/org.apache.karaf.shell.cfg", "sshHost", host);
	}

	private void setKarafHost(final String name, final String path, final String key, final String host) throws IOException {
		execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			checkPid(instance);
			if ((instance.pid) != 0) {
				throw new IllegalStateException("Instance is not stopped");
			}
			File f = new File(instance.loc, path);
			FileLockUtils.execute(f, ( properties) -> {
				properties.put(key, host);
			}, true);
			return null;
		}, true);
	}

	private boolean isInstancePidNeedUpdate(final String name) {
		return execute(( state) -> {
			InstanceServiceImpl.InstanceState instance = state.instances.get(name);
			if (instance == null) {
				throw new IllegalArgumentException((("Instance " + name) + " not found"));
			}
			int originalPid = instance.pid;
			checkPid(instance);
			int newPid = instance.pid;
			return originalPid != newPid;
		}, false);
	}

	private static class ProfileApplier {
		static void verify() {
		}
	}
}

