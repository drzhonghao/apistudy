import org.apache.accumulo.server.master.recovery.*;


import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.accumulo.server.fs.ViewFSUtils;
import org.apache.accumulo.server.fs.VolumeManager;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HadoopLogCloser implements LogCloser {

  private static final Logger log = LoggerFactory.getLogger(HadoopLogCloser.class);

  @Override
  public long close(AccumuloConfiguration conf, VolumeManager fs, Path source) throws IOException {
    FileSystem ns = fs.getVolumeByPath(source).getFileSystem();

    // if path points to a viewfs path, then resolve to underlying filesystem
    if (ViewFSUtils.isViewFS(ns)) {
      Path newSource = ns.resolvePath(source);
      if (!newSource.equals(source) && newSource.toUri().getScheme() != null) {
        ns = newSource.getFileSystem(CachedConfiguration.getInstance());
        source = newSource;
      }
    }

    if (ns instanceof DistributedFileSystem) {
      DistributedFileSystem dfs = (DistributedFileSystem) ns;
      try {
        if (!dfs.recoverLease(source)) {
          log.info("Waiting for file to be closed " + source.toString());
          return conf.getTimeInMillis(Property.MASTER_LEASE_RECOVERY_WAITING_PERIOD);
        }
        log.info("Recovered lease on " + source.toString());
      } catch (FileNotFoundException ex) {
        throw ex;
      } catch (Exception ex) {
        log.warn("Error recovering lease on " + source.toString(), ex);
        ns.append(source).close();
        log.info("Recovered lease on " + source.toString() + " using append");
      }
    } else if (ns instanceof LocalFileSystem || ns instanceof RawLocalFileSystem) {
      // ignore
    } else {
      throw new IllegalStateException(
          "Don't know how to recover a lease for " + ns.getClass().getName());
    }
    return 0;
  }

}
