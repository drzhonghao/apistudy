import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RateLimiter;
import org.apache.lucene.store.*;



import java.io.IOException;

/**
 * A {@link RateLimiter rate limiting} {@link IndexOutput}
 * 
 * @lucene.internal
 */

public final class RateLimitedIndexOutput extends IndexOutput {
  
  private final IndexOutput delegate;
  private final RateLimiter rateLimiter;

  /** How many bytes we've written since we last called rateLimiter.pause. */
  private long bytesSinceLastPause;
  
  /** Cached here not not always have to call RateLimiter#getMinPauseCheckBytes()
   * which does volatile read. */
  private long currentMinPauseCheckBytes;

  public RateLimitedIndexOutput(final RateLimiter rateLimiter, final IndexOutput delegate) {
    super("RateLimitedIndexOutput(" + delegate + ")", delegate.getName());
    this.delegate = delegate;
    this.rateLimiter = rateLimiter;
    this.currentMinPauseCheckBytes = rateLimiter.getMinPauseCheckBytes();
  }
  
  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public long getFilePointer() {
    return delegate.getFilePointer();
  }

  @Override
  public long getChecksum() throws IOException {
    return delegate.getChecksum();
  }

  @Override
  public void writeByte(byte b) throws IOException {
    bytesSinceLastPause++;
    checkRate();
    delegate.writeByte(b);
  }

  @Override
  public void writeBytes(byte[] b, int offset, int length) throws IOException {
    bytesSinceLastPause += length;
    checkRate();
    delegate.writeBytes(b, offset, length);
  }
  
  private void checkRate() throws IOException {
    if (bytesSinceLastPause > currentMinPauseCheckBytes) {
      rateLimiter.pause(bytesSinceLastPause);
      bytesSinceLastPause = 0;
      currentMinPauseCheckBytes = rateLimiter.getMinPauseCheckBytes();
    }    
  }
}
