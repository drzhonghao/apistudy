import org.apache.accumulo.core.rpc.FilterTransport;
import org.apache.accumulo.core.rpc.*;


import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * The Thrift SASL transports call Sasl.createSaslServer and Sasl.createSaslClient inside open().
 * So, we need to assume the correct UGI when the transport is opened so that the SASL mechanisms
 * have access to the right principal. This transport wraps the Sasl transports to set up the right
 * UGI context for open().
 *
 * This is used on the client side, where the API explicitly opens a transport to the server.
 *
 * Lifted from Apache Hive 0.14
 */
public class UGIAssumingTransport extends FilterTransport {
  protected UserGroupInformation ugi;

  public UGIAssumingTransport(TTransport wrapped, UserGroupInformation ugi) {
    super(wrapped);
    this.ugi = ugi;
  }

  @Override
  public void open() throws TTransportException {
    final AtomicReference<TTransportException> holder = new AtomicReference<>(null);
    try {
      ugi.doAs(new PrivilegedExceptionAction<Void>() {
        @Override
        public Void run() {
          try {
            getWrapped().open();
          } catch (TTransportException tte) {
            holder.set(tte);
          }
          return null;
        }
      });
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    // Make sure the transport exception gets (re)thrown if it happened
    TTransportException tte = holder.get();
    if (null != tte) {
      throw tte;
    }
  }
}
