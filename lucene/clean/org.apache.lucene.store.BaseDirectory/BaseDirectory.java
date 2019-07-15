import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.*;



import java.io.IOException;

/**
 * Base implementation for a concrete {@link Directory} that uses a {@link LockFactory} for locking.
 * @lucene.experimental
 */
public abstract class BaseDirectory extends Directory {

  volatile protected boolean isOpen = true;

  /** Holds the LockFactory instance (implements locking for
   * this Directory instance). */
  protected final LockFactory lockFactory;

  /** Sole constructor. */
  protected BaseDirectory(LockFactory lockFactory) {
    super();
    if (lockFactory == null) {
      throw new NullPointerException("LockFactory must not be null, use an explicit instance!");
    }
    this.lockFactory = lockFactory;
  }

  @Override
  public final Lock obtainLock(String name) throws IOException {
    return lockFactory.obtainLock(this, name);
  }

  @Override
  protected final void ensureOpen() throws AlreadyClosedException {
    if (!isOpen) {
      throw new AlreadyClosedException("this Directory is closed");
    }
  }

  @Override
  public String toString() {
    return super.toString()  + " lockFactory=" + lockFactory;
  }
}
