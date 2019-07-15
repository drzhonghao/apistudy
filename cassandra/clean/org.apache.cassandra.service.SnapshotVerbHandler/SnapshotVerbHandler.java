import org.apache.cassandra.service.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.SnapshotCommand;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;

public class SnapshotVerbHandler implements IVerbHandler<SnapshotCommand>
{
    private static final Logger logger = LoggerFactory.getLogger(SnapshotVerbHandler.class);

    public void doVerb(MessageIn<SnapshotCommand> message, int id)
    {
        SnapshotCommand command = message.payload;
        if (command.clear_snapshot)
        {
            Keyspace.clearSnapshot(command.snapshot_name, command.keyspace);
        }
        else
            Keyspace.open(command.keyspace).getColumnFamilyStore(command.column_family).snapshot(command.snapshot_name);
        logger.debug("Enqueuing response to snapshot request {} to {}", command.snapshot_name, message.from);
        MessagingService.instance().sendReply(new MessageOut(MessagingService.Verb.INTERNAL_RESPONSE), id, message.from);
    }
}
