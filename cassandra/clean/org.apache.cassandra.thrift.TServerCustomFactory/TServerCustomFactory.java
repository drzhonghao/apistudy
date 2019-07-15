import org.apache.cassandra.thrift.ThriftServer;
import org.apache.cassandra.thrift.CustomTThreadPoolServer;
import org.apache.cassandra.thrift.CustomTNonBlockingServer;
import org.apache.cassandra.thrift.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.thrift.server.TServer;

/**
 * Helper implementation to create a thrift TServer based on one of the common types we support (sync, hsha),
 * or a custom type by setting the fully qualified java class name in the rpc_server_type setting.
 */
public class TServerCustomFactory implements TServerFactory
{
    private static Logger logger = LoggerFactory.getLogger(TServerCustomFactory.class);
    private final String serverType;

    public TServerCustomFactory(String serverType)
    {
        assert serverType != null;
        this.serverType = serverType;
    }

    public TServer buildTServer(TServerFactory.Args args)
    {
        TServer server;
        if (ThriftServer.ThriftServerType.SYNC.equalsIgnoreCase(serverType))
        {
            server = new CustomTThreadPoolServer.Factory().buildTServer(args);
        }
        else if(ThriftServer.ThriftServerType.ASYNC.equalsIgnoreCase(serverType))
        {
            server = new CustomTNonBlockingServer.Factory().buildTServer(args);
            logger.info("Using non-blocking/asynchronous thrift server on {} : {}", args.addr.getHostName(), args.addr.getPort());
        }
        else if(ThriftServer.ThriftServerType.HSHA.equalsIgnoreCase(serverType))
        {
            server = new THsHaDisruptorServer.Factory().buildTServer(args);
            logger.info("Using custom half-sync/half-async thrift server on {} : {}", args.addr.getHostName(), args.addr.getPort());
        }
        else
        {
            TServerFactory serverFactory;
            try
            {
                serverFactory = (TServerFactory) Class.forName(serverType).newInstance();
            }
            catch (Exception e)
            {
                throw new RuntimeException("Failed to instantiate server factory:" + serverType, e);
            }
            server = serverFactory.buildTServer(args);
            logger.info("Using custom thrift server {} on {} : {}", server.getClass().getName(), args.addr.getHostName(), args.addr.getPort());
        }
        return server;
    }
}
