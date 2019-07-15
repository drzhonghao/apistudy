import org.apache.cassandra.cql3.statements.*;



import org.apache.cassandra.auth.DataResource;
import org.apache.cassandra.auth.IResource;
import org.apache.cassandra.cql3.CQLStatement;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.transport.messages.ResultMessage;

public abstract class AuthorizationStatement extends ParsedStatement implements CQLStatement
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
    throws RequestValidationException, RequestExecutionException
    {
        return execute(state.getClientState());
    }

    public abstract ResultMessage execute(ClientState state) throws RequestValidationException, RequestExecutionException;

    public ResultMessage executeInternal(QueryState state, QueryOptions options)
    {
        // executeInternal is for local query only, thus altering permission doesn't make sense and is not supported
        throw new UnsupportedOperationException();
    }

    public static IResource maybeCorrectResource(IResource resource, ClientState state) throws InvalidRequestException
    {
        if (DataResource.class.isInstance(resource))
        {
            DataResource dataResource = (DataResource) resource;
            if (dataResource.isTableLevel() && dataResource.getKeyspace() == null)
                return DataResource.table(state.getKeyspace(), dataResource.getTable());
        }
        return resource;
    }
}
