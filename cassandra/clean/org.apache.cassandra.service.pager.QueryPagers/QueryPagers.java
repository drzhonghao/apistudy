import org.apache.cassandra.service.pager.*;


import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.filter.*;
import org.apache.cassandra.db.partitions.*;
import org.apache.cassandra.exceptions.RequestExecutionException;
import org.apache.cassandra.exceptions.RequestValidationException;
import org.apache.cassandra.service.ClientState;
import org.apache.cassandra.transport.ProtocolVersion;

/**
 * Static utility methods for paging.
 */
public class QueryPagers
{
    private QueryPagers() {};

    /**
     * Convenience method that count (live) cells/rows for a given slice of a row, but page underneath.
     */
    public static int countPaged(CFMetaData metadata,
                                 DecoratedKey key,
                                 ColumnFilter columnFilter,
                                 ClusteringIndexFilter filter,
                                 DataLimits limits,
                                 ConsistencyLevel consistencyLevel,
                                 ClientState state,
                                 final int pageSize,
                                 int nowInSec,
                                 boolean isForThrift,
                                 long queryStartNanoTime) throws RequestValidationException, RequestExecutionException
    {
        SinglePartitionReadCommand command = SinglePartitionReadCommand.create(isForThrift, metadata, nowInSec, columnFilter, RowFilter.NONE, limits, key, filter);
        final SinglePartitionPager pager = new SinglePartitionPager(command, null, ProtocolVersion.CURRENT);

        int count = 0;
        while (!pager.isExhausted())
        {
            try (PartitionIterator iter = pager.fetchPage(pageSize, consistencyLevel, state, queryStartNanoTime))
            {
                DataLimits.Counter counter = limits.newCounter(nowInSec, true, command.selectsFullPartition(), metadata.enforceStrictLiveness());
                PartitionIterators.consume(counter.applyTo(iter));
                count += counter.counted();
            }
        }
        return count;
    }
}
