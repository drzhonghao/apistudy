import org.apache.cassandra.thrift.ThriftSessionManager;
import org.apache.cassandra.thrift.TCustomNonblockingServerSocket;
import org.apache.cassandra.thrift.*;


import java.net.InetSocketAddress;

import java.nio.channels.SelectionKey;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TTransportException;

public class CustomTNonBlockingServer extends TNonblockingServer
{
    public CustomTNonBlockingServer(Args args)
    {
        super(args);
    }

    @Override
    @SuppressWarnings("resource")
    protected boolean requestInvoke(FrameBuffer frameBuffer)
    {
        TNonblockingSocket socket = (TNonblockingSocket)((CustomFrameBuffer)frameBuffer).getTransport();
        ThriftSessionManager.instance.setCurrentSocket(socket.getSocketChannel().socket().getRemoteSocketAddress());
        frameBuffer.invoke();
        return true;
    }

    public static class Factory implements TServerFactory
    {
        @SuppressWarnings("resource")
        public TServer buildTServer(Args args)
        {
            if (DatabaseDescriptor.getClientEncryptionOptions().enabled)
                throw new RuntimeException("Client SSL is not supported for non-blocking sockets. Please remove client ssl from the configuration.");

            final InetSocketAddress addr = args.addr;
            TNonblockingServerTransport serverTransport;
            try
            {
                serverTransport = new TCustomNonblockingServerSocket(addr, args.keepAlive, args.sendBufferSize, args.recvBufferSize);
            }
            catch (TTransportException e)
            {
                throw new RuntimeException(String.format("Unable to create thrift socket to %s:%s", addr.getAddress(), addr.getPort()), e);
            }

            // This is single threaded hence the invocation will be all
            // in one thread.
            TNonblockingServer.Args serverArgs = new TNonblockingServer.Args(serverTransport).inputTransportFactory(args.inTransportFactory)
                                                                                             .outputTransportFactory(args.outTransportFactory)
                                                                                             .inputProtocolFactory(args.tProtocolFactory)
                                                                                             .outputProtocolFactory(args.tProtocolFactory)
                                                                                             .processor(args.processor);
            return new CustomTNonBlockingServer(serverArgs);
        }
    }

    public class CustomFrameBuffer extends FrameBuffer
    {
        public CustomFrameBuffer(final TNonblockingTransport trans,
          final SelectionKey selectionKey,
          final AbstractSelectThread selectThread)
        {
			super(trans, selectionKey, selectThread);
        }

        public TNonblockingTransport getTransport()
        {
            return this.trans_;
        }
    }
}
