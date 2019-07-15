import org.apache.cassandra.db.ReadCommand;
import org.apache.cassandra.db.ReadResponse;
import org.apache.cassandra.db.ReadExecutionController;
import org.apache.cassandra.db.*;


import org.apache.cassandra.db.partitions.UnfilteredPartitionIterator;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.tracing.Tracing;

public class ReadCommandVerbHandler implements IVerbHandler<ReadCommand>
{
    protected IVersionedSerializer<ReadResponse> serializer()
    {
        return ReadResponse.serializer;
    }

    public void doVerb(MessageIn<ReadCommand> message, int id)
    {
        if (StorageService.instance.isBootstrapMode())
        {
            throw new RuntimeException("Cannot service reads while bootstrapping!");
        }

        ReadCommand command = message.payload;
        command.setMonitoringTime(message.constructionTime, message.isCrossNode(), message.getTimeout(), message.getSlowQueryTimeout());

        ReadResponse response;
        try (ReadExecutionController executionController = command.executionController();
             UnfilteredPartitionIterator iterator = command.executeLocally(executionController))
        {
            response = command.createResponse(iterator);
        }

        if (!command.complete())
        {
            Tracing.trace("Discarding partial response to {} (timed out)", message.from);
            MessagingService.instance().incrementDroppedMessages(message, message.getLifetimeInMS());
            return;
        }

        Tracing.trace("Enqueuing response to {}", message.from);
        MessageOut<ReadResponse> reply = new MessageOut<>(MessagingService.Verb.REQUEST_RESPONSE, response, serializer());
        MessagingService.instance().sendReply(reply, id, message.from);
    }
}
