import org.apache.cassandra.streaming.*;


import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.net.OutboundTcpConnectionPool;

public class DefaultConnectionFactory implements StreamConnectionFactory
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultConnectionFactory.class);

    private static final int MAX_CONNECT_ATTEMPTS = 3;

    /**
     * Connect to peer and start exchanging message.
     * When connect attempt fails, this retries for maximum of MAX_CONNECT_ATTEMPTS times.
     *
     * @param peer the peer to connect to.
     * @return the created socket.
     *
     * @throws IOException when connection failed.
     */
    public Socket createConnection(InetAddress peer) throws IOException
    {
        int attempts = 0;
        while (true)
        {
            Socket socket = null;
            try
            {
                socket = OutboundTcpConnectionPool.newSocket(peer);
                socket.setSoTimeout(DatabaseDescriptor.getStreamingSocketTimeout());
                socket.setKeepAlive(true);
                return socket;
            }
            catch (IOException e)
            {
                if (socket != null)
                {
                    socket.close();
                }
                if (++attempts >= MAX_CONNECT_ATTEMPTS)
                    throw e;

                long waitms = DatabaseDescriptor.getRpcTimeout() * (long)Math.pow(2, attempts);
                logger.warn("Failed attempt {} to connect to {}. Retrying in {} ms. ({})", attempts, peer, waitms, e);
                try
                {
                    Thread.sleep(waitms);
                }
                catch (InterruptedException wtf)
                {
                    throw new IOException("interrupted", wtf);
                }
            }
        }
    }
}
