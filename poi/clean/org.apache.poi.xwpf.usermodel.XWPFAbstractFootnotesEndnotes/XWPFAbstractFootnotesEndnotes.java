import org.apache.poi.xwpf.usermodel.*;


import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;

/**
 * Base class for the Footnotes and Endnotes part implementations.
 * @since 4.0.0
 */
public abstract class XWPFAbstractFootnotesEndnotes extends POIXMLDocumentPart {

    protected XWPFDocument document;
    protected List<XWPFAbstractFootnoteEndnote> listFootnote = new ArrayList<>();
    private FootnoteEndnoteIdManager idManager;
    
    public XWPFAbstractFootnotesEndnotes(OPCPackage pkg) {
        super(pkg);
    }

    public XWPFAbstractFootnotesEndnotes(OPCPackage pkg,
            String coreDocumentRel) {
        super(pkg, coreDocumentRel);
    }

    public XWPFAbstractFootnotesEndnotes() {
        super();
    }

    public XWPFAbstractFootnotesEndnotes(PackagePart part) {
        super(part);
    }

    public XWPFAbstractFootnotesEndnotes(POIXMLDocumentPart parent, PackagePart part) {
        super(parent, part);
    }


    public XWPFAbstractFootnoteEndnote getFootnoteById(int id) {
        for (XWPFAbstractFootnoteEndnote note : listFootnote) {
            if (note.getCTFtnEdn().getId().intValue() == id)
                return note;
        }
        return null;
    }

    /**
     * @see org.apache.poi.xwpf.usermodel.IBody#getPart()
     */
    public XWPFDocument getXWPFDocument() {
        if (document != null) {
            return document;
        } else {
            return (XWPFDocument) getParent();
        }
    }

    public void setXWPFDocument(XWPFDocument doc) {
        document = doc;
    }

    public void setIdManager(FootnoteEndnoteIdManager footnoteIdManager) {
       this.idManager = footnoteIdManager;
        
    }
    
    public FootnoteEndnoteIdManager getIdManager() {
        return this.idManager;
    }

}
