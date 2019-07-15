import org.apache.poi.xdgf.exceptions.*;


import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;

public class XDGFException {

    /**
     * Creates an error message to be thrown
     */
    public static POIXMLException error(String message, Object o) {
        return new POIXMLException(o + ": " + message);
    }

    public static POIXMLException error(String message, Object o, Throwable t) {
        return new POIXMLException(o + ": " + message, t);
    }

    //
    // Use these to wrap error messages coming up so that we have at least
    // some idea where the error was located
    //

    public static POIXMLException wrap(POIXMLDocumentPart part,
            POIXMLException e) {
        return new POIXMLException(part.getPackagePart().getPartName()
                + ": " + e.getMessage(), e.getCause() == null ? e
                        : e.getCause());
    }

    public static POIXMLException wrap(String where, POIXMLException e) {
        return new POIXMLException(where + ": " + e.getMessage(),
                e.getCause() == null ? e : e.getCause());
    }
}
