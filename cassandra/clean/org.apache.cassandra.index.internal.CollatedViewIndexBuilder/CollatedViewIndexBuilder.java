import org.apache.cassandra.index.internal.*;


import java.util.Set;
import java.util.UUID;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.compaction.CompactionInfo;
import org.apache.cassandra.db.compaction.CompactionInterruptedException;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.index.SecondaryIndexBuilder;
import org.apache.cassandra.io.sstable.ReducingKeyIterator;
import org.apache.cassandra.utils.UUIDGen;

/**
 * Manages building an entire index from column family data. Runs on to compaction manager.
 */
public class CollatedViewIndexBuilder extends SecondaryIndexBuilder
{
    private final ColumnFamilyStore cfs;
    private final Set<Index> indexers;
    private final ReducingKeyIterator iter;
    private final UUID compactionId;

    public CollatedViewIndexBuilder(ColumnFamilyStore cfs, Set<Index> indexers, ReducingKeyIterator iter)
    {
        this.cfs = cfs;
        this.indexers = indexers;
        this.iter = iter;
        this.compactionId = UUIDGen.getTimeUUID();
    }

    public CompactionInfo getCompactionInfo()
    {
        return new CompactionInfo(cfs.metadata,
                OperationType.INDEX_BUILD,
                iter.getBytesRead(),
                iter.getTotalBytes(),
                compactionId);
    }

    public void build()
    {
        try
        {
            int pageSize = cfs.indexManager.calculateIndexingPageSize();
            while (iter.hasNext())
            {
                if (isStopRequested())
                    throw new CompactionInterruptedException(getCompactionInfo());
                DecoratedKey key = iter.next();
                cfs.indexManager.indexPartition(key, indexers, pageSize);
            }
        }
        finally
        {
            iter.close();
        }
    }
}
