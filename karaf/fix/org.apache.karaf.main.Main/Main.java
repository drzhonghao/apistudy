

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.karaf.info.ServerInfo;
import org.apache.karaf.main.ConfigProperties;
import org.apache.karaf.main.KarafActivatorManager;
import org.apache.karaf.main.ServerInfoImpl;
import org.apache.karaf.main.ShutdownCallback;
import org.apache.karaf.main.internal.Systemd;
import org.apache.karaf.main.lock.Lock;
import org.apache.karaf.main.lock.LockCallBack;
import org.apache.karaf.main.util.ArtifactResolver;
import org.apache.karaf.main.util.BootstrapLogManager;
import org.apache.karaf.main.util.SimpleMavenResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.FrameworkStartLevel;


public class Main {
	public static final String STARTUP_PROPERTIES_FILE_NAME = "startup.properties";

	private static final Logger LOG = Logger.getLogger(Main.class.getName());

	private ConfigProperties config;

	private Framework framework;

	private final String[] args;

	private int exitCode;

	private ShutdownCallback shutdownCallback;

	private KarafActivatorManager activatorManager;

	private volatile Lock lock;

	private final Main.KarafLockCallback lockCallback = new Main.KarafLockCallback();

	private volatile boolean exiting;

	private AutoCloseable shutdownThread;

	private Thread monitorThread;

	public static void main(String[] args) throws Exception {
		while (true) {
			boolean restart = false;
			boolean restartJvm = false;
			System.setProperty("karaf.restart.jvm", "false");
			System.setProperty("karaf.restart", "false");
			final Main main = new Main(args);
			try {
				main.launch();
			} catch (Throwable ex) {
				System.err.println(ex.getMessage());
				main.LOG.log(Level.SEVERE, "Could not launch framework", ex);
				main.destroy();
				main.setExitCode((-1));
			}
			try {
				main.awaitShutdown();
				boolean stopped = main.destroy();
				restart = Boolean.getBoolean("karaf.restart");
				restartJvm = Boolean.getBoolean("karaf.restart.jvm");
				main.updateInstancePidAfterShutdown();
				if (!stopped) {
					if (restart) {
						System.err.println("Timeout waiting for framework to stop.  Restarting now.");
					}else {
						System.err.println("Timeout waiting for framework to stop.  Exiting VM.");
						main.setExitCode((-3));
					}
				}
			} catch (Throwable ex) {
				main.setExitCode((-2));
				System.err.println(("Error occurred shutting down framework: " + ex));
				ex.printStackTrace();
			} finally {
				if (restartJvm && restart) {
					System.exit(10);
				}else
					if (!restart) {
						System.exit(main.getExitCode());
					}else {
						System.gc();
					}

			}
		} 
	}

	public Main(String[] args) {
		this.args = args;
	}

	public void setShutdownCallback(ShutdownCallback shutdownCallback) {
		this.shutdownCallback = shutdownCallback;
	}

	public void updateInstancePidAfterShutdown() throws Exception {
		if ((config) == null) {
			config = new ConfigProperties();
		}
	}

	public void launch() throws Exception {
		if ((config) == null) {
			config = new ConfigProperties();
		}
		config.performInit();
		String log4jConfigPath = (System.getProperty("karaf.etc")) + "/org.ops4j.pax.logging.cfg";
		BootstrapLogManager.configureLogger(Main.LOG);
		List<File> bundleDirs = getBundleRepos();
		ArtifactResolver resolver = new SimpleMavenResolver(bundleDirs);
		ClassLoader classLoader = createClassLoader(resolver);
		FrameworkFactory factory = loadFrameworkFactory(classLoader);
		setLogger();
		framework.init();
		framework.getBundleContext().addFrameworkListener(lockCallback);
		framework.start();
		FrameworkStartLevel sl = framework.adapt(FrameworkStartLevel.class);
		if ((framework.getBundleContext().getBundles().length) == 1) {
			Main.LOG.info("Installing and starting initial bundles");
			Main.LOG.info("All initial bundles installed and set to start");
		}
		ServerInfo serverInfo = new ServerInfoImpl(args, config);
		framework.getBundleContext().registerService(ServerInfo.class, serverInfo, null);
		activatorManager = new KarafActivatorManager(classLoader, framework);
		monitorThread = monitor();
		registerSignalHandler();
		watchdog();
	}

	private void setLogger() {
		try {
			if (framework.getClass().getName().startsWith("org.apache.felix.")) {
				Field field = framework.getClass().getDeclaredField("m_logger");
				field.setAccessible(true);
				Object logger = field.get(framework);
				Method method = logger.getClass().getDeclaredMethod("setLogger", Object.class);
				method.setAccessible(true);
				method.invoke(logger, new Object() {
					public void log(int level, String message, Throwable exception) {
						Level lvl;
						switch (level) {
							case 1 :
								lvl = Level.SEVERE;
								break;
							case 2 :
								lvl = Level.WARNING;
								break;
							case 3 :
								lvl = Level.INFO;
								break;
							case 4 :
								lvl = Level.FINE;
								break;
							default :
								lvl = Level.FINEST;
								break;
						}
						Logger.getLogger("Felix").log(lvl, message, exception);
					}
				});
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void registerSignalHandler() {
		if (!(Boolean.valueOf(System.getProperty("karaf.handle.sigterm", "true")))) {
			return;
		}
		try {
			final Class<?> signalClass = Class.forName("sun.misc.Signal");
			final Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
			Object signalHandler = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ signalHandlerClass }, ( proxy, method, args) -> {
				new Thread() {
					@Override
					public void run() {
						try {
							exiting = true;
							framework.stop();
						} catch (BundleException e) {
							e.printStackTrace();
						}
					}
				}.start();
				return null;
			});
			signalClass.getMethod("handle", signalClass, signalHandlerClass).invoke(null, signalClass.getConstructor(String.class).newInstance("TERM"), signalHandler);
		} catch (Exception e) {
		}
	}

	private Thread monitor() {
		Thread th = new Thread("Karaf Lock Monitor Thread") {
			public void run() {
				try {
					doMonitor();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		th.start();
		return th;
	}

	private void doMonitor() throws Exception {
		lock = createLock();
		File dataDir = new File(System.getProperty(ConfigProperties.PROP_KARAF_DATA));
		int livenessFailureCount = 0;
		boolean locked = false;
		while (!(exiting)) {
			if (lock.lock()) {
				livenessFailureCount = 0;
				if (!locked) {
					lockCallback.lockAcquired();
					locked = true;
				}
				for (; ;) {
					if (!(dataDir.isDirectory())) {
						Main.LOG.info("Data directory does not exist anymore, halting");
						framework.stop();
						System.exit((-1));
						return;
					}
					if ((!(lock.isAlive())) || (exiting)) {
						break;
					}
				}
				if (!(exiting)) {
					livenessFailureCount++;
				}else {
					lockCallback.stopShutdownThread();
				}
			}else {
				if (locked) {
					livenessFailureCount++;
				}else {
				}
			}
		} 
	}

	Lock getLock() {
		return lock;
	}

	private void watchdog() {
		if (Boolean.getBoolean("karaf.systemd.enabled")) {
			new Thread("Karaf Systemd Watchdog Thread") {
				public void run() {
					try {
						doWatchdog();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	private void doWatchdog() throws Exception {
		Systemd systemd = new Systemd();
		long timeout = systemd.getWatchdogTimeout(TimeUnit.MILLISECONDS);
		int code;
		while ((!(exiting)) && (timeout > 0)) {
			code = systemd.notifyWatchdog();
			if (code < 0) {
				System.err.println(("Systemd sd_notify failed with error code: " + code));
				break;
			}
			Thread.sleep((timeout / 2));
		} 
	}

	private ClassLoader createClassLoader(ArtifactResolver resolver) throws Exception {
		List<URL> urls = new ArrayList<>();
		return new URLClassLoader(urls.toArray(new URL[urls.size()]), Main.class.getClassLoader());
	}

	private FrameworkFactory loadFrameworkFactory(ClassLoader classLoader) throws Exception {
		return null;
	}

	private Lock createLock() {
		try {
		} catch (Exception e) {
			if (e instanceof InvocationTargetException) {
			}else {
			}
		}
		return null;
	}

	private static void addSecurityProvider(String provider) {
		try {
			Security.addProvider(((Provider) (Class.forName(provider).newInstance())));
		} catch (Throwable t) {
			System.err.println(("Unable to register security provider: " + t));
		}
	}

	private boolean isNotFragment(Bundle b) {
		String fragmentHostHeader = b.getHeaders().get(Constants.FRAGMENT_HOST);
		return (fragmentHostHeader == null) || ((fragmentHostHeader.trim().length()) == 0);
	}

	private List<File> getBundleRepos() {
		List<File> bundleDirs = new ArrayList<>();
		return bundleDirs;
	}

	public String[] getArgs() {
		return args;
	}

	public int getExitCode() {
		return exitCode;
	}

	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

	public Framework getFramework() {
		return framework;
	}

	protected void setStartLevel(int level) {
		framework.adapt(FrameworkStartLevel.class).setStartLevel(level);
	}

	public ConfigProperties getConfig() {
		return config;
	}

	public void setConfig(ConfigProperties config) {
		this.config = config;
	}

	public void awaitShutdown() throws Exception {
		if ((framework) == null) {
			return;
		}
		while (true) {
			FrameworkEvent event = framework.waitForStop(0);
			if ((event.getType()) == (FrameworkEvent.STOPPED_UPDATE)) {
				if ((lock) != null) {
					lock.release();
				}
				while (((framework.getState()) != (Bundle.STARTING)) && ((framework.getState()) != (Bundle.ACTIVE))) {
					Thread.sleep(10);
				} 
				monitorThread = monitor();
			}else {
				return;
			}
		} 
	}

	public boolean destroy() throws Exception {
		if ((framework) == null) {
			return true;
		}
		try {
			if ((shutdownCallback) != null) {
			}
			exiting = true;
			if (((framework.getState()) == (Bundle.ACTIVE)) || ((framework.getState()) == (Bundle.STARTING))) {
				new Thread(() -> {
					try {
						framework.stop();
					} catch (BundleException e) {
						System.err.println(("Error stopping karaf: " + (e.getMessage())));
					}
				}).start();
			}
			int step = 5000;
			return false;
		} finally {
			if ((lock) != null) {
				exiting = true;
				if ((monitorThread) != null) {
					try {
						monitorThread.interrupt();
						monitorThread.join();
					} finally {
						monitorThread = null;
					}
				}
				lock.release();
			}
		}
	}

	private final class KarafLockCallback implements LockCallBack , FrameworkListener {
		private Object startLevelLock = new Object();

		@Override
		public void lockLost() {
			stopShutdownThread();
			if ((framework.getState()) == (Bundle.ACTIVE)) {
				synchronized(startLevelLock) {
					Main.LOG.fine("Waiting for start level change to complete...");
				}
			}
		}

		public void stopShutdownThread() {
			if ((shutdownThread) != null) {
				try {
					shutdownThread.close();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					shutdownThread = null;
				}
			}
		}

		@Override
		public void lockAcquired() {
		}

		@Override
		public void waitingForLock() {
			Main.LOG.fine("Waiting for the lock ...");
		}

		@Override
		public void frameworkEvent(FrameworkEvent event) {
			if ((event.getType()) == (FrameworkEvent.STARTLEVEL_CHANGED)) {
				synchronized(startLevelLock) {
					Main.LOG.fine("Start level change complete.");
					startLevelLock.notifyAll();
				}
			}
		}
	}
}

