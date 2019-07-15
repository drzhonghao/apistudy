import org.apache.cassandra.db.rows.*;


import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.filter.ColumnFilter;
import org.apache.cassandra.db.transform.Transformation;

/**
 * Function to skip cells (from an iterator) that are not part of those queried by the user
 * according to the provided {@code ColumnFilter}. See {@link UnfilteredRowIterators#withOnlyQueriedData}
 * for more details.
 */
public class WithOnlyQueriedData<I extends BaseRowIterator<?>> extends Transformation<I>
{
    private final ColumnFilter filter;

    public WithOnlyQueriedData(ColumnFilter filter)
    {
        this.filter = filter;
    }

    @Override
    protected PartitionColumns applyToPartitionColumns(PartitionColumns columns)
    {
        return filter.queriedColumns();
    }

    @Override
    protected Row applyToStatic(Row row)
    {
        return row.withOnlyQueriedData(filter);
    }

    @Override
    protected Row applyToRow(Row row)
    {
        return row.withOnlyQueriedData(filter);
    }
};
