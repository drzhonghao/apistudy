import org.apache.cassandra.thrift.*;


import java.net.InetSocketAddress;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.QueryState;

/**
 * ClientState used by thrift that also provide a QueryState.
 *
 * Thrift is intrinsically synchronous so there could be only one query per
 * client at a given time. So ClientState and QueryState can be merge into the
 * same object.
 */
public class ThriftClientState extends ClientState
{
    private final QueryState queryState;

    public ThriftClientState(InetSocketAddress remoteAddress)
    {
        super(remoteAddress);
        this.queryState = new QueryState(this);
    }

    public QueryState getQueryState()
    {
        return queryState;
    }

    public String getSchedulingValue()
    {
        switch(DatabaseDescriptor.getRequestSchedulerId())
        {
            case keyspace: return getRawKeyspace();
        }
        return "default";
    }
}
