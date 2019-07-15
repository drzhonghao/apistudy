

import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.RowRecord;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.util.Configurator;
import org.apache.poi.util.LocaleUtil;

import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK;
import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL;
import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_NULL_AND_BLANK;


public final class HSSFRow implements Comparable<HSSFRow> , Row {
	public static final int INITIAL_CAPACITY = Configurator.getIntValue("HSSFRow.ColInitialCapacity", 5);

	private int rowNum;

	private HSSFCell[] cells;

	private final RowRecord row;

	private final HSSFWorkbook book;

	private final HSSFSheet sheet;

	HSSFRow(HSSFWorkbook book, HSSFSheet sheet, int rowNum) {
		this(book, sheet, new RowRecord(rowNum));
	}

	HSSFRow(HSSFWorkbook book, HSSFSheet sheet, RowRecord record) {
		this.book = book;
		this.sheet = sheet;
		row = record;
		setRowNum(record.getRowNumber());
		cells = new HSSFCell[(record.getLastCol()) + (HSSFRow.INITIAL_CAPACITY)];
		record.setEmpty();
	}

	@Override
	public HSSFCell createCell(int column) {
		return this.createCell(column, CellType.BLANK);
	}

	@Override
	public HSSFCell createCell(int columnIndex, CellType type) {
		short shortCellNum = ((short) (columnIndex));
		if (columnIndex > 32767) {
			shortCellNum = ((short) (65535 - columnIndex));
		}
		return null;
	}

	@Override
	public void removeCell(Cell cell) {
		if (cell == null) {
			throw new IllegalArgumentException("cell must not be null");
		}
		removeCell(((HSSFCell) (cell)), true);
	}

	private void removeCell(HSSFCell cell, boolean alsoRemoveRecords) {
		int column = cell.getColumnIndex();
		if (column < 0) {
			throw new RuntimeException("Negative cell indexes not allowed");
		}
		if ((column >= (cells.length)) || (cell != (cells[column]))) {
			throw new RuntimeException("Specified cell is not from this row");
		}
		if (cell.isPartOfArrayFormulaGroup()) {
		}
		cells[column] = null;
		if (alsoRemoveRecords) {
		}
		if (((cell.getColumnIndex()) + 1) == (row.getLastCol())) {
			row.setLastCol(calculateNewLastCellPlusOne(row.getLastCol()));
		}
		if ((cell.getColumnIndex()) == (row.getFirstCol())) {
			row.setFirstCol(calculateNewFirstCell(row.getFirstCol()));
		}
	}

	protected void removeAllCells() {
		for (HSSFCell cell : cells) {
			if (cell != null) {
				removeCell(cell, true);
			}
		}
		cells = new HSSFCell[HSSFRow.INITIAL_CAPACITY];
	}

	HSSFCell createCellFromRecord(CellValueRecordInterface cell) {
		int colIx = cell.getColumn();
		if (row.isEmpty()) {
			row.setFirstCol(colIx);
			row.setLastCol((colIx + 1));
		}else {
			if (colIx < (row.getFirstCol())) {
				row.setFirstCol(colIx);
			}else
				if (colIx > (row.getLastCol())) {
					row.setLastCol((colIx + 1));
				}

		}
		return null;
	}

	@Override
	public void setRowNum(int rowIndex) {
		int maxrow = SpreadsheetVersion.EXCEL97.getLastRowIndex();
		if ((rowIndex < 0) || (rowIndex > maxrow)) {
			throw new IllegalArgumentException((((("Invalid row number (" + rowIndex) + ") outside allowable range (0..") + maxrow) + ")"));
		}
		rowNum = rowIndex;
		if ((row) != null) {
			row.setRowNumber(rowIndex);
		}
	}

	@Override
	public int getRowNum() {
		return rowNum;
	}

	@Override
	public HSSFSheet getSheet() {
		return sheet;
	}

	@Override
	public int getOutlineLevel() {
		return row.getOutlineLevel();
	}

	public void moveCell(HSSFCell cell, short newColumn) {
		if (((cells.length) > newColumn) && ((cells[newColumn]) != null)) {
			throw new IllegalArgumentException((("Asked to move cell to column " + newColumn) + " but there's already a cell there"));
		}
		if (!(cells[cell.getColumnIndex()].equals(cell))) {
			throw new IllegalArgumentException("Asked to move a cell, but it didn't belong to our row");
		}
		removeCell(cell, false);
		addCell(cell);
	}

	private void addCell(HSSFCell cell) {
		int column = cell.getColumnIndex();
		if (column >= (cells.length)) {
			HSSFCell[] oldCells = cells;
			int newSize = (((oldCells.length) * 3) / 2) + 1;
			if (newSize < (column + 1)) {
				newSize = column + (HSSFRow.INITIAL_CAPACITY);
			}
			cells = new HSSFCell[newSize];
			System.arraycopy(oldCells, 0, cells, 0, oldCells.length);
		}
		cells[column] = cell;
		if ((row.isEmpty()) || (column < (row.getFirstCol()))) {
			row.setFirstCol(((short) (column)));
		}
		if ((row.isEmpty()) || (column >= (row.getLastCol()))) {
			row.setLastCol(((short) (column + 1)));
		}
	}

	private HSSFCell retrieveCell(int cellIndex) {
		if ((cellIndex < 0) || (cellIndex >= (cells.length))) {
			return null;
		}
		return cells[cellIndex];
	}

	@Override
	public HSSFCell getCell(int cellnum) {
		return getCell(cellnum, book.getMissingCellPolicy());
	}

	@Override
	public HSSFCell getCell(int cellnum, Row.MissingCellPolicy policy) {
		HSSFCell cell = retrieveCell(cellnum);
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
		if (row.isEmpty()) {
			return -1;
		}
		return ((short) (row.getFirstCol()));
	}

	@Override
	public short getLastCellNum() {
		if (row.isEmpty()) {
			return -1;
		}
		return ((short) (row.getLastCol()));
	}

	@Override
	public int getPhysicalNumberOfCells() {
		int count = 0;
		for (HSSFCell cell : cells) {
			if (cell != null)
				count++;

		}
		return count;
	}

	@Override
	public void setHeight(short height) {
		if (height == (-1)) {
			row.setHeight(((short) (255 | 32768)));
			row.setBadFontHeight(false);
		}else {
			row.setBadFontHeight(true);
			row.setHeight(height);
		}
	}

	@Override
	public void setZeroHeight(boolean zHeight) {
		row.setZeroHeight(zHeight);
	}

	@Override
	public boolean getZeroHeight() {
		return row.getZeroHeight();
	}

	@Override
	public void setHeightInPoints(float height) {
		if (height == (-1)) {
			row.setHeight(((short) (255 | 32768)));
			row.setBadFontHeight(false);
		}else {
			row.setBadFontHeight(true);
			row.setHeight(((short) (height * 20)));
		}
	}

	@Override
	public short getHeight() {
		short height = row.getHeight();
		if ((height & 32768) != 0) {
		}else
			height &= 32767;

		return height;
	}

	@Override
	public float getHeightInPoints() {
		return ((float) (getHeight())) / 20;
	}

	protected RowRecord getRowRecord() {
		return row;
	}

	private int calculateNewLastCellPlusOne(int lastcell) {
		int cellIx = lastcell - 1;
		HSSFCell r = retrieveCell(cellIx);
		while (r == null) {
			if (cellIx < 0) {
				return 0;
			}
			r = retrieveCell((--cellIx));
		} 
		return cellIx + 1;
	}

	private int calculateNewFirstCell(int firstcell) {
		int cellIx = firstcell + 1;
		HSSFCell r = retrieveCell(cellIx);
		while (r == null) {
			if (cellIx <= (cells.length)) {
				return 0;
			}
			r = retrieveCell((++cellIx));
		} 
		return cellIx;
	}

	@Override
	public boolean isFormatted() {
		return row.getFormatted();
	}

	@Override
	public HSSFCellStyle getRowStyle() {
		if (!(isFormatted())) {
			return null;
		}
		short styleIndex = row.getXFIndex();
		return null;
	}

	public void setRowStyle(HSSFCellStyle style) {
		row.setFormatted(true);
		row.setXFIndex(style.getIndex());
	}

	@Override
	public void setRowStyle(CellStyle style) {
		setRowStyle(((HSSFCellStyle) (style)));
	}

	@Override
	public Iterator<Cell> cellIterator() {
		return new HSSFRow.CellIterator();
	}

	@Override
	public Iterator<Cell> iterator() {
		return cellIterator();
	}

	private class CellIterator implements Iterator<Cell> {
		int thisId = -1;

		int nextId = -1;

		public CellIterator() {
			findNext();
		}

		@Override
		public boolean hasNext() {
			return (nextId) < (cells.length);
		}

		@Override
		public Cell next() {
			if (!(hasNext()))
				throw new NoSuchElementException("At last element");

			HSSFCell cell = cells[nextId];
			thisId = nextId;
			findNext();
			return cell;
		}

		@Override
		public void remove() {
			if ((thisId) == (-1))
				throw new IllegalStateException("remove() called before next()");

			cells[thisId] = null;
		}

		private void findNext() {
			int i = (nextId) + 1;
			for (; i < (cells.length); i++) {
				if ((cells[i]) != null)
					break;

			}
			nextId = i;
		}
	}

	@Override
	public int compareTo(HSSFRow other) {
		if ((this.getSheet()) != (other.getSheet())) {
			throw new IllegalArgumentException("The compared rows must belong to the same sheet");
		}
		Integer thisRow = this.getRowNum();
		Integer otherRow = other.getRowNum();
		return thisRow.compareTo(otherRow);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof HSSFRow)) {
			return false;
		}
		HSSFRow other = ((HSSFRow) (obj));
		return ((this.getRowNum()) == (other.getRowNum())) && ((this.getSheet()) == (other.getSheet()));
	}

	@Override
	public int hashCode() {
		return row.hashCode();
	}

	@Override
	public void shiftCellsRight(int firstShiftColumnIndex, int lastShiftColumnIndex, int step) {
		if (step < 0)
			throw new IllegalArgumentException("Shifting step may not be negative ");

		if (firstShiftColumnIndex > lastShiftColumnIndex)
			throw new IllegalArgumentException(String.format(LocaleUtil.getUserLocale(), "Incorrect shifting range : %d-%d", firstShiftColumnIndex, lastShiftColumnIndex));

		if (((lastShiftColumnIndex + step) + 1) > (cells.length))
			extend(((lastShiftColumnIndex + step) + 1));

		for (int columnIndex = lastShiftColumnIndex; columnIndex >= firstShiftColumnIndex; columnIndex--) {
			HSSFCell cell = getCell(columnIndex);
			cells[(columnIndex + step)] = null;
			if (cell != null)
				moveCell(cell, ((short) (columnIndex + step)));

		}
		for (int columnIndex = firstShiftColumnIndex; columnIndex <= ((firstShiftColumnIndex + step) - 1); columnIndex++)
			cells[columnIndex] = null;

	}

	private void extend(int newLenght) {
		HSSFCell[] temp = cells.clone();
		cells = new HSSFCell[newLenght];
		System.arraycopy(temp, 0, cells, 0, temp.length);
	}

	@Override
	public void shiftCellsLeft(int firstShiftColumnIndex, int lastShiftColumnIndex, int step) {
		if (step < 0)
			throw new IllegalArgumentException("Shifting step may not be negative ");

		if (firstShiftColumnIndex > lastShiftColumnIndex)
			throw new IllegalArgumentException(String.format(LocaleUtil.getUserLocale(), "Incorrect shifting range : %d-%d", firstShiftColumnIndex, lastShiftColumnIndex));

		if ((firstShiftColumnIndex - step) < 0)
			throw new IllegalStateException(("Column index less than zero : " + (Integer.valueOf((firstShiftColumnIndex + step)).toString())));

		for (int columnIndex = firstShiftColumnIndex; columnIndex <= lastShiftColumnIndex; columnIndex++) {
			HSSFCell cell = getCell(columnIndex);
			if (cell != null) {
				cells[(columnIndex - step)] = null;
				moveCell(cell, ((short) (columnIndex - step)));
			}else
				cells[(columnIndex - step)] = null;

		}
		for (int columnIndex = (lastShiftColumnIndex - step) + 1; columnIndex <= lastShiftColumnIndex; columnIndex++)
			cells[columnIndex] = null;

	}
}

