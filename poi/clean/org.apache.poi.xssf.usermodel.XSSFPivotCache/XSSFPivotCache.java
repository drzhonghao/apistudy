import org.apache.poi.xssf.usermodel.*;


import static org.apache.poi.ooxml.POIXMLTypeLoader.DEFAULT_XML_OPTIONS;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.util.Beta;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPivotCache;

public class XSSFPivotCache extends POIXMLDocumentPart {

    private CTPivotCache ctPivotCache;

    @Beta
    public XSSFPivotCache(){
        super();
        ctPivotCache = CTPivotCache.Factory.newInstance();
    }

    @Beta
    public XSSFPivotCache(CTPivotCache ctPivotCache) {
        super();
        this.ctPivotCache = ctPivotCache;
    }

     /**
     * Creates n XSSFPivotCache representing the given package part and relationship.
     * Should only be called when reading in an existing file.
     *
     * @param part - The package part that holds xml data representing this pivot cache definition.
     * 
     * @since POI 3.14-Beta1
     */
    @Beta
    protected XSSFPivotCache(PackagePart part) throws IOException {
        super(part);
        readFrom(part.getInputStream());
    }
    
    @Beta
    protected void readFrom(InputStream is) throws IOException {
	try {
        XmlOptions options  = new XmlOptions(DEFAULT_XML_OPTIONS);
        //Removing root element
        options.setLoadReplaceDocumentElement(null);
        ctPivotCache = CTPivotCache.Factory.parse(is, options);
        } catch (XmlException e) {
            throw new IOException(e.getLocalizedMessage());
        }
    }

    @Beta
    public CTPivotCache getCTPivotCache() {
        return ctPivotCache;
    }
}
