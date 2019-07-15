import org.apache.cassandra.cache.IRowCacheEntry;
import org.apache.cassandra.cache.*;


import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Objects;

/**
 * A sentinel object for row caches.  See comments to getThroughCache and CASSANDRA-3862.
 */
public class RowCacheSentinel implements IRowCacheEntry
{
    private static final AtomicLong generator = new AtomicLong();

    final long sentinelId;

    public RowCacheSentinel()
    {
        sentinelId = generator.getAndIncrement();
    }

    RowCacheSentinel(long sentinelId)
    {
        this.sentinelId = sentinelId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof RowCacheSentinel)) return false;

        RowCacheSentinel other = (RowCacheSentinel) o;
        return this.sentinelId == other.sentinelId;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(sentinelId);
    }
}
