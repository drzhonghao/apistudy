

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.FontFamily;
import org.apache.poi.ss.usermodel.FontScheme;
import org.apache.poi.ss.usermodel.TableStyle;
import org.apache.poi.xssf.model.Styles;
import org.apache.poi.xssf.model.ThemesTable;
import org.apache.poi.xssf.usermodel.CustomIndexedColorMap;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFBuiltinTableStyle;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFactory;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFTableStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellFill;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorder;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorderPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorders;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyleXfs;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellXfs;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColors;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDxf;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDxfs;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFills;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFonts;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTNumFmt;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTNumFmts;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPatternFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTStylesheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyles;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STPatternType;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.StyleSheetDocument;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf.Factory.newInstance;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.StyleSheetDocument.Factory.parse;


public class StylesTable extends POIXMLDocumentPart implements Styles {
	private final SortedMap<Short, String> numberFormats = new TreeMap<>();

	private final List<XSSFFont> fonts = new ArrayList<>();

	private final List<XSSFCellFill> fills = new ArrayList<>();

	private final List<XSSFCellBorder> borders = new ArrayList<>();

	private final List<CTXf> styleXfs = new ArrayList<>();

	private final List<CTXf> xfs = new ArrayList<>();

	private final List<CTDxf> dxfs = new ArrayList<>();

	private final Map<String, TableStyle> tableStyles = new HashMap<>();

	private IndexedColorMap indexedColors = new DefaultIndexedColorMap();

	public static final int FIRST_CUSTOM_STYLE_ID = (BuiltinFormats.FIRST_USER_DEFINED_FORMAT_INDEX) + 1;

	private static final int MAXIMUM_STYLE_ID = SpreadsheetVersion.EXCEL2007.getMaxCellStyles();

	private static final short FIRST_USER_DEFINED_NUMBER_FORMAT_ID = BuiltinFormats.FIRST_USER_DEFINED_FORMAT_INDEX;

	private int MAXIMUM_NUMBER_OF_DATA_FORMATS = 250;

	public void setMaxNumberOfDataFormats(int num) {
		if (num < (getNumDataFormats())) {
			if (num < 0) {
				throw new IllegalArgumentException("Maximum Number of Data Formats must be greater than or equal to 0");
			}else {
				throw new IllegalStateException(("Cannot set the maximum number of data formats less than the current quantity. " + "Data formats must be explicitly removed (via StylesTable.removeNumberFormat) before the limit can be decreased."));
			}
		}
		MAXIMUM_NUMBER_OF_DATA_FORMATS = num;
	}

	public int getMaxNumberOfDataFormats() {
		return MAXIMUM_NUMBER_OF_DATA_FORMATS;
	}

	private StyleSheetDocument doc;

	private XSSFWorkbook workbook;

	private ThemesTable theme;

	public StylesTable() {
		super();
		doc = StyleSheetDocument.Factory.newInstance();
		doc.addNewStyleSheet();
		initialize();
	}

	public StylesTable(PackagePart part) throws IOException {
		super(part);
		readFrom(part.getInputStream());
	}

	public void setWorkbook(XSSFWorkbook wb) {
		this.workbook = wb;
	}

	public ThemesTable getTheme() {
		return theme;
	}

	public void setTheme(ThemesTable theme) {
		this.theme = theme;
		for (XSSFFont font : fonts) {
			font.setThemesTable(theme);
		}
		for (XSSFCellBorder border : borders) {
			border.setThemesTable(theme);
		}
	}

	public void ensureThemesTable() {
		if ((theme) != null)
			return;

		setTheme(((ThemesTable) (workbook.createRelationship(XSSFRelation.THEME, XSSFFactory.getInstance()))));
	}

	public void readFrom(InputStream is) throws IOException {
		try {
			doc = parse(is, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
			CTStylesheet styleSheet = doc.getStyleSheet();
			IndexedColorMap customColors = CustomIndexedColorMap.fromColors(styleSheet.getColors());
			if (customColors != null)
				indexedColors = customColors;

			CTNumFmts ctfmts = styleSheet.getNumFmts();
			if (ctfmts != null) {
				for (CTNumFmt nfmt : ctfmts.getNumFmtArray()) {
					short formatId = ((short) (nfmt.getNumFmtId()));
					numberFormats.put(formatId, nfmt.getFormatCode());
				}
			}
			CTFonts ctfonts = styleSheet.getFonts();
			if (ctfonts != null) {
				int idx = 0;
				for (CTFont font : ctfonts.getFontArray()) {
					XSSFFont f = new XSSFFont(font, idx, indexedColors);
					fonts.add(f);
					idx++;
				}
			}
			CTFills ctfills = styleSheet.getFills();
			if (ctfills != null) {
				for (CTFill fill : ctfills.getFillArray()) {
					fills.add(new XSSFCellFill(fill, indexedColors));
				}
			}
			CTBorders ctborders = styleSheet.getBorders();
			if (ctborders != null) {
				for (CTBorder border : ctborders.getBorderArray()) {
					borders.add(new XSSFCellBorder(border, indexedColors));
				}
			}
			CTCellXfs cellXfs = styleSheet.getCellXfs();
			if (cellXfs != null)
				xfs.addAll(Arrays.asList(cellXfs.getXfArray()));

			CTCellStyleXfs cellStyleXfs = styleSheet.getCellStyleXfs();
			if (cellStyleXfs != null)
				styleXfs.addAll(Arrays.asList(cellStyleXfs.getXfArray()));

			CTDxfs styleDxfs = styleSheet.getDxfs();
			if (styleDxfs != null)
				dxfs.addAll(Arrays.asList(styleDxfs.getDxfArray()));

			CTTableStyles ctTableStyles = styleSheet.getTableStyles();
			if (ctTableStyles != null) {
				int idx = 0;
				for (CTTableStyle style : Arrays.asList(ctTableStyles.getTableStyleArray())) {
					tableStyles.put(style.getName(), new XSSFTableStyle(idx, styleDxfs, style, indexedColors));
					idx++;
				}
			}
		} catch (XmlException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}

	@Override
	public String getNumberFormatAt(short fmtId) {
		return numberFormats.get(fmtId);
	}

	private short getNumberFormatId(String fmt) {
		for (Map.Entry<Short, String> numFmt : numberFormats.entrySet()) {
			if (numFmt.getValue().equals(fmt)) {
				return numFmt.getKey();
			}
		}
		throw new IllegalStateException(("Number format not in style table: " + fmt));
	}

	@Override
	public int putNumberFormat(String fmt) {
		if (numberFormats.containsValue(fmt)) {
			try {
				return getNumberFormatId(fmt);
			} catch (final IllegalStateException e) {
				throw new IllegalStateException("Found the format, but couldn't figure out where - should never happen!");
			}
		}
		if ((numberFormats.size()) >= (MAXIMUM_NUMBER_OF_DATA_FORMATS)) {
			throw new IllegalStateException(((("The maximum number of Data Formats was exceeded. " + "You can define up to ") + (MAXIMUM_NUMBER_OF_DATA_FORMATS)) + " formats in a .xlsx Workbook."));
		}
		final short formatIndex;
		if (numberFormats.isEmpty()) {
			formatIndex = StylesTable.FIRST_USER_DEFINED_NUMBER_FORMAT_ID;
		}else {
			short nextKey = ((short) ((numberFormats.lastKey()) + 1));
			if (nextKey < 0) {
				throw new IllegalStateException(("Cowardly avoiding creating a number format with a negative id. " + "This is probably due to arithmetic overflow."));
			}
			formatIndex = ((short) (Math.max(nextKey, StylesTable.FIRST_USER_DEFINED_NUMBER_FORMAT_ID)));
		}
		numberFormats.put(formatIndex, fmt);
		return formatIndex;
	}

	@Override
	public void putNumberFormat(short index, String fmt) {
		numberFormats.put(index, fmt);
	}

	@Override
	public boolean removeNumberFormat(short index) {
		String fmt = numberFormats.remove(index);
		boolean removed = fmt != null;
		if (removed) {
			for (final CTXf style : xfs) {
				if ((style.isSetNumFmtId()) && ((style.getNumFmtId()) == index)) {
					style.unsetApplyNumberFormat();
					style.unsetNumFmtId();
				}
			}
		}
		return removed;
	}

	@Override
	public boolean removeNumberFormat(String fmt) {
		short id = getNumberFormatId(fmt);
		return removeNumberFormat(id);
	}

	@Override
	public XSSFFont getFontAt(int idx) {
		return fonts.get(idx);
	}

	@Override
	public int putFont(XSSFFont font, boolean forceRegistration) {
		int idx = -1;
		if (!forceRegistration) {
			idx = fonts.indexOf(font);
		}
		if (idx != (-1)) {
			return idx;
		}
		idx = fonts.size();
		fonts.add(font);
		return idx;
	}

	@Override
	public int putFont(XSSFFont font) {
		return putFont(font, false);
	}

	@Override
	public XSSFCellStyle getStyleAt(int idx) {
		int styleXfId = 0;
		if ((idx < 0) || (idx >= (xfs.size()))) {
			return null;
		}
		if ((xfs.get(idx).getXfId()) > 0) {
			styleXfId = ((int) (xfs.get(idx).getXfId()));
		}
		return null;
	}

	@Override
	public int putStyle(XSSFCellStyle style) {
		CTXf mainXF = style.getCoreXf();
		if (!(xfs.contains(mainXF))) {
			xfs.add(mainXF);
		}
		return xfs.indexOf(mainXF);
	}

	@Override
	public XSSFCellBorder getBorderAt(int idx) {
		return borders.get(idx);
	}

	@Override
	public int putBorder(XSSFCellBorder border) {
		int idx = borders.indexOf(border);
		if (idx != (-1)) {
			return idx;
		}
		borders.add(border);
		border.setThemesTable(theme);
		return (borders.size()) - 1;
	}

	@Override
	public XSSFCellFill getFillAt(int idx) {
		return fills.get(idx);
	}

	public List<XSSFCellBorder> getBorders() {
		return Collections.unmodifiableList(borders);
	}

	public List<XSSFCellFill> getFills() {
		return Collections.unmodifiableList(fills);
	}

	public List<XSSFFont> getFonts() {
		return Collections.unmodifiableList(fonts);
	}

	public Map<Short, String> getNumberFormats() {
		return Collections.unmodifiableMap(numberFormats);
	}

	@Override
	public int putFill(XSSFCellFill fill) {
		int idx = fills.indexOf(fill);
		if (idx != (-1)) {
			return idx;
		}
		fills.add(fill);
		return (fills.size()) - 1;
	}

	@org.apache.poi.util.Internal
	public CTXf getCellXfAt(int idx) {
		return xfs.get(idx);
	}

	@org.apache.poi.util.Internal
	public int putCellXf(CTXf cellXf) {
		xfs.add(cellXf);
		return xfs.size();
	}

	@org.apache.poi.util.Internal
	public void replaceCellXfAt(int idx, CTXf cellXf) {
		xfs.set(idx, cellXf);
	}

	@org.apache.poi.util.Internal
	public CTXf getCellStyleXfAt(int idx) {
		try {
			return styleXfs.get(idx);
		} catch (final IndexOutOfBoundsException e) {
			return null;
		}
	}

	@org.apache.poi.util.Internal
	public int putCellStyleXf(CTXf cellStyleXf) {
		styleXfs.add(cellStyleXf);
		return styleXfs.size();
	}

	@org.apache.poi.util.Internal
	protected void replaceCellStyleXfAt(int idx, CTXf cellStyleXf) {
		styleXfs.set(idx, cellStyleXf);
	}

	@Override
	public int getNumCellStyles() {
		return xfs.size();
	}

	@Override
	public int getNumDataFormats() {
		return numberFormats.size();
	}

	@org.apache.poi.util.Internal
	int _getXfsSize() {
		return xfs.size();
	}

	@org.apache.poi.util.Internal
	public int _getStyleXfsSize() {
		return styleXfs.size();
	}

	@org.apache.poi.util.Internal
	public CTStylesheet getCTStylesheet() {
		return doc.getStyleSheet();
	}

	@org.apache.poi.util.Internal
	public int _getDXfsSize() {
		return dxfs.size();
	}

	public void writeTo(OutputStream out) throws IOException {
		CTStylesheet styleSheet = doc.getStyleSheet();
		CTNumFmts formats = CTNumFmts.Factory.newInstance();
		formats.setCount(numberFormats.size());
		for (final Map.Entry<Short, String> entry : numberFormats.entrySet()) {
			CTNumFmt ctFmt = formats.addNewNumFmt();
			ctFmt.setNumFmtId(entry.getKey());
			ctFmt.setFormatCode(entry.getValue());
		}
		styleSheet.setNumFmts(formats);
		int idx;
		CTFonts ctFonts = styleSheet.getFonts();
		if (ctFonts == null) {
			ctFonts = CTFonts.Factory.newInstance();
		}
		ctFonts.setCount(fonts.size());
		CTFont[] ctfnt = new CTFont[fonts.size()];
		idx = 0;
		for (XSSFFont f : fonts)
			ctfnt[(idx++)] = f.getCTFont();

		ctFonts.setFontArray(ctfnt);
		styleSheet.setFonts(ctFonts);
		CTFills ctFills = styleSheet.getFills();
		if (ctFills == null) {
			ctFills = CTFills.Factory.newInstance();
		}
		ctFills.setCount(fills.size());
		CTFill[] ctf = new CTFill[fills.size()];
		idx = 0;
		for (XSSFCellFill f : fills)
			ctf[(idx++)] = f.getCTFill();

		ctFills.setFillArray(ctf);
		styleSheet.setFills(ctFills);
		CTBorders ctBorders = styleSheet.getBorders();
		if (ctBorders == null) {
			ctBorders = CTBorders.Factory.newInstance();
		}
		ctBorders.setCount(borders.size());
		CTBorder[] ctb = new CTBorder[borders.size()];
		idx = 0;
		for (XSSFCellBorder b : borders)
			ctb[(idx++)] = b.getCTBorder();

		ctBorders.setBorderArray(ctb);
		styleSheet.setBorders(ctBorders);
		if ((xfs.size()) > 0) {
			CTCellXfs ctXfs = styleSheet.getCellXfs();
			if (ctXfs == null) {
				ctXfs = CTCellXfs.Factory.newInstance();
			}
			ctXfs.setCount(xfs.size());
			ctXfs.setXfArray(xfs.toArray(new CTXf[xfs.size()]));
			styleSheet.setCellXfs(ctXfs);
		}
		if ((styleXfs.size()) > 0) {
			CTCellStyleXfs ctSXfs = styleSheet.getCellStyleXfs();
			if (ctSXfs == null) {
				ctSXfs = CTCellStyleXfs.Factory.newInstance();
			}
			ctSXfs.setCount(styleXfs.size());
			ctSXfs.setXfArray(styleXfs.toArray(new CTXf[styleXfs.size()]));
			styleSheet.setCellStyleXfs(ctSXfs);
		}
		if ((dxfs.size()) > 0) {
			CTDxfs ctDxfs = styleSheet.getDxfs();
			if (ctDxfs == null) {
				ctDxfs = CTDxfs.Factory.newInstance();
			}
			ctDxfs.setCount(dxfs.size());
			ctDxfs.setDxfArray(dxfs.toArray(new CTDxf[dxfs.size()]));
			styleSheet.setDxfs(ctDxfs);
		}
		doc.save(out, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
	}

	@Override
	protected void commit() throws IOException {
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		writeTo(out);
		out.close();
	}

	private void initialize() {
		XSSFFont xssfFont = StylesTable.createDefaultFont();
		fonts.add(xssfFont);
		CTFill[] ctFill = StylesTable.createDefaultFills();
		fills.add(new XSSFCellFill(ctFill[0], indexedColors));
		fills.add(new XSSFCellFill(ctFill[1], indexedColors));
		CTBorder ctBorder = StylesTable.createDefaultBorder();
		borders.add(new XSSFCellBorder(ctBorder));
		CTXf styleXf = StylesTable.createDefaultXf();
		styleXfs.add(styleXf);
		CTXf xf = StylesTable.createDefaultXf();
		xf.setXfId(0);
		xfs.add(xf);
	}

	private static CTXf createDefaultXf() {
		CTXf ctXf = newInstance();
		ctXf.setNumFmtId(0);
		ctXf.setFontId(0);
		ctXf.setFillId(0);
		ctXf.setBorderId(0);
		return ctXf;
	}

	private static CTBorder createDefaultBorder() {
		CTBorder ctBorder = CTBorder.Factory.newInstance();
		ctBorder.addNewBottom();
		ctBorder.addNewTop();
		ctBorder.addNewLeft();
		ctBorder.addNewRight();
		ctBorder.addNewDiagonal();
		return ctBorder;
	}

	private static CTFill[] createDefaultFills() {
		CTFill[] ctFill = new CTFill[]{ CTFill.Factory.newInstance(), CTFill.Factory.newInstance() };
		ctFill[0].addNewPatternFill().setPatternType(STPatternType.NONE);
		ctFill[1].addNewPatternFill().setPatternType(STPatternType.DARK_GRAY);
		return ctFill;
	}

	private static XSSFFont createDefaultFont() {
		CTFont ctFont = CTFont.Factory.newInstance();
		XSSFFont xssfFont = new XSSFFont(ctFont, 0, null);
		xssfFont.setFontHeightInPoints(XSSFFont.DEFAULT_FONT_SIZE);
		xssfFont.setColor(XSSFFont.DEFAULT_FONT_COLOR);
		xssfFont.setFontName(XSSFFont.DEFAULT_FONT_NAME);
		xssfFont.setFamily(FontFamily.SWISS);
		xssfFont.setScheme(FontScheme.MINOR);
		return xssfFont;
	}

	@org.apache.poi.util.Internal
	public CTDxf getDxfAt(int idx) {
		return dxfs.get(idx);
	}

	@org.apache.poi.util.Internal
	public int putDxf(CTDxf dxf) {
		this.dxfs.add(dxf);
		return this.dxfs.size();
	}

	public TableStyle getExplicitTableStyle(String name) {
		return tableStyles.get(name);
	}

	public Set<String> getExplicitTableStyleNames() {
		return tableStyles.keySet();
	}

	public TableStyle getTableStyle(String name) {
		if (name == null)
			return null;

		try {
			return XSSFBuiltinTableStyle.valueOf(name).getStyle();
		} catch (IllegalArgumentException e) {
			return getExplicitTableStyle(name);
		}
	}

	public XSSFCellStyle createCellStyle() {
		if ((getNumCellStyles()) > (StylesTable.MAXIMUM_STYLE_ID)) {
			throw new IllegalStateException(((("The maximum number of Cell Styles was exceeded. " + "You can define up to ") + (StylesTable.MAXIMUM_STYLE_ID)) + " style in a .xlsx Workbook"));
		}
		int xfSize = styleXfs.size();
		CTXf xf = newInstance();
		xf.setNumFmtId(0);
		xf.setFontId(0);
		xf.setFillId(0);
		xf.setBorderId(0);
		xf.setXfId(0);
		int indexXf = putCellXf(xf);
		return null;
	}

	public XSSFFont findFont(boolean bold, short color, short fontHeight, String name, boolean italic, boolean strikeout, short typeOffset, byte underline) {
		for (XSSFFont font : fonts) {
			if (((((((((font.getBold()) == bold) && ((font.getColor()) == color)) && ((font.getFontHeight()) == fontHeight)) && (font.getFontName().equals(name))) && ((font.getItalic()) == italic)) && ((font.getStrikeout()) == strikeout)) && ((font.getTypeOffset()) == typeOffset)) && ((font.getUnderline()) == underline)) {
				return font;
			}
		}
		return null;
	}

	public XSSFFont findFont(boolean bold, Color color, short fontHeight, String name, boolean italic, boolean strikeout, short typeOffset, byte underline) {
		for (XSSFFont font : fonts) {
			if (((((((((font.getBold()) == bold) && (font.getXSSFColor().equals(color))) && ((font.getFontHeight()) == fontHeight)) && (font.getFontName().equals(name))) && ((font.getItalic()) == italic)) && ((font.getStrikeout()) == strikeout)) && ((font.getTypeOffset()) == typeOffset)) && ((font.getUnderline()) == underline)) {
				return font;
			}
		}
		return null;
	}

	public IndexedColorMap getIndexedColors() {
		return indexedColors;
	}
}

