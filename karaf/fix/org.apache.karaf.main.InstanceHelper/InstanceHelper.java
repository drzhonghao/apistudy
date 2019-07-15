

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.AbstractMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.main.ConfigProperties;
import org.apache.karaf.util.locks.FileLockUtils;
import org.osgi.framework.launch.Framework;


public class InstanceHelper {
	static void updateInstancePid(final File karafHome, final File karafBase, final boolean isStartingInstance) {
		try {
			final String instanceName = System.getProperty("karaf.name");
			final String pid = (isStartingInstance) ? InstanceHelper.getPid() : "0";
			if (instanceName != null) {
				String storage = System.getProperty("karaf.instances");
				if (storage == null) {
					throw new Exception(("System property \'karaf.instances\' is not set. \n" + "This property needs to be set to the full path of the instance.properties file."));
				}
				File storageFile = new File(storage);
				final File propertiesFile = new File(storageFile, "instance.properties");
				if (!(propertiesFile.getParentFile().exists())) {
					try {
						if (!(propertiesFile.getParentFile().mkdirs())) {
							throw new Exception(("Unable to create directory " + (propertiesFile.getParentFile())));
						}
					} catch (SecurityException se) {
						throw new Exception(se.getMessage());
					}
				}
				if (!isStartingInstance) {
					boolean proceed = true;
					try (RandomAccessFile raf = new RandomAccessFile(propertiesFile, "rw")) {
						FileLock lock = raf.getChannel().tryLock();
						if (lock == null) {
							proceed = false;
						}else {
							lock.release();
						}
					}
					if (!proceed) {
						return;
					}
				}
				FileLockUtils.execute(propertiesFile, (TypedProperties props) -> {
					if (props.isEmpty()) {
						props.put("count", "1");
						props.put("item.0.name", instanceName);
						props.put("item.0.loc", karafBase.getAbsolutePath());
						props.put("item.0.pid", pid);
						props.put("item.0.root", "true");
					}else {
						int count = Integer.parseInt(props.get("count").toString());
						for (int i = 0; i < count; i++) {
							String name = props.get((("item." + i) + ".name")).toString();
							if (name.equals(instanceName)) {
								props.put((("item." + i) + ".pid"), pid);
								return;
							}
						}
						props.put("item.0.name", instanceName);
						props.put("item.0.pid", pid);
					}
				}, true);
			}
		} catch (Exception e) {
			System.err.println(("Unable to update instance pid: " + (e.getMessage())));
		}
	}

	private static String getPid() {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		Pattern pattern = Pattern.compile("^([0-9]+)@.+$");
		Matcher matcher = pattern.matcher(name);
		return matcher.matches() ? matcher.group(1) : name;
	}

	static void writePid(String pidFile) {
		try {
			if (pidFile != null) {
				int pid = Integer.parseInt(InstanceHelper.getPid());
				Writer w = new OutputStreamWriter(new FileOutputStream(pidFile));
				w.write(Integer.toString(pid));
				w.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static AutoCloseable setupShutdown(ConfigProperties config, Framework framework) {
		try {
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}

