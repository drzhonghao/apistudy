import org.apache.cassandra.io.sstable.*;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.rows.EncodingStats;
import org.apache.cassandra.db.partitions.PartitionUpdate;
import org.apache.cassandra.io.sstable.format.SSTableFormat;
import org.apache.cassandra.service.ActiveRepairService;
import org.apache.cassandra.utils.Pair;

/**
 * Base class for the sstable writers used by CQLSSTableWriter.
 */
abstract class AbstractSSTableSimpleWriter implements Closeable
{
    protected final File directory;
    protected final CFMetaData metadata;
    protected final PartitionColumns columns;
    protected SSTableFormat.Type formatType = SSTableFormat.Type.current();
    protected static AtomicInteger generation = new AtomicInteger(0);
    protected boolean makeRangeAware = false;

    protected AbstractSSTableSimpleWriter(File directory, CFMetaData metadata, PartitionColumns columns)
    {
        this.metadata = metadata;
        this.directory = directory;
        this.columns = columns;
    }

    protected void setSSTableFormatType(SSTableFormat.Type type)
    {
        this.formatType = type;
    }

    protected void setRangeAwareWriting(boolean makeRangeAware)
    {
        this.makeRangeAware = makeRangeAware;
    }


    protected SSTableTxnWriter createWriter()
    {
        SerializationHeader header = new SerializationHeader(true, metadata, columns, EncodingStats.NO_STATS);

        if (makeRangeAware)
            return SSTableTxnWriter.createRangeAware(metadata, 0,  ActiveRepairService.UNREPAIRED_SSTABLE, formatType, 0, header);

        return SSTableTxnWriter.create(metadata,
                                       createDescriptor(directory, metadata.ksName, metadata.cfName, formatType),
                                       0,
                                       ActiveRepairService.UNREPAIRED_SSTABLE,
                                       0,
                                       header,
                                       Collections.emptySet());
    }

    private static Descriptor createDescriptor(File directory, final String keyspace, final String columnFamily, final SSTableFormat.Type fmt)
    {
        int maxGen = getNextGeneration(directory, columnFamily);
        return new Descriptor(directory, keyspace, columnFamily, maxGen + 1, fmt);
    }

    private static int getNextGeneration(File directory, final String columnFamily)
    {
        final Set<Descriptor> existing = new HashSet<>();
        directory.list(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                Pair<Descriptor, Component> p = SSTable.tryComponentFromFilename(dir, name);
                Descriptor desc = p == null ? null : p.left;
                if (desc == null)
                    return false;

                if (desc.cfname.equals(columnFamily))
                    existing.add(desc);

                return false;
            }
        });
        int maxGen = generation.getAndIncrement();
        for (Descriptor desc : existing)
        {
            while (desc.generation > maxGen)
            {
                maxGen = generation.getAndIncrement();
            }
        }
        return maxGen;
    }

    PartitionUpdate getUpdateFor(ByteBuffer key) throws IOException
    {
        return getUpdateFor(metadata.decorateKey(key));
    }

    /**
     * Returns a PartitionUpdate suitable to write on this writer for the provided key.
     *
     * @param key they partition key for which the returned update will be.
     * @return an update on partition {@code key} that is tied to this writer.
     */
    abstract PartitionUpdate getUpdateFor(DecoratedKey key) throws IOException;
}

