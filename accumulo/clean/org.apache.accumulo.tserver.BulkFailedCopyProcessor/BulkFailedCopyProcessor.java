import org.apache.accumulo.tserver.*;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.apache.accumulo.core.conf.SiteConfiguration;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.accumulo.server.fs.VolumeManagerImpl;
import org.apache.accumulo.server.zookeeper.DistributedWorkQueue.Processor;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copy failed bulk imports.
 */
public class BulkFailedCopyProcessor implements Processor {

  private static final Logger log = LoggerFactory.getLogger(BulkFailedCopyProcessor.class);

  @Override
  public Processor newProcessor() {
    return new BulkFailedCopyProcessor();
  }

  @Override
  public void process(String workID, byte[] data) {

    String paths[] = new String(data, UTF_8).split(",");

    Path orig = new Path(paths[0]);
    Path dest = new Path(paths[1]);
    Path tmp = new Path(dest.getParent(), dest.getName() + ".tmp");

    try {
      VolumeManager vm = VolumeManagerImpl.get(SiteConfiguration.getInstance());
      FileSystem origFs = vm.getVolumeByPath(orig).getFileSystem();
      FileSystem destFs = vm.getVolumeByPath(dest).getFileSystem();

      FileUtil.copy(origFs, orig, destFs, tmp, false, true, CachedConfiguration.getInstance());
      destFs.rename(tmp, dest);
      log.debug("copied " + orig + " to " + dest);
    } catch (IOException ex) {
      try {
        VolumeManager vm = VolumeManagerImpl.get(SiteConfiguration.getInstance());
        FileSystem destFs = vm.getVolumeByPath(dest).getFileSystem();
        destFs.create(dest).close();
        log.warn(" marked " + dest + " failed", ex);
      } catch (IOException e) {
        log.error("Unable to create failure flag file " + dest, e);
      }
    }

  }

}
