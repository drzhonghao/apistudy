import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.auth.DataResource;
import org.apache.cassandra.auth.AuthenticatedUser;
import org.apache.cassandra.auth.FunctionResource;
import org.apache.cassandra.cql3.statements.*;


import java.util.regex.Pattern;

import org.apache.cassandra.auth.*;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.SchemaConstants;
import org.apache.cassandra.exceptions.*;
import org.apache.cassandra.locator.LocalStrategy;
import org.apache.cassandra.schema.KeyspaceMetadata;
import org.apache.cassandra.schema.KeyspaceParams;
import org.apache.cassandra.service.*;
import org.apache.cassandra.thrift.ThriftValidation;
import org.apache.cassandra.transport.Event;

/** A <code>CREATE KEYSPACE</code> statement parsed from a CQL query. */
public class CreateKeyspaceStatement extends SchemaAlteringStatement
{
    private static final Pattern PATTERN_WORD_CHARS = Pattern.compile("\\w+");

    private final String name;
    private final KeyspaceAttributes attrs;
    private final boolean ifNotExists;

    /**
     * Creates a new <code>CreateKeyspaceStatement</code> instance for a given
     * keyspace name and keyword arguments.
     *
     * @param name the name of the keyspace to create
     * @param attrs map of the raw keyword arguments that followed the <code>WITH</code> keyword.
     */
    public CreateKeyspaceStatement(String name, KeyspaceAttributes attrs, boolean ifNotExists)
    {
        super();
        this.name = name;
        this.attrs = attrs;
        this.ifNotExists = ifNotExists;
    }

    @Override
    public String keyspace()
    {
        return name;
    }

    public void checkAccess(ClientState state) throws UnauthorizedException
    {
        state.hasAllKeyspacesAccess(Permission.CREATE);
    }

    /**
     * The <code>CqlParser</code> only goes as far as extracting the keyword arguments
     * from these statements, so this method is responsible for processing and
     * validating.
     *
     * @throws InvalidRequestException if arguments are missing or unacceptable
     */
    public void validate(ClientState state) throws RequestValidationException
    {
        ThriftValidation.validateKeyspaceNotSystem(name);

        // keyspace name
        if (!PATTERN_WORD_CHARS.matcher(name).matches())
            throw new InvalidRequestException(String.format("\"%s\" is not a valid keyspace name", name));
        if (name.length() > SchemaConstants.NAME_LENGTH)
            throw new InvalidRequestException(String.format("Keyspace names shouldn't be more than %s characters long (got \"%s\")", SchemaConstants.NAME_LENGTH, name));

        attrs.validate();

        if (attrs.getReplicationStrategyClass() == null)
            throw new ConfigurationException("Missing mandatory replication strategy class");

        // The strategy is validated through KSMetaData.validate() in announceNewKeyspace below.
        // However, for backward compatibility with thrift, this doesn't validate unexpected options yet,
        // so doing proper validation here.
        KeyspaceParams params = attrs.asNewKeyspaceParams();
        params.validate(name);
        if (params.replication.klass.equals(LocalStrategy.class))
            throw new ConfigurationException("Unable to use given strategy class: LocalStrategy is reserved for internal use.");
    }

    public Event.SchemaChange announceMigration(QueryState queryState, boolean isLocalOnly) throws RequestValidationException
    {
        KeyspaceMetadata ksm = KeyspaceMetadata.create(name, attrs.asNewKeyspaceParams());
        try
        {
            MigrationManager.announceNewKeyspace(ksm, isLocalOnly);
            return new Event.SchemaChange(Event.SchemaChange.Change.CREATED, keyspace());
        }
        catch (AlreadyExistsException e)
        {
            if (ifNotExists)
                return null;
            throw e;
        }
    }

    protected void grantPermissionsToCreator(QueryState state)
    {
        try
        {
            RoleResource role = RoleResource.role(state.getClientState().getUser().getName());
            DataResource keyspace = DataResource.keyspace(keyspace());
            DatabaseDescriptor.getAuthorizer().grant(AuthenticatedUser.SYSTEM_USER,
                                                     keyspace.applicablePermissions(),
                                                     keyspace,
                                                     role);
            FunctionResource functions = FunctionResource.keyspace(keyspace());
            DatabaseDescriptor.getAuthorizer().grant(AuthenticatedUser.SYSTEM_USER,
                                                     functions.applicablePermissions(),
                                                     functions,
                                                     role);
        }
        catch (RequestExecutionException e)
        {
            throw new RuntimeException(e);
        }
    }
}
