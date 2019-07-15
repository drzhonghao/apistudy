import org.apache.cassandra.db.compaction.*;


import java.io.File;
import java.util.*;
import java.util.function.Predicate;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.io.sstable.*;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.SSTableWriter;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.OutputHandler;
import org.apache.cassandra.utils.UUIDGen;

public class Upgrader
{
    private final ColumnFamilyStore cfs;
    private final SSTableReader sstable;
    private final LifecycleTransaction transaction;
    private final File directory;

    private final CompactionController controller;
    private final CompactionStrategyManager strategyManager;
    private final long estimatedRows;

    private final OutputHandler outputHandler;

    public Upgrader(ColumnFamilyStore cfs, LifecycleTransaction txn, OutputHandler outputHandler)
    {
        this.cfs = cfs;
        this.transaction = txn;
        this.sstable = txn.onlyOne();
        this.outputHandler = outputHandler;

        this.directory = new File(sstable.getFilename()).getParentFile();

        this.controller = new UpgradeController(cfs);

        this.strategyManager = cfs.getCompactionStrategyManager();
        long estimatedTotalKeys = Math.max(cfs.metadata.params.minIndexInterval, SSTableReader.getApproximateKeyCount(Arrays.asList(this.sstable)));
        long estimatedSSTables = Math.max(1, SSTableReader.getTotalBytes(Arrays.asList(this.sstable)) / strategyManager.getMaxSSTableBytes());
        this.estimatedRows = (long) Math.ceil((double) estimatedTotalKeys / estimatedSSTables);
    }

    private SSTableWriter createCompactionWriter(long repairedAt)
    {
        MetadataCollector sstableMetadataCollector = new MetadataCollector(cfs.getComparator());
        sstableMetadataCollector.sstableLevel(sstable.getSSTableLevel());
        return SSTableWriter.create(Descriptor.fromFilename(cfs.getSSTablePath(directory)),
                                    estimatedRows,
                                    repairedAt,
                                    cfs.metadata,
                                    sstableMetadataCollector,
                                    SerializationHeader.make(cfs.metadata, Sets.newHashSet(sstable)),
                                    cfs.indexManager.listIndexes(),
                                    transaction);
    }

    public void upgrade(boolean keepOriginals)
    {
        outputHandler.output("Upgrading " + sstable);
        int nowInSec = FBUtilities.nowInSeconds();
        try (SSTableRewriter writer = SSTableRewriter.construct(cfs, transaction, keepOriginals, CompactionTask.getMaxDataAge(transaction.originals()));
             AbstractCompactionStrategy.ScannerList scanners = strategyManager.getScanners(transaction.originals());
             CompactionIterator iter = new CompactionIterator(transaction.opType(), scanners.scanners, controller, nowInSec, UUIDGen.getTimeUUID()))
        {
            writer.switchWriter(createCompactionWriter(sstable.getSSTableMetadata().repairedAt));
            while (iter.hasNext())
                writer.append(iter.next());

            writer.finish();
            outputHandler.output("Upgrade of " + sstable + " complete.");
        }
        catch (Exception e)
        {
            Throwables.propagate(e);
        }
        finally
        {
            controller.close();
        }
    }

    private static class UpgradeController extends CompactionController
    {
        public UpgradeController(ColumnFamilyStore cfs)
        {
            super(cfs, Integer.MAX_VALUE);
        }

        @Override
        public Predicate<Long> getPurgeEvaluator(DecoratedKey key)
        {
            return time -> false;
        }
    }
}

