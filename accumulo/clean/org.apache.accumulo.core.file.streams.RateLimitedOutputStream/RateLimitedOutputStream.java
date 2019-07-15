import org.apache.accumulo.core.file.streams.PositionedOutputs;
import org.apache.accumulo.core.file.streams.*;


import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.accumulo.core.util.ratelimit.NullRateLimiter;
import org.apache.accumulo.core.util.ratelimit.RateLimiter;

/**
 * A decorator for {@code OutputStream} which limits the rate at which data may be written.
 */
public class RateLimitedOutputStream extends FilterOutputStream implements PositionedOutput {
  private final RateLimiter writeLimiter;

  public RateLimitedOutputStream(OutputStream wrappedStream, RateLimiter writeLimiter) {
    super(PositionedOutputs.wrap(wrappedStream));
    this.writeLimiter = writeLimiter == null ? NullRateLimiter.INSTANCE : writeLimiter;
  }

  @Override
  public void write(int i) throws IOException {
    writeLimiter.acquire(1);
    out.write(i);
  }

  @Override
  public void write(byte[] buffer, int offset, int length) throws IOException {
    writeLimiter.acquire(length);
    out.write(buffer, offset, length);
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  @Override
  public long position() throws IOException {
    return ((PositionedOutput) out).position();
  }
}
