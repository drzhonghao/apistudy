import org.apache.cassandra.db.compaction.CompactionManager;
import org.apache.cassandra.db.compaction.CompactionTask;
import org.apache.cassandra.db.compaction.*;


import java.util.*;
import java.util.function.Predicate;

import org.apache.cassandra.db.*;
import org.apache.cassandra.db.compaction.writers.CompactionAwareWriter;
import org.apache.cassandra.db.compaction.writers.MaxSSTableSizeWriter;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;

public class SSTableSplitter 
{
    private final SplittingCompactionTask task;

    private CompactionInfo.Holder info;

    public SSTableSplitter(ColumnFamilyStore cfs, LifecycleTransaction transaction, int sstableSizeInMB)
    {
        this.task = new SplittingCompactionTask(cfs, transaction, sstableSizeInMB);
    }

    public void split()
    {
        task.execute(new StatsCollector());
    }

    public class StatsCollector implements CompactionManager.CompactionExecutorStatsCollector
    {
        public void beginCompaction(CompactionInfo.Holder ci)
        {
            SSTableSplitter.this.info = ci;
        }

        public void finishCompaction(CompactionInfo.Holder ci)
        {
            // no-op
        }
    }

    public static class SplittingCompactionTask extends CompactionTask
    {
        private final int sstableSizeInMB;

        public SplittingCompactionTask(ColumnFamilyStore cfs, LifecycleTransaction transaction, int sstableSizeInMB)
        {
            super(cfs, transaction, CompactionManager.NO_GC, false);
            this.sstableSizeInMB = sstableSizeInMB;

            if (sstableSizeInMB <= 0)
                throw new IllegalArgumentException("Invalid target size for SSTables, must be > 0 (got: " + sstableSizeInMB + ")");
        }

        @Override
        protected CompactionController getCompactionController(Set<SSTableReader> toCompact)
        {
            return new SplitController(cfs);
        }

        @Override
        public CompactionAwareWriter getCompactionAwareWriter(ColumnFamilyStore cfs,
                                                              Directories directories,
                                                              LifecycleTransaction txn,
                                                              Set<SSTableReader> nonExpiredSSTables)
        {
            return new MaxSSTableSizeWriter(cfs, directories, txn, nonExpiredSSTables, sstableSizeInMB * 1024L * 1024L, 0, false);
        }

        @Override
        protected boolean partialCompactionsAcceptable()
        {
            return true;
        }
    }

    public static class SplitController extends CompactionController
    {
        public SplitController(ColumnFamilyStore cfs)
        {
            super(cfs, CompactionManager.NO_GC);
        }

        @Override
        public Predicate<Long> getPurgeEvaluator(DecoratedKey key)
        {
            return time -> false;
        }
    }
}
