import org.apache.accumulo.server.util.*;


import org.apache.accumulo.core.cli.Help;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.server.ServerConstants;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.fs.VolumeManagerImpl;
import org.apache.accumulo.start.spi.KeywordExecutable;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.beust.jcommander.Parameter;
import com.google.auto.service.AutoService;

@AutoService(KeywordExecutable.class)
public class ZooKeeperMain implements KeywordExecutable {

  static class Opts extends Help {

    @Parameter(names = {"-z", "--keepers"},
        description = "Comma separated list of zookeeper hosts (host:port,host:port)")
    String servers = null;

    @Parameter(names = {"-t", "--timeout"},
        description = "timeout, in seconds to timeout the zookeeper connection")
    long timeout = 30;
  }

  public static void main(String[] args) throws Exception {
    new ZooKeeperMain().execute(args);
  }

  @Override
  public String keyword() {
    return "zookeeper";
  }

  @Override
  public void execute(final String[] args) throws Exception {
    Opts opts = new Opts();
    opts.parseArgs(ZooKeeperMain.class.getName(), args);
    FileSystem fs = VolumeManagerImpl.get().getDefaultVolume().getFileSystem();
    String baseDir = ServerConstants.getBaseUris()[0];
    System.out.println("Using " + fs.makeQualified(new Path(baseDir + "/instance_id"))
        + " to lookup accumulo instance");
    Instance instance = HdfsZooInstance.getInstance();
    if (opts.servers == null) {
      opts.servers = instance.getZooKeepers();
    }
    System.out.println("The accumulo instance id is " + instance.getInstanceID());
    if (!opts.servers.contains("/"))
      opts.servers += "/accumulo/" + instance.getInstanceID();
    org.apache.zookeeper.ZooKeeperMain
        .main(new String[] {"-server", opts.servers, "-timeout", "" + (opts.timeout * 1000)});
  }
}
