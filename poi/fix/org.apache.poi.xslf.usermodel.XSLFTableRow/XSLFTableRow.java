

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTable;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableCol;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableGrid;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableRow;


public class XSLFTableRow implements Iterable<XSLFTableCell> {
	private final CTTableRow _row;

	private final List<XSLFTableCell> _cells;

	private final XSLFTable _table;

	XSLFTableRow(CTTableRow row, XSLFTable table) {
		_row = row;
		_table = table;
		@SuppressWarnings("deprecation")
		CTTableCell[] tcArray = _row.getTcArray();
		_cells = new ArrayList<>(tcArray.length);
		for (CTTableCell cell : tcArray) {
		}
	}

	public CTTableRow getXmlObject() {
		return _row;
	}

	public Iterator<XSLFTableCell> iterator() {
		return _cells.iterator();
	}

	public List<XSLFTableCell> getCells() {
		return Collections.unmodifiableList(_cells);
	}

	public double getHeight() {
		return Units.toPoints(_row.getH());
	}

	public void setHeight(double height) {
		_row.setH(Units.toEMU(height));
	}

	public XSLFTableCell addCell() {
		CTTableCell c = _row.addNewTc();
		if ((_table.getNumberOfColumns()) < (_row.sizeOfTcArray())) {
			_table.getCTTable().getTblGrid().addNewGridCol().setW(Units.toEMU(100.0));
		}
		return null;
	}

	@SuppressWarnings("WeakerAccess")
	public void mergeCells(int firstCol, int lastCol) {
		if (firstCol >= lastCol) {
			throw new IllegalArgumentException(((("Cannot merge, first column >= last column : " + firstCol) + " >= ") + lastCol));
		}
		final int colSpan = (lastCol - firstCol) + 1;
		for (final XSLFTableCell cell : _cells.subList((firstCol + 1), (lastCol + 1))) {
		}
	}
}

