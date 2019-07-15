import org.apache.cassandra.cache.CacheKey;
import org.apache.cassandra.cache.*;


import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.utils.*;

public final class CounterCacheKey extends CacheKey
{
    private static final long EMPTY_SIZE = ObjectSizes.measure(new CounterCacheKey(null, ByteBufferUtil.EMPTY_BYTE_BUFFER, ByteBuffer.allocate(1)));

    public final byte[] partitionKey;
    public final byte[] cellName;

    public CounterCacheKey(Pair<String, String> ksAndCFName, ByteBuffer partitionKey, ByteBuffer cellName)
    {
        super(ksAndCFName);
        this.partitionKey = ByteBufferUtil.getArray(partitionKey);
        this.cellName = ByteBufferUtil.getArray(cellName);
    }

    public static CounterCacheKey create(Pair<String, String> ksAndCFName, ByteBuffer partitionKey, Clustering clustering, ColumnDefinition c, CellPath path)
    {
        return new CounterCacheKey(ksAndCFName, partitionKey, makeCellName(clustering, c, path));
    }

    private static ByteBuffer makeCellName(Clustering clustering, ColumnDefinition c, CellPath path)
    {
        int cs = clustering.size();
        ByteBuffer[] values = new ByteBuffer[cs + 1 + (path == null ? 0 : path.size())];
        for (int i = 0; i < cs; i++)
            values[i] = clustering.get(i);
        values[cs] = c.name.bytes;
        if (path != null)
            for (int i = 0; i < path.size(); i++)
                values[cs + 1 + i] = path.get(i);
        return CompositeType.build(values);
    }

    public long unsharedHeapSize()
    {
        return EMPTY_SIZE
               + ObjectSizes.sizeOfArray(partitionKey)
               + ObjectSizes.sizeOfArray(cellName);
    }

    @Override
    public String toString()
    {
        return String.format("CounterCacheKey(%s, %s, %s)",
                             ksAndCFName,
                             ByteBufferUtil.bytesToHex(ByteBuffer.wrap(partitionKey)),
                             ByteBufferUtil.bytesToHex(ByteBuffer.wrap(cellName)));
    }

    @Override
    public int hashCode()
    {
        return Arrays.deepHashCode(new Object[]{ksAndCFName, partitionKey, cellName});
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof CounterCacheKey))
            return false;

        CounterCacheKey cck = (CounterCacheKey) o;

        return ksAndCFName.equals(cck.ksAndCFName)
            && Arrays.equals(partitionKey, cck.partitionKey)
            && Arrays.equals(cellName, cck.cellName);
    }
}
