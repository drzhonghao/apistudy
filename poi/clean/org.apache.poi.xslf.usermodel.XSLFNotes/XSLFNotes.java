import org.apache.poi.xslf.usermodel.*;


import static org.apache.poi.ooxml.POIXMLTypeLoader.DEFAULT_XML_OPTIONS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.sl.usermodel.Notes;
import org.apache.poi.util.Beta;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.presentationml.x2006.main.CTCommonSlideData;
import org.openxmlformats.schemas.presentationml.x2006.main.CTNotesSlide;
import org.openxmlformats.schemas.presentationml.x2006.main.NotesDocument;

@Beta
public final class XSLFNotes extends XSLFSheet
implements Notes<XSLFShape,XSLFTextParagraph> {
   private CTNotesSlide _notes;

    /**
     * Create a new notes
     */
    XSLFNotes() {
        super();
        _notes = prototype();
    }

    /**
     * Construct a SpreadsheetML notes from a package part
     *
     * @param part the package part holding the notes data,
     * the content type must be <code>application/vnd.openxmlformats-officedocument.notes+xml</code>
     *
     * @since POI 3.14-Beta1
     */
    XSLFNotes(PackagePart part) throws IOException, XmlException {
        super(part);

        NotesDocument doc =
            NotesDocument.Factory.parse(getPackagePart().getInputStream(), DEFAULT_XML_OPTIONS);
        _notes = doc.getNotes();
    }

    private static CTNotesSlide prototype(){
        CTNotesSlide ctNotes = CTNotesSlide.Factory.newInstance();
        CTCommonSlideData cSld = ctNotes.addNewCSld();
        cSld.addNewSpTree();

        return ctNotes;
    }

    @Override
    public CTNotesSlide getXmlObject() {
       return _notes;
    }

    @Override
    protected String getRootElementName(){
        return "notes";
    }

    @Override
    public XSLFTheme getTheme(){
        final XSLFNotesMaster m = getMasterSheet();
    	return (m != null) ? m.getTheme() : null;
    }

    @Override
    public XSLFNotesMaster getMasterSheet() {
        for (POIXMLDocumentPart p : getRelations()) {
           if (p instanceof XSLFNotesMaster){
              return (XSLFNotesMaster)p;
           }
        }
        return null;
    }

    @Override
    public List<List<XSLFTextParagraph>> getTextParagraphs() {
        List<List<XSLFTextParagraph>> tp = new ArrayList<>();
        for (XSLFShape sh : super.getShapes()) {
            if (sh instanceof XSLFTextShape) {
                XSLFTextShape txt = (XSLFTextShape)sh;
                tp.add(txt.getTextParagraphs());
            }
        }
        return tp;
    }
}
