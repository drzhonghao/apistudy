

import com.microsoft.schemas.office.visio.x2012.main.CellType;
import com.microsoft.schemas.office.visio.x2012.main.SectionType;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.xdgf.usermodel.XDGFCell;
import org.apache.poi.xdgf.usermodel.XDGFSheet;
import org.apache.poi.xdgf.util.ObjectFactory;


public abstract class XDGFSection {
	static final ObjectFactory<XDGFSection, SectionType> _sectionTypes;

	static {
		_sectionTypes = new ObjectFactory<>();
	}

	public static XDGFSection load(SectionType section, XDGFSheet containingSheet) {
		return XDGFSection._sectionTypes.load(section.getN(), section, containingSheet);
	}

	protected SectionType _section;

	protected XDGFSheet _containingSheet;

	protected Map<String, XDGFCell> _cells = new HashMap<>();

	public XDGFSection(SectionType section, XDGFSheet containingSheet) {
		_section = section;
		_containingSheet = containingSheet;
		for (CellType cell : section.getCellArray()) {
			_cells.put(cell.getN(), new XDGFCell(cell));
		}
	}

	@org.apache.poi.util.Internal
	public SectionType getXmlObject() {
		return _section;
	}

	@Override
	public String toString() {
		return ((("<Section type=" + (_section.getN())) + " from ") + (_containingSheet)) + ">";
	}

	public abstract void setupMaster(XDGFSection section);
}

