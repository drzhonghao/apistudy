

import com.microsoft.schemas.office.visio.x2012.main.VisioDocumentDocument1;
import com.microsoft.schemas.office.visio.x2012.main.VisioDocumentType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.util.PackageHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
import org.apache.poi.xdgf.usermodel.XDGFDocument;
import org.apache.poi.xdgf.usermodel.XDGFFactory;
import org.apache.poi.xdgf.usermodel.XDGFMasters;
import org.apache.poi.xdgf.usermodel.XDGFPage;
import org.apache.poi.xdgf.usermodel.XDGFPages;
import org.apache.poi.xdgf.usermodel.XDGFStyleSheet;
import org.apache.xmlbeans.XmlException;

import static com.microsoft.schemas.office.visio.x2012.main.VisioDocumentDocument1.Factory.parse;


public class XmlVisioDocument extends POIXMLDocument {
	protected XDGFPages _pages;

	protected XDGFMasters _masters;

	protected XDGFDocument _document;

	public XmlVisioDocument(OPCPackage pkg) throws IOException {
		super(pkg, PackageRelationshipTypes.VISIO_CORE_DOCUMENT);
		VisioDocumentType document;
		try {
			document = parse(getPackagePart().getInputStream()).getVisioDocument();
		} catch (XmlException | IOException e) {
			throw new POIXMLException(e);
		}
		_document = new XDGFDocument(document);
		load(new XDGFFactory(_document));
	}

	public XmlVisioDocument(InputStream is) throws IOException {
		this(PackageHelper.open(is));
	}

	@Override
	protected void onDocumentRead() {
		for (POIXMLDocumentPart part : getRelations()) {
			if (part instanceof XDGFPages)
				_pages = ((XDGFPages) (part));
			else
				if (part instanceof XDGFMasters)
					_masters = ((XDGFMasters) (part));


		}
	}

	@Override
	public List<PackagePart> getAllEmbeddedParts() {
		return new ArrayList<>();
	}

	public Collection<XDGFPage> getPages() {
		return _pages.getPageList();
	}

	public XDGFStyleSheet getStyleById(long id) {
		return _document.getStyleById(id);
	}
}

