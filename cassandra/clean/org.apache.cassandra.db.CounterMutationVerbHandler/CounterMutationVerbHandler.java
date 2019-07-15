import org.apache.cassandra.db.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.StorageProxy;
import org.apache.cassandra.utils.FBUtilities;

public class CounterMutationVerbHandler implements IVerbHandler<CounterMutation>
{
    private static final Logger logger = LoggerFactory.getLogger(CounterMutationVerbHandler.class);

    public void doVerb(final MessageIn<CounterMutation> message, final int id)
    {
        long queryStartNanoTime = System.nanoTime();
        final CounterMutation cm = message.payload;
        logger.trace("Applying forwarded {}", cm);

        String localDataCenter = DatabaseDescriptor.getEndpointSnitch().getDatacenter(FBUtilities.getBroadcastAddress());
        // We should not wait for the result of the write in this thread,
        // otherwise we could have a distributed deadlock between replicas
        // running this VerbHandler (see #4578).
        // Instead, we use a callback to send the response. Note that the callback
        // will not be called if the request timeout, but this is ok
        // because the coordinator of the counter mutation will timeout on
        // it's own in that case.
        StorageProxy.applyCounterMutationOnLeader(cm, localDataCenter, new Runnable()
        {
            public void run()
            {
                MessagingService.instance().sendReply(WriteResponse.createMessage(), id, message.from);
            }
        }, queryStartNanoTime);
    }
}
