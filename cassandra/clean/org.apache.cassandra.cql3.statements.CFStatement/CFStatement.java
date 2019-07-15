import org.apache.cassandra.cql3.statements.*;


import org.apache.cassandra.cql3.CFName;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.exceptions.InvalidRequestException;

/**
 * Abstract class for statements that apply on a given column family.
 */
public abstract class CFStatement extends ParsedStatement
{
    protected final CFName cfName;

    protected CFStatement(CFName cfName)
    {
        this.cfName = cfName;
    }

    public void prepareKeyspace(ClientState state) throws InvalidRequestException
    {
        if (!cfName.hasKeyspace())
        {
            // XXX: We explicitely only want to call state.getKeyspace() in this case, as we don't want to throw
            // if not logged in any keyspace but a keyspace is explicitely set on the statement. So don't move
            // the call outside the 'if' or replace the method by 'prepareKeyspace(state.getKeyspace())'
            cfName.setKeyspace(state.getKeyspace(), true);
        }
    }

    // Only for internal calls, use the version with ClientState for user queries
    public void prepareKeyspace(String keyspace)
    {
        if (!cfName.hasKeyspace())
            cfName.setKeyspace(keyspace, true);
    }

    public String keyspace()
    {
        assert cfName.hasKeyspace() : "The statement hasn't be prepared correctly";
        return cfName.getKeyspace();
    }

    public String columnFamily()
    {
        return cfName.getColumnFamily();
    }
}
