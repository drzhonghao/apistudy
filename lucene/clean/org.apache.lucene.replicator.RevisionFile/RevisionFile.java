import org.apache.lucene.replicator.*;


/**
 * Describes a file in a {@link Revision}. A file has a source, which allows a
 * single revision to contain files from multiple sources (e.g. multiple
 * indexes).
 * 
 * @lucene.experimental
 */
public class RevisionFile {
  
  /** The name of the file. */
  public final String fileName;
  
  /** The size of the file denoted by {@link #fileName}. */
  public long size = -1;
  
  /** Constructor with the given file name. */
  public RevisionFile(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      throw new IllegalArgumentException("fileName must not be null or empty");
    }
    this.fileName = fileName;
  }
  
  @Override
  public boolean equals(Object obj) {
    RevisionFile other = (RevisionFile) obj;
    return fileName.equals(other.fileName) && size == other.size;
  }
  
  @Override
  public int hashCode() {
    return fileName.hashCode() ^ (int) (size ^ (size >>> 32));
  }
  
  @Override
  public String toString() {
    return "fileName=" + fileName + " size=" + size;
  }
  
}
