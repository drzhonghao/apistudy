import org.apache.poi.xslf.usermodel.*;


import static org.apache.poi.ooxml.POIXMLTypeLoader.DEFAULT_XML_OPTIONS;

import java.io.IOException;

import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.util.Beta;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.presentationml.x2006.main.CTCommentAuthor;
import org.openxmlformats.schemas.presentationml.x2006.main.CTCommentAuthorList;
import org.openxmlformats.schemas.presentationml.x2006.main.CmAuthorLstDocument;

@Beta
public class XSLFCommentAuthors extends POIXMLDocumentPart {
    private final CTCommentAuthorList _authors;
    
    /**
     * Create a new set of slide comments
     */
    XSLFCommentAuthors() {
       super();
       CmAuthorLstDocument doc = CmAuthorLstDocument.Factory.newInstance();
       _authors = doc.addNewCmAuthorLst();
    }

    /**
     * Construct a SpreadsheetML slide authors from a package part
     *
     * @param part the package part holding the comment authors data,
     * the content type must be <code>application/vnd.openxmlformats-officedocument.commentAuthors+xml</code>
     * 
     * @since POI 3.14-Beta1
     */
    XSLFCommentAuthors(PackagePart part) throws IOException, XmlException {
        super(part);
        CmAuthorLstDocument doc =
           CmAuthorLstDocument.Factory.parse(getPackagePart().getInputStream(), DEFAULT_XML_OPTIONS);
        _authors = doc.getCmAuthorLst();
    }
    
    public CTCommentAuthorList getCTCommentAuthorsList() {
       return _authors;
    }
    
    public CTCommentAuthor getAuthorById(long id) {
       // TODO Have a map
       for (CTCommentAuthor author : _authors.getCmAuthorArray()) {
          if (author.getId() == id) {
             return author;
          }
       }
       return null;
    }
}
