import org.apache.cassandra.db.*;


import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.schema.SchemaKeyspace;
import org.apache.cassandra.utils.WrappedRunnable;

/**
 * Called when node receives updated schema state from the schema migration coordinator node.
 * Such happens when user makes local schema migration on one of the nodes in the ring
 * (which is going to act as coordinator) and that node sends (pushes) it's updated schema state
 * (in form of mutations) to all the alive nodes in the cluster.
 */
public class DefinitionsUpdateVerbHandler implements IVerbHandler<Collection<Mutation>>
{
    private static final Logger logger = LoggerFactory.getLogger(DefinitionsUpdateVerbHandler.class);

    public void doVerb(final MessageIn<Collection<Mutation>> message, int id)
    {
        logger.trace("Received schema mutation push from {}", message.from);

        StageManager.getStage(Stage.MIGRATION).submit(new WrappedRunnable()
        {
            public void runMayThrow() throws ConfigurationException
            {
                SchemaKeyspace.mergeSchemaAndAnnounceVersion(message.payload);
            }
        });
    }
}
