

import com.google.common.collect.Maps;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.accumulo.cluster.ClusterControl;
import org.apache.accumulo.minicluster.ServerType;
import org.apache.accumulo.minicluster.impl.MiniAccumuloClusterImpl;
import org.apache.accumulo.minicluster.impl.MiniAccumuloConfigImpl;
import org.apache.accumulo.minicluster.impl.ProcessNotFoundException;
import org.apache.accumulo.minicluster.impl.ProcessReference;
import org.apache.accumulo.server.util.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MiniAccumuloClusterControl implements ClusterControl {
	private static final Logger log = LoggerFactory.getLogger(MiniAccumuloClusterControl.class);

	protected MiniAccumuloClusterImpl cluster;

	Process zooKeeperProcess = null;

	Process masterProcess = null;

	Process gcProcess = null;

	Process monitor = null;

	Process tracer = null;

	final List<Process> tabletServerProcesses = new ArrayList<>();

	public MiniAccumuloClusterControl(MiniAccumuloClusterImpl cluster) {
		Objects.requireNonNull(cluster);
		this.cluster = cluster;
	}

	public void start(ServerType server) throws IOException {
		start(server, null);
	}

	@Override
	public int exec(Class<?> clz, String[] args) throws IOException {
		Process p = cluster.exec(clz, args);
		int exitCode;
		try {
			exitCode = p.waitFor();
		} catch (InterruptedException e) {
			MiniAccumuloClusterControl.log.warn("Interrupted waiting for process to exit", e);
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
		return exitCode;
	}

	@Override
	public Map.Entry<Integer, String> execWithStdout(Class<?> clz, String[] args) throws IOException {
		Process p = cluster.exec(clz, args);
		int exitCode;
		try {
			exitCode = p.waitFor();
		} catch (InterruptedException e) {
			MiniAccumuloClusterControl.log.warn("Interrupted waiting for process to exit", e);
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
		for (MiniAccumuloClusterImpl.LogWriter writer : cluster.getLogWriters()) {
			writer.flush();
		}
		return Maps.immutableEntry(exitCode, readAll(new FileInputStream(((((((cluster.getConfig().getLogDir()) + "/") + (clz.getSimpleName())) + "_") + (p.hashCode())) + ".out"))));
	}

	private String readAll(InputStream is) throws IOException {
		byte[] buffer = new byte[4096];
		StringBuilder result = new StringBuilder();
		while (true) {
			int n = is.read(buffer);
			if (n <= 0)
				break;

			result.append(new String(buffer, 0, n));
		} 
		return result.toString();
	}

	@Override
	public void adminStopAll() throws IOException {
		Process p = cluster.exec(Admin.class, "stopAll");
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
		if (0 != (p.exitValue())) {
			throw new IOException("Failed to run `accumulo admin stopAll`");
		}
	}

	@Override
	public synchronized void startAllServers(ServerType server) throws IOException {
		start(server, null);
	}

	@Override
	public synchronized void start(ServerType server, String hostname) throws IOException {
		start(server, hostname, Collections.<String, String>emptyMap(), Integer.MAX_VALUE);
	}

	public synchronized void start(ServerType server, String hostname, Map<String, String> configOverrides, int limit) throws IOException {
		if (limit <= 0) {
			return;
		}
		switch (server) {
			case TABLET_SERVER :
				synchronized(tabletServerProcesses) {
					int count = 0;
					for (int i = tabletServerProcesses.size(); (count < limit) && (i < (cluster.getConfig().getNumTservers())); i++ , ++count) {
					}
				}
				break;
			case MASTER :
				if (null == (masterProcess)) {
				}
				break;
			case ZOOKEEPER :
				if (null == (zooKeeperProcess)) {
				}
				break;
			case GARBAGE_COLLECTOR :
				if (null == (gcProcess)) {
				}
				break;
			case MONITOR :
				if (null == (monitor)) {
				}
				break;
			case TRACER :
				if (null == (tracer)) {
				}
				break;
			default :
				throw new UnsupportedOperationException(("Cannot start process for " + server));
		}
	}

	@Override
	public synchronized void stopAllServers(ServerType server) throws IOException {
		stop(server);
	}

	public void stop(ServerType server) throws IOException {
		stop(server, null);
	}

	@Override
	public synchronized void stop(ServerType server, String hostname) throws IOException {
		switch (server) {
			case MASTER :
				if (null != (masterProcess)) {
				}
				break;
			case GARBAGE_COLLECTOR :
				if (null != (gcProcess)) {
				}
				break;
			case ZOOKEEPER :
				if (null != (zooKeeperProcess)) {
				}
				break;
			case TABLET_SERVER :
				synchronized(tabletServerProcesses) {
					try {
						for (Process tserver : tabletServerProcesses) {
						}
					} finally {
						tabletServerProcesses.clear();
					}
				}
				break;
			case MONITOR :
				if ((monitor) != null) {
				}
				break;
			case TRACER :
				if ((tracer) != null) {
				}
				break;
			default :
				throw new UnsupportedOperationException(("ServerType is not yet supported " + server));
		}
	}

	@Override
	public void signal(ServerType server, String hostname, String signal) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void suspend(ServerType server, String hostname) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resume(ServerType server, String hostname) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void killProcess(ServerType type, ProcessReference procRef) throws InterruptedException, ProcessNotFoundException {
		boolean found = false;
		switch (type) {
			case MASTER :
				if (procRef.getProcess().equals(masterProcess)) {
					masterProcess = null;
					found = true;
				}
				break;
			case TABLET_SERVER :
				synchronized(tabletServerProcesses) {
					for (Process tserver : tabletServerProcesses) {
						if (procRef.getProcess().equals(tserver)) {
							tabletServerProcesses.remove(tserver);
							found = true;
							break;
						}
					}
				}
				break;
			case ZOOKEEPER :
				if (procRef.getProcess().equals(zooKeeperProcess)) {
					zooKeeperProcess = null;
					found = true;
				}
				break;
			case GARBAGE_COLLECTOR :
				if (procRef.getProcess().equals(gcProcess)) {
					gcProcess = null;
					found = true;
				}
				break;
			default :
				found = true;
				break;
		}
		if (!found)
			throw new ProcessNotFoundException();

	}

	@Override
	public void kill(ServerType server, String hostname) throws IOException {
		stop(server, hostname);
	}
}

