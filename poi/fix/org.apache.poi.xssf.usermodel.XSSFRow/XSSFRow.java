

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressBase;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.helpers.XSSFRowShifter;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRow;

import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK;
import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL;
import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_NULL_AND_BLANK;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell.Factory.newInstance;


public class XSSFRow implements Comparable<XSSFRow> , Row {
	private final CTRow _row;

	private final TreeMap<Integer, XSSFCell> _cells;

	private final XSSFSheet _sheet;

	protected XSSFRow(CTRow row, XSSFSheet sheet) {
		_row = row;
		_sheet = sheet;
		_cells = new TreeMap<>();
		for (CTCell c : row.getCArray()) {
		}
		if (!(row.isSetR())) {
			int nextRowNum = (sheet.getLastRowNum()) + 2;
			if ((nextRowNum == 2) && ((sheet.getPhysicalNumberOfRows()) == 0)) {
				nextRowNum = 1;
			}
			row.setR(nextRowNum);
		}
	}

	@Override
	public XSSFSheet getSheet() {
		return this._sheet;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<Cell> cellIterator() {
		return ((Iterator<Cell>) ((Iterator<? extends Cell>) (_cells.values().iterator())));
	}

	@Override
	public Iterator<Cell> iterator() {
		return cellIterator();
	}

	@Override
	public int compareTo(XSSFRow other) {
		if ((this.getSheet()) != (other.getSheet())) {
			throw new IllegalArgumentException("The compared rows must belong to the same sheet");
		}
		int thisRow = this.getRowNum();
		int otherRow = other.getRowNum();
		return Integer.compare(thisRow, otherRow);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof XSSFRow)) {
			return false;
		}
		XSSFRow other = ((XSSFRow) (obj));
		return ((this.getRowNum()) == (other.getRowNum())) && ((this.getSheet()) == (other.getSheet()));
	}

	@Override
	public int hashCode() {
		return _row.hashCode();
	}

	@Override
	public XSSFCell createCell(int columnIndex) {
		return createCell(columnIndex, CellType.BLANK);
	}

	@Override
	public XSSFCell createCell(int columnIndex, CellType type) {
		final Integer colI = Integer.valueOf(columnIndex);
		CTCell ctCell;
		XSSFCell prev = _cells.get(colI);
		if (prev != null) {
			ctCell = prev.getCTCell();
			ctCell.set(newInstance());
		}else {
			ctCell = _row.addNewC();
		}
		if (type != (CellType.BLANK)) {
		}
		return null;
	}

	@Override
	public XSSFCell getCell(int cellnum) {
		return getCell(cellnum, _sheet.getWorkbook().getMissingCellPolicy());
	}

	@Override
	public XSSFCell getCell(int cellnum, Row.MissingCellPolicy policy) {
		if (cellnum < 0) {
			throw new IllegalArgumentException("Cell index must be >= 0");
		}
		final Integer colI = Integer.valueOf(cellnum);
		XSSFCell cell = _cells.get(colI);
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
		return ((short) ((_cells.size()) == 0 ? -1 : _cells.firstKey()));
	}

	@Override
	public short getLastCellNum() {
		return ((short) ((_cells.size()) == 0 ? -1 : (_cells.lastKey()) + 1));
	}

	@Override
	public short getHeight() {
		return ((short) ((getHeightInPoints()) * 20));
	}

	@Override
	public float getHeightInPoints() {
		if (this._row.isSetHt()) {
			return ((float) (this._row.getHt()));
		}
		return _sheet.getDefaultRowHeightInPoints();
	}

	@Override
	public void setHeight(short height) {
		if (height == (-1)) {
			if (_row.isSetHt()) {
				_row.unsetHt();
			}
			if (_row.isSetCustomHeight()) {
				_row.unsetCustomHeight();
			}
		}else {
			_row.setHt((((double) (height)) / 20));
			_row.setCustomHeight(true);
		}
	}

	@Override
	public void setHeightInPoints(float height) {
		setHeight(((short) (height == (-1) ? -1 : height * 20)));
	}

	@Override
	public int getPhysicalNumberOfCells() {
		return _cells.size();
	}

	@Override
	public int getRowNum() {
		return ((int) ((_row.getR()) - 1));
	}

	@Override
	public void setRowNum(int rowIndex) {
		int maxrow = SpreadsheetVersion.EXCEL2007.getLastRowIndex();
		if ((rowIndex < 0) || (rowIndex > maxrow)) {
			throw new IllegalArgumentException((((("Invalid row number (" + rowIndex) + ") outside allowable range (0..") + maxrow) + ")"));
		}
		_row.setR((rowIndex + 1));
	}

	@Override
	public boolean getZeroHeight() {
		return this._row.getHidden();
	}

	@Override
	public void setZeroHeight(boolean height) {
		this._row.setHidden(height);
	}

	@Override
	public boolean isFormatted() {
		return _row.isSetS();
	}

	@Override
	public XSSFCellStyle getRowStyle() {
		if (!(isFormatted())) {
			return null;
		}
		StylesTable stylesSource = getSheet().getWorkbook().getStylesSource();
		if ((stylesSource.getNumCellStyles()) > 0) {
			return stylesSource.getStyleAt(((int) (_row.getS())));
		}else {
			return null;
		}
	}

	@Override
	public void setRowStyle(CellStyle style) {
		if (style == null) {
			if (_row.isSetS()) {
				_row.unsetS();
				_row.unsetCustomFormat();
			}
		}else {
			StylesTable styleSource = getSheet().getWorkbook().getStylesSource();
			XSSFCellStyle xStyle = ((XSSFCellStyle) (style));
			xStyle.verifyBelongsToStylesSource(styleSource);
			long idx = styleSource.putStyle(xStyle);
			_row.setS(idx);
			_row.setCustomFormat(true);
		}
	}

	@Override
	public void removeCell(Cell cell) {
		if ((cell.getRow()) != (this)) {
			throw new IllegalArgumentException("Specified cell does not belong to this row");
		}
		XSSFCell xcell = ((XSSFCell) (cell));
		if (xcell.isPartOfArrayFormulaGroup()) {
		}
		if ((cell.getCellType()) == (CellType.FORMULA)) {
		}
		final Integer colI = Integer.valueOf(cell.getColumnIndex());
		_cells.remove(colI);
	}

	@org.apache.poi.util.Internal
	public CTRow getCTRow() {
		return _row;
	}

	protected void onDocumentWrite() {
		CTCell[] cArray = new CTCell[_cells.size()];
		int i = 0;
		for (XSSFCell xssfCell : _cells.values()) {
			cArray[i] = ((CTCell) (xssfCell.getCTCell().copy()));
			xssfCell.setCTCell(cArray[i]);
			i++;
		}
		_row.setCArray(cArray);
	}

	@Override
	public String toString() {
		return _row.toString();
	}

	protected void shift(int n) {
		int rownum = (getRowNum()) + n;
		String msg = (("Row[rownum=" + (getRowNum())) + "] contains cell(s) included in a multi-cell array formula. ") + "You cannot change part of an array.";
		for (Cell c : this) {
			((XSSFCell) (c)).updateCellReferencesForShifting(msg);
		}
		setRowNum(rownum);
	}

	@org.apache.poi.util.Beta
	public void copyRowFrom(Row srcRow, CellCopyPolicy policy) {
		if (srcRow == null) {
			for (Cell destCell : this) {
				final XSSFCell srcCell = null;
				((XSSFCell) (destCell)).copyCellFrom(srcCell, policy);
			}
			if (policy.isCopyMergedRegions()) {
				final int destRowNum = getRowNum();
				int index = 0;
				final Set<Integer> indices = new HashSet<>();
				for (CellRangeAddress destRegion : getSheet().getMergedRegions()) {
					if ((destRowNum == (destRegion.getFirstRow())) && (destRowNum == (destRegion.getLastRow()))) {
						indices.add(index);
					}
					index++;
				}
				getSheet().removeMergedRegions(indices);
			}
			if (policy.isCopyRowHeight()) {
				setHeight(((short) (-1)));
			}
		}else {
			for (final Cell c : srcRow) {
				final XSSFCell srcCell = ((XSSFCell) (c));
				final XSSFCell destCell = createCell(srcCell.getColumnIndex(), srcCell.getCellType());
				destCell.copyCellFrom(srcCell, policy);
			}
			final int sheetIndex = _sheet.getWorkbook().getSheetIndex(_sheet);
			final String sheetName = _sheet.getWorkbook().getSheetName(sheetIndex);
			final int srcRowNum = srcRow.getRowNum();
			final int destRowNum = getRowNum();
			final int rowDifference = destRowNum - srcRowNum;
			final FormulaShifter formulaShifter = FormulaShifter.createForRowCopy(sheetIndex, sheetName, srcRowNum, srcRowNum, rowDifference, SpreadsheetVersion.EXCEL2007);
			final XSSFRowShifter rowShifter = new XSSFRowShifter(_sheet);
			if (policy.isCopyMergedRegions()) {
				for (CellRangeAddress srcRegion : srcRow.getSheet().getMergedRegions()) {
					if ((srcRowNum == (srcRegion.getFirstRow())) && (srcRowNum == (srcRegion.getLastRow()))) {
						CellRangeAddress destRegion = srcRegion.copy();
						destRegion.setFirstRow(destRowNum);
						destRegion.setLastRow(destRowNum);
						getSheet().addMergedRegion(destRegion);
					}
				}
			}
			if (policy.isCopyRowHeight()) {
				setHeight(srcRow.getHeight());
			}
		}
	}

	@Override
	public int getOutlineLevel() {
		return _row.getOutlineLevel();
	}

	@Override
	public void shiftCellsRight(int firstShiftColumnIndex, int lastShiftColumnIndex, int step) {
		if (step < 0) {
			throw new IllegalArgumentException("Shifting step may not be negative ");
		}
		if (firstShiftColumnIndex > lastShiftColumnIndex) {
			throw new IllegalArgumentException(String.format(LocaleUtil.getUserLocale(), "Incorrect shifting range : %d-%d", firstShiftColumnIndex, lastShiftColumnIndex));
		}
		for (int columnIndex = lastShiftColumnIndex; columnIndex >= firstShiftColumnIndex; columnIndex--) {
			shiftCell(columnIndex, step);
		}
		for (int columnIndex = firstShiftColumnIndex; columnIndex <= ((firstShiftColumnIndex + step) - 1); columnIndex++) {
			_cells.remove(columnIndex);
			XSSFCell targetCell = getCell(columnIndex);
			if (targetCell != null) {
				targetCell.getCTCell().set(newInstance());
			}
		}
	}

	@Override
	public void shiftCellsLeft(int firstShiftColumnIndex, int lastShiftColumnIndex, int step) {
		if (step < 0) {
			throw new IllegalArgumentException("Shifting step may not be negative ");
		}
		if (firstShiftColumnIndex > lastShiftColumnIndex) {
			throw new IllegalArgumentException(String.format(LocaleUtil.getUserLocale(), "Incorrect shifting range : %d-%d", firstShiftColumnIndex, lastShiftColumnIndex));
		}
		if ((firstShiftColumnIndex - step) < 0) {
			throw new IllegalStateException(("Column index less than zero : " + (Integer.valueOf((firstShiftColumnIndex + step)).toString())));
		}
		for (int columnIndex = firstShiftColumnIndex; columnIndex <= lastShiftColumnIndex; columnIndex++) {
			shiftCell(columnIndex, (-step));
		}
		for (int columnIndex = (lastShiftColumnIndex - step) + 1; columnIndex <= lastShiftColumnIndex; columnIndex++) {
			_cells.remove(columnIndex);
			XSSFCell targetCell = getCell(columnIndex);
			if (targetCell != null) {
				targetCell.getCTCell().set(newInstance());
			}
		}
	}

	private void shiftCell(int columnIndex, int step) {
		if ((columnIndex + step) < 0) {
			throw new IllegalStateException(("Column index less than zero : " + (Integer.valueOf((columnIndex + step)).toString())));
		}
		XSSFCell currentCell = getCell(columnIndex);
		if (currentCell != null) {
			_cells.put((columnIndex + step), currentCell);
		}else {
			_cells.remove((columnIndex + step));
			XSSFCell targetCell = getCell((columnIndex + step));
			if (targetCell != null) {
				targetCell.getCTCell().set(newInstance());
			}
		}
	}
}

