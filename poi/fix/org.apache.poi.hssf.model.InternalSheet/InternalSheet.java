

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.hssf.model.DrawingManager2;
import org.apache.poi.hssf.model.RecordStream;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.CFHeader12Record;
import org.apache.poi.hssf.record.CFHeaderRecord;
import org.apache.poi.hssf.record.CalcCountRecord;
import org.apache.poi.hssf.record.CalcModeRecord;
import org.apache.poi.hssf.record.CellValueRecordInterface;
import org.apache.poi.hssf.record.ColumnInfoRecord;
import org.apache.poi.hssf.record.DVALRecord;
import org.apache.poi.hssf.record.DefaultColWidthRecord;
import org.apache.poi.hssf.record.DefaultRowHeightRecord;
import org.apache.poi.hssf.record.DeltaRecord;
import org.apache.poi.hssf.record.DimensionsRecord;
import org.apache.poi.hssf.record.DrawingRecord;
import org.apache.poi.hssf.record.EOFRecord;
import org.apache.poi.hssf.record.EscherAggregate;
import org.apache.poi.hssf.record.FeatHdrRecord;
import org.apache.poi.hssf.record.FeatRecord;
import org.apache.poi.hssf.record.GridsetRecord;
import org.apache.poi.hssf.record.GutsRecord;
import org.apache.poi.hssf.record.IndexRecord;
import org.apache.poi.hssf.record.IterationRecord;
import org.apache.poi.hssf.record.MergeCellsRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.PaneRecord;
import org.apache.poi.hssf.record.PrintGridlinesRecord;
import org.apache.poi.hssf.record.PrintHeadersRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RecordBase;
import org.apache.poi.hssf.record.RefModeRecord;
import org.apache.poi.hssf.record.RowRecord;
import org.apache.poi.hssf.record.SCLRecord;
import org.apache.poi.hssf.record.SaveRecalcRecord;
import org.apache.poi.hssf.record.SelectionRecord;
import org.apache.poi.hssf.record.UncalcedRecord;
import org.apache.poi.hssf.record.WSBoolRecord;
import org.apache.poi.hssf.record.WindowTwoRecord;
import org.apache.poi.hssf.record.aggregates.ChartSubstreamRecordAggregate;
import org.apache.poi.hssf.record.aggregates.ColumnInfoRecordsAggregate;
import org.apache.poi.hssf.record.aggregates.ConditionalFormattingTable;
import org.apache.poi.hssf.record.aggregates.CustomViewSettingsRecordAggregate;
import org.apache.poi.hssf.record.aggregates.DataValidityTable;
import org.apache.poi.hssf.record.aggregates.MergedCellsTable;
import org.apache.poi.hssf.record.aggregates.PageSettingsBlock;
import org.apache.poi.hssf.record.aggregates.RecordAggregate;
import org.apache.poi.hssf.record.aggregates.RowRecordsAggregate;
import org.apache.poi.hssf.record.aggregates.WorksheetProtectionBlock;
import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.util.Internal;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.RecordFormatException;


@Internal
public final class InternalSheet {
	public static final short LeftMargin = 0;

	public static final short RightMargin = 1;

	public static final short TopMargin = 2;

	public static final short BottomMargin = 3;

	private static POILogger log = POILogFactory.getLogger(InternalSheet.class);

	private List<RecordBase> _records;

	protected PrintGridlinesRecord printGridlines;

	protected PrintHeadersRecord printHeaders;

	protected GridsetRecord gridset;

	private GutsRecord _gutsRecord;

	protected DefaultColWidthRecord defaultcolwidth = new DefaultColWidthRecord();

	protected DefaultRowHeightRecord defaultrowheight = new DefaultRowHeightRecord();

	private PageSettingsBlock _psBlock;

	private final WorksheetProtectionBlock _protectionBlock = new WorksheetProtectionBlock();

	protected WindowTwoRecord windowTwo;

	protected SelectionRecord _selection;

	private final MergedCellsTable _mergedCellsTable;

	ColumnInfoRecordsAggregate _columnInfos;

	private DimensionsRecord _dimensions;

	protected final RowRecordsAggregate _rowsAggregate;

	private DataValidityTable _dataValidityTable;

	private ConditionalFormattingTable condFormatting;

	private Iterator<RowRecord> rowRecIterator;

	protected boolean _isUncalced;

	public static final byte PANE_LOWER_RIGHT = ((byte) (0));

	public static final byte PANE_UPPER_RIGHT = ((byte) (1));

	public static final byte PANE_LOWER_LEFT = ((byte) (2));

	public static final byte PANE_UPPER_LEFT = ((byte) (3));

	public static InternalSheet createSheet(RecordStream rs) {
		return new InternalSheet(rs);
	}

	private InternalSheet(RecordStream rs) {
		_mergedCellsTable = new MergedCellsTable();
		RowRecordsAggregate rra = null;
		List<RecordBase> records = new ArrayList<>(128);
		_records = records;
		int dimsloc = -1;
		if ((rs.peekNextSid()) != (BOFRecord.sid)) {
			throw new RecordFormatException("BOF record expected");
		}
		BOFRecord bof = ((BOFRecord) (rs.getNext()));
		if ((bof.getType()) == (BOFRecord.TYPE_WORKSHEET)) {
		}else
			if (((bof.getType()) == (BOFRecord.TYPE_CHART)) || ((bof.getType()) == (BOFRecord.TYPE_EXCEL_4_MACRO))) {
			}else {
				while (rs.hasNext()) {
					Record rec = rs.getNext();
					if (rec instanceof EOFRecord) {
						break;
					}
				} 
				throw new InternalSheet.UnsupportedBOFType(bof.getType());
			}

		records.add(bof);
		while (rs.hasNext()) {
			int recSid = rs.peekNextSid();
			if ((recSid == (CFHeaderRecord.sid)) || (recSid == (CFHeader12Record.sid))) {
				condFormatting = new ConditionalFormattingTable(rs);
				records.add(condFormatting);
				continue;
			}
			if (recSid == (ColumnInfoRecord.sid)) {
				_columnInfos = new ColumnInfoRecordsAggregate(rs);
				records.add(_columnInfos);
				continue;
			}
			if (recSid == (DVALRecord.sid)) {
				_dataValidityTable = new DataValidityTable(rs);
				records.add(_dataValidityTable);
				continue;
			}
			if (CustomViewSettingsRecordAggregate.isBeginRecord(recSid)) {
				records.add(new CustomViewSettingsRecordAggregate(rs));
				continue;
			}
			if (PageSettingsBlock.isComponentRecord(recSid)) {
				if ((_psBlock) == null) {
					_psBlock = new PageSettingsBlock(rs);
					records.add(_psBlock);
				}else {
					_psBlock.addLateRecords(rs);
				}
				_psBlock.positionRecords(records);
				continue;
			}
			if (WorksheetProtectionBlock.isComponentRecord(recSid)) {
				_protectionBlock.addRecords(rs);
				continue;
			}
			if (recSid == (MergeCellsRecord.sid)) {
				_mergedCellsTable.read(rs);
				continue;
			}
			if (recSid == (BOFRecord.sid)) {
				ChartSubstreamRecordAggregate chartAgg = new ChartSubstreamRecordAggregate(rs);
				InternalSheet.spillAggregate(chartAgg, records);
				continue;
			}
			Record rec = rs.getNext();
			if (recSid == (IndexRecord.sid)) {
				continue;
			}
			if (recSid == (UncalcedRecord.sid)) {
				_isUncalced = true;
				continue;
			}
			if ((recSid == (FeatRecord.sid)) || (recSid == (FeatHdrRecord.sid))) {
				records.add(rec);
				continue;
			}
			if (recSid == (EOFRecord.sid)) {
				records.add(rec);
				break;
			}
			if (recSid == (DimensionsRecord.sid)) {
				if ((_columnInfos) == null) {
					_columnInfos = new ColumnInfoRecordsAggregate();
					records.add(_columnInfos);
				}
				_dimensions = ((DimensionsRecord) (rec));
				dimsloc = records.size();
			}else
				if (recSid == (DefaultColWidthRecord.sid)) {
					defaultcolwidth = ((DefaultColWidthRecord) (rec));
				}else
					if (recSid == (DefaultRowHeightRecord.sid)) {
						defaultrowheight = ((DefaultRowHeightRecord) (rec));
					}else
						if (recSid == (PrintGridlinesRecord.sid)) {
							printGridlines = ((PrintGridlinesRecord) (rec));
						}else
							if (recSid == (PrintHeadersRecord.sid)) {
								printHeaders = ((PrintHeadersRecord) (rec));
							}else
								if (recSid == (GridsetRecord.sid)) {
									gridset = ((GridsetRecord) (rec));
								}else
									if (recSid == (SelectionRecord.sid)) {
										_selection = ((SelectionRecord) (rec));
									}else
										if (recSid == (WindowTwoRecord.sid)) {
											windowTwo = ((WindowTwoRecord) (rec));
										}else
											if (recSid == (GutsRecord.sid)) {
												_gutsRecord = ((GutsRecord) (rec));
											}








			records.add(rec);
		} 
		if ((windowTwo) == null) {
			throw new RecordFormatException("WINDOW2 was not found");
		}
		if ((_dimensions) == null) {
			if (rra == null) {
				rra = new RowRecordsAggregate();
			}else {
				if (InternalSheet.log.check(POILogger.WARN)) {
					InternalSheet.log.log(POILogger.WARN, "DIMENSION record not found even though row/cells present");
				}
			}
			dimsloc = findFirstRecordLocBySid(WindowTwoRecord.sid);
			_dimensions = rra.createDimensions();
			records.add(dimsloc, _dimensions);
		}
		if (rra == null) {
			rra = new RowRecordsAggregate();
			records.add((dimsloc + 1), rra);
		}
		_rowsAggregate = rra;
		if (InternalSheet.log.check(POILogger.DEBUG))
			InternalSheet.log.log(POILogger.DEBUG, "sheet createSheet (existing file) exited");

	}

	private static void spillAggregate(RecordAggregate ra, final List<RecordBase> recs) {
		ra.visitContainedRecords(new RecordAggregate.RecordVisitor() {
			public void visitRecord(Record r) {
				recs.add(r);
			}
		});
	}

	public static class UnsupportedBOFType extends RecordFormatException {
		private final int type;

		protected UnsupportedBOFType(int type) {
			super(("BOF not of a supported type, found " + type));
			this.type = type;
		}

		public int getType() {
			return type;
		}
	}

	private static final class RecordCloner implements RecordAggregate.RecordVisitor {
		private final List<Record> _destList;

		public RecordCloner(List<Record> destList) {
			_destList = destList;
		}

		public void visitRecord(Record r) {
			try {
				_destList.add(((Record) (r.clone())));
			} catch (CloneNotSupportedException e) {
				throw new RecordFormatException(e);
			}
		}
	}

	public InternalSheet cloneSheet() {
		List<Record> clonedRecords = new ArrayList<>(_records.size());
		for (int i = 0; i < (_records.size()); i++) {
			RecordBase rb = _records.get(i);
			if (rb instanceof RecordAggregate) {
				((RecordAggregate) (rb)).visitContainedRecords(new InternalSheet.RecordCloner(clonedRecords));
				continue;
			}
			if (rb instanceof EscherAggregate) {
				rb = new DrawingRecord();
			}
			try {
				Record rec = ((Record) (((Record) (rb)).clone()));
				clonedRecords.add(rec);
			} catch (CloneNotSupportedException e) {
				throw new RecordFormatException(e);
			}
		}
		return InternalSheet.createSheet(new RecordStream(clonedRecords, 0));
	}

	public static InternalSheet createSheet() {
		return new InternalSheet();
	}

	private InternalSheet() {
		_mergedCellsTable = new MergedCellsTable();
		List<RecordBase> records = new ArrayList<>(32);
		if (InternalSheet.log.check(POILogger.DEBUG))
			InternalSheet.log.log(POILogger.DEBUG, "Sheet createsheet from scratch called");

		records.add(InternalSheet.createBOF());
		records.add(InternalSheet.createCalcMode());
		records.add(InternalSheet.createCalcCount());
		records.add(InternalSheet.createRefMode());
		records.add(InternalSheet.createIteration());
		records.add(InternalSheet.createDelta());
		records.add(InternalSheet.createSaveRecalc());
		printHeaders = InternalSheet.createPrintHeaders();
		records.add(printHeaders);
		printGridlines = InternalSheet.createPrintGridlines();
		records.add(printGridlines);
		gridset = InternalSheet.createGridset();
		records.add(gridset);
		_gutsRecord = InternalSheet.createGuts();
		records.add(_gutsRecord);
		defaultrowheight = InternalSheet.createDefaultRowHeight();
		records.add(defaultrowheight);
		records.add(InternalSheet.createWSBool());
		_psBlock = new PageSettingsBlock();
		records.add(_psBlock);
		records.add(_protectionBlock);
		defaultcolwidth = InternalSheet.createDefaultColWidth();
		records.add(defaultcolwidth);
		ColumnInfoRecordsAggregate columns = new ColumnInfoRecordsAggregate();
		records.add(columns);
		_columnInfos = columns;
		_dimensions = InternalSheet.createDimensions();
		records.add(_dimensions);
		_rowsAggregate = new RowRecordsAggregate();
		records.add(_rowsAggregate);
		records.add((windowTwo = InternalSheet.createWindowTwo()));
		_selection = InternalSheet.createSelection();
		records.add(_selection);
		records.add(_mergedCellsTable);
		records.add(EOFRecord.instance);
		_records = records;
		if (InternalSheet.log.check(POILogger.DEBUG))
			InternalSheet.log.log(POILogger.DEBUG, "Sheet createsheet from scratch exit");

	}

	public RowRecordsAggregate getRowsAggregate() {
		return _rowsAggregate;
	}

	private MergedCellsTable getMergedRecords() {
		return _mergedCellsTable;
	}

	public void updateFormulasAfterCellShift(FormulaShifter shifter, int externSheetIndex) {
		getRowsAggregate().updateFormulasAfterRowShift(shifter, externSheetIndex);
		if ((condFormatting) != null) {
			getConditionalFormattingTable().updateFormulasAfterCellShift(shifter, externSheetIndex);
		}
	}

	public int addMergedRegion(int rowFrom, int colFrom, int rowTo, int colTo) {
		if (rowTo < rowFrom) {
			throw new IllegalArgumentException((((("The 'to' row (" + rowTo) + ") must not be less than the 'from' row (") + rowFrom) + ")"));
		}
		if (colTo < colFrom) {
			throw new IllegalArgumentException((((("The 'to' col (" + colTo) + ") must not be less than the 'from' col (") + colFrom) + ")"));
		}
		MergedCellsTable mrt = getMergedRecords();
		mrt.addArea(rowFrom, colFrom, rowTo, colTo);
		return (mrt.getNumberOfMergedRegions()) - 1;
	}

	public void removeMergedRegion(int index) {
		MergedCellsTable mrt = getMergedRecords();
		if (index >= (mrt.getNumberOfMergedRegions())) {
			return;
		}
		mrt.remove(index);
	}

	public CellRangeAddress getMergedRegionAt(int index) {
		MergedCellsTable mrt = getMergedRecords();
		if (index >= (mrt.getNumberOfMergedRegions())) {
			return null;
		}
		return mrt.get(index);
	}

	public int getNumMergedRegions() {
		return getMergedRecords().getNumberOfMergedRegions();
	}

	public ConditionalFormattingTable getConditionalFormattingTable() {
		if ((condFormatting) == null) {
			condFormatting = new ConditionalFormattingTable();
		}
		return condFormatting;
	}

	public void setDimensions(int firstrow, short firstcol, int lastrow, short lastcol) {
		if (InternalSheet.log.check(POILogger.DEBUG)) {
			InternalSheet.log.log(POILogger.DEBUG, "Sheet.setDimensions");
			InternalSheet.log.log(POILogger.DEBUG, new StringBuffer("firstrow").append(firstrow).append("firstcol").append(firstcol).append("lastrow").append(lastrow).append("lastcol").append(lastcol).toString());
		}
		_dimensions.setFirstCol(firstcol);
		_dimensions.setFirstRow(firstrow);
		_dimensions.setLastCol(lastcol);
		_dimensions.setLastRow(lastrow);
		if (InternalSheet.log.check(POILogger.DEBUG))
			InternalSheet.log.log(POILogger.DEBUG, "Sheet.setDimensions exiting");

	}

	public void visitContainedRecords(RecordAggregate.RecordVisitor rv, int offset) {
		RecordAggregate.PositionTrackingVisitor ptv = new RecordAggregate.PositionTrackingVisitor(rv, offset);
		boolean haveSerializedIndex = false;
		for (int k = 0; k < (_records.size()); k++) {
			RecordBase record = _records.get(k);
			if (record instanceof RecordAggregate) {
				RecordAggregate agg = ((RecordAggregate) (record));
				agg.visitContainedRecords(ptv);
			}else {
				ptv.visitRecord(((Record) (record)));
			}
			if (record instanceof BOFRecord) {
				if (!haveSerializedIndex) {
					haveSerializedIndex = true;
					if (_isUncalced) {
						ptv.visitRecord(new UncalcedRecord());
					}
					if ((_rowsAggregate) != null) {
						int initRecsSize = getSizeOfInitialSheetRecords(k);
						int currentPos = ptv.getPosition();
						ptv.visitRecord(_rowsAggregate.createIndexRecord(currentPos, initRecsSize));
					}
				}
			}
		}
	}

	private int getSizeOfInitialSheetRecords(int bofRecordIndex) {
		int result = 0;
		for (int j = bofRecordIndex + 1; j < (_records.size()); j++) {
			RecordBase tmpRec = _records.get(j);
			if (tmpRec instanceof RowRecordsAggregate) {
				break;
			}
			result += tmpRec.getRecordSize();
		}
		if (_isUncalced) {
			result += UncalcedRecord.getStaticRecordSize();
		}
		return result;
	}

	public void addValueRecord(int row, CellValueRecordInterface col) {
		if (InternalSheet.log.check(POILogger.DEBUG)) {
			InternalSheet.log.log(POILogger.DEBUG, ("add value record  row" + row));
		}
		DimensionsRecord d = _dimensions;
		if ((col.getColumn()) >= (d.getLastCol())) {
			d.setLastCol(((short) ((col.getColumn()) + 1)));
		}
		if ((col.getColumn()) < (d.getFirstCol())) {
			d.setFirstCol(col.getColumn());
		}
		_rowsAggregate.insertCell(col);
	}

	public void removeValueRecord(int row, CellValueRecordInterface col) {
		InternalSheet.log.log(POILogger.DEBUG, ("remove value record row " + row));
		_rowsAggregate.removeCell(col);
	}

	public void replaceValueRecord(CellValueRecordInterface newval) {
		if (InternalSheet.log.check(POILogger.DEBUG))
			InternalSheet.log.log(POILogger.DEBUG, "replaceValueRecord ");

		_rowsAggregate.removeCell(newval);
		_rowsAggregate.insertCell(newval);
	}

	public void addRow(RowRecord row) {
		if (InternalSheet.log.check(POILogger.DEBUG))
			InternalSheet.log.log(POILogger.DEBUG, "addRow ");

		DimensionsRecord d = _dimensions;
		if ((row.getRowNumber()) >= (d.getLastRow())) {
			d.setLastRow(((row.getRowNumber()) + 1));
		}
		if ((row.getRowNumber()) < (d.getFirstRow())) {
			d.setFirstRow(row.getRowNumber());
		}
		RowRecord existingRow = _rowsAggregate.getRow(row.getRowNumber());
		if (existingRow != null) {
			_rowsAggregate.removeRow(existingRow);
		}
		_rowsAggregate.insertRow(row);
		if (InternalSheet.log.check(POILogger.DEBUG))
			InternalSheet.log.log(POILogger.DEBUG, "exit addRow");

	}

	public void removeRow(RowRecord row) {
		_rowsAggregate.removeRow(row);
	}

	public Iterator<CellValueRecordInterface> getCellValueIterator() {
		return _rowsAggregate.getCellValueIterator();
	}

	public RowRecord getNextRow() {
		if ((rowRecIterator) == null) {
			rowRecIterator = _rowsAggregate.getIterator();
		}
		if (!(rowRecIterator.hasNext())) {
			return null;
		}
		return rowRecIterator.next();
	}

	public RowRecord getRow(int rownum) {
		return _rowsAggregate.getRow(rownum);
	}

	static BOFRecord createBOF() {
		BOFRecord retval = new BOFRecord();
		retval.setVersion(((short) (1536)));
		retval.setType(((short) (16)));
		retval.setBuild(((short) (3515)));
		retval.setBuildYear(((short) (1996)));
		retval.setHistoryBitMask(193);
		retval.setRequiredVersion(6);
		return retval;
	}

	private static CalcModeRecord createCalcMode() {
		CalcModeRecord retval = new CalcModeRecord();
		retval.setCalcMode(((short) (1)));
		return retval;
	}

	private static CalcCountRecord createCalcCount() {
		CalcCountRecord retval = new CalcCountRecord();
		retval.setIterations(((short) (100)));
		return retval;
	}

	private static RefModeRecord createRefMode() {
		RefModeRecord retval = new RefModeRecord();
		retval.setMode(RefModeRecord.USE_A1_MODE);
		return retval;
	}

	private static IterationRecord createIteration() {
		return new IterationRecord(false);
	}

	private static DeltaRecord createDelta() {
		return new DeltaRecord(DeltaRecord.DEFAULT_VALUE);
	}

	private static SaveRecalcRecord createSaveRecalc() {
		SaveRecalcRecord retval = new SaveRecalcRecord();
		retval.setRecalc(true);
		return retval;
	}

	private static PrintHeadersRecord createPrintHeaders() {
		PrintHeadersRecord retval = new PrintHeadersRecord();
		retval.setPrintHeaders(false);
		return retval;
	}

	private static PrintGridlinesRecord createPrintGridlines() {
		PrintGridlinesRecord retval = new PrintGridlinesRecord();
		retval.setPrintGridlines(false);
		return retval;
	}

	private static GridsetRecord createGridset() {
		GridsetRecord retval = new GridsetRecord();
		retval.setGridset(true);
		return retval;
	}

	private static GutsRecord createGuts() {
		GutsRecord retval = new GutsRecord();
		retval.setLeftRowGutter(((short) (0)));
		retval.setTopColGutter(((short) (0)));
		retval.setRowLevelMax(((short) (0)));
		retval.setColLevelMax(((short) (0)));
		return retval;
	}

	private GutsRecord getGutsRecord() {
		if ((_gutsRecord) == null) {
			GutsRecord result = InternalSheet.createGuts();
			_gutsRecord = result;
		}
		return _gutsRecord;
	}

	private static DefaultRowHeightRecord createDefaultRowHeight() {
		DefaultRowHeightRecord retval = new DefaultRowHeightRecord();
		retval.setOptionFlags(((short) (0)));
		retval.setRowHeight(DefaultRowHeightRecord.DEFAULT_ROW_HEIGHT);
		return retval;
	}

	private static WSBoolRecord createWSBool() {
		WSBoolRecord retval = new WSBoolRecord();
		retval.setWSBool1(((byte) (4)));
		retval.setWSBool2(((byte) (-63)));
		return retval;
	}

	private static DefaultColWidthRecord createDefaultColWidth() {
		DefaultColWidthRecord retval = new DefaultColWidthRecord();
		retval.setColWidth(DefaultColWidthRecord.DEFAULT_COLUMN_WIDTH);
		return retval;
	}

	public int getDefaultColumnWidth() {
		return defaultcolwidth.getColWidth();
	}

	public boolean isGridsPrinted() {
		if ((gridset) == null) {
			gridset = InternalSheet.createGridset();
			int loc = findFirstRecordLocBySid(EOFRecord.sid);
			_records.add(loc, gridset);
		}
		return !(gridset.getGridset());
	}

	public void setGridsPrinted(boolean value) {
		gridset.setGridset((!value));
	}

	public void setDefaultColumnWidth(int dcw) {
		defaultcolwidth.setColWidth(dcw);
	}

	public void setDefaultRowHeight(short dch) {
		defaultrowheight.setRowHeight(dch);
		defaultrowheight.setOptionFlags(((short) (1)));
	}

	public short getDefaultRowHeight() {
		return defaultrowheight.getRowHeight();
	}

	public int getColumnWidth(int columnIndex) {
		ColumnInfoRecord ci = _columnInfos.findColumnInfo(columnIndex);
		if (ci != null) {
			return ci.getColumnWidth();
		}
		return 256 * (defaultcolwidth.getColWidth());
	}

	public short getXFIndexForColAt(short columnIndex) {
		ColumnInfoRecord ci = _columnInfos.findColumnInfo(columnIndex);
		if (ci != null) {
			return ((short) (ci.getXFIndex()));
		}
		return 15;
	}

	public void setColumnWidth(int column, int width) {
		if (width > (255 * 256))
			throw new IllegalArgumentException("The maximum column width for an individual cell is 255 characters.");

		setColumn(column, null, Integer.valueOf(width), null, null, null);
	}

	public boolean isColumnHidden(int columnIndex) {
		ColumnInfoRecord cir = _columnInfos.findColumnInfo(columnIndex);
		if (cir == null) {
			return false;
		}
		return cir.getHidden();
	}

	public void setColumnHidden(int column, boolean hidden) {
		setColumn(column, null, null, null, Boolean.valueOf(hidden), null);
	}

	public void setDefaultColumnStyle(int column, int styleIndex) {
		setColumn(column, Short.valueOf(((short) (styleIndex))), null, null, null, null);
	}

	private void setColumn(int column, Short xfStyle, Integer width, Integer level, Boolean hidden, Boolean collapsed) {
		_columnInfos.setColumn(column, xfStyle, width, level, hidden, collapsed);
	}

	public void groupColumnRange(int fromColumn, int toColumn, boolean indent) {
		_columnInfos.groupColumnRange(fromColumn, toColumn, indent);
		int maxLevel = _columnInfos.getMaxOutlineLevel();
		GutsRecord guts = getGutsRecord();
		guts.setColLevelMax(((short) (maxLevel + 1)));
		if (maxLevel == 0) {
			guts.setTopColGutter(((short) (0)));
		}else {
			guts.setTopColGutter(((short) (29 + (12 * (maxLevel - 1)))));
		}
	}

	private static DimensionsRecord createDimensions() {
		DimensionsRecord retval = new DimensionsRecord();
		retval.setFirstCol(((short) (0)));
		retval.setLastRow(1);
		retval.setFirstRow(0);
		retval.setLastCol(((short) (1)));
		return retval;
	}

	private static WindowTwoRecord createWindowTwo() {
		WindowTwoRecord retval = new WindowTwoRecord();
		retval.setOptions(((short) (1718)));
		retval.setTopRow(((short) (0)));
		retval.setLeftCol(((short) (0)));
		retval.setHeaderColor(64);
		retval.setPageBreakZoom(((short) (0)));
		retval.setNormalZoom(((short) (0)));
		return retval;
	}

	private static SelectionRecord createSelection() {
		return new SelectionRecord(0, 0);
	}

	public short getTopRow() {
		return (windowTwo) == null ? ((short) (0)) : windowTwo.getTopRow();
	}

	public void setTopRow(short topRow) {
		if ((windowTwo) != null) {
			windowTwo.setTopRow(topRow);
		}
	}

	public void setLeftCol(short leftCol) {
		if ((windowTwo) != null) {
			windowTwo.setLeftCol(leftCol);
		}
	}

	public short getLeftCol() {
		return (windowTwo) == null ? ((short) (0)) : windowTwo.getLeftCol();
	}

	public int getActiveCellRow() {
		if ((_selection) == null) {
			return 0;
		}
		return _selection.getActiveCellRow();
	}

	public void setActiveCellRow(int row) {
		if ((_selection) != null) {
			_selection.setActiveCellRow(row);
		}
	}

	public short getActiveCellCol() {
		if ((_selection) == null) {
			return 0;
		}
		return ((short) (_selection.getActiveCellCol()));
	}

	public void setActiveCellCol(short col) {
		if ((_selection) != null) {
			_selection.setActiveCellCol(col);
		}
	}

	public List<RecordBase> getRecords() {
		return _records;
	}

	public GridsetRecord getGridsetRecord() {
		return gridset;
	}

	public Record findFirstRecordBySid(short sid) {
		int ix = findFirstRecordLocBySid(sid);
		if (ix < 0) {
			return null;
		}
		return ((Record) (_records.get(ix)));
	}

	public void setSCLRecord(SCLRecord sclRecord) {
		int oldRecordLoc = findFirstRecordLocBySid(SCLRecord.sid);
		if (oldRecordLoc == (-1)) {
			int windowRecordLoc = findFirstRecordLocBySid(WindowTwoRecord.sid);
			_records.add((windowRecordLoc + 1), sclRecord);
		}else {
			_records.set(oldRecordLoc, sclRecord);
		}
	}

	public int findFirstRecordLocBySid(short sid) {
		int max = _records.size();
		for (int i = 0; i < max; i++) {
			Object rb = _records.get(i);
			if (!(rb instanceof Record)) {
				continue;
			}
			Record record = ((Record) (rb));
			if ((record.getSid()) == sid) {
				return i;
			}
		}
		return -1;
	}

	public WindowTwoRecord getWindowTwo() {
		return windowTwo;
	}

	public PrintGridlinesRecord getPrintGridlines() {
		return printGridlines;
	}

	public void setPrintGridlines(PrintGridlinesRecord newPrintGridlines) {
		printGridlines = newPrintGridlines;
	}

	public PrintHeadersRecord getPrintHeaders() {
		return printHeaders;
	}

	public void setPrintHeaders(PrintHeadersRecord newPrintHeaders) {
		printHeaders = newPrintHeaders;
	}

	public void setSelected(boolean sel) {
		windowTwo.setSelected(sel);
	}

	public void createFreezePane(int colSplit, int rowSplit, int topRow, int leftmostColumn) {
		int paneLoc = findFirstRecordLocBySid(PaneRecord.sid);
		if (paneLoc != (-1))
			_records.remove(paneLoc);

		if ((colSplit == 0) && (rowSplit == 0)) {
			windowTwo.setFreezePanes(false);
			windowTwo.setFreezePanesNoSplit(false);
			SelectionRecord sel = ((SelectionRecord) (findFirstRecordBySid(SelectionRecord.sid)));
			if (sel != null) {
				sel.setPane(PaneInformation.PANE_UPPER_LEFT);
			}
			return;
		}
		int loc = findFirstRecordLocBySid(WindowTwoRecord.sid);
		PaneRecord pane = new PaneRecord();
		pane.setX(((short) (colSplit)));
		pane.setY(((short) (rowSplit)));
		pane.setTopRow(((short) (topRow)));
		pane.setLeftColumn(((short) (leftmostColumn)));
		if (rowSplit == 0) {
			pane.setTopRow(((short) (0)));
			pane.setActivePane(((short) (1)));
		}else
			if (colSplit == 0) {
				pane.setLeftColumn(((short) (0)));
				pane.setActivePane(((short) (2)));
			}else {
				pane.setActivePane(((short) (0)));
			}

		_records.add((loc + 1), pane);
		windowTwo.setFreezePanes(true);
		windowTwo.setFreezePanesNoSplit(true);
		SelectionRecord sel = ((SelectionRecord) (findFirstRecordBySid(SelectionRecord.sid)));
		if (sel != null) {
			sel.setPane(((byte) (pane.getActivePane())));
		}
	}

	public void createSplitPane(int xSplitPos, int ySplitPos, int topRow, int leftmostColumn, int activePane) {
		int paneLoc = findFirstRecordLocBySid(PaneRecord.sid);
		if (paneLoc != (-1))
			_records.remove(paneLoc);

		int loc = findFirstRecordLocBySid(WindowTwoRecord.sid);
		PaneRecord r = new PaneRecord();
		r.setX(((short) (xSplitPos)));
		r.setY(((short) (ySplitPos)));
		r.setTopRow(((short) (topRow)));
		r.setLeftColumn(((short) (leftmostColumn)));
		r.setActivePane(((short) (activePane)));
		_records.add((loc + 1), r);
		windowTwo.setFreezePanes(false);
		windowTwo.setFreezePanesNoSplit(false);
		SelectionRecord sel = ((SelectionRecord) (findFirstRecordBySid(SelectionRecord.sid)));
		if (sel != null) {
			sel.setPane(InternalSheet.PANE_LOWER_RIGHT);
		}
	}

	public PaneInformation getPaneInformation() {
		PaneRecord rec = ((PaneRecord) (findFirstRecordBySid(PaneRecord.sid)));
		if (rec == null)
			return null;

		return new PaneInformation(rec.getX(), rec.getY(), rec.getTopRow(), rec.getLeftColumn(), ((byte) (rec.getActivePane())), windowTwo.getFreezePanes());
	}

	public SelectionRecord getSelection() {
		return _selection;
	}

	public void setSelection(SelectionRecord selection) {
		_selection = selection;
	}

	public WorksheetProtectionBlock getProtectionBlock() {
		return _protectionBlock;
	}

	public void setDisplayGridlines(boolean show) {
		windowTwo.setDisplayGridlines(show);
	}

	public boolean isDisplayGridlines() {
		return windowTwo.getDisplayGridlines();
	}

	public void setDisplayFormulas(boolean show) {
		windowTwo.setDisplayFormulas(show);
	}

	public boolean isDisplayFormulas() {
		return windowTwo.getDisplayFormulas();
	}

	public void setDisplayRowColHeadings(boolean show) {
		windowTwo.setDisplayRowColHeadings(show);
	}

	public boolean isDisplayRowColHeadings() {
		return windowTwo.getDisplayRowColHeadings();
	}

	public void setPrintRowColHeadings(boolean show) {
		windowTwo.setDisplayRowColHeadings(show);
	}

	public boolean isPrintRowColHeadings() {
		return windowTwo.getDisplayRowColHeadings();
	}

	public boolean getUncalced() {
		return _isUncalced;
	}

	public void setUncalced(boolean uncalced) {
		this._isUncalced = uncalced;
	}

	public int aggregateDrawingRecords(DrawingManager2 drawingManager, boolean createIfMissing) {
		int loc = findFirstRecordLocBySid(DrawingRecord.sid);
		boolean noDrawingRecordsFound = loc == (-1);
		if (noDrawingRecordsFound) {
			if (!createIfMissing) {
				return -1;
			}
			EscherAggregate aggregate = new EscherAggregate(true);
			loc = findFirstRecordLocBySid(EscherAggregate.sid);
			if (loc == (-1)) {
				loc = findFirstRecordLocBySid(WindowTwoRecord.sid);
			}else {
				getRecords().remove(loc);
			}
			getRecords().add(loc, aggregate);
			return loc;
		}
		List<RecordBase> records = getRecords();
		EscherAggregate.createAggregate(records, loc);
		return loc;
	}

	public void preSerialize() {
		for (RecordBase r : getRecords()) {
			if (r instanceof EscherAggregate) {
				r.getRecordSize();
			}
		}
	}

	public PageSettingsBlock getPageSettings() {
		if ((_psBlock) == null) {
			_psBlock = new PageSettingsBlock();
		}
		return _psBlock;
	}

	public void setColumnGroupCollapsed(int columnNumber, boolean collapsed) {
		if (collapsed) {
			_columnInfos.collapseColumn(columnNumber);
		}else {
			_columnInfos.expandColumn(columnNumber);
		}
	}

	public void groupRowRange(int fromRow, int toRow, boolean indent) {
		for (int rowNum = fromRow; rowNum <= toRow; rowNum++) {
			RowRecord row = getRow(rowNum);
			if (row == null) {
				row = RowRecordsAggregate.createRow(rowNum);
				addRow(row);
			}
			int level = row.getOutlineLevel();
			if (indent)
				level++;
			else
				level--;

			level = Math.max(0, level);
			level = Math.min(7, level);
			row.setOutlineLevel(((short) (level)));
		}
		recalcRowGutter();
	}

	private void recalcRowGutter() {
		int maxLevel = 0;
		Iterator<RowRecord> iterator = _rowsAggregate.getIterator();
		while (iterator.hasNext()) {
			RowRecord rowRecord = iterator.next();
			maxLevel = Math.max(rowRecord.getOutlineLevel(), maxLevel);
		} 
		GutsRecord guts = getGutsRecord();
		guts.setRowLevelMax(((short) (maxLevel + 1)));
		guts.setLeftRowGutter(((short) (29 + (12 * maxLevel))));
	}

	public DataValidityTable getOrCreateDataValidityTable() {
		if ((_dataValidityTable) == null) {
			DataValidityTable result = new DataValidityTable();
			_dataValidityTable = result;
		}
		return _dataValidityTable;
	}

	public NoteRecord[] getNoteRecords() {
		List<NoteRecord> temp = new ArrayList<>();
		for (int i = (_records.size()) - 1; i >= 0; i--) {
			RecordBase rec = _records.get(i);
			if (rec instanceof NoteRecord) {
				temp.add(((NoteRecord) (rec)));
			}
		}
		if ((temp.size()) < 1) {
			return NoteRecord.EMPTY_ARRAY;
		}
		NoteRecord[] result = new NoteRecord[temp.size()];
		temp.toArray(result);
		return result;
	}

	public int getColumnOutlineLevel(int columnIndex) {
		return _columnInfos.getOutlineLevel(columnIndex);
	}

	public int getMinColumnIndex() {
		return _columnInfos.getMinColumnIndex();
	}

	public int getMaxColumnIndex() {
		return _columnInfos.getMaxColumnIndex();
	}
}

