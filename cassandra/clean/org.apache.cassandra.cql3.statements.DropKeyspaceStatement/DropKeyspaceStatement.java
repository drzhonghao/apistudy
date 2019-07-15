import org.apache.cassandra.cql3.statements.*;


import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.exceptions.UnauthorizedException;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.thrift.ThriftValidation;
import org.apache.cassandra.transport.Event;

public class DropKeyspaceStatement extends SchemaAlteringStatement
{
    private final String keyspace;
    private final boolean ifExists;

    public DropKeyspaceStatement(String keyspace, boolean ifExists)
    {
        super();
        this.keyspace = keyspace;
        this.ifExists = ifExists;
    }

    public void checkAccess(ClientState state) throws UnauthorizedException, InvalidRequestException
    {
        state.hasKeyspaceAccess(keyspace, Permission.DROP);
    }

    public void validate(ClientState state) throws RequestValidationException
    {
        ThriftValidation.validateKeyspaceNotSystem(keyspace);
    }

    @Override
    public String keyspace()
    {
        return keyspace;
    }

    public Event.SchemaChange announceMigration(QueryState queryState, boolean isLocalOnly) throws ConfigurationException
    {
        try
        {
            MigrationManager.announceKeyspaceDrop(keyspace, isLocalOnly);
            return new Event.SchemaChange(Event.SchemaChange.Change.DROPPED, keyspace());
        }
        catch(ConfigurationException e)
        {
            if (ifExists)
                return null;
            throw e;
        }
    }
}
