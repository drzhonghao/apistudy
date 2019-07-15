import org.apache.cassandra.db.*;


import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;

import org.apache.cassandra.batchlog.LegacyBatchlogMigrator;
import org.apache.cassandra.exceptions.WriteTimeoutException;
import org.apache.cassandra.io.util.FastByteArrayInputStream;
import org.apache.cassandra.net.*;
import org.apache.cassandra.tracing.Tracing;

public class MutationVerbHandler implements IVerbHandler<Mutation>
{
    private void reply(int id, InetAddress replyTo)
    {
        Tracing.trace("Enqueuing response to {}", replyTo);
        MessagingService.instance().sendReply(WriteResponse.createMessage(), id, replyTo);
    }

    private void failed()
    {
        Tracing.trace("Payload application resulted in WriteTimeout, not replying");
    }

    public void doVerb(MessageIn<Mutation> message, int id)  throws IOException
    {
        // Check if there were any forwarding headers in this message
        byte[] from = message.parameters.get(Mutation.FORWARD_FROM);
        InetAddress replyTo;
        if (from == null)
        {
            replyTo = message.from;
            byte[] forwardBytes = message.parameters.get(Mutation.FORWARD_TO);
            if (forwardBytes != null)
                forwardToLocalNodes(message.payload, message.verb, forwardBytes, message.from);
        }
        else
        {
            replyTo = InetAddress.getByAddress(from);
        }

        try
        {
            if (message.version < MessagingService.VERSION_30 && LegacyBatchlogMigrator.isLegacyBatchlogMutation(message.payload))
            {
                LegacyBatchlogMigrator.handleLegacyMutation(message.payload);
                reply(id, replyTo);
            }
            else
                message.payload.applyFuture().thenAccept(o -> reply(id, replyTo)).exceptionally(wto -> {
                    failed();
                    return null;
                });
        }
        catch (WriteTimeoutException wto)
        {
            failed();
        }
    }

    /**
     * Older version (< 1.0) will not send this message at all, hence we don't
     * need to check the version of the data.
     */
    private static void forwardToLocalNodes(Mutation mutation, MessagingService.Verb verb, byte[] forwardBytes, InetAddress from) throws IOException
    {
        try (DataInputStream in = new DataInputStream(new FastByteArrayInputStream(forwardBytes)))
        {
            int size = in.readInt();

            // tell the recipients who to send their ack to
            MessageOut<Mutation> message = new MessageOut<>(verb, mutation, Mutation.serializer).withParameter(Mutation.FORWARD_FROM, from.getAddress());
            // Send a message to each of the addresses on our Forward List
            for (int i = 0; i < size; i++)
            {
                InetAddress address = CompactEndpointSerializationHelper.deserialize(in);
                int id = in.readInt();
                Tracing.trace("Enqueuing forwarded write to {}", address);
                MessagingService.instance().sendOneWay(message, id, address);
            }
        }
    }
}
