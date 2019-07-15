import org.apache.cassandra.cql3.statements.*;


import java.util.concurrent.TimeoutException;

import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.cql3.*;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.exceptions.*;
import org.apache.cassandra.transport.messages.ResultMessage;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.service.QueryState;
import org.apache.cassandra.service.StorageProxy;
import org.apache.cassandra.thrift.ThriftValidation;

public class TruncateStatement extends CFStatement implements CQLStatement
{
    public TruncateStatement(CFName name)
    {
        super(name);
    }

    public int getBoundTerms()
    {
        return 0;
    }

    public Prepared prepare(ClientState clientState) throws InvalidRequestException
    {
        return new Prepared(this);
    }

    public void checkAccess(ClientState state) throws InvalidRequestException, UnauthorizedException
    {
        state.hasColumnFamilyAccess(keyspace(), columnFamily(), Permission.MODIFY);
    }

    public void validate(ClientState state) throws InvalidRequestException
    {
        ThriftValidation.validateColumnFamily(keyspace(), columnFamily());
    }

    public ResultMessage execute(QueryState state, QueryOptions options, long queryStartNanoTime) throws InvalidRequestException, TruncateException
    {
        try
        {
            CFMetaData metaData = Schema.instance.getCFMetaData(keyspace(), columnFamily());
            if (metaData.isView())
                throw new InvalidRequestException("Cannot TRUNCATE materialized view directly; must truncate base table instead");

            StorageProxy.truncateBlocking(keyspace(), columnFamily());
        }
        catch (UnavailableException | TimeoutException e)
        {
            throw new TruncateException(e);
        }
        return null;
    }

    public ResultMessage executeInternal(QueryState state, QueryOptions options)
    {
        try
        {
            ColumnFamilyStore cfs = Keyspace.open(keyspace()).getColumnFamilyStore(columnFamily());
            cfs.truncateBlocking();
        }
        catch (Exception e)
        {
            throw new TruncateException(e);
        }
        return null;
    }
}
