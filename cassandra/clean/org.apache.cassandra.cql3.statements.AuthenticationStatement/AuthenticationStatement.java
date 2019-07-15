import org.apache.cassandra.cql3.statements.*;


import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.auth.RoleResource;
import org.apache.cassandra.cql3.CQLStatement;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.exceptions.UnauthorizedException;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.transport.messages.ResultMessage;

public abstract class AuthenticationStatement extends ParsedStatement implements CQLStatement
{
    @Override
    public Prepared prepare(ClientState clientState)
    {
        return new Prepared(this);
    }

    public int getBoundTerms()
    {
        return 0;
    }

    public ResultMessage execute(QueryState state, QueryOptions options, long queryStartNanoTime)
    throws RequestExecutionException, RequestValidationException
    {
        return execute(state.getClientState());
    }

    public abstract ResultMessage execute(ClientState state) throws RequestExecutionException, RequestValidationException;

    public ResultMessage executeInternal(QueryState state, QueryOptions options)
    {
        // executeInternal is for local query only, thus altering users doesn't make sense and is not supported
        throw new UnsupportedOperationException();
    }

    public void checkPermission(ClientState state, Permission required, RoleResource resource) throws UnauthorizedException
    {
        try
        {
            state.ensureHasPermission(required, resource);
        }
        catch (UnauthorizedException e)
        {
            // Catch and rethrow with a more friendly message
            throw new UnauthorizedException(String.format("User %s does not have sufficient privileges " +
                                                          "to perform the requested operation",
                                                          state.getUser().getName()));
        }
    }
}

