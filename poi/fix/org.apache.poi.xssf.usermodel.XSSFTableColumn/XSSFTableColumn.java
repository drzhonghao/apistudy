

import org.apache.poi.util.Internal;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.helpers.XSSFXmlColumnPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXmlColumnPr;


public class XSSFTableColumn {
	private final XSSFTable table;

	private final CTTableColumn ctTableColumn;

	private XSSFXmlColumnPr xmlColumnPr;

	@Internal
	protected XSSFTableColumn(XSSFTable table, CTTableColumn ctTableColumn) {
		this.table = table;
		this.ctTableColumn = ctTableColumn;
	}

	public XSSFTable getTable() {
		return table;
	}

	public long getId() {
		return ctTableColumn.getId();
	}

	public void setId(long columnId) {
		ctTableColumn.setId(columnId);
	}

	public String getName() {
		return ctTableColumn.getName();
	}

	public void setName(String columnName) {
		ctTableColumn.setName(columnName);
	}

	public XSSFXmlColumnPr getXmlColumnPr() {
		if ((xmlColumnPr) == null) {
			CTXmlColumnPr ctXmlColumnPr = ctTableColumn.getXmlColumnPr();
			if (ctXmlColumnPr != null) {
			}
		}
		return xmlColumnPr;
	}

	public int getColumnIndex() {
		return table.findColumnIndex(getName());
	}
}

