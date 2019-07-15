import org.apache.cassandra.db.compaction.writers.*;



import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.SSTableWriter;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;

/**
 * The default compaction writer - creates one output file in L0
 */
public class DefaultCompactionWriter extends CompactionAwareWriter
{
    protected static final Logger logger = LoggerFactory.getLogger(DefaultCompactionWriter.class);
    private final int sstableLevel;

    public DefaultCompactionWriter(ColumnFamilyStore cfs, Directories directories, LifecycleTransaction txn, Set<SSTableReader> nonExpiredSSTables)
    {
        this(cfs, directories, txn, nonExpiredSSTables, false, 0);
    }

    @Deprecated
    public DefaultCompactionWriter(ColumnFamilyStore cfs, Directories directories, LifecycleTransaction txn, Set<SSTableReader> nonExpiredSSTables, boolean offline, boolean keepOriginals, int sstableLevel)
    {
        this(cfs, directories, txn, nonExpiredSSTables, keepOriginals, sstableLevel);
    }

    @SuppressWarnings("resource")
    public DefaultCompactionWriter(ColumnFamilyStore cfs, Directories directories, LifecycleTransaction txn, Set<SSTableReader> nonExpiredSSTables, boolean keepOriginals, int sstableLevel)
    {
        super(cfs, directories, txn, nonExpiredSSTables, keepOriginals);
        this.sstableLevel = sstableLevel;
    }

    @Override
    public boolean realAppend(UnfilteredRowIterator partition)
    {
        return sstableWriter.append(partition) != null;
    }

    @Override
    public void switchCompactionLocation(Directories.DataDirectory directory)
    {
        @SuppressWarnings("resource")
        SSTableWriter writer = SSTableWriter.create(Descriptor.fromFilename(cfs.getSSTablePath(getDirectories().getLocationForDisk(directory))),
                                                    estimatedTotalKeys,
                                                    minRepairedAt,
                                                    cfs.metadata,
                                                    new MetadataCollector(txn.originals(), cfs.metadata.comparator, sstableLevel),
                                                    SerializationHeader.make(cfs.metadata, nonExpiredSSTables),
                                                    cfs.indexManager.listIndexes(),
                                                    txn);
        sstableWriter.switchWriter(writer);
    }

    @Override
    public long estimatedKeys()
    {
        return estimatedTotalKeys;
    }
}
