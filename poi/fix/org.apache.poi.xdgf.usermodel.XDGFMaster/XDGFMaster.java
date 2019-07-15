

import com.microsoft.schemas.office.visio.x2012.main.MasterType;
import com.microsoft.schemas.office.visio.x2012.main.PageSheetType;
import org.apache.poi.xdgf.usermodel.XDGFDocument;
import org.apache.poi.xdgf.usermodel.XDGFMasterContents;
import org.apache.poi.xdgf.usermodel.XDGFPageSheet;
import org.apache.poi.xdgf.usermodel.XDGFSheet;


public class XDGFMaster {
	private MasterType _master;

	protected XDGFMasterContents _content;

	protected XDGFSheet _pageSheet;

	public XDGFMaster(MasterType master, XDGFMasterContents content, XDGFDocument document) {
		_master = master;
		_content = content;
		if (master.isSetPageSheet())
			_pageSheet = new XDGFPageSheet(master.getPageSheet(), document);

	}

	@org.apache.poi.util.Internal
	protected MasterType getXmlObject() {
		return _master;
	}

	@Override
	public String toString() {
		return ((("<Master ID=\"" + (getID())) + "\" ") + (_content)) + ">";
	}

	public long getID() {
		return _master.getID();
	}

	public String getName() {
		return _master.getName();
	}

	public XDGFMasterContents getContent() {
		return _content;
	}

	public XDGFSheet getPageSheet() {
		return _pageSheet;
	}
}

