import org.apache.cassandra.batchlog.Batch;
import org.apache.cassandra.batchlog.BatchlogManager;
import org.apache.cassandra.batchlog.*;


import org.apache.cassandra.db.WriteResponse;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessagingService;

public final class BatchStoreVerbHandler implements IVerbHandler<Batch>
{
    public void doVerb(MessageIn<Batch> message, int id)
    {
        BatchlogManager.store(message.payload);
        MessagingService.instance().sendReply(WriteResponse.createMessage(), id, message.from);
    }
}
