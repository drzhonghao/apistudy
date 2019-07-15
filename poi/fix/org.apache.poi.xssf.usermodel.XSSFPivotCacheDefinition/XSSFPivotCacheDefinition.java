

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Beta;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCacheField;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCacheFields;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCacheSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPivotCacheDefinition;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSharedItems;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheetSource;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPivotCacheDefinition.Factory.newInstance;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPivotCacheDefinition.Factory.parse;


public class XSSFPivotCacheDefinition extends POIXMLDocumentPart {
	private CTPivotCacheDefinition ctPivotCacheDefinition;

	@Beta
	public XSSFPivotCacheDefinition() {
		super();
		ctPivotCacheDefinition = newInstance();
		createDefaultValues();
	}

	@Beta
	protected XSSFPivotCacheDefinition(PackagePart part) throws IOException {
		super(part);
		readFrom(part.getInputStream());
	}

	@Beta
	public void readFrom(InputStream is) throws IOException {
		try {
			XmlOptions options = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
			options.setLoadReplaceDocumentElement(null);
			ctPivotCacheDefinition = parse(is, options);
		} catch (XmlException e) {
			throw new IOException(e.getLocalizedMessage(), e);
		}
	}

	@Beta
	@org.apache.poi.util.Internal
	public CTPivotCacheDefinition getCTPivotCacheDefinition() {
		return ctPivotCacheDefinition;
	}

	@Beta
	private void createDefaultValues() {
		ctPivotCacheDefinition.setRefreshedBy("Apache POI");
		ctPivotCacheDefinition.setRefreshedDate(new Date().getTime());
		ctPivotCacheDefinition.setRefreshOnLoad(true);
	}

	@Beta
	@Override
	protected void commit() throws IOException {
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		xmlOptions.setSaveSyntheticDocumentElement(new QName(CTPivotCacheDefinition.type.getName().getNamespaceURI(), "pivotCacheDefinition"));
		ctPivotCacheDefinition.save(out, xmlOptions);
		out.close();
	}

	@Beta
	public AreaReference getPivotArea(Workbook wb) throws IllegalArgumentException {
		final CTWorksheetSource wsSource = ctPivotCacheDefinition.getCacheSource().getWorksheetSource();
		final String ref = wsSource.getRef();
		final String name = wsSource.getName();
		if ((ref == null) && (name == null)) {
			throw new IllegalArgumentException("Pivot cache must reference an area, named range, or table.");
		}
		if (ref != null) {
			return new AreaReference(ref, SpreadsheetVersion.EXCEL2007);
		}
		assert name != null;
		final Name range = wb.getName(name);
		if (range != null) {
			return new AreaReference(range.getRefersToFormula(), SpreadsheetVersion.EXCEL2007);
		}
		final XSSFSheet sheet = ((XSSFSheet) (wb.getSheet(wsSource.getSheet())));
		for (XSSFTable table : sheet.getTables()) {
			if (name.equals(table.getName())) {
				return new AreaReference(table.getStartCellReference(), table.getEndCellReference(), SpreadsheetVersion.EXCEL2007);
			}
		}
		throw new IllegalArgumentException((("Name '" + name) + "' was not found."));
	}

	@Beta
	protected void createCacheFields(Sheet sheet) {
		AreaReference ar = getPivotArea(sheet.getWorkbook());
		CellReference firstCell = ar.getFirstCell();
		CellReference lastCell = ar.getLastCell();
		int columnStart = firstCell.getCol();
		int columnEnd = lastCell.getCol();
		Row row = sheet.getRow(firstCell.getRow());
		CTCacheFields cFields;
		if ((ctPivotCacheDefinition.getCacheFields()) != null) {
			cFields = ctPivotCacheDefinition.getCacheFields();
		}else {
			cFields = ctPivotCacheDefinition.addNewCacheFields();
		}
		for (int i = columnStart; i <= columnEnd; i++) {
			CTCacheField cf = cFields.addNewCacheField();
			if (i == columnEnd) {
				cFields.setCount(cFields.sizeOfCacheFieldArray());
			}
			cf.setNumFmtId(0);
			Cell cell = row.getCell(i);
			cell.setCellType(CellType.STRING);
			cf.setName(row.getCell(i).getStringCellValue());
			cf.addNewSharedItems();
		}
	}
}

