import org.apache.cassandra.service.paxos.*;


import org.apache.cassandra.db.WriteResponse;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.tracing.Tracing;

public class CommitVerbHandler implements IVerbHandler<Commit>
{
    public void doVerb(MessageIn<Commit> message, int id)
    {
        PaxosState.commit(message.payload);

        Tracing.trace("Enqueuing acknowledge to {}", message.from);
        MessagingService.instance().sendReply(WriteResponse.createMessage(), id, message.from);
    }
}
