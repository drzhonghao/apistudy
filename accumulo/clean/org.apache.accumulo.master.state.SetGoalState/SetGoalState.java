import org.apache.accumulo.master.state.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.master.thrift.MasterGoalState;
import org.apache.accumulo.core.zookeeper.ZooUtil;
import org.apache.accumulo.fate.zookeeper.ZooUtil.NodeExistsPolicy;
import org.apache.accumulo.server.Accumulo;
import org.apache.accumulo.server.client.HdfsZooInstance;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.fs.VolumeManagerImpl;
import org.apache.accumulo.server.security.SecurityUtil;
import org.apache.accumulo.server.zookeeper.ZooReaderWriter;

public class SetGoalState {

  /**
   * Utility program that will change the goal state for the master from the command line.
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1 || MasterGoalState.valueOf(args[0]) == null) {
      System.err.println(
          "Usage: accumulo " + SetGoalState.class.getName() + " [NORMAL|SAFE_MODE|CLEAN_STOP]");
      System.exit(-1);
    }
    SecurityUtil.serverLogin(SiteConfiguration.getInstance());

    VolumeManager fs = VolumeManagerImpl.get();
    Accumulo.waitForZookeeperAndHdfs(fs);
    ZooReaderWriter.getInstance().putPersistentData(
        ZooUtil.getRoot(HdfsZooInstance.getInstance()) + Constants.ZMASTER_GOAL_STATE,
        args[0].getBytes(UTF_8), NodeExistsPolicy.OVERWRITE);
  }

}
