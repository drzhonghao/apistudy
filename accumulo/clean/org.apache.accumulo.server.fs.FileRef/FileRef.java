import org.apache.accumulo.server.fs.*;


import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.server.fs.VolumeManager.FileType;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;

/**
 * This is a glue object, to convert short file references to long references. The metadata may
 * contain old relative file references. This class keeps track of the short file reference, so it
 * can be removed properly from the metadata tables.
 */
public class FileRef implements Comparable<FileRef> {
  private String metaReference; // something like ../2/d-00000/A00001.rf
  private Path fullReference; // something like hdfs://nn:9001/accumulo/tables/2/d-00000/A00001.rf
  private Path suffix;

  public FileRef(VolumeManager fs, Key key) {
    this(key.getColumnQualifier().toString(), fs.getFullPath(key));
  }

  public FileRef(String metaReference, Path fullReference) {
    this.metaReference = metaReference;
    this.fullReference = fullReference;
    this.suffix = extractSuffix(fullReference);
  }

  public FileRef(String path) {
    this(path, new Path(path));
  }

  @Override
  public String toString() {
    return fullReference.toString();
  }

  public Path path() {
    return fullReference;
  }

  public Text meta() {
    return new Text(metaReference);
  }

  static Path extractSuffix(Path path) {
    String pstr = path.toString();
    int index = pstr.lastIndexOf(FileType.TABLE.getDirectory());
    if (index < 0)
      throw new IllegalArgumentException("Invalid table path " + pstr);

    try {
      Path parent = path.getParent().getParent();
      if (!parent.getName().equals(FileType.TABLE.getDirectory())
          && !parent.getParent().getName().equals(FileType.TABLE.getDirectory()))
        throw new IllegalArgumentException("Invalid table path " + pstr);
    } catch (NullPointerException npe) {
      throw new IllegalArgumentException("Invalid table path " + pstr);
    }

    return new Path(pstr.substring(index + FileType.TABLE.getDirectory().length() + 1));
  }

  @Override
  public int compareTo(FileRef o) {
    return suffix.compareTo(o.suffix);
  }

  @Override
  public int hashCode() {
    return suffix.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FileRef) {
      return compareTo((FileRef) obj) == 0;
    }
    return false;
  }

}
