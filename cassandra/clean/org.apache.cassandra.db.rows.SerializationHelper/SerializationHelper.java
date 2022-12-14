import org.apache.cassandra.db.rows.*;


import java.nio.ByteBuffer;
import java.util.*;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.context.CounterContext;
import org.apache.cassandra.db.filter.ColumnFilter;

public class SerializationHelper
{
    /**
     * Flag affecting deserialization behavior (this only affect counters in practice).
     *  - LOCAL: for deserialization of local data (Expired columns are
     *      converted to tombstones (to gain disk space)).
     *  - FROM_REMOTE: for deserialization of data received from remote hosts
     *      (Expired columns are converted to tombstone and counters have
     *      their delta cleared)
     *  - PRESERVE_SIZE: used when no transformation must be performed, i.e,
     *      when we must ensure that deserializing and reserializing the
     *      result yield the exact same bytes. Streaming uses this.
     */
    public enum Flag
    {
        LOCAL, FROM_REMOTE, PRESERVE_SIZE
    }

    private final Flag flag;
    public final int version;

    private final ColumnFilter columnsToFetch;
    private ColumnFilter.Tester tester;

    private final Map<ByteBuffer, CFMetaData.DroppedColumn> droppedColumns;
    private CFMetaData.DroppedColumn currentDroppedComplex;


    public SerializationHelper(CFMetaData metadata, int version, Flag flag, ColumnFilter columnsToFetch)
    {
        this.flag = flag;
        this.version = version;
        this.columnsToFetch = columnsToFetch;
        this.droppedColumns = metadata.getDroppedColumns();
    }

    public SerializationHelper(CFMetaData metadata, int version, Flag flag)
    {
        this(metadata, version, flag, null);
    }

    public boolean includes(ColumnDefinition column)
    {
        return columnsToFetch == null || columnsToFetch.fetches(column);
    }

    public boolean includes(Cell cell, LivenessInfo rowLiveness)
    {
        if (columnsToFetch == null)
            return true;

        // During queries, some columns are included even though they are not queried by the user because
        // we always need to distinguish between having a row (with potentially only null values) and not
        // having a row at all (see #CASSANDRA-7085 for background). In the case where the column is not
        // actually requested by the user however (canSkipValue), we can skip the full cell if the cell
        // timestamp is lower than the row one, because in that case, the row timestamp is enough proof
        // of the liveness of the row. Otherwise, we'll only be able to skip the values of those cells.
        ColumnDefinition column = cell.column();
        if (column.isComplex())
        {
            if (!includes(cell.path()))
                return false;

            return !canSkipValue(cell.path()) || cell.timestamp() >= rowLiveness.timestamp();
        }
        else
        {
            return columnsToFetch.fetchedColumnIsQueried(column) || cell.timestamp() >= rowLiveness.timestamp();
        }
    }

    public boolean includes(CellPath path)
    {
        return path == null || tester == null || tester.fetches(path);
    }

    public boolean canSkipValue(ColumnDefinition column)
    {
        return columnsToFetch != null && !columnsToFetch.fetchedColumnIsQueried(column);
    }

    public boolean canSkipValue(CellPath path)
    {
        return path != null && tester != null && !tester.fetchedCellIsQueried(path);
    }

    public void startOfComplexColumn(ColumnDefinition column)
    {
        this.tester = columnsToFetch == null ? null : columnsToFetch.newTester(column);
        this.currentDroppedComplex = droppedColumns.get(column.name.bytes);
    }

    public void endOfComplexColumn()
    {
        this.tester = null;
    }

    public boolean isDropped(Cell cell, boolean isComplex)
    {
        CFMetaData.DroppedColumn dropped = isComplex ? currentDroppedComplex : droppedColumns.get(cell.column().name.bytes);
        return dropped != null && cell.timestamp() <= dropped.droppedTime;
    }

    public boolean isDroppedComplexDeletion(DeletionTime complexDeletion)
    {
        return currentDroppedComplex != null && complexDeletion.markedForDeleteAt() <= currentDroppedComplex.droppedTime;
    }

    public ByteBuffer maybeClearCounterValue(ByteBuffer value)
    {
        return flag == Flag.FROM_REMOTE || (flag == Flag.LOCAL && CounterContext.instance().shouldClearLocal(value))
             ? CounterContext.instance().clearAllLocal(value)
             : value;
    }
}
