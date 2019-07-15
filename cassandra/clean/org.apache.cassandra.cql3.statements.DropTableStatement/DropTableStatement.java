import org.apache.cassandra.cql3.statements.*;


import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ViewDefinition;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.cql3.CFName;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.UnauthorizedException;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.transport.Event;

public class DropTableStatement extends SchemaAlteringStatement
{
    private final boolean ifExists;

    public DropTableStatement(CFName name, boolean ifExists)
    {
        super(name);
        this.ifExists = ifExists;
    }

    public void checkAccess(ClientState state) throws UnauthorizedException, InvalidRequestException
    {
        try
        {
            state.hasColumnFamilyAccess(keyspace(), columnFamily(), Permission.DROP);
        }
        catch (InvalidRequestException e)
        {
            if (!ifExists)
                throw e;
        }
    }

    public void validate(ClientState state)
    {
        // validated in announceMigration()
    }

    public Event.SchemaChange announceMigration(QueryState queryState, boolean isLocalOnly) throws ConfigurationException
    {
        try
        {
            KeyspaceMetadata ksm = Schema.instance.getKSMetaData(keyspace());
            if (ksm == null)
                throw new ConfigurationException(String.format("Cannot drop table in unknown keyspace '%s'", keyspace()));
            CFMetaData cfm = ksm.getTableOrViewNullable(columnFamily());
            if (cfm != null)
            {
                if (cfm.isView())
                    throw new InvalidRequestException("Cannot use DROP TABLE on Materialized View");

                boolean rejectDrop = false;
                StringBuilder messageBuilder = new StringBuilder();
                for (ViewDefinition def : ksm.views)
                {
                    if (def.baseTableId.equals(cfm.cfId))
                    {
                        if (rejectDrop)
                            messageBuilder.append(',');
                        rejectDrop = true;
                        messageBuilder.append(def.viewName);
                    }
                }
                if (rejectDrop)
                {
                    throw new InvalidRequestException(String.format("Cannot drop table when materialized views still depend on it (%s.{%s})",
                                                                    keyspace(),
                                                                    messageBuilder.toString()));
                }
            }
            MigrationManager.announceColumnFamilyDrop(keyspace(), columnFamily(), isLocalOnly);
            return new Event.SchemaChange(Event.SchemaChange.Change.DROPPED, Event.SchemaChange.Target.TABLE, keyspace(), columnFamily());
        }
        catch (ConfigurationException e)
        {
            if (ifExists)
                return null;
            throw e;
        }
    }
}
