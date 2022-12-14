import org.apache.accumulo.start.classloader.vfs.*;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.FileReplicator;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponent;
import org.apache.commons.vfs2.provider.VfsComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class UniqueFileReplicator implements VfsComponent, FileReplicator {

  private static final char[] TMP_RESERVED_CHARS = new char[] {'?', '/', '\\', ' ', '&', '"', '\'',
      '*', '#', ';', ':', '<', '>', '|'};
  private static final Logger log = LoggerFactory.getLogger(UniqueFileReplicator.class);

  private File tempDir;
  private VfsComponentContext context;
  private List<File> tmpFiles = Collections.synchronizedList(new ArrayList<File>());

  public UniqueFileReplicator(File tempDir) {
    this.tempDir = tempDir;
    if (!tempDir.exists() && !tempDir.mkdirs())
      log.warn("Unexpected error creating directory " + tempDir);
  }

  @Override
  public File replicateFile(FileObject srcFile, FileSelector selector) throws FileSystemException {
    String baseName = srcFile.getName().getBaseName();

    try {
      String safeBasename = UriParser.encode(baseName, TMP_RESERVED_CHARS).replace('%', '_');
      File file = File.createTempFile("vfsr_", "_" + safeBasename, tempDir);
      file.deleteOnExit();

      final FileObject destFile = context.toFileObject(file);
      destFile.copyFrom(srcFile, selector);

      return file;
    } catch (IOException e) {
      throw new FileSystemException(e);
    }
  }

  @Override
  public void setLogger(Log logger) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setContext(VfsComponentContext context) {
    this.context = context;
  }

  @Override
  public void init() throws FileSystemException {

  }

  @Override
  public void close() {
    synchronized (tmpFiles) {
      for (File tmpFile : tmpFiles) {
        if (!tmpFile.delete())
          log.warn("File does not exist: " + tmpFile);
      }
    }

    if (tempDir.exists()) {
      String[] list = tempDir.list();
      int numChildren = list == null ? 0 : list.length;
      if (0 == numChildren && !tempDir.delete())
        log.warn("Cannot delete empty directory: " + tempDir);
    }
  }
}
