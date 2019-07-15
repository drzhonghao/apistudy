import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.*;



import java.io.IOException;

/** A {@link CompositeReader} which reads multiple indexes, appending
 *  their content. It can be used to create a view on several
 *  sub-readers (like {@link DirectoryReader}) and execute searches on it.
 * 
 * <p> For efficiency, in this API documents are often referred to via
 * <i>document numbers</i>, non-negative integers which each name a unique
 * document in the index.  These document numbers are ephemeral -- they may change
 * as documents are added to and deleted from an index.  Clients should thus not
 * rely on a given document having the same number between sessions.
 * 
 * <p><a name="thread-safety"></a><p><b>NOTE</b>: {@link
 * IndexReader} instances are completely thread
 * safe, meaning multiple threads can call any of its methods,
 * concurrently.  If your application requires external
 * synchronization, you should <b>not</b> synchronize on the
 * <code>IndexReader</code> instance; use your own
 * (non-Lucene) objects instead.
 */
public class MultiReader extends BaseCompositeReader<IndexReader> {
  private final boolean closeSubReaders;
  
 /**
  * <p>Construct a MultiReader aggregating the named set of (sub)readers.
  * <p>Note that all subreaders are closed if this Multireader is closed.</p>
  * @param subReaders set of (sub)readers
  */
  public MultiReader(IndexReader... subReaders) throws IOException {
    this(subReaders, true);
  }

  /**
   * <p>Construct a MultiReader aggregating the named set of (sub)readers.
   * @param subReaders set of (sub)readers; this array will be cloned.
   * @param closeSubReaders indicates whether the subreaders should be closed
   * when this MultiReader is closed
   */
  public MultiReader(IndexReader[] subReaders, boolean closeSubReaders) throws IOException {
    super(subReaders.clone());
    this.closeSubReaders = closeSubReaders;
    if (!closeSubReaders) {
      for (int i = 0; i < subReaders.length; i++) {
        subReaders[i].incRef();
      }
    }
  }

  @Override
  public CacheHelper getReaderCacheHelper() {
    // MultiReader instances can be short-lived, which would make caching trappy
    // so we do not cache on them, unless they wrap a single reader in which
    // case we delegate
    if (getSequentialSubReaders().size() == 1) {
      return getSequentialSubReaders().get(0).getReaderCacheHelper();
    }
    return null;
  }

  @Override
  protected synchronized void doClose() throws IOException {
    IOException ioe = null;
    for (final IndexReader r : getSequentialSubReaders()) {
      try {
        if (closeSubReaders) {
          r.close();
        } else {
          r.decRef();
        }
      } catch (IOException e) {
        if (ioe == null) ioe = e;
      }
    }
    // throw the first exception
    if (ioe != null) throw ioe;
  }
}
