import org.apache.cassandra.utils.*;


import java.util.concurrent.TimeUnit;

import org.apache.cassandra.concurrent.ScheduledExecutors;
import org.apache.cassandra.config.Config;

/*
 * Convert from nanotime to non-monotonic current time millis. Beware of weaker ordering guarantees.
 */
public class NanoTimeToCurrentTimeMillis
{
    /*
     * How often to pull a new timestamp from the system.
     */
    private static final String TIMESTAMP_UPDATE_INTERVAL_PROPERTY = Config.PROPERTY_PREFIX + "NANOTIMETOMILLIS_TIMESTAMP_UPDATE_INTERVAL";
    private static final long TIMESTAMP_UPDATE_INTERVAL = Long.getLong(TIMESTAMP_UPDATE_INTERVAL_PROPERTY, 10000);

    private static volatile long TIMESTAMP_BASE[] = new long[] { System.currentTimeMillis(), System.nanoTime() };

    /*
     * System.currentTimeMillis() is 25 nanoseconds. This is 2 nanoseconds (maybe) according to JMH.
     * Faster than calling both currentTimeMillis() and nanoTime().
     *
     * There is also the issue of how scalable nanoTime() and currentTimeMillis() are which is a moving target.
     *
     * These timestamps don't order with System.currentTimeMillis() because currentTimeMillis() can tick over
     * before this one does. I have seen it behind by as much as 2ms on Linux and 25ms on Windows.
     */
    public static long convert(long nanoTime)
    {
        final long timestampBase[] = TIMESTAMP_BASE;
        return timestampBase[0] + TimeUnit.NANOSECONDS.toMillis(nanoTime - timestampBase[1]);
    }

    public static void updateNow()
    {
        ScheduledExecutors.scheduledFastTasks.submit(NanoTimeToCurrentTimeMillis::updateTimestampBase);
    }

    static
    {
        ScheduledExecutors.scheduledFastTasks.scheduleWithFixedDelay(NanoTimeToCurrentTimeMillis::updateTimestampBase,
                                                                     TIMESTAMP_UPDATE_INTERVAL,
                                                                     TIMESTAMP_UPDATE_INTERVAL,
                                                                     TimeUnit.MILLISECONDS);
    }

    private static void updateTimestampBase()
    {
        TIMESTAMP_BASE = new long[] {
                                    Math.max(TIMESTAMP_BASE[0], System.currentTimeMillis()),
                                    Math.max(TIMESTAMP_BASE[1], System.nanoTime()) };
    }
}
