import org.apache.cassandra.cql3.statements.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.cql3.CFName;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.exceptions.UnauthorizedException;
import org.apache.cassandra.schema.TriggerMetadata;
import org.apache.cassandra.schema.Triggers;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.thrift.ThriftValidation;
import org.apache.cassandra.transport.Event;
import org.apache.cassandra.triggers.TriggerExecutor;

public class CreateTriggerStatement extends SchemaAlteringStatement
{
    private static final Logger logger = LoggerFactory.getLogger(CreateTriggerStatement.class);

    private final String triggerName;
    private final String triggerClass;
    private final boolean ifNotExists;

    public CreateTriggerStatement(CFName name, String triggerName, String clazz, boolean ifNotExists)
    {
        super(name);
        this.triggerName = triggerName;
        this.triggerClass = clazz;
        this.ifNotExists = ifNotExists;
    }

    public void checkAccess(ClientState state) throws UnauthorizedException
    {
        state.ensureIsSuper("Only superusers are allowed to perform CREATE TRIGGER queries");
    }

    public void validate(ClientState state) throws RequestValidationException
    {
        CFMetaData cfm = ThriftValidation.validateColumnFamily(keyspace(), columnFamily());
        if (cfm.isView())
            throw new InvalidRequestException("Cannot CREATE TRIGGER against a materialized view");

        try
        {
            TriggerExecutor.instance.loadTriggerInstance(triggerClass);
        }
        catch (Exception e)
        {
            throw new ConfigurationException(String.format("Trigger class '%s' doesn't exist", triggerClass));
        }
    }

    public Event.SchemaChange announceMigration(QueryState queryState, boolean isLocalOnly) throws ConfigurationException, InvalidRequestException
    {
        CFMetaData cfm = Schema.instance.getCFMetaData(keyspace(), columnFamily()).copy();
        Triggers triggers = cfm.getTriggers();

        if (triggers.get(triggerName).isPresent())
        {
            if (ifNotExists)
                return null;
            else
                throw new InvalidRequestException(String.format("Trigger %s already exists", triggerName));
        }

        cfm.triggers(triggers.with(TriggerMetadata.create(triggerName, triggerClass)));
        logger.info("Adding trigger with name {} and class {}", triggerName, triggerClass);
        MigrationManager.announceColumnFamilyUpdate(cfm, isLocalOnly);
        return new Event.SchemaChange(Event.SchemaChange.Change.UPDATED, Event.SchemaChange.Target.TABLE, keyspace(), columnFamily());
    }
}
