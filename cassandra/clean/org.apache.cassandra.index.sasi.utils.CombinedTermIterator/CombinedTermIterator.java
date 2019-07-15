import org.apache.cassandra.index.sasi.utils.*;


import java.nio.ByteBuffer;

import org.apache.cassandra.index.sasi.disk.Descriptor;
import org.apache.cassandra.index.sasi.disk.OnDiskIndex;
import org.apache.cassandra.index.sasi.disk.TokenTreeBuilder;
import org.apache.cassandra.index.sasi.sa.IndexedTerm;
import org.apache.cassandra.index.sasi.sa.TermIterator;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.utils.Pair;

@SuppressWarnings("resource")
public class CombinedTermIterator extends TermIterator
{
    final Descriptor descriptor;
    final RangeIterator<OnDiskIndex.DataTerm, CombinedTerm> union;
    final ByteBuffer min;
    final ByteBuffer max;

    public CombinedTermIterator(OnDiskIndex... sas)
    {
        this(Descriptor.CURRENT, sas);
    }

    public CombinedTermIterator(Descriptor d, OnDiskIndex... parts)
    {
        descriptor = d;
        union = OnDiskIndexIterator.union(parts);

        AbstractType<?> comparator = parts[0].getComparator(); // assumes all SAs have same comparator
        ByteBuffer minimum = parts[0].minTerm();
        ByteBuffer maximum = parts[0].maxTerm();

        for (int i = 1; i < parts.length; i++)
        {
            OnDiskIndex part = parts[i];
            if (part == null)
                continue;

            minimum = comparator.compare(minimum, part.minTerm()) > 0 ? part.minTerm() : minimum;
            maximum = comparator.compare(maximum, part.maxTerm()) < 0 ? part.maxTerm() : maximum;
        }

        min = minimum;
        max = maximum;
    }

    public ByteBuffer minTerm()
    {
        return min;
    }

    public ByteBuffer maxTerm()
    {
        return max;
    }

    protected Pair<IndexedTerm, TokenTreeBuilder> computeNext()
    {
        if (!union.hasNext())
        {
            return endOfData();
        }
        else
        {
            CombinedTerm term = union.next();
            return Pair.create(new IndexedTerm(term.getTerm(), term.isPartial()), term.getTokenTreeBuilder());
        }

    }
}
