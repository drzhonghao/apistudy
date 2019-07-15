import org.apache.cassandra.db.compaction.*;



import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.io.sstable.format.SSTableReader;

public class TimeWindowCompactionController extends CompactionController
{
    private static final Logger logger = LoggerFactory.getLogger(TimeWindowCompactionController.class);

    private final boolean ignoreOverlaps;

    public TimeWindowCompactionController(ColumnFamilyStore cfs, Set<SSTableReader> compacting, int gcBefore, boolean ignoreOverlaps)
    {
        super(cfs, compacting, gcBefore);
        this.ignoreOverlaps = ignoreOverlaps;
        if (ignoreOverlaps)
            logger.warn("You are running with sstables overlapping checks disabled, it can result in loss of data");
    }

    @Override
    protected boolean ignoreOverlaps()
    {
        return ignoreOverlaps;
    }
}
