import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.*;



import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/** Directory implementation that delegates calls to another directory.
 *  This class can be used to add limitations on top of an existing
 *  {@link Directory} implementation such as
 *  {@link NRTCachingDirectory} or to add additional
 *  sanity checks for tests. However, if you plan to write your own
 *  {@link Directory} implementation, you should consider extending directly
 *  {@link Directory} or {@link BaseDirectory} rather than try to reuse
 *  functionality of existing {@link Directory}s by extending this class.
 *  @lucene.internal */
public abstract class FilterDirectory extends Directory {

  /** Get the wrapped instance by <code>dir</code> as long as this reader is
   *  an instance of {@link FilterDirectory}.  */
  public static Directory unwrap(Directory dir) {
    while (dir instanceof FilterDirectory) {
      dir = ((FilterDirectory) dir).in;
    }
    return dir;
  }

  protected final Directory in;

  /** Sole constructor, typically called from sub-classes. */
  protected FilterDirectory(Directory in) {
    this.in = in;
  }

  /** Return the wrapped {@link Directory}. */
  public final Directory getDelegate() {
    return in;
  }

  @Override
  public String[] listAll() throws IOException {
    return in.listAll();
  }

  @Override
  public void deleteFile(String name) throws IOException {
    in.deleteFile(name);
  }

  @Override
  public long fileLength(String name) throws IOException {
    return in.fileLength(name);
  }

  @Override
  public IndexOutput createOutput(String name, IOContext context)
      throws IOException {
    return in.createOutput(name, context);
  }

  @Override
  public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
    return in.createTempOutput(prefix, suffix, context);
  }

  @Override
  public void sync(Collection<String> names) throws IOException {
    in.sync(names);
  }

  @Override
  public void rename(String source, String dest) throws IOException {
    in.rename(source, dest);
  }

  @Override
  public void syncMetaData() throws IOException {
    in.syncMetaData();
  }

  @Override
  public IndexInput openInput(String name, IOContext context)
      throws IOException {
    return in.openInput(name, context);
  }

  @Override
  public Lock obtainLock(String name) throws IOException {
    return in.obtainLock(name);
  }

  @Override
  public void close() throws IOException {
    in.close();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + in.toString() + ")";
  }

  @Override
  public Set<String> getPendingDeletions() throws IOException {
    return super.getPendingDeletions();
  }
}
