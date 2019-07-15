import org.apache.cassandra.utils.*;


import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.rmi.server.RMIServerSocketFactory;
import javax.net.ServerSocketFactory;

public class RMIServerSocketFactoryImpl implements RMIServerSocketFactory
{
    // Address to bind server sockets too, may be null indicating all local interfaces are to be bound
    private final InetAddress bindAddress;

    public RMIServerSocketFactoryImpl(InetAddress bindAddress)
    {
        this.bindAddress = bindAddress;
    }

    public ServerSocket createServerSocket(final int pPort) throws IOException
    {
        ServerSocket socket = ServerSocketFactory.getDefault().createServerSocket(pPort, 0, bindAddress);
        try
        {
            socket.setReuseAddress(true);
            return socket;
        }
        catch (SocketException e)
        {
            socket.close();
            throw e;
        }
    }

    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (obj == this)
        {
            return true;
        }

        return obj.getClass().equals(getClass());
    }

    public int hashCode()
    {
        return RMIServerSocketFactoryImpl.class.hashCode();
    }
}

