import org.apache.accumulo.core.client.impl.ClientContext;
import org.apache.accumulo.core.client.impl.*;


import static java.util.Objects.requireNonNull;

import java.util.Objects;

import org.apache.accumulo.core.rpc.SaslConnectionParams;
import org.apache.accumulo.core.rpc.SslConnectionParams;
import org.apache.accumulo.core.util.HostAndPort;

import com.google.common.annotations.VisibleForTesting;

@VisibleForTesting
public class ThriftTransportKey {
  private final HostAndPort server;
  private final long timeout;
  private final SslConnectionParams sslParams;
  private final SaslConnectionParams saslParams;

  private int hash = -1;

  @VisibleForTesting
  public ThriftTransportKey(HostAndPort server, long timeout, ClientContext context) {
    requireNonNull(server, "location is null");
    this.server = server;
    this.timeout = timeout;
    this.sslParams = context.getClientSslParams();
    this.saslParams = context.getSaslParams();
    if (null != saslParams) {
      // TSasl and TSSL transport factories don't play nicely together
      if (null != sslParams) {
        throw new RuntimeException("Cannot use both SSL and SASL thrift transports");
      }
    }
  }

  /**
   * Visible only for testing
   */
  ThriftTransportKey(HostAndPort server, long timeout, SslConnectionParams sslParams,
      SaslConnectionParams saslParams) {
    requireNonNull(server, "location is null");
    this.server = server;
    this.timeout = timeout;
    this.sslParams = sslParams;
    this.saslParams = saslParams;
  }

  HostAndPort getServer() {
    return server;
  }

  long getTimeout() {
    return timeout;
  }

  public boolean isSsl() {
    return sslParams != null;
  }

  public boolean isSasl() {
    return saslParams != null;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ThriftTransportKey))
      return false;
    ThriftTransportKey ttk = (ThriftTransportKey) o;
    return server.equals(ttk.server) && timeout == ttk.timeout
        && (!isSsl() || (ttk.isSsl() && sslParams.equals(ttk.sslParams)))
        && (!isSasl() || (ttk.isSasl() && saslParams.equals(ttk.saslParams)));
  }

  public final void precomputeHashCode() {
    hashCode();
  }

  @Override
  public int hashCode() {
    if (hash == -1)
      hash = Objects.hash(server, timeout, sslParams, saslParams);
    return hash;
  }

  @Override
  public String toString() {
    String prefix = "";
    if (isSsl()) {
      prefix = "ssl:";
    } else if (isSasl()) {
      prefix = saslParams.toString() + ":";
    }
    return prefix + server + " (" + Long.toString(timeout) + ")";
  }

  public SslConnectionParams getSslParams() {
    return sslParams;
  }

  public SaslConnectionParams getSaslParams() {
    return saslParams;
  }
}
