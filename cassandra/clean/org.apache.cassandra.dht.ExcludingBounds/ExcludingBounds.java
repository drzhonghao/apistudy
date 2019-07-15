import org.apache.cassandra.dht.RingPosition;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.*;


import java.util.Collections;
import java.util.List;

import org.apache.cassandra.utils.Pair;

/**
 * AbstractBounds containing neither of its endpoints: (left, right).  Used by {@code CQL key > X AND key < Y} range scans.
 */
public class ExcludingBounds<T extends RingPosition<T>> extends AbstractBounds<T>
{
    public ExcludingBounds(T left, T right)
    {
        super(left, right);
        // unlike a Range, an ExcludingBounds may not wrap, nor be empty
        assert !strictlyWrapsAround(left, right) && (right.isMinimum() || left.compareTo(right) != 0) : "(" + left + "," + right + ")";
    }

    public boolean contains(T position)
    {
        return Range.contains(left, right, position) && !right.equals(position);
    }

    public Pair<AbstractBounds<T>, AbstractBounds<T>> split(T position)
    {
        assert contains(position) || left.equals(position);
        if (left.equals(position))
            return null;
        AbstractBounds<T> lb = new Range<T>(left, position);
        AbstractBounds<T> rb = new ExcludingBounds<T>(position, right);
        return Pair.create(lb, rb);
    }

    public boolean inclusiveLeft()
    {
        return false;
    }

    public boolean inclusiveRight()
    {
        return false;
    }

    public List<? extends AbstractBounds<T>> unwrap()
    {
        // ExcludingBounds objects never wrap
        return Collections.<AbstractBounds<T>>singletonList(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof ExcludingBounds))
            return false;
        ExcludingBounds<T> rhs = (ExcludingBounds<T>)o;
        return left.equals(rhs.left) && right.equals(rhs.right);
    }

    @Override
    public String toString()
    {
        return "(" + left + "," + right + ")";
    }

    protected String getOpeningString()
    {
        return "(";
    }

    protected String getClosingString()
    {
        return ")";
    }

    public boolean isStartInclusive()
    {
        return false;
    }

    public boolean isEndInclusive()
    {
        return false;
    }

    public AbstractBounds<T> withNewRight(T newRight)
    {
        return new ExcludingBounds<T>(left, newRight);
    }
}
