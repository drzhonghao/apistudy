import org.apache.cassandra.service.*;


import java.net.InetAddress;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.SystemKeyspace.BootstrapState;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.gms.FailureDetector;
import org.apache.cassandra.net.IAsyncCallback;
import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.schema.SchemaKeyspace;
import org.apache.cassandra.utils.WrappedRunnable;


class MigrationTask extends WrappedRunnable
{
    private static final Logger logger = LoggerFactory.getLogger(MigrationTask.class);

    private static final ConcurrentLinkedQueue<CountDownLatch> inflightTasks = new ConcurrentLinkedQueue<>();

    private static final Set<BootstrapState> monitoringBootstrapStates = EnumSet.of(BootstrapState.NEEDS_BOOTSTRAP, BootstrapState.IN_PROGRESS);

    private final InetAddress endpoint;

    MigrationTask(InetAddress endpoint)
    {
        this.endpoint = endpoint;
    }

    public static ConcurrentLinkedQueue<CountDownLatch> getInflightTasks()
    {
        return inflightTasks;
    }

    public void runMayThrow() throws Exception
    {
        if (!FailureDetector.instance.isAlive(endpoint))
        {
            logger.warn("Can't send schema pull request: node {} is down.", endpoint);
            return;
        }

        // There is a chance that quite some time could have passed between now and the MM#maybeScheduleSchemaPull(),
        // potentially enough for the endpoint node to restart - which is an issue if it does restart upgraded, with
        // a higher major.
        if (!MigrationManager.shouldPullSchemaFrom(endpoint))
        {
            logger.info("Skipped sending a migration request: node {} has a higher major version now.", endpoint);
            return;
        }

        MessageOut message = new MessageOut<>(MessagingService.Verb.MIGRATION_REQUEST, null, MigrationManager.MigrationsSerializer.instance);

        final CountDownLatch completionLatch = new CountDownLatch(1);

        IAsyncCallback<Collection<Mutation>> cb = new IAsyncCallback<Collection<Mutation>>()
        {
            @Override
            public void response(MessageIn<Collection<Mutation>> message)
            {
                try
                {
                    SchemaKeyspace.mergeSchemaAndAnnounceVersion(message.payload);
                }
                catch (ConfigurationException e)
                {
                    logger.error("Configuration exception merging remote schema", e);
                }
                finally
                {
                    completionLatch.countDown();
                }
            }

            public boolean isLatencyForSnitch()
            {
                return false;
            }
        };

        // Only save the latches if we need bootstrap or are bootstrapping
        if (monitoringBootstrapStates.contains(SystemKeyspace.getBootstrapState()))
            inflightTasks.offer(completionLatch);

        MessagingService.instance().sendRR(message, endpoint, cb);
    }
}
