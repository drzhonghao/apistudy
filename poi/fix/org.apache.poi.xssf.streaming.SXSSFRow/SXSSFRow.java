

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK;
import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL;
import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_NULL_AND_BLANK;


public class SXSSFRow implements Comparable<SXSSFRow> , Row {
	private static final Boolean UNDEFINED = null;

	private final SXSSFSheet _sheet;

	private final SortedMap<Integer, SXSSFCell> _cells = new TreeMap<>();

	private short _style = -1;

	private short _height = -1;

	private boolean _zHeight;

	private int _outlineLevel;

	private Boolean _hidden = SXSSFRow.UNDEFINED;

	private Boolean _collapsed = SXSSFRow.UNDEFINED;

	public SXSSFRow(SXSSFSheet sheet) {
		_sheet = sheet;
	}

	public Iterator<Cell> allCellsIterator() {
		return new SXSSFRow.CellIterator();
	}

	public boolean hasCustomHeight() {
		return (_height) != (-1);
	}

	@Override
	public int getOutlineLevel() {
		return _outlineLevel;
	}

	void setOutlineLevel(int level) {
		_outlineLevel = level;
	}

	public Boolean getHidden() {
		return _hidden;
	}

	public void setHidden(Boolean hidden) {
		this._hidden = hidden;
	}

	public Boolean getCollapsed() {
		return _collapsed;
	}

	public void setCollapsed(Boolean collapsed) {
		this._collapsed = collapsed;
	}

	@Override
	public Iterator<Cell> iterator() {
		return new SXSSFRow.FilledCellIterator();
	}

	@Override
	public SXSSFCell createCell(int column) {
		return createCell(column, CellType.BLANK);
	}

	@Override
	public SXSSFCell createCell(int column, CellType type) {
		SXSSFRow.checkBounds(column);
		return null;
	}

	private static void checkBounds(int cellIndex) {
		SpreadsheetVersion v = SpreadsheetVersion.EXCEL2007;
		int maxcol = SpreadsheetVersion.EXCEL2007.getLastColumnIndex();
		if ((cellIndex < 0) || (cellIndex > maxcol)) {
			throw new IllegalArgumentException((((((((("Invalid column index (" + cellIndex) + ").  Allowable column range for ") + (v.name())) + " is (0..") + maxcol) + ") or ('A'..'") + (v.getLastColumnName())) + "')"));
		}
	}

	@Override
	public void removeCell(Cell cell) {
		int index = getCellIndex(((SXSSFCell) (cell)));
		_cells.remove(index);
	}

	int getCellIndex(SXSSFCell cell) {
		for (Map.Entry<Integer, SXSSFCell> entry : _cells.entrySet()) {
			if ((entry.getValue()) == cell) {
				return entry.getKey();
			}
		}
		return -1;
	}

	@Override
	public void setRowNum(int rowNum) {
	}

	@Override
	public int getRowNum() {
		return 0;
	}

	@Override
	public SXSSFCell getCell(int cellnum) {
		Row.MissingCellPolicy policy = _sheet.getWorkbook().getMissingCellPolicy();
		return getCell(cellnum, policy);
	}

	@Override
	public SXSSFCell getCell(int cellnum, Row.MissingCellPolicy policy) {
		SXSSFRow.checkBounds(cellnum);
		final SXSSFCell cell = _cells.get(cellnum);
		switch (policy) {
			case RETURN_NULL_AND_BLANK :
				return cell;
			case RETURN_BLANK_AS_NULL :
				boolean isBlank = (cell != null) && ((cell.getCellType()) == (CellType.BLANK));
				return isBlank ? null : cell;
			case CREATE_NULL_AS_BLANK :
				return cell == null ? createCell(cellnum, CellType.BLANK) : cell;
			default :
				throw new IllegalArgumentException(("Illegal policy " + policy));
		}
	}

	@Override
	public short getFirstCellNum() {
		try {
			return _cells.firstKey().shortValue();
		} catch (final NoSuchElementException e) {
			return -1;
		}
	}

	@Override
	public short getLastCellNum() {
		return _cells.isEmpty() ? -1 : ((short) ((_cells.lastKey()) + 1));
	}

	@Override
	public int getPhysicalNumberOfCells() {
		return _cells.size();
	}

	@Override
	public void setHeight(short height) {
		_height = height;
	}

	@Override
	public void setZeroHeight(boolean zHeight) {
		_zHeight = zHeight;
	}

	@Override
	public boolean getZeroHeight() {
		return _zHeight;
	}

	@Override
	public void setHeightInPoints(float height) {
		if (height == (-1)) {
			_height = -1;
		}else {
			_height = ((short) (height * 20));
		}
	}

	@Override
	public short getHeight() {
		return ((short) ((_height) == (-1) ? (getSheet().getDefaultRowHeightInPoints()) * 20 : _height));
	}

	@Override
	public float getHeightInPoints() {
		return ((float) ((_height) == (-1) ? getSheet().getDefaultRowHeightInPoints() : (_height) / 20.0));
	}

	@Override
	public boolean isFormatted() {
		return (_style) > (-1);
	}

	@Override
	public CellStyle getRowStyle() {
		if (!(isFormatted())) {
			return null;
		}
		return getSheet().getWorkbook().getCellStyleAt(_style);
	}

	@org.apache.poi.util.Internal
	int getRowStyleIndex() {
		return _style;
	}

	@Override
	public void setRowStyle(CellStyle style) {
		if (style == null) {
			_style = -1;
		}else {
			_style = style.getIndex();
		}
	}

	@Override
	public Iterator<Cell> cellIterator() {
		return iterator();
	}

	@Override
	public SXSSFSheet getSheet() {
		return _sheet;
	}

	public class FilledCellIterator implements Iterator<Cell> {
		private final Iterator<SXSSFCell> iter = _cells.values().iterator();

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public Cell next() throws NoSuchElementException {
			return iter.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public class CellIterator implements Iterator<Cell> {
		final int maxColumn = getLastCellNum();

		int pos;

		@Override
		public boolean hasNext() {
			return (pos) < (maxColumn);
		}

		@Override
		public Cell next() throws NoSuchElementException {
			if (hasNext()) {
				return _cells.get(((pos)++));
			}else {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public int compareTo(SXSSFRow other) {
		if ((this.getSheet()) != (other.getSheet())) {
			throw new IllegalArgumentException("The compared rows must belong to the same sheet");
		}
		int thisRow = this.getRowNum();
		int otherRow = other.getRowNum();
		return Integer.compare(thisRow, otherRow);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SXSSFRow)) {
			return false;
		}
		SXSSFRow other = ((SXSSFRow) (obj));
		return ((this.getRowNum()) == (other.getRowNum())) && ((this.getSheet()) == (other.getSheet()));
	}

	@Override
	public int hashCode() {
		return _cells.hashCode();
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void shiftCellsRight(int firstShiftColumnIndex, int lastShiftColumnIndex, int step) {
		throw new NotImplementedException("shiftCellsRight");
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void shiftCellsLeft(int firstShiftColumnIndex, int lastShiftColumnIndex, int step) {
		throw new NotImplementedException("shiftCellsLeft");
	}
}

