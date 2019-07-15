import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ClusteringPrefix;
import org.apache.cassandra.db.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.utils.ObjectSizes;

public abstract class AbstractBufferClusteringPrefix extends AbstractClusteringPrefix
{
    public static final ByteBuffer[] EMPTY_VALUES_ARRAY = new ByteBuffer[0];
    private static final long EMPTY_SIZE = ObjectSizes.measure(Clustering.make(EMPTY_VALUES_ARRAY));

    protected final Kind kind;
    protected final ByteBuffer[] values;

    protected AbstractBufferClusteringPrefix(Kind kind, ByteBuffer[] values)
    {
        this.kind = kind;
        this.values = values;
    }

    public Kind kind()
    {
        return kind;
    }

    public ClusteringPrefix clustering()
    {
        return this;
    }

    public int size()
    {
        return values.length;
    }

    public ByteBuffer get(int i)
    {
        return values[i];
    }

    public ByteBuffer[] getRawValues()
    {
        return values;
    }

    public long unsharedHeapSize()
    {
        return EMPTY_SIZE + ObjectSizes.sizeOnHeapOf(values);
    }

    public long unsharedHeapSizeExcludingData()
    {
        return EMPTY_SIZE + ObjectSizes.sizeOnHeapExcludingData(values);
    }
}
