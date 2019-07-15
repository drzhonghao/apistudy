import org.apache.poi.util.POILogger;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.*;


import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Helper methods for working with javax.xml classes.
 */
public final class XMLHelper
{
    private static POILogger logger = POILogFactory.getLogger(XMLHelper.class);
    
    /**
     * Creates a new DocumentBuilderFactory, with sensible defaults
     */
    public static DocumentBuilderFactory getDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        trySetSAXFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        trySetSAXFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        trySetSAXFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        trySetSAXFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        trySetSAXFeature(factory, "http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        return factory;
    }
    
    private static void trySetSAXFeature(DocumentBuilderFactory documentBuilderFactory, String feature, boolean enabled) {
        try {
            documentBuilderFactory.setFeature(feature, enabled);
        } catch (Exception e) {
            logger.log(POILogger.WARN, "SAX Feature unsupported", feature, e);
        } catch (AbstractMethodError ame) {
            logger.log(POILogger.WARN, "Cannot set SAX feature because outdated XML parser in classpath", feature, ame);
        }
    }
    

}
