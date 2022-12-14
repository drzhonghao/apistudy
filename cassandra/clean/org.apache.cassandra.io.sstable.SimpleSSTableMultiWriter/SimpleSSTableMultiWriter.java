import org.apache.cassandra.io.sstable.*;


import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.index.Index;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.SSTableWriter;
import org.apache.cassandra.io.sstable.metadata.MetadataCollector;

public class SimpleSSTableMultiWriter implements SSTableMultiWriter
{
    private final SSTableWriter writer;
    private final LifecycleTransaction txn;

    protected SimpleSSTableMultiWriter(SSTableWriter writer, LifecycleTransaction txn)
    {
        this.txn = txn;
        this.writer = writer;
    }

    public boolean append(UnfilteredRowIterator partition)
    {
        RowIndexEntry<?> indexEntry = writer.append(partition);
        return indexEntry != null;
    }

    public Collection<SSTableReader> finish(long repairedAt, long maxDataAge, boolean openResult)
    {
        return Collections.singleton(writer.finish(repairedAt, maxDataAge, openResult));
    }

    public Collection<SSTableReader> finish(boolean openResult)
    {
        return Collections.singleton(writer.finish(openResult));
    }

    public Collection<SSTableReader> finished()
    {
        return Collections.singleton(writer.finished());
    }

    public SSTableMultiWriter setOpenResult(boolean openResult)
    {
        writer.setOpenResult(openResult);
        return this;
    }

    public String getFilename()
    {
        return writer.getFilename();
    }

    public long getFilePointer()
    {
        return writer.getFilePointer();
    }

    public UUID getCfId()
    {
        return writer.metadata.cfId;
    }

    public Throwable commit(Throwable accumulate)
    {
        return writer.commit(accumulate);
    }

    public Throwable abort(Throwable accumulate)
    {
        txn.untrackNew(writer);
        return writer.abort(accumulate);
    }

    public void prepareToCommit()
    {
        writer.prepareToCommit();
    }

    public void close()
    {
        writer.close();
    }

    @SuppressWarnings("resource") // SimpleSSTableMultiWriter closes writer
    public static SSTableMultiWriter create(Descriptor descriptor,
                                            long keyCount,
                                            long repairedAt,
                                            CFMetaData cfm,
                                            MetadataCollector metadataCollector,
                                            SerializationHeader header,
                                            Collection<Index> indexes,
                                            LifecycleTransaction txn)
    {
        SSTableWriter writer = SSTableWriter.create(descriptor, keyCount, repairedAt, cfm, metadataCollector, header, indexes, txn);
        return new SimpleSSTableMultiWriter(writer, txn);
    }
}
