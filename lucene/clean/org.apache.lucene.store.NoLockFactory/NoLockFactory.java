import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.*;



import java.io.IOException;

/**
 * Use this {@link LockFactory} to disable locking entirely.
 * This is a singleton, you have to use {@link #INSTANCE}.
 *
 * @see LockFactory
 */

public final class NoLockFactory extends LockFactory {

  /** The singleton */
  public static final NoLockFactory INSTANCE = new NoLockFactory();
  
  // visible for AssertingLock!
  static final NoLock SINGLETON_LOCK = new NoLock();
  
  private NoLockFactory() {}

  @Override
  public Lock obtainLock(Directory dir, String lockName) {
    return SINGLETON_LOCK;
  }
  
  private static class NoLock extends Lock {
    @Override
    public void close() {
    }

    @Override
    public void ensureValid() throws IOException {
    }

    @Override
    public String toString() {
      return "NoLock";
    }
  }
}
