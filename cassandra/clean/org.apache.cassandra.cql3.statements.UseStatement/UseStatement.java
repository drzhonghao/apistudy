import org.apache.cassandra.cql3.statements.*;


import org.apache.cassandra.cql3.CQLStatement;
import org.apache.cassandra.cql3.QueryOptions;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.exceptions.UnauthorizedException;
import org.apache.cassandra.transport.messages.ResultMessage;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.QueryState;

public class UseStatement extends ParsedStatement implements CQLStatement
{
    private final String keyspace;

    public UseStatement(String keyspace)
    {
        this.keyspace = keyspace;
    }

    public int getBoundTerms()
    {
        return 0;
    }

    public Prepared prepare(ClientState clientState) throws InvalidRequestException
    {
        return new Prepared(this);
    }

    public void checkAccess(ClientState state) throws UnauthorizedException
    {
        state.validateLogin();
    }

    public void validate(ClientState state) throws InvalidRequestException
    {
    }

    public ResultMessage execute(QueryState state, QueryOptions options, long queryStartNanoTime) throws InvalidRequestException
    {
        state.getClientState().setKeyspace(keyspace);
        return new ResultMessage.SetKeyspace(keyspace);
    }

    public ResultMessage executeInternal(QueryState state, QueryOptions options) throws InvalidRequestException
    {
        // In production, internal queries are exclusively on the system keyspace and 'use' is thus useless
        // but for some unit tests we need to set the keyspace (e.g. for tests with DROP INDEX)
        return execute(state, options, System.nanoTime());
    }
}
