import org.apache.cassandra.db.rows.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.ClusteringBoundOrBoundary;

public abstract class AbstractRangeTombstoneMarker<B extends ClusteringBoundOrBoundary> implements RangeTombstoneMarker
{
    protected final B bound;

    protected AbstractRangeTombstoneMarker(B bound)
    {
        this.bound = bound;
    }

    public B clustering()
    {
        return bound;
    }

    public Unfiltered.Kind kind()
    {
        return Unfiltered.Kind.RANGE_TOMBSTONE_MARKER;
    }

    public boolean isBoundary()
    {
        return bound.isBoundary();
    }

    public boolean isOpen(boolean reversed)
    {
        return bound.isOpen(reversed);
    }

    public boolean isClose(boolean reversed)
    {
        return bound.isClose(reversed);
    }

    public void validateData(CFMetaData metadata)
    {
        ClusteringBoundOrBoundary bound = clustering();
        for (int i = 0; i < bound.size(); i++)
        {
            ByteBuffer value = bound.get(i);
            if (value != null)
                metadata.comparator.subtype(i).validate(value);
        }
    }

    public String toString(CFMetaData metadata, boolean fullDetails)
    {
        return toString(metadata);
    }
    public String toString(CFMetaData metadata, boolean includeClusteringKeys, boolean fullDetails)
    {
        return toString(metadata);
    }
}
