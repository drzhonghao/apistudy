import org.apache.cassandra.cache.CacheKey;
import org.apache.cassandra.cache.*;


import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.ObjectSizes;
import org.apache.cassandra.utils.Pair;

public final class RowCacheKey extends CacheKey
{
    public final byte[] key;

    private static final long EMPTY_SIZE = ObjectSizes.measure(new RowCacheKey(null, ByteBufferUtil.EMPTY_BYTE_BUFFER));

    public RowCacheKey(Pair<String, String> ksAndCFName, byte[] key)
    {
        super(ksAndCFName);
        this.key = key;
    }

    public RowCacheKey(Pair<String, String> ksAndCFName, DecoratedKey key)
    {
        this(ksAndCFName, key.getKey());
    }

    public RowCacheKey(Pair<String, String> ksAndCFName, ByteBuffer key)
    {
        super(ksAndCFName);
        this.key = ByteBufferUtil.getArray(key);
        assert this.key != null;
    }

    public long unsharedHeapSize()
    {
        return EMPTY_SIZE + ObjectSizes.sizeOfArray(key);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RowCacheKey that = (RowCacheKey) o;

        return ksAndCFName.equals(that.ksAndCFName) && Arrays.equals(key, that.key);
    }

    @Override
    public int hashCode()
    {
        int result = ksAndCFName.hashCode();
        result = 31 * result + (key != null ? Arrays.hashCode(key) : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return String.format("RowCacheKey(ksAndCFName:%s, key:%s)", ksAndCFName, Arrays.toString(key));
    }
}
