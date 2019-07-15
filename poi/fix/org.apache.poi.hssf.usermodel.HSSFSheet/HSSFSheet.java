

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.hssf.model.DrawingManager2;
import org.apache.poi.hssf.model.HSSFFormulaParser;
import org.apache.poi.hssf.model.InternalSheet;
import org.apache.poi.hssf.model.InternalWorkbook;
import org.apache.poi.hssf.record.AbstractEscherHolderRecord;
import org.apache.poi.hssf.record.AutoFilterInfoRecord;
import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.DVRecord;
import org.apache.poi.hssf.record.DimensionsRecord;
import org.apache.poi.hssf.record.DrawingRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.HCenterRecord;
import org.apache.poi.hssf.record.HyperlinkRecord;
import org.apache.poi.hssf.record.NameRecord;
import org.apache.poi.hssf.record.PrintGridlinesRecord;
import org.apache.poi.hssf.record.PrintHeadersRecord;
import org.apache.poi.hssf.record.PrintSetupRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordBase;
import org.apache.poi.hssf.record.RowRecord;
import org.apache.poi.hssf.record.SCLRecord;
import org.apache.poi.hssf.record.VCenterRecord;
import org.apache.poi.hssf.record.WSBoolRecord;
import org.apache.poi.hssf.record.WindowTwoRecord;
import org.apache.poi.hssf.record.aggregates.DataValidityTable;
import org.apache.poi.hssf.record.aggregates.PageSettingsBlock;
import org.apache.poi.hssf.record.aggregates.RecordAggregate;
import org.apache.poi.hssf.record.aggregates.RowRecordsAggregate;
import org.apache.poi.hssf.record.aggregates.WorksheetProtectionBlock;
import org.apache.poi.hssf.usermodel.HSSFAutoFilter;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFDataValidation;
import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFFooter;
import org.apache.poi.hssf.usermodel.HSSFHeader;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFPrintSetup;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFShapeContainer;
import org.apache.poi.hssf.usermodel.HSSFShapeGroup;
import org.apache.poi.hssf.usermodel.HSSFSheetConditionalFormatting;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.Area3DPtg;
import org.apache.poi.ss.formula.ptg.AreaPtgBase;
import org.apache.poi.ss.formula.ptg.MemFuncPtg;
import org.apache.poi.ss.formula.ptg.OperationPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.UnionPtg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellRange;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressBase;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.ss.util.SSCellRange;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.util.Configurator;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public final class HSSFSheet implements Sheet {
	private static final POILogger log = POILogFactory.getLogger(HSSFSheet.class);

	private static final int DEBUG = POILogger.DEBUG;

	private static final float PX_DEFAULT = 32.0F;

	private static final float PX_MODIFIED = 36.56F;

	public static final int INITIAL_CAPACITY = Configurator.getIntValue("HSSFSheet.RowInitialCapacity", 20);

	private final InternalSheet _sheet;

	private final TreeMap<Integer, HSSFRow> _rows;

	protected final InternalWorkbook _book;

	protected final HSSFWorkbook _workbook;

	private HSSFPatriarch _patriarch;

	private int _firstrow;

	private int _lastrow;

	protected HSSFSheet(HSSFWorkbook workbook) {
		_sheet = InternalSheet.createSheet();
		_rows = new TreeMap<>();
		this._workbook = workbook;
		_book = null;
	}

	protected HSSFSheet(HSSFWorkbook workbook, InternalSheet sheet) {
		this._sheet = sheet;
		_rows = new TreeMap<>();
		this._workbook = workbook;
		setPropertiesFromSheet(sheet);
		_book = null;
	}

	HSSFSheet cloneSheet(HSSFWorkbook workbook) {
		this.getDrawingPatriarch();
		HSSFSheet sheet = new HSSFSheet(workbook, _sheet.cloneSheet());
		int pos = sheet._sheet.findFirstRecordLocBySid(DrawingRecord.sid);
		DrawingRecord dr = ((DrawingRecord) (sheet._sheet.findFirstRecordBySid(DrawingRecord.sid)));
		if (null != dr) {
			sheet._sheet.getRecords().remove(dr);
		}
		if ((getDrawingPatriarch()) != null) {
		}
		return sheet;
	}

	protected void preSerialize() {
		if ((_patriarch) != null) {
		}
	}

	@Override
	public HSSFWorkbook getWorkbook() {
		return _workbook;
	}

	private void setPropertiesFromSheet(InternalSheet sheet) {
		RowRecord row = sheet.getNextRow();
		while (row != null) {
			createRowFromRecord(row);
			row = sheet.getNextRow();
		} 
		Iterator<CellValueRecordInterface> iter = sheet.getCellValueIterator();
		long timestart = System.currentTimeMillis();
		if (HSSFSheet.log.check(POILogger.DEBUG)) {
			HSSFSheet.log.log(HSSFSheet.DEBUG, "Time at start of cell creating in HSSF sheet = ", Long.valueOf(timestart));
		}
		HSSFRow lastrow = null;
		while (iter.hasNext()) {
			CellValueRecordInterface cval = iter.next();
			long cellstart = System.currentTimeMillis();
			HSSFRow hrow = lastrow;
			if ((hrow == null) || ((hrow.getRowNum()) != (cval.getRow()))) {
				hrow = getRow(cval.getRow());
				lastrow = hrow;
				if (hrow == null) {
					RowRecord rowRec = new RowRecord(cval.getRow());
					sheet.addRow(rowRec);
					hrow = createRowFromRecord(rowRec);
				}
			}
			if (HSSFSheet.log.check(POILogger.DEBUG)) {
				if (cval instanceof Record) {
					HSSFSheet.log.log(HSSFSheet.DEBUG, ("record id = " + (Integer.toHexString(((Record) (cval)).getSid()))));
				}else {
					HSSFSheet.log.log(HSSFSheet.DEBUG, ("record = " + cval));
				}
			}
			if (HSSFSheet.log.check(POILogger.DEBUG)) {
				HSSFSheet.log.log(HSSFSheet.DEBUG, "record took ", Long.valueOf(((System.currentTimeMillis()) - cellstart)));
			}
		} 
		if (HSSFSheet.log.check(POILogger.DEBUG)) {
			HSSFSheet.log.log(HSSFSheet.DEBUG, "total sheet cell creation took ", Long.valueOf(((System.currentTimeMillis()) - timestart)));
		}
	}

	@Override
	public HSSFRow createRow(int rownum) {
		return null;
	}

	private HSSFRow createRowFromRecord(RowRecord row) {
		return null;
	}

	@Override
	public void removeRow(Row row) {
		HSSFRow hrow = ((HSSFRow) (row));
		if ((row.getSheet()) != (this)) {
			throw new IllegalArgumentException("Specified row does not belong to this sheet");
		}
		for (Cell cell : row) {
			HSSFCell xcell = ((HSSFCell) (cell));
			if (xcell.isPartOfArrayFormulaGroup()) {
				String msg = ("Row[rownum=" + (row.getRowNum())) + "] contains cell(s) included in a multi-cell array formula. You cannot change part of an array.";
			}
		}
		if ((_rows.size()) > 0) {
			Integer key = Integer.valueOf(row.getRowNum());
			HSSFRow removedRow = _rows.remove(key);
			if (removedRow != row) {
				throw new IllegalArgumentException("Specified row does not belong to this sheet");
			}
			if ((hrow.getRowNum()) == (getLastRowNum())) {
				_lastrow = findLastRow(_lastrow);
			}
			if ((hrow.getRowNum()) == (getFirstRowNum())) {
				_firstrow = findFirstRow(_firstrow);
			}
		}
	}

	private int findLastRow(int lastrow) {
		if (lastrow < 1) {
			return 0;
		}
		int rownum = lastrow - 1;
		HSSFRow r = getRow(rownum);
		while ((r == null) && (rownum > 0)) {
			r = getRow((--rownum));
		} 
		if (r == null) {
			return 0;
		}
		return rownum;
	}

	private int findFirstRow(int firstrow) {
		int rownum = firstrow + 1;
		HSSFRow r = getRow(rownum);
		while ((r == null) && (rownum <= (getLastRowNum()))) {
			r = getRow((++rownum));
		} 
		if (rownum > (getLastRowNum()))
			return 0;

		return rownum;
	}

	private void addRow(HSSFRow row, boolean addLow) {
		_rows.put(Integer.valueOf(row.getRowNum()), row);
		if (addLow) {
		}
		boolean firstRow = (_rows.size()) == 1;
		if (((row.getRowNum()) > (getLastRowNum())) || firstRow) {
			_lastrow = row.getRowNum();
		}
		if (((row.getRowNum()) < (getFirstRowNum())) || firstRow) {
			_firstrow = row.getRowNum();
		}
	}

	@Override
	public HSSFRow getRow(int rowIndex) {
		return _rows.get(Integer.valueOf(rowIndex));
	}

	@Override
	public int getPhysicalNumberOfRows() {
		return _rows.size();
	}

	@Override
	public int getFirstRowNum() {
		return _firstrow;
	}

	@Override
	public int getLastRowNum() {
		return _lastrow;
	}

	@Override
	public List<HSSFDataValidation> getDataValidations() {
		DataValidityTable dvt = _sheet.getOrCreateDataValidityTable();
		final List<HSSFDataValidation> hssfValidations = new ArrayList<>();
		RecordAggregate.RecordVisitor visitor = new RecordAggregate.RecordVisitor() {
			private HSSFEvaluationWorkbook book = HSSFEvaluationWorkbook.create(getWorkbook());

			@Override
			public void visitRecord(Record r) {
				if (!(r instanceof DVRecord)) {
					return;
				}
				DVRecord dvRecord = ((DVRecord) (r));
				CellRangeAddressList regions = dvRecord.getCellRangeAddress().copy();
			}
		};
		dvt.visitContainedRecords(visitor);
		return hssfValidations;
	}

	@Override
	public void addValidationData(DataValidation dataValidation) {
		if (dataValidation == null) {
			throw new IllegalArgumentException("objValidation must not be null");
		}
		HSSFDataValidation hssfDataValidation = ((HSSFDataValidation) (dataValidation));
		DataValidityTable dvt = _sheet.getOrCreateDataValidityTable();
	}

	@Override
	public void setColumnHidden(int columnIndex, boolean hidden) {
		_sheet.setColumnHidden(columnIndex, hidden);
	}

	@Override
	public boolean isColumnHidden(int columnIndex) {
		return _sheet.isColumnHidden(columnIndex);
	}

	@Override
	public void setColumnWidth(int columnIndex, int width) {
		_sheet.setColumnWidth(columnIndex, width);
	}

	@Override
	public int getColumnWidth(int columnIndex) {
		return _sheet.getColumnWidth(columnIndex);
	}

	@Override
	public float getColumnWidthInPixels(int column) {
		int cw = getColumnWidth(column);
		int def = (getDefaultColumnWidth()) * 256;
		float px = (cw == def) ? HSSFSheet.PX_DEFAULT : HSSFSheet.PX_MODIFIED;
		return cw / px;
	}

	@Override
	public int getDefaultColumnWidth() {
		return _sheet.getDefaultColumnWidth();
	}

	@Override
	public void setDefaultColumnWidth(int width) {
		_sheet.setDefaultColumnWidth(width);
	}

	@Override
	public short getDefaultRowHeight() {
		return _sheet.getDefaultRowHeight();
	}

	@Override
	public float getDefaultRowHeightInPoints() {
		return ((float) (_sheet.getDefaultRowHeight())) / 20;
	}

	@Override
	public void setDefaultRowHeight(short height) {
		_sheet.setDefaultRowHeight(height);
	}

	@Override
	public void setDefaultRowHeightInPoints(float height) {
		_sheet.setDefaultRowHeight(((short) (height * 20)));
	}

	@Override
	public HSSFCellStyle getColumnStyle(int column) {
		short styleIndex = _sheet.getXFIndexForColAt(((short) (column)));
		if (styleIndex == 15) {
			return null;
		}
		ExtendedFormatRecord xf = _book.getExFormatAt(styleIndex);
		return null;
	}

	public boolean isGridsPrinted() {
		return _sheet.isGridsPrinted();
	}

	public void setGridsPrinted(boolean value) {
		_sheet.setGridsPrinted(value);
	}

	@Override
	public int addMergedRegion(CellRangeAddress region) {
		return addMergedRegion(region, true);
	}

	@Override
	public int addMergedRegionUnsafe(CellRangeAddress region) {
		return addMergedRegion(region, false);
	}

	@Override
	public void validateMergedRegions() {
		checkForMergedRegionsIntersectingArrayFormulas();
		checkForIntersectingMergedRegions();
	}

	private int addMergedRegion(CellRangeAddress region, boolean validate) {
		if ((region.getNumberOfCells()) < 2) {
			throw new IllegalArgumentException((("Merged region " + (region.formatAsString())) + " must contain 2 or more cells"));
		}
		region.validate(SpreadsheetVersion.EXCEL97);
		if (validate) {
			validateArrayFormulas(region);
			validateMergedRegions(region);
		}
		return _sheet.addMergedRegion(region.getFirstRow(), region.getFirstColumn(), region.getLastRow(), region.getLastColumn());
	}

	private void validateArrayFormulas(CellRangeAddress region) {
		int firstRow = region.getFirstRow();
		int firstColumn = region.getFirstColumn();
		int lastRow = region.getLastRow();
		int lastColumn = region.getLastColumn();
		for (int rowIn = firstRow; rowIn <= lastRow; rowIn++) {
			HSSFRow row = getRow(rowIn);
			if (row == null)
				continue;

			for (int colIn = firstColumn; colIn <= lastColumn; colIn++) {
				HSSFCell cell = row.getCell(colIn);
				if (cell == null)
					continue;

				if (cell.isPartOfArrayFormulaGroup()) {
					CellRangeAddress arrayRange = cell.getArrayFormulaRange();
					if (((arrayRange.getNumberOfCells()) > 1) && (region.intersects(arrayRange))) {
						String msg = (("The range " + (region.formatAsString())) + " intersects with a multi-cell array formula. ") + "You cannot merge cells of an array.";
						throw new IllegalStateException(msg);
					}
				}
			}
		}
	}

	private void checkForMergedRegionsIntersectingArrayFormulas() {
		for (CellRangeAddress region : getMergedRegions()) {
			validateArrayFormulas(region);
		}
	}

	private void validateMergedRegions(CellRangeAddress candidateRegion) {
		for (final CellRangeAddress existingRegion : getMergedRegions()) {
			if (existingRegion.intersects(candidateRegion)) {
				throw new IllegalStateException((((("Cannot add merged region " + (candidateRegion.formatAsString())) + " to sheet because it overlaps with an existing merged region (") + (existingRegion.formatAsString())) + ")."));
			}
		}
	}

	private void checkForIntersectingMergedRegions() {
		final List<CellRangeAddress> regions = getMergedRegions();
		final int size = regions.size();
		for (int i = 0; i < size; i++) {
			final CellRangeAddress region = regions.get(i);
			for (final CellRangeAddress other : regions.subList((i + 1), regions.size())) {
				if (region.intersects(other)) {
					String msg = ((("The range " + (region.formatAsString())) + " intersects with another merged region ") + (other.formatAsString())) + " in this sheet";
					throw new IllegalStateException(msg);
				}
			}
		}
	}

	@Override
	public void setForceFormulaRecalculation(boolean value) {
		_sheet.setUncalced(value);
	}

	@Override
	public boolean getForceFormulaRecalculation() {
		return _sheet.getUncalced();
	}

	@Override
	public void setVerticallyCenter(boolean value) {
		_sheet.getPageSettings().getVCenter().setVCenter(value);
	}

	@Override
	public boolean getVerticallyCenter() {
		return _sheet.getPageSettings().getVCenter().getVCenter();
	}

	@Override
	public void setHorizontallyCenter(boolean value) {
		_sheet.getPageSettings().getHCenter().setHCenter(value);
	}

	@Override
	public boolean getHorizontallyCenter() {
		return _sheet.getPageSettings().getHCenter().getHCenter();
	}

	@Override
	public void setRightToLeft(boolean value) {
		_sheet.getWindowTwo().setArabic(value);
	}

	@Override
	public boolean isRightToLeft() {
		return _sheet.getWindowTwo().getArabic();
	}

	@Override
	public void removeMergedRegion(int index) {
		_sheet.removeMergedRegion(index);
	}

	@Override
	public void removeMergedRegions(Collection<Integer> indices) {
		for (int i : new TreeSet<>(indices).descendingSet()) {
			_sheet.removeMergedRegion(i);
		}
	}

	@Override
	public int getNumMergedRegions() {
		return _sheet.getNumMergedRegions();
	}

	@Override
	public CellRangeAddress getMergedRegion(int index) {
		return _sheet.getMergedRegionAt(index);
	}

	@Override
	public List<CellRangeAddress> getMergedRegions() {
		List<CellRangeAddress> addresses = new ArrayList<>();
		int count = _sheet.getNumMergedRegions();
		for (int i = 0; i < count; i++) {
			addresses.add(_sheet.getMergedRegionAt(i));
		}
		return addresses;
	}

	@Override
	public Iterator<Row> rowIterator() {
		@SuppressWarnings("unchecked")
		Iterator<Row> result = ((Iterator<Row>) ((Iterator<? extends Row>) (_rows.values().iterator())));
		return result;
	}

	@Override
	public Iterator<Row> iterator() {
		return rowIterator();
	}

	InternalSheet getSheet() {
		return _sheet;
	}

	public void setAlternativeExpression(boolean b) {
		WSBoolRecord record = ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid)));
		record.setAlternateExpression(b);
	}

	public void setAlternativeFormula(boolean b) {
		WSBoolRecord record = ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid)));
		record.setAlternateFormula(b);
	}

	@Override
	public void setAutobreaks(boolean b) {
		WSBoolRecord record = ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid)));
		record.setAutobreaks(b);
	}

	public void setDialog(boolean b) {
		WSBoolRecord record = ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid)));
		record.setDialog(b);
	}

	@Override
	public void setDisplayGuts(boolean b) {
		WSBoolRecord record = ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid)));
		record.setDisplayGuts(b);
	}

	@Override
	public void setFitToPage(boolean b) {
		WSBoolRecord record = ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid)));
		record.setFitToPage(b);
	}

	@Override
	public void setRowSumsBelow(boolean b) {
		WSBoolRecord record = ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid)));
		record.setRowSumsBelow(b);
		record.setAlternateExpression(b);
	}

	@Override
	public void setRowSumsRight(boolean b) {
		WSBoolRecord record = ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid)));
		record.setRowSumsRight(b);
	}

	public boolean getAlternateExpression() {
		return ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid))).getAlternateExpression();
	}

	public boolean getAlternateFormula() {
		return ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid))).getAlternateFormula();
	}

	@Override
	public boolean getAutobreaks() {
		return ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid))).getAutobreaks();
	}

	public boolean getDialog() {
		return ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid))).getDialog();
	}

	@Override
	public boolean getDisplayGuts() {
		return ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid))).getDisplayGuts();
	}

	@Override
	public boolean isDisplayZeros() {
		return _sheet.getWindowTwo().getDisplayZeros();
	}

	@Override
	public void setDisplayZeros(boolean value) {
		_sheet.getWindowTwo().setDisplayZeros(value);
	}

	@Override
	public boolean getFitToPage() {
		return ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid))).getFitToPage();
	}

	@Override
	public boolean getRowSumsBelow() {
		return ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid))).getRowSumsBelow();
	}

	@Override
	public boolean getRowSumsRight() {
		return ((WSBoolRecord) (_sheet.findFirstRecordBySid(WSBoolRecord.sid))).getRowSumsRight();
	}

	@Override
	public boolean isPrintGridlines() {
		return getSheet().getPrintGridlines().getPrintGridlines();
	}

	@Override
	public void setPrintGridlines(boolean show) {
		getSheet().getPrintGridlines().setPrintGridlines(show);
	}

	@Override
	public boolean isPrintRowAndColumnHeadings() {
		return getSheet().getPrintHeaders().getPrintHeaders();
	}

	@Override
	public void setPrintRowAndColumnHeadings(boolean show) {
		getSheet().getPrintHeaders().setPrintHeaders(show);
	}

	@Override
	public HSSFPrintSetup getPrintSetup() {
		return null;
	}

	@Override
	public HSSFHeader getHeader() {
		return null;
	}

	@Override
	public HSSFFooter getFooter() {
		return null;
	}

	@Override
	public boolean isSelected() {
		return getSheet().getWindowTwo().getSelected();
	}

	@Override
	public void setSelected(boolean sel) {
		getSheet().getWindowTwo().setSelected(sel);
	}

	public boolean isActive() {
		return getSheet().getWindowTwo().isActive();
	}

	public void setActive(boolean sel) {
		getSheet().getWindowTwo().setActive(sel);
	}

	@Override
	public double getMargin(short margin) {
		switch (margin) {
			case Sheet.FooterMargin :
				return _sheet.getPageSettings().getPrintSetup().getFooterMargin();
			case Sheet.HeaderMargin :
				return _sheet.getPageSettings().getPrintSetup().getHeaderMargin();
			default :
				return _sheet.getPageSettings().getMargin(margin);
		}
	}

	@Override
	public void setMargin(short margin, double size) {
		switch (margin) {
			case Sheet.FooterMargin :
				_sheet.getPageSettings().getPrintSetup().setFooterMargin(size);
				break;
			case Sheet.HeaderMargin :
				_sheet.getPageSettings().getPrintSetup().setHeaderMargin(size);
				break;
			default :
				_sheet.getPageSettings().setMargin(margin, size);
		}
	}

	private WorksheetProtectionBlock getProtectionBlock() {
		return _sheet.getProtectionBlock();
	}

	@Override
	public boolean getProtect() {
		return getProtectionBlock().isSheetProtected();
	}

	public short getPassword() {
		return ((short) (getProtectionBlock().getPasswordHash()));
	}

	public boolean getObjectProtect() {
		return getProtectionBlock().isObjectProtected();
	}

	@Override
	public boolean getScenarioProtect() {
		return getProtectionBlock().isScenarioProtected();
	}

	@Override
	public void protectSheet(String password) {
		getProtectionBlock().protectSheet(password, true, true);
	}

	public void setZoom(int numerator, int denominator) {
		if ((numerator < 1) || (numerator > 65535))
			throw new IllegalArgumentException("Numerator must be greater than 0 and less than 65536");

		if ((denominator < 1) || (denominator > 65535))
			throw new IllegalArgumentException("Denominator must be greater than 0 and less than 65536");

		SCLRecord sclRecord = new SCLRecord();
		sclRecord.setNumerator(((short) (numerator)));
		sclRecord.setDenominator(((short) (denominator)));
		getSheet().setSCLRecord(sclRecord);
	}

	@Override
	public void setZoom(int scale) {
		setZoom(scale, 100);
	}

	@Override
	public short getTopRow() {
		return _sheet.getTopRow();
	}

	@Override
	public short getLeftCol() {
		return _sheet.getLeftCol();
	}

	@Override
	public void showInPane(int toprow, int leftcol) {
		int maxrow = SpreadsheetVersion.EXCEL97.getLastRowIndex();
		if (toprow > maxrow)
			throw new IllegalArgumentException(("Maximum row number is " + maxrow));

		showInPane(((short) (toprow)), ((short) (leftcol)));
	}

	private void showInPane(short toprow, short leftcol) {
		_sheet.setTopRow(toprow);
		_sheet.setLeftCol(leftcol);
	}

	protected void shiftMerged(int startRow, int endRow, int n, boolean isRow) {
	}

	@Override
	public void shiftRows(int startRow, int endRow, int n) {
		shiftRows(startRow, endRow, n, false, false);
	}

	@Override
	public void shiftRows(int startRow, int endRow, int n, boolean copyRowHeight, boolean resetOriginalRowHeight) {
		shiftRows(startRow, endRow, n, copyRowHeight, resetOriginalRowHeight, true);
	}

	private static int clip(int row) {
		return Math.min(Math.max(0, row), SpreadsheetVersion.EXCEL97.getLastRowIndex());
	}

	public void shiftRows(int startRow, int endRow, int n, boolean copyRowHeight, boolean resetOriginalRowHeight, boolean moveComments) {
		int s;
		int inc;
		if (endRow < startRow) {
			throw new IllegalArgumentException("startRow must be less than or equal to endRow. To shift rows up, use n<0.");
		}
		if (n < 0) {
			s = startRow;
			inc = 1;
		}else
			if (n > 0) {
				s = endRow;
				inc = -1;
			}else {
				return;
			}

		if (moveComments) {
			moveCommentsForRowShift(startRow, endRow, n);
		}
		_sheet.getPageSettings().shiftRowBreaks(startRow, endRow, n);
		deleteOverwrittenHyperlinksForRowShift(startRow, endRow, n);
		for (int rowNum = s; (((rowNum >= startRow) && (rowNum <= endRow)) && (rowNum >= 0)) && (rowNum < 65536); rowNum += inc) {
			HSSFRow row = getRow(rowNum);
			if (row != null)
				notifyRowShifting(row);

			HSSFRow row2Replace = getRow((rowNum + n));
			if (row2Replace == null)
				row2Replace = createRow((rowNum + n));

			if (row == null)
				continue;

			if (copyRowHeight) {
				row2Replace.setHeight(row.getHeight());
			}
			if (resetOriginalRowHeight) {
				row.setHeight(((short) (255)));
			}
			for (Iterator<Cell> cells = row.cellIterator(); cells.hasNext();) {
				HSSFCell cell = ((HSSFCell) (cells.next()));
				HSSFHyperlink link = cell.getHyperlink();
				row.removeCell(cell);
				if (link != null) {
					link.setFirstRow(((link.getFirstRow()) + n));
					link.setLastRow(((link.getLastRow()) + n));
				}
			}
		}
		recomputeFirstAndLastRowsForRowShift(startRow, endRow, n);
		int sheetIndex = _workbook.getSheetIndex(this);
		short externSheetIndex = _book.checkExternSheet(sheetIndex);
		String sheetName = _workbook.getSheetName(sheetIndex);
		FormulaShifter formulaShifter = FormulaShifter.createForRowShift(externSheetIndex, sheetName, startRow, endRow, n, SpreadsheetVersion.EXCEL97);
		updateFormulasForShift(formulaShifter);
	}

	private void updateFormulasForShift(FormulaShifter formulaShifter) {
		int sheetIndex = _workbook.getSheetIndex(this);
		short externSheetIndex = _book.checkExternSheet(sheetIndex);
		_sheet.updateFormulasAfterCellShift(formulaShifter, externSheetIndex);
		int nSheets = _workbook.getNumberOfSheets();
		for (int i = 0; i < nSheets; i++) {
			short otherExtSheetIx = _book.checkExternSheet(i);
		}
	}

	private void recomputeFirstAndLastRowsForRowShift(int startRow, int endRow, int n) {
		if (n > 0) {
			if (startRow == (_firstrow)) {
				_firstrow = Math.max((startRow + n), 0);
				for (int i = startRow + 1; i < (startRow + n); i++) {
					if ((getRow(i)) != null) {
						_firstrow = i;
						break;
					}
				}
			}
			if ((endRow + n) > (_lastrow)) {
				_lastrow = Math.min((endRow + n), SpreadsheetVersion.EXCEL97.getLastRowIndex());
			}
		}else {
			if ((startRow + n) < (_firstrow)) {
				_firstrow = Math.max((startRow + n), 0);
			}
			if (endRow == (_lastrow)) {
				_lastrow = Math.min((endRow + n), SpreadsheetVersion.EXCEL97.getLastRowIndex());
				for (int i = endRow - 1; i > (endRow + n); i--) {
					if ((getRow(i)) != null) {
						_lastrow = i;
						break;
					}
				}
			}
		}
	}

	private void deleteOverwrittenHyperlinksForRowShift(int startRow, int endRow, int n) {
		final int firstOverwrittenRow = startRow + n;
		final int lastOverwrittenRow = endRow + n;
		for (HSSFHyperlink link : getHyperlinkList()) {
			final int firstRow = link.getFirstRow();
			final int lastRow = link.getLastRow();
			if ((((firstOverwrittenRow <= firstRow) && (firstRow <= lastOverwrittenRow)) && (lastOverwrittenRow <= lastRow)) && (lastRow <= lastOverwrittenRow)) {
				removeHyperlink(link);
			}
		}
	}

	private void moveCommentsForRowShift(int startRow, int endRow, int n) {
		final HSSFPatriarch patriarch = createDrawingPatriarch();
		for (final HSSFShape shape : patriarch.getChildren()) {
			if (!(shape instanceof HSSFComment)) {
				continue;
			}
			final HSSFComment comment = ((HSSFComment) (shape));
			final int r = comment.getRow();
			if ((startRow <= r) && (r <= endRow)) {
				comment.setRow(HSSFSheet.clip((r + n)));
			}
		}
	}

	@org.apache.poi.util.Beta
	@Override
	public void shiftColumns(int startColumn, int endColumn, int n) {
		int sheetIndex = _workbook.getSheetIndex(this);
		short externSheetIndex = _book.checkExternSheet(sheetIndex);
		String sheetName = _workbook.getSheetName(sheetIndex);
		FormulaShifter formulaShifter = FormulaShifter.createForColumnShift(externSheetIndex, sheetName, startColumn, endColumn, n, SpreadsheetVersion.EXCEL97);
		updateFormulasForShift(formulaShifter);
	}

	protected void insertChartRecords(List<Record> records) {
		int window2Loc = _sheet.findFirstRecordLocBySid(WindowTwoRecord.sid);
		_sheet.getRecords().addAll(window2Loc, records);
	}

	private void notifyRowShifting(HSSFRow row) {
		String msg = (("Row[rownum=" + (row.getRowNum())) + "] contains cell(s) included in a multi-cell array formula. ") + "You cannot change part of an array.";
		for (Cell cell : row) {
			HSSFCell hcell = ((HSSFCell) (cell));
			if (hcell.isPartOfArrayFormulaGroup()) {
			}
		}
	}

	@Override
	public void createFreezePane(int colSplit, int rowSplit, int leftmostColumn, int topRow) {
		validateColumn(colSplit);
		validateRow(rowSplit);
		if (leftmostColumn < colSplit)
			throw new IllegalArgumentException("leftmostColumn parameter must not be less than colSplit parameter");

		if (topRow < rowSplit)
			throw new IllegalArgumentException("topRow parameter must not be less than leftmostColumn parameter");

		getSheet().createFreezePane(colSplit, rowSplit, topRow, leftmostColumn);
	}

	@Override
	public void createFreezePane(int colSplit, int rowSplit) {
		createFreezePane(colSplit, rowSplit, colSplit, rowSplit);
	}

	@Override
	public void createSplitPane(int xSplitPos, int ySplitPos, int leftmostColumn, int topRow, int activePane) {
		getSheet().createSplitPane(xSplitPos, ySplitPos, topRow, leftmostColumn, activePane);
	}

	@Override
	public PaneInformation getPaneInformation() {
		return getSheet().getPaneInformation();
	}

	@Override
	public void setDisplayGridlines(boolean show) {
		_sheet.setDisplayGridlines(show);
	}

	@Override
	public boolean isDisplayGridlines() {
		return _sheet.isDisplayGridlines();
	}

	@Override
	public void setDisplayFormulas(boolean show) {
		_sheet.setDisplayFormulas(show);
	}

	@Override
	public boolean isDisplayFormulas() {
		return _sheet.isDisplayFormulas();
	}

	@Override
	public void setDisplayRowColHeadings(boolean show) {
		_sheet.setDisplayRowColHeadings(show);
	}

	@Override
	public boolean isDisplayRowColHeadings() {
		return _sheet.isDisplayRowColHeadings();
	}

	@Override
	public void setRowBreak(int row) {
		validateRow(row);
		_sheet.getPageSettings().setRowBreak(row, ((short) (0)), ((short) (255)));
	}

	@Override
	public boolean isRowBroken(int row) {
		return _sheet.getPageSettings().isRowBroken(row);
	}

	@Override
	public void removeRowBreak(int row) {
		_sheet.getPageSettings().removeRowBreak(row);
	}

	@Override
	public int[] getRowBreaks() {
		return _sheet.getPageSettings().getRowBreaks();
	}

	@Override
	public int[] getColumnBreaks() {
		return _sheet.getPageSettings().getColumnBreaks();
	}

	@Override
	public void setColumnBreak(int column) {
		validateColumn(((short) (column)));
		_sheet.getPageSettings().setColumnBreak(((short) (column)), ((short) (0)), ((short) (SpreadsheetVersion.EXCEL97.getLastRowIndex())));
	}

	@Override
	public boolean isColumnBroken(int column) {
		return _sheet.getPageSettings().isColumnBroken(column);
	}

	@Override
	public void removeColumnBreak(int column) {
		_sheet.getPageSettings().removeColumnBreak(column);
	}

	protected void validateRow(int row) {
		int maxrow = SpreadsheetVersion.EXCEL97.getLastRowIndex();
		if (row > maxrow)
			throw new IllegalArgumentException(("Maximum row number is " + maxrow));

		if (row < 0)
			throw new IllegalArgumentException("Minumum row number is 0");

	}

	protected void validateColumn(int column) {
		int maxcol = SpreadsheetVersion.EXCEL97.getLastColumnIndex();
		if (column > maxcol)
			throw new IllegalArgumentException(("Maximum column number is " + maxcol));

		if (column < 0)
			throw new IllegalArgumentException("Minimum column number is 0");

	}

	public void dumpDrawingRecords(boolean fat, PrintWriter pw) {
		_sheet.aggregateDrawingRecords(_book.getDrawingManager(), false);
		EscherAggregate r = ((EscherAggregate) (getSheet().findFirstRecordBySid(EscherAggregate.sid)));
		List<EscherRecord> escherRecords = r.getEscherRecords();
		for (EscherRecord escherRecord : escherRecords) {
			if (fat) {
				pw.println(escherRecord);
			}else {
				escherRecord.display(pw, 0);
			}
		}
		pw.flush();
	}

	public EscherAggregate getDrawingEscherAggregate() {
		_book.findDrawingGroup();
		if ((_book.getDrawingManager()) == null) {
			return null;
		}
		int found = _sheet.aggregateDrawingRecords(_book.getDrawingManager(), false);
		if (found == (-1)) {
			return null;
		}
		return ((EscherAggregate) (_sheet.findFirstRecordBySid(EscherAggregate.sid)));
	}

	@Override
	public HSSFPatriarch getDrawingPatriarch() {
		_patriarch = getPatriarch(false);
		return _patriarch;
	}

	@Override
	public HSSFPatriarch createDrawingPatriarch() {
		_patriarch = getPatriarch(true);
		return _patriarch;
	}

	private HSSFPatriarch getPatriarch(boolean createIfMissing) {
		if ((_patriarch) != null) {
			return _patriarch;
		}
		DrawingManager2 dm = _book.findDrawingGroup();
		if (null == dm) {
			if (!createIfMissing) {
				return null;
			}else {
				_book.createDrawingGroup();
				dm = _book.getDrawingManager();
			}
		}
		EscherAggregate agg = ((EscherAggregate) (_sheet.findFirstRecordBySid(EscherAggregate.sid)));
		if (null == agg) {
			int pos = _sheet.aggregateDrawingRecords(dm, false);
			if ((-1) == pos) {
				if (createIfMissing) {
					pos = _sheet.aggregateDrawingRecords(dm, true);
					agg = ((EscherAggregate) (_sheet.getRecords().get(pos)));
				}else {
					return null;
				}
			}
			agg = ((EscherAggregate) (_sheet.getRecords().get(pos)));
		}
		return null;
	}

	@Override
	public void setColumnGroupCollapsed(int columnNumber, boolean collapsed) {
		_sheet.setColumnGroupCollapsed(columnNumber, collapsed);
	}

	@Override
	public void groupColumn(int fromColumn, int toColumn) {
		_sheet.groupColumnRange(fromColumn, toColumn, true);
	}

	@Override
	public void ungroupColumn(int fromColumn, int toColumn) {
		_sheet.groupColumnRange(fromColumn, toColumn, false);
	}

	@Override
	public void groupRow(int fromRow, int toRow) {
		_sheet.groupRowRange(fromRow, toRow, true);
	}

	@Override
	public void ungroupRow(int fromRow, int toRow) {
		_sheet.groupRowRange(fromRow, toRow, false);
	}

	@Override
	public void setRowGroupCollapsed(int rowIndex, boolean collapse) {
		if (collapse) {
			_sheet.getRowsAggregate().collapseRow(rowIndex);
		}else {
			_sheet.getRowsAggregate().expandRow(rowIndex);
		}
	}

	@Override
	public void setDefaultColumnStyle(int column, CellStyle style) {
		_sheet.setDefaultColumnStyle(column, style.getIndex());
	}

	@Override
	public void autoSizeColumn(int column) {
		autoSizeColumn(column, false);
	}

	@Override
	public void autoSizeColumn(int column, boolean useMergedCells) {
		double width = SheetUtil.getColumnWidth(this, column, useMergedCells);
		if (width != (-1)) {
			width *= 256;
			int maxColumnWidth = 255 * 256;
			if (width > maxColumnWidth) {
				width = maxColumnWidth;
			}
			setColumnWidth(column, ((int) (width)));
		}
	}

	@Override
	public HSSFComment getCellComment(CellAddress ref) {
		return findCellComment(ref.getRow(), ref.getColumn());
	}

	@Override
	public HSSFHyperlink getHyperlink(int row, int column) {
		for (RecordBase rec : _sheet.getRecords()) {
			if (rec instanceof HyperlinkRecord) {
				HyperlinkRecord link = ((HyperlinkRecord) (rec));
				if (((link.getFirstColumn()) == column) && ((link.getFirstRow()) == row)) {
				}
			}
		}
		return null;
	}

	@Override
	public HSSFHyperlink getHyperlink(CellAddress addr) {
		return getHyperlink(addr.getRow(), addr.getColumn());
	}

	@Override
	public List<HSSFHyperlink> getHyperlinkList() {
		final List<HSSFHyperlink> hyperlinkList = new ArrayList<>();
		for (RecordBase rec : _sheet.getRecords()) {
			if (rec instanceof HyperlinkRecord) {
				HyperlinkRecord link = ((HyperlinkRecord) (rec));
			}
		}
		return hyperlinkList;
	}

	protected void removeHyperlink(HSSFHyperlink link) {
	}

	protected void removeHyperlink(HyperlinkRecord link) {
		for (Iterator<RecordBase> it = _sheet.getRecords().iterator(); it.hasNext();) {
			RecordBase rec = it.next();
			if (rec instanceof HyperlinkRecord) {
				HyperlinkRecord recLink = ((HyperlinkRecord) (rec));
				if (link == recLink) {
					it.remove();
					return;
				}
			}
		}
	}

	@Override
	public HSSFSheetConditionalFormatting getSheetConditionalFormatting() {
		return null;
	}

	@SuppressWarnings("resource")
	@Override
	public String getSheetName() {
		HSSFWorkbook wb = getWorkbook();
		int idx = wb.getSheetIndex(this);
		return wb.getSheetName(idx);
	}

	private CellRange<HSSFCell> getCellRange(CellRangeAddress range) {
		int firstRow = range.getFirstRow();
		int firstColumn = range.getFirstColumn();
		int lastRow = range.getLastRow();
		int lastColumn = range.getLastColumn();
		int height = (lastRow - firstRow) + 1;
		int width = (lastColumn - firstColumn) + 1;
		List<HSSFCell> temp = new ArrayList<>((height * width));
		for (int rowIn = firstRow; rowIn <= lastRow; rowIn++) {
			for (int colIn = firstColumn; colIn <= lastColumn; colIn++) {
				HSSFRow row = getRow(rowIn);
				if (row == null) {
					row = createRow(rowIn);
				}
				HSSFCell cell = row.getCell(colIn);
				if (cell == null) {
					cell = row.createCell(colIn);
				}
				temp.add(cell);
			}
		}
		return SSCellRange.create(firstRow, firstColumn, height, width, temp, HSSFCell.class);
	}

	@Override
	public CellRange<HSSFCell> setArrayFormula(String formula, CellRangeAddress range) {
		int sheetIndex = _workbook.getSheetIndex(this);
		Ptg[] ptgs = HSSFFormulaParser.parse(formula, _workbook, FormulaType.ARRAY, sheetIndex);
		CellRange<HSSFCell> cells = getCellRange(range);
		for (HSSFCell c : cells) {
		}
		HSSFCell mainArrayFormulaCell = cells.getTopLeftCell();
		return cells;
	}

	@Override
	public CellRange<HSSFCell> removeArrayFormula(Cell cell) {
		if ((cell.getSheet()) != (this)) {
			throw new IllegalArgumentException("Specified cell does not belong to this sheet.");
		}
		return null;
	}

	@Override
	public DataValidationHelper getDataValidationHelper() {
		return null;
	}

	@Override
	public HSSFAutoFilter setAutoFilter(CellRangeAddress range) {
		int sheetIndex = _workbook.getSheetIndex(this);
		int firstRow = range.getFirstRow();
		if (firstRow == (-1)) {
			firstRow = 0;
		}
		Area3DPtg ptg = new Area3DPtg(firstRow, range.getLastRow(), range.getFirstColumn(), range.getLastColumn(), false, false, false, false, sheetIndex);
		AutoFilterInfoRecord r = new AutoFilterInfoRecord();
		int numcols = (1 + (range.getLastColumn())) - (range.getFirstColumn());
		r.setNumEntries(((short) (numcols)));
		int idx = _sheet.findFirstRecordLocBySid(DimensionsRecord.sid);
		_sheet.getRecords().add(idx, r);
		HSSFPatriarch p = createDrawingPatriarch();
		final int firstColumn = range.getFirstColumn();
		final int lastColumn = range.getLastColumn();
		for (int col = firstColumn; col <= lastColumn; col++) {
		}
		return null;
	}

	protected HSSFComment findCellComment(int row, int column) {
		HSSFPatriarch patriarch = getDrawingPatriarch();
		if (null == patriarch) {
			patriarch = createDrawingPatriarch();
		}
		return lookForComment(patriarch, row, column);
	}

	private HSSFComment lookForComment(HSSFShapeContainer container, int row, int column) {
		for (Object object : container.getChildren()) {
			HSSFShape shape = ((HSSFShape) (object));
			if (shape instanceof HSSFShapeGroup) {
				HSSFShape res = lookForComment(((HSSFShapeContainer) (shape)), row, column);
				if (null != res) {
					return ((HSSFComment) (res));
				}
				continue;
			}
			if (shape instanceof HSSFComment) {
				HSSFComment comment = ((HSSFComment) (shape));
				if (((comment.hasPosition()) && ((comment.getColumn()) == column)) && ((comment.getRow()) == row)) {
					return comment;
				}
			}
		}
		return null;
	}

	@Override
	public Map<CellAddress, HSSFComment> getCellComments() {
		HSSFPatriarch patriarch = getDrawingPatriarch();
		if (null == patriarch) {
			patriarch = createDrawingPatriarch();
		}
		Map<CellAddress, HSSFComment> locations = new TreeMap<>();
		findCellCommentLocations(patriarch, locations);
		return locations;
	}

	private void findCellCommentLocations(HSSFShapeContainer container, Map<CellAddress, HSSFComment> locations) {
		for (Object object : container.getChildren()) {
			HSSFShape shape = ((HSSFShape) (object));
			if (shape instanceof HSSFShapeGroup) {
				findCellCommentLocations(((HSSFShapeGroup) (shape)), locations);
				continue;
			}
			if (shape instanceof HSSFComment) {
				HSSFComment comment = ((HSSFComment) (shape));
				if (comment.hasPosition()) {
					locations.put(new CellAddress(comment.getRow(), comment.getColumn()), comment);
				}
			}
		}
	}

	@Override
	public CellRangeAddress getRepeatingRows() {
		return getRepeatingRowsOrColums(true);
	}

	@Override
	public CellRangeAddress getRepeatingColumns() {
		return getRepeatingRowsOrColums(false);
	}

	@Override
	public void setRepeatingRows(CellRangeAddress rowRangeRef) {
		CellRangeAddress columnRangeRef = getRepeatingColumns();
		setRepeatingRowsAndColumns(rowRangeRef, columnRangeRef);
	}

	@Override
	public void setRepeatingColumns(CellRangeAddress columnRangeRef) {
		CellRangeAddress rowRangeRef = getRepeatingRows();
		setRepeatingRowsAndColumns(rowRangeRef, columnRangeRef);
	}

	private void setRepeatingRowsAndColumns(CellRangeAddress rowDef, CellRangeAddress colDef) {
		int sheetIndex = _workbook.getSheetIndex(this);
		int maxRowIndex = SpreadsheetVersion.EXCEL97.getLastRowIndex();
		int maxColIndex = SpreadsheetVersion.EXCEL97.getLastColumnIndex();
		int col1 = -1;
		int col2 = -1;
		int row1 = -1;
		int row2 = -1;
		if (rowDef != null) {
			row1 = rowDef.getFirstRow();
			row2 = rowDef.getLastRow();
			if (((((row1 == (-1)) && (row2 != (-1))) || (row1 > row2)) || ((row1 < 0) || (row1 > maxRowIndex))) || ((row2 < 0) || (row2 > maxRowIndex))) {
				throw new IllegalArgumentException("Invalid row range specification");
			}
		}
		if (colDef != null) {
			col1 = colDef.getFirstColumn();
			col2 = colDef.getLastColumn();
			if (((((col1 == (-1)) && (col2 != (-1))) || (col1 > col2)) || ((col1 < 0) || (col1 > maxColIndex))) || ((col2 < 0) || (col2 > maxColIndex))) {
				throw new IllegalArgumentException("Invalid column range specification");
			}
		}
		boolean setBoth = (rowDef != null) && (colDef != null);
		boolean removeAll = (rowDef == null) && (colDef == null);
		if (removeAll) {
			return;
		}
		List<Ptg> ptgList = new ArrayList<>();
		if (setBoth) {
			final int exprsSize = (2 * 11) + 1;
			ptgList.add(new MemFuncPtg(exprsSize));
		}
		if (colDef != null) {
		}
		if (rowDef != null) {
		}
		if (setBoth) {
			ptgList.add(UnionPtg.instance);
		}
		Ptg[] ptgs = new Ptg[ptgList.size()];
		ptgList.toArray(ptgs);
		HSSFPrintSetup printSetup = getPrintSetup();
		printSetup.setValidSettings(false);
		setActive(true);
	}

	private CellRangeAddress getRepeatingRowsOrColums(boolean rows) {
		NameRecord rec = getBuiltinNameRecord(NameRecord.BUILTIN_PRINT_TITLE);
		if (rec == null) {
			return null;
		}
		Ptg[] nameDefinition = rec.getNameDefinition();
		if (nameDefinition == null) {
			return null;
		}
		int maxRowIndex = SpreadsheetVersion.EXCEL97.getLastRowIndex();
		int maxColIndex = SpreadsheetVersion.EXCEL97.getLastColumnIndex();
		for (Ptg ptg : nameDefinition) {
			if (ptg instanceof Area3DPtg) {
				Area3DPtg areaPtg = ((Area3DPtg) (ptg));
				if (((areaPtg.getFirstColumn()) == 0) && ((areaPtg.getLastColumn()) == maxColIndex)) {
					if (rows) {
						return new CellRangeAddress(areaPtg.getFirstRow(), areaPtg.getLastRow(), (-1), (-1));
					}
				}else
					if (((areaPtg.getFirstRow()) == 0) && ((areaPtg.getLastRow()) == maxRowIndex)) {
						if (!rows) {
							return new CellRangeAddress((-1), (-1), areaPtg.getFirstColumn(), areaPtg.getLastColumn());
						}
					}

			}
		}
		return null;
	}

	private NameRecord getBuiltinNameRecord(byte builtinCode) {
		int sheetIndex = _workbook.getSheetIndex(this);
		return null;
	}

	@Override
	public int getColumnOutlineLevel(int columnIndex) {
		return _sheet.getColumnOutlineLevel(columnIndex);
	}

	@Override
	public CellAddress getActiveCell() {
		int row = _sheet.getActiveCellRow();
		int col = _sheet.getActiveCellCol();
		return new CellAddress(row, col);
	}

	@Override
	public void setActiveCell(CellAddress address) {
		int row = address.getRow();
		short col = ((short) (address.getColumn()));
		_sheet.setActiveCellRow(row);
		_sheet.setActiveCellCol(col);
	}
}

