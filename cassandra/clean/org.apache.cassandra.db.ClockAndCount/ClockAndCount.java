import org.apache.cassandra.db.*;


import com.google.common.base.Objects;

import org.apache.cassandra.cache.IMeasurableMemory;
import org.apache.cassandra.utils.ObjectSizes;

public class ClockAndCount implements IMeasurableMemory
{

    private static final long EMPTY_SIZE = ObjectSizes.measure(new ClockAndCount(0, 0));

    public static ClockAndCount BLANK = ClockAndCount.create(0L, 0L);

    public final long clock;
    public final long count;

    private ClockAndCount(long clock, long count)
    {
        this.clock = clock;
        this.count = count;
    }

    public static ClockAndCount create(long clock, long count)
    {
        return new ClockAndCount(clock, count);
    }

    public long unsharedHeapSize()
    {
        return EMPTY_SIZE;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof ClockAndCount))
            return false;

        ClockAndCount other = (ClockAndCount) o;
        return clock == other.clock && count == other.count;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(clock, count);
    }

    @Override
    public String toString()
    {
        return String.format("ClockAndCount(%s,%s)", clock, count);
    }
}
