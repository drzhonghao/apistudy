import org.apache.cassandra.index.sasi.memory.*;


import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.index.sasi.conf.ColumnIndex;
import org.apache.cassandra.index.sasi.disk.Token;
import org.apache.cassandra.index.sasi.plan.Expression;
import org.apache.cassandra.index.sasi.utils.RangeUnionIterator;
import org.apache.cassandra.index.sasi.utils.RangeIterator;
import org.apache.cassandra.db.marshal.AbstractType;

public class SkipListMemIndex extends MemIndex
{
    public static final int CSLM_OVERHEAD = 128; // average overhead of CSLM

    private final ConcurrentSkipListMap<ByteBuffer, ConcurrentSkipListSet<DecoratedKey>> index;

    public SkipListMemIndex(AbstractType<?> keyValidator, ColumnIndex columnIndex)
    {
        super(keyValidator, columnIndex);
        index = new ConcurrentSkipListMap<>(columnIndex.getValidator());
    }

    public long add(DecoratedKey key, ByteBuffer value)
    {
        long overhead = CSLM_OVERHEAD; // DKs are shared
        ConcurrentSkipListSet<DecoratedKey> keys = index.get(value);

        if (keys == null)
        {
            ConcurrentSkipListSet<DecoratedKey> newKeys = new ConcurrentSkipListSet<>(DecoratedKey.comparator);
            keys = index.putIfAbsent(value, newKeys);
            if (keys == null)
            {
                overhead += CSLM_OVERHEAD + value.remaining();
                keys = newKeys;
            }
        }

        keys.add(key);

        return overhead;
    }

    public RangeIterator<Long, Token> search(Expression expression)
    {
        ByteBuffer min = expression.lower == null ? null : expression.lower.value;
        ByteBuffer max = expression.upper == null ? null : expression.upper.value;

        SortedMap<ByteBuffer, ConcurrentSkipListSet<DecoratedKey>> search;

        if (min == null && max == null)
        {
            throw new IllegalArgumentException();
        }
        if (min != null && max != null)
        {
            search = index.subMap(min, expression.lower.inclusive, max, expression.upper.inclusive);
        }
        else if (min == null)
        {
            search = index.headMap(max, expression.upper.inclusive);
        }
        else
        {
            search = index.tailMap(min, expression.lower.inclusive);
        }

        RangeUnionIterator.Builder<Long, Token> builder = RangeUnionIterator.builder();
        search.values().stream()
                       .filter(keys -> !keys.isEmpty())
                       .forEach(keys -> builder.add(new KeyRangeIterator(keys)));

        return builder.build();
    }
}
