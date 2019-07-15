import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.*;



import java.io.IOException;
import java.util.Collection;

/** 
 * This class makes a best-effort check that a provided {@link Lock}
 * is valid before any destructive filesystem operation.
 */
public final class LockValidatingDirectoryWrapper extends FilterDirectory {
  private final Lock writeLock;

  public LockValidatingDirectoryWrapper(Directory in, Lock writeLock) {
    super(in);
    this.writeLock = writeLock;
  }

  @Override
  public void deleteFile(String name) throws IOException {
    writeLock.ensureValid();
    in.deleteFile(name);
  }

  @Override
  public IndexOutput createOutput(String name, IOContext context) throws IOException {
    writeLock.ensureValid();
    return in.createOutput(name, context);
  }

  @Override
  public void copyFrom(Directory from, String src, String dest, IOContext context) throws IOException {
    writeLock.ensureValid();
    in.copyFrom(from, src, dest, context);
  }

  @Override
  public void rename(String source, String dest) throws IOException {
    writeLock.ensureValid();
    in.rename(source, dest);
  }

  @Override
  public void syncMetaData() throws IOException {
    writeLock.ensureValid();
    in.syncMetaData();
  }

  @Override
  public void sync(Collection<String> names) throws IOException {
    writeLock.ensureValid();
    in.sync(names);
  }
}
