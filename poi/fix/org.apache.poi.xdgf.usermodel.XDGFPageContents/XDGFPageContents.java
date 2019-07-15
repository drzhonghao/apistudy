

import com.microsoft.schemas.office.visio.x2012.main.PageContentsDocument;
import com.microsoft.schemas.office.visio.x2012.main.PageContentsType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xdgf.exceptions.XDGFException;
import org.apache.poi.xdgf.usermodel.XDGFBaseContents;
import org.apache.poi.xdgf.usermodel.XDGFDocument;
import org.apache.poi.xdgf.usermodel.XDGFMaster;
import org.apache.poi.xdgf.usermodel.XDGFMasterContents;
import org.apache.poi.xdgf.usermodel.XDGFPage;
import org.apache.poi.xdgf.usermodel.XDGFShape;
import org.apache.xmlbeans.XmlException;

import static com.microsoft.schemas.office.visio.x2012.main.PageContentsDocument.Factory.parse;


public class XDGFPageContents extends XDGFBaseContents {
	protected Map<Long, XDGFMaster> _masters = new HashMap<>();

	protected XDGFPage _page;

	public XDGFPageContents(PackagePart part, XDGFDocument document) {
		super(part, document);
	}

	@Override
	protected void onDocumentRead() {
		try {
			try {
				_pageContents = parse(getPackagePart().getInputStream()).getPageContents();
			} catch (XmlException | IOException e) {
				throw new POIXMLException(e);
			}
			for (POIXMLDocumentPart part : getRelations()) {
				if (!(part instanceof XDGFMasterContents))
					continue;

				XDGFMaster master = ((XDGFMasterContents) (part)).getMaster();
				_masters.put(master.getID(), master);
			}
			super.onDocumentRead();
			for (XDGFShape shape : _shapes.values()) {
			}
		} catch (POIXMLException e) {
			throw XDGFException.wrap(this, e);
		}
	}

	public XDGFPage getPage() {
		return _page;
	}

	protected void setPage(XDGFPage page) {
		_page = page;
	}

	public XDGFMaster getMasterById(long id) {
		return _masters.get(id);
	}
}

