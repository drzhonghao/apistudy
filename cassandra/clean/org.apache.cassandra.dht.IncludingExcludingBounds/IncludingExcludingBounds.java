import org.apache.cassandra.dht.RingPosition;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.ExcludingBounds;
import org.apache.cassandra.dht.*;


import java.util.Collections;
import java.util.List;

import org.apache.cassandra.utils.Pair;

/**
 * AbstractBounds containing only its left endpoint: [left, right).  Used by {@code CQL key >= X AND key < Y} range scans.
 */
public class IncludingExcludingBounds<T extends RingPosition<T>> extends AbstractBounds<T>
{
    public IncludingExcludingBounds(T left, T right)
    {
        super(left, right);
        // unlike a Range, an IncludingExcludingBounds may not wrap, nor have
        // right == left unless the right is the min token
        assert !strictlyWrapsAround(left, right) && (right.isMinimum() || left.compareTo(right) != 0) : "(" + left + "," + right + ")";
    }

    public boolean contains(T position)
    {
        return (Range.contains(left, right, position) || left.equals(position)) && !right.equals(position);
    }

    public Pair<AbstractBounds<T>, AbstractBounds<T>> split(T position)
    {
        assert contains(position);
        AbstractBounds<T> lb = new Bounds<T>(left, position);
        AbstractBounds<T> rb = new ExcludingBounds<T>(position, right);
        return Pair.create(lb, rb);
    }

    public boolean inclusiveLeft()
    {
        return true;
    }

    public boolean inclusiveRight()
    {
        return false;
    }

    public List<? extends AbstractBounds<T>> unwrap()
    {
        // IncludingExcludingBounds objects never wrap
        return Collections.<AbstractBounds<T>>singletonList(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof IncludingExcludingBounds))
            return false;
        IncludingExcludingBounds<?> rhs = (IncludingExcludingBounds<?>)o;
        return left.equals(rhs.left) && right.equals(rhs.right);
    }

    @Override
    public String toString()
    {
        return "[" + left + "," + right + ")";
    }

    protected String getOpeningString()
    {
        return "[";
    }

    protected String getClosingString()
    {
        return ")";
    }

    public boolean isStartInclusive()
    {
        return true;
    }

    public boolean isEndInclusive()
    {
        return false;
    }

    public AbstractBounds<T> withNewRight(T newRight)
    {
        return new IncludingExcludingBounds<T>(left, newRight);
    }
}
