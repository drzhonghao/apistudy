import org.apache.cassandra.db.ClusteringBoundary;
import org.apache.cassandra.db.ClusteringBound;
import org.apache.cassandra.db.rows.*;


import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.Set;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.*;
import org.apache.cassandra.utils.memory.AbstractAllocator;

/**
 * A range tombstone marker that represents a boundary between 2 range tombstones (i.e. it closes one range and open another).
 */
public class RangeTombstoneBoundaryMarker extends AbstractRangeTombstoneMarker<ClusteringBoundary>
{
    private final DeletionTime endDeletion;
    private final DeletionTime startDeletion;

    public RangeTombstoneBoundaryMarker(ClusteringBoundary bound, DeletionTime endDeletion, DeletionTime startDeletion)
    {
        super(bound);
        assert bound.isBoundary();
        this.endDeletion = endDeletion;
        this.startDeletion = startDeletion;
    }

    public static RangeTombstoneBoundaryMarker exclusiveCloseInclusiveOpen(boolean reversed, ByteBuffer[] boundValues, DeletionTime closeDeletion, DeletionTime openDeletion)
    {
        ClusteringBoundary bound = ClusteringBoundary.exclusiveCloseInclusiveOpen(reversed, boundValues);
        DeletionTime endDeletion = reversed ? openDeletion : closeDeletion;
        DeletionTime startDeletion = reversed ? closeDeletion : openDeletion;
        return new RangeTombstoneBoundaryMarker(bound, endDeletion, startDeletion);
    }

    public static RangeTombstoneBoundaryMarker inclusiveCloseExclusiveOpen(boolean reversed, ByteBuffer[] boundValues, DeletionTime closeDeletion, DeletionTime openDeletion)
    {
        ClusteringBoundary bound = ClusteringBoundary.inclusiveCloseExclusiveOpen(reversed, boundValues);
        DeletionTime endDeletion = reversed ? openDeletion : closeDeletion;
        DeletionTime startDeletion = reversed ? closeDeletion : openDeletion;
        return new RangeTombstoneBoundaryMarker(bound, endDeletion, startDeletion);
    }

    /**
     * The deletion time for the range tombstone this boundary ends (in clustering order).
     */
    public DeletionTime endDeletionTime()
    {
        return endDeletion;
    }

    /**
     * The deletion time for the range tombstone this boundary starts (in clustering order).
     */
    public DeletionTime startDeletionTime()
    {
        return startDeletion;
    }

    public DeletionTime closeDeletionTime(boolean reversed)
    {
        return reversed ? startDeletion : endDeletion;
    }

    public DeletionTime openDeletionTime(boolean reversed)
    {
        return reversed ? endDeletion : startDeletion;
    }

    public boolean openIsInclusive(boolean reversed)
    {
        return (bound.kind() == ClusteringPrefix.Kind.EXCL_END_INCL_START_BOUNDARY) ^ reversed;
    }

    public ClusteringBound openBound(boolean reversed)
    {
        return bound.openBound(reversed);
    }

    public ClusteringBound closeBound(boolean reversed)
    {
        return bound.closeBound(reversed);
    }

    public boolean closeIsInclusive(boolean reversed)
    {
        return (bound.kind() == ClusteringPrefix.Kind.INCL_END_EXCL_START_BOUNDARY) ^ reversed;
    }

    public boolean isOpen(boolean reversed)
    {
        // A boundary always open one side
        return true;
    }

    public boolean isClose(boolean reversed)
    {
        // A boundary always close one side
        return true;
    }

    public RangeTombstoneBoundaryMarker copy(AbstractAllocator allocator)
    {
        return new RangeTombstoneBoundaryMarker(clustering().copy(allocator), endDeletion, startDeletion);
    }

    public RangeTombstoneBoundaryMarker withNewOpeningDeletionTime(boolean reversed, DeletionTime newDeletionTime)
    {
        return new RangeTombstoneBoundaryMarker(clustering(), reversed ? newDeletionTime : endDeletion, reversed ? startDeletion : newDeletionTime);
    }

    public static RangeTombstoneBoundaryMarker makeBoundary(boolean reversed, ClusteringBound close, ClusteringBound open, DeletionTime closeDeletion, DeletionTime openDeletion)
    {
        assert ClusteringPrefix.Kind.compare(close.kind(), open.kind()) == 0 : "Both bound don't form a boundary";
        boolean isExclusiveClose = close.isExclusive() || (close.isInclusive() && open.isInclusive() && openDeletion.supersedes(closeDeletion));
        return isExclusiveClose
             ? exclusiveCloseInclusiveOpen(reversed, close.getRawValues(), closeDeletion, openDeletion)
             : inclusiveCloseExclusiveOpen(reversed, close.getRawValues(), closeDeletion, openDeletion);
    }

    public RangeTombstoneBoundMarker createCorrespondingCloseMarker(boolean reversed)
    {
        return new RangeTombstoneBoundMarker(closeBound(reversed), endDeletion);
    }

    public RangeTombstoneBoundMarker createCorrespondingOpenMarker(boolean reversed)
    {
        return new RangeTombstoneBoundMarker(openBound(reversed), startDeletion);
    }

    public void digest(MessageDigest digest)
    {
        bound.digest(digest);
        endDeletion.digest(digest);
        startDeletion.digest(digest);
    }

    @Override
    public void digest(MessageDigest digest, Set<ByteBuffer> columnsToExclude)
    {
        digest(digest);
    }

    public String toString(CFMetaData metadata)
    {
        return String.format("Marker %s@%d-%d", bound.toString(metadata), endDeletion.markedForDeleteAt(), startDeletion.markedForDeleteAt());
    }

    @Override
    public boolean equals(Object other)
    {
        if(!(other instanceof RangeTombstoneBoundaryMarker))
            return false;

        RangeTombstoneBoundaryMarker that = (RangeTombstoneBoundaryMarker)other;
        return this.bound.equals(that.bound)
            && this.endDeletion.equals(that.endDeletion)
            && this.startDeletion.equals(that.startDeletion);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(bound, endDeletion, startDeletion);
    }
}
