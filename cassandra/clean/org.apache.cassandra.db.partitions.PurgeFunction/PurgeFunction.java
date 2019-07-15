import org.apache.cassandra.db.DeletionPurger;
import org.apache.cassandra.db.partitions.*;


import java.util.function.Predicate;

import org.apache.cassandra.db.*;
import org.apache.cassandra.db.rows.*;
import org.apache.cassandra.db.transform.Transformation;

public abstract class PurgeFunction extends Transformation<UnfilteredRowIterator>
{
    private final boolean isForThrift;
    private final DeletionPurger purger;
    private final int nowInSec;

    private final boolean enforceStrictLiveness;
    private boolean isReverseOrder;

    public PurgeFunction(boolean isForThrift,
                         int nowInSec,
                         int gcBefore,
                         int oldestUnrepairedTombstone,
                         boolean onlyPurgeRepairedTombstones,
                         boolean enforceStrictLiveness)
    {
        this.isForThrift = isForThrift;
        this.nowInSec = nowInSec;
        this.purger = (timestamp, localDeletionTime) ->
                      !(onlyPurgeRepairedTombstones && localDeletionTime >= oldestUnrepairedTombstone)
                      && localDeletionTime < gcBefore
                      && getPurgeEvaluator().test(timestamp);
        this.enforceStrictLiveness = enforceStrictLiveness;
    }

    protected abstract Predicate<Long> getPurgeEvaluator();

    // Called at the beginning of each new partition
    protected void onNewPartition(DecoratedKey partitionKey)
    {
    }

    // Called for each partition that had only purged infos and are empty post-purge.
    protected void onEmptyPartitionPostPurge(DecoratedKey partitionKey)
    {
    }

    // Called for every unfiltered. Meant for CompactionIterator to update progress
    protected void updateProgress()
    {
    }

    @Override
    protected UnfilteredRowIterator applyToPartition(UnfilteredRowIterator partition)
    {
        onNewPartition(partition.partitionKey());

        isReverseOrder = partition.isReverseOrder();
        UnfilteredRowIterator purged = Transformation.apply(partition, this);
        if (!isForThrift && purged.isEmpty())
        {
            onEmptyPartitionPostPurge(purged.partitionKey());
            purged.close();
            return null;
        }

        return purged;
    }

    @Override
    protected DeletionTime applyToDeletion(DeletionTime deletionTime)
    {
        return purger.shouldPurge(deletionTime) ? DeletionTime.LIVE : deletionTime;
    }

    @Override
    protected Row applyToStatic(Row row)
    {
        updateProgress();
        return row.purge(purger, nowInSec, enforceStrictLiveness);
    }

    @Override
    protected Row applyToRow(Row row)
    {
        updateProgress();
        return row.purge(purger, nowInSec, enforceStrictLiveness);
    }

    @Override
    protected RangeTombstoneMarker applyToMarker(RangeTombstoneMarker marker)
    {
        updateProgress();
        boolean reversed = isReverseOrder;
        if (marker.isBoundary())
        {
            // We can only skip the whole marker if both deletion time are purgeable.
            // If only one of them is, filterTombstoneMarker will deal with it.
            RangeTombstoneBoundaryMarker boundary = (RangeTombstoneBoundaryMarker)marker;
            boolean shouldPurgeClose = purger.shouldPurge(boundary.closeDeletionTime(reversed));
            boolean shouldPurgeOpen = purger.shouldPurge(boundary.openDeletionTime(reversed));

            if (shouldPurgeClose)
            {
                if (shouldPurgeOpen)
                    return null;

                return boundary.createCorrespondingOpenMarker(reversed);
            }

            return shouldPurgeOpen
                   ? boundary.createCorrespondingCloseMarker(reversed)
                   : marker;
        }
        else
        {
            return purger.shouldPurge(((RangeTombstoneBoundMarker)marker).deletionTime()) ? null : marker;
        }
    }
}
