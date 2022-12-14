import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.io.FSError;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.tracing.Tracing;

public class TruncateVerbHandler implements IVerbHandler<Truncation>
{
    private static final Logger logger = LoggerFactory.getLogger(TruncateVerbHandler.class);

    public void doVerb(MessageIn<Truncation> message, int id)
    {
        Truncation t = message.payload;
        Tracing.trace("Applying truncation of {}.{}", t.keyspace, t.columnFamily);
        try
        {
            ColumnFamilyStore cfs = Keyspace.open(t.keyspace).getColumnFamilyStore(t.columnFamily);
            cfs.truncateBlocking();
        }
        catch (Exception e)
        {
            logger.error("Error in truncation", e);
            respondError(t, message);

            if (FSError.findNested(e) != null)
                throw FSError.findNested(e);
        }
        Tracing.trace("Enqueuing response to truncate operation to {}", message.from);

        TruncateResponse response = new TruncateResponse(t.keyspace, t.columnFamily, true);
        logger.trace("{} applied.  Enqueuing response to {}@{} ", new Object[]{ t, id, message.from });
        MessagingService.instance().sendReply(response.createMessage(), id, message.from);
    }

    private static void respondError(Truncation t, MessageIn truncateRequestMessage)
    {
        TruncateResponse response = new TruncateResponse(t.keyspace, t.columnFamily, false);
        MessagingService.instance().sendOneWay(response.createMessage(), truncateRequestMessage.from);
    }
}
