import org.apache.cassandra.streaming.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.StorageService;

public class ReplicationFinishedVerbHandler implements IVerbHandler
{
    private static final Logger logger = LoggerFactory.getLogger(ReplicationFinishedVerbHandler.class);

    public void doVerb(MessageIn msg, int id)
    {
        StorageService.instance.confirmReplication(msg.from);
        MessageOut response = new MessageOut(MessagingService.Verb.INTERNAL_RESPONSE);
        if (logger.isDebugEnabled())
            logger.debug("Replying to {}@{}", id, msg.from);
        MessagingService.instance().sendReply(response, id, msg.from);
    }
}
