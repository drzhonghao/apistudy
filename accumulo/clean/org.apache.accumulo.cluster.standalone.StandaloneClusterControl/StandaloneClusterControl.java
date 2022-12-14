import org.apache.accumulo.cluster.standalone.*;


import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.accumulo.cluster.ClusterControl;
import org.apache.accumulo.cluster.RemoteShell;
import org.apache.accumulo.cluster.RemoteShellOptions;
import org.apache.accumulo.core.master.thrift.MasterGoalState;
import org.apache.accumulo.master.state.SetGoalState;
import org.apache.accumulo.minicluster.ServerType;
import org.apache.accumulo.server.util.Admin;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.util.Shell.ExitCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * Use the {@link RemoteShell} to control a standalone (possibly distibuted) Accumulo instance
 */
public class StandaloneClusterControl implements ClusterControl {
  private static final Logger log = LoggerFactory.getLogger(StandaloneClusterControl.class);

  private static final String SUDO_CMD = "sudo";
  private static final String START_SERVER_SCRIPT = "start-server.sh", ACCUMULO_SCRIPT = "accumulo",
      TOOL_SCRIPT = "tool.sh";
  private static final String MASTER_HOSTS_FILE = "masters", GC_HOSTS_FILE = "gc",
      TSERVER_HOSTS_FILE = "slaves", TRACER_HOSTS_FILE = "tracers", MONITOR_HOSTS_FILE = "monitor";
  private static final String ACCUMULO_CONF_DIR = "ACCUMULO_CONF_DIR=";

  protected String user;
  protected String accumuloHome, clientAccumuloConfDir, serverAccumuloConfDir;
  protected RemoteShellOptions options;

  protected String startServerPath, accumuloPath, toolPath;

  public StandaloneClusterControl(String user) {
    this(user, System.getenv("ACCUMULO_HOME"), System.getenv("ACCUMULO_CONF_DIR"),
        System.getenv("ACCUMULO_CONF_DIR"));
  }

  public StandaloneClusterControl(String user, String accumuloHome, String clientAccumuloConfDir,
      String serverAccumuloConfDir) {
    this.user = user;
    this.options = new RemoteShellOptions();
    this.accumuloHome = accumuloHome;
    this.clientAccumuloConfDir = clientAccumuloConfDir;
    this.serverAccumuloConfDir = serverAccumuloConfDir;

    File bin = new File(accumuloHome, "bin");
    this.startServerPath = new File(bin, START_SERVER_SCRIPT).getAbsolutePath();
    this.accumuloPath = new File(bin, ACCUMULO_SCRIPT).getAbsolutePath();
    this.toolPath = new File(bin, TOOL_SCRIPT).getAbsolutePath();
  }

  String getToolPath() {
    return this.toolPath;
  }

  protected Entry<Integer,String> exec(String hostname, String[] command) throws IOException {
    RemoteShell shell = new RemoteShell(hostname, command, options);
    try {
      shell.execute();
    } catch (ExitCodeException e) {
      // capture the stdout of the process as well.
      String output = shell.getOutput();
      // add output for the ExitCodeException.
      ExitCodeException ece = new ExitCodeException(e.getExitCode(),
          "stderr: " + e.getMessage() + ", stdout: " + output);
      log.error("Failed to run command", ece);
      return Maps.immutableEntry(e.getExitCode(), output);
    }

    return Maps.immutableEntry(shell.getExitCode(), shell.getOutput());
  }

  @Override
  public int exec(Class<?> clz, String[] args) throws IOException {
    return execWithStdout(clz, args).getKey();
  }

  @Override
  public Entry<Integer,String> execWithStdout(Class<?> clz, String[] args) throws IOException {
    File confDir = getConfDir();
    String master = getHosts(new File(confDir, "masters")).get(0);
    String[] cmd = new String[3 + args.length];
    // Make sure we always set the right ACCUMULO_CONF_DIR
    cmd[0] = ACCUMULO_CONF_DIR + clientAccumuloConfDir;
    cmd[1] = accumuloPath;
    cmd[2] = clz.getName();
    // Quote the arguments to prevent shell expansion
    for (int i = 0, j = 3; i < args.length; i++, j++) {
      cmd[j] = "'" + args[i] + "'";
    }
    log.info("Running: '{}' on {}", StringUtils.join(cmd, " "), master);
    return exec(master, cmd);
  }

  public Entry<Integer,String> execMapreduceWithStdout(Class<?> clz, String[] args)
      throws IOException {
    String host = "localhost";
    String[] cmd = new String[3 + args.length];
    cmd[0] = getToolPath();
    cmd[1] = getJarFromClass(clz);
    cmd[2] = clz.getName();
    for (int i = 0, j = 3; i < args.length; i++, j++) {
      cmd[j] = "'" + args[i] + "'";
    }
    log.info("Running: '{}' on {}", StringUtils.join(cmd, " "), host);
    return exec(host, cmd);
  }

  String getJarFromClass(Class<?> clz) {
    CodeSource source = clz.getProtectionDomain().getCodeSource();
    if (null == source) {
      throw new RuntimeException("Could not get CodeSource for class");
    }
    URL jarUrl = source.getLocation();
    String jar = jarUrl.getPath();
    if (!jar.endsWith(".jar")) {
      throw new RuntimeException("Need to have a jar to run mapreduce: " + jar);
    }
    return jar;
  }

  @Override
  public void adminStopAll() throws IOException {
    File confDir = getConfDir();
    String master = getHosts(new File(confDir, "masters")).get(0);
    String[] cmd = new String[] {SUDO_CMD, "-u", user, ACCUMULO_CONF_DIR + serverAccumuloConfDir,
        accumuloPath, Admin.class.getName(), "stopAll"};
    // Directly invoke the RemoteShell
    Entry<Integer,String> pair = exec(master, cmd);
    if (0 != pair.getKey().intValue()) {
      throw new IOException("stopAll did not finish successfully, retcode=" + pair.getKey()
          + ", stdout=" + pair.getValue());
    }
  }

  /**
   * Wrapper around SetGoalState using the "server" <code>ACCUMULO_CONF_DIR</code>
   *
   * @param goalState
   *          The goal state to set
   * @throws IOException
   *           If SetGoalState returns a non-zero result
   */
  public void setGoalState(String goalState) throws IOException {
    requireNonNull(goalState, "Goal state must not be null");
    checkArgument(MasterGoalState.valueOf(goalState) != null, "Unknown goal state: " + goalState);
    File confDir = getConfDir();
    String master = getHosts(new File(confDir, "masters")).get(0);
    String[] cmd = new String[] {SUDO_CMD, "-u", user, ACCUMULO_CONF_DIR + serverAccumuloConfDir,
        accumuloPath, SetGoalState.class.getName(), goalState};
    Entry<Integer,String> pair = exec(master, cmd);
    if (0 != pair.getKey().intValue()) {
      throw new IOException("SetGoalState did not finish successfully, retcode=" + pair.getKey()
          + ", stdout=" + pair.getValue());
    }
  }

  @Override
  public void startAllServers(ServerType server) throws IOException {
    File confDir = getConfDir();

    switch (server) {
      case TABLET_SERVER:
        for (String tserver : getHosts(new File(confDir, TSERVER_HOSTS_FILE))) {
          start(server, tserver);
        }
        break;
      case MASTER:
        for (String master : getHosts(new File(confDir, MASTER_HOSTS_FILE))) {
          start(server, master);
        }
        break;
      case GARBAGE_COLLECTOR:
        List<String> hosts = getHosts(new File(confDir, GC_HOSTS_FILE));
        if (hosts.isEmpty()) {
          hosts = getHosts(new File(confDir, MASTER_HOSTS_FILE));
          if (hosts.isEmpty()) {
            throw new IOException("Found hosts to run garbage collector on");
          }
          hosts = Collections.singletonList(hosts.get(0));
        }
        for (String gc : hosts) {
          start(server, gc);
        }
        break;
      case TRACER:
        for (String tracer : getHosts(new File(confDir, TRACER_HOSTS_FILE))) {
          start(server, tracer);
        }
        break;
      case MONITOR:
        for (String monitor : getHosts(new File(confDir, MONITOR_HOSTS_FILE))) {
          start(server, monitor);
        }
        break;
      case ZOOKEEPER:
      default:
        throw new UnsupportedOperationException("Could not start servers for " + server);
    }
  }

  @Override
  public void start(ServerType server, String hostname) throws IOException {
    String[] cmd = new String[] {SUDO_CMD, "-u", user, ACCUMULO_CONF_DIR + serverAccumuloConfDir,
        startServerPath, hostname, getProcessString(server)};
    Entry<Integer,String> pair = exec(hostname, cmd);
    if (0 != pair.getKey()) {
      throw new IOException(
          "Start " + server + " on " + hostname + " failed for execute successfully");
    }
  }

  @Override
  public void stopAllServers(ServerType server) throws IOException {
    File confDir = getConfDir();

    switch (server) {
      case TABLET_SERVER:
        for (String tserver : getHosts(new File(confDir, TSERVER_HOSTS_FILE))) {
          stop(server, tserver);
        }
        break;
      case MASTER:
        for (String master : getHosts(new File(confDir, MASTER_HOSTS_FILE))) {
          stop(server, master);
        }
        break;
      case GARBAGE_COLLECTOR:
        for (String gc : getHosts(new File(confDir, GC_HOSTS_FILE))) {
          stop(server, gc);
        }
        break;
      case TRACER:
        for (String tracer : getHosts(new File(confDir, TRACER_HOSTS_FILE))) {
          stop(server, tracer);
        }
        break;
      case MONITOR:
        for (String monitor : getHosts(new File(confDir, MONITOR_HOSTS_FILE))) {
          stop(server, monitor);
        }
        break;
      case ZOOKEEPER:
      default:
        throw new UnsupportedOperationException("Could not start servers for " + server);
    }
  }

  @Override
  public void stop(ServerType server, String hostname) throws IOException {
    // TODO Use `accumulo admin stop` for tservers, instrument clean stop for GC, monitor, tracer
    // instead kill

    kill(server, hostname);
  }

  @Override
  public void signal(ServerType server, String hostname, String signal) throws IOException {
    String pid = getPid(server, accumuloHome, hostname);

    if (pid.trim().isEmpty()) {
      log.debug("Found no processes for {} on {}", server, hostname);
      return;
    }

    boolean isSignalNumber = false;
    try {
      Integer.parseInt(signal);
      isSignalNumber = true;
    } catch (NumberFormatException e) {}

    String[] stopCmd;
    if (isSignalNumber) {
      stopCmd = new String[] {SUDO_CMD, "-u", user, "kill", "-" + signal, pid};
    } else {
      stopCmd = new String[] {SUDO_CMD, "-u", user, "kill", "-s", signal, pid};
    }

    Entry<Integer,String> pair = exec(hostname, stopCmd);
    if (0 != pair.getKey()) {
      throw new IOException("Signal " + signal + " to " + server + " on " + hostname
          + " failed for execute successfully. stdout=" + pair.getValue());
    }
  }

  @Override
  public void suspend(ServerType server, String hostname) throws IOException {
    signal(server, hostname, "SIGSTOP");
  }

  @Override
  public void resume(ServerType server, String hostname) throws IOException {
    signal(server, hostname, "SIGCONT");
  }

  @Override
  public void kill(ServerType server, String hostname) throws IOException {
    signal(server, hostname, "SIGKILL");
  }

  protected String getPid(ServerType server, String accumuloHome, String hostname)
      throws IOException {
    String[] getPidCommand = getPidCommand(server, accumuloHome);
    Entry<Integer,String> ret = exec(hostname, getPidCommand);
    if (0 != ret.getKey()) {
      throw new IOException(
          "Could not locate PID for " + getProcessString(server) + " on " + hostname);
    }

    return ret.getValue();
  }

  protected String[] getPidCommand(ServerType server, String accumuloHome) {
    // Lifted from stop-server.sh to get the PID
    return new String[] {"ps", "aux", "|", "fgrep", accumuloHome, "|", "fgrep",
        getProcessString(server), "|", "fgrep", "-v", "grep", "|", "fgrep", "-v", "ssh", "|", "awk",
        "'{print \\$2}'", "|", "head", "-1", "|", "tr", "-d", "'\\n'"};
  }

  protected String getProcessString(ServerType server) {
    switch (server) {
      case TABLET_SERVER:
        return "tserver";
      case GARBAGE_COLLECTOR:
        return "gc";
      case MASTER:
        return "master";
      case TRACER:
        return "tracer";
      case MONITOR:
        return "monitor";
      default:
        throw new UnsupportedOperationException("Unhandled ServerType " + server);
    }
  }

  protected File getConfDir() {
    String confPath = null == clientAccumuloConfDir ? System.getenv("ACCUMULO_CONF_DIR")
        : clientAccumuloConfDir;
    File confDir;
    if (null == confPath) {
      String homePath = null == accumuloHome ? System.getenv("ACCUMULO_HOME") : accumuloHome;
      if (null == homePath) {
        throw new IllegalStateException("Cannot extrapolate an ACCUMULO_CONF_DIR");
      }
      confDir = new File(homePath, "conf");
    } else {
      confDir = new File(confPath);
    }

    if (!confDir.exists() || !confDir.isDirectory()) {
      throw new IllegalStateException(
          "ACCUMULO_CONF_DIR does not exist or is not a directory: " + confDir);
    }

    return confDir;
  }

  /**
   * Read the provided file and return all lines which don't start with a '#' character
   */
  protected List<String> getHosts(File f) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(f));
    try {
      List<String> hosts = new ArrayList<>();
      String line = null;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty() && !line.startsWith("#")) {
          hosts.add(line);
        }
      }

      return hosts;
    } finally {
      reader.close();
    }
  }
}
