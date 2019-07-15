

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.poi.hpsf.ClassID;
import org.apache.poi.hpsf.ClassIDPredefined;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ooxml.POIXMLRelation;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.ooxml.util.PackageHelper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageProperties;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.poifs.crypt.HashAlgorithm;
import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.Ole10Native;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.SheetNameFormatter;
import org.apache.poi.ss.formula.udf.AggregatingUDFFinder;
import org.apache.poi.ss.formula.udf.IndexedUDFFinder;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Removal;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.XLSBUnsupportedException;
import org.apache.poi.xssf.model.CalculationChain;
import org.apache.poi.xssf.model.ExternalLinksTable;
import org.apache.poi.xssf.model.MapInfo;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.model.ThemesTable;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFDialogsheet;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFactory;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFMap;
import org.apache.poi.xssf.usermodel.XSSFName;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;
import org.apache.poi.xssf.usermodel.helpers.XSSFPasswordHelper;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTDrawing;
import org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.CTProperties;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookView;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookViews;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCalcChain;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCalcPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDefinedName;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDefinedNames;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDialogsheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTExternalReference;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTExternalReferences;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPivotCache;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPivotCaches;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheets;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbookPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbookProtection;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STCalcMode;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.WorkbookDocument;

import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_NULL_AND_BLANK;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDefinedNames.Factory.newInstance;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.WorkbookDocument.Factory.parse;


public class XSSFWorkbook extends POIXMLDocument implements Workbook {
	private static final Pattern COMMA_PATTERN = Pattern.compile(",");

	@Removal(version = "4.1")
	public static final float DEFAULT_CHARACTER_WIDTH = Units.DEFAULT_CHARACTER_WIDTH;

	private static final int MAX_SENSITIVE_SHEET_NAME_LEN = 31;

	public static final int PICTURE_TYPE_GIF = 8;

	public static final int PICTURE_TYPE_TIFF = 9;

	public static final int PICTURE_TYPE_EPS = 10;

	public static final int PICTURE_TYPE_BMP = 11;

	public static final int PICTURE_TYPE_WPG = 12;

	private CTWorkbook workbook;

	private List<XSSFSheet> sheets;

	private ListValuedMap<String, XSSFName> namedRangesByName;

	private List<XSSFName> namedRanges;

	private SharedStringsTable sharedStringSource;

	private StylesTable stylesSource;

	private IndexedUDFFinder _udfFinder = new IndexedUDFFinder(AggregatingUDFFinder.DEFAULT);

	private CalculationChain calcChain;

	private List<ExternalLinksTable> externalLinks;

	private MapInfo mapInfo;

	private XSSFDataFormat formatter;

	private Row.MissingCellPolicy _missingCellPolicy = RETURN_NULL_AND_BLANK;

	private boolean cellFormulaValidation = true;

	private List<XSSFPictureData> pictures;

	private static POILogger logger = POILogFactory.getLogger(XSSFWorkbook.class);

	private XSSFCreationHelper _creationHelper;

	private List<XSSFPivotTable> pivotTables;

	private List<CTPivotCache> pivotCaches;

	private final XSSFFactory xssfFactory;

	public XSSFWorkbook() {
		this(XSSFWorkbookType.XLSX);
	}

	public XSSFWorkbook(XSSFFactory factory) {
		this(XSSFWorkbookType.XLSX, factory);
	}

	public XSSFWorkbook(XSSFWorkbookType workbookType) {
		this(workbookType, null);
	}

	private XSSFWorkbook(XSSFWorkbookType workbookType, XSSFFactory factory) {
		super(XSSFWorkbook.newPackage(workbookType));
		this.xssfFactory = (factory == null) ? XSSFFactory.getInstance() : factory;
		onWorkbookCreate();
	}

	public XSSFWorkbook(OPCPackage pkg) throws IOException {
		super(pkg);
		this.xssfFactory = XSSFFactory.getInstance();
		beforeDocumentRead();
		load(this.xssfFactory);
		setBookViewsIfMissing();
	}

	public XSSFWorkbook(InputStream is) throws IOException {
		this(PackageHelper.open(is));
	}

	public XSSFWorkbook(File file) throws IOException, InvalidFormatException {
		this(OPCPackage.open(file));
	}

	public XSSFWorkbook(String path) throws IOException {
		this(POIXMLDocument.openPackage(path));
	}

	public XSSFWorkbook(PackagePart part) throws IOException {
		this(part.getInputStream());
	}

	protected void beforeDocumentRead() {
		if (getCorePart().getContentType().equals(XSSFRelation.XLSB_BINARY_WORKBOOK.getContentType())) {
			throw new XLSBUnsupportedException();
		}
		pivotTables = new ArrayList<>();
		pivotCaches = new ArrayList<>();
	}

	@Override
	protected void onDocumentRead() throws IOException {
		try {
			WorkbookDocument doc = parse(getPackagePart().getInputStream(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
			this.workbook = doc.getWorkbook();
			ThemesTable theme = null;
			Map<String, XSSFSheet> shIdMap = new HashMap<>();
			Map<String, ExternalLinksTable> elIdMap = new HashMap<>();
			for (POIXMLDocumentPart.RelationPart rp : getRelationParts()) {
				POIXMLDocumentPart p = rp.getDocumentPart();
				if (p instanceof SharedStringsTable) {
					sharedStringSource = ((SharedStringsTable) (p));
				}else
					if (p instanceof StylesTable) {
						stylesSource = ((StylesTable) (p));
					}else
						if (p instanceof ThemesTable) {
							theme = ((ThemesTable) (p));
						}else
							if (p instanceof CalculationChain) {
								calcChain = ((CalculationChain) (p));
							}else
								if (p instanceof MapInfo) {
									mapInfo = ((MapInfo) (p));
								}else
									if (p instanceof XSSFSheet) {
										shIdMap.put(rp.getRelationship().getId(), ((XSSFSheet) (p)));
									}else
										if (p instanceof ExternalLinksTable) {
											elIdMap.put(rp.getRelationship().getId(), ((ExternalLinksTable) (p)));
										}






			}
			boolean packageReadOnly = (getPackage().getPackageAccess()) == (PackageAccess.READ);
			if ((stylesSource) == null) {
				if (packageReadOnly) {
					stylesSource = new StylesTable();
				}else {
					stylesSource = ((StylesTable) (createRelationship(XSSFRelation.STYLES, this.xssfFactory)));
				}
			}
			stylesSource.setTheme(theme);
			if ((sharedStringSource) == null) {
				if (packageReadOnly) {
					sharedStringSource = new SharedStringsTable();
				}else {
					sharedStringSource = ((SharedStringsTable) (createRelationship(XSSFRelation.SHARED_STRINGS, this.xssfFactory)));
				}
			}
			sheets = new ArrayList<>(shIdMap.size());
			for (CTSheet ctSheet : this.workbook.getSheets().getSheetArray()) {
				parseSheet(shIdMap, ctSheet);
			}
			externalLinks = new ArrayList<>(elIdMap.size());
			if (this.workbook.isSetExternalReferences()) {
				for (CTExternalReference er : this.workbook.getExternalReferences().getExternalReferenceArray()) {
					ExternalLinksTable el = elIdMap.get(er.getId());
					if (el == null) {
						XSSFWorkbook.logger.log(POILogger.WARN, (("ExternalLinksTable with r:id " + (er.getId())) + " was defined, but didn't exist in package, skipping"));
						continue;
					}
					externalLinks.add(el);
				}
			}
			reprocessNamedRanges();
		} catch (XmlException e) {
			throw new POIXMLException(e);
		}
	}

	public void parseSheet(Map<String, XSSFSheet> shIdMap, CTSheet ctSheet) {
		XSSFSheet sh = shIdMap.get(ctSheet.getId());
		if (sh == null) {
			XSSFWorkbook.logger.log(POILogger.WARN, (((("Sheet with name " + (ctSheet.getName())) + " and r:id ") + (ctSheet.getId())) + " was defined, but didn't exist in package, skipping"));
			return;
		}
		sheets.add(sh);
	}

	private void onWorkbookCreate() {
		workbook = CTWorkbook.Factory.newInstance();
		CTWorkbookPr workbookPr = workbook.addNewWorkbookPr();
		workbookPr.setDate1904(false);
		setBookViewsIfMissing();
		workbook.addNewSheets();
		POIXMLProperties.ExtendedProperties expProps = getProperties().getExtendedProperties();
		expProps.getUnderlyingProperties().setApplication(POIXMLDocument.DOCUMENT_CREATOR);
		sharedStringSource = ((SharedStringsTable) (createRelationship(XSSFRelation.SHARED_STRINGS, this.xssfFactory)));
		stylesSource = ((StylesTable) (createRelationship(XSSFRelation.STYLES, this.xssfFactory)));
		namedRanges = new ArrayList<>();
		namedRangesByName = new ArrayListValuedHashMap<>();
		sheets = new ArrayList<>();
		pivotTables = new ArrayList<>();
	}

	private void setBookViewsIfMissing() {
		if (!(workbook.isSetBookViews())) {
			CTBookViews bvs = workbook.addNewBookViews();
			CTBookView bv = bvs.addNewWorkbookView();
			bv.setActiveTab(0);
		}
	}

	protected static OPCPackage newPackage(XSSFWorkbookType workbookType) {
		try {
			OPCPackage pkg = OPCPackage.create(new ByteArrayOutputStream());
			PackagePartName corePartName = PackagingURIHelper.createPartName(XSSFRelation.WORKBOOK.getDefaultFileName());
			pkg.addRelationship(corePartName, TargetMode.INTERNAL, PackageRelationshipTypes.CORE_DOCUMENT);
			pkg.createPart(corePartName, workbookType.getContentType());
			pkg.getPackageProperties().setCreatorProperty(POIXMLDocument.DOCUMENT_CREATOR);
			return pkg;
		} catch (Exception e) {
			throw new POIXMLException(e);
		}
	}

	@org.apache.poi.util.Internal
	public CTWorkbook getCTWorkbook() {
		return this.workbook;
	}

	@Override
	public int addPicture(byte[] pictureData, int format) {
		int imageNumber = (getAllPictures().size()) + 1;
		return imageNumber - 1;
	}

	public int addPicture(InputStream is, int format) throws IOException {
		int imageNumber = (getAllPictures().size()) + 1;
		return imageNumber - 1;
	}

	@Override
	public XSSFSheet cloneSheet(int sheetNum) {
		return cloneSheet(sheetNum, null);
	}

	@Override
	public void close() throws IOException {
		super.close();
		sharedStringSource.close();
	}

	public XSSFSheet cloneSheet(int sheetNum, String newName) {
		validateSheetIndex(sheetNum);
		XSSFSheet srcSheet = sheets.get(sheetNum);
		if (newName == null) {
			String srcName = srcSheet.getSheetName();
			newName = getUniqueSheetName(srcName);
		}else {
			validateSheetName(newName);
		}
		XSSFSheet clonedSheet = createSheet(newName);
		List<POIXMLDocumentPart.RelationPart> rels = srcSheet.getRelationParts();
		XSSFDrawing dg = null;
		for (POIXMLDocumentPart.RelationPart rp : rels) {
			POIXMLDocumentPart r = rp.getDocumentPart();
			if (r instanceof XSSFDrawing) {
				dg = ((XSSFDrawing) (r));
				continue;
			}
			XSSFWorkbook.addRelation(rp, clonedSheet);
		}
		try {
			for (PackageRelationship pr : srcSheet.getPackagePart().getRelationships()) {
				if ((pr.getTargetMode()) == (TargetMode.EXTERNAL)) {
					clonedSheet.getPackagePart().addExternalRelationship(pr.getTargetURI().toASCIIString(), pr.getRelationshipType(), pr.getId());
				}
			}
		} catch (InvalidFormatException e) {
			throw new POIXMLException("Failed to clone sheet", e);
		}
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			try (final ByteArrayInputStream bis = new ByteArrayInputStream(out.toByteArray())) {
			}
		} catch (IOException e) {
			throw new POIXMLException("Failed to clone sheet", e);
		}
		CTWorksheet ct = clonedSheet.getCTWorksheet();
		if (ct.isSetLegacyDrawing()) {
			XSSFWorkbook.logger.log(POILogger.WARN, "Cloning sheets with comments is not yet supported.");
			ct.unsetLegacyDrawing();
		}
		if (ct.isSetPageSetup()) {
			XSSFWorkbook.logger.log(POILogger.WARN, "Cloning sheets with page setup is not yet supported.");
			ct.unsetPageSetup();
		}
		clonedSheet.setSelected(false);
		if (dg != null) {
			if (ct.isSetDrawing()) {
				ct.unsetDrawing();
			}
			XSSFDrawing clonedDg = clonedSheet.createDrawingPatriarch();
			clonedDg.getCTDrawing().set(dg.getCTDrawing());
			clonedDg = clonedSheet.createDrawingPatriarch();
			List<POIXMLDocumentPart.RelationPart> srcRels = srcSheet.createDrawingPatriarch().getRelationParts();
			for (POIXMLDocumentPart.RelationPart rp : srcRels) {
				XSSFWorkbook.addRelation(rp, clonedDg);
			}
		}
		return clonedSheet;
	}

	private static void addRelation(POIXMLDocumentPart.RelationPart rp, POIXMLDocumentPart target) {
		PackageRelationship rel = rp.getRelationship();
		if ((rel.getTargetMode()) == (TargetMode.EXTERNAL)) {
			target.getPackagePart().addRelationship(rel.getTargetURI(), rel.getTargetMode(), rel.getRelationshipType(), rel.getId());
		}else {
			XSSFRelation xssfRel = XSSFRelation.getInstance(rel.getRelationshipType());
			if (xssfRel == null) {
				throw new POIXMLException(("Can't clone sheet - unknown relation type found: " + (rel.getRelationshipType())));
			}
			target.addRelation(rel.getId(), xssfRel, rp.getDocumentPart());
		}
	}

	private String getUniqueSheetName(String srcName) {
		int uniqueIndex = 2;
		String baseName = srcName;
		int bracketPos = srcName.lastIndexOf('(');
		if ((bracketPos > 0) && (srcName.endsWith(")"))) {
			String suffix = srcName.substring((bracketPos + 1), ((srcName.length()) - (")".length())));
			try {
				uniqueIndex = Integer.parseInt(suffix.trim());
				uniqueIndex++;
				baseName = srcName.substring(0, bracketPos).trim();
			} catch (NumberFormatException e) {
			}
		}
		while (true) {
			String index = Integer.toString((uniqueIndex++));
			String name;
			if ((((baseName.length()) + (index.length())) + 2) < 31) {
				name = ((baseName + " (") + index) + ")";
			}else {
				name = (((baseName.substring(0, ((31 - (index.length())) - 2))) + "(") + index) + ")";
			}
			if ((getSheetIndex(name)) == (-1)) {
				return name;
			}
		} 
	}

	@Override
	public XSSFCellStyle createCellStyle() {
		return stylesSource.createCellStyle();
	}

	@Override
	public XSSFDataFormat createDataFormat() {
		if ((formatter) == null) {
		}
		return formatter;
	}

	@Override
	public XSSFFont createFont() {
		XSSFFont font = new XSSFFont();
		font.registerTo(stylesSource);
		return font;
	}

	@Override
	public XSSFName createName() {
		CTDefinedName ctName = CTDefinedName.Factory.newInstance();
		ctName.setName("");
		return createAndStoreName(ctName);
	}

	private XSSFName createAndStoreName(CTDefinedName ctName) {
		return null;
	}

	@Override
	public XSSFSheet createSheet() {
		String sheetname = "Sheet" + (sheets.size());
		int idx = 0;
		while ((getSheet(sheetname)) != null) {
			sheetname = "Sheet" + idx;
			idx++;
		} 
		return createSheet(sheetname);
	}

	@Override
	public XSSFSheet createSheet(String sheetname) {
		if (sheetname == null) {
			throw new IllegalArgumentException("sheetName must not be null");
		}
		validateSheetName(sheetname);
		if ((sheetname.length()) > 31) {
			sheetname = sheetname.substring(0, 31);
		}
		WorkbookUtil.validateSheetName(sheetname);
		CTSheet sheet = addSheet(sheetname);
		int sheetNumber = 1;
		outerloop : while (true) {
			for (XSSFSheet sh : sheets) {
			}
			String sheetName = XSSFRelation.WORKSHEET.getFileName(sheetNumber);
			for (POIXMLDocumentPart relation : getRelations()) {
				if (((relation.getPackagePart()) != null) && (sheetName.equals(relation.getPackagePart().getPartName().getName()))) {
					sheetNumber++;
					continue outerloop;
				}
			}
			break;
		} 
		POIXMLDocumentPart.RelationPart rp = createRelationship(XSSFRelation.WORKSHEET, this.xssfFactory, sheetNumber, false);
		XSSFSheet wrapper = rp.getDocumentPart();
		sheet.setId(rp.getRelationship().getId());
		sheet.setSheetId(sheetNumber);
		if (sheets.isEmpty()) {
			wrapper.setSelected(true);
		}
		sheets.add(wrapper);
		return wrapper;
	}

	private void validateSheetName(final String sheetName) throws IllegalArgumentException {
		if (containsSheet(sheetName, sheets.size())) {
			throw new IllegalArgumentException((("The workbook already contains a sheet named '" + sheetName) + "'"));
		}
	}

	protected XSSFDialogsheet createDialogsheet(String sheetname, CTDialogsheet dialogsheet) {
		XSSFSheet sheet = createSheet(sheetname);
		return null;
	}

	private CTSheet addSheet(String sheetname) {
		CTSheet sheet = workbook.getSheets().addNewSheet();
		sheet.setName(sheetname);
		return sheet;
	}

	@Override
	public XSSFFont findFont(boolean bold, short color, short fontHeight, String name, boolean italic, boolean strikeout, short typeOffset, byte underline) {
		return stylesSource.findFont(bold, color, fontHeight, name, italic, strikeout, typeOffset, underline);
	}

	@Override
	public int getActiveSheetIndex() {
		return ((int) (workbook.getBookViews().getWorkbookViewArray(0).getActiveTab()));
	}

	@Override
	public List<XSSFPictureData> getAllPictures() {
		if ((pictures) == null) {
			List<PackagePart> mediaParts = getPackage().getPartsByName(Pattern.compile("/xl/media/.*?"));
			pictures = new ArrayList<>(mediaParts.size());
			for (PackagePart part : mediaParts) {
			}
		}
		return pictures;
	}

	@Override
	public XSSFCellStyle getCellStyleAt(int idx) {
		return stylesSource.getStyleAt(idx);
	}

	@Override
	public XSSFFont getFontAt(short idx) {
		return stylesSource.getFontAt(idx);
	}

	@Override
	public XSSFFont getFontAt(int idx) {
		return stylesSource.getFontAt(idx);
	}

	@Override
	public XSSFName getName(String name) {
		Collection<XSSFName> list = getNames(name);
		if (list.isEmpty()) {
			return null;
		}
		return list.iterator().next();
	}

	@Override
	public List<XSSFName> getNames(String name) {
		return Collections.unmodifiableList(namedRangesByName.get(name.toLowerCase(Locale.ENGLISH)));
	}

	@Override
	@Deprecated
	public XSSFName getNameAt(int nameIndex) {
		int nNames = namedRanges.size();
		if (nNames < 1) {
			throw new IllegalStateException("There are no defined names in this workbook");
		}
		if ((nameIndex < 0) || (nameIndex > nNames)) {
			throw new IllegalArgumentException((((("Specified name index " + nameIndex) + " is outside the allowable range (0..") + (nNames - 1)) + ")."));
		}
		return namedRanges.get(nameIndex);
	}

	@Override
	public List<XSSFName> getAllNames() {
		return Collections.unmodifiableList(namedRanges);
	}

	@Override
	@Deprecated
	public int getNameIndex(String name) {
		XSSFName nm = getName(name);
		if (nm != null) {
			return namedRanges.indexOf(nm);
		}
		return -1;
	}

	@Override
	public int getNumCellStyles() {
		return stylesSource.getNumCellStyles();
	}

	@Override
	public short getNumberOfFonts() {
		return ((short) (getNumberOfFontsAsInt()));
	}

	@Override
	public int getNumberOfFontsAsInt() {
		return ((short) (stylesSource.getFonts().size()));
	}

	@Override
	public int getNumberOfNames() {
		return namedRanges.size();
	}

	@Override
	public int getNumberOfSheets() {
		return sheets.size();
	}

	@Override
	public String getPrintArea(int sheetIndex) {
		XSSFName name = getBuiltInName(XSSFName.BUILTIN_PRINT_AREA, sheetIndex);
		if (name == null) {
			return null;
		}
		return name.getRefersToFormula();
	}

	@Override
	public XSSFSheet getSheet(String name) {
		for (XSSFSheet sheet : sheets) {
			if (name.equalsIgnoreCase(sheet.getSheetName())) {
				return sheet;
			}
		}
		return null;
	}

	@Override
	public XSSFSheet getSheetAt(int index) {
		validateSheetIndex(index);
		return sheets.get(index);
	}

	@Override
	public int getSheetIndex(String name) {
		int idx = 0;
		for (XSSFSheet sh : sheets) {
			if (name.equalsIgnoreCase(sh.getSheetName())) {
				return idx;
			}
			idx++;
		}
		return -1;
	}

	@Override
	public int getSheetIndex(Sheet sheet) {
		int idx = 0;
		for (XSSFSheet sh : sheets) {
			if (sh == sheet) {
				return idx;
			}
			idx++;
		}
		return -1;
	}

	@Override
	public String getSheetName(int sheetIx) {
		validateSheetIndex(sheetIx);
		return sheets.get(sheetIx).getSheetName();
	}

	@Override
	public Iterator<Sheet> sheetIterator() {
		return new XSSFWorkbook.SheetIterator<>();
	}

	@Override
	public Iterator<Sheet> iterator() {
		return sheetIterator();
	}

	private final class SheetIterator<T extends Sheet> implements Iterator<T> {
		private final Iterator<T> it;

		@SuppressWarnings("unchecked")
		public SheetIterator() {
			it = ((Iterator<T>) (sheets.iterator()));
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public T next() throws NoSuchElementException {
			return it.next();
		}

		@Override
		public void remove() throws IllegalStateException {
			throw new UnsupportedOperationException(("remove method not supported on XSSFWorkbook.iterator(). " + "Use Sheet.removeSheetAt(int) instead."));
		}
	}

	public boolean isMacroEnabled() {
		return getPackagePart().getContentType().equals(XSSFRelation.MACROS_WORKBOOK.getContentType());
	}

	@Override
	@Deprecated
	public void removeName(int nameIndex) {
		removeName(getNameAt(nameIndex));
	}

	@Override
	@Deprecated
	public void removeName(String name) {
		List<XSSFName> names = namedRangesByName.get(name.toLowerCase(Locale.ENGLISH));
		if (names.isEmpty()) {
			throw new IllegalArgumentException(("Named range was not found: " + name));
		}
		removeName(names.get(0));
	}

	@Override
	public void removeName(Name name) {
		if ((!(namedRangesByName.removeMapping(name.getNameName().toLowerCase(Locale.ENGLISH), name))) || (!(namedRanges.remove(name)))) {
			throw new IllegalArgumentException(("Name was not found: " + name));
		}
	}

	void updateName(XSSFName name, String oldName) {
		if (!(namedRangesByName.removeMapping(oldName.toLowerCase(Locale.ENGLISH), name))) {
			throw new IllegalArgumentException(("Name was not found: " + name));
		}
		namedRangesByName.put(name.getNameName().toLowerCase(Locale.ENGLISH), name);
	}

	@Override
	public void removePrintArea(int sheetIndex) {
		XSSFName name = getBuiltInName(XSSFName.BUILTIN_PRINT_AREA, sheetIndex);
		if (name != null) {
			removeName(name);
		}
	}

	@Override
	public void removeSheetAt(int index) {
		validateSheetIndex(index);
		onSheetDelete(index);
		XSSFSheet sheet = getSheetAt(index);
		removeRelation(sheet);
		sheets.remove(index);
		if ((sheets.size()) == 0) {
			return;
		}
		int newSheetIndex = index;
		if (newSheetIndex >= (sheets.size())) {
			newSheetIndex = (sheets.size()) - 1;
		}
		int active = getActiveSheetIndex();
		if (active == index) {
			setActiveSheet(newSheetIndex);
		}else
			if (active > index) {
				setActiveSheet((active - 1));
			}

	}

	private void onSheetDelete(int index) {
		final XSSFSheet sheet = getSheetAt(index);
		workbook.getSheets().removeSheet(index);
		if ((calcChain) != null) {
			removeRelation(calcChain);
			calcChain = null;
		}
		List<XSSFName> toRemove = new ArrayList<>();
		for (XSSFName nm : namedRanges) {
		}
		for (XSSFName nm : toRemove) {
			removeName(nm);
		}
	}

	@Override
	public Row.MissingCellPolicy getMissingCellPolicy() {
		return _missingCellPolicy;
	}

	@Override
	public void setMissingCellPolicy(Row.MissingCellPolicy missingCellPolicy) {
		_missingCellPolicy = missingCellPolicy;
	}

	@Override
	public void setActiveSheet(int index) {
		validateSheetIndex(index);
		for (CTBookView arrayBook : workbook.getBookViews().getWorkbookViewArray()) {
			arrayBook.setActiveTab(index);
		}
	}

	private void validateSheetIndex(int index) {
		int lastSheetIx = (sheets.size()) - 1;
		if ((index < 0) || (index > lastSheetIx)) {
			String range = ("(0.." + lastSheetIx) + ")";
			if (lastSheetIx == (-1)) {
				range = "(no sheets)";
			}
			throw new IllegalArgumentException(((("Sheet index (" + index) + ") is out of range ") + range));
		}
	}

	@Override
	public int getFirstVisibleTab() {
		CTBookViews bookViews = workbook.getBookViews();
		CTBookView bookView = bookViews.getWorkbookViewArray(0);
		return ((short) (bookView.getFirstSheet()));
	}

	@Override
	public void setFirstVisibleTab(int index) {
		CTBookViews bookViews = workbook.getBookViews();
		CTBookView bookView = bookViews.getWorkbookViewArray(0);
		bookView.setFirstSheet(index);
	}

	@Override
	public void setPrintArea(int sheetIndex, String reference) {
		XSSFName name = getBuiltInName(XSSFName.BUILTIN_PRINT_AREA, sheetIndex);
		if (name == null) {
			name = createBuiltInName(XSSFName.BUILTIN_PRINT_AREA, sheetIndex);
		}
		String[] parts = XSSFWorkbook.COMMA_PATTERN.split(reference);
		StringBuilder sb = new StringBuilder(32);
		for (int i = 0; i < (parts.length); i++) {
			if (i > 0) {
				sb.append(',');
			}
			SheetNameFormatter.appendFormat(sb, getSheetName(sheetIndex));
			sb.append('!');
			sb.append(parts[i]);
		}
		name.setRefersToFormula(sb.toString());
	}

	@Override
	public void setPrintArea(int sheetIndex, int startColumn, int endColumn, int startRow, int endRow) {
		String reference = XSSFWorkbook.getReferencePrintArea(getSheetName(sheetIndex), startColumn, endColumn, startRow, endRow);
		setPrintArea(sheetIndex, reference);
	}

	private static String getReferencePrintArea(String sheetName, int startC, int endC, int startR, int endR) {
		CellReference colRef = new CellReference(sheetName, startR, startC, true, true);
		CellReference colRef2 = new CellReference(sheetName, endR, endC, true, true);
		return (((((("$" + (colRef.getCellRefParts()[2])) + "$") + (colRef.getCellRefParts()[1])) + ":$") + (colRef2.getCellRefParts()[2])) + "$") + (colRef2.getCellRefParts()[1]);
	}

	XSSFName getBuiltInName(String builtInCode, int sheetNumber) {
		for (XSSFName name : namedRangesByName.get(builtInCode.toLowerCase(Locale.ENGLISH))) {
			if ((name.getSheetIndex()) == sheetNumber) {
				return name;
			}
		}
		return null;
	}

	XSSFName createBuiltInName(String builtInName, int sheetNumber) {
		validateSheetIndex(sheetNumber);
		CTDefinedNames names = ((workbook.getDefinedNames()) == null) ? workbook.addNewDefinedNames() : workbook.getDefinedNames();
		CTDefinedName nameRecord = names.addNewDefinedName();
		nameRecord.setName(builtInName);
		nameRecord.setLocalSheetId(sheetNumber);
		if ((getBuiltInName(builtInName, sheetNumber)) != null) {
			throw new POIXMLException((((("Builtin (" + builtInName) + ") already exists for sheet (") + sheetNumber) + ")"));
		}
		return createAndStoreName(nameRecord);
	}

	@Override
	public void setSelectedTab(int index) {
		int idx = 0;
		for (XSSFSheet sh : sheets) {
			sh.setSelected((idx == index));
			idx++;
		}
	}

	@Override
	public void setSheetName(int sheetIndex, String sheetname) {
		if (sheetname == null) {
			throw new IllegalArgumentException("sheetName must not be null");
		}
		validateSheetIndex(sheetIndex);
		String oldSheetName = getSheetName(sheetIndex);
		if ((sheetname.length()) > 31) {
			sheetname = sheetname.substring(0, 31);
		}
		WorkbookUtil.validateSheetName(sheetname);
		if (sheetname.equals(oldSheetName)) {
			return;
		}
		if (containsSheet(sheetname, sheetIndex)) {
			throw new IllegalArgumentException("The workbook already contains a sheet of this name");
		}
		workbook.getSheets().getSheetArray(sheetIndex).setName(sheetname);
	}

	@Override
	public void setSheetOrder(String sheetname, int pos) {
		int idx = getSheetIndex(sheetname);
		sheets.add(pos, sheets.remove(idx));
		CTSheets ct = workbook.getSheets();
		XmlObject cts = ct.getSheetArray(idx).copy();
		workbook.getSheets().removeSheet(idx);
		CTSheet newcts = ct.insertNewSheet(pos);
		newcts.set(cts);
		CTSheet[] sheetArray = ct.getSheetArray();
		for (int i = 0; i < (sheetArray.length); i++) {
		}
		updateNamedRangesAfterSheetReorder(idx, pos);
		updateActiveSheetAfterSheetReorder(idx, pos);
	}

	private void updateNamedRangesAfterSheetReorder(int oldIndex, int newIndex) {
		for (final XSSFName name : namedRanges) {
			final int i = name.getSheetIndex();
			if (i != (-1)) {
				if (i == oldIndex) {
					name.setSheetIndex(newIndex);
				}else
					if ((newIndex <= i) && (i < oldIndex)) {
						name.setSheetIndex((i + 1));
					}else
						if ((oldIndex < i) && (i <= newIndex)) {
							name.setSheetIndex((i - 1));
						}


			}
		}
	}

	private void updateActiveSheetAfterSheetReorder(int oldIndex, int newIndex) {
		int active = getActiveSheetIndex();
		if (active == oldIndex) {
			setActiveSheet(newIndex);
		}else
			if (((active < oldIndex) && (active < newIndex)) || ((active > oldIndex) && (active > newIndex))) {
			}else
				if (newIndex > oldIndex) {
					setActiveSheet((active - 1));
				}else {
					setActiveSheet((active + 1));
				}


	}

	private void saveNamedRanges() {
		if ((namedRanges.size()) > 0) {
			CTDefinedNames names = newInstance();
			CTDefinedName[] nr = new CTDefinedName[namedRanges.size()];
			int i = 0;
			for (XSSFName name : namedRanges) {
				i++;
			}
			names.setDefinedNameArray(nr);
			if (workbook.isSetDefinedNames()) {
				workbook.unsetDefinedNames();
			}
			workbook.setDefinedNames(names);
			reprocessNamedRanges();
		}else {
			if (workbook.isSetDefinedNames()) {
				workbook.unsetDefinedNames();
			}
		}
	}

	private void reprocessNamedRanges() {
		namedRangesByName = new ArrayListValuedHashMap<>();
		namedRanges = new ArrayList<>();
		if (workbook.isSetDefinedNames()) {
			for (CTDefinedName ctName : workbook.getDefinedNames().getDefinedNameArray()) {
				createAndStoreName(ctName);
			}
		}
	}

	private void saveCalculationChain() {
		if ((calcChain) != null) {
			int count = calcChain.getCTCalcChain().sizeOfCArray();
			if (count == 0) {
				removeRelation(calcChain);
				calcChain = null;
			}
		}
	}

	@Override
	protected void commit() throws IOException {
		saveNamedRanges();
		saveCalculationChain();
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		xmlOptions.setSaveSyntheticDocumentElement(new QName(CTWorkbook.type.getName().getNamespaceURI(), "workbook"));
		PackagePart part = getPackagePart();
		try (OutputStream out = part.getOutputStream()) {
			workbook.save(out, xmlOptions);
		}
	}

	@org.apache.poi.util.Internal
	public SharedStringsTable getSharedStringSource() {
		return this.sharedStringSource;
	}

	public StylesTable getStylesSource() {
		return this.stylesSource;
	}

	public ThemesTable getTheme() {
		if ((stylesSource) == null) {
			return null;
		}
		return stylesSource.getTheme();
	}

	@Override
	public XSSFCreationHelper getCreationHelper() {
		if ((_creationHelper) == null) {
		}
		return _creationHelper;
	}

	private boolean containsSheet(String name, int excludeSheetIdx) {
		CTSheet[] ctSheetArray = workbook.getSheets().getSheetArray();
		if ((name.length()) > (XSSFWorkbook.MAX_SENSITIVE_SHEET_NAME_LEN)) {
			name = name.substring(0, XSSFWorkbook.MAX_SENSITIVE_SHEET_NAME_LEN);
		}
		for (int i = 0; i < (ctSheetArray.length); i++) {
			String ctName = ctSheetArray[i].getName();
			if ((ctName.length()) > (XSSFWorkbook.MAX_SENSITIVE_SHEET_NAME_LEN)) {
				ctName = ctName.substring(0, XSSFWorkbook.MAX_SENSITIVE_SHEET_NAME_LEN);
			}
			if ((excludeSheetIdx != i) && (name.equalsIgnoreCase(ctName))) {
				return true;
			}
		}
		return false;
	}

	@org.apache.poi.util.Internal
	public boolean isDate1904() {
		CTWorkbookPr workbookPr = workbook.getWorkbookPr();
		return (workbookPr != null) && (workbookPr.getDate1904());
	}

	@Override
	public List<PackagePart> getAllEmbeddedParts() throws OpenXML4JException {
		List<PackagePart> embedds = new LinkedList<>();
		for (XSSFSheet sheet : sheets) {
			for (PackageRelationship rel : sheet.getPackagePart().getRelationshipsByType(XSSFRelation.OLEEMBEDDINGS.getRelation())) {
				embedds.add(sheet.getPackagePart().getRelatedPart(rel));
			}
			for (PackageRelationship rel : sheet.getPackagePart().getRelationshipsByType(XSSFRelation.PACKEMBEDDINGS.getRelation())) {
				embedds.add(sheet.getPackagePart().getRelatedPart(rel));
			}
		}
		return embedds;
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public boolean isHidden() {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void setHidden(boolean hiddenFlag) {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public boolean isSheetHidden(int sheetIx) {
		validateSheetIndex(sheetIx);
		return false;
	}

	@Override
	public boolean isSheetVeryHidden(int sheetIx) {
		validateSheetIndex(sheetIx);
		return false;
	}

	@Override
	public SheetVisibility getSheetVisibility(int sheetIx) {
		validateSheetIndex(sheetIx);
		throw new IllegalArgumentException("This should never happen");
	}

	@Override
	public void setSheetHidden(int sheetIx, boolean hidden) {
		setSheetVisibility(sheetIx, (hidden ? SheetVisibility.HIDDEN : SheetVisibility.VISIBLE));
	}

	@Override
	public void setSheetVisibility(int sheetIx, SheetVisibility visibility) {
		validateSheetIndex(sheetIx);
	}

	protected void onDeleteFormula(XSSFCell cell) {
		if ((calcChain) != null) {
		}
	}

	@org.apache.poi.util.Internal
	public CalculationChain getCalculationChain() {
		return calcChain;
	}

	@org.apache.poi.util.Internal
	public List<ExternalLinksTable> getExternalLinksTable() {
		return externalLinks;
	}

	public Collection<XSSFMap> getCustomXMLMappings() {
		return (mapInfo) == null ? new ArrayList<>() : mapInfo.getAllXSSFMaps();
	}

	@org.apache.poi.util.Internal
	public MapInfo getMapInfo() {
		return mapInfo;
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public int linkExternalWorkbook(String name, Workbook workbook) {
		throw new RuntimeException("Not Implemented - see bug #57184");
	}

	public boolean isStructureLocked() {
		return (workbookProtectionPresent()) && (workbook.getWorkbookProtection().getLockStructure());
	}

	public boolean isWindowsLocked() {
		return (workbookProtectionPresent()) && (workbook.getWorkbookProtection().getLockWindows());
	}

	public boolean isRevisionLocked() {
		return (workbookProtectionPresent()) && (workbook.getWorkbookProtection().getLockRevision());
	}

	public void lockStructure() {
		safeGetWorkbookProtection().setLockStructure(true);
	}

	public void unLockStructure() {
		safeGetWorkbookProtection().setLockStructure(false);
	}

	public void lockWindows() {
		safeGetWorkbookProtection().setLockWindows(true);
	}

	public void unLockWindows() {
		safeGetWorkbookProtection().setLockWindows(false);
	}

	public void lockRevision() {
		safeGetWorkbookProtection().setLockRevision(true);
	}

	public void unLockRevision() {
		safeGetWorkbookProtection().setLockRevision(false);
	}

	public void setWorkbookPassword(String password, HashAlgorithm hashAlgo) {
		if ((password == null) && (!(workbookProtectionPresent()))) {
			return;
		}
		XSSFPasswordHelper.setPassword(safeGetWorkbookProtection(), password, hashAlgo, "workbook");
	}

	public boolean validateWorkbookPassword(String password) {
		if (!(workbookProtectionPresent())) {
			return password == null;
		}
		return XSSFPasswordHelper.validatePassword(safeGetWorkbookProtection(), password, "workbook");
	}

	public void setRevisionsPassword(String password, HashAlgorithm hashAlgo) {
		if ((password == null) && (!(workbookProtectionPresent()))) {
			return;
		}
		XSSFPasswordHelper.setPassword(safeGetWorkbookProtection(), password, hashAlgo, "revisions");
	}

	public boolean validateRevisionsPassword(String password) {
		if (!(workbookProtectionPresent())) {
			return password == null;
		}
		return XSSFPasswordHelper.validatePassword(safeGetWorkbookProtection(), password, "revisions");
	}

	public void unLock() {
		if (workbookProtectionPresent()) {
			workbook.unsetWorkbookProtection();
		}
	}

	private boolean workbookProtectionPresent() {
		return workbook.isSetWorkbookProtection();
	}

	private CTWorkbookProtection safeGetWorkbookProtection() {
		if (!(workbookProtectionPresent())) {
			return workbook.addNewWorkbookProtection();
		}
		return workbook.getWorkbookProtection();
	}

	UDFFinder getUDFFinder() {
		return _udfFinder;
	}

	@Override
	public void addToolPack(UDFFinder toopack) {
		_udfFinder.add(toopack);
	}

	@Override
	public void setForceFormulaRecalculation(boolean value) {
		CTWorkbook ctWorkbook = getCTWorkbook();
		CTCalcPr calcPr = (ctWorkbook.isSetCalcPr()) ? ctWorkbook.getCalcPr() : ctWorkbook.addNewCalcPr();
		calcPr.setCalcId(0);
		if (value && ((calcPr.getCalcMode()) == (STCalcMode.MANUAL))) {
			calcPr.setCalcMode(STCalcMode.AUTO);
		}
	}

	@Override
	public boolean getForceFormulaRecalculation() {
		CTWorkbook ctWorkbook = getCTWorkbook();
		CTCalcPr calcPr = ctWorkbook.getCalcPr();
		return (calcPr != null) && ((calcPr.getCalcId()) != 0);
	}

	@org.apache.poi.util.Beta
	protected CTPivotCache addPivotCache(String rId) {
		CTWorkbook ctWorkbook = getCTWorkbook();
		CTPivotCaches caches;
		if (ctWorkbook.isSetPivotCaches()) {
			caches = ctWorkbook.getPivotCaches();
		}else {
			caches = ctWorkbook.addNewPivotCaches();
		}
		CTPivotCache cache = caches.addNewPivotCache();
		int tableId = (getPivotTables().size()) + 1;
		cache.setCacheId(tableId);
		cache.setId(rId);
		if ((pivotCaches) == null) {
			pivotCaches = new ArrayList<>();
		}
		pivotCaches.add(cache);
		return cache;
	}

	@org.apache.poi.util.Beta
	public List<XSSFPivotTable> getPivotTables() {
		return pivotTables;
	}

	@org.apache.poi.util.Beta
	protected void setPivotTables(List<XSSFPivotTable> pivotTables) {
		this.pivotTables = pivotTables;
	}

	public XSSFWorkbookType getWorkbookType() {
		return isMacroEnabled() ? XSSFWorkbookType.XLSM : XSSFWorkbookType.XLSX;
	}

	public void setWorkbookType(XSSFWorkbookType type) {
		try {
			getPackagePart().setContentType(type.getContentType());
		} catch (InvalidFormatException e) {
			throw new POIXMLException(e);
		}
	}

	public void setVBAProject(InputStream vbaProjectStream) throws IOException {
		if (!(isMacroEnabled())) {
			setWorkbookType(XSSFWorkbookType.XLSM);
		}
		PackagePartName ppName;
		try {
			ppName = PackagingURIHelper.createPartName(XSSFRelation.VBA_MACROS.getDefaultFileName());
		} catch (InvalidFormatException e) {
			throw new POIXMLException(e);
		}
		OPCPackage opc = getPackage();
		OutputStream outputStream;
		if (!(opc.containPart(ppName))) {
			POIXMLDocumentPart relationship = createRelationship(XSSFRelation.VBA_MACROS, this.xssfFactory);
			outputStream = relationship.getPackagePart().getOutputStream();
		}else {
			PackagePart part = opc.getPart(ppName);
			outputStream = part.getOutputStream();
		}
		try {
			IOUtils.copy(vbaProjectStream, outputStream);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

	public void setVBAProject(XSSFWorkbook macroWorkbook) throws IOException, InvalidFormatException {
		if (!(macroWorkbook.isMacroEnabled())) {
			return;
		}
		InputStream vbaProjectStream = XSSFRelation.VBA_MACROS.getContents(macroWorkbook.getCorePart());
		if (vbaProjectStream != null) {
			setVBAProject(vbaProjectStream);
		}
	}

	@Override
	public SpreadsheetVersion getSpreadsheetVersion() {
		return SpreadsheetVersion.EXCEL2007;
	}

	public XSSFTable getTable(String name) {
		if ((name != null) && ((sheets) != null)) {
			for (XSSFSheet sheet : sheets) {
				for (XSSFTable tbl : sheet.getTables()) {
					if (name.equalsIgnoreCase(tbl.getName())) {
						return tbl;
					}
				}
			}
		}
		return null;
	}

	@Override
	public int addOlePackage(byte[] oleData, String label, String fileName, String command) throws IOException {
		OPCPackage opc = getPackage();
		PackagePartName pnOLE;
		int oleId = 0;
		do {
			try {
				pnOLE = PackagingURIHelper.createPartName((("/xl/embeddings/oleObject" + (++oleId)) + ".bin"));
			} catch (InvalidFormatException e) {
				throw new IOException("ole object name not recognized", e);
			}
		} while (opc.containPart(pnOLE) );
		PackagePart pp = opc.createPart(pnOLE, "application/vnd.openxmlformats-officedocument.oleObject");
		Ole10Native ole10 = new Ole10Native(label, fileName, command, oleData);
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream(((oleData.length) + 500))) {
			ole10.writeOut(bos);
			try (POIFSFileSystem poifs = new POIFSFileSystem()) {
				DirectoryNode root = poifs.getRoot();
				root.createDocument(Ole10Native.OLE10_NATIVE, new ByteArrayInputStream(bos.toByteArray()));
				root.setStorageClsid(ClassIDPredefined.OLE_V1_PACKAGE.getClassID());
				try (OutputStream os = pp.getOutputStream()) {
					poifs.writeFilesystem(os);
				}
			}
		}
		return oleId;
	}

	public void setCellFormulaValidation(final boolean value) {
		this.cellFormulaValidation = value;
	}

	public boolean getCellFormulaValidation() {
		return this.cellFormulaValidation;
	}
}

