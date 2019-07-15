import org.apache.accumulo.core.util.ratelimit.*;


/** A rate limiter which doesn't actually limit rates at all. */
public class NullRateLimiter implements RateLimiter {
  public static final NullRateLimiter INSTANCE = new NullRateLimiter();

  private NullRateLimiter() {}

  @Override
  public long getRate() {
    return 0;
  }

  @Override
  public void acquire(long permits) {}

}
