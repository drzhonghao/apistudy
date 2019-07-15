

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Table;
import org.apache.poi.ss.usermodel.TableStyleInfo;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTableColumn;
import org.apache.poi.xssf.usermodel.XSSFTableStyleInfo;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.helpers.XSSFXmlColumnPr;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTAutoFilter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRow;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyleInfo;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.TableDocument;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.TableDocument.Factory.newInstance;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.TableDocument.Factory.parse;


public class XSSFTable extends POIXMLDocumentPart implements Table {
	private CTTable ctTable;

	private transient List<XSSFXmlColumnPr> xmlColumnPrs;

	private transient List<XSSFTableColumn> tableColumns;

	private transient HashMap<String, Integer> columnMap;

	private transient CellReference startCellReference;

	private transient CellReference endCellReference;

	private transient String commonXPath;

	private transient String name;

	private transient String styleName;

	public XSSFTable() {
		super();
		ctTable = CTTable.Factory.newInstance();
	}

	public XSSFTable(PackagePart part) throws IOException {
		super(part);
		readFrom(part.getInputStream());
	}

	public void readFrom(InputStream is) throws IOException {
		try {
			TableDocument doc = parse(is, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
			ctTable = doc.getTable();
		} catch (XmlException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}

	public XSSFSheet getXSSFSheet() {
		return ((XSSFSheet) (getParent()));
	}

	public void writeTo(OutputStream out) throws IOException {
		updateHeaders();
		TableDocument doc = newInstance();
		doc.setTable(ctTable);
		doc.save(out, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
	}

	@Override
	protected void commit() throws IOException {
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		writeTo(out);
		out.close();
	}

	@org.apache.poi.util.Internal(since = "POI 3.15 beta 3")
	public CTTable getCTTable() {
		return ctTable;
	}

	public boolean mapsTo(long id) {
		List<XSSFXmlColumnPr> pointers = getXmlColumnPrs();
		for (XSSFXmlColumnPr pointer : pointers) {
			if ((pointer.getMapId()) == id) {
				return true;
			}
		}
		return false;
	}

	public String getCommonXpath() {
		if ((commonXPath) == null) {
			String[] commonTokens = new String[]{  };
			for (XSSFTableColumn column : getColumns()) {
				if ((column.getXmlColumnPr()) != null) {
					String xpath = column.getXmlColumnPr().getXPath();
					String[] tokens = xpath.split("/");
					if ((commonTokens.length) == 0) {
						commonTokens = tokens;
					}else {
						final int maxLength = Math.min(commonTokens.length, tokens.length);
						for (int i = 0; i < maxLength; i++) {
							if (!(commonTokens[i].equals(tokens[i]))) {
								List<String> subCommonTokens = Arrays.asList(commonTokens).subList(0, i);
								String[] container = new String[]{  };
								commonTokens = subCommonTokens.toArray(container);
								break;
							}
						}
					}
				}
			}
			commonTokens[0] = "";
			commonXPath = StringUtil.join(commonTokens, "/");
		}
		return commonXPath;
	}

	public List<XSSFTableColumn> getColumns() {
		if ((tableColumns) == null) {
			List<XSSFTableColumn> columns = new ArrayList<>();
			CTTableColumns ctTableColumns = ctTable.getTableColumns();
			if (ctTableColumns != null) {
				for (CTTableColumn column : ctTableColumns.getTableColumnList()) {
				}
			}
			tableColumns = Collections.unmodifiableList(columns);
		}
		return tableColumns;
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2.0")
	public List<XSSFXmlColumnPr> getXmlColumnPrs() {
		if ((xmlColumnPrs) == null) {
			xmlColumnPrs = new ArrayList<>();
			for (XSSFTableColumn column : getColumns()) {
				XSSFXmlColumnPr xmlColumnPr = column.getXmlColumnPr();
				if (xmlColumnPr != null) {
					xmlColumnPrs.add(xmlColumnPr);
				}
			}
		}
		return xmlColumnPrs;
	}

	public XSSFTableColumn createColumn(String columnName) {
		return createColumn(columnName, getColumnCount());
	}

	public XSSFTableColumn createColumn(String columnName, int columnIndex) {
		int columnCount = getColumnCount();
		if ((columnIndex < 0) || (columnIndex > columnCount)) {
			throw new IllegalArgumentException("Column index out of bounds");
		}
		CTTableColumns columns = ctTable.getTableColumns();
		if (columns == null) {
			columns = ctTable.addNewTableColumns();
		}
		long nextColumnId = 0;
		for (XSSFTableColumn tableColumn : getColumns()) {
			if ((columnName != null) && (columnName.equalsIgnoreCase(tableColumn.getName()))) {
				throw new IllegalArgumentException((("Column '" + columnName) + "' already exists. Column names must be unique per table."));
			}
			nextColumnId = Math.max(nextColumnId, tableColumn.getId());
		}
		nextColumnId++;
		CTTableColumn column = columns.insertNewTableColumn(columnIndex);
		columns.setCount(columns.sizeOfTableColumnArray());
		column.setId(nextColumnId);
		if (columnName != null) {
			column.setName(columnName);
		}else {
			column.setName(("Column " + nextColumnId));
		}
		if ((ctTable.getRef()) != null) {
			int newColumnCount = columnCount + 1;
			CellReference tableStart = getStartCellReference();
			CellReference tableEnd = getEndCellReference();
			SpreadsheetVersion version = getXSSFSheet().getWorkbook().getSpreadsheetVersion();
			CellReference newTableEnd = new CellReference(tableEnd.getRow(), (((tableStart.getCol()) + newColumnCount) - 1));
			AreaReference newTableArea = new AreaReference(tableStart, newTableEnd, version);
			setCellRef(newTableArea);
		}
		updateHeaders();
		return getColumns().get(columnIndex);
	}

	public void removeColumn(XSSFTableColumn column) {
		int columnIndex = getColumns().indexOf(column);
		if (columnIndex >= 0) {
			ctTable.getTableColumns().removeTableColumn(columnIndex);
			updateReferences();
			updateHeaders();
		}
	}

	public void removeColumn(int columnIndex) {
		if ((columnIndex < 0) || (columnIndex > ((getColumnCount()) - 1))) {
			throw new IllegalArgumentException("Column index out of bounds");
		}
		if ((getColumnCount()) == 1) {
			throw new IllegalArgumentException("Table must have at least one column");
		}
		CTTableColumns tableColumns = ctTable.getTableColumns();
		tableColumns.removeTableColumn(columnIndex);
		tableColumns.setCount(tableColumns.getTableColumnList().size());
		updateReferences();
		updateHeaders();
	}

	public String getName() {
		if ((name) == null) {
			setName(ctTable.getName());
		}
		return name;
	}

	public void setName(String newName) {
		if (newName == null) {
			ctTable.unsetName();
			name = null;
			return;
		}
		ctTable.setName(newName);
		name = newName;
	}

	public String getStyleName() {
		if (((styleName) == null) && (ctTable.isSetTableStyleInfo())) {
			setStyleName(ctTable.getTableStyleInfo().getName());
		}
		return styleName;
	}

	public void setStyleName(String newStyleName) {
		if (newStyleName == null) {
			if (ctTable.isSetTableStyleInfo()) {
				ctTable.getTableStyleInfo().unsetName();
			}
			styleName = null;
			return;
		}
		if (!(ctTable.isSetTableStyleInfo())) {
			ctTable.addNewTableStyleInfo();
		}
		ctTable.getTableStyleInfo().setName(newStyleName);
		styleName = newStyleName;
	}

	public String getDisplayName() {
		return ctTable.getDisplayName();
	}

	public void setDisplayName(String name) {
		ctTable.setDisplayName(name);
	}

	@Deprecated
	@org.apache.poi.util.Removal(version = "4.2.0")
	public long getNumberOfMappedColumns() {
		return ctTable.getTableColumns().getCount();
	}

	public AreaReference getCellReferences() {
		return new AreaReference(getStartCellReference(), getEndCellReference(), SpreadsheetVersion.EXCEL2007);
	}

	public void setCellReferences(AreaReference refs) {
		setCellRef(refs);
	}

	@org.apache.poi.util.Internal
	protected void setCellRef(AreaReference refs) {
		String ref = refs.formatAsString();
		if ((ref.indexOf('!')) != (-1)) {
			ref = ref.substring(((ref.indexOf('!')) + 1));
		}
		ctTable.setRef(ref);
		if (ctTable.isSetAutoFilter()) {
			String filterRef;
			int totalsRowCount = getTotalsRowCount();
			if (totalsRowCount == 0) {
				filterRef = ref;
			}else {
				final CellReference start = new CellReference(refs.getFirstCell().getRow(), refs.getFirstCell().getCol());
				final CellReference end = new CellReference(((refs.getLastCell().getRow()) - totalsRowCount), refs.getLastCell().getCol());
				filterRef = new AreaReference(start, end, SpreadsheetVersion.EXCEL2007).formatAsString();
			}
			ctTable.getAutoFilter().setRef(filterRef);
		}
		updateReferences();
		updateHeaders();
	}

	public void setArea(AreaReference tableArea) {
		if (tableArea == null) {
			throw new IllegalArgumentException("AreaReference must not be null");
		}
		String areaSheetName = tableArea.getFirstCell().getSheetName();
		if ((areaSheetName != null) && (!(areaSheetName.equals(getXSSFSheet().getSheetName())))) {
			throw new IllegalArgumentException("The AreaReference must not reference a different sheet");
		}
		int rowCount = ((tableArea.getLastCell().getRow()) - (tableArea.getFirstCell().getRow())) + 1;
		int minimumRowCount = (1 + (getHeaderRowCount())) + (getTotalsRowCount());
		if (rowCount < minimumRowCount) {
			throw new IllegalArgumentException((("AreaReference needs at least " + minimumRowCount) + " rows, to cover at least one data row and all header rows and totals rows"));
		}
		String ref = tableArea.formatAsString();
		if ((ref.indexOf('!')) != (-1)) {
			ref = ref.substring(((ref.indexOf('!')) + 1));
		}
		ctTable.setRef(ref);
		if (ctTable.isSetAutoFilter()) {
			ctTable.getAutoFilter().setRef(ref);
		}
		updateReferences();
		int columnCount = getColumnCount();
		int newColumnCount = ((tableArea.getLastCell().getCol()) - (tableArea.getFirstCell().getCol())) + 1;
		if (newColumnCount > columnCount) {
			for (int i = columnCount; i < newColumnCount; i++) {
				createColumn(null, i);
			}
		}else
			if (newColumnCount < columnCount) {
				for (int i = columnCount; i > newColumnCount; i--) {
					removeColumn((i - 1));
				}
			}

		updateHeaders();
	}

	public AreaReference getArea() {
		String ref = ctTable.getRef();
		if (ref != null) {
			SpreadsheetVersion version = getXSSFSheet().getWorkbook().getSpreadsheetVersion();
			return new AreaReference(ctTable.getRef(), version);
		}else {
			return null;
		}
	}

	public CellReference getStartCellReference() {
		if ((startCellReference) == null) {
			setCellReferences();
		}
		return startCellReference;
	}

	public CellReference getEndCellReference() {
		if ((endCellReference) == null) {
			setCellReferences();
		}
		return endCellReference;
	}

	private void setCellReferences() {
		String ref = ctTable.getRef();
		if (ref != null) {
			String[] boundaries = ref.split(":", 2);
			String from = boundaries[0];
			String to = boundaries[1];
			startCellReference = new CellReference(from);
			endCellReference = new CellReference(to);
		}
	}

	public void updateReferences() {
		startCellReference = null;
		endCellReference = null;
	}

	public int getRowCount() {
		CellReference from = getStartCellReference();
		CellReference to = getEndCellReference();
		int rowCount = 0;
		if ((from != null) && (to != null)) {
			rowCount = ((to.getRow()) - (from.getRow())) + 1;
		}
		return rowCount;
	}

	public int getDataRowCount() {
		CellReference from = getStartCellReference();
		CellReference to = getEndCellReference();
		int rowCount = 0;
		if ((from != null) && (to != null)) {
			rowCount = ((((to.getRow()) - (from.getRow())) + 1) - (getHeaderRowCount())) - (getTotalsRowCount());
		}
		return rowCount;
	}

	public void setDataRowCount(int newDataRowCount) {
		if (newDataRowCount < 1) {
			throw new IllegalArgumentException("Table must have at least one data row");
		}
		updateReferences();
		int dataRowCount = getDataRowCount();
		if (dataRowCount == newDataRowCount) {
			return;
		}
		CellReference tableStart = getStartCellReference();
		CellReference tableEnd = getEndCellReference();
		SpreadsheetVersion version = getXSSFSheet().getWorkbook().getSpreadsheetVersion();
		int newTotalRowCount = ((getHeaderRowCount()) + newDataRowCount) + (getTotalsRowCount());
		CellReference newTableEnd = new CellReference((((tableStart.getRow()) + newTotalRowCount) - 1), tableEnd.getCol());
		AreaReference newTableArea = new AreaReference(tableStart, newTableEnd, version);
		CellReference clearAreaStart;
		CellReference clearAreaEnd;
		if (newDataRowCount < dataRowCount) {
			clearAreaStart = new CellReference(((newTableArea.getLastCell().getRow()) + 1), newTableArea.getFirstCell().getCol());
			clearAreaEnd = tableEnd;
		}else {
			clearAreaStart = new CellReference(((tableEnd.getRow()) + 1), newTableArea.getFirstCell().getCol());
			clearAreaEnd = newTableEnd;
		}
		AreaReference areaToClear = new AreaReference(clearAreaStart, clearAreaEnd, version);
		for (CellReference cellRef : areaToClear.getAllReferencedCells()) {
			XSSFRow row = getXSSFSheet().getRow(cellRef.getRow());
			if (row != null) {
				XSSFCell cell = row.getCell(cellRef.getCol());
				if (cell != null) {
					cell.setCellType(CellType.BLANK);
					cell.setCellStyle(null);
				}
			}
		}
		setCellRef(newTableArea);
	}

	public int getColumnCount() {
		CTTableColumns tableColumns = ctTable.getTableColumns();
		if (tableColumns == null) {
			return 0;
		}
		return ((int) (tableColumns.getCount()));
	}

	public void updateHeaders() {
		XSSFSheet sheet = ((XSSFSheet) (getParent()));
		CellReference ref = getStartCellReference();
		if (ref == null)
			return;

		int headerRow = ref.getRow();
		int firstHeaderColumn = ref.getCol();
		XSSFRow row = sheet.getRow(headerRow);
		DataFormatter formatter = new DataFormatter();
		if ((row != null) && (row.getCTRow().validate())) {
			int cellnum = firstHeaderColumn;
			CTTableColumns ctTableColumns = getCTTable().getTableColumns();
			if (ctTableColumns != null) {
				for (CTTableColumn col : ctTableColumns.getTableColumnList()) {
					XSSFCell cell = row.getCell(cellnum);
					if (cell != null) {
						col.setName(formatter.formatCellValue(cell));
					}
					cellnum++;
				}
			}
		}
		tableColumns = null;
		columnMap = null;
		xmlColumnPrs = null;
		commonXPath = null;
	}

	private static String caseInsensitive(String s) {
		return s.toUpperCase(Locale.ROOT);
	}

	public int findColumnIndex(String columnHeader) {
		if (columnHeader == null)
			return -1;

		if ((columnMap) == null) {
			final int count = getColumnCount();
			columnMap = new HashMap<>(((count * 3) / 2));
			int i = 0;
			for (XSSFTableColumn column : getColumns()) {
				String columnName = column.getName();
				columnMap.put(XSSFTable.caseInsensitive(columnName), i);
				i++;
			}
		}
		Integer idx = columnMap.get(XSSFTable.caseInsensitive(columnHeader.replace("'", "")));
		return idx == null ? -1 : idx.intValue();
	}

	public String getSheetName() {
		return getXSSFSheet().getSheetName();
	}

	public boolean isHasTotalsRow() {
		return ctTable.getTotalsRowShown();
	}

	public int getTotalsRowCount() {
		return ((int) (ctTable.getTotalsRowCount()));
	}

	public int getHeaderRowCount() {
		return ((int) (ctTable.getHeaderRowCount()));
	}

	public int getStartColIndex() {
		return getStartCellReference().getCol();
	}

	public int getStartRowIndex() {
		return getStartCellReference().getRow();
	}

	public int getEndColIndex() {
		return getEndCellReference().getCol();
	}

	public int getEndRowIndex() {
		return getEndCellReference().getRow();
	}

	public TableStyleInfo getStyle() {
		if (!(ctTable.isSetTableStyleInfo()))
			return null;

		return new XSSFTableStyleInfo(((XSSFSheet) (getParent())).getWorkbook().getStylesSource(), ctTable.getTableStyleInfo());
	}

	public boolean contains(CellReference cell) {
		if (cell == null)
			return false;

		if (!(getSheetName().equals(cell.getSheetName())))
			return false;

		if (((((cell.getRow()) >= (getStartRowIndex())) && ((cell.getRow()) <= (getEndRowIndex()))) && ((cell.getCol()) >= (getStartColIndex()))) && ((cell.getCol()) <= (getEndColIndex()))) {
			return true;
		}
		return false;
	}

	protected void onTableDelete() {
		for (POIXMLDocumentPart.RelationPart part : getRelationParts()) {
			removeRelation(part.getDocumentPart(), true);
		}
	}
}

