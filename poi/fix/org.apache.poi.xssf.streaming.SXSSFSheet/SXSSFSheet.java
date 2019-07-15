

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.AutoFilter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellRange;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.xssf.streaming.SXSSFDrawing;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.streaming.SheetDataWriter;
import org.apache.poi.xssf.usermodel.XSSFAutoFilter;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFPrintSetup;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSheetConditionalFormatting;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetFormatPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor.Factory.newInstance;


public class SXSSFSheet implements Sheet {
	final XSSFSheet _sh;

	private final SXSSFWorkbook _workbook;

	private final TreeMap<Integer, SXSSFRow> _rows = new TreeMap<>();

	private final SheetDataWriter _writer;

	private int _randomAccessWindowSize = SXSSFWorkbook.DEFAULT_WINDOW_SIZE;

	private int outlineLevelRow;

	private int lastFlushedRowNumber = -1;

	private boolean allFlushed;

	public SXSSFSheet(SXSSFWorkbook workbook, XSSFSheet xSheet) throws IOException {
		_workbook = workbook;
		_sh = xSheet;
		setRandomAccessWindowSize(_workbook.getRandomAccessWindowSize());
		_writer = null;
	}

	@org.apache.poi.util.Internal
	SheetDataWriter getSheetDataWriter() {
		return _writer;
	}

	public InputStream getWorksheetXMLInputStream() throws IOException {
		flushRows(0);
		_writer.close();
		return _writer.getWorksheetXMLInputStream();
	}

	@Override
	public Iterator<Row> iterator() {
		return rowIterator();
	}

	@Override
	public SXSSFRow createRow(int rownum) {
		int maxrow = SpreadsheetVersion.EXCEL2007.getLastRowIndex();
		if ((rownum < 0) || (rownum > maxrow)) {
			throw new IllegalArgumentException((((("Invalid row number (" + rownum) + ") outside allowable range (0..") + maxrow) + ")"));
		}
		if (rownum <= (_writer.getLastFlushedRow())) {
			throw new IllegalArgumentException(((((("Attempting to write a row[" + rownum) + "] ") + "in the range [0,") + (_writer.getLastFlushedRow())) + "] that is already written to disk."));
		}
		if (((_sh.getPhysicalNumberOfRows()) > 0) && (rownum <= (_sh.getLastRowNum()))) {
			throw new IllegalArgumentException(((((("Attempting to write a row[" + rownum) + "] ") + "in the range [0,") + (_sh.getLastRowNum())) + "] that is already written to disk."));
		}
		allFlushed = false;
		if (((_randomAccessWindowSize) >= 0) && ((_rows.size()) > (_randomAccessWindowSize))) {
			try {
				flushRows(_randomAccessWindowSize);
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
		return null;
	}

	@Override
	public void removeRow(Row row) {
		if ((row.getSheet()) != (this)) {
			throw new IllegalArgumentException("Specified row does not belong to this sheet");
		}
		for (Iterator<Map.Entry<Integer, SXSSFRow>> iter = _rows.entrySet().iterator(); iter.hasNext();) {
			Map.Entry<Integer, SXSSFRow> entry = iter.next();
			if ((entry.getValue()) == row) {
				iter.remove();
				return;
			}
		}
	}

	@Override
	public SXSSFRow getRow(int rownum) {
		return _rows.get(rownum);
	}

	@Override
	public int getPhysicalNumberOfRows() {
		return (_rows.size()) + (_writer.getNumberOfFlushedRows());
	}

	@Override
	public int getFirstRowNum() {
		if ((_writer.getNumberOfFlushedRows()) > 0) {
			return _writer.getLowestIndexOfFlushedRows();
		}
		return (_rows.size()) == 0 ? 0 : _rows.firstKey();
	}

	@Override
	public int getLastRowNum() {
		return (_rows.size()) == 0 ? 0 : _rows.lastKey();
	}

	@Override
	public void setColumnHidden(int columnIndex, boolean hidden) {
		_sh.setColumnHidden(columnIndex, hidden);
	}

	@Override
	public boolean isColumnHidden(int columnIndex) {
		return _sh.isColumnHidden(columnIndex);
	}

	@Override
	public void setColumnWidth(int columnIndex, int width) {
		_sh.setColumnWidth(columnIndex, width);
	}

	@Override
	public int getColumnWidth(int columnIndex) {
		return _sh.getColumnWidth(columnIndex);
	}

	@Override
	public float getColumnWidthInPixels(int columnIndex) {
		return _sh.getColumnWidthInPixels(columnIndex);
	}

	@Override
	public void setDefaultColumnWidth(int width) {
		_sh.setDefaultColumnWidth(width);
	}

	@Override
	public int getDefaultColumnWidth() {
		return _sh.getDefaultColumnWidth();
	}

	@Override
	public short getDefaultRowHeight() {
		return _sh.getDefaultRowHeight();
	}

	@Override
	public float getDefaultRowHeightInPoints() {
		return _sh.getDefaultRowHeightInPoints();
	}

	@Override
	public void setDefaultRowHeight(short height) {
		_sh.setDefaultRowHeight(height);
	}

	@Override
	public void setDefaultRowHeightInPoints(float height) {
		_sh.setDefaultRowHeightInPoints(height);
	}

	@Override
	public CellStyle getColumnStyle(int column) {
		return _sh.getColumnStyle(column);
	}

	@Override
	public int addMergedRegion(CellRangeAddress region) {
		return _sh.addMergedRegion(region);
	}

	@Override
	public int addMergedRegionUnsafe(CellRangeAddress region) {
		return _sh.addMergedRegionUnsafe(region);
	}

	@Override
	public void validateMergedRegions() {
		_sh.validateMergedRegions();
	}

	@Override
	public void setVerticallyCenter(boolean value) {
		_sh.setVerticallyCenter(value);
	}

	@Override
	public void setHorizontallyCenter(boolean value) {
		_sh.setHorizontallyCenter(value);
	}

	@Override
	public boolean getHorizontallyCenter() {
		return _sh.getHorizontallyCenter();
	}

	@Override
	public boolean getVerticallyCenter() {
		return _sh.getVerticallyCenter();
	}

	@Override
	public void removeMergedRegion(int index) {
		_sh.removeMergedRegion(index);
	}

	@Override
	public void removeMergedRegions(Collection<Integer> indices) {
		_sh.removeMergedRegions(indices);
	}

	@Override
	public int getNumMergedRegions() {
		return _sh.getNumMergedRegions();
	}

	@Override
	public CellRangeAddress getMergedRegion(int index) {
		return _sh.getMergedRegion(index);
	}

	@Override
	public List<CellRangeAddress> getMergedRegions() {
		return _sh.getMergedRegions();
	}

	@Override
	public Iterator<Row> rowIterator() {
		@SuppressWarnings("unchecked")
		Iterator<Row> result = ((Iterator<Row>) ((Iterator<? extends Row>) (_rows.values().iterator())));
		return result;
	}

	@Override
	public void setAutobreaks(boolean value) {
		_sh.setAutobreaks(value);
	}

	@Override
	public void setDisplayGuts(boolean value) {
		_sh.setDisplayGuts(value);
	}

	@Override
	public void setDisplayZeros(boolean value) {
		_sh.setDisplayZeros(value);
	}

	@Override
	public boolean isDisplayZeros() {
		return _sh.isDisplayZeros();
	}

	@Override
	public void setRightToLeft(boolean value) {
		_sh.setRightToLeft(value);
	}

	@Override
	public boolean isRightToLeft() {
		return _sh.isRightToLeft();
	}

	@Override
	public void setFitToPage(boolean value) {
		_sh.setFitToPage(value);
	}

	@Override
	public void setRowSumsBelow(boolean value) {
		_sh.setRowSumsBelow(value);
	}

	@Override
	public void setRowSumsRight(boolean value) {
		_sh.setRowSumsRight(value);
	}

	@Override
	public boolean getAutobreaks() {
		return _sh.getAutobreaks();
	}

	@Override
	public boolean getDisplayGuts() {
		return _sh.getDisplayGuts();
	}

	@Override
	public boolean getFitToPage() {
		return _sh.getFitToPage();
	}

	@Override
	public boolean getRowSumsBelow() {
		return _sh.getRowSumsBelow();
	}

	@Override
	public boolean getRowSumsRight() {
		return _sh.getRowSumsRight();
	}

	@Override
	public boolean isPrintGridlines() {
		return _sh.isPrintGridlines();
	}

	@Override
	public void setPrintGridlines(boolean show) {
		_sh.setPrintGridlines(show);
	}

	@Override
	public boolean isPrintRowAndColumnHeadings() {
		return _sh.isPrintRowAndColumnHeadings();
	}

	@Override
	public void setPrintRowAndColumnHeadings(boolean show) {
		_sh.setPrintRowAndColumnHeadings(show);
	}

	@Override
	public PrintSetup getPrintSetup() {
		return _sh.getPrintSetup();
	}

	@Override
	public Header getHeader() {
		return _sh.getHeader();
	}

	@Override
	public Footer getFooter() {
		return _sh.getFooter();
	}

	@Override
	public void setSelected(boolean value) {
		_sh.setSelected(value);
	}

	@Override
	public double getMargin(short margin) {
		return _sh.getMargin(margin);
	}

	@Override
	public void setMargin(short margin, double size) {
		_sh.setMargin(margin, size);
	}

	@Override
	public boolean getProtect() {
		return _sh.getProtect();
	}

	@Override
	public void protectSheet(String password) {
		_sh.protectSheet(password);
	}

	@Override
	public boolean getScenarioProtect() {
		return _sh.getScenarioProtect();
	}

	@Override
	public void setZoom(int scale) {
		_sh.setZoom(scale);
	}

	@Override
	public short getTopRow() {
		return _sh.getTopRow();
	}

	@Override
	public short getLeftCol() {
		return _sh.getLeftCol();
	}

	@Override
	public void showInPane(int toprow, int leftcol) {
		_sh.showInPane(toprow, leftcol);
	}

	@Override
	public void setForceFormulaRecalculation(boolean value) {
		_sh.setForceFormulaRecalculation(value);
	}

	@Override
	public boolean getForceFormulaRecalculation() {
		return _sh.getForceFormulaRecalculation();
	}

	@org.apache.poi.util.NotImplemented
	@Override
	public void shiftRows(int startRow, int endRow, int n) {
		throw new RuntimeException("Not Implemented");
	}

	@org.apache.poi.util.NotImplemented
	@Override
	public void shiftRows(int startRow, int endRow, int n, boolean copyRowHeight, boolean resetOriginalRowHeight) {
		throw new RuntimeException("Not Implemented");
	}

	@Override
	public void createFreezePane(int colSplit, int rowSplit, int leftmostColumn, int topRow) {
		_sh.createFreezePane(colSplit, rowSplit, leftmostColumn, topRow);
	}

	@Override
	public void createFreezePane(int colSplit, int rowSplit) {
		_sh.createFreezePane(colSplit, rowSplit);
	}

	@Override
	public void createSplitPane(int xSplitPos, int ySplitPos, int leftmostColumn, int topRow, int activePane) {
		_sh.createSplitPane(xSplitPos, ySplitPos, leftmostColumn, topRow, activePane);
	}

	@Override
	public PaneInformation getPaneInformation() {
		return _sh.getPaneInformation();
	}

	@Override
	public void setDisplayGridlines(boolean show) {
		_sh.setDisplayGridlines(show);
	}

	@Override
	public boolean isDisplayGridlines() {
		return _sh.isDisplayGridlines();
	}

	@Override
	public void setDisplayFormulas(boolean show) {
		_sh.setDisplayFormulas(show);
	}

	@Override
	public boolean isDisplayFormulas() {
		return _sh.isDisplayFormulas();
	}

	@Override
	public void setDisplayRowColHeadings(boolean show) {
		_sh.setDisplayRowColHeadings(show);
	}

	@Override
	public boolean isDisplayRowColHeadings() {
		return _sh.isDisplayRowColHeadings();
	}

	@Override
	public void setRowBreak(int row) {
		_sh.setRowBreak(row);
	}

	@Override
	public boolean isRowBroken(int row) {
		return _sh.isRowBroken(row);
	}

	@Override
	public void removeRowBreak(int row) {
		_sh.removeRowBreak(row);
	}

	@Override
	public int[] getRowBreaks() {
		return _sh.getRowBreaks();
	}

	@Override
	public int[] getColumnBreaks() {
		return _sh.getColumnBreaks();
	}

	@Override
	public void setColumnBreak(int column) {
		_sh.setColumnBreak(column);
	}

	@Override
	public boolean isColumnBroken(int column) {
		return _sh.isColumnBroken(column);
	}

	@Override
	public void removeColumnBreak(int column) {
		_sh.removeColumnBreak(column);
	}

	@Override
	public void setColumnGroupCollapsed(int columnNumber, boolean collapsed) {
		_sh.setColumnGroupCollapsed(columnNumber, collapsed);
	}

	@Override
	public void groupColumn(int fromColumn, int toColumn) {
		_sh.groupColumn(fromColumn, toColumn);
	}

	@Override
	public void ungroupColumn(int fromColumn, int toColumn) {
		_sh.ungroupColumn(fromColumn, toColumn);
	}

	@Override
	public void groupRow(int fromRow, int toRow) {
		for (SXSSFRow row : _rows.subMap(fromRow, (toRow + 1)).values()) {
			int level = (row.getOutlineLevel()) + 1;
			if (level > (outlineLevelRow)) {
				outlineLevelRow = level;
			}
		}
		setWorksheetOutlineLevelRow();
	}

	public void setRowOutlineLevel(int rownum, int level) {
		SXSSFRow row = _rows.get(rownum);
		if ((level > 0) && (level > (outlineLevelRow))) {
			outlineLevelRow = level;
			setWorksheetOutlineLevelRow();
		}
	}

	private void setWorksheetOutlineLevelRow() {
		CTWorksheet ct = _sh.getCTWorksheet();
		CTSheetFormatPr pr = (ct.isSetSheetFormatPr()) ? ct.getSheetFormatPr() : ct.addNewSheetFormatPr();
		if ((outlineLevelRow) > 0) {
			pr.setOutlineLevelRow(((short) (outlineLevelRow)));
		}
	}

	@Override
	public void ungroupRow(int fromRow, int toRow) {
		_sh.ungroupRow(fromRow, toRow);
	}

	@Override
	public void setRowGroupCollapsed(int row, boolean collapse) {
		if (collapse) {
			collapseRow(row);
		}else {
			throw new RuntimeException("Unable to expand row: Not Implemented");
		}
	}

	private void collapseRow(int rowIndex) {
		SXSSFRow row = getRow(rowIndex);
		if (row == null) {
			throw new IllegalArgumentException((("Invalid row number(" + rowIndex) + "). Row does not exist."));
		}else {
			int startRow = findStartOfRowOutlineGroup(rowIndex);
			int lastRow = writeHidden(row, startRow);
			SXSSFRow lastRowObj = getRow(lastRow);
			if (lastRowObj != null) {
				lastRowObj.setCollapsed(true);
			}else {
				SXSSFRow newRow = createRow(lastRow);
				newRow.setCollapsed(true);
			}
		}
	}

	private int findStartOfRowOutlineGroup(int rowIndex) {
		Row row = getRow(rowIndex);
		int level = row.getOutlineLevel();
		if (level == 0) {
			throw new IllegalArgumentException((("Outline level is zero for the row (" + rowIndex) + ")."));
		}
		int currentRow = rowIndex;
		while ((getRow(currentRow)) != null) {
			if ((getRow(currentRow).getOutlineLevel()) < level) {
				return currentRow + 1;
			}
			currentRow--;
		} 
		return currentRow + 1;
	}

	private int writeHidden(SXSSFRow xRow, int rowIndex) {
		int level = xRow.getOutlineLevel();
		SXSSFRow currRow = getRow(rowIndex);
		while ((currRow != null) && ((currRow.getOutlineLevel()) >= level)) {
			currRow.setHidden(true);
			rowIndex++;
			currRow = getRow(rowIndex);
		} 
		return rowIndex;
	}

	@Override
	public void setDefaultColumnStyle(int column, CellStyle style) {
		_sh.setDefaultColumnStyle(column, style);
	}

	public void trackColumnForAutoSizing(int column) {
	}

	public void trackColumnsForAutoSizing(Collection<Integer> columns) {
	}

	public void trackAllColumnsForAutoSizing() {
	}

	public boolean untrackColumnForAutoSizing(int column) {
		return false;
	}

	public boolean untrackColumnsForAutoSizing(Collection<Integer> columns) {
		return false;
	}

	public void untrackAllColumnsForAutoSizing() {
	}

	public boolean isColumnTrackedForAutoSizing(int column) {
		return false;
	}

	public Set<Integer> getTrackedColumnsForAutoSizing() {
		return null;
	}

	@Override
	public void autoSizeColumn(int column) {
		autoSizeColumn(column, false);
	}

	@Override
	public void autoSizeColumn(int column, boolean useMergedCells) {
		final int flushedWidth;
		try {
		} catch (final IllegalStateException e) {
			throw new IllegalStateException("Could not auto-size column. Make sure the column was tracked prior to auto-sizing the column.", e);
		}
		final int activeWidth = ((int) (256 * (SheetUtil.getColumnWidth(this, column, useMergedCells))));
		flushedWidth = 0;
		final int bestFitWidth = Math.max(flushedWidth, activeWidth);
		if (bestFitWidth > 0) {
			final int maxColumnWidth = 255 * 256;
			final int width = Math.min(bestFitWidth, maxColumnWidth);
			setColumnWidth(column, width);
		}
	}

	@Override
	public XSSFComment getCellComment(CellAddress ref) {
		return _sh.getCellComment(ref);
	}

	@Override
	public Map<CellAddress, XSSFComment> getCellComments() {
		return _sh.getCellComments();
	}

	@Override
	public XSSFHyperlink getHyperlink(int row, int column) {
		return _sh.getHyperlink(row, column);
	}

	@Override
	public XSSFHyperlink getHyperlink(CellAddress addr) {
		return _sh.getHyperlink(addr);
	}

	@Override
	public List<XSSFHyperlink> getHyperlinkList() {
		return _sh.getHyperlinkList();
	}

	@Override
	public XSSFDrawing getDrawingPatriarch() {
		return _sh.getDrawingPatriarch();
	}

	@Override
	public SXSSFDrawing createDrawingPatriarch() {
		return new SXSSFDrawing(getWorkbook(), _sh.createDrawingPatriarch());
	}

	@Override
	public SXSSFWorkbook getWorkbook() {
		return _workbook;
	}

	@Override
	public String getSheetName() {
		return _sh.getSheetName();
	}

	@Override
	public boolean isSelected() {
		return _sh.isSelected();
	}

	@Override
	public CellRange<? extends Cell> setArrayFormula(String formula, CellRangeAddress range) {
		throw new RuntimeException("Not Implemented");
	}

	@Override
	public CellRange<? extends Cell> removeArrayFormula(Cell cell) {
		throw new RuntimeException("Not Implemented");
	}

	@Override
	public DataValidationHelper getDataValidationHelper() {
		return _sh.getDataValidationHelper();
	}

	@Override
	public List<XSSFDataValidation> getDataValidations() {
		return _sh.getDataValidations();
	}

	@Override
	public void addValidationData(DataValidation dataValidation) {
		_sh.addValidationData(dataValidation);
	}

	@Override
	public AutoFilter setAutoFilter(CellRangeAddress range) {
		return _sh.setAutoFilter(range);
	}

	@Override
	public SheetConditionalFormatting getSheetConditionalFormatting() {
		return _sh.getSheetConditionalFormatting();
	}

	@Override
	public CellRangeAddress getRepeatingRows() {
		return _sh.getRepeatingRows();
	}

	@Override
	public CellRangeAddress getRepeatingColumns() {
		return _sh.getRepeatingColumns();
	}

	@Override
	public void setRepeatingRows(CellRangeAddress rowRangeRef) {
		_sh.setRepeatingRows(rowRangeRef);
	}

	@Override
	public void setRepeatingColumns(CellRangeAddress columnRangeRef) {
		_sh.setRepeatingColumns(columnRangeRef);
	}

	public void setRandomAccessWindowSize(int value) {
		if ((value == 0) || (value < (-1))) {
			throw new IllegalArgumentException("RandomAccessWindowSize must be either -1 or a positive integer");
		}
		_randomAccessWindowSize = value;
	}

	public boolean areAllRowsFlushed() {
		return allFlushed;
	}

	public int getLastFlushedRowNum() {
		return lastFlushedRowNumber;
	}

	public void flushRows(int remaining) throws IOException {
		while ((_rows.size()) > remaining) {
			flushOneRow();
		} 
		if (remaining == 0) {
			allFlushed = true;
		}
	}

	public void flushRows() throws IOException {
		this.flushRows(0);
	}

	private void flushOneRow() throws IOException {
		Integer firstRowNum = _rows.firstKey();
		if (firstRowNum != null) {
			int rowIndex = firstRowNum.intValue();
			SXSSFRow row = _rows.get(firstRowNum);
			_writer.writeRow(rowIndex, row);
			_rows.remove(firstRowNum);
			lastFlushedRowNumber = rowIndex;
		}
	}

	public void changeRowNum(SXSSFRow row, int newRowNum) {
		removeRow(row);
		_rows.put(newRowNum, row);
	}

	public int getRowNum(SXSSFRow row) {
		for (Map.Entry<Integer, SXSSFRow> entry : _rows.entrySet()) {
			if ((entry.getValue()) == row) {
				return entry.getKey().intValue();
			}
		}
		return -1;
	}

	boolean dispose() throws IOException {
		if (!(allFlushed)) {
			flushRows();
		}
		return false;
	}

	@Override
	public int getColumnOutlineLevel(int columnIndex) {
		return _sh.getColumnOutlineLevel(columnIndex);
	}

	@Override
	public CellAddress getActiveCell() {
		return _sh.getActiveCell();
	}

	@Override
	public void setActiveCell(CellAddress address) {
		_sh.setActiveCell(address);
	}

	public XSSFColor getTabColor() {
		return _sh.getTabColor();
	}

	public void setTabColor(XSSFColor color) {
		_sh.setTabColor(color);
	}

	public void enableLocking() {
		safeGetProtectionField().setSheet(true);
	}

	public void disableLocking() {
		safeGetProtectionField().setSheet(false);
	}

	public void lockAutoFilter(boolean enabled) {
		safeGetProtectionField().setAutoFilter(enabled);
	}

	public void lockDeleteColumns(boolean enabled) {
		safeGetProtectionField().setDeleteColumns(enabled);
	}

	public void lockDeleteRows(boolean enabled) {
		safeGetProtectionField().setDeleteRows(enabled);
	}

	public void lockFormatCells(boolean enabled) {
		safeGetProtectionField().setFormatCells(enabled);
	}

	public void lockFormatColumns(boolean enabled) {
		safeGetProtectionField().setFormatColumns(enabled);
	}

	public void lockFormatRows(boolean enabled) {
		safeGetProtectionField().setFormatRows(enabled);
	}

	public void lockInsertColumns(boolean enabled) {
		safeGetProtectionField().setInsertColumns(enabled);
	}

	public void lockInsertHyperlinks(boolean enabled) {
		safeGetProtectionField().setInsertHyperlinks(enabled);
	}

	public void lockInsertRows(boolean enabled) {
		safeGetProtectionField().setInsertRows(enabled);
	}

	public void lockPivotTables(boolean enabled) {
		safeGetProtectionField().setPivotTables(enabled);
	}

	public void lockSort(boolean enabled) {
		safeGetProtectionField().setSort(enabled);
	}

	public void lockObjects(boolean enabled) {
		safeGetProtectionField().setObjects(enabled);
	}

	public void lockScenarios(boolean enabled) {
		safeGetProtectionField().setScenarios(enabled);
	}

	public void lockSelectLockedCells(boolean enabled) {
		safeGetProtectionField().setSelectLockedCells(enabled);
	}

	public void lockSelectUnlockedCells(boolean enabled) {
		safeGetProtectionField().setSelectUnlockedCells(enabled);
	}

	private CTSheetProtection safeGetProtectionField() {
		CTWorksheet ct = _sh.getCTWorksheet();
		if (!(isSheetProtectionEnabled())) {
			return ct.addNewSheetProtection();
		}
		return ct.getSheetProtection();
	}

	boolean isSheetProtectionEnabled() {
		CTWorksheet ct = _sh.getCTWorksheet();
		return ct.isSetSheetProtection();
	}

	public void setTabColor(int colorIndex) {
		CTWorksheet ct = _sh.getCTWorksheet();
		CTSheetPr pr = ct.getSheetPr();
		if (pr == null)
			pr = ct.addNewSheetPr();

		CTColor color = newInstance();
		color.setIndexed(colorIndex);
		pr.setTabColor(color);
	}

	@org.apache.poi.util.NotImplemented
	@Override
	public void shiftColumns(int startColumn, int endColumn, int n) {
		throw new UnsupportedOperationException("Not Implemented");
	}
}

