import org.apache.cassandra.db.*;


import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.SchemaKeyspace;
import org.apache.cassandra.service.MigrationManager;

/**
 * Sends it's current schema state in form of mutations in reply to the remote node's request.
 * Such a request is made when one of the nodes, by means of Gossip, detects schema disagreement in the ring.
 */
public class MigrationRequestVerbHandler implements IVerbHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MigrationRequestVerbHandler.class);

    public void doVerb(MessageIn message, int id)
    {
        logger.trace("Received migration request from {}.", message.from);
        MessageOut<Collection<Mutation>> response = new MessageOut<>(MessagingService.Verb.INTERNAL_RESPONSE,
                                                                     SchemaKeyspace.convertSchemaToMutations(),
                                                                     MigrationManager.MigrationsSerializer.instance);
        MessagingService.instance().sendReply(response, id, message.from);
    }
}
