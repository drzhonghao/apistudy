

import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FontCharset;
import org.apache.poi.ss.usermodel.FontFamily;
import org.apache.poi.ss.usermodel.FontScheme;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.util.Internal;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.model.ThemesTable;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBooleanProperty;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontName;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontScheme;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontSize;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTIntProperty;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTUnderlineProperty;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTVerticalAlignFontProperty;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STFontScheme;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STUnderlineValues;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STVerticalAlignRun;

import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont.Factory.newInstance;


public class XSSFFont implements Font {
	public static final String DEFAULT_FONT_NAME = "Calibri";

	public static final short DEFAULT_FONT_SIZE = 11;

	public static final short DEFAULT_FONT_COLOR = IndexedColors.BLACK.getIndex();

	private IndexedColorMap _indexedColorMap;

	private ThemesTable _themes;

	private CTFont _ctFont;

	private int _index;

	@Internal
	public XSSFFont(CTFont font) {
		_ctFont = font;
		_index = 0;
	}

	@Internal
	public XSSFFont(CTFont font, int index, IndexedColorMap colorMap) {
		_ctFont = font;
		_index = ((short) (index));
		_indexedColorMap = colorMap;
	}

	public XSSFFont() {
		this._ctFont = newInstance();
		setFontName(XSSFFont.DEFAULT_FONT_NAME);
		setFontHeight(((double) (XSSFFont.DEFAULT_FONT_SIZE)));
	}

	@Internal
	public CTFont getCTFont() {
		return _ctFont;
	}

	public boolean getBold() {
		CTBooleanProperty bold = ((_ctFont.sizeOfBArray()) == 0) ? null : _ctFont.getBArray(0);
		return (bold != null) && (bold.getVal());
	}

	public int getCharSet() {
		CTIntProperty charset = ((_ctFont.sizeOfCharsetArray()) == 0) ? null : _ctFont.getCharsetArray(0);
		return charset == null ? FontCharset.ANSI.getValue() : FontCharset.valueOf(charset.getVal()).getValue();
	}

	public short getColor() {
		CTColor color = ((_ctFont.sizeOfColorArray()) == 0) ? null : _ctFont.getColorArray(0);
		if (color == null)
			return IndexedColors.BLACK.getIndex();

		long index = color.getIndexed();
		if (index == (XSSFFont.DEFAULT_FONT_COLOR)) {
			return IndexedColors.BLACK.getIndex();
		}else
			if (index == (IndexedColors.RED.getIndex())) {
				return IndexedColors.RED.getIndex();
			}else {
				return ((short) (index));
			}

	}

	public XSSFColor getXSSFColor() {
		CTColor ctColor = ((_ctFont.sizeOfColorArray()) == 0) ? null : _ctFont.getColorArray(0);
		if (ctColor != null) {
			XSSFColor color = XSSFColor.from(ctColor, _indexedColorMap);
			if ((_themes) != null) {
				_themes.inheritFromThemeAsRequired(color);
			}
			return color;
		}else {
			return null;
		}
	}

	public short getThemeColor() {
		CTColor color = ((_ctFont.sizeOfColorArray()) == 0) ? null : _ctFont.getColorArray(0);
		long index = (color == null) ? 0 : color.getTheme();
		return ((short) (index));
	}

	public short getFontHeight() {
		return ((short) ((getFontHeightRaw()) * 20));
	}

	public short getFontHeightInPoints() {
		return ((short) (getFontHeightRaw()));
	}

	private double getFontHeightRaw() {
		CTFontSize size = ((_ctFont.sizeOfSzArray()) == 0) ? null : _ctFont.getSzArray(0);
		if (size != null) {
			return size.getVal();
		}
		return XSSFFont.DEFAULT_FONT_SIZE;
	}

	public String getFontName() {
		CTFontName name = ((_ctFont.sizeOfNameArray()) == 0) ? null : _ctFont.getNameArray(0);
		return name == null ? XSSFFont.DEFAULT_FONT_NAME : name.getVal();
	}

	public boolean getItalic() {
		CTBooleanProperty italic = ((_ctFont.sizeOfIArray()) == 0) ? null : _ctFont.getIArray(0);
		return (italic != null) && (italic.getVal());
	}

	public boolean getStrikeout() {
		CTBooleanProperty strike = ((_ctFont.sizeOfStrikeArray()) == 0) ? null : _ctFont.getStrikeArray(0);
		return (strike != null) && (strike.getVal());
	}

	public short getTypeOffset() {
		CTVerticalAlignFontProperty vAlign = ((_ctFont.sizeOfVertAlignArray()) == 0) ? null : _ctFont.getVertAlignArray(0);
		if (vAlign == null) {
			return Font.SS_NONE;
		}
		int val = vAlign.getVal().intValue();
		switch (val) {
			case STVerticalAlignRun.INT_BASELINE :
				return Font.SS_NONE;
			case STVerticalAlignRun.INT_SUBSCRIPT :
				return Font.SS_SUB;
			case STVerticalAlignRun.INT_SUPERSCRIPT :
				return Font.SS_SUPER;
			default :
				throw new POIXMLException(("Wrong offset value " + val));
		}
	}

	public byte getUnderline() {
		CTUnderlineProperty underline = ((_ctFont.sizeOfUArray()) == 0) ? null : _ctFont.getUArray(0);
		if (underline != null) {
			FontUnderline val = FontUnderline.valueOf(underline.getVal().intValue());
			return val.getByteValue();
		}
		return Font.U_NONE;
	}

	public void setBold(boolean bold) {
		if (bold) {
			CTBooleanProperty ctBold = ((_ctFont.sizeOfBArray()) == 0) ? _ctFont.addNewB() : _ctFont.getBArray(0);
			ctBold.setVal(true);
		}else {
			_ctFont.setBArray(null);
		}
	}

	public void setCharSet(byte charset) {
		int cs = charset & 255;
		setCharSet(cs);
	}

	public void setCharSet(int charset) {
		FontCharset fontCharset = FontCharset.valueOf(charset);
		if (fontCharset != null) {
			setCharSet(fontCharset);
		}else {
			throw new POIXMLException("Attention: an attempt to set a type of unknow charset and charset");
		}
	}

	public void setCharSet(FontCharset charSet) {
		CTIntProperty charsetProperty;
		if ((_ctFont.sizeOfCharsetArray()) == 0) {
			charsetProperty = _ctFont.addNewCharset();
		}else {
			charsetProperty = _ctFont.getCharsetArray(0);
		}
		charsetProperty.setVal(charSet.getValue());
	}

	public void setColor(short color) {
		CTColor ctColor = ((_ctFont.sizeOfColorArray()) == 0) ? _ctFont.addNewColor() : _ctFont.getColorArray(0);
		switch (color) {
			case Font.COLOR_NORMAL :
				{
					ctColor.setIndexed(XSSFFont.DEFAULT_FONT_COLOR);
					break;
				}
			case Font.COLOR_RED :
				{
					ctColor.setIndexed(IndexedColors.RED.getIndex());
					break;
				}
			default :
				ctColor.setIndexed(color);
		}
	}

	public void setColor(XSSFColor color) {
		if (color == null)
			_ctFont.setColorArray(null);
		else {
			CTColor ctColor = ((_ctFont.sizeOfColorArray()) == 0) ? _ctFont.addNewColor() : _ctFont.getColorArray(0);
			if (ctColor.isSetIndexed()) {
				ctColor.unsetIndexed();
			}
			ctColor.setRgb(color.getRGB());
		}
	}

	public void setFontHeight(short height) {
		setFontHeight((((double) (height)) / 20));
	}

	public void setFontHeight(double height) {
		CTFontSize fontSize = ((_ctFont.sizeOfSzArray()) == 0) ? _ctFont.addNewSz() : _ctFont.getSzArray(0);
		fontSize.setVal(height);
	}

	public void setFontHeightInPoints(short height) {
		setFontHeight(((double) (height)));
	}

	public void setThemeColor(short theme) {
		CTColor ctColor = ((_ctFont.sizeOfColorArray()) == 0) ? _ctFont.addNewColor() : _ctFont.getColorArray(0);
		ctColor.setTheme(theme);
	}

	public void setFontName(String name) {
		CTFontName fontName = ((_ctFont.sizeOfNameArray()) == 0) ? _ctFont.addNewName() : _ctFont.getNameArray(0);
		fontName.setVal((name == null ? XSSFFont.DEFAULT_FONT_NAME : name));
	}

	public void setItalic(boolean italic) {
		if (italic) {
			CTBooleanProperty bool = ((_ctFont.sizeOfIArray()) == 0) ? _ctFont.addNewI() : _ctFont.getIArray(0);
			bool.setVal(true);
		}else {
			_ctFont.setIArray(null);
		}
	}

	public void setStrikeout(boolean strikeout) {
		if (strikeout) {
			CTBooleanProperty strike = ((_ctFont.sizeOfStrikeArray()) == 0) ? _ctFont.addNewStrike() : _ctFont.getStrikeArray(0);
			strike.setVal(true);
		}else {
			_ctFont.setStrikeArray(null);
		}
	}

	public void setTypeOffset(short offset) {
		if (offset == (Font.SS_NONE)) {
			_ctFont.setVertAlignArray(null);
		}else {
			CTVerticalAlignFontProperty offsetProperty = ((_ctFont.sizeOfVertAlignArray()) == 0) ? _ctFont.addNewVertAlign() : _ctFont.getVertAlignArray(0);
			switch (offset) {
				case Font.SS_NONE :
					offsetProperty.setVal(STVerticalAlignRun.BASELINE);
					break;
				case Font.SS_SUB :
					offsetProperty.setVal(STVerticalAlignRun.SUBSCRIPT);
					break;
				case Font.SS_SUPER :
					offsetProperty.setVal(STVerticalAlignRun.SUPERSCRIPT);
					break;
				default :
					throw new IllegalStateException(("Invalid type offset: " + offset));
			}
		}
	}

	public void setUnderline(byte underline) {
		setUnderline(FontUnderline.valueOf(underline));
	}

	public void setUnderline(FontUnderline underline) {
		if ((underline == (FontUnderline.NONE)) && ((_ctFont.sizeOfUArray()) > 0)) {
			_ctFont.setUArray(null);
		}else {
			CTUnderlineProperty ctUnderline = ((_ctFont.sizeOfUArray()) == 0) ? _ctFont.addNewU() : _ctFont.getUArray(0);
			STUnderlineValues.Enum val = STUnderlineValues.Enum.forInt(underline.getValue());
			ctUnderline.setVal(val);
		}
	}

	public String toString() {
		return _ctFont.toString();
	}

	public long registerTo(StylesTable styles) {
		this._themes = styles.getTheme();
		return this._index;
	}

	public void setThemesTable(ThemesTable themes) {
		this._themes = themes;
	}

	public FontScheme getScheme() {
		CTFontScheme scheme = ((_ctFont.sizeOfSchemeArray()) == 0) ? null : _ctFont.getSchemeArray(0);
		return scheme == null ? FontScheme.NONE : FontScheme.valueOf(scheme.getVal().intValue());
	}

	public void setScheme(FontScheme scheme) {
		CTFontScheme ctFontScheme = ((_ctFont.sizeOfSchemeArray()) == 0) ? _ctFont.addNewScheme() : _ctFont.getSchemeArray(0);
		STFontScheme.Enum val = STFontScheme.Enum.forInt(scheme.getValue());
		ctFontScheme.setVal(val);
	}

	public int getFamily() {
		CTIntProperty family = ((_ctFont.sizeOfFamilyArray()) == 0) ? null : _ctFont.getFamilyArray(0);
		return family == null ? FontFamily.NOT_APPLICABLE.getValue() : FontFamily.valueOf(family.getVal()).getValue();
	}

	public void setFamily(int value) {
		CTIntProperty family = ((_ctFont.sizeOfFamilyArray()) == 0) ? _ctFont.addNewFamily() : _ctFont.getFamilyArray(0);
		family.setVal(value);
	}

	public void setFamily(FontFamily family) {
		setFamily(family.getValue());
	}

	@Override
	@Deprecated
	public short getIndex() {
		return ((short) (getIndexAsInt()));
	}

	@Override
	public int getIndexAsInt() {
		return _index;
	}

	public int hashCode() {
		return _ctFont.toString().hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof XSSFFont))
			return false;

		XSSFFont cf = ((XSSFFont) (o));
		return _ctFont.toString().equals(cf.getCTFont().toString());
	}
}

