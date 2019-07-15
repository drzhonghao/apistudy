import org.apache.cassandra.utils.*;


import com.google.common.base.Objects;

public class Interval<C, D>
{
    public final C min;
    public final C max;
    public final D data;

    public Interval(C min, C max, D data)
    {
        this.min = min;
        this.max = max;
        this.data = data;
    }

    public static <C, D> Interval<C, D> create(C min, C max)
    {
        return create(min, max, null);
    }

    public static <C, D> Interval<C, D> create(C min, C max, D data)
    {
        return new Interval(min, max, data);
    }

    @Override
    public String toString()
    {
        return String.format("[%s, %s]%s", min, max, data == null ? "" : (String.format("(%s)", data)));
    }

    @Override
    public final int hashCode()
    {
        return Objects.hashCode(min, max, data);
    }

    @Override
    public final boolean equals(Object o)
    {
        if(!(o instanceof Interval))
            return false;

        Interval that = (Interval)o;
        // handles nulls properly
        return Objects.equal(min, that.min) && Objects.equal(max, that.max) && Objects.equal(data, that.data);
    }

    private static final AsymmetricOrdering<Interval<Comparable, Object>, Comparable> minOrdering
    = new AsymmetricOrdering<Interval<Comparable, Object>, Comparable>()
    {
        public int compareAsymmetric(Interval<Comparable, Object> left, Comparable right)
        {
            return left.min.compareTo(right);
        }

        public int compare(Interval<Comparable, Object> i1, Interval<Comparable, Object> i2)
        {
            return i1.min.compareTo(i2.min);
        }
    };

    private static final AsymmetricOrdering<Interval<Comparable, Object>, Comparable> maxOrdering
    = new AsymmetricOrdering<Interval<Comparable, Object>, Comparable>()
    {
        public int compareAsymmetric(Interval<Comparable, Object> left, Comparable right)
        {
            return left.max.compareTo(right);
        }

        public int compare(Interval<Comparable, Object> i1, Interval<Comparable, Object> i2)
        {
            return i1.max.compareTo(i2.max);
        }
    };

    private static final AsymmetricOrdering<Interval<Comparable, Object>, Comparable> reverseMaxOrdering = maxOrdering.reverse();

    public static <C extends Comparable<? super C>, V> AsymmetricOrdering<Interval<C, V>, C> minOrdering()
    {
        return (AsymmetricOrdering) minOrdering;
    }

    public static <C extends Comparable<? super C>, V> AsymmetricOrdering<Interval<C, V>, C> maxOrdering()
    {
        return (AsymmetricOrdering) maxOrdering;
    }
}
