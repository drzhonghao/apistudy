import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.partitions.*;


import java.util.Iterator;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionInfo;
import org.apache.cassandra.db.PartitionColumns;
import org.apache.cassandra.db.rows.*;

public class FilteredPartition extends ImmutableBTreePartition
{
    public FilteredPartition(RowIterator rows)
    {
        super(rows.metadata(), rows.partitionKey(), build(rows, DeletionInfo.LIVE, false, 16));
    }

    /**
     * Create a FilteredPartition holding all the rows of the provided iterator.
     *
     * Warning: Note that this method does not close the provided iterator and it is
     * up to the caller to do so.
     */
    public static FilteredPartition create(RowIterator iterator)
    {
        return new FilteredPartition(iterator);
    }

    public RowIterator rowIterator()
    {
        final Iterator<Row> iter = iterator();
        return new RowIterator()
        {
            public CFMetaData metadata()
            {
                return metadata;
            }

            public boolean isReverseOrder()
            {
                return false;
            }

            public PartitionColumns columns()
            {
                return FilteredPartition.this.columns();
            }

            public DecoratedKey partitionKey()
            {
                return FilteredPartition.this.partitionKey();
            }

            public Row staticRow()
            {
                return FilteredPartition.this.staticRow();
            }

            public void close() {}

            public boolean hasNext()
            {
                return iter.hasNext();
            }

            public Row next()
            {
                return iter.next();
            }

            public boolean isEmpty()
            {
                return staticRow().isEmpty() && !hasRows();
            }
        };
    }
}
