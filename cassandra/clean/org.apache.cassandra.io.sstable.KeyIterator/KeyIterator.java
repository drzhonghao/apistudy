import org.apache.cassandra.io.sstable.*;


import java.io.File;
import java.io.IOException;

import org.apache.cassandra.utils.AbstractIterator;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.RandomAccessReader;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.CloseableIterator;

public class KeyIterator extends AbstractIterator<DecoratedKey> implements CloseableIterator<DecoratedKey>
{
    private final static class In
    {
        private final File path;
        private RandomAccessReader in;

        public In(File path)
        {
            this.path = path;
        }

        private void maybeInit()
        {
            if (in == null)
                in = RandomAccessReader.open(path);
        }

        public DataInputPlus get()
        {
            maybeInit();
            return in;
        }

        public boolean isEOF()
        {
            maybeInit();
            return in.isEOF();
        }

        public void close()
        {
            if (in != null)
                in.close();
        }

        public long getFilePointer()
        {
            maybeInit();
            return in.getFilePointer();
        }

        public long length()
        {
            maybeInit();
            return in.length();
        }
    }

    private final Descriptor desc;
    private final In in;
    private final IPartitioner partitioner;

    private long keyPosition;

    public KeyIterator(Descriptor desc, CFMetaData metadata)
    {
        this.desc = desc;
        in = new In(new File(desc.filenameFor(Component.PRIMARY_INDEX)));
        partitioner = metadata.partitioner;
    }

    protected DecoratedKey computeNext()
    {
        try
        {
            if (in.isEOF())
                return endOfData();

            keyPosition = in.getFilePointer();
            DecoratedKey key = partitioner.decorateKey(ByteBufferUtil.readWithShortLength(in.get()));
            RowIndexEntry.Serializer.skip(in.get(), desc.version); // skip remainder of the entry
            return key;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void close()
    {
        in.close();
    }

    public long getBytesRead()
    {
        return in.getFilePointer();
    }

    public long getTotalBytes()
    {
        return in.length();
    }

    public long getKeyPosition()
    {
        return keyPosition;
    }
}
