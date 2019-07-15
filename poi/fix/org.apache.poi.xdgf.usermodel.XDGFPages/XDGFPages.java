

import com.microsoft.schemas.office.visio.x2012.main.PageType;
import com.microsoft.schemas.office.visio.x2012.main.PagesDocument;
import com.microsoft.schemas.office.visio.x2012.main.PagesType;
import com.microsoft.schemas.office.visio.x2012.main.RelType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xdgf.exceptions.XDGFException;
import org.apache.poi.xdgf.usermodel.XDGFDocument;
import org.apache.poi.xdgf.usermodel.XDGFPage;
import org.apache.poi.xdgf.usermodel.XDGFPageContents;
import org.apache.poi.xdgf.xml.XDGFXMLDocumentPart;
import org.apache.xmlbeans.XmlException;

import static com.microsoft.schemas.office.visio.x2012.main.PagesDocument.Factory.parse;


public class XDGFPages extends XDGFXMLDocumentPart {
	PagesType _pagesObject;

	List<XDGFPage> _pages = new ArrayList<>();

	public XDGFPages(PackagePart part, XDGFDocument document) {
		super(part, document);
	}

	@org.apache.poi.util.Internal
	PagesType getXmlObject() {
		return _pagesObject;
	}

	@Override
	protected void onDocumentRead() {
		try {
			try {
				_pagesObject = parse(getPackagePart().getInputStream()).getPages();
			} catch (XmlException | IOException e) {
				throw new POIXMLException(e);
			}
			for (PageType pageSettings : _pagesObject.getPageArray()) {
				String relId = pageSettings.getRel().getId();
				POIXMLDocumentPart pageContentsPart = getRelationById(relId);
				if (pageContentsPart == null)
					throw new POIXMLException((("PageSettings relationship for " + relId) + " not found"));

				if (!(pageContentsPart instanceof XDGFPageContents))
					throw new POIXMLException(((("Unexpected pages relationship for " + relId) + ": ") + pageContentsPart));

				XDGFPageContents contents = ((XDGFPageContents) (pageContentsPart));
			}
		} catch (POIXMLException e) {
			throw XDGFException.wrap(this, e);
		}
	}

	public List<XDGFPage> getPageList() {
		return Collections.unmodifiableList(_pages);
	}
}

