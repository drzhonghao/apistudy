import org.apache.poi.xdgf.usermodel.*;


import java.io.IOException;

import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xdgf.exceptions.XDGFException;
import org.apache.xmlbeans.XmlException;

import com.microsoft.schemas.office.visio.x2012.main.MasterContentsDocument;

/**
 * Contains the actual contents of the master/stencil
 */
public class XDGFMasterContents extends XDGFBaseContents {

    protected XDGFMaster _master;

    /**
     * @since POI 3.14-Beta1
     */
    public XDGFMasterContents(PackagePart part, XDGFDocument document) {
        super(part, document);
    }
    
    @Override
    protected void onDocumentRead() {

        try {

            try {
                _pageContents = MasterContentsDocument.Factory.parse(getPackagePart().getInputStream()).getMasterContents();
            } catch (XmlException | IOException e) {
                throw new POIXMLException(e);
            }

            super.onDocumentRead();

        } catch (POIXMLException e) {
            throw XDGFException.wrap(this, e);
        }
    }

    public XDGFMaster getMaster() {
        return _master;
    }

    protected void setMaster(XDGFMaster master) {
        _master = master;
    }

}
