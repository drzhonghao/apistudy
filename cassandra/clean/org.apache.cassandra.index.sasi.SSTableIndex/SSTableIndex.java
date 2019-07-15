import org.apache.cassandra.index.sasi.*;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.index.sasi.conf.ColumnIndex;
import org.apache.cassandra.index.sasi.disk.OnDiskIndex;
import org.apache.cassandra.index.sasi.disk.OnDiskIndexBuilder;
import org.apache.cassandra.index.sasi.disk.Token;
import org.apache.cassandra.index.sasi.plan.Expression;
import org.apache.cassandra.index.sasi.utils.RangeIterator;
import org.apache.cassandra.io.FSReadError;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.concurrent.Ref;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.Function;

public class SSTableIndex
{
    private final ColumnIndex columnIndex;
    private final Ref<SSTableReader> sstableRef;
    private final SSTableReader sstable;
    private final OnDiskIndex index;
    private final AtomicInteger references = new AtomicInteger(1);
    private final AtomicBoolean obsolete = new AtomicBoolean(false);

    public SSTableIndex(ColumnIndex index, File indexFile, SSTableReader referent)
    {
        this.columnIndex = index;
        this.sstableRef = referent.tryRef();
        this.sstable = sstableRef.get();

        if (sstable == null)
            throw new IllegalStateException("Couldn't acquire reference to the sstable: " + referent);

        AbstractType<?> validator = columnIndex.getValidator();

        assert validator != null;
        assert indexFile.exists() : String.format("SSTable %s should have index %s.",
                sstable.getFilename(),
                columnIndex.getIndexName());

        this.index = new OnDiskIndex(indexFile, validator, new DecoratedKeyFetcher(sstable));
    }

    public OnDiskIndexBuilder.Mode mode()
    {
        return index.mode();
    }

    public boolean hasMarkedPartials()
    {
        return index.hasMarkedPartials();
    }

    public ByteBuffer minTerm()
    {
        return index.minTerm();
    }

    public ByteBuffer maxTerm()
    {
        return index.maxTerm();
    }

    public ByteBuffer minKey()
    {
        return index.minKey();
    }

    public ByteBuffer maxKey()
    {
        return index.maxKey();
    }

    public RangeIterator<Long, Token> search(Expression expression)
    {
        return index.search(expression);
    }

    public SSTableReader getSSTable()
    {
        return sstable;
    }

    public String getPath()
    {
        return index.getIndexPath();
    }

    public boolean reference()
    {
        while (true)
        {
            int n = references.get();
            if (n <= 0)
                return false;
            if (references.compareAndSet(n, n + 1))
                return true;
        }
    }

    public void release()
    {
        int n = references.decrementAndGet();
        if (n == 0)
        {
            FileUtils.closeQuietly(index);
            sstableRef.release();
            if (obsolete.get() || sstableRef.globalCount() == 0)
                FileUtils.delete(index.getIndexPath());
        }
    }

    public void markObsolete()
    {
        obsolete.getAndSet(true);
        release();
    }

    public boolean isObsolete()
    {
        return obsolete.get();
    }

    public boolean equals(Object o)
    {
        return o instanceof SSTableIndex && index.getIndexPath().equals(((SSTableIndex) o).index.getIndexPath());
    }

    public int hashCode()
    {
        return new HashCodeBuilder().append(index.getIndexPath()).build();
    }

    public String toString()
    {
        return String.format("SSTableIndex(column: %s, SSTable: %s)", columnIndex.getColumnName(), sstable.descriptor);
    }

    private static class DecoratedKeyFetcher implements Function<Long, DecoratedKey>
    {
        private final SSTableReader sstable;

        DecoratedKeyFetcher(SSTableReader reader)
        {
            sstable = reader;
        }

        public DecoratedKey apply(Long offset)
        {
            try
            {
                return sstable.keyAt(offset);
            }
            catch (IOException e)
            {
                throw new FSReadError(new IOException("Failed to read key from " + sstable.descriptor, e), sstable.getFilename());
            }
        }

        public int hashCode()
        {
            return sstable.descriptor.hashCode();
        }

        public boolean equals(Object other)
        {
            return other instanceof DecoratedKeyFetcher
                    && sstable.descriptor.equals(((DecoratedKeyFetcher) other).sstable.descriptor);
        }
    }
}
