import org.apache.poi.util.POILogger;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.*;


import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;


/**
 * Provides handy methods for working with StAX parsers and readers
 */
public final class StaxHelper {
    private static final POILogger logger = POILogFactory.getLogger(StaxHelper.class);

    private StaxHelper() {}

    /**
     * Creates a new StAX XMLInputFactory, with sensible defaults
     */
    public static XMLInputFactory newXMLInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        trySetProperty(factory, XMLInputFactory.IS_NAMESPACE_AWARE, true);
        trySetProperty(factory, XMLInputFactory.IS_VALIDATING, false);
        trySetProperty(factory, XMLInputFactory.SUPPORT_DTD, false);
        trySetProperty(factory, XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory;
    }

    /**
     * Creates a new StAX XMLOutputFactory, with sensible defaults
     */
    public static XMLOutputFactory newXMLOutputFactory() {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        trySetProperty(factory, XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
        return factory;
    }

    /**
     * Creates a new StAX XMLEventFactory, with sensible defaults
     */
    public static XMLEventFactory newXMLEventFactory() {
        // this method seems safer on Android than getFactory()
        return XMLEventFactory.newInstance();
    }
            
    private static void trySetProperty(XMLInputFactory factory, String feature, boolean flag) {
        try {
            factory.setProperty(feature, flag);
        } catch (Exception e) {
            logger.log(POILogger.WARN, "StAX Property unsupported", feature, e);
        } catch (AbstractMethodError ame) {
            logger.log(POILogger.WARN, "Cannot set StAX property because outdated StAX parser in classpath", feature, ame);
        }
    }

    private static void trySetProperty(XMLOutputFactory factory, String feature, boolean flag) {
        try {
            factory.setProperty(feature, flag);
        } catch (Exception e) {
            logger.log(POILogger.WARN, "StAX Property unsupported", feature, e);
        } catch (AbstractMethodError ame) {
            logger.log(POILogger.WARN, "Cannot set StAX property because outdated StAX parser in classpath", feature, ame);
        }
    }
}
