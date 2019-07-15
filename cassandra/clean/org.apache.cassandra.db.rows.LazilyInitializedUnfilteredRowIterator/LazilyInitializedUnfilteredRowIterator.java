import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.*;


import org.apache.cassandra.utils.AbstractIterator;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.*;

/**
 * Abstract class to create UnfilteredRowIterator that lazily initialize themselves.
 *
 * This is used during partition range queries when we know the partition key but want
 * to defer the initialization of the rest of the UnfilteredRowIterator until we need those informations.
 * See {@link org.apache.cassandra.io.sstable.format.big.BigTableScanner.KeyScanningIterator} for instance.
 */
public abstract class LazilyInitializedUnfilteredRowIterator extends AbstractIterator<Unfiltered> implements UnfilteredRowIterator
{
    private final DecoratedKey partitionKey;

    private UnfilteredRowIterator iterator;

    public LazilyInitializedUnfilteredRowIterator(DecoratedKey partitionKey)
    {
        this.partitionKey = partitionKey;
    }

    protected abstract UnfilteredRowIterator initializeIterator();

    protected void maybeInit()
    {
        if (iterator == null)
            iterator = initializeIterator();
    }

    public boolean initialized()
    {
        return iterator != null;
    }

    public CFMetaData metadata()
    {
        maybeInit();
        return iterator.metadata();
    }

    public PartitionColumns columns()
    {
        maybeInit();
        return iterator.columns();
    }

    public boolean isReverseOrder()
    {
        maybeInit();
        return iterator.isReverseOrder();
    }

    public DecoratedKey partitionKey()
    {
        return partitionKey;
    }

    public DeletionTime partitionLevelDeletion()
    {
        maybeInit();
        return iterator.partitionLevelDeletion();
    }

    public Row staticRow()
    {
        maybeInit();
        return iterator.staticRow();
    }

    public EncodingStats stats()
    {
        maybeInit();
        return iterator.stats();
    }

    protected Unfiltered computeNext()
    {
        maybeInit();
        return iterator.hasNext() ? iterator.next() : endOfData();
    }

    public void close()
    {
        if (iterator != null)
            iterator.close();
    }
}
