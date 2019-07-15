import org.apache.accumulo.core.zookeeper.*;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.accumulo.core.Constants;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.accumulo.core.volume.VolumeConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooUtil extends org.apache.accumulo.fate.zookeeper.ZooUtil {

  private static final Logger log = LoggerFactory.getLogger(ZooUtil.class);

  public static String getRoot(final Instance instance) {
    return getRoot(instance.getInstanceID());
  }

  public static String getRoot(final String instanceId) {
    return Constants.ZROOT + "/" + instanceId;
  }

  /**
   * Utility to support certain client side utilities to minimize command-line options.
   */
  public static String getInstanceIDFromHdfs(Path instanceDirectory, AccumuloConfiguration conf) {
    return getInstanceIDFromHdfs(instanceDirectory, conf, CachedConfiguration.getInstance());
  }

  public static String getInstanceIDFromHdfs(Path instanceDirectory, AccumuloConfiguration conf,
      Configuration hadoopConf) {
    try {
      FileSystem fs = VolumeConfiguration.getVolume(instanceDirectory.toString(), hadoopConf, conf)
          .getFileSystem();
      FileStatus[] files = null;
      try {
        files = fs.listStatus(instanceDirectory);
      } catch (FileNotFoundException ex) {
        // ignored
      }
      log.debug("Trying to read instance id from " + instanceDirectory);
      if (files == null || files.length == 0) {
        log.error("unable obtain instance id at " + instanceDirectory);
        throw new RuntimeException(
            "Accumulo not initialized, there is no instance id at " + instanceDirectory);
      } else if (files.length != 1) {
        log.error("multiple potential instances in " + instanceDirectory);
        throw new RuntimeException(
            "Accumulo found multiple possible instance ids in " + instanceDirectory);
      } else {
        String result = files[0].getPath().getName();
        return result;
      }
    } catch (IOException e) {
      log.error("Problem reading instance id out of hdfs at " + instanceDirectory, e);
      throw new RuntimeException(
          "Can't tell if Accumulo is initialized; can't read instance id at " + instanceDirectory,
          e);
    } catch (IllegalArgumentException exception) {
      /* HDFS throws this when there's a UnknownHostException due to DNS troubles. */
      if (exception.getCause() instanceof UnknownHostException) {
        log.error("Problem reading instance id out of hdfs at " + instanceDirectory, exception);
      }
      throw exception;
    }
  }
}
