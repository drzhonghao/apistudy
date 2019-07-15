import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.*;


import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessagingService;

public class ReadRepairVerbHandler implements IVerbHandler<Mutation>
{
    public void doVerb(MessageIn<Mutation> message, int id)
    {
        message.payload.apply();
        MessagingService.instance().sendReply(WriteResponse.createMessage(), id, message.from);
    }
}
