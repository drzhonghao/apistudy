import org.apache.cassandra.io.sstable.format.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Directories;
import org.apache.cassandra.db.DiskBoundaries;
import org.apache.cassandra.db.PartitionPosition;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.io.sstable.SSTableMultiWriter;
import org.apache.cassandra.utils.FBUtilities;

public class RangeAwareSSTableWriter implements SSTableMultiWriter
{
    private final List<PartitionPosition> boundaries;
    private final List<Directories.DataDirectory> directories;
    private final int sstableLevel;
    private final long estimatedKeys;
    private final long repairedAt;
    private final SSTableFormat.Type format;
    private final SerializationHeader header;
    private final LifecycleTransaction txn;
    private int currentIndex = -1;
    public final ColumnFamilyStore cfs;
    private final List<SSTableMultiWriter> finishedWriters = new ArrayList<>();
    private final List<SSTableReader> finishedReaders = new ArrayList<>();
    private SSTableMultiWriter currentWriter = null;

    public RangeAwareSSTableWriter(ColumnFamilyStore cfs, long estimatedKeys, long repairedAt, SSTableFormat.Type format, int sstableLevel, long totalSize, LifecycleTransaction txn, SerializationHeader header) throws IOException
    {
        DiskBoundaries db = cfs.getDiskBoundaries();
        directories = db.directories;
        this.sstableLevel = sstableLevel;
        this.cfs = cfs;
        this.estimatedKeys = estimatedKeys / directories.size();
        this.repairedAt = repairedAt;
        this.format = format;
        this.txn = txn;
        this.header = header;
        boundaries = db.positions;
        if (boundaries == null)
        {
            Directories.DataDirectory localDir = cfs.getDirectories().getWriteableLocation(totalSize);
            if (localDir == null)
                throw new IOException(String.format("Insufficient disk space to store %s",
                                                    FBUtilities.prettyPrintMemory(totalSize)));
            Descriptor desc = Descriptor.fromFilename(cfs.getSSTablePath(cfs.getDirectories().getLocationForDisk(localDir), format));
            currentWriter = cfs.createSSTableMultiWriter(desc, estimatedKeys, repairedAt, sstableLevel, header, txn);
        }
    }

    private void maybeSwitchWriter(DecoratedKey key)
    {
        if (boundaries == null)
            return;

        boolean switched = false;
        while (currentIndex < 0 || key.compareTo(boundaries.get(currentIndex)) > 0)
        {
            switched = true;
            currentIndex++;
        }

        if (switched)
        {
            if (currentWriter != null)
                finishedWriters.add(currentWriter);

            Descriptor desc = Descriptor.fromFilename(cfs.getSSTablePath(cfs.getDirectories().getLocationForDisk(directories.get(currentIndex))), format);
            currentWriter = cfs.createSSTableMultiWriter(desc, estimatedKeys, repairedAt, sstableLevel, header, txn);
        }
    }

    public boolean append(UnfilteredRowIterator partition)
    {
        maybeSwitchWriter(partition.partitionKey());
        return currentWriter.append(partition);
    }

    @Override
    public Collection<SSTableReader> finish(long repairedAt, long maxDataAge, boolean openResult)
    {
        if (currentWriter != null)
            finishedWriters.add(currentWriter);
        currentWriter = null;
        for (SSTableMultiWriter writer : finishedWriters)
        {
            if (writer.getFilePointer() > 0)
                finishedReaders.addAll(writer.finish(repairedAt, maxDataAge, openResult));
            else
                SSTableMultiWriter.abortOrDie(writer);
        }
        return finishedReaders;
    }

    @Override
    public Collection<SSTableReader> finish(boolean openResult)
    {
        if (currentWriter != null)
            finishedWriters.add(currentWriter);
        currentWriter = null;
        for (SSTableMultiWriter writer : finishedWriters)
        {
            if (writer.getFilePointer() > 0)
                finishedReaders.addAll(writer.finish(openResult));
            else
                SSTableMultiWriter.abortOrDie(writer);
        }
        return finishedReaders;
    }

    @Override
    public Collection<SSTableReader> finished()
    {
        return finishedReaders;
    }

    @Override
    public SSTableMultiWriter setOpenResult(boolean openResult)
    {
        finishedWriters.forEach((w) -> w.setOpenResult(openResult));
        currentWriter.setOpenResult(openResult);
        return this;
    }

    public String getFilename()
    {
        return String.join("/", cfs.keyspace.getName(), cfs.getTableName());
    }

    @Override
    public long getFilePointer()
    {
        return currentWriter.getFilePointer();
    }

    @Override
    public UUID getCfId()
    {
        return currentWriter.getCfId();
    }

    @Override
    public Throwable commit(Throwable accumulate)
    {
        if (currentWriter != null)
            finishedWriters.add(currentWriter);
        currentWriter = null;
        for (SSTableMultiWriter writer : finishedWriters)
            accumulate = writer.commit(accumulate);
        return accumulate;
    }

    @Override
    public Throwable abort(Throwable accumulate)
    {
        if (currentWriter != null)
            finishedWriters.add(currentWriter);
        currentWriter = null;
        for (SSTableMultiWriter finishedWriter : finishedWriters)
            accumulate = finishedWriter.abort(accumulate);

        return accumulate;
    }

    @Override
    public void prepareToCommit()
    {
        if (currentWriter != null)
            finishedWriters.add(currentWriter);
        currentWriter = null;
        finishedWriters.forEach(SSTableMultiWriter::prepareToCommit);
    }

    @Override
    public void close()
    {
        if (currentWriter != null)
            finishedWriters.add(currentWriter);
        currentWriter = null;
        finishedWriters.forEach(SSTableMultiWriter::close);
    }
}
