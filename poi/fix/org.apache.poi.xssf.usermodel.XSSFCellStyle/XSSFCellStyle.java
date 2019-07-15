

import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.ReadingOrder;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.model.ThemesTable;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellAlignment;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellFill;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorder;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorderPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellAlignment;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellProtection;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPatternFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STBorderStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STHorizontalAlignment;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STPatternType;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STVerticalAlignment;

import static org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide.BOTTOM;
import static org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide.LEFT;
import static org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide.RIGHT;
import static org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder.BorderSide.TOP;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellAlignment.Factory.newInstance;
import static org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont.Factory.parse;


public class XSSFCellStyle implements CellStyle {
	private int _cellXfId;

	private final StylesTable _stylesSource;

	private CTXf _cellXf;

	private final CTXf _cellStyleXf;

	private XSSFFont _font;

	private XSSFCellAlignment _cellAlignment;

	private ThemesTable _theme;

	public XSSFCellStyle(int cellXfId, int cellStyleXfId, StylesTable stylesSource, ThemesTable theme) {
		_cellXfId = cellXfId;
		_stylesSource = stylesSource;
		_cellXf = stylesSource.getCellXfAt(this._cellXfId);
		_cellStyleXf = (cellStyleXfId == (-1)) ? null : stylesSource.getCellStyleXfAt(cellStyleXfId);
		_theme = theme;
	}

	@org.apache.poi.util.Internal
	public CTXf getCoreXf() {
		return _cellXf;
	}

	@org.apache.poi.util.Internal
	public CTXf getStyleXf() {
		return _cellStyleXf;
	}

	public XSSFCellStyle(StylesTable stylesSource) {
		_stylesSource = stylesSource;
		_cellXf = CTXf.Factory.newInstance();
		_cellStyleXf = null;
	}

	public void verifyBelongsToStylesSource(StylesTable src) {
		if ((this._stylesSource) != src) {
			throw new IllegalArgumentException("This Style does not belong to the supplied Workbook Styles Source. Are you trying to assign a style from one workbook to the cell of a different workbook?");
		}
	}

	@Override
	public void cloneStyleFrom(CellStyle source) {
		if (source instanceof XSSFCellStyle) {
			XSSFCellStyle src = ((XSSFCellStyle) (source));
			if ((src._stylesSource) == (_stylesSource)) {
				_cellXf.set(src.getCoreXf());
				_cellStyleXf.set(src.getStyleXf());
			}else {
				try {
					if (_cellXf.isSetAlignment())
						_cellXf.unsetAlignment();

					if (_cellXf.isSetExtLst())
						_cellXf.unsetExtLst();

					_cellXf = CTXf.Factory.parse(src.getCoreXf().toString(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
					CTFill fill = CTFill.Factory.parse(src.getCTFill().toString(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
					addFill(fill);
					CTBorder border = CTBorder.Factory.parse(src.getCTBorder().toString(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
					addBorder(border);
					_stylesSource.replaceCellXfAt(_cellXfId, _cellXf);
				} catch (XmlException e) {
					throw new POIXMLException(e);
				}
				String fmt = src.getDataFormatString();
				try {
					CTFont ctFont = parse(src.getFont().getCTFont().toString(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
					XSSFFont font = new XSSFFont(ctFont);
					font.registerTo(_stylesSource);
					setFont(font);
				} catch (XmlException e) {
					throw new POIXMLException(e);
				}
			}
			_font = null;
			_cellAlignment = null;
		}else {
			throw new IllegalArgumentException("Can only clone from one XSSFCellStyle to another, not between HSSFCellStyle and XSSFCellStyle");
		}
	}

	private void addFill(CTFill fill) {
		int idx = _stylesSource.putFill(new XSSFCellFill(fill, _stylesSource.getIndexedColors()));
		_cellXf.setFillId(idx);
		_cellXf.setApplyFill(true);
	}

	private void addBorder(CTBorder border) {
		int idx = _stylesSource.putBorder(new XSSFCellBorder(border, _theme, _stylesSource.getIndexedColors()));
		_cellXf.setBorderId(idx);
		_cellXf.setApplyBorder(true);
	}

	@Override
	public HorizontalAlignment getAlignment() {
		CTCellAlignment align = _cellXf.getAlignment();
		if ((align != null) && (align.isSetHorizontal())) {
			return HorizontalAlignment.forInt(((align.getHorizontal().intValue()) - 1));
		}
		return HorizontalAlignment.GENERAL;
	}

	@Override
	public HorizontalAlignment getAlignmentEnum() {
		return getAlignment();
	}

	@Override
	public BorderStyle getBorderBottom() {
		if (!(_cellXf.getApplyBorder()))
			return BorderStyle.NONE;

		int idx = ((int) (_cellXf.getBorderId()));
		CTBorder ct = _stylesSource.getBorderAt(idx).getCTBorder();
		STBorderStyle.Enum ptrn = (ct.isSetBottom()) ? ct.getBottom().getStyle() : null;
		if (ptrn == null) {
			return BorderStyle.NONE;
		}
		return BorderStyle.valueOf(((short) ((ptrn.intValue()) - 1)));
	}

	@Override
	public BorderStyle getBorderBottomEnum() {
		return getBorderBottom();
	}

	@Override
	public BorderStyle getBorderLeft() {
		if (!(_cellXf.getApplyBorder()))
			return BorderStyle.NONE;

		int idx = ((int) (_cellXf.getBorderId()));
		CTBorder ct = _stylesSource.getBorderAt(idx).getCTBorder();
		STBorderStyle.Enum ptrn = (ct.isSetLeft()) ? ct.getLeft().getStyle() : null;
		if (ptrn == null) {
			return BorderStyle.NONE;
		}
		return BorderStyle.valueOf(((short) ((ptrn.intValue()) - 1)));
	}

	@Override
	public BorderStyle getBorderLeftEnum() {
		return getBorderLeft();
	}

	@Override
	public BorderStyle getBorderRight() {
		if (!(_cellXf.getApplyBorder()))
			return BorderStyle.NONE;

		int idx = ((int) (_cellXf.getBorderId()));
		CTBorder ct = _stylesSource.getBorderAt(idx).getCTBorder();
		STBorderStyle.Enum ptrn = (ct.isSetRight()) ? ct.getRight().getStyle() : null;
		if (ptrn == null) {
			return BorderStyle.NONE;
		}
		return BorderStyle.valueOf(((short) ((ptrn.intValue()) - 1)));
	}

	@Override
	public BorderStyle getBorderRightEnum() {
		return getBorderRight();
	}

	@Override
	public BorderStyle getBorderTop() {
		if (!(_cellXf.getApplyBorder()))
			return BorderStyle.NONE;

		int idx = ((int) (_cellXf.getBorderId()));
		CTBorder ct = _stylesSource.getBorderAt(idx).getCTBorder();
		STBorderStyle.Enum ptrn = (ct.isSetTop()) ? ct.getTop().getStyle() : null;
		if (ptrn == null) {
			return BorderStyle.NONE;
		}
		return BorderStyle.valueOf(((short) ((ptrn.intValue()) - 1)));
	}

	@Override
	public BorderStyle getBorderTopEnum() {
		return getBorderTop();
	}

	@Override
	public short getBottomBorderColor() {
		XSSFColor clr = getBottomBorderXSSFColor();
		return clr == null ? IndexedColors.BLACK.getIndex() : clr.getIndexed();
	}

	public XSSFColor getBottomBorderXSSFColor() {
		if (!(_cellXf.getApplyBorder()))
			return null;

		int idx = ((int) (_cellXf.getBorderId()));
		XSSFCellBorder border = _stylesSource.getBorderAt(idx);
		return border.getBorderColor(BOTTOM);
	}

	@Override
	public short getDataFormat() {
		return ((short) (_cellXf.getNumFmtId()));
	}

	@Override
	public String getDataFormatString() {
		int idx = getDataFormat();
		return null;
	}

	@Override
	public short getFillBackgroundColor() {
		XSSFColor clr = getFillBackgroundXSSFColor();
		return clr == null ? IndexedColors.AUTOMATIC.getIndex() : clr.getIndexed();
	}

	@Override
	public XSSFColor getFillBackgroundColorColor() {
		return getFillBackgroundXSSFColor();
	}

	public XSSFColor getFillBackgroundXSSFColor() {
		if ((_cellXf.isSetApplyFill()) && (!(_cellXf.getApplyFill())))
			return null;

		int fillIndex = ((int) (_cellXf.getFillId()));
		XSSFCellFill fg = _stylesSource.getFillAt(fillIndex);
		XSSFColor fillBackgroundColor = fg.getFillBackgroundColor();
		if ((fillBackgroundColor != null) && ((_theme) != null)) {
			_theme.inheritFromThemeAsRequired(fillBackgroundColor);
		}
		return fillBackgroundColor;
	}

	@Override
	public short getFillForegroundColor() {
		XSSFColor clr = getFillForegroundXSSFColor();
		return clr == null ? IndexedColors.AUTOMATIC.getIndex() : clr.getIndexed();
	}

	@Override
	public XSSFColor getFillForegroundColorColor() {
		return getFillForegroundXSSFColor();
	}

	public XSSFColor getFillForegroundXSSFColor() {
		if ((_cellXf.isSetApplyFill()) && (!(_cellXf.getApplyFill())))
			return null;

		int fillIndex = ((int) (_cellXf.getFillId()));
		XSSFCellFill fg = _stylesSource.getFillAt(fillIndex);
		XSSFColor fillForegroundColor = fg.getFillForegroundColor();
		if ((fillForegroundColor != null) && ((_theme) != null)) {
			_theme.inheritFromThemeAsRequired(fillForegroundColor);
		}
		return fillForegroundColor;
	}

	@Override
	public FillPatternType getFillPattern() {
		if ((_cellXf.isSetApplyFill()) && (!(_cellXf.getApplyFill())))
			return FillPatternType.NO_FILL;

		int fillIndex = ((int) (_cellXf.getFillId()));
		XSSFCellFill fill = _stylesSource.getFillAt(fillIndex);
		STPatternType.Enum ptrn = fill.getPatternType();
		if (ptrn == null)
			return FillPatternType.NO_FILL;

		return FillPatternType.forInt(((ptrn.intValue()) - 1));
	}

	@Override
	public FillPatternType getFillPatternEnum() {
		return getFillPattern();
	}

	public XSSFFont getFont() {
		if ((_font) == null) {
			_font = _stylesSource.getFontAt(getFontId());
		}
		return _font;
	}

	@Override
	@Deprecated
	public short getFontIndex() {
		return ((short) (getFontId()));
	}

	@Override
	public int getFontIndexAsInt() {
		return getFontId();
	}

	@Override
	public boolean getHidden() {
		return ((_cellXf.isSetProtection()) && (_cellXf.getProtection().isSetHidden())) && (_cellXf.getProtection().getHidden());
	}

	@Override
	public short getIndention() {
		CTCellAlignment align = _cellXf.getAlignment();
		return ((short) (align == null ? 0 : align.getIndent()));
	}

	@Override
	public short getIndex() {
		return ((short) (this._cellXfId));
	}

	protected int getUIndex() {
		return this._cellXfId;
	}

	@Override
	public short getLeftBorderColor() {
		XSSFColor clr = getLeftBorderXSSFColor();
		return clr == null ? IndexedColors.BLACK.getIndex() : clr.getIndexed();
	}

	public XSSFColor getLeftBorderXSSFColor() {
		if (!(_cellXf.getApplyBorder()))
			return null;

		int idx = ((int) (_cellXf.getBorderId()));
		XSSFCellBorder border = _stylesSource.getBorderAt(idx);
		return border.getBorderColor(LEFT);
	}

	@Override
	public boolean getLocked() {
		return ((!(_cellXf.isSetProtection())) || (!(_cellXf.getProtection().isSetLocked()))) || (_cellXf.getProtection().getLocked());
	}

	@Override
	public boolean getQuotePrefixed() {
		return _cellXf.getQuotePrefix();
	}

	@Override
	public short getRightBorderColor() {
		XSSFColor clr = getRightBorderXSSFColor();
		return clr == null ? IndexedColors.BLACK.getIndex() : clr.getIndexed();
	}

	public XSSFColor getRightBorderXSSFColor() {
		if (!(_cellXf.getApplyBorder()))
			return null;

		int idx = ((int) (_cellXf.getBorderId()));
		XSSFCellBorder border = _stylesSource.getBorderAt(idx);
		return border.getBorderColor(RIGHT);
	}

	@Override
	public short getRotation() {
		CTCellAlignment align = _cellXf.getAlignment();
		return ((short) (align == null ? 0 : align.getTextRotation()));
	}

	@Override
	public boolean getShrinkToFit() {
		CTCellAlignment align = _cellXf.getAlignment();
		return (align != null) && (align.getShrinkToFit());
	}

	@Override
	public short getTopBorderColor() {
		XSSFColor clr = getTopBorderXSSFColor();
		return clr == null ? IndexedColors.BLACK.getIndex() : clr.getIndexed();
	}

	public XSSFColor getTopBorderXSSFColor() {
		if (!(_cellXf.getApplyBorder()))
			return null;

		int idx = ((int) (_cellXf.getBorderId()));
		XSSFCellBorder border = _stylesSource.getBorderAt(idx);
		return border.getBorderColor(TOP);
	}

	@Override
	public VerticalAlignment getVerticalAlignment() {
		CTCellAlignment align = _cellXf.getAlignment();
		if ((align != null) && (align.isSetVertical())) {
			return VerticalAlignment.forInt(((align.getVertical().intValue()) - 1));
		}
		return VerticalAlignment.BOTTOM;
	}

	@Override
	public VerticalAlignment getVerticalAlignmentEnum() {
		return getVerticalAlignment();
	}

	@Override
	public boolean getWrapText() {
		CTCellAlignment align = _cellXf.getAlignment();
		return (align != null) && (align.getWrapText());
	}

	@Override
	public void setAlignment(HorizontalAlignment align) {
		getCellAlignment().setHorizontal(align);
	}

	@Override
	public void setBorderBottom(BorderStyle border) {
		CTBorder ct = getCTBorder();
		CTBorderPr pr = (ct.isSetBottom()) ? ct.getBottom() : ct.addNewBottom();
		if (border == (BorderStyle.NONE))
			ct.unsetBottom();
		else
			pr.setStyle(STBorderStyle.Enum.forInt(((border.getCode()) + 1)));

		int idx = _stylesSource.putBorder(new XSSFCellBorder(ct, _theme, _stylesSource.getIndexedColors()));
		_cellXf.setBorderId(idx);
		_cellXf.setApplyBorder(true);
	}

	@Override
	public void setBorderLeft(BorderStyle border) {
		CTBorder ct = getCTBorder();
		CTBorderPr pr = (ct.isSetLeft()) ? ct.getLeft() : ct.addNewLeft();
		if (border == (BorderStyle.NONE))
			ct.unsetLeft();
		else
			pr.setStyle(STBorderStyle.Enum.forInt(((border.getCode()) + 1)));

		int idx = _stylesSource.putBorder(new XSSFCellBorder(ct, _theme, _stylesSource.getIndexedColors()));
		_cellXf.setBorderId(idx);
		_cellXf.setApplyBorder(true);
	}

	@Override
	public void setBorderRight(BorderStyle border) {
		CTBorder ct = getCTBorder();
		CTBorderPr pr = (ct.isSetRight()) ? ct.getRight() : ct.addNewRight();
		if (border == (BorderStyle.NONE))
			ct.unsetRight();
		else
			pr.setStyle(STBorderStyle.Enum.forInt(((border.getCode()) + 1)));

		int idx = _stylesSource.putBorder(new XSSFCellBorder(ct, _theme, _stylesSource.getIndexedColors()));
		_cellXf.setBorderId(idx);
		_cellXf.setApplyBorder(true);
	}

	@Override
	public void setBorderTop(BorderStyle border) {
		CTBorder ct = getCTBorder();
		CTBorderPr pr = (ct.isSetTop()) ? ct.getTop() : ct.addNewTop();
		if (border == (BorderStyle.NONE))
			ct.unsetTop();
		else
			pr.setStyle(STBorderStyle.Enum.forInt(((border.getCode()) + 1)));

		int idx = _stylesSource.putBorder(new XSSFCellBorder(ct, _theme, _stylesSource.getIndexedColors()));
		_cellXf.setBorderId(idx);
		_cellXf.setApplyBorder(true);
	}

	@Override
	public void setBottomBorderColor(short color) {
		XSSFColor clr = XSSFColor.from(CTColor.Factory.newInstance(), _stylesSource.getIndexedColors());
		clr.setIndexed(color);
		setBottomBorderColor(clr);
	}

	public void setBottomBorderColor(XSSFColor color) {
		CTBorder ct = getCTBorder();
		if ((color == null) && (!(ct.isSetBottom())))
			return;

		CTBorderPr pr = (ct.isSetBottom()) ? ct.getBottom() : ct.addNewBottom();
		if (color != null)
			pr.setColor(color.getCTColor());
		else
			pr.unsetColor();

		int idx = _stylesSource.putBorder(new XSSFCellBorder(ct, _theme, _stylesSource.getIndexedColors()));
		_cellXf.setBorderId(idx);
		_cellXf.setApplyBorder(true);
	}

	@Override
	public void setDataFormat(short fmt) {
		setDataFormat((fmt & 65535));
	}

	public void setDataFormat(int fmt) {
		_cellXf.setApplyNumberFormat(true);
		_cellXf.setNumFmtId(fmt);
	}

	public void setFillBackgroundColor(XSSFColor color) {
		CTFill ct = getCTFill();
		CTPatternFill ptrn = ct.getPatternFill();
		if (color == null) {
			if ((ptrn != null) && (ptrn.isSetBgColor()))
				ptrn.unsetBgColor();

		}else {
			if (ptrn == null)
				ptrn = ct.addNewPatternFill();

			ptrn.setBgColor(color.getCTColor());
		}
		addFill(ct);
	}

	@Override
	public void setFillBackgroundColor(short bg) {
		XSSFColor clr = XSSFColor.from(CTColor.Factory.newInstance(), _stylesSource.getIndexedColors());
		clr.setIndexed(bg);
		setFillBackgroundColor(clr);
	}

	public void setFillForegroundColor(XSSFColor color) {
		CTFill ct = getCTFill();
		CTPatternFill ptrn = ct.getPatternFill();
		if (color == null) {
			if ((ptrn != null) && (ptrn.isSetFgColor()))
				ptrn.unsetFgColor();

		}else {
			if (ptrn == null)
				ptrn = ct.addNewPatternFill();

			ptrn.setFgColor(color.getCTColor());
		}
		addFill(ct);
	}

	@Override
	public void setFillForegroundColor(short fg) {
		XSSFColor clr = XSSFColor.from(CTColor.Factory.newInstance(), _stylesSource.getIndexedColors());
		clr.setIndexed(fg);
		setFillForegroundColor(clr);
	}

	private CTFill getCTFill() {
		CTFill ct;
		if ((!(_cellXf.isSetApplyFill())) || (_cellXf.getApplyFill())) {
			int fillIndex = ((int) (_cellXf.getFillId()));
			XSSFCellFill cf = _stylesSource.getFillAt(fillIndex);
			ct = ((CTFill) (cf.getCTFill().copy()));
		}else {
			ct = CTFill.Factory.newInstance();
		}
		return ct;
	}

	public void setReadingOrder(ReadingOrder order) {
		getCellAlignment().setReadingOrder(order);
	}

	public ReadingOrder getReadingOrder() {
		return getCellAlignment().getReadingOrder();
	}

	private CTBorder getCTBorder() {
		CTBorder ct;
		if (_cellXf.getApplyBorder()) {
			int idx = ((int) (_cellXf.getBorderId()));
			XSSFCellBorder cf = _stylesSource.getBorderAt(idx);
			ct = ((CTBorder) (cf.getCTBorder().copy()));
		}else {
			ct = CTBorder.Factory.newInstance();
		}
		return ct;
	}

	@Override
	public void setFillPattern(FillPatternType pattern) {
		CTFill ct = getCTFill();
		CTPatternFill ctptrn = (ct.isSetPatternFill()) ? ct.getPatternFill() : ct.addNewPatternFill();
		if ((pattern == (FillPatternType.NO_FILL)) && (ctptrn.isSetPatternType())) {
			ctptrn.unsetPatternType();
		}else {
			ctptrn.setPatternType(STPatternType.Enum.forInt(((pattern.getCode()) + 1)));
		}
		addFill(ct);
	}

	@Override
	public void setFont(Font font) {
		if (font != null) {
			long index = font.getIndexAsInt();
			this._cellXf.setFontId(index);
			this._cellXf.setApplyFont(true);
		}else {
			this._cellXf.setApplyFont(false);
		}
	}

	@Override
	public void setHidden(boolean hidden) {
		if (!(_cellXf.isSetProtection())) {
			_cellXf.addNewProtection();
		}
		_cellXf.getProtection().setHidden(hidden);
	}

	@Override
	public void setIndention(short indent) {
		getCellAlignment().setIndent(indent);
	}

	@Override
	public void setLeftBorderColor(short color) {
		XSSFColor clr = XSSFColor.from(CTColor.Factory.newInstance(), _stylesSource.getIndexedColors());
		clr.setIndexed(color);
		setLeftBorderColor(clr);
	}

	public void setLeftBorderColor(XSSFColor color) {
		CTBorder ct = getCTBorder();
		if ((color == null) && (!(ct.isSetLeft())))
			return;

		CTBorderPr pr = (ct.isSetLeft()) ? ct.getLeft() : ct.addNewLeft();
		if (color != null)
			pr.setColor(color.getCTColor());
		else
			pr.unsetColor();

		int idx = _stylesSource.putBorder(new XSSFCellBorder(ct, _theme, _stylesSource.getIndexedColors()));
		_cellXf.setBorderId(idx);
		_cellXf.setApplyBorder(true);
	}

	@Override
	public void setLocked(boolean locked) {
		if (!(_cellXf.isSetProtection())) {
			_cellXf.addNewProtection();
		}
		_cellXf.getProtection().setLocked(locked);
	}

	@Override
	public void setQuotePrefixed(boolean quotePrefix) {
		_cellXf.setQuotePrefix(quotePrefix);
	}

	@Override
	public void setRightBorderColor(short color) {
		XSSFColor clr = XSSFColor.from(CTColor.Factory.newInstance(), _stylesSource.getIndexedColors());
		clr.setIndexed(color);
		setRightBorderColor(clr);
	}

	public void setRightBorderColor(XSSFColor color) {
		CTBorder ct = getCTBorder();
		if ((color == null) && (!(ct.isSetRight())))
			return;

		CTBorderPr pr = (ct.isSetRight()) ? ct.getRight() : ct.addNewRight();
		if (color != null)
			pr.setColor(color.getCTColor());
		else
			pr.unsetColor();

		int idx = _stylesSource.putBorder(new XSSFCellBorder(ct, _theme, _stylesSource.getIndexedColors()));
		_cellXf.setBorderId(idx);
		_cellXf.setApplyBorder(true);
	}

	@Override
	public void setRotation(short rotation) {
		getCellAlignment().setTextRotation(rotation);
	}

	@Override
	public void setTopBorderColor(short color) {
		XSSFColor clr = XSSFColor.from(CTColor.Factory.newInstance(), _stylesSource.getIndexedColors());
		clr.setIndexed(color);
		setTopBorderColor(clr);
	}

	public void setTopBorderColor(XSSFColor color) {
		CTBorder ct = getCTBorder();
		if ((color == null) && (!(ct.isSetTop())))
			return;

		CTBorderPr pr = (ct.isSetTop()) ? ct.getTop() : ct.addNewTop();
		if (color != null)
			pr.setColor(color.getCTColor());
		else
			pr.unsetColor();

		int idx = _stylesSource.putBorder(new XSSFCellBorder(ct, _theme, _stylesSource.getIndexedColors()));
		_cellXf.setBorderId(idx);
		_cellXf.setApplyBorder(true);
	}

	public void setVerticalAlignment(VerticalAlignment align) {
		getCellAlignment().setVertical(align);
	}

	@Override
	public void setWrapText(boolean wrapped) {
		getCellAlignment().setWrapText(wrapped);
	}

	public XSSFColor getBorderColor(XSSFCellBorder.BorderSide side) {
		switch (side) {
			case BOTTOM :
				return getBottomBorderXSSFColor();
			case RIGHT :
				return getRightBorderXSSFColor();
			case TOP :
				return getTopBorderXSSFColor();
			case LEFT :
				return getLeftBorderXSSFColor();
			default :
				throw new IllegalArgumentException(("Unknown border: " + side));
		}
	}

	public void setBorderColor(XSSFCellBorder.BorderSide side, XSSFColor color) {
		switch (side) {
			case BOTTOM :
				setBottomBorderColor(color);
				break;
			case RIGHT :
				setRightBorderColor(color);
				break;
			case TOP :
				setTopBorderColor(color);
				break;
			case LEFT :
				setLeftBorderColor(color);
				break;
		}
	}

	@Override
	public void setShrinkToFit(boolean shrinkToFit) {
		getCellAlignment().setShrinkToFit(shrinkToFit);
	}

	private int getFontId() {
		if (_cellXf.isSetFontId()) {
			return ((int) (_cellXf.getFontId()));
		}
		return ((int) (_cellStyleXf.getFontId()));
	}

	protected XSSFCellAlignment getCellAlignment() {
		if ((this._cellAlignment) == null) {
			this._cellAlignment = new XSSFCellAlignment(getCTCellAlignment());
		}
		return this._cellAlignment;
	}

	private CTCellAlignment getCTCellAlignment() {
		if ((_cellXf.getAlignment()) == null) {
			_cellXf.setAlignment(newInstance());
		}
		return _cellXf.getAlignment();
	}

	@Override
	public int hashCode() {
		return _cellXf.toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if ((o == null) || (!(o instanceof XSSFCellStyle)))
			return false;

		XSSFCellStyle cf = ((XSSFCellStyle) (o));
		return _cellXf.toString().equals(cf.getCoreXf().toString());
	}

	@Override
	public Object clone() {
		CTXf xf = ((CTXf) (_cellXf.copy()));
		int xfSize = _stylesSource._getStyleXfsSize();
		int indexXf = _stylesSource.putCellXf(xf);
		return new XSSFCellStyle((indexXf - 1), (xfSize - 1), _stylesSource, _theme);
	}
}

