

import com.microsoft.schemas.vml.CTShape;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLRelation;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.PartAlreadyExistsException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.FormulaShifter;
import org.apache.poi.ss.formula.SheetNameFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.CellRange;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.IgnoredErrorType;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Table;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressBase;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.ss.util.SSCellRange;
import org.apache.poi.ss.util.SheetUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.model.CommentsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.BaseXSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFAutoFilter;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFactory;
import org.apache.poi.xssf.usermodel.XSSFHeaderFooterProperties;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFPivotCacheDefinition;
import org.apache.poi.xssf.usermodel.XSSFPivotCacheRecords;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFPrintSetup;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheetConditionalFormatting;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFVMLDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.helpers.ColumnHelper;
import org.apache.poi.xssf.usermodel.helpers.XSSFIgnoredErrorHelper;
import org.apache.poi.xssf.usermodel.helpers.XSSFPasswordHelper;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTAutoFilter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBreak;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCalcPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellFormula;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCols;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTComment;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCommentList;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTComments;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDataValidation;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDataValidations;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTHeaderFooter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTHyperlinks;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTIgnoredError;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTIgnoredErrors;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTLegacyDrawing;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMergeCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMergeCells;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTOleObject;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTOleObjects;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTOutlinePr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPageBreak;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPageMargins;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPageSetUpPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPane;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPivotCacheDefinition;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPrintOptions;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRow;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSelection;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetCalcPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetData;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetDimension;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetFormatPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetView;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetViews;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTablePart;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableParts;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCalcMode;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellFormulaType;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STPane;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STPaneState;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.WorksheetDocument;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol.Factory.newInstance;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTOleObjects.Factory.parse;


@SuppressWarnings("deprecation")
public class XSSFSheet extends POIXMLDocumentPart implements Sheet {
	private static final POILogger logger = POILogFactory.getLogger(XSSFSheet.class);

	private static final double DEFAULT_ROW_HEIGHT = 15.0;

	private static final double DEFAULT_MARGIN_HEADER = 0.3;

	private static final double DEFAULT_MARGIN_FOOTER = 0.3;

	private static final double DEFAULT_MARGIN_TOP = 0.75;

	private static final double DEFAULT_MARGIN_BOTTOM = 0.75;

	private static final double DEFAULT_MARGIN_LEFT = 0.7;

	private static final double DEFAULT_MARGIN_RIGHT = 0.7;

	public static final int TWIPS_PER_POINT = 20;

	protected CTSheet sheet;

	protected CTWorksheet worksheet;

	private final SortedMap<Integer, XSSFRow> _rows = new TreeMap<>();

	private List<XSSFHyperlink> hyperlinks;

	private ColumnHelper columnHelper;

	private CommentsTable sheetComments;

	private Map<Integer, CTCellFormula> sharedFormulas;

	private SortedMap<String, XSSFTable> tables;

	private List<CellRangeAddress> arrayFormulas;

	private XSSFDataValidationHelper dataValidationHelper;

	protected XSSFSheet() {
		super();
		onDocumentCreate();
	}

	protected XSSFSheet(PackagePart part) {
		super(part);
	}

	@Override
	public XSSFWorkbook getWorkbook() {
		return ((XSSFWorkbook) (getParent()));
	}

	@Override
	protected void onDocumentRead() {
		try {
			read(getPackagePart().getInputStream());
		} catch (IOException e) {
			throw new POIXMLException(e);
		}
	}

	protected void read(InputStream is) throws IOException {
		try {
			worksheet = WorksheetDocument.Factory.parse(is, POIXMLTypeLoader.DEFAULT_XML_OPTIONS).getWorksheet();
		} catch (XmlException e) {
			throw new POIXMLException(e);
		}
		initRows(worksheet);
		columnHelper = new ColumnHelper(worksheet);
		for (POIXMLDocumentPart.RelationPart rp : getRelationParts()) {
			POIXMLDocumentPart p = rp.getDocumentPart();
			if (p instanceof CommentsTable) {
				sheetComments = ((CommentsTable) (p));
			}
			if (p instanceof XSSFTable) {
				tables.put(rp.getRelationship().getId(), ((XSSFTable) (p)));
			}
			if (p instanceof XSSFPivotTable) {
				getWorkbook().getPivotTables().add(((XSSFPivotTable) (p)));
			}
		}
		initHyperlinks();
	}

	@Override
	protected void onDocumentCreate() {
		worksheet = XSSFSheet.newSheet();
		initRows(worksheet);
		columnHelper = new ColumnHelper(worksheet);
		hyperlinks = new ArrayList<>();
	}

	private void initRows(CTWorksheet worksheetParam) {
		_rows.clear();
		tables = new TreeMap<>();
		sharedFormulas = new HashMap<>();
		arrayFormulas = new ArrayList<>();
		for (CTRow row : worksheetParam.getSheetData().getRowArray()) {
		}
	}

	private void initHyperlinks() {
		hyperlinks = new ArrayList<>();
		if (!(worksheet.isSetHyperlinks())) {
			return;
		}
		try {
			PackageRelationshipCollection hyperRels = getPackagePart().getRelationshipsByType(XSSFRelation.SHEET_HYPERLINKS.getRelation());
			for (CTHyperlink hyperlink : worksheet.getHyperlinks().getHyperlinkArray()) {
				PackageRelationship hyperRel = null;
				if ((hyperlink.getId()) != null) {
					hyperRel = hyperRels.getRelationshipByID(hyperlink.getId());
				}
			}
		} catch (InvalidFormatException e) {
			throw new POIXMLException(e);
		}
	}

	private static CTWorksheet newSheet() {
		CTWorksheet worksheet = CTWorksheet.Factory.newInstance();
		CTSheetFormatPr ctFormat = worksheet.addNewSheetFormatPr();
		ctFormat.setDefaultRowHeight(XSSFSheet.DEFAULT_ROW_HEIGHT);
		CTSheetView ctView = worksheet.addNewSheetViews().addNewSheetView();
		ctView.setWorkbookViewId(0);
		worksheet.addNewDimension().setRef("A1");
		worksheet.addNewSheetData();
		CTPageMargins ctMargins = worksheet.addNewPageMargins();
		ctMargins.setBottom(XSSFSheet.DEFAULT_MARGIN_BOTTOM);
		ctMargins.setFooter(XSSFSheet.DEFAULT_MARGIN_FOOTER);
		ctMargins.setHeader(XSSFSheet.DEFAULT_MARGIN_HEADER);
		ctMargins.setLeft(XSSFSheet.DEFAULT_MARGIN_LEFT);
		ctMargins.setRight(XSSFSheet.DEFAULT_MARGIN_RIGHT);
		ctMargins.setTop(XSSFSheet.DEFAULT_MARGIN_TOP);
		return worksheet;
	}

	@org.apache.poi.util.Internal
	public CTWorksheet getCTWorksheet() {
		return this.worksheet;
	}

	public ColumnHelper getColumnHelper() {
		return columnHelper;
	}

	@Override
	public String getSheetName() {
		return sheet.getName();
	}

	@Override
	public int addMergedRegion(CellRangeAddress region) {
		return addMergedRegion(region, true);
	}

	@Override
	public int addMergedRegionUnsafe(CellRangeAddress region) {
		return addMergedRegion(region, false);
	}

	private int addMergedRegion(CellRangeAddress region, boolean validate) {
		if ((region.getNumberOfCells()) < 2) {
			throw new IllegalArgumentException((("Merged region " + (region.formatAsString())) + " must contain 2 or more cells"));
		}
		region.validate(SpreadsheetVersion.EXCEL2007);
		if (validate) {
			validateArrayFormulas(region);
			validateMergedRegions(region);
		}
		CTMergeCells ctMergeCells = (worksheet.isSetMergeCells()) ? worksheet.getMergeCells() : worksheet.addNewMergeCells();
		CTMergeCell ctMergeCell = ctMergeCells.addNewMergeCell();
		ctMergeCell.setRef(region.formatAsString());
		return ctMergeCells.sizeOfMergeCellArray();
	}

	private void validateArrayFormulas(CellRangeAddress region) {
		int firstRow = region.getFirstRow();
		int firstColumn = region.getFirstColumn();
		int lastRow = region.getLastRow();
		int lastColumn = region.getLastColumn();
		for (int rowIn = firstRow; rowIn <= lastRow; rowIn++) {
			XSSFRow row = getRow(rowIn);
			if (row == null) {
				continue;
			}
			for (int colIn = firstColumn; colIn <= lastColumn; colIn++) {
				XSSFCell cell = row.getCell(colIn);
				if (cell == null) {
					continue;
				}
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
	public void validateMergedRegions() {
		checkForMergedRegionsIntersectingArrayFormulas();
		checkForIntersectingMergedRegions();
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
			columnHelper.setColBestFit(column, true);
		}
	}

	@Override
	public XSSFDrawing getDrawingPatriarch() {
		CTDrawing ctDrawing = getCTDrawing();
		if (ctDrawing != null) {
			for (POIXMLDocumentPart.RelationPart rp : getRelationParts()) {
				POIXMLDocumentPart p = rp.getDocumentPart();
				if (p instanceof XSSFDrawing) {
					XSSFDrawing dr = ((XSSFDrawing) (p));
					String drId = rp.getRelationship().getId();
					if (drId.equals(ctDrawing.getId())) {
						return dr;
					}
					break;
				}
			}
			XSSFSheet.logger.log(POILogger.ERROR, (("Can't find drawing with id=" + (ctDrawing.getId())) + " in the list of the sheet's relationships"));
		}
		return null;
	}

	@Override
	public XSSFDrawing createDrawingPatriarch() {
		CTDrawing ctDrawing = getCTDrawing();
		if (ctDrawing != null) {
			return getDrawingPatriarch();
		}
		int drawingNumber = (getPackagePart().getPackage().getPartsByContentType(XSSFRelation.DRAWINGS.getContentType()).size()) + 1;
		drawingNumber = getNextPartNumber(XSSFRelation.DRAWINGS, drawingNumber);
		POIXMLDocumentPart.RelationPart rp = createRelationship(XSSFRelation.DRAWINGS, XSSFFactory.getInstance(), drawingNumber, false);
		XSSFDrawing drawing = rp.getDocumentPart();
		String relId = rp.getRelationship().getId();
		ctDrawing = worksheet.addNewDrawing();
		ctDrawing.setId(relId);
		return drawing;
	}

	protected XSSFVMLDrawing getVMLDrawing(boolean autoCreate) {
		XSSFVMLDrawing drawing = null;
		CTLegacyDrawing ctDrawing = getCTLegacyDrawing();
		if (ctDrawing == null) {
			if (autoCreate) {
				int drawingNumber = (getPackagePart().getPackage().getPartsByContentType(XSSFRelation.VML_DRAWINGS.getContentType()).size()) + 1;
				POIXMLDocumentPart.RelationPart rp = createRelationship(XSSFRelation.VML_DRAWINGS, XSSFFactory.getInstance(), drawingNumber, false);
				drawing = rp.getDocumentPart();
				String relId = rp.getRelationship().getId();
				ctDrawing = worksheet.addNewLegacyDrawing();
				ctDrawing.setId(relId);
			}
		}else {
			final String id = ctDrawing.getId();
			for (POIXMLDocumentPart.RelationPart rp : getRelationParts()) {
				POIXMLDocumentPart p = rp.getDocumentPart();
				if (p instanceof XSSFVMLDrawing) {
					XSSFVMLDrawing dr = ((XSSFVMLDrawing) (p));
					String drId = rp.getRelationship().getId();
					if (drId.equals(id)) {
						drawing = dr;
						break;
					}
				}
			}
			if (drawing == null) {
				XSSFSheet.logger.log(POILogger.ERROR, (("Can't find VML drawing with id=" + id) + " in the list of the sheet's relationships"));
			}
		}
		return drawing;
	}

	protected CTDrawing getCTDrawing() {
		return worksheet.getDrawing();
	}

	protected CTLegacyDrawing getCTLegacyDrawing() {
		return worksheet.getLegacyDrawing();
	}

	@Override
	public void createFreezePane(int colSplit, int rowSplit) {
		createFreezePane(colSplit, rowSplit, colSplit, rowSplit);
	}

	@Override
	public void createFreezePane(int colSplit, int rowSplit, int leftmostColumn, int topRow) {
		final boolean removeSplit = (colSplit == 0) && (rowSplit == 0);
		final CTSheetView ctView = getDefaultSheetView((!removeSplit));
		if (ctView != null) {
			ctView.setSelectionArray(null);
		}
		if (removeSplit) {
			if ((ctView != null) && (ctView.isSetPane())) {
				ctView.unsetPane();
			}
			return;
		}
		assert ctView != null;
		final CTPane pane = (ctView.isSetPane()) ? ctView.getPane() : ctView.addNewPane();
		assert pane != null;
		if (colSplit > 0) {
			pane.setXSplit(colSplit);
		}else
			if (pane.isSetXSplit()) {
				pane.unsetXSplit();
			}

		if (rowSplit > 0) {
			pane.setYSplit(rowSplit);
		}else
			if (pane.isSetYSplit()) {
				pane.unsetYSplit();
			}

		STPane.Enum activePane = STPane.BOTTOM_RIGHT;
		int pRow = topRow;
		int pCol = leftmostColumn;
		if (rowSplit == 0) {
			pRow = 0;
			activePane = STPane.TOP_RIGHT;
		}else
			if (colSplit == 0) {
				pCol = 0;
				activePane = STPane.BOTTOM_LEFT;
			}

		pane.setState(STPaneState.FROZEN);
		pane.setTopLeftCell(new CellReference(pRow, pCol).formatAsString());
		pane.setActivePane(activePane);
		ctView.addNewSelection().setPane(activePane);
	}

	@Override
	public XSSFRow createRow(int rownum) {
		final Integer rownumI = Integer.valueOf(rownum);
		CTRow ctRow;
		XSSFRow prev = _rows.get(rownumI);
		if (prev != null) {
			while ((prev.getFirstCellNum()) != (-1)) {
				prev.removeCell(prev.getCell(prev.getFirstCellNum()));
			} 
			ctRow = prev.getCTRow();
			ctRow.set(CTRow.Factory.newInstance());
		}else {
			if ((_rows.isEmpty()) || (rownum > (_rows.lastKey()))) {
				ctRow = worksheet.getSheetData().addNewRow();
			}else {
				int idx = _rows.headMap(rownumI).size();
				ctRow = worksheet.getSheetData().insertNewRow(idx);
			}
		}
		return null;
	}

	@Override
	public void createSplitPane(int xSplitPos, int ySplitPos, int leftmostColumn, int topRow, int activePane) {
		createFreezePane(xSplitPos, ySplitPos, leftmostColumn, topRow);
		if ((xSplitPos > 0) || (ySplitPos > 0)) {
			final CTPane pane = getPane(true);
			pane.setState(STPaneState.SPLIT);
			pane.setActivePane(STPane.Enum.forInt(activePane));
		}
	}

	@Override
	public XSSFComment getCellComment(CellAddress address) {
		if ((sheetComments) == null) {
			return null;
		}
		final int row = address.getRow();
		final int column = address.getColumn();
		CellAddress ref = new CellAddress(row, column);
		CTComment ctComment = sheetComments.getCTComment(ref);
		if (ctComment == null) {
			return null;
		}
		XSSFVMLDrawing vml = getVMLDrawing(false);
		return new XSSFComment(sheetComments, ctComment, (vml == null ? null : vml.findCommentShape(row, column)));
	}

	@Override
	public Map<CellAddress, XSSFComment> getCellComments() {
		if ((sheetComments) == null) {
			return Collections.emptyMap();
		}
		Map<CellAddress, XSSFComment> map = new HashMap<>();
		for (Iterator<CellAddress> iter = sheetComments.getCellAddresses(); iter.hasNext();) {
			CellAddress address = iter.next();
			map.put(address, getCellComment(address));
		}
		return map;
	}

	@Override
	public XSSFHyperlink getHyperlink(int row, int column) {
		return getHyperlink(new CellAddress(row, column));
	}

	@Override
	public XSSFHyperlink getHyperlink(CellAddress addr) {
		String ref = addr.formatAsString();
		for (XSSFHyperlink hyperlink : hyperlinks) {
			if (hyperlink.getCellRef().equals(ref)) {
				return hyperlink;
			}
		}
		return null;
	}

	@Override
	public List<XSSFHyperlink> getHyperlinkList() {
		return Collections.unmodifiableList(hyperlinks);
	}

	private int[] getBreaks(CTPageBreak ctPageBreak) {
		CTBreak[] brkArray = ctPageBreak.getBrkArray();
		int[] breaks = new int[brkArray.length];
		for (int i = 0; i < (brkArray.length); i++) {
			breaks[i] = ((int) (brkArray[i].getId())) - 1;
		}
		return breaks;
	}

	private void removeBreak(int index, CTPageBreak ctPageBreak) {
		int index1 = index + 1;
		CTBreak[] brkArray = ctPageBreak.getBrkArray();
		for (int i = 0; i < (brkArray.length); i++) {
			if ((brkArray[i].getId()) == index1) {
				ctPageBreak.removeBrk(i);
			}
		}
	}

	@Override
	public int[] getColumnBreaks() {
		return worksheet.isSetColBreaks() ? getBreaks(worksheet.getColBreaks()) : new int[0];
	}

	@Override
	public int getColumnWidth(int columnIndex) {
		CTCol col = columnHelper.getColumn(columnIndex, false);
		double width = ((col == null) || (!(col.isSetWidth()))) ? getDefaultColumnWidth() : col.getWidth();
		return ((int) (width * 256));
	}

	@Override
	public float getColumnWidthInPixels(int columnIndex) {
		float widthIn256 = getColumnWidth(columnIndex);
		return ((float) ((widthIn256 / 256.0) * (Units.DEFAULT_CHARACTER_WIDTH)));
	}

	@Override
	public int getDefaultColumnWidth() {
		CTSheetFormatPr pr = worksheet.getSheetFormatPr();
		return pr == null ? 8 : ((int) (pr.getBaseColWidth()));
	}

	@Override
	public short getDefaultRowHeight() {
		return ((short) ((getDefaultRowHeightInPoints()) * (XSSFSheet.TWIPS_PER_POINT)));
	}

	@Override
	public float getDefaultRowHeightInPoints() {
		CTSheetFormatPr pr = worksheet.getSheetFormatPr();
		return ((float) (pr == null ? 0 : pr.getDefaultRowHeight()));
	}

	private CTSheetFormatPr getSheetTypeSheetFormatPr() {
		return worksheet.isSetSheetFormatPr() ? worksheet.getSheetFormatPr() : worksheet.addNewSheetFormatPr();
	}

	@Override
	public CellStyle getColumnStyle(int column) {
		int idx = columnHelper.getColDefaultStyle(column);
		return getWorkbook().getCellStyleAt(((short) (idx == (-1) ? 0 : idx)));
	}

	@Override
	public void setRightToLeft(boolean value) {
		final CTSheetView dsv = getDefaultSheetView(true);
		assert dsv != null;
		dsv.setRightToLeft(value);
	}

	@Override
	public boolean isRightToLeft() {
		final CTSheetView dsv = getDefaultSheetView(false);
		return (dsv != null) && (dsv.getRightToLeft());
	}

	@Override
	public boolean getDisplayGuts() {
		CTSheetPr sheetPr = getSheetTypeSheetPr();
		CTOutlinePr outlinePr = ((sheetPr.getOutlinePr()) == null) ? CTOutlinePr.Factory.newInstance() : sheetPr.getOutlinePr();
		return outlinePr.getShowOutlineSymbols();
	}

	@Override
	public void setDisplayGuts(boolean value) {
		CTSheetPr sheetPr = getSheetTypeSheetPr();
		CTOutlinePr outlinePr = ((sheetPr.getOutlinePr()) == null) ? sheetPr.addNewOutlinePr() : sheetPr.getOutlinePr();
		outlinePr.setShowOutlineSymbols(value);
	}

	@Override
	public boolean isDisplayZeros() {
		final CTSheetView dsv = getDefaultSheetView(false);
		return dsv != null ? dsv.getShowZeros() : true;
	}

	@Override
	public void setDisplayZeros(boolean value) {
		final CTSheetView view = getDefaultSheetView(true);
		assert view != null;
		view.setShowZeros(value);
	}

	@Override
	public int getFirstRowNum() {
		return _rows.isEmpty() ? 0 : _rows.firstKey();
	}

	@Override
	public boolean getFitToPage() {
		CTSheetPr sheetPr = getSheetTypeSheetPr();
		CTPageSetUpPr psSetup = ((sheetPr == null) || (!(sheetPr.isSetPageSetUpPr()))) ? CTPageSetUpPr.Factory.newInstance() : sheetPr.getPageSetUpPr();
		return psSetup.getFitToPage();
	}

	private CTSheetPr getSheetTypeSheetPr() {
		if ((worksheet.getSheetPr()) == null) {
			worksheet.setSheetPr(CTSheetPr.Factory.newInstance());
		}
		return worksheet.getSheetPr();
	}

	private CTHeaderFooter getSheetTypeHeaderFooter() {
		if ((worksheet.getHeaderFooter()) == null) {
			worksheet.setHeaderFooter(CTHeaderFooter.Factory.newInstance());
		}
		return worksheet.getHeaderFooter();
	}

	@Override
	public Footer getFooter() {
		return getOddFooter();
	}

	@Override
	public Header getHeader() {
		return getOddHeader();
	}

	public Footer getOddFooter() {
		return null;
	}

	public Footer getEvenFooter() {
		return null;
	}

	public Footer getFirstFooter() {
		return null;
	}

	public Header getOddHeader() {
		return null;
	}

	public Header getEvenHeader() {
		return null;
	}

	public Header getFirstHeader() {
		return null;
	}

	@Override
	public boolean getHorizontallyCenter() {
		CTPrintOptions opts = worksheet.getPrintOptions();
		return (opts != null) && (opts.getHorizontalCentered());
	}

	@Override
	public int getLastRowNum() {
		return _rows.isEmpty() ? 0 : _rows.lastKey();
	}

	@Override
	public short getLeftCol() {
		String cellRef = worksheet.getSheetViews().getSheetViewArray(0).getTopLeftCell();
		if (cellRef == null) {
			return 0;
		}
		CellReference cellReference = new CellReference(cellRef);
		return cellReference.getCol();
	}

	@Override
	public double getMargin(short margin) {
		if (!(worksheet.isSetPageMargins())) {
			return 0;
		}
		CTPageMargins pageMargins = worksheet.getPageMargins();
		switch (margin) {
			case Sheet.LeftMargin :
				return pageMargins.getLeft();
			case Sheet.RightMargin :
				return pageMargins.getRight();
			case Sheet.TopMargin :
				return pageMargins.getTop();
			case Sheet.BottomMargin :
				return pageMargins.getBottom();
			case Sheet.HeaderMargin :
				return pageMargins.getHeader();
			case Sheet.FooterMargin :
				return pageMargins.getFooter();
			default :
				throw new IllegalArgumentException(("Unknown margin constant:  " + margin));
		}
	}

	@Override
	public void setMargin(short margin, double size) {
		CTPageMargins pageMargins = (worksheet.isSetPageMargins()) ? worksheet.getPageMargins() : worksheet.addNewPageMargins();
		switch (margin) {
			case Sheet.LeftMargin :
				pageMargins.setLeft(size);
				break;
			case Sheet.RightMargin :
				pageMargins.setRight(size);
				break;
			case Sheet.TopMargin :
				pageMargins.setTop(size);
				break;
			case Sheet.BottomMargin :
				pageMargins.setBottom(size);
				break;
			case Sheet.HeaderMargin :
				pageMargins.setHeader(size);
				break;
			case Sheet.FooterMargin :
				pageMargins.setFooter(size);
				break;
			default :
				throw new IllegalArgumentException(("Unknown margin constant:  " + margin));
		}
	}

	@Override
	public CellRangeAddress getMergedRegion(int index) {
		CTMergeCells ctMergeCells = worksheet.getMergeCells();
		if (ctMergeCells == null) {
			throw new IllegalStateException("This worksheet does not contain merged regions");
		}
		CTMergeCell ctMergeCell = ctMergeCells.getMergeCellArray(index);
		String ref = ctMergeCell.getRef();
		return CellRangeAddress.valueOf(ref);
	}

	@Override
	public List<CellRangeAddress> getMergedRegions() {
		List<CellRangeAddress> addresses = new ArrayList<>();
		CTMergeCells ctMergeCells = worksheet.getMergeCells();
		if (ctMergeCells == null) {
			return addresses;
		}
		for (CTMergeCell ctMergeCell : ctMergeCells.getMergeCellArray()) {
			String ref = ctMergeCell.getRef();
			addresses.add(CellRangeAddress.valueOf(ref));
		}
		return addresses;
	}

	@Override
	public int getNumMergedRegions() {
		CTMergeCells ctMergeCells = worksheet.getMergeCells();
		return ctMergeCells == null ? 0 : ctMergeCells.sizeOfMergeCellArray();
	}

	public int getNumHyperlinks() {
		return hyperlinks.size();
	}

	@Override
	public PaneInformation getPaneInformation() {
		final CTPane pane = getPane(false);
		if (pane == null) {
			return null;
		}
		short row = 0;
		short col = 0;
		if (pane.isSetTopLeftCell()) {
			final CellReference cellRef = new CellReference(pane.getTopLeftCell());
			row = ((short) (cellRef.getRow()));
			col = ((short) (cellRef.getCol()));
		}
		final short x = ((short) (pane.getXSplit()));
		final short y = ((short) (pane.getYSplit()));
		final byte active = ((byte) ((pane.getActivePane().intValue()) - 1));
		final boolean frozen = (pane.getState()) == (STPaneState.FROZEN);
		return new PaneInformation(x, y, row, col, active, frozen);
	}

	@Override
	public int getPhysicalNumberOfRows() {
		return _rows.size();
	}

	@Override
	public XSSFPrintSetup getPrintSetup() {
		return null;
	}

	@Override
	public boolean getProtect() {
		return isSheetLocked();
	}

	@Override
	public void protectSheet(String password) {
		if (password != null) {
			CTSheetProtection sheetProtection = safeGetProtectionField();
			setSheetPassword(password, null);
			sheetProtection.setSheet(true);
			sheetProtection.setScenarios(true);
			sheetProtection.setObjects(true);
		}else {
			worksheet.unsetSheetProtection();
		}
	}

	public void setSheetPassword(String password, HashAlgorithm hashAlgo) {
		if ((password == null) && (!(isSheetProtectionEnabled()))) {
			return;
		}
		XSSFPasswordHelper.setPassword(safeGetProtectionField(), password, hashAlgo, null);
	}

	public boolean validateSheetPassword(String password) {
		if (!(isSheetProtectionEnabled())) {
			return password == null;
		}
		return XSSFPasswordHelper.validatePassword(safeGetProtectionField(), password, null);
	}

	@Override
	public XSSFRow getRow(int rownum) {
		final Integer rownumI = Integer.valueOf(rownum);
		return _rows.get(rownumI);
	}

	private List<XSSFRow> getRows(int startRowNum, int endRowNum, boolean createRowIfMissing) {
		if (startRowNum > endRowNum) {
			throw new IllegalArgumentException("getRows: startRowNum must be less than or equal to endRowNum");
		}
		final List<XSSFRow> rows = new ArrayList<>();
		if (createRowIfMissing) {
			for (int i = startRowNum; i <= endRowNum; i++) {
				XSSFRow row = getRow(i);
				if (row == null) {
					row = createRow(i);
				}
				rows.add(row);
			}
		}else {
			final Integer startI = Integer.valueOf(startRowNum);
			final Integer endI = Integer.valueOf((endRowNum + 1));
			final Collection<XSSFRow> inclusive = _rows.subMap(startI, endI).values();
			rows.addAll(inclusive);
		}
		return rows;
	}

	@Override
	public int[] getRowBreaks() {
		return worksheet.isSetRowBreaks() ? getBreaks(worksheet.getRowBreaks()) : new int[0];
	}

	@Override
	public boolean getRowSumsBelow() {
		CTSheetPr sheetPr = worksheet.getSheetPr();
		CTOutlinePr outlinePr = ((sheetPr != null) && (sheetPr.isSetOutlinePr())) ? sheetPr.getOutlinePr() : null;
		return (outlinePr == null) || (outlinePr.getSummaryBelow());
	}

	@Override
	public void setRowSumsBelow(boolean value) {
		ensureOutlinePr().setSummaryBelow(value);
	}

	@Override
	public boolean getRowSumsRight() {
		CTSheetPr sheetPr = worksheet.getSheetPr();
		CTOutlinePr outlinePr = ((sheetPr != null) && (sheetPr.isSetOutlinePr())) ? sheetPr.getOutlinePr() : CTOutlinePr.Factory.newInstance();
		return outlinePr.getSummaryRight();
	}

	@Override
	public void setRowSumsRight(boolean value) {
		ensureOutlinePr().setSummaryRight(value);
	}

	private CTOutlinePr ensureOutlinePr() {
		CTSheetPr sheetPr = (worksheet.isSetSheetPr()) ? worksheet.getSheetPr() : worksheet.addNewSheetPr();
		return sheetPr.isSetOutlinePr() ? sheetPr.getOutlinePr() : sheetPr.addNewOutlinePr();
	}

	@Override
	public boolean getScenarioProtect() {
		return (worksheet.isSetSheetProtection()) && (worksheet.getSheetProtection().getScenarios());
	}

	@Override
	public short getTopRow() {
		final CTSheetView dsv = getDefaultSheetView(false);
		final String cellRef = (dsv == null) ? null : dsv.getTopLeftCell();
		if (cellRef == null) {
			return 0;
		}
		return ((short) (new CellReference(cellRef).getRow()));
	}

	@Override
	public boolean getVerticallyCenter() {
		CTPrintOptions opts = worksheet.getPrintOptions();
		return (opts != null) && (opts.getVerticalCentered());
	}

	@Override
	public void groupColumn(int fromColumn, int toColumn) {
		groupColumn1Based((fromColumn + 1), (toColumn + 1));
	}

	private void groupColumn1Based(int fromColumn, int toColumn) {
		CTCols ctCols = worksheet.getColsArray(0);
		CTCol ctCol = newInstance();
		CTCol fixCol_before = this.columnHelper.getColumn1Based(toColumn, false);
		if (fixCol_before != null) {
			fixCol_before = ((CTCol) (fixCol_before.copy()));
		}
		ctCol.setMin(fromColumn);
		ctCol.setMax(toColumn);
		this.columnHelper.addCleanColIntoCols(ctCols, ctCol);
		CTCol fixCol_after = this.columnHelper.getColumn1Based(toColumn, false);
		if ((fixCol_before != null) && (fixCol_after != null)) {
			this.columnHelper.setColumnAttributes(fixCol_before, fixCol_after);
		}
		for (int index = fromColumn; index <= toColumn; index++) {
			CTCol col = columnHelper.getColumn1Based(index, false);
			short outlineLevel = col.getOutlineLevel();
			col.setOutlineLevel(((short) (outlineLevel + 1)));
			index = ((int) (col.getMax()));
		}
		worksheet.setColsArray(0, ctCols);
		setSheetFormatPrOutlineLevelCol();
	}

	private void setColWidthAttribute(CTCols ctCols) {
		for (CTCol col : ctCols.getColArray()) {
			if (!(col.isSetWidth())) {
				col.setWidth(getDefaultColumnWidth());
				col.setCustomWidth(false);
			}
		}
	}

	@Override
	public void groupRow(int fromRow, int toRow) {
		for (int i = fromRow; i <= toRow; i++) {
			XSSFRow xrow = getRow(i);
			if (xrow == null) {
				xrow = createRow(i);
			}
			CTRow ctrow = xrow.getCTRow();
			short outlineLevel = ctrow.getOutlineLevel();
			ctrow.setOutlineLevel(((short) (outlineLevel + 1)));
		}
		setSheetFormatPrOutlineLevelRow();
	}

	private short getMaxOutlineLevelRows() {
		int outlineLevel = 0;
		for (XSSFRow xrow : _rows.values()) {
			outlineLevel = Math.max(outlineLevel, xrow.getCTRow().getOutlineLevel());
		}
		return ((short) (outlineLevel));
	}

	private short getMaxOutlineLevelCols() {
		CTCols ctCols = worksheet.getColsArray(0);
		int outlineLevel = 0;
		for (CTCol col : ctCols.getColArray()) {
			outlineLevel = Math.max(outlineLevel, col.getOutlineLevel());
		}
		return ((short) (outlineLevel));
	}

	@Override
	public boolean isColumnBroken(int column) {
		for (int colBreak : getColumnBreaks()) {
			if (colBreak == column) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isColumnHidden(int columnIndex) {
		CTCol col = columnHelper.getColumn(columnIndex, false);
		return (col != null) && (col.getHidden());
	}

	@Override
	public boolean isDisplayFormulas() {
		final CTSheetView dsv = getDefaultSheetView(false);
		return dsv != null ? dsv.getShowFormulas() : false;
	}

	@Override
	public boolean isDisplayGridlines() {
		final CTSheetView dsv = getDefaultSheetView(false);
		return dsv != null ? dsv.getShowGridLines() : true;
	}

	@Override
	public void setDisplayGridlines(boolean show) {
		final CTSheetView dsv = getDefaultSheetView(true);
		assert dsv != null;
		dsv.setShowGridLines(show);
	}

	@Override
	public boolean isDisplayRowColHeadings() {
		final CTSheetView dsv = getDefaultSheetView(false);
		return dsv != null ? dsv.getShowRowColHeaders() : true;
	}

	@Override
	public void setDisplayRowColHeadings(boolean show) {
		final CTSheetView dsv = getDefaultSheetView(true);
		assert dsv != null;
		dsv.setShowRowColHeaders(show);
	}

	@Override
	public boolean isPrintGridlines() {
		CTPrintOptions opts = worksheet.getPrintOptions();
		return (opts != null) && (opts.getGridLines());
	}

	@Override
	public void setPrintGridlines(boolean value) {
		CTPrintOptions opts = (worksheet.isSetPrintOptions()) ? worksheet.getPrintOptions() : worksheet.addNewPrintOptions();
		opts.setGridLines(value);
	}

	@Override
	public boolean isPrintRowAndColumnHeadings() {
		CTPrintOptions opts = worksheet.getPrintOptions();
		return (opts != null) && (opts.getHeadings());
	}

	@Override
	public void setPrintRowAndColumnHeadings(boolean value) {
		CTPrintOptions opts = (worksheet.isSetPrintOptions()) ? worksheet.getPrintOptions() : worksheet.addNewPrintOptions();
		opts.setHeadings(value);
	}

	@Override
	public boolean isRowBroken(int row) {
		for (int rowBreak : getRowBreaks()) {
			if (rowBreak == row) {
				return true;
			}
		}
		return false;
	}

	private void setBreak(int id, CTPageBreak ctPgBreak, int lastIndex) {
		CTBreak brk = ctPgBreak.addNewBrk();
		brk.setId((id + 1));
		brk.setMan(true);
		brk.setMax(lastIndex);
		int nPageBreaks = ctPgBreak.sizeOfBrkArray();
		ctPgBreak.setCount(nPageBreaks);
		ctPgBreak.setManualBreakCount(nPageBreaks);
	}

	@Override
	public void setRowBreak(int row) {
		if (!(isRowBroken(row))) {
			CTPageBreak pgBreak = (worksheet.isSetRowBreaks()) ? worksheet.getRowBreaks() : worksheet.addNewRowBreaks();
			setBreak(row, pgBreak, SpreadsheetVersion.EXCEL2007.getLastColumnIndex());
		}
	}

	@Override
	public void removeColumnBreak(int column) {
		if (worksheet.isSetColBreaks()) {
			removeBreak(column, worksheet.getColBreaks());
		}
	}

	@Override
	public void removeMergedRegion(int index) {
		if (!(worksheet.isSetMergeCells())) {
			return;
		}
		CTMergeCells ctMergeCells = worksheet.getMergeCells();
		int size = ctMergeCells.sizeOfMergeCellArray();
		assert (0 <= index) && (index < size);
		if (size > 1) {
			ctMergeCells.removeMergeCell(index);
		}else {
			worksheet.unsetMergeCells();
		}
	}

	@Override
	public void removeMergedRegions(Collection<Integer> indices) {
		if (!(worksheet.isSetMergeCells())) {
			return;
		}
		CTMergeCells ctMergeCells = worksheet.getMergeCells();
		List<CTMergeCell> newMergeCells = new ArrayList<>(ctMergeCells.sizeOfMergeCellArray());
		int idx = 0;
		for (CTMergeCell mc : ctMergeCells.getMergeCellArray()) {
			if (!(indices.contains((idx++)))) {
				newMergeCells.add(mc);
			}
		}
		if (newMergeCells.isEmpty()) {
			worksheet.unsetMergeCells();
		}else {
			CTMergeCell[] newMergeCellsArray = new CTMergeCell[newMergeCells.size()];
			ctMergeCells.setMergeCellArray(newMergeCells.toArray(newMergeCellsArray));
		}
	}

	@Override
	public void removeRow(Row row) {
		if ((row.getSheet()) != (this)) {
			throw new IllegalArgumentException("Specified row does not belong to this sheet");
		}
		ArrayList<XSSFCell> cellsToDelete = new ArrayList<>();
		for (Cell cell : row) {
			cellsToDelete.add(((XSSFCell) (cell)));
		}
		for (XSSFCell cell : cellsToDelete) {
			row.removeCell(cell);
		}
		final int rowNum = row.getRowNum();
		final Integer rowNumI = Integer.valueOf(rowNum);
		final int idx = _rows.headMap(rowNumI).size();
		_rows.remove(rowNumI);
		worksheet.getSheetData().removeRow(idx);
		if ((sheetComments) != null) {
			for (CellAddress ref : getCellComments().keySet()) {
				if ((ref.getRow()) == rowNum) {
					sheetComments.removeComment(ref);
				}
			}
		}
	}

	@Override
	public void removeRowBreak(int row) {
		if (worksheet.isSetRowBreaks()) {
			removeBreak(row, worksheet.getRowBreaks());
		}
	}

	@Override
	public void setForceFormulaRecalculation(boolean value) {
		CTCalcPr calcPr = getWorkbook().getCTWorkbook().getCalcPr();
		if (worksheet.isSetSheetCalcPr()) {
			CTSheetCalcPr calc = worksheet.getSheetCalcPr();
			calc.setFullCalcOnLoad(value);
		}else
			if (value) {
				CTSheetCalcPr calc = worksheet.addNewSheetCalcPr();
				calc.setFullCalcOnLoad(value);
			}

		if ((value && (calcPr != null)) && ((calcPr.getCalcMode()) == (STCalcMode.MANUAL))) {
			calcPr.setCalcMode(STCalcMode.AUTO);
		}
	}

	@Override
	public boolean getForceFormulaRecalculation() {
		if (worksheet.isSetSheetCalcPr()) {
			CTSheetCalcPr calc = worksheet.getSheetCalcPr();
			return calc.getFullCalcOnLoad();
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<Row> rowIterator() {
		return ((Iterator<Row>) ((Iterator<? extends Row>) (_rows.values().iterator())));
	}

	@Override
	public Iterator<Row> iterator() {
		return rowIterator();
	}

	@Override
	public boolean getAutobreaks() {
		CTSheetPr sheetPr = getSheetTypeSheetPr();
		CTPageSetUpPr psSetup = ((sheetPr == null) || (!(sheetPr.isSetPageSetUpPr()))) ? CTPageSetUpPr.Factory.newInstance() : sheetPr.getPageSetUpPr();
		return psSetup.getAutoPageBreaks();
	}

	@Override
	public void setAutobreaks(boolean value) {
		CTSheetPr sheetPr = getSheetTypeSheetPr();
		CTPageSetUpPr psSetup = (sheetPr.isSetPageSetUpPr()) ? sheetPr.getPageSetUpPr() : sheetPr.addNewPageSetUpPr();
		psSetup.setAutoPageBreaks(value);
	}

	@Override
	public void setColumnBreak(int column) {
		if (!(isColumnBroken(column))) {
			CTPageBreak pgBreak = (worksheet.isSetColBreaks()) ? worksheet.getColBreaks() : worksheet.addNewColBreaks();
			setBreak(column, pgBreak, SpreadsheetVersion.EXCEL2007.getLastRowIndex());
		}
	}

	@Override
	public void setColumnGroupCollapsed(int columnNumber, boolean collapsed) {
		if (collapsed) {
			collapseColumn(columnNumber);
		}else {
			expandColumn(columnNumber);
		}
	}

	private void collapseColumn(int columnNumber) {
		CTCols cols = worksheet.getColsArray(0);
		CTCol col = columnHelper.getColumn(columnNumber, false);
		int colInfoIx = columnHelper.getIndexOfColumn(cols, col);
		if (colInfoIx == (-1)) {
			return;
		}
		int groupStartColInfoIx = findStartOfColumnOutlineGroup(colInfoIx);
		CTCol columnInfo = cols.getColArray(groupStartColInfoIx);
		int lastColMax = setGroupHidden(groupStartColInfoIx, columnInfo.getOutlineLevel(), true);
		setColumn((lastColMax + 1), 0, null, null, Boolean.TRUE);
	}

	private void setColumn(int targetColumnIx, Integer style, Integer level, Boolean hidden, Boolean collapsed) {
		CTCols cols = worksheet.getColsArray(0);
		CTCol ci = null;
		for (CTCol tci : cols.getColArray()) {
			long tciMin = tci.getMin();
			long tciMax = tci.getMax();
			if ((tciMin >= targetColumnIx) && (tciMax <= targetColumnIx)) {
				ci = tci;
				break;
			}
			if (tciMin > targetColumnIx) {
				break;
			}
		}
		if (ci == null) {
			CTCol nci = newInstance();
			nci.setMin(targetColumnIx);
			nci.setMax(targetColumnIx);
			unsetCollapsed(collapsed, nci);
			this.columnHelper.addCleanColIntoCols(cols, nci);
			return;
		}
		boolean styleChanged = (style != null) && ((ci.getStyle()) != style);
		boolean levelChanged = (level != null) && ((ci.getOutlineLevel()) != level);
		boolean hiddenChanged = (hidden != null) && ((ci.getHidden()) != hidden);
		boolean collapsedChanged = (collapsed != null) && ((ci.getCollapsed()) != collapsed);
		boolean columnChanged = ((levelChanged || hiddenChanged) || collapsedChanged) || styleChanged;
		if (!columnChanged) {
			return;
		}
		long ciMin = ci.getMin();
		long ciMax = ci.getMax();
		if ((ciMin == targetColumnIx) && (ciMax == targetColumnIx)) {
			unsetCollapsed(collapsed, ci);
			return;
		}
		if ((ciMin == targetColumnIx) || (ciMax == targetColumnIx)) {
			if (ciMin == targetColumnIx) {
				ci.setMin((targetColumnIx + 1));
			}else {
				ci.setMax((targetColumnIx - 1));
			}
			CTCol nci = columnHelper.cloneCol(cols, ci);
			nci.setMin(targetColumnIx);
			unsetCollapsed(collapsed, nci);
			this.columnHelper.addCleanColIntoCols(cols, nci);
		}else {
			CTCol ciMid = columnHelper.cloneCol(cols, ci);
			CTCol ciEnd = columnHelper.cloneCol(cols, ci);
			int lastcolumn = ((int) (ciMax));
			ci.setMax((targetColumnIx - 1));
			ciMid.setMin(targetColumnIx);
			ciMid.setMax(targetColumnIx);
			unsetCollapsed(collapsed, ciMid);
			this.columnHelper.addCleanColIntoCols(cols, ciMid);
			ciEnd.setMin((targetColumnIx + 1));
			ciEnd.setMax(lastcolumn);
			this.columnHelper.addCleanColIntoCols(cols, ciEnd);
		}
	}

	private void unsetCollapsed(Boolean collapsed, CTCol ci) {
		if ((collapsed != null) && (collapsed.booleanValue())) {
			ci.setCollapsed(collapsed);
		}else {
			ci.unsetCollapsed();
		}
	}

	private int setGroupHidden(int pIdx, int level, boolean hidden) {
		CTCols cols = worksheet.getColsArray(0);
		int idx = pIdx;
		CTCol[] colArray = cols.getColArray();
		CTCol columnInfo = colArray[idx];
		while (idx < (colArray.length)) {
			columnInfo.setHidden(hidden);
			if ((idx + 1) < (colArray.length)) {
				CTCol nextColumnInfo = colArray[(idx + 1)];
				if (!(isAdjacentBefore(columnInfo, nextColumnInfo))) {
					break;
				}
				if ((nextColumnInfo.getOutlineLevel()) < level) {
					break;
				}
				columnInfo = nextColumnInfo;
			}
			idx++;
		} 
		return ((int) (columnInfo.getMax()));
	}

	private boolean isAdjacentBefore(CTCol col, CTCol otherCol) {
		return (col.getMax()) == ((otherCol.getMin()) - 1);
	}

	private int findStartOfColumnOutlineGroup(int pIdx) {
		CTCols cols = worksheet.getColsArray(0);
		CTCol[] colArray = cols.getColArray();
		CTCol columnInfo = colArray[pIdx];
		int level = columnInfo.getOutlineLevel();
		int idx = pIdx;
		while (idx != 0) {
			CTCol prevColumnInfo = colArray[(idx - 1)];
			if (!(isAdjacentBefore(prevColumnInfo, columnInfo))) {
				break;
			}
			if ((prevColumnInfo.getOutlineLevel()) < level) {
				break;
			}
			idx--;
			columnInfo = prevColumnInfo;
		} 
		return idx;
	}

	private int findEndOfColumnOutlineGroup(int colInfoIndex) {
		CTCols cols = worksheet.getColsArray(0);
		CTCol[] colArray = cols.getColArray();
		CTCol columnInfo = colArray[colInfoIndex];
		int level = columnInfo.getOutlineLevel();
		int idx = colInfoIndex;
		int lastIdx = (colArray.length) - 1;
		while (idx < lastIdx) {
			CTCol nextColumnInfo = colArray[(idx + 1)];
			if (!(isAdjacentBefore(columnInfo, nextColumnInfo))) {
				break;
			}
			if ((nextColumnInfo.getOutlineLevel()) < level) {
				break;
			}
			idx++;
			columnInfo = nextColumnInfo;
		} 
		return idx;
	}

	private void expandColumn(int columnIndex) {
		CTCols cols = worksheet.getColsArray(0);
		CTCol col = columnHelper.getColumn(columnIndex, false);
		int colInfoIx = columnHelper.getIndexOfColumn(cols, col);
		int idx = findColInfoIdx(((int) (col.getMax())), colInfoIx);
		if (idx == (-1)) {
			return;
		}
		if (!(isColumnGroupCollapsed(idx))) {
			return;
		}
		int startIdx = findStartOfColumnOutlineGroup(idx);
		int endIdx = findEndOfColumnOutlineGroup(idx);
		CTCol[] colArray = cols.getColArray();
		CTCol columnInfo = colArray[endIdx];
		if (!(isColumnGroupHiddenByParent(idx))) {
			short outlineLevel = columnInfo.getOutlineLevel();
			boolean nestedGroup = false;
			for (int i = startIdx; i <= endIdx; i++) {
				CTCol ci = colArray[i];
				if (outlineLevel == (ci.getOutlineLevel())) {
					ci.unsetHidden();
					if (nestedGroup) {
						nestedGroup = false;
						ci.setCollapsed(true);
					}
				}else {
					nestedGroup = true;
				}
			}
		}
		setColumn((((int) (columnInfo.getMax())) + 1), null, null, Boolean.FALSE, Boolean.FALSE);
	}

	private boolean isColumnGroupHiddenByParent(int idx) {
		CTCols cols = worksheet.getColsArray(0);
		int endLevel = 0;
		boolean endHidden = false;
		int endOfOutlineGroupIdx = findEndOfColumnOutlineGroup(idx);
		CTCol[] colArray = cols.getColArray();
		if (endOfOutlineGroupIdx < ((colArray.length) - 1)) {
			CTCol nextInfo = colArray[(endOfOutlineGroupIdx + 1)];
			if (isAdjacentBefore(colArray[endOfOutlineGroupIdx], nextInfo)) {
				endLevel = nextInfo.getOutlineLevel();
				endHidden = nextInfo.getHidden();
			}
		}
		int startLevel = 0;
		boolean startHidden = false;
		int startOfOutlineGroupIdx = findStartOfColumnOutlineGroup(idx);
		if (startOfOutlineGroupIdx > 0) {
			CTCol prevInfo = colArray[(startOfOutlineGroupIdx - 1)];
			if (isAdjacentBefore(prevInfo, colArray[startOfOutlineGroupIdx])) {
				startLevel = prevInfo.getOutlineLevel();
				startHidden = prevInfo.getHidden();
			}
		}
		if (endLevel > startLevel) {
			return endHidden;
		}
		return startHidden;
	}

	private int findColInfoIdx(int columnValue, int fromColInfoIdx) {
		CTCols cols = worksheet.getColsArray(0);
		if (columnValue < 0) {
			throw new IllegalArgumentException(("column parameter out of range: " + columnValue));
		}
		if (fromColInfoIdx < 0) {
			throw new IllegalArgumentException(("fromIdx parameter out of range: " + fromColInfoIdx));
		}
		CTCol[] colArray = cols.getColArray();
		for (int k = fromColInfoIdx; k < (colArray.length); k++) {
			CTCol ci = colArray[k];
			if (containsColumn(ci, columnValue)) {
				return k;
			}
			if ((ci.getMin()) > fromColInfoIdx) {
				break;
			}
		}
		return -1;
	}

	private boolean containsColumn(CTCol col, int columnIndex) {
		return ((col.getMin()) <= columnIndex) && (columnIndex <= (col.getMax()));
	}

	private boolean isColumnGroupCollapsed(int idx) {
		CTCols cols = worksheet.getColsArray(0);
		CTCol[] colArray = cols.getColArray();
		int endOfOutlineGroupIdx = findEndOfColumnOutlineGroup(idx);
		int nextColInfoIx = endOfOutlineGroupIdx + 1;
		if (nextColInfoIx >= (colArray.length)) {
			return false;
		}
		CTCol nextColInfo = colArray[nextColInfoIx];
		CTCol col = colArray[endOfOutlineGroupIdx];
		if (!(isAdjacentBefore(col, nextColInfo))) {
			return false;
		}
		return nextColInfo.getCollapsed();
	}

	@Override
	public void setColumnHidden(int columnIndex, boolean hidden) {
		columnHelper.setColHidden(columnIndex, hidden);
	}

	@Override
	public void setColumnWidth(int columnIndex, int width) {
		if (width > (255 * 256)) {
			throw new IllegalArgumentException("The maximum column width for an individual cell is 255 characters.");
		}
		columnHelper.setColWidth(columnIndex, (((double) (width)) / 256));
		columnHelper.setCustomWidth(columnIndex, true);
	}

	@Override
	public void setDefaultColumnStyle(int column, CellStyle style) {
		columnHelper.setColDefaultStyle(column, style);
	}

	@Override
	public void setDefaultColumnWidth(int width) {
		getSheetTypeSheetFormatPr().setBaseColWidth(width);
	}

	@Override
	public void setDefaultRowHeight(short height) {
		setDefaultRowHeightInPoints((((float) (height)) / (XSSFSheet.TWIPS_PER_POINT)));
	}

	@Override
	public void setDefaultRowHeightInPoints(float height) {
		CTSheetFormatPr pr = getSheetTypeSheetFormatPr();
		pr.setDefaultRowHeight(height);
		pr.setCustomHeight(true);
	}

	@Override
	public void setDisplayFormulas(boolean show) {
		final CTSheetView dsv = getDefaultSheetView(true);
		assert dsv != null;
		dsv.setShowFormulas(show);
	}

	@Override
	public void setFitToPage(boolean b) {
		getSheetTypePageSetUpPr().setFitToPage(b);
	}

	@Override
	public void setHorizontallyCenter(boolean value) {
		CTPrintOptions opts = (worksheet.isSetPrintOptions()) ? worksheet.getPrintOptions() : worksheet.addNewPrintOptions();
		opts.setHorizontalCentered(value);
	}

	@Override
	public void setVerticallyCenter(boolean value) {
		CTPrintOptions opts = (worksheet.isSetPrintOptions()) ? worksheet.getPrintOptions() : worksheet.addNewPrintOptions();
		opts.setVerticalCentered(value);
	}

	@Override
	public void setRowGroupCollapsed(int rowIndex, boolean collapse) {
		if (collapse) {
			collapseRow(rowIndex);
		}else {
			expandRow(rowIndex);
		}
	}

	private void collapseRow(int rowIndex) {
		XSSFRow row = getRow(rowIndex);
		if (row != null) {
			int startRow = findStartOfRowOutlineGroup(rowIndex);
			int lastRow = writeHidden(row, startRow, true);
			if ((getRow(lastRow)) != null) {
				getRow(lastRow).getCTRow().setCollapsed(true);
			}else {
				XSSFRow newRow = createRow(lastRow);
				newRow.getCTRow().setCollapsed(true);
			}
		}
	}

	private int findStartOfRowOutlineGroup(int rowIndex) {
		short level = getRow(rowIndex).getCTRow().getOutlineLevel();
		int currentRow = rowIndex;
		while ((getRow(currentRow)) != null) {
			if ((getRow(currentRow).getCTRow().getOutlineLevel()) < level) {
				return currentRow + 1;
			}
			currentRow--;
		} 
		return currentRow;
	}

	private int writeHidden(XSSFRow xRow, int rowIndex, boolean hidden) {
		short level = xRow.getCTRow().getOutlineLevel();
		for (Iterator<Row> it = rowIterator(); it.hasNext();) {
			xRow = ((XSSFRow) (it.next()));
			if ((xRow.getRowNum()) < rowIndex) {
				continue;
			}
			if ((xRow.getCTRow().getOutlineLevel()) >= level) {
				xRow.getCTRow().setHidden(hidden);
				rowIndex++;
			}
		}
		return rowIndex;
	}

	private void expandRow(int rowNumber) {
		if (rowNumber == (-1)) {
			return;
		}
		XSSFRow row = getRow(rowNumber);
		if (!(row.getCTRow().isSetHidden())) {
			return;
		}
		int startIdx = findStartOfRowOutlineGroup(rowNumber);
		int endIdx = findEndOfRowOutlineGroup(rowNumber);
		short level = row.getCTRow().getOutlineLevel();
		if (!(isRowGroupHiddenByParent(rowNumber))) {
			for (int i = startIdx; i < endIdx; i++) {
				if (level == (getRow(i).getCTRow().getOutlineLevel())) {
					getRow(i).getCTRow().unsetHidden();
				}else
					if (!(isRowGroupCollapsed(i))) {
						getRow(i).getCTRow().unsetHidden();
					}

			}
		}
		CTRow ctRow = getRow(endIdx).getCTRow();
		if (ctRow.getCollapsed()) {
			ctRow.unsetCollapsed();
		}
	}

	public int findEndOfRowOutlineGroup(int row) {
		short level = getRow(row).getCTRow().getOutlineLevel();
		int currentRow;
		final int lastRowNum = getLastRowNum();
		for (currentRow = row; currentRow < lastRowNum; currentRow++) {
			if (((getRow(currentRow)) == null) || ((getRow(currentRow).getCTRow().getOutlineLevel()) < level)) {
				break;
			}
		}
		return currentRow;
	}

	private boolean isRowGroupHiddenByParent(int row) {
		int endLevel;
		boolean endHidden;
		int endOfOutlineGroupIdx = findEndOfRowOutlineGroup(row);
		if ((getRow(endOfOutlineGroupIdx)) == null) {
			endLevel = 0;
			endHidden = false;
		}else {
			endLevel = getRow(endOfOutlineGroupIdx).getCTRow().getOutlineLevel();
			endHidden = getRow(endOfOutlineGroupIdx).getCTRow().getHidden();
		}
		int startLevel;
		boolean startHidden;
		int startOfOutlineGroupIdx = findStartOfRowOutlineGroup(row);
		if ((startOfOutlineGroupIdx < 0) || ((getRow(startOfOutlineGroupIdx)) == null)) {
			startLevel = 0;
			startHidden = false;
		}else {
			startLevel = getRow(startOfOutlineGroupIdx).getCTRow().getOutlineLevel();
			startHidden = getRow(startOfOutlineGroupIdx).getCTRow().getHidden();
		}
		if (endLevel > startLevel) {
			return endHidden;
		}
		return startHidden;
	}

	private boolean isRowGroupCollapsed(int row) {
		int collapseRow = (findEndOfRowOutlineGroup(row)) + 1;
		if ((getRow(collapseRow)) == null) {
			return false;
		}
		return getRow(collapseRow).getCTRow().getCollapsed();
	}

	@Override
	public void setZoom(int scale) {
		if ((scale < 10) || (scale > 400)) {
			throw new IllegalArgumentException("Valid scale values range from 10 to 400");
		}
		final CTSheetView dsv = getDefaultSheetView(true);
		assert dsv != null;
		dsv.setZoomScale(scale);
	}

	@org.apache.poi.util.Beta
	public void copyRows(List<? extends Row> srcRows, int destStartRow, CellCopyPolicy policy) {
		if ((srcRows == null) || ((srcRows.size()) == 0)) {
			throw new IllegalArgumentException("No rows to copy");
		}
		final Row srcStartRow = srcRows.get(0);
		final Row srcEndRow = srcRows.get(((srcRows.size()) - 1));
		if (srcStartRow == null) {
			throw new IllegalArgumentException("copyRows: First row cannot be null");
		}
		final int srcStartRowNum = srcStartRow.getRowNum();
		final int srcEndRowNum = srcEndRow.getRowNum();
		final int size = srcRows.size();
		for (int index = 1; index < size; index++) {
			final Row curRow = srcRows.get(index);
			if (curRow == null) {
				throw new IllegalArgumentException((("srcRows may not contain null rows. Found null row at index " + index) + "."));
			}else
				if ((srcStartRow.getSheet().getWorkbook()) != (curRow.getSheet().getWorkbook())) {
					throw new IllegalArgumentException((((((((("All rows in srcRows must belong to the same sheet in the same workbook. " + "Expected all rows from same workbook (") + (srcStartRow.getSheet().getWorkbook())) + "). ") + "Got srcRows[") + index) + "] from different workbook (") + (curRow.getSheet().getWorkbook())) + ")."));
				}else
					if ((srcStartRow.getSheet()) != (curRow.getSheet())) {
						throw new IllegalArgumentException(((((((("All rows in srcRows must belong to the same sheet. " + "Expected all rows from ") + (srcStartRow.getSheet().getSheetName())) + ". ") + "Got srcRows[") + index) + "] from ") + (curRow.getSheet().getSheetName())));
					}


		}
		final CellCopyPolicy options = new CellCopyPolicy(policy);
		options.setCopyMergedRegions(false);
		int r = destStartRow;
		for (Row srcRow : srcRows) {
			int destRowNum;
			if (policy.isCondenseRows()) {
				destRowNum = r++;
			}else {
				final int shift = (srcRow.getRowNum()) - srcStartRowNum;
				destRowNum = destStartRow + shift;
			}
			final XSSFRow destRow = createRow(destRowNum);
			destRow.copyRowFrom(srcRow, options);
		}
		if (policy.isCopyMergedRegions()) {
			final int shift = destStartRow - srcStartRowNum;
			for (CellRangeAddress srcRegion : srcStartRow.getSheet().getMergedRegions()) {
				if ((srcStartRowNum <= (srcRegion.getFirstRow())) && ((srcRegion.getLastRow()) <= srcEndRowNum)) {
					final CellRangeAddress destRegion = srcRegion.copy();
					destRegion.setFirstRow(((destRegion.getFirstRow()) + shift));
					destRegion.setLastRow(((destRegion.getLastRow()) + shift));
					addMergedRegion(destRegion);
				}
			}
		}
	}

	@org.apache.poi.util.Beta
	public void copyRows(int srcStartRow, int srcEndRow, int destStartRow, CellCopyPolicy cellCopyPolicy) {
		final List<XSSFRow> srcRows = getRows(srcStartRow, srcEndRow, false);
		copyRows(srcRows, destStartRow, cellCopyPolicy);
	}

	@Override
	public void shiftRows(int startRow, int endRow, int n) {
		shiftRows(startRow, endRow, n, false, false);
	}

	@Override
	public void shiftRows(int startRow, int endRow, final int n, boolean copyRowHeight, boolean resetOriginalRowHeight) {
		XSSFVMLDrawing vml = getVMLDrawing(false);
		int sheetIndex = getWorkbook().getSheetIndex(this);
		String sheetName = getWorkbook().getSheetName(sheetIndex);
		FormulaShifter formulaShifter = FormulaShifter.createForRowShift(sheetIndex, sheetName, startRow, endRow, n, SpreadsheetVersion.EXCEL2007);
		removeOverwritten(vml, startRow, endRow, n);
		shiftCommentsAndRows(vml, startRow, endRow, n);
		rebuildRows();
	}

	@Override
	public void shiftColumns(int startColumn, int endColumn, final int n) {
		XSSFVMLDrawing vml = getVMLDrawing(false);
		shiftCommentsForColumns(vml, startColumn, endColumn, n);
		FormulaShifter formulaShifter = FormulaShifter.createForColumnShift(this.getWorkbook().getSheetIndex(this), this.getSheetName(), startColumn, endColumn, n, SpreadsheetVersion.EXCEL2007);
		rebuildRows();
	}

	private void rebuildRows() {
		List<XSSFRow> rowList = new ArrayList<>(_rows.values());
		_rows.clear();
		for (XSSFRow r : rowList) {
			final Integer rownumI = new Integer(r.getRowNum());
			_rows.put(rownumI, r);
		}
	}

	private void removeOverwritten(XSSFVMLDrawing vml, int startRow, int endRow, final int n) {
		for (Iterator<Row> it = rowIterator(); it.hasNext();) {
			XSSFRow row = ((XSSFRow) (it.next()));
			int rownum = row.getRowNum();
			if (XSSFSheet.shouldRemoveRow(startRow, endRow, n, rownum)) {
				final Integer rownumI = Integer.valueOf(row.getRowNum());
				int idx = _rows.headMap(rownumI).size();
				worksheet.getSheetData().removeRow(idx);
				it.remove();
				if ((sheetComments) != null) {
					CTCommentList lst = sheetComments.getCTComments().getCommentList();
					for (CTComment comment : lst.getCommentArray()) {
						String strRef = comment.getRef();
						CellAddress ref = new CellAddress(strRef);
						if ((ref.getRow()) == rownum) {
							sheetComments.removeComment(ref);
						}
					}
				}
				if ((hyperlinks) != null) {
					for (XSSFHyperlink link : new ArrayList<>(hyperlinks)) {
						CellReference ref = new CellReference(link.getCellRef());
						if ((ref.getRow()) == rownum) {
							hyperlinks.remove(link);
						}
					}
				}
			}
		}
	}

	private void shiftCommentsAndRows(XSSFVMLDrawing vml, int startRow, int endRow, final int n) {
		SortedMap<XSSFComment, Integer> commentsToShift = new TreeMap<>(( o1, o2) -> {
			int row1 = o1.getRow();
			int row2 = o2.getRow();
			if (row1 == row2) {
				return (o1.hashCode()) - (o2.hashCode());
			}
			if (n > 0) {
				return row1 < row2 ? 1 : -1;
			}else {
				return row1 > row2 ? 1 : -1;
			}
		});
		for (Iterator<Row> it = rowIterator(); it.hasNext();) {
			XSSFRow row = ((XSSFRow) (it.next()));
			int rownum = row.getRowNum();
			if ((sheetComments) != null) {
				int newrownum = shiftedRowNum(startRow, endRow, n, rownum);
				if (newrownum != rownum) {
					CTCommentList lst = sheetComments.getCTComments().getCommentList();
					for (CTComment comment : lst.getCommentArray()) {
						String oldRef = comment.getRef();
						CellReference ref = new CellReference(oldRef);
						if ((ref.getRow()) == rownum) {
							XSSFComment xssfComment = new XSSFComment(sheetComments, comment, (vml == null ? null : vml.findCommentShape(rownum, ref.getCol())));
							commentsToShift.put(xssfComment, newrownum);
						}
					}
				}
			}
			if ((rownum < startRow) || (rownum > endRow)) {
				continue;
			}
		}
		for (Map.Entry<XSSFComment, Integer> entry : commentsToShift.entrySet()) {
			entry.getKey().setRow(entry.getValue());
		}
		rebuildRows();
	}

	private int shiftedRowNum(int startRow, int endRow, int n, int rownum) {
		if ((rownum < startRow) && ((n > 0) || ((startRow - rownum) > n))) {
			return rownum;
		}
		if ((rownum > endRow) && ((n < 0) || ((rownum - endRow) > n))) {
			return rownum;
		}
		if (rownum < startRow) {
			return rownum + (endRow - startRow);
		}
		if (rownum > endRow) {
			return rownum - (endRow - startRow);
		}
		return rownum + n;
	}

	private void shiftCommentsForColumns(XSSFVMLDrawing vml, int startColumnIndex, int endColumnIndex, final int n) {
		SortedMap<XSSFComment, Integer> commentsToShift = new TreeMap<>(( o1, o2) -> {
			int column1 = o1.getColumn();
			int column2 = o2.getColumn();
			if (column1 == column2) {
				return (o1.hashCode()) - (o2.hashCode());
			}
			if (n > 0) {
				return column1 < column2 ? 1 : -1;
			}else {
				return column1 > column2 ? 1 : -1;
			}
		});
		if ((sheetComments) != null) {
			CTCommentList lst = sheetComments.getCTComments().getCommentList();
			for (CTComment comment : lst.getCommentArray()) {
				String oldRef = comment.getRef();
				CellReference ref = new CellReference(oldRef);
				int columnIndex = ref.getCol();
				int newColumnIndex = shiftedRowNum(startColumnIndex, endColumnIndex, n, columnIndex);
				if (newColumnIndex != columnIndex) {
					XSSFComment xssfComment = new XSSFComment(sheetComments, comment, (vml == null ? null : vml.findCommentShape(ref.getRow(), columnIndex)));
					commentsToShift.put(xssfComment, newColumnIndex);
				}
			}
		}
		for (Map.Entry<XSSFComment, Integer> entry : commentsToShift.entrySet()) {
			entry.getKey().setColumn(entry.getValue());
		}
		rebuildRows();
	}

	@Override
	public void showInPane(int toprow, int leftcol) {
		final CellReference cellReference = new CellReference(toprow, leftcol);
		final String cellRef = cellReference.formatAsString();
		final CTPane pane = getPane(true);
		assert pane != null;
		pane.setTopLeftCell(cellRef);
	}

	@Override
	public void ungroupColumn(int fromColumn, int toColumn) {
		CTCols cols = worksheet.getColsArray(0);
		for (int index = fromColumn; index <= toColumn; index++) {
			CTCol col = columnHelper.getColumn(index, false);
			if (col != null) {
				short outlineLevel = col.getOutlineLevel();
				col.setOutlineLevel(((short) (outlineLevel - 1)));
				index = ((int) (col.getMax()));
				if ((col.getOutlineLevel()) <= 0) {
					int colIndex = columnHelper.getIndexOfColumn(cols, col);
					worksheet.getColsArray(0).removeCol(colIndex);
				}
			}
		}
		worksheet.setColsArray(0, cols);
		setSheetFormatPrOutlineLevelCol();
	}

	@Override
	public void ungroupRow(int fromRow, int toRow) {
		for (int i = fromRow; i <= toRow; i++) {
			XSSFRow xrow = getRow(i);
			if (xrow != null) {
				CTRow ctRow = xrow.getCTRow();
				int outlineLevel = ctRow.getOutlineLevel();
				ctRow.setOutlineLevel(((short) (outlineLevel - 1)));
				if ((outlineLevel == 1) && ((xrow.getFirstCellNum()) == (-1))) {
					removeRow(xrow);
				}
			}
		}
		setSheetFormatPrOutlineLevelRow();
	}

	private void setSheetFormatPrOutlineLevelRow() {
		short maxLevelRow = getMaxOutlineLevelRows();
		getSheetTypeSheetFormatPr().setOutlineLevelRow(maxLevelRow);
	}

	private void setSheetFormatPrOutlineLevelCol() {
		short maxLevelCol = getMaxOutlineLevelCols();
		getSheetTypeSheetFormatPr().setOutlineLevelCol(maxLevelCol);
	}

	protected CTSheetViews getSheetTypeSheetViews(final boolean create) {
		final CTSheetViews views = ((worksheet.isSetSheetViews()) || (!create)) ? worksheet.getSheetViews() : worksheet.addNewSheetViews();
		assert (views != null) || (!create);
		if (views == null) {
			return null;
		}
		if (((views.sizeOfSheetViewArray()) == 0) && create) {
			views.addNewSheetView();
		}
		return views;
	}

	@Override
	public boolean isSelected() {
		final CTSheetView dsv = getDefaultSheetView(false);
		return dsv != null ? dsv.getTabSelected() : false;
	}

	@Override
	public void setSelected(boolean value) {
		final CTSheetViews views = getSheetTypeSheetViews(true);
		assert views != null;
		for (CTSheetView view : views.getSheetViewArray()) {
			view.setTabSelected(value);
		}
	}

	@org.apache.poi.util.Internal
	public void addHyperlink(XSSFHyperlink hyperlink) {
		hyperlinks.add(hyperlink);
	}

	@org.apache.poi.util.Internal
	public void removeHyperlink(int row, int column) {
		String ref = new CellReference(row, column).formatAsString();
		for (Iterator<XSSFHyperlink> it = hyperlinks.iterator(); it.hasNext();) {
			XSSFHyperlink hyperlink = it.next();
			if (hyperlink.getCellRef().equals(ref)) {
				it.remove();
				return;
			}
		}
	}

	@Override
	public CellAddress getActiveCell() {
		final CTSelection sts = getSheetTypeSelection(false);
		final String address = (sts != null) ? sts.getActiveCell() : null;
		return address != null ? new CellAddress(address) : null;
	}

	@Override
	public void setActiveCell(CellAddress address) {
		final CTSelection ctsel = getSheetTypeSelection(true);
		assert ctsel != null;
		String ref = address.formatAsString();
		ctsel.setActiveCell(ref);
		ctsel.setSqref(Collections.singletonList(ref));
	}

	public boolean hasComments() {
		return ((sheetComments) != null) && ((sheetComments.getNumberOfComments()) > 0);
	}

	protected int getNumberOfComments() {
		return (sheetComments) == null ? 0 : sheetComments.getNumberOfComments();
	}

	private CTSelection getSheetTypeSelection(final boolean create) {
		final CTSheetView dsv = getDefaultSheetView(create);
		assert (dsv != null) || (!create);
		if (dsv == null) {
			return null;
		}
		final int sz = dsv.sizeOfSelectionArray();
		if (sz == 0) {
			return create ? dsv.addNewSelection() : null;
		}
		return dsv.getSelectionArray((sz - 1));
	}

	private CTSheetView getDefaultSheetView(final boolean create) {
		final CTSheetViews views = getSheetTypeSheetViews(create);
		assert (views != null) || (!create);
		if (views == null) {
			return null;
		}
		final int sz = views.sizeOfSheetViewArray();
		assert (sz > 0) || (!create);
		return sz == 0 ? null : views.getSheetViewArray((sz - 1));
	}

	protected CommentsTable getCommentsTable(boolean create) {
		if (((sheetComments) == null) && create) {
			try {
				sheetComments = ((CommentsTable) (createRelationship(XSSFRelation.SHEET_COMMENTS, XSSFFactory.getInstance(), ((int) (sheet.getSheetId())))));
			} catch (PartAlreadyExistsException e) {
				sheetComments = ((CommentsTable) (createRelationship(XSSFRelation.SHEET_COMMENTS, XSSFFactory.getInstance(), (-1))));
			}
		}
		return sheetComments;
	}

	private CTPageSetUpPr getSheetTypePageSetUpPr() {
		CTSheetPr sheetPr = getSheetTypeSheetPr();
		return sheetPr.isSetPageSetUpPr() ? sheetPr.getPageSetUpPr() : sheetPr.addNewPageSetUpPr();
	}

	private static boolean shouldRemoveRow(int startRow, int endRow, int n, int rownum) {
		if ((rownum >= (startRow + n)) && (rownum <= (endRow + n))) {
			if ((n > 0) && (rownum > endRow)) {
				return true;
			}else
				if ((n < 0) && (rownum < startRow)) {
					return true;
				}

		}
		return false;
	}

	private CTPane getPane(final boolean create) {
		final CTSheetView dsv = getDefaultSheetView(create);
		assert (dsv != null) || (!create);
		if (dsv == null) {
			return null;
		}
		return (dsv.isSetPane()) || (!create) ? dsv.getPane() : dsv.addNewPane();
	}

	@org.apache.poi.util.Internal
	public CTCellFormula getSharedFormula(int sid) {
		return sharedFormulas.get(sid);
	}

	void onReadCell(XSSFCell cell) {
		CTCell ct = cell.getCTCell();
		CTCellFormula f = ct.getF();
		if ((((f != null) && ((f.getT()) == (STCellFormulaType.SHARED))) && (f.isSetRef())) && ((f.getStringValue()) != null)) {
			CTCellFormula sf = ((CTCellFormula) (f.copy()));
			CellRangeAddress sfRef = CellRangeAddress.valueOf(sf.getRef());
			CellReference cellRef = new CellReference(cell);
			if (((cellRef.getCol()) > (sfRef.getFirstColumn())) || ((cellRef.getRow()) > (sfRef.getFirstRow()))) {
				String effectiveRef = new CellRangeAddress(Math.max(cellRef.getRow(), sfRef.getFirstRow()), sfRef.getLastRow(), Math.max(cellRef.getCol(), sfRef.getFirstColumn()), sfRef.getLastColumn()).formatAsString();
				sf.setRef(effectiveRef);
			}
			sharedFormulas.put(((int) (f.getSi())), sf);
		}
		if (((f != null) && ((f.getT()) == (STCellFormulaType.ARRAY))) && ((f.getRef()) != null)) {
			arrayFormulas.add(CellRangeAddress.valueOf(f.getRef()));
		}
	}

	@Override
	protected void commit() throws IOException {
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		write(out);
		out.close();
	}

	protected void write(OutputStream out) throws IOException {
		boolean setToNull = false;
		if ((worksheet.sizeOfColsArray()) == 1) {
			CTCols col = worksheet.getColsArray(0);
			if ((col.sizeOfColArray()) == 0) {
				setToNull = true;
				worksheet.setColsArray(null);
			}else {
				setColWidthAttribute(col);
			}
		}
		if ((hyperlinks.size()) > 0) {
			if ((worksheet.getHyperlinks()) == null) {
				worksheet.addNewHyperlinks();
			}
			CTHyperlink[] ctHls = new CTHyperlink[hyperlinks.size()];
			for (int i = 0; i < (ctHls.length); i++) {
				XSSFHyperlink hyperlink = hyperlinks.get(i);
				ctHls[i] = hyperlink.getCTHyperlink();
			}
			worksheet.getHyperlinks().setHyperlinkArray(ctHls);
		}else {
			if ((worksheet.getHyperlinks()) != null) {
				final int count = worksheet.getHyperlinks().sizeOfHyperlinkArray();
				for (int i = count - 1; i >= 0; i--) {
					worksheet.getHyperlinks().removeHyperlink(i);
				}
				worksheet.unsetHyperlinks();
			}
		}
		int minCell = Integer.MAX_VALUE;
		int maxCell = Integer.MIN_VALUE;
		for (Map.Entry<Integer, XSSFRow> entry : _rows.entrySet()) {
			XSSFRow row = entry.getValue();
			if ((row.getFirstCellNum()) != (-1)) {
				minCell = Math.min(minCell, row.getFirstCellNum());
			}
			if ((row.getLastCellNum()) != (-1)) {
				maxCell = Math.max(maxCell, ((row.getLastCellNum()) - 1));
			}
		}
		if (minCell != (Integer.MAX_VALUE)) {
			String ref = new CellRangeAddress(getFirstRowNum(), getLastRowNum(), minCell, maxCell).formatAsString();
			if (worksheet.isSetDimension()) {
				worksheet.getDimension().setRef(ref);
			}else {
				worksheet.addNewDimension().setRef(ref);
			}
		}
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		xmlOptions.setSaveSyntheticDocumentElement(new QName(CTWorksheet.type.getName().getNamespaceURI(), "worksheet"));
		worksheet.save(out, xmlOptions);
		if (setToNull) {
			worksheet.addNewCols();
		}
	}

	public boolean isAutoFilterLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getAutoFilter());
	}

	public boolean isDeleteColumnsLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getDeleteColumns());
	}

	public boolean isDeleteRowsLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getDeleteRows());
	}

	public boolean isFormatCellsLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getFormatCells());
	}

	public boolean isFormatColumnsLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getFormatColumns());
	}

	public boolean isFormatRowsLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getFormatRows());
	}

	public boolean isInsertColumnsLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getInsertColumns());
	}

	public boolean isInsertHyperlinksLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getInsertHyperlinks());
	}

	public boolean isInsertRowsLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getInsertRows());
	}

	public boolean isPivotTablesLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getPivotTables());
	}

	public boolean isSortLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getSort());
	}

	public boolean isObjectsLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getObjects());
	}

	public boolean isScenariosLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getScenarios());
	}

	public boolean isSelectLockedCellsLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getSelectLockedCells());
	}

	public boolean isSelectUnlockedCellsLocked() {
		return (isSheetLocked()) && (safeGetProtectionField().getSelectUnlockedCells());
	}

	public boolean isSheetLocked() {
		return (worksheet.isSetSheetProtection()) && (safeGetProtectionField().getSheet());
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
		if (!(isSheetProtectionEnabled())) {
			return worksheet.addNewSheetProtection();
		}
		return worksheet.getSheetProtection();
	}

	boolean isSheetProtectionEnabled() {
		return worksheet.isSetSheetProtection();
	}

	boolean isCellInArrayFormulaContext(XSSFCell cell) {
		for (CellRangeAddress range : arrayFormulas) {
			if (range.isInRange(cell.getRowIndex(), cell.getColumnIndex())) {
				return true;
			}
		}
		return false;
	}

	XSSFCell getFirstCellInArrayFormula(XSSFCell cell) {
		for (CellRangeAddress range : arrayFormulas) {
			if (range.isInRange(cell.getRowIndex(), cell.getColumnIndex())) {
				return getRow(range.getFirstRow()).getCell(range.getFirstColumn());
			}
		}
		return null;
	}

	private CellRange<XSSFCell> getCellRange(CellRangeAddress range) {
		int firstRow = range.getFirstRow();
		int firstColumn = range.getFirstColumn();
		int lastRow = range.getLastRow();
		int lastColumn = range.getLastColumn();
		int height = (lastRow - firstRow) + 1;
		int width = (lastColumn - firstColumn) + 1;
		List<XSSFCell> temp = new ArrayList<>((height * width));
		for (int rowIn = firstRow; rowIn <= lastRow; rowIn++) {
			for (int colIn = firstColumn; colIn <= lastColumn; colIn++) {
				XSSFRow row = getRow(rowIn);
				if (row == null) {
					row = createRow(rowIn);
				}
				XSSFCell cell = row.getCell(colIn);
				if (cell == null) {
					cell = row.createCell(colIn);
				}
				temp.add(cell);
			}
		}
		return SSCellRange.create(firstRow, firstColumn, height, width, temp, XSSFCell.class);
	}

	@Override
	public CellRange<XSSFCell> setArrayFormula(String formula, CellRangeAddress range) {
		CellRange<XSSFCell> cr = getCellRange(range);
		XSSFCell mainArrayFormulaCell = cr.getTopLeftCell();
		arrayFormulas.add(range);
		return cr;
	}

	@Override
	public CellRange<XSSFCell> removeArrayFormula(Cell cell) {
		if ((cell.getSheet()) != (this)) {
			throw new IllegalArgumentException("Specified cell does not belong to this sheet.");
		}
		for (CellRangeAddress range : arrayFormulas) {
			if (range.isInRange(cell)) {
				arrayFormulas.remove(range);
				CellRange<XSSFCell> cr = getCellRange(range);
				for (XSSFCell c : cr) {
					c.setCellType(CellType.BLANK);
				}
				return cr;
			}
		}
		String ref = ((XSSFCell) (cell)).getCTCell().getR();
		throw new IllegalArgumentException((("Cell " + ref) + " is not part of an array formula."));
	}

	@Override
	public DataValidationHelper getDataValidationHelper() {
		return dataValidationHelper;
	}

	@Override
	public List<XSSFDataValidation> getDataValidations() {
		List<XSSFDataValidation> xssfValidations = new ArrayList<>();
		CTDataValidations dataValidations = this.worksheet.getDataValidations();
		if ((dataValidations != null) && ((dataValidations.getCount()) > 0)) {
			for (CTDataValidation ctDataValidation : dataValidations.getDataValidationArray()) {
				CellRangeAddressList addressList = new CellRangeAddressList();
				@SuppressWarnings("unchecked")
				List<String> sqref = ctDataValidation.getSqref();
				for (String stRef : sqref) {
					String[] regions = stRef.split(" ");
					for (String region : regions) {
						String[] parts = region.split(":");
						CellReference begin = new CellReference(parts[0]);
						CellReference end = ((parts.length) > 1) ? new CellReference(parts[1]) : begin;
						CellRangeAddress cellRangeAddress = new CellRangeAddress(begin.getRow(), end.getRow(), begin.getCol(), end.getCol());
						addressList.addCellRangeAddress(cellRangeAddress);
					}
				}
			}
		}
		return xssfValidations;
	}

	@Override
	public void addValidationData(DataValidation dataValidation) {
		XSSFDataValidation xssfDataValidation = ((XSSFDataValidation) (dataValidation));
		CTDataValidations dataValidations = worksheet.getDataValidations();
		if (dataValidations == null) {
			dataValidations = worksheet.addNewDataValidations();
		}
		int currentCount = dataValidations.sizeOfDataValidationArray();
		CTDataValidation newval = dataValidations.addNewDataValidation();
		dataValidations.setCount((currentCount + 1));
	}

	@Override
	public XSSFAutoFilter setAutoFilter(CellRangeAddress range) {
		CTAutoFilter af = worksheet.getAutoFilter();
		if (af == null) {
			af = worksheet.addNewAutoFilter();
		}
		CellRangeAddress norm = new CellRangeAddress(range.getFirstRow(), range.getLastRow(), range.getFirstColumn(), range.getLastColumn());
		String ref = norm.formatAsString();
		af.setRef(ref);
		XSSFWorkbook wb = getWorkbook();
		int sheetIndex = getWorkbook().getSheetIndex(this);
		CellReference r1 = new CellReference(getSheetName(), range.getFirstRow(), range.getFirstColumn(), true, true);
		CellReference r2 = new CellReference(null, range.getLastRow(), range.getLastColumn(), true, true);
		String fmla = ((r1.formatAsString()) + ":") + (r2.formatAsString());
		return null;
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2.0")
	public XSSFTable createTable() {
		return createTable(null);
	}

	public XSSFTable createTable(AreaReference tableArea) {
		if (!(worksheet.isSetTableParts())) {
			worksheet.addNewTableParts();
		}
		CTTableParts tblParts = worksheet.getTableParts();
		CTTablePart tbl = tblParts.addNewTablePart();
		int tableNumber = (getPackagePart().getPackage().getPartsByContentType(XSSFRelation.TABLE.getContentType()).size()) + 1;
		boolean loop = true;
		while (loop) {
			loop = false;
			for (PackagePart packagePart : getPackagePart().getPackage().getPartsByContentType(XSSFRelation.TABLE.getContentType())) {
				String fileName = XSSFRelation.TABLE.getFileName(tableNumber);
				if (fileName.equals(packagePart.getPartName().getName())) {
					tableNumber++;
					loop = true;
				}
			}
		} 
		POIXMLDocumentPart.RelationPart rp = createRelationship(XSSFRelation.TABLE, XSSFFactory.getInstance(), tableNumber, false);
		XSSFTable table = rp.getDocumentPart();
		tbl.setId(rp.getRelationship().getId());
		table.getCTTable().setId(tableNumber);
		tables.put(tbl.getId(), table);
		if (tableArea != null) {
			table.setArea(tableArea);
		}
		return table;
	}

	public List<XSSFTable> getTables() {
		return new ArrayList<>(tables.values());
	}

	public void removeTable(XSSFTable t) {
		long id = t.getCTTable().getId();
		Map.Entry<String, XSSFTable> toDelete = null;
		for (Map.Entry<String, XSSFTable> entry : tables.entrySet()) {
			if ((entry.getValue().getCTTable().getId()) == id)
				toDelete = entry;

		}
		if (toDelete != null) {
			removeRelation(getRelationById(toDelete.getKey()), true);
			tables.remove(toDelete.getKey());
		}
	}

	@Override
	public XSSFSheetConditionalFormatting getSheetConditionalFormatting() {
		return null;
	}

	public XSSFColor getTabColor() {
		CTSheetPr pr = worksheet.getSheetPr();
		if (pr == null) {
			pr = worksheet.addNewSheetPr();
		}
		if (!(pr.isSetTabColor())) {
			return null;
		}
		return XSSFColor.from(pr.getTabColor(), getWorkbook().getStylesSource().getIndexedColors());
	}

	public void setTabColor(XSSFColor color) {
		CTSheetPr pr = worksheet.getSheetPr();
		if (pr == null) {
			pr = worksheet.addNewSheetPr();
		}
		pr.setTabColor(color.getCTColor());
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
		int col1 = -1;
		int col2 = -1;
		int row1 = -1;
		int row2 = -1;
		if (rowDef != null) {
			row1 = rowDef.getFirstRow();
			row2 = rowDef.getLastRow();
			if (((((row1 == (-1)) && (row2 != (-1))) || (row1 < (-1))) || (row2 < (-1))) || (row1 > row2)) {
				throw new IllegalArgumentException("Invalid row range specification");
			}
		}
		if (colDef != null) {
			col1 = colDef.getFirstColumn();
			col2 = colDef.getLastColumn();
			if (((((col1 == (-1)) && (col2 != (-1))) || (col1 < (-1))) || (col2 < (-1))) || (col1 > col2)) {
				throw new IllegalArgumentException("Invalid column range specification");
			}
		}
		int sheetIndex = getWorkbook().getSheetIndex(this);
		boolean removeAll = (rowDef == null) && (colDef == null);
		if (removeAll) {
			return;
		}
		if ((worksheet.isSetPageSetup()) && (worksheet.isSetPageMargins())) {
		}else {
			getPrintSetup().setValidSettings(false);
		}
	}

	private static String getReferenceBuiltInRecord(String sheetName, int startC, int endC, int startR, int endR) {
		CellReference colRef = new CellReference(sheetName, 0, startC, true, true);
		CellReference colRef2 = new CellReference(sheetName, 0, endC, true, true);
		CellReference rowRef = new CellReference(sheetName, startR, 0, true, true);
		CellReference rowRef2 = new CellReference(sheetName, endR, 0, true, true);
		String escapedName = SheetNameFormatter.format(sheetName);
		String c = "";
		String r = "";
		if ((startC != (-1)) || (endC != (-1))) {
			String col1 = colRef.getCellRefParts()[2];
			String col2 = colRef2.getCellRefParts()[2];
			c = (((escapedName + "!$") + col1) + ":$") + col2;
		}
		if ((startR != (-1)) || (endR != (-1))) {
			String row1 = rowRef.getCellRefParts()[1];
			String row2 = rowRef2.getCellRefParts()[1];
			if ((!(row1.equals("0"))) && (!(row2.equals("0")))) {
				r = (((escapedName + "!$") + row1) + ":$") + row2;
			}
		}
		StringBuilder rng = new StringBuilder();
		rng.append(c);
		if (((rng.length()) > 0) && ((r.length()) > 0)) {
			rng.append(',');
		}
		rng.append(r);
		return rng.toString();
	}

	private CellRangeAddress getRepeatingRowsOrColums(boolean rows) {
		int sheetIndex = getWorkbook().getSheetIndex(this);
		int maxRowIndex = SpreadsheetVersion.EXCEL2007.getLastRowIndex();
		int maxColIndex = SpreadsheetVersion.EXCEL2007.getLastColumnIndex();
		return null;
	}

	@org.apache.poi.util.Beta
	private XSSFPivotTable createPivotTable() {
		XSSFWorkbook wb = getWorkbook();
		List<XSSFPivotTable> pivotTables = wb.getPivotTables();
		int tableId = (getWorkbook().getPivotTables().size()) + 1;
		XSSFPivotTable pivotTable = ((XSSFPivotTable) (createRelationship(XSSFRelation.PIVOT_TABLE, XSSFFactory.getInstance(), tableId)));
		pivotTables.add(pivotTable);
		XSSFWorkbook workbook = getWorkbook();
		XSSFPivotCacheDefinition pivotCacheDefinition = ((XSSFPivotCacheDefinition) (workbook.createRelationship(XSSFRelation.PIVOT_CACHE_DEFINITION, XSSFFactory.getInstance(), tableId)));
		String rId = workbook.getRelationId(pivotCacheDefinition);
		PackagePart pivotPackagePart = pivotTable.getPackagePart();
		pivotPackagePart.addRelationship(pivotCacheDefinition.getPackagePart().getPartName(), TargetMode.INTERNAL, XSSFRelation.PIVOT_CACHE_DEFINITION.getRelation());
		pivotTable.setPivotCacheDefinition(pivotCacheDefinition);
		XSSFPivotCacheRecords pivotCacheRecords = ((XSSFPivotCacheRecords) (pivotCacheDefinition.createRelationship(XSSFRelation.PIVOT_CACHE_RECORDS, XSSFFactory.getInstance(), tableId)));
		pivotTable.getPivotCacheDefinition().getCTPivotCacheDefinition().setId(pivotCacheDefinition.getRelationId(pivotCacheRecords));
		return pivotTable;
	}

	@org.apache.poi.util.Beta
	public XSSFPivotTable createPivotTable(final AreaReference source, CellReference position, Sheet sourceSheet) {
		final String sourceSheetName = source.getFirstCell().getSheetName();
		if ((sourceSheetName != null) && (!(sourceSheetName.equalsIgnoreCase(sourceSheet.getSheetName())))) {
			throw new IllegalArgumentException(((("The area is referenced in another sheet than the " + "defined source sheet ") + (sourceSheet.getSheetName())) + "."));
		}
		return null;
	}

	@org.apache.poi.util.Beta
	public XSSFPivotTable createPivotTable(AreaReference source, CellReference position) {
		final String sourceSheetName = source.getFirstCell().getSheetName();
		if ((sourceSheetName != null) && (!(sourceSheetName.equalsIgnoreCase(this.getSheetName())))) {
		}
		return createPivotTable(source, position, this);
	}

	@org.apache.poi.util.Beta
	public XSSFPivotTable createPivotTable(final Name source, CellReference position, Sheet sourceSheet) {
		if (((source.getSheetName()) != null) && (!(source.getSheetName().equals(sourceSheet.getSheetName())))) {
			throw new IllegalArgumentException(((("The named range references another sheet than the " + "defined source sheet ") + (sourceSheet.getSheetName())) + "."));
		}
		return null;
	}

	@org.apache.poi.util.Beta
	public XSSFPivotTable createPivotTable(Name source, CellReference position) {
		return createPivotTable(source, position, getWorkbook().getSheet(source.getSheetName()));
	}

	@org.apache.poi.util.Beta
	public XSSFPivotTable createPivotTable(final Table source, CellReference position) {
		return null;
	}

	@org.apache.poi.util.Beta
	public List<XSSFPivotTable> getPivotTables() {
		List<XSSFPivotTable> tables = new ArrayList<>();
		for (XSSFPivotTable table : getWorkbook().getPivotTables()) {
			if ((table.getParent()) == (this)) {
				tables.add(table);
			}
		}
		return tables;
	}

	@Override
	public int getColumnOutlineLevel(int columnIndex) {
		CTCol col = columnHelper.getColumn(columnIndex, false);
		if (col == null) {
			return 0;
		}
		return col.getOutlineLevel();
	}

	public void addIgnoredErrors(CellReference cell, IgnoredErrorType... ignoredErrorTypes) {
		addIgnoredErrors(cell.formatAsString(), ignoredErrorTypes);
	}

	public void addIgnoredErrors(CellRangeAddress region, IgnoredErrorType... ignoredErrorTypes) {
		region.validate(SpreadsheetVersion.EXCEL2007);
		addIgnoredErrors(region.formatAsString(), ignoredErrorTypes);
	}

	public Map<IgnoredErrorType, Set<CellRangeAddress>> getIgnoredErrors() {
		Map<IgnoredErrorType, Set<CellRangeAddress>> result = new LinkedHashMap<>();
		if (worksheet.isSetIgnoredErrors()) {
			for (CTIgnoredError err : worksheet.getIgnoredErrors().getIgnoredErrorList()) {
				for (IgnoredErrorType errType : XSSFIgnoredErrorHelper.getErrorTypes(err)) {
					if (!(result.containsKey(errType))) {
						result.put(errType, new LinkedHashSet<>());
					}
					for (Object ref : err.getSqref()) {
						result.get(errType).add(CellRangeAddress.valueOf(ref.toString()));
					}
				}
			}
		}
		return result;
	}

	private void addIgnoredErrors(String ref, IgnoredErrorType... ignoredErrorTypes) {
		CTIgnoredErrors ctIgnoredErrors = (worksheet.isSetIgnoredErrors()) ? worksheet.getIgnoredErrors() : worksheet.addNewIgnoredErrors();
		CTIgnoredError ctIgnoredError = ctIgnoredErrors.addNewIgnoredError();
		XSSFIgnoredErrorHelper.addIgnoredErrors(ctIgnoredError, ref, ignoredErrorTypes);
	}

	protected void onSheetDelete() {
		for (POIXMLDocumentPart.RelationPart part : getRelationParts()) {
			if ((part.getDocumentPart()) instanceof XSSFTable) {
				removeTable(part.getDocumentPart());
				continue;
			}
			removeRelation(part.getDocumentPart(), true);
		}
	}

	protected void onDeleteFormula(XSSFCell cell, BaseXSSFEvaluationWorkbook evalWb) {
		CTCellFormula f = cell.getCTCell().getF();
		if ((((f != null) && ((f.getT()) == (STCellFormulaType.SHARED))) && (f.isSetRef())) && ((f.getStringValue()) != null)) {
			CellRangeAddress ref = CellRangeAddress.valueOf(f.getRef());
			if ((ref.getNumberOfCells()) > 1) {
				DONE : for (int i = cell.getRowIndex(); i <= (ref.getLastRow()); i++) {
					XSSFRow row = getRow(i);
					if (row != null)
						for (int j = cell.getColumnIndex(); j <= (ref.getLastColumn()); j++) {
							XSSFCell nextCell = row.getCell(j);
							if (((nextCell != null) && (nextCell != cell)) && ((nextCell.getCellType()) == (CellType.FORMULA))) {
								CTCellFormula nextF = nextCell.getCTCell().getF();
								CellRangeAddress nextRef = new CellRangeAddress(nextCell.getRowIndex(), ref.getLastRow(), nextCell.getColumnIndex(), ref.getLastColumn());
								nextF.setRef(nextRef.formatAsString());
								sharedFormulas.put(((int) (nextF.getSi())), nextF);
								break DONE;
							}
						}

				}
			}
		}
	}

	protected CTOleObject readOleObject(long shapeId) {
		if (!(getCTWorksheet().isSetOleObjects())) {
			return null;
		}
		String xquery = ("declare namespace p='" + (XSSFRelation.NS_SPREADSHEETML)) + "' .//p:oleObject";
		XmlCursor cur = getCTWorksheet().getOleObjects().newCursor();
		try {
			cur.selectPath(xquery);
			CTOleObject coo = null;
			while (cur.toNextSelection()) {
				String sId = cur.getAttributeText(new QName(null, "shapeId"));
				if ((sId == null) || ((Long.parseLong(sId)) != shapeId)) {
					continue;
				}
				XmlObject xObj = cur.getObject();
				if (xObj instanceof CTOleObject) {
					coo = ((CTOleObject) (xObj));
				}else {
					XMLStreamReader reader = cur.newXMLStreamReader();
					try {
						CTOleObjects coos = parse(reader);
						if ((coos.sizeOfOleObjectArray()) == 0) {
							continue;
						}
						coo = coos.getOleObjectArray(0);
					} catch (XmlException e) {
						XSSFSheet.logger.log(POILogger.INFO, "can't parse CTOleObjects", e);
					} finally {
						try {
							reader.close();
						} catch (XMLStreamException e) {
							XSSFSheet.logger.log(POILogger.INFO, "can't close reader", e);
						}
					}
				}
				if (cur.toChild(XSSFRelation.NS_SPREADSHEETML, "objectPr")) {
					break;
				}
			} 
			return coo == null ? null : coo;
		} finally {
			cur.dispose();
		}
	}

	public XSSFHeaderFooterProperties getHeaderFooterProperties() {
		return new XSSFHeaderFooterProperties(getSheetTypeHeaderFooter());
	}
}

