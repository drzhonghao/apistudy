import org.apache.cassandra.cache.CacheKey;
import org.apache.cassandra.cache.*;


import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.cassandra.io.sstable.Descriptor;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.ObjectSizes;
import org.apache.cassandra.utils.Pair;

public class KeyCacheKey extends CacheKey
{
    public final Descriptor desc;

    private static final long EMPTY_SIZE = ObjectSizes.measure(new KeyCacheKey(null, null, ByteBufferUtil.EMPTY_BYTE_BUFFER));

    // keeping an array instead of a ByteBuffer lowers the overhead of the key cache working set,
    // without extra copies on lookup since client-provided key ByteBuffers will be array-backed already
    public final byte[] key;

    public KeyCacheKey(Pair<String, String> ksAndCFName, Descriptor desc, ByteBuffer key)
    {

        super(ksAndCFName);
        this.desc = desc;
        this.key = ByteBufferUtil.getArray(key);
        assert this.key != null;
    }

    public String toString()
    {
        return String.format("KeyCacheKey(%s, %s)", desc, ByteBufferUtil.bytesToHex(ByteBuffer.wrap(key)));
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

        KeyCacheKey that = (KeyCacheKey) o;

        return ksAndCFName.equals(that.ksAndCFName) && desc.equals(that.desc) && Arrays.equals(key, that.key);
    }

    @Override
    public int hashCode()
    {
        int result = ksAndCFName.hashCode();
        result = 31 * result + desc.hashCode();
        result = 31 * result + Arrays.hashCode(key);
        return result;
    }
}
