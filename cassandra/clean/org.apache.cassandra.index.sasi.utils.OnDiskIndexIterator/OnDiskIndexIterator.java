import org.apache.cassandra.index.sasi.utils.*;


import java.io.IOException;
import java.util.Iterator;

import org.apache.cassandra.index.sasi.disk.OnDiskIndex;
import org.apache.cassandra.index.sasi.disk.OnDiskIndex.DataTerm;
import org.apache.cassandra.db.marshal.AbstractType;

public class OnDiskIndexIterator extends RangeIterator<DataTerm, CombinedTerm>
{
    private final AbstractType<?> comparator;
    private final Iterator<DataTerm> terms;

    public OnDiskIndexIterator(OnDiskIndex index)
    {
        super(index.min(), index.max(), Long.MAX_VALUE);

        this.comparator = index.getComparator();
        this.terms = index.iterator();
    }

    public static RangeIterator<DataTerm, CombinedTerm> union(OnDiskIndex... union)
    {
        RangeUnionIterator.Builder<DataTerm, CombinedTerm> builder = RangeUnionIterator.builder();
        for (OnDiskIndex e : union)
        {
            if (e != null)
                builder.add(new OnDiskIndexIterator(e));
        }

        return builder.build();
    }

    protected CombinedTerm computeNext()
    {
        return terms.hasNext() ? new CombinedTerm(comparator, terms.next()) : endOfData();
    }

    protected void performSkipTo(DataTerm nextToken)
    {
        throw new UnsupportedOperationException();
    }

    public void close() throws IOException
    {}
}
