import org.apache.lucene.replicator.*;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.replicator.ReplicationClient.SourceDirectoryFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;

/**
 * A {@link SourceDirectoryFactory} which returns {@link FSDirectory} under a
 * dedicated session directory. When a session is over, the entire directory is
 * deleted.
 * 
 * @lucene.experimental
 */
public class PerSessionDirectoryFactory implements SourceDirectoryFactory {
  
  private final Path workDir;
  
  /** Constructor with the given sources mapping. */
  public PerSessionDirectoryFactory(Path workDir) {
    this.workDir = workDir;
  }
  
  @Override
  public Directory getDirectory(String sessionID, String source) throws IOException {
    Path sessionDir = workDir.resolve(sessionID);
    Files.createDirectories(sessionDir);
    Path sourceDir = sessionDir.resolve(source);
    Files.createDirectories(sourceDir);
    return FSDirectory.open(sourceDir);
  }
  
  @Override
  public void cleanupSession(String sessionID) throws IOException {
    if (sessionID.isEmpty()) { // protect against deleting workDir entirely!
      throw new IllegalArgumentException("sessionID cannot be empty");
    }
    IOUtils.rm(workDir.resolve(sessionID));
  }
  
}
