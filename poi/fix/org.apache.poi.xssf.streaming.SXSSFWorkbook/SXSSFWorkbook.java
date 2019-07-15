

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.util.ZipArchiveThresholdInputStream;
import org.apache.poi.openxml4j.util.ZipEntrySource;
import org.apache.poi.openxml4j.util.ZipFileZipEntrySource;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.TempFile;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.streaming.GZIPSheetDataWriter;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SheetDataWriter;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFChartSheet;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFName;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class SXSSFWorkbook implements Workbook {
	public static final int DEFAULT_WINDOW_SIZE = 100;

	private static final POILogger logger = POILogFactory.getLogger(SXSSFWorkbook.class);

	private final XSSFWorkbook _wb;

	private final Map<SXSSFSheet, XSSFSheet> _sxFromXHash = new HashMap<>();

	private final Map<XSSFSheet, SXSSFSheet> _xFromSxHash = new HashMap<>();

	private int _randomAccessWindowSize = SXSSFWorkbook.DEFAULT_WINDOW_SIZE;

	private boolean _compressTmpFiles;

	private final SharedStringsTable _sharedStringSource;

	public SXSSFWorkbook() {
		this(null);
	}

	public SXSSFWorkbook(XSSFWorkbook workbook) {
		this(workbook, SXSSFWorkbook.DEFAULT_WINDOW_SIZE);
	}

	public SXSSFWorkbook(XSSFWorkbook workbook, int rowAccessWindowSize) {
		this(workbook, rowAccessWindowSize, false);
	}

	public SXSSFWorkbook(XSSFWorkbook workbook, int rowAccessWindowSize, boolean compressTmpFiles) {
		this(workbook, rowAccessWindowSize, compressTmpFiles, false);
	}

	public SXSSFWorkbook(XSSFWorkbook workbook, int rowAccessWindowSize, boolean compressTmpFiles, boolean useSharedStringsTable) {
		setRandomAccessWindowSize(rowAccessWindowSize);
		setCompressTempFiles(compressTmpFiles);
		if (workbook == null) {
			_wb = new XSSFWorkbook();
			_sharedStringSource = (useSharedStringsTable) ? _wb.getSharedStringSource() : null;
		}else {
			_wb = workbook;
			_sharedStringSource = (useSharedStringsTable) ? _wb.getSharedStringSource() : null;
			for (Sheet sheet : _wb) {
				createAndRegisterSXSSFSheet(((XSSFSheet) (sheet)));
			}
		}
	}

	public SXSSFWorkbook(int rowAccessWindowSize) {
		this(null, rowAccessWindowSize);
	}

	public int getRandomAccessWindowSize() {
		return _randomAccessWindowSize;
	}

	private void setRandomAccessWindowSize(int rowAccessWindowSize) {
		if ((rowAccessWindowSize == 0) || (rowAccessWindowSize < (-1))) {
			throw new IllegalArgumentException("rowAccessWindowSize must be greater than 0 or -1");
		}
		_randomAccessWindowSize = rowAccessWindowSize;
	}

	public boolean isCompressTempFiles() {
		return _compressTmpFiles;
	}

	public void setCompressTempFiles(boolean compress) {
		_compressTmpFiles = compress;
	}

	@org.apache.poi.util.Internal
	protected SharedStringsTable getSharedStringSource() {
		return _sharedStringSource;
	}

	protected SheetDataWriter createSheetDataWriter() throws IOException {
		if (_compressTmpFiles) {
			return new GZIPSheetDataWriter(_sharedStringSource);
		}
		return new SheetDataWriter(_sharedStringSource);
	}

	XSSFSheet getXSSFSheet(SXSSFSheet sheet) {
		return _sxFromXHash.get(sheet);
	}

	SXSSFSheet getSXSSFSheet(XSSFSheet sheet) {
		return _xFromSxHash.get(sheet);
	}

	void registerSheetMapping(SXSSFSheet sxSheet, XSSFSheet xSheet) {
		_sxFromXHash.put(sxSheet, xSheet);
		_xFromSxHash.put(xSheet, sxSheet);
	}

	void deregisterSheetMapping(XSSFSheet xSheet) {
		SXSSFSheet sxSheet = getSXSSFSheet(xSheet);
		_sxFromXHash.remove(sxSheet);
		_xFromSxHash.remove(xSheet);
	}

	private XSSFSheet getSheetFromZipEntryName(String sheetRef) {
		for (XSSFSheet sheet : _sxFromXHash.values()) {
			if (sheetRef.equals(sheet.getPackagePart().getPartName().getName().substring(1))) {
				return sheet;
			}
		}
		return null;
	}

	protected void injectData(ZipEntrySource zipEntrySource, OutputStream out) throws IOException {
		ZipArchiveOutputStream zos = new ZipArchiveOutputStream(out);
		try {
			Enumeration<? extends ZipArchiveEntry> en = zipEntrySource.getEntries();
			while (en.hasMoreElements()) {
				ZipArchiveEntry ze = en.nextElement();
				ZipArchiveEntry zeOut = new ZipArchiveEntry(ze.getName());
				zeOut.setSize(ze.getSize());
				zeOut.setTime(ze.getTime());
				zos.putArchiveEntry(zeOut);
				try (final InputStream is = zipEntrySource.getInputStream(ze)) {
					if (is instanceof ZipArchiveThresholdInputStream) {
						((ZipArchiveThresholdInputStream) (is)).setGuardState(false);
					}
					XSSFSheet xSheet = getSheetFromZipEntryName(ze.getName());
					if ((xSheet != null) && (!(xSheet instanceof XSSFChartSheet))) {
						SXSSFSheet sxSheet = getSXSSFSheet(xSheet);
						try (InputStream xis = sxSheet.getWorksheetXMLInputStream()) {
							SXSSFWorkbook.copyStreamAndInjectWorksheet(is, zos, xis);
						}
					}else {
						IOUtils.copy(is, zos);
					}
				} finally {
					zos.closeArchiveEntry();
				}
			} 
		} finally {
			zos.finish();
			zipEntrySource.close();
		}
	}

	private static void copyStreamAndInjectWorksheet(InputStream in, OutputStream out, InputStream worksheetData) throws IOException {
		InputStreamReader inReader = new InputStreamReader(in, StandardCharsets.UTF_8);
		OutputStreamWriter outWriter = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		boolean needsStartTag = true;
		int c;
		int pos = 0;
		String s = "<sheetData";
		int n = s.length();
		while ((c = inReader.read()) != (-1)) {
			if (c == (s.charAt(pos))) {
				pos++;
				if (pos == n) {
					if ("<sheetData".equals(s)) {
						c = inReader.read();
						if (c == (-1)) {
							outWriter.write(s);
							break;
						}
						if (c == '>') {
							outWriter.write(s);
							outWriter.write(c);
							s = "</sheetData>";
							n = s.length();
							pos = 0;
							needsStartTag = false;
							continue;
						}
						if (c == '/') {
							c = inReader.read();
							if (c == (-1)) {
								outWriter.write(s);
								break;
							}
							if (c == '>') {
								break;
							}
							outWriter.write(s);
							outWriter.write('/');
							outWriter.write(c);
							pos = 0;
							continue;
						}
						outWriter.write(s);
						outWriter.write('/');
						outWriter.write(c);
						pos = 0;
						continue;
					}else {
						break;
					}
				}
			}else {
				if (pos > 0) {
					outWriter.write(s, 0, pos);
				}
				if (c == (s.charAt(0))) {
					pos = 1;
				}else {
					outWriter.write(c);
					pos = 0;
				}
			}
		} 
		outWriter.flush();
		if (needsStartTag) {
			outWriter.write("<sheetData>\n");
			outWriter.flush();
		}
		IOUtils.copy(worksheetData, out);
		outWriter.write("</sheetData>");
		outWriter.flush();
		while ((c = inReader.read()) != (-1)) {
			outWriter.write(c);
		} 
		outWriter.flush();
	}

	public XSSFWorkbook getXSSFWorkbook() {
		return _wb;
	}

	@Override
	public int getActiveSheetIndex() {
		return _wb.getActiveSheetIndex();
	}

	@Override
	public void setActiveSheet(int sheetIndex) {
		_wb.setActiveSheet(sheetIndex);
	}

	@Override
	public int getFirstVisibleTab() {
		return _wb.getFirstVisibleTab();
	}

	@Override
	public void setFirstVisibleTab(int sheetIndex) {
		_wb.setFirstVisibleTab(sheetIndex);
	}

	@Override
	public void setSheetOrder(String sheetname, int pos) {
		_wb.setSheetOrder(sheetname, pos);
	}

	@Override
	public void setSelectedTab(int index) {
		_wb.setSelectedTab(index);
	}

	@Override
	public void setSheetName(int sheet, String name) {
		_wb.setSheetName(sheet, name);
	}

	@Override
	public String getSheetName(int sheet) {
		return _wb.getSheetName(sheet);
	}

	@Override
	public int getSheetIndex(String name) {
		return _wb.getSheetIndex(name);
	}

	@Override
	public int getSheetIndex(Sheet sheet) {
		return _wb.getSheetIndex(getXSSFSheet(((SXSSFSheet) (sheet))));
	}

	@Override
	public SXSSFSheet createSheet() {
		return createAndRegisterSXSSFSheet(_wb.createSheet());
	}

	SXSSFSheet createAndRegisterSXSSFSheet(XSSFSheet xSheet) {
		final SXSSFSheet sxSheet;
		sxSheet = null;
		registerSheetMapping(sxSheet, xSheet);
		return sxSheet;
	}

	@Override
	public SXSSFSheet createSheet(String sheetname) {
		return createAndRegisterSXSSFSheet(_wb.createSheet(sheetname));
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public Sheet cloneSheet(int sheetNum) {
		throw new RuntimeException("Not Implemented");
	}

	@Override
	public int getNumberOfSheets() {
		return _wb.getNumberOfSheets();
	}

	@Override
	public Iterator<Sheet> sheetIterator() {
		return new SXSSFWorkbook.SheetIterator<>();
	}

	private final class SheetIterator<T extends Sheet> implements Iterator<T> {
		private final Iterator<XSSFSheet> it;

		@SuppressWarnings("unchecked")
		public SheetIterator() {
			it = ((Iterator<XSSFSheet>) ((Iterator<? extends Sheet>) (_wb.iterator())));
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public T next() throws NoSuchElementException {
			final XSSFSheet xssfSheet = it.next();
			return ((T) (getSXSSFSheet(xssfSheet)));
		}

		@Override
		public void remove() throws IllegalStateException {
			throw new UnsupportedOperationException(("remove method not supported on XSSFWorkbook.iterator(). " + "Use Sheet.removeSheetAt(int) instead."));
		}
	}

	@Override
	public Iterator<Sheet> iterator() {
		return sheetIterator();
	}

	@Override
	public SXSSFSheet getSheetAt(int index) {
		return getSXSSFSheet(_wb.getSheetAt(index));
	}

	@Override
	public SXSSFSheet getSheet(String name) {
		return getSXSSFSheet(_wb.getSheet(name));
	}

	@Override
	public void removeSheetAt(int index) {
		XSSFSheet xSheet = _wb.getSheetAt(index);
		SXSSFSheet sxSheet = getSXSSFSheet(xSheet);
		_wb.removeSheetAt(index);
		deregisterSheetMapping(xSheet);
	}

	@Override
	public Font createFont() {
		return _wb.createFont();
	}

	@Override
	public Font findFont(boolean bold, short color, short fontHeight, String name, boolean italic, boolean strikeout, short typeOffset, byte underline) {
		return _wb.findFont(bold, color, fontHeight, name, italic, strikeout, typeOffset, underline);
	}

	@Override
	@Deprecated
	public short getNumberOfFonts() {
		return ((short) (getNumberOfFontsAsInt()));
	}

	@Override
	public int getNumberOfFontsAsInt() {
		return _wb.getNumberOfFontsAsInt();
	}

	@Override
	@Deprecated
	public Font getFontAt(short idx) {
		return getFontAt(((int) (idx)));
	}

	@Override
	public Font getFontAt(int idx) {
		return _wb.getFontAt(idx);
	}

	@Override
	public CellStyle createCellStyle() {
		return _wb.createCellStyle();
	}

	@Override
	public int getNumCellStyles() {
		return _wb.getNumCellStyles();
	}

	@Override
	public CellStyle getCellStyleAt(int idx) {
		return _wb.getCellStyleAt(idx);
	}

	@Override
	public void close() throws IOException {
		for (SXSSFSheet sheet : _xFromSxHash.values()) {
		}
		_wb.close();
	}

	@Override
	public void write(OutputStream stream) throws IOException {
		flushSheets();
		File tmplFile = TempFile.createTempFile("poi-sxssf-template", ".xlsx");
		boolean deleted;
		try {
			try (FileOutputStream os = new FileOutputStream(tmplFile)) {
				_wb.write(os);
			}
			try (ZipSecureFile zf = new ZipSecureFile(tmplFile);ZipFileZipEntrySource source = new ZipFileZipEntrySource(zf)) {
				injectData(source, stream);
			}
		} finally {
			deleted = tmplFile.delete();
		}
		if (!deleted) {
			throw new IOException(("Could not delete temporary file after processing: " + tmplFile));
		}
	}

	protected void flushSheets() throws IOException {
		for (SXSSFSheet sheet : _xFromSxHash.values()) {
			sheet.flushRows();
		}
	}

	public boolean dispose() {
		boolean success = true;
		for (SXSSFSheet sheet : _sxFromXHash.keySet()) {
		}
		return success;
	}

	@Override
	public int getNumberOfNames() {
		return _wb.getNumberOfNames();
	}

	@Override
	public Name getName(String name) {
		return _wb.getName(name);
	}

	@Override
	public List<? extends Name> getNames(String name) {
		return _wb.getNames(name);
	}

	@Override
	public List<? extends Name> getAllNames() {
		return _wb.getAllNames();
	}

	@Override
	public Name createName() {
		return _wb.createName();
	}

	@Override
	public void removeName(Name name) {
		_wb.removeName(name);
	}

	@Override
	public void setPrintArea(int sheetIndex, String reference) {
		_wb.setPrintArea(sheetIndex, reference);
	}

	@Override
	public void setPrintArea(int sheetIndex, int startColumn, int endColumn, int startRow, int endRow) {
		_wb.setPrintArea(sheetIndex, startColumn, endColumn, startRow, endRow);
	}

	@Override
	public String getPrintArea(int sheetIndex) {
		return _wb.getPrintArea(sheetIndex);
	}

	@Override
	public void removePrintArea(int sheetIndex) {
		_wb.removePrintArea(sheetIndex);
	}

	@Override
	public Row.MissingCellPolicy getMissingCellPolicy() {
		return _wb.getMissingCellPolicy();
	}

	@Override
	public void setMissingCellPolicy(Row.MissingCellPolicy missingCellPolicy) {
		_wb.setMissingCellPolicy(missingCellPolicy);
	}

	@Override
	public DataFormat createDataFormat() {
		return _wb.createDataFormat();
	}

	@Override
	public int addPicture(byte[] pictureData, int format) {
		return _wb.addPicture(pictureData, format);
	}

	@Override
	public List<? extends PictureData> getAllPictures() {
		return _wb.getAllPictures();
	}

	@Override
	public CreationHelper getCreationHelper() {
		return null;
	}

	protected boolean isDate1904() {
		return _wb.isDate1904();
	}

	@Override
	@org.apache.poi.util.NotImplemented("XSSFWorkbook#isHidden is not implemented")
	public boolean isHidden() {
		return _wb.isHidden();
	}

	@Override
	@org.apache.poi.util.NotImplemented("XSSFWorkbook#setHidden is not implemented")
	public void setHidden(boolean hiddenFlag) {
		_wb.setHidden(hiddenFlag);
	}

	@Override
	public boolean isSheetHidden(int sheetIx) {
		return _wb.isSheetHidden(sheetIx);
	}

	@Override
	public boolean isSheetVeryHidden(int sheetIx) {
		return _wb.isSheetVeryHidden(sheetIx);
	}

	@Override
	public SheetVisibility getSheetVisibility(int sheetIx) {
		return _wb.getSheetVisibility(sheetIx);
	}

	@Override
	public void setSheetHidden(int sheetIx, boolean hidden) {
		_wb.setSheetHidden(sheetIx, hidden);
	}

	@Override
	public void setSheetVisibility(int sheetIx, SheetVisibility visibility) {
		_wb.setSheetVisibility(sheetIx, visibility);
	}

	@Override
	@Deprecated
	@org.apache.poi.util.Removal(version = "3.20")
	public Name getNameAt(int nameIndex) {
		return _wb.getNameAt(nameIndex);
	}

	@Override
	@Deprecated
	@org.apache.poi.util.Removal(version = "3.20")
	public int getNameIndex(String name) {
		return _wb.getNameIndex(name);
	}

	@Override
	@Deprecated
	@org.apache.poi.util.Removal(version = "3.20")
	public void removeName(int index) {
		_wb.removeName(index);
	}

	@Override
	@Deprecated
	@org.apache.poi.util.Removal(version = "3.20")
	public void removeName(String name) {
		_wb.removeName(name);
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public int linkExternalWorkbook(String name, Workbook workbook) {
		throw new RuntimeException("Not Implemented");
	}

	@Override
	public void addToolPack(UDFFinder toopack) {
		_wb.addToolPack(toopack);
	}

	@Override
	public void setForceFormulaRecalculation(boolean value) {
		_wb.setForceFormulaRecalculation(value);
	}

	@Override
	public boolean getForceFormulaRecalculation() {
		return _wb.getForceFormulaRecalculation();
	}

	@Override
	public SpreadsheetVersion getSpreadsheetVersion() {
		return SpreadsheetVersion.EXCEL2007;
	}

	@Override
	public int addOlePackage(byte[] oleData, String label, String fileName, String command) throws IOException {
		return _wb.addOlePackage(oleData, label, fileName, command);
	}
}

