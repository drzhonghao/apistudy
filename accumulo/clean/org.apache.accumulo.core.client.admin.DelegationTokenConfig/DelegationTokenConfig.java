import org.apache.accumulo.core.client.admin.*;


import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;

/**
 * Configuration options for obtaining a delegation token created by
 * {@link SecurityOperations#getDelegationToken(DelegationTokenConfig)}
 *
 * @since 1.7.0
 */
public class DelegationTokenConfig {

  private long lifetime = 0;

  /**
   * Requests a specific lifetime for the token that is different than the default system lifetime.
   * The lifetime must not exceed the secret key lifetime configured on the servers.
   *
   * @param lifetime
   *          Token lifetime
   * @param unit
   *          Unit of time for the lifetime
   * @return this
   */
  public DelegationTokenConfig setTokenLifetime(long lifetime, TimeUnit unit) {
    checkArgument(0 <= lifetime, "Lifetime must be non-negative");
    requireNonNull(unit, "TimeUnit was null");
    this.lifetime = TimeUnit.MILLISECONDS.convert(lifetime, unit);
    return this;
  }

  /**
   * The current token lifetime. A value of zero corresponds to using the system configured
   * lifetime.
   *
   * @param unit
   *          The unit of time the lifetime should be returned in
   * @return Token lifetime in requested unit of time
   */
  public long getTokenLifetime(TimeUnit unit) {
    requireNonNull(unit);
    return unit.convert(lifetime, TimeUnit.MILLISECONDS);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DelegationTokenConfig) {
      DelegationTokenConfig other = (DelegationTokenConfig) o;
      return lifetime == other.lifetime;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Long.valueOf(lifetime).hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(32);
    sb.append("DelegationTokenConfig[lifetime=").append(lifetime).append("ms]");
    return sb.toString();
  }
}
