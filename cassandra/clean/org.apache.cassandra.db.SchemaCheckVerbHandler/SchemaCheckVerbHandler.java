import org.apache.cassandra.db.*;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.config.Schema;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.utils.UUIDSerializer;

public class SchemaCheckVerbHandler implements IVerbHandler
{
    private final Logger logger = LoggerFactory.getLogger(SchemaCheckVerbHandler.class);

    public void doVerb(MessageIn message, int id)
    {
        logger.trace("Received schema check request.");

        /*
        3.11 is special here: We return the 3.0 compatible version, if the requesting node
        is running 3.0. Otherwise the "real" schema version.
        */
        MessageOut<UUID> response = new MessageOut<>(MessagingService.Verb.INTERNAL_RESPONSE,
                                                     Schema.instance.getVersion(),
                                                     UUIDSerializer.serializer);
        MessagingService.instance().sendReply(response, id, message.from);
    }
}
