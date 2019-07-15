

import com.microsoft.schemas.office.visio.x2012.main.PageSheetType;
import com.microsoft.schemas.office.visio.x2012.main.PageType;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xdgf.geom.Dimension2dDouble;
import org.apache.poi.xdgf.usermodel.XDGFCell;
import org.apache.poi.xdgf.usermodel.XDGFDocument;
import org.apache.poi.xdgf.usermodel.XDGFPageContents;
import org.apache.poi.xdgf.usermodel.XDGFPageSheet;
import org.apache.poi.xdgf.usermodel.XDGFPages;
import org.apache.poi.xdgf.usermodel.XDGFSheet;


public class XDGFPage {
	private PageType _page;

	protected XDGFPageContents _content;

	protected XDGFPages _pages;

	protected XDGFSheet _pageSheet;

	public XDGFPage(PageType page, XDGFPageContents content, XDGFDocument document, XDGFPages pages) {
		_page = page;
		_content = content;
		_pages = pages;
		if (page.isSetPageSheet())
			_pageSheet = new XDGFPageSheet(page.getPageSheet(), document);

	}

	@org.apache.poi.util.Internal
	protected PageType getXmlObject() {
		return _page;
	}

	public long getID() {
		return _page.getID();
	}

	public String getName() {
		return _page.getName();
	}

	public XDGFPageContents getContent() {
		return _content;
	}

	public XDGFSheet getPageSheet() {
		return _pageSheet;
	}

	public long getPageNumber() {
		return (_pages.getPageList().indexOf(this)) + 1;
	}

	public Dimension2dDouble getPageSize() {
		XDGFCell w = _pageSheet.getCell("PageWidth");
		XDGFCell h = _pageSheet.getCell("PageHeight");
		if ((w == null) || (h == null))
			throw new POIXMLException("Cannot determine page size");

		return new Dimension2dDouble(Double.parseDouble(w.getValue()), Double.parseDouble(h.getValue()));
	}

	public Point2D.Double getPageOffset() {
		XDGFCell xoffcell = _pageSheet.getCell("XRulerOrigin");
		XDGFCell yoffcell = _pageSheet.getCell("YRulerOrigin");
		double xoffset = 0;
		double yoffset = 0;
		if (xoffcell != null)
			xoffset = Double.parseDouble(xoffcell.getValue());

		if (yoffcell != null)
			yoffset = Double.parseDouble(yoffcell.getValue());

		return new Point2D.Double(xoffset, yoffset);
	}

	public Rectangle2D getBoundingBox() {
		Dimension2dDouble sz = getPageSize();
		Point2D.Double offset = getPageOffset();
		return new Rectangle2D.Double((-(offset.getX())), (-(offset.getY())), sz.getWidth(), sz.getHeight());
	}
}

