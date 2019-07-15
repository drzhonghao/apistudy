import org.apache.cassandra.db.rows.Unfiltered;
import org.apache.cassandra.db.rows.UnfilteredRowIterator;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.db.rows.*;


import com.google.common.collect.UnmodifiableIterator;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.*;

/**
 * Abstract class to make writing unfiltered iterators that wrap another iterator
 * easier. By default, the wrapping iterator simply delegate every call to
 * the wrapped iterator so concrete implementations will have to override
 * some of the methods.
 * <p>
 * Note that if most of what you want to do is modifying/filtering the returned
 * {@code Unfiltered}, {@link org.apache.cassandra.db.transform.Transformation#apply(UnfilteredRowIterator,Transformation)} can be a simpler option.
 */
public abstract class WrappingUnfilteredRowIterator extends UnmodifiableIterator<Unfiltered>  implements UnfilteredRowIterator
{
    protected final UnfilteredRowIterator wrapped;

    protected WrappingUnfilteredRowIterator(UnfilteredRowIterator wrapped)
    {
        this.wrapped = wrapped;
    }

    public CFMetaData metadata()
    {
        return wrapped.metadata();
    }

    public PartitionColumns columns()
    {
        return wrapped.columns();
    }

    public boolean isReverseOrder()
    {
        return wrapped.isReverseOrder();
    }

    public DecoratedKey partitionKey()
    {
        return wrapped.partitionKey();
    }

    public DeletionTime partitionLevelDeletion()
    {
        return wrapped.partitionLevelDeletion();
    }

    public Row staticRow()
    {
        return wrapped.staticRow();
    }

    public EncodingStats stats()
    {
        return wrapped.stats();
    }

    public boolean hasNext()
    {
        return wrapped.hasNext();
    }

    public Unfiltered next()
    {
        return wrapped.next();
    }

    public void close()
    {
        wrapped.close();
    }
}
