import org.apache.cassandra.cql3.statements.*;


import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.exceptions.*;
import org.apache.cassandra.locator.LocalStrategy;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.MigrationManager;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.transport.Event;

public class AlterKeyspaceStatement extends SchemaAlteringStatement
{
    private final String name;
    private final KeyspaceAttributes attrs;

    public AlterKeyspaceStatement(String name, KeyspaceAttributes attrs)
    {
        super();
        this.name = name;
        this.attrs = attrs;
    }

    @Override
    public String keyspace()
    {
        return name;
    }

    public void checkAccess(ClientState state) throws UnauthorizedException, InvalidRequestException
    {
        state.hasKeyspaceAccess(name, Permission.ALTER);
    }

    public void validate(ClientState state) throws RequestValidationException
    {
        KeyspaceMetadata ksm = Schema.instance.getKSMetaData(name);
        if (ksm == null)
            throw new InvalidRequestException("Unknown keyspace " + name);
        if (SchemaConstants.isLocalSystemKeyspace(ksm.name))
            throw new InvalidRequestException("Cannot alter system keyspace");

        attrs.validate();

        if (attrs.getReplicationStrategyClass() == null && !attrs.getReplicationOptions().isEmpty())
            throw new ConfigurationException("Missing replication strategy class");

        if (attrs.getReplicationStrategyClass() != null)
        {
            // The strategy is validated through KSMetaData.validate() in announceKeyspaceUpdate below.
            // However, for backward compatibility with thrift, this doesn't validate unexpected options yet,
            // so doing proper validation here.
            KeyspaceParams params = attrs.asAlteredKeyspaceParams(ksm.params);
            params.validate(name);
            if (params.replication.klass.equals(LocalStrategy.class))
                throw new ConfigurationException("Unable to use given strategy class: LocalStrategy is reserved for internal use.");
        }
    }

    public Event.SchemaChange announceMigration(QueryState queryState, boolean isLocalOnly) throws RequestValidationException
    {
        KeyspaceMetadata oldKsm = Schema.instance.getKSMetaData(name);
        // In the (very) unlikely case the keyspace was dropped since validate()
        if (oldKsm == null)
            throw new InvalidRequestException("Unknown keyspace " + name);

        KeyspaceMetadata newKsm = oldKsm.withSwapped(attrs.asAlteredKeyspaceParams(oldKsm.params));
        MigrationManager.announceKeyspaceUpdate(newKsm, isLocalOnly);
        return new Event.SchemaChange(Event.SchemaChange.Change.UPDATED, keyspace());
    }
}
