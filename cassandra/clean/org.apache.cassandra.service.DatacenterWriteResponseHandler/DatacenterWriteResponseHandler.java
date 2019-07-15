import org.apache.cassandra.service.*;


import java.net.InetAddress;
import java.util.Collection;

import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.db.ConsistencyLevel;
import org.apache.cassandra.db.WriteType;

/**
 * This class blocks for a quorum of responses _in the local datacenter only_ (CL.LOCAL_QUORUM).
 */
public class DatacenterWriteResponseHandler<T> extends WriteResponseHandler<T>
{
    public DatacenterWriteResponseHandler(Collection<InetAddress> naturalEndpoints,
                                          Collection<InetAddress> pendingEndpoints,
                                          ConsistencyLevel consistencyLevel,
                                          Keyspace keyspace,
                                          Runnable callback,
                                          WriteType writeType,
                                          long queryStartNanoTime)
    {
        super(naturalEndpoints, pendingEndpoints, consistencyLevel, keyspace, callback, writeType, queryStartNanoTime);
        assert consistencyLevel.isDatacenterLocal();
    }

    @Override
    public void response(MessageIn<T> message)
    {
        if (message == null || waitingFor(message.from))
            super.response(message);
    }

    @Override
    protected int totalBlockFor()
    {
        // during bootstrap, include pending endpoints (only local here) in the count
        // or we may fail the consistency level guarantees (see #833, #8058)
        return consistencyLevel.blockFor(keyspace) + consistencyLevel.countLocalEndpoints(pendingEndpoints);
    }

    @Override
    protected boolean waitingFor(InetAddress from)
    {
        return consistencyLevel.isLocal(from);
    }
}
