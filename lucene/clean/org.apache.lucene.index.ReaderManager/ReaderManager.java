import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.*;



import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;

/**
 * Utility class to safely share {@link DirectoryReader} instances across
 * multiple threads, while periodically reopening. This class ensures each
 * reader is closed only once all threads have finished using it.
 * 
 * @see SearcherManager
 * 
 * @lucene.experimental
 */
public final class ReaderManager extends ReferenceManager<DirectoryReader> {

  /**
   * Creates and returns a new ReaderManager from the given
   * {@link IndexWriter}.
   * 
   * @param writer
   *          the IndexWriter to open the IndexReader from.
   * 
   * @throws IOException If there is a low-level I/O error
   */
  public ReaderManager(IndexWriter writer) throws IOException {
    this(writer, true, false);
  }

  /**
   * Expert: creates and returns a new ReaderManager from the given
   * {@link IndexWriter}, controlling whether past deletions should be applied.
   * 
   * @param writer
   *          the IndexWriter to open the IndexReader from.
   * @param applyAllDeletes
   *          If <code>true</code>, all buffered deletes will be applied (made
   *          visible) in the {@link IndexSearcher} / {@link DirectoryReader}.
   *          If <code>false</code>, the deletes may or may not be applied, but
   *          remain buffered (in IndexWriter) so that they will be applied in
   *          the future. Applying deletes can be costly, so if your app can
   *          tolerate deleted documents being returned you might gain some
   *          performance by passing <code>false</code>. See
   *          {@link DirectoryReader#openIfChanged(DirectoryReader, IndexWriter, boolean)}.
   * @param writeAllDeletes
   *          If <code>true</code>, new deletes will be forcefully written to index files.
   * 
   * @throws IOException If there is a low-level I/O error
   */
  public ReaderManager(IndexWriter writer, boolean applyAllDeletes, boolean writeAllDeletes) throws IOException {
    current = DirectoryReader.open(writer, applyAllDeletes, writeAllDeletes);
  }
  
  /**
   * Creates and returns a new ReaderManager from the given {@link Directory}. 
   * @param dir the directory to open the DirectoryReader on.
   *        
   * @throws IOException If there is a low-level I/O error
   */
  public ReaderManager(Directory dir) throws IOException {
    current = DirectoryReader.open(dir);
  }

  /**
   * Creates and returns a new ReaderManager from the given
   * already-opened {@link DirectoryReader}, stealing
   * the incoming reference.
   *
   * @param reader the directoryReader to use for future reopens
   *        
   * @throws IOException If there is a low-level I/O error
   */
  public ReaderManager(DirectoryReader reader) throws IOException {
    current = reader;
  }

  @Override
  protected void decRef(DirectoryReader reference) throws IOException {
    reference.decRef();
  }
  
  @Override
  protected DirectoryReader refreshIfNeeded(DirectoryReader referenceToRefresh) throws IOException {
    return DirectoryReader.openIfChanged(referenceToRefresh);
  }
  
  @Override
  protected boolean tryIncRef(DirectoryReader reference) {
    return reference.tryIncRef();
  }

  @Override
  protected int getRefCount(DirectoryReader reference) {
    return reference.getRefCount();
  }

}
