import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.*;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A {@link LockFactory} that wraps another {@link
 * LockFactory} and verifies that each lock obtain/release
 * is "correct" (never results in two processes holding the
 * lock at the same time).  It does this by contacting an
 * external server ({@link LockVerifyServer}) to assert that
 * at most one process holds the lock at a time.  To use
 * this, you should also run {@link LockVerifyServer} on the
 * host and port matching what you pass to the constructor.
 *
 * @see LockVerifyServer
 * @see LockStressTest
 */

public final class VerifyingLockFactory extends LockFactory {

  final LockFactory lf;
  final InputStream in;
  final OutputStream out;

  private class CheckedLock extends Lock {
    private final Lock lock;

    public CheckedLock(Lock lock) throws IOException {
      this.lock = lock;
      verify((byte) 1);
    }

    @Override
    public void ensureValid() throws IOException {
      lock.ensureValid();
    }

    @Override
    public void close() throws IOException {
      try (Lock l = lock) {
        l.ensureValid();
        verify((byte) 0);
      }
    }

    private void verify(byte message) throws IOException {
      out.write(message);
      out.flush();
      final int ret = in.read();
      if (ret < 0) {
        throw new IllegalStateException("Lock server died because of locking error.");
      }
      if (ret != message) {
        throw new IOException("Protocol violation.");
      }
    }
  }

  /**
   * @param lf the LockFactory that we are testing
   * @param in the socket's input to {@link LockVerifyServer}
   * @param out the socket's output to {@link LockVerifyServer}
  */
  public VerifyingLockFactory(LockFactory lf, InputStream in, OutputStream out) throws IOException {
    this.lf = lf;
    this.in = in;
    this.out = out;
  }

  @Override
  public Lock obtainLock(Directory dir, String lockName) throws IOException {
    return new CheckedLock(lf.obtainLock(dir, lockName));
  }
}
