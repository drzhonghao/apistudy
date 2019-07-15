import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.*;


import java.util.Objects;

import org.apache.cassandra.db.rows.RangeTombstoneMarker;


/**
 * A range tombstone is a tombstone that covers a slice/range of rows.
 * <p>
 * Note that in most of the storage engine, a range tombstone is actually represented by its separated
 * opening and closing bound, see {@link RangeTombstoneMarker}. So in practice, this is only used when
 * full partitions are materialized in memory in a {@code Partition} object, and more precisely through
 * the use of a {@code RangeTombstoneList} in a {@code DeletionInfo} object.
 */
public class RangeTombstone
{
    private final Slice slice;
    private final DeletionTime deletion;

    public RangeTombstone(Slice slice, DeletionTime deletion)
    {
        this.slice = slice;
        this.deletion = deletion;
    }

    /**
     * The slice of rows that is deleted by this range tombstone.
     *
     * @return the slice of rows that is deleted by this range tombstone.
     */
    public Slice deletedSlice()
    {
        return slice;
    }

    /**
     * The deletion time for this (range) tombstone.
     *
     * @return the deletion time for this range tombstone.
     */
    public DeletionTime deletionTime()
    {
        return deletion;
    }

    public String toString(ClusteringComparator comparator)
    {
        return slice.toString(comparator) + '@' + deletion;
    }

    @Override
    public boolean equals(Object other)
    {
        if(!(other instanceof RangeTombstone))
            return false;

        RangeTombstone that = (RangeTombstone)other;
        return this.deletedSlice().equals(that.deletedSlice())
            && this.deletionTime().equals(that.deletionTime());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(deletedSlice(), deletionTime());
    }
}
