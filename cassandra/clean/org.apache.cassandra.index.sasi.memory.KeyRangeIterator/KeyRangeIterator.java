import org.apache.cassandra.index.sasi.memory.*;


import java.io.IOException;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.index.sasi.disk.Token;
import org.apache.cassandra.index.sasi.utils.AbstractIterator;
import org.apache.cassandra.index.sasi.utils.CombinedValue;
import org.apache.cassandra.index.sasi.utils.RangeIterator;

import com.carrotsearch.hppc.LongOpenHashSet;
import com.carrotsearch.hppc.LongSet;
import com.google.common.collect.PeekingIterator;

public class KeyRangeIterator extends RangeIterator<Long, Token>
{
    private final DKIterator iterator;

    public KeyRangeIterator(ConcurrentSkipListSet<DecoratedKey> keys)
    {
        super((Long) keys.first().getToken().getTokenValue(), (Long) keys.last().getToken().getTokenValue(), keys.size());
        this.iterator = new DKIterator(keys.iterator());
    }

    protected Token computeNext()
    {
        return iterator.hasNext() ? new DKToken(iterator.next()) : endOfData();
    }

    protected void performSkipTo(Long nextToken)
    {
        while (iterator.hasNext())
        {
            DecoratedKey key = iterator.peek();
            if (Long.compare((long) key.getToken().getTokenValue(), nextToken) >= 0)
                break;

            // consume smaller key
            iterator.next();
        }
    }

    public void close() throws IOException
    {}

    private static class DKIterator extends AbstractIterator<DecoratedKey> implements PeekingIterator<DecoratedKey>
    {
        private final Iterator<DecoratedKey> keys;

        public DKIterator(Iterator<DecoratedKey> keys)
        {
            this.keys = keys;
        }

        protected DecoratedKey computeNext()
        {
            return keys.hasNext() ? keys.next() : endOfData();
        }
    }

    private static class DKToken extends Token
    {
        private final SortedSet<DecoratedKey> keys;

        public DKToken(final DecoratedKey key)
        {
            super((long) key.getToken().getTokenValue());

            keys = new TreeSet<DecoratedKey>(DecoratedKey.comparator)
            {{
                add(key);
            }};
        }

        public LongSet getOffsets()
        {
            LongSet offsets = new LongOpenHashSet(4);
            for (DecoratedKey key : keys)
                offsets.add((long) key.getToken().getTokenValue());

            return offsets;
        }

        public void merge(CombinedValue<Long> other)
        {
            if (!(other instanceof Token))
                return;

            Token o = (Token) other;
            assert o.get().equals(token);

            if (o instanceof DKToken)
            {
                keys.addAll(((DKToken) o).keys);
            }
            else
            {
                for (DecoratedKey key : o)
                    keys.add(key);
            }
        }

        public Iterator<DecoratedKey> iterator()
        {
            return keys.iterator();
        }
    }
}
