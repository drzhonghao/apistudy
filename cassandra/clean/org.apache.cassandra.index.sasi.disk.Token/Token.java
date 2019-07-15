import org.apache.cassandra.index.sasi.disk.*;


import com.google.common.primitives.Longs;

import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.index.sasi.utils.CombinedValue;

import com.carrotsearch.hppc.LongSet;

public abstract class Token implements CombinedValue<Long>, Iterable<DecoratedKey>
{
    protected final long token;

    public Token(long token)
    {
        this.token = token;
    }

    public Long get()
    {
        return token;
    }

    public abstract LongSet getOffsets();

    public int compareTo(CombinedValue<Long> o)
    {
        return Longs.compare(token, ((Token) o).token);
    }
}
