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
import org.apache.cassandra.schema.Triggers;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.thrift.ThriftValidation;
import org.apache.cassandra.transport.Event;

public class DropTriggerStatement extends SchemaAlteringStatement
{
    private static final Logger logger = LoggerFactory.getLogger(DropTriggerStatement.class);

    private final String triggerName;

    private final boolean ifExists;

    public DropTriggerStatement(CFName name, String triggerName, boolean ifExists)
    {
        super(name);
        this.triggerName = triggerName;
        this.ifExists = ifExists;
    }

    public void checkAccess(ClientState state) throws UnauthorizedException
    {
        state.ensureIsSuper("Only superusers are allowed to perfrom DROP TRIGGER queries");
    }

    public void validate(ClientState state) throws RequestValidationException
    {
        ThriftValidation.validateColumnFamily(keyspace(), columnFamily());
    }

    public Event.SchemaChange announceMigration(QueryState queryState, boolean isLocalOnly) throws ConfigurationException, InvalidRequestException
    {
        CFMetaData cfm = Schema.instance.getCFMetaData(keyspace(), columnFamily()).copy();
        Triggers triggers = cfm.getTriggers();

        if (!triggers.get(triggerName).isPresent())
        {
            if (ifExists)
                return null;
            else
                throw new InvalidRequestException(String.format("Trigger %s was not found", triggerName));
        }

        logger.info("Dropping trigger with name {}", triggerName);
        cfm.triggers(triggers.without(triggerName));
        MigrationManager.announceColumnFamilyUpdate(cfm, isLocalOnly);
        return new Event.SchemaChange(Event.SchemaChange.Change.UPDATED, Event.SchemaChange.Target.TABLE, keyspace(), columnFamily());
    }
}
