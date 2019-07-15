import org.apache.cassandra.db.compaction.writers.*;


import java.util.Set;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.SSTableWriter;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;

public class MaxSSTableSizeWriter extends CompactionAwareWriter
{
    private final long maxSSTableSize;
    private final int level;
    private final long estimatedSSTables;
    private final Set<SSTableReader> allSSTables;
    private Directories.DataDirectory sstableDirectory;

    public MaxSSTableSizeWriter(ColumnFamilyStore cfs,
                                Directories directories,
                                LifecycleTransaction txn,
                                Set<SSTableReader> nonExpiredSSTables,
                                long maxSSTableSize,
                                int level)
    {
        this(cfs, directories, txn, nonExpiredSSTables, maxSSTableSize, level, false);
    }

    @Deprecated
    public MaxSSTableSizeWriter(ColumnFamilyStore cfs,
                                Directories directories,
                                LifecycleTransaction txn,
                                Set<SSTableReader> nonExpiredSSTables,
                                long maxSSTableSize,
                                int level,
                                boolean offline,
                                boolean keepOriginals)
    {
        this(cfs, directories, txn, nonExpiredSSTables, maxSSTableSize, level, keepOriginals);
    }

    public MaxSSTableSizeWriter(ColumnFamilyStore cfs,
                                Directories directories,
                                LifecycleTransaction txn,
                                Set<SSTableReader> nonExpiredSSTables,
                                long maxSSTableSize,
                                int level,
                                boolean keepOriginals)
    {
        super(cfs, directories, txn, nonExpiredSSTables, keepOriginals);
        this.allSSTables = txn.originals();
        this.level = level;
        this.maxSSTableSize = maxSSTableSize;

        long totalSize = getTotalWriteSize(nonExpiredSSTables, estimatedTotalKeys, cfs, txn.opType());
        estimatedSSTables = Math.max(1, totalSize / maxSSTableSize);
    }

    /**
     * Gets the estimated total amount of data to write during compaction
     */
    private static long getTotalWriteSize(Iterable<SSTableReader> nonExpiredSSTables, long estimatedTotalKeys, ColumnFamilyStore cfs, OperationType compactionType)
    {
        long estimatedKeysBeforeCompaction = 0;
        for (SSTableReader sstable : nonExpiredSSTables)
            estimatedKeysBeforeCompaction += sstable.estimatedKeys();
        estimatedKeysBeforeCompaction = Math.max(1, estimatedKeysBeforeCompaction);
        double estimatedCompactionRatio = (double) estimatedTotalKeys / estimatedKeysBeforeCompaction;

        return Math.round(estimatedCompactionRatio * cfs.getExpectedCompactedFileSize(nonExpiredSSTables, compactionType));
    }

    protected boolean realAppend(UnfilteredRowIterator partition)
    {
        RowIndexEntry rie = sstableWriter.append(partition);
        if (sstableWriter.currentWriter().getEstimatedOnDiskBytesWritten() > maxSSTableSize)
        {
            switchCompactionLocation(sstableDirectory);
        }
        return rie != null;
    }

    @Override
    public void switchCompactionLocation(Directories.DataDirectory location)
    {
        sstableDirectory = location;
        @SuppressWarnings("resource")
        SSTableWriter writer = SSTableWriter.create(Descriptor.fromFilename(cfs.getSSTablePath(getDirectories().getLocationForDisk(sstableDirectory))),
                                                    estimatedTotalKeys / estimatedSSTables,
                                                    minRepairedAt,
                                                    cfs.metadata,
                                                    new MetadataCollector(allSSTables, cfs.metadata.comparator, level),
                                                    SerializationHeader.make(cfs.metadata, nonExpiredSSTables),
                                                    cfs.indexManager.listIndexes(),
                                                    txn);

        sstableWriter.switchWriter(writer);
    }
}
