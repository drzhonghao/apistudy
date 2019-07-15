import org.apache.cassandra.batchlog.BatchlogManager;
import org.apache.cassandra.batchlog.*;


import java.util.UUID;

import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;

public final class BatchRemoveVerbHandler implements IVerbHandler<UUID>
{
    public void doVerb(MessageIn<UUID> message, int id)
    {
        BatchlogManager.remove(message.payload);
    }
}
