import org.apache.cassandra.db.compaction.*;


import java.util.Set;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.compaction.CompactionManager.CompactionExecutorStatsCollector;
import org.apache.cassandra.db.compaction.writers.CompactionAwareWriter;
import org.apache.cassandra.io.FSDiskFullWriteError;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.utils.WrappedRunnable;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;

public abstract class AbstractCompactionTask extends WrappedRunnable
{
    protected final ColumnFamilyStore cfs;
    protected LifecycleTransaction transaction;
    protected boolean isUserDefined;
    protected OperationType compactionType;

    /**
     * @param cfs
     * @param transaction the modifying managing the status of the sstables we're replacing
     */
    public AbstractCompactionTask(ColumnFamilyStore cfs, LifecycleTransaction transaction)
    {
        this.cfs = cfs;
        this.transaction = transaction;
        this.isUserDefined = false;
        this.compactionType = OperationType.COMPACTION;
        // enforce contract that caller should mark sstables compacting
        Set<SSTableReader> compacting = transaction.tracker.getCompacting();
        for (SSTableReader sstable : transaction.originals())
            assert compacting.contains(sstable) : sstable.getFilename() + " is not correctly marked compacting";
    }

    /**
     * executes the task and unmarks sstables compacting
     */
    public int execute(CompactionExecutorStatsCollector collector)
    {
        try
        {
            return executeInternal(collector);
        }
        catch(FSDiskFullWriteError e)
        {
            RuntimeException cause = new RuntimeException("Converted from FSDiskFullWriteError: " + e.getMessage());
            cause.setStackTrace(e.getStackTrace());
            throw new RuntimeException("Throwing new Runtime to bypass exception handler when disk is full", cause);
        }
        finally
        {
            transaction.close();
        }
    }
    public abstract CompactionAwareWriter getCompactionAwareWriter(ColumnFamilyStore cfs, Directories directories, LifecycleTransaction txn, Set<SSTableReader> nonExpiredSSTables);

    protected abstract int executeInternal(CompactionExecutorStatsCollector collector);

    public AbstractCompactionTask setUserDefined(boolean isUserDefined)
    {
        this.isUserDefined = isUserDefined;
        return this;
    }

    public AbstractCompactionTask setCompactionType(OperationType compactionType)
    {
        this.compactionType = compactionType;
        return this;
    }

    public String toString()
    {
        return "CompactionTask(" + transaction + ")";
    }
}
