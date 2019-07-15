import org.apache.cassandra.utils.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around time related functions that are either implemented by using the default JVM calls
 * or by using a custom implementation for testing purposes.
 *
 * See {@link #instance} for how to use a custom implementation.
 *
 * Please note that {@link java.time.Clock} wasn't used, as it would not be possible to provide an
 * implementation for {@link #nanoTime()} with the exact same properties of {@link System#nanoTime()}.
 */
public class Clock
{
    private static final Logger logger = LoggerFactory.getLogger(Clock.class);

    /**
     * Static singleton object that will be instanciated by default with a system clock
     * implementation. Set <code>cassandra.clock</code> system property to a FQCN to use a
     * different implementation instead.
     */
    public static Clock instance;

    static
    {
        String sclock = System.getProperty("cassandra.clock");
        if (sclock == null)
        {
            instance = new Clock();
        }
        else
        {
            try
            {
                logger.debug("Using custom clock implementation: {}", sclock);
                instance = (Clock) Class.forName(sclock).newInstance();
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * @see System#nanoTime()
     */
    public long nanoTime()
    {
        return System.nanoTime();
    }

    /**
     * @see System#currentTimeMillis()
     */
    public long currentTimeMillis()
    {
        return System.currentTimeMillis();
    }

}
