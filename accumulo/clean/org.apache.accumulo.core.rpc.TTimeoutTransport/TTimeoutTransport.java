import org.apache.accumulo.core.rpc.*;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.spi.SelectorProvider;

import org.apache.accumulo.core.util.HostAndPort;
import org.apache.hadoop.net.NetUtils;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;

public class TTimeoutTransport {

  private static volatile Method GET_INPUT_STREAM_METHOD = null;

  private static Method getNetUtilsInputStreamMethod() {
    if (null == GET_INPUT_STREAM_METHOD) {
      synchronized (TTimeoutTransport.class) {
        if (null == GET_INPUT_STREAM_METHOD) {
          try {
            GET_INPUT_STREAM_METHOD = NetUtils.class.getMethod("getInputStream", Socket.class,
                Long.TYPE);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      }
    }

    return GET_INPUT_STREAM_METHOD;
  }

  private static InputStream getInputStream(Socket socket, long timeout) {
    try {
      return (InputStream) getNetUtilsInputStreamMethod().invoke(null, socket, timeout);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static TTransport create(HostAndPort addr, long timeoutMillis) throws IOException {
    return create(new InetSocketAddress(addr.getHost(), addr.getPort()), timeoutMillis);
  }

  public static TTransport create(SocketAddress addr, long timeoutMillis) throws IOException {
    Socket socket = null;
    try {
      socket = SelectorProvider.provider().openSocketChannel().socket();
      socket.setSoLinger(false, 0);
      socket.setTcpNoDelay(true);
      socket.connect(addr);
      InputStream input = new BufferedInputStream(getInputStream(socket, timeoutMillis), 1024 * 10);
      OutputStream output = new BufferedOutputStream(
          NetUtils.getOutputStream(socket, timeoutMillis), 1024 * 10);
      return new TIOStreamTransport(input, output);
    } catch (IOException e) {
      try {
        if (socket != null)
          socket.close();
      } catch (IOException ioe) {}

      throw e;
    }
  }
}
