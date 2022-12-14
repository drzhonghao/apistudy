import org.apache.poi.util.POILogger;
import org.apache.poi.util.NullLogger;
import org.apache.poi.util.*;


import java.util.HashMap;
import java.util.Map;

/**
 * Provides logging without clients having to mess with
 * configuration/initialization.
 *
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Marc Johnson (mjohnson at apache dot org)
 * @author Nicola Ken Barozzi (nicolaken at apache.org)
 */
@Internal
public final class POILogFactory {
    /**
     * Map of POILogger instances, with classes as keys
     */
    private static final Map<String,POILogger> _loggers = new HashMap<>();

    /**
     * A common instance of NullLogger, as it does nothing
     *  we only need the one
     */
    private static final POILogger _nullLogger = new NullLogger();
    /**
     * The name of the class to use. Initialised the
     *  first time we need it
     */
    static String _loggerClassName;

    /**
     * Construct a POILogFactory.
     */
    private POILogFactory() {}

    /**
     * Get a logger, based on a class name
     *
     * @param theclass the class whose name defines the log
     *
     * @return a POILogger for the specified class
     */
    public static POILogger getLogger(final Class<?> theclass) {
        return getLogger(theclass.getName());
    }

    /**
     * Get a logger, based on a String
     *
     * @param cat the String that defines the log
     *
     * @return a POILogger for the specified class
     */
    public static POILogger getLogger(final String cat) {
        // If we haven't found out what logger to use yet,
        //  then do so now
        // Don't look it up until we're first asked, so
        //  that our users can set the system property
        //  between class loading and first use
        if(_loggerClassName == null) {
        	try {
        		_loggerClassName = System.getProperty("org.apache.poi.util.POILogger");
        	} catch(Exception e) {
                // ignore any exception here
            }

        	// Use the default logger if none specified,
        	//  or none could be fetched
        	if(_loggerClassName == null) {
                _loggerClassName = _nullLogger.getClass().getName();
        	}
        }

        // Short circuit for the null logger, which
        //  ignores all categories
        if(_loggerClassName.equals(_nullLogger.getClass().getName())) {
        	return _nullLogger;
        }


        // Fetch the right logger for them, creating
        //  it if that's required
        POILogger logger = _loggers.get(cat);
        if (logger == null) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends POILogger> loggerClass =
                    (Class<? extends POILogger>) Class.forName(_loggerClassName);
                logger = loggerClass.newInstance();
                logger.initialize(cat);
            } catch(Exception e) {
                // Give up and use the null logger
                logger = _nullLogger;
                _loggerClassName = _nullLogger.getClass().getName();
            }

            // Save for next time
            _loggers.put(cat, logger);
        }
        return logger;
    }
}
