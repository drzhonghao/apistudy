import org.apache.cassandra.tools.*;


import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.exceptions.ConfigurationException;

public final class Util
{
    private Util()
    {
    }

    /**
     * This is used by standalone tools to force static initialization of DatabaseDescriptor, and fail if configuration
     * is bad.
     */
    public static void initDatabaseDescriptor()
    {
        try
        {
            DatabaseDescriptor.toolInitialization();
        }
        catch (Throwable e)
        {
            boolean logStackTrace = !(e instanceof ConfigurationException) || ((ConfigurationException) e).logStackTrace;
            System.out.println("Exception (" + e.getClass().getName() + ") encountered during startup: " + e.getMessage());

            if (logStackTrace)
            {
                e.printStackTrace();
                System.exit(3);
            }
            else
            {
                System.err.println(e.getMessage());
                System.exit(3);
            }
        }
    }
}
