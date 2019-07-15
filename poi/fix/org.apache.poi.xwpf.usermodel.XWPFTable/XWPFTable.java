

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.BodyType;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ISDTContents;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDecimalNumber;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblCellMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPrBase;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.Enum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHexColor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;


@SuppressWarnings("WeakerAccess")
public class XWPFTable implements IBodyElement , ISDTContents {
	public static final String REGEX_PERCENTAGE = "[0-9]+(\\.[0-9]+)?%";

	public static final String DEFAULT_PERCENTAGE_WIDTH = "100%";

	static final String NS_OOXML_WP_MAIN = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

	public static final String REGEX_WIDTH_VALUE = "auto|[0-9]+|" + (XWPFTable.REGEX_PERCENTAGE);

	public enum XWPFBorderType {

		NIL,
		NONE,
		SINGLE,
		THICK,
		DOUBLE,
		DOTTED,
		DASHED,
		DOT_DASH,
		DOT_DOT_DASH,
		TRIPLE,
		THIN_THICK_SMALL_GAP,
		THICK_THIN_SMALL_GAP,
		THIN_THICK_THIN_SMALL_GAP,
		THIN_THICK_MEDIUM_GAP,
		THICK_THIN_MEDIUM_GAP,
		THIN_THICK_THIN_MEDIUM_GAP,
		THIN_THICK_LARGE_GAP,
		THICK_THIN_LARGE_GAP,
		THIN_THICK_THIN_LARGE_GAP,
		WAVE,
		DOUBLE_WAVE,
		DASH_SMALL_GAP,
		DASH_DOT_STROKED,
		THREE_D_EMBOSS,
		THREE_D_ENGRAVE,
		OUTSET,
		INSET;}

	private enum Border {

		INSIDE_V,
		INSIDE_H,
		LEFT,
		TOP,
		BOTTOM,
		RIGHT;}

	private static final EnumMap<XWPFTable.XWPFBorderType, STBorder.Enum> xwpfBorderTypeMap;

	private static final HashMap<Integer, XWPFTable.XWPFBorderType> stBorderTypeMap;

	static {
		xwpfBorderTypeMap = new EnumMap<>(XWPFTable.XWPFBorderType.class);
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.NIL, STBorder.Enum.forInt(STBorder.INT_NIL));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.NONE, STBorder.Enum.forInt(STBorder.INT_NONE));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.SINGLE, STBorder.Enum.forInt(STBorder.INT_SINGLE));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THICK, STBorder.Enum.forInt(STBorder.INT_THICK));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.DOUBLE, STBorder.Enum.forInt(STBorder.INT_DOUBLE));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.DOTTED, STBorder.Enum.forInt(STBorder.INT_DOTTED));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.DASHED, STBorder.Enum.forInt(STBorder.INT_DASHED));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.DOT_DASH, STBorder.Enum.forInt(STBorder.INT_DOT_DASH));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.DOT_DOT_DASH, STBorder.Enum.forInt(STBorder.INT_DOT_DOT_DASH));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.TRIPLE, STBorder.Enum.forInt(STBorder.INT_TRIPLE));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THIN_THICK_SMALL_GAP, STBorder.Enum.forInt(STBorder.INT_THIN_THICK_SMALL_GAP));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THICK_THIN_SMALL_GAP, STBorder.Enum.forInt(STBorder.INT_THICK_THIN_SMALL_GAP));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THIN_THICK_THIN_SMALL_GAP, STBorder.Enum.forInt(STBorder.INT_THIN_THICK_THIN_SMALL_GAP));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THIN_THICK_MEDIUM_GAP, STBorder.Enum.forInt(STBorder.INT_THIN_THICK_MEDIUM_GAP));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THICK_THIN_MEDIUM_GAP, STBorder.Enum.forInt(STBorder.INT_THICK_THIN_MEDIUM_GAP));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THIN_THICK_THIN_MEDIUM_GAP, STBorder.Enum.forInt(STBorder.INT_THIN_THICK_THIN_MEDIUM_GAP));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THIN_THICK_LARGE_GAP, STBorder.Enum.forInt(STBorder.INT_THIN_THICK_LARGE_GAP));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THICK_THIN_LARGE_GAP, STBorder.Enum.forInt(STBorder.INT_THICK_THIN_LARGE_GAP));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THIN_THICK_THIN_LARGE_GAP, STBorder.Enum.forInt(STBorder.INT_THIN_THICK_THIN_LARGE_GAP));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.WAVE, STBorder.Enum.forInt(STBorder.INT_WAVE));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.DOUBLE_WAVE, STBorder.Enum.forInt(STBorder.INT_DOUBLE_WAVE));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.DASH_SMALL_GAP, STBorder.Enum.forInt(STBorder.INT_DASH_SMALL_GAP));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.DASH_DOT_STROKED, STBorder.Enum.forInt(STBorder.INT_DASH_DOT_STROKED));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THREE_D_EMBOSS, STBorder.Enum.forInt(STBorder.INT_THREE_D_EMBOSS));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.THREE_D_ENGRAVE, STBorder.Enum.forInt(STBorder.INT_THREE_D_ENGRAVE));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.OUTSET, STBorder.Enum.forInt(STBorder.INT_OUTSET));
		XWPFTable.xwpfBorderTypeMap.put(XWPFTable.XWPFBorderType.INSET, STBorder.Enum.forInt(STBorder.INT_INSET));
		stBorderTypeMap = new HashMap<>();
		XWPFTable.stBorderTypeMap.put(STBorder.INT_NIL, XWPFTable.XWPFBorderType.NIL);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_NONE, XWPFTable.XWPFBorderType.NONE);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_SINGLE, XWPFTable.XWPFBorderType.SINGLE);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THICK, XWPFTable.XWPFBorderType.THICK);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_DOUBLE, XWPFTable.XWPFBorderType.DOUBLE);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_DOTTED, XWPFTable.XWPFBorderType.DOTTED);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_DASHED, XWPFTable.XWPFBorderType.DASHED);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_DOT_DASH, XWPFTable.XWPFBorderType.DOT_DASH);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_DOT_DOT_DASH, XWPFTable.XWPFBorderType.DOT_DOT_DASH);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_TRIPLE, XWPFTable.XWPFBorderType.TRIPLE);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THIN_THICK_SMALL_GAP, XWPFTable.XWPFBorderType.THIN_THICK_SMALL_GAP);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THICK_THIN_SMALL_GAP, XWPFTable.XWPFBorderType.THICK_THIN_SMALL_GAP);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THIN_THICK_THIN_SMALL_GAP, XWPFTable.XWPFBorderType.THIN_THICK_THIN_SMALL_GAP);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THIN_THICK_MEDIUM_GAP, XWPFTable.XWPFBorderType.THIN_THICK_MEDIUM_GAP);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THICK_THIN_MEDIUM_GAP, XWPFTable.XWPFBorderType.THICK_THIN_MEDIUM_GAP);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THIN_THICK_THIN_MEDIUM_GAP, XWPFTable.XWPFBorderType.THIN_THICK_THIN_MEDIUM_GAP);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THIN_THICK_LARGE_GAP, XWPFTable.XWPFBorderType.THIN_THICK_LARGE_GAP);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THICK_THIN_LARGE_GAP, XWPFTable.XWPFBorderType.THICK_THIN_LARGE_GAP);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THIN_THICK_THIN_LARGE_GAP, XWPFTable.XWPFBorderType.THIN_THICK_THIN_LARGE_GAP);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_WAVE, XWPFTable.XWPFBorderType.WAVE);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_DOUBLE_WAVE, XWPFTable.XWPFBorderType.DOUBLE_WAVE);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_DASH_SMALL_GAP, XWPFTable.XWPFBorderType.DASH_SMALL_GAP);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_DASH_DOT_STROKED, XWPFTable.XWPFBorderType.DASH_DOT_STROKED);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THREE_D_EMBOSS, XWPFTable.XWPFBorderType.THREE_D_EMBOSS);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_THREE_D_ENGRAVE, XWPFTable.XWPFBorderType.THREE_D_ENGRAVE);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_OUTSET, XWPFTable.XWPFBorderType.OUTSET);
		XWPFTable.stBorderTypeMap.put(STBorder.INT_INSET, XWPFTable.XWPFBorderType.INSET);
	}

	protected StringBuilder text = new StringBuilder(64);

	protected final List<XWPFTableRow> tableRows = new ArrayList<>();

	protected IBody part;

	private CTTbl ctTbl;

	public XWPFTable(CTTbl table, IBody part, int row, int col) {
		this(table, part);
		for (int i = 0; i < row; i++) {
			XWPFTableRow tabRow = ((getRow(i)) == null) ? createRow() : getRow(i);
			for (int k = 0; k < col; k++) {
				if ((tabRow.getCell(k)) == null) {
					tabRow.createCell();
				}
			}
		}
	}

	public XWPFTable(CTTbl table, IBody part) {
		this.part = part;
		this.ctTbl = table;
		if ((table.sizeOfTrArray()) == 0) {
			createEmptyTable(table);
		}
		for (CTRow row : table.getTrList()) {
			StringBuilder rowText = new StringBuilder();
			for (CTTc cell : row.getTcList()) {
				for (CTP ctp : cell.getPList()) {
					XWPFParagraph p = new XWPFParagraph(ctp, part);
					if ((rowText.length()) > 0) {
						rowText.append('\t');
					}
					rowText.append(p.getText());
				}
			}
			if ((rowText.length()) > 0) {
				this.text.append(rowText);
				this.text.append('\n');
			}
		}
	}

	private void createEmptyTable(CTTbl table) {
		table.addNewTr().addNewTc().addNewP();
		CTTblPr tblpro = table.addNewTblPr();
		tblpro.addNewTblW().setW(new BigInteger("0"));
		tblpro.getTblW().setType(STTblWidth.AUTO);
		CTTblBorders borders = tblpro.addNewTblBorders();
		borders.addNewBottom().setVal(STBorder.SINGLE);
		borders.addNewInsideH().setVal(STBorder.SINGLE);
		borders.addNewInsideV().setVal(STBorder.SINGLE);
		borders.addNewLeft().setVal(STBorder.SINGLE);
		borders.addNewRight().setVal(STBorder.SINGLE);
		borders.addNewTop().setVal(STBorder.SINGLE);
	}

	@org.apache.poi.util.Internal
	public CTTbl getCTTbl() {
		return ctTbl;
	}

	public String getText() {
		return text.toString();
	}

	@Deprecated
	@org.apache.poi.util.Removal
	@org.apache.poi.util.NotImplemented
	public void addNewRowBetween(int start, int end) {
		throw new UnsupportedOperationException("XWPFTable#addNewRowBetween(int, int) not implemented");
	}

	public void addNewCol() {
		if ((ctTbl.sizeOfTrArray()) == 0) {
			createRow();
		}
		for (int i = 0; i < (ctTbl.sizeOfTrArray()); i++) {
		}
	}

	public XWPFTableRow createRow() {
		int sizeCol = ((ctTbl.sizeOfTrArray()) > 0) ? ctTbl.getTrArray(0).sizeOfTcArray() : 0;
		return null;
	}

	public XWPFTableRow getRow(int pos) {
		if ((pos >= 0) && (pos < (ctTbl.sizeOfTrArray()))) {
			return getRows().get(pos);
		}
		return null;
	}

	public int getWidth() {
		CTTblPr tblPr = getTblPr();
		return tblPr.isSetTblW() ? tblPr.getTblW().getW().intValue() : -1;
	}

	public void setWidth(int width) {
		CTTblPr tblPr = getTblPr();
		CTTblWidth tblWidth = (tblPr.isSetTblW()) ? tblPr.getTblW() : tblPr.addNewTblW();
		tblWidth.setW(new BigInteger(Integer.toString(width)));
		tblWidth.setType(STTblWidth.DXA);
	}

	public int getNumberOfRows() {
		return ctTbl.sizeOfTrArray();
	}

	private CTTblPr getTblPr() {
		return getTblPr(true);
	}

	private CTTblPr getTblPr(boolean force) {
		return (ctTbl.getTblPr()) != null ? ctTbl.getTblPr() : force ? ctTbl.addNewTblPr() : null;
	}

	private CTTblBorders getTblBorders(boolean force) {
		CTTblPr tblPr = getTblPr(force);
		return tblPr == null ? null : tblPr.isSetTblBorders() ? tblPr.getTblBorders() : force ? tblPr.addNewTblBorders() : null;
	}

	private CTBorder getTblBorder(boolean force, XWPFTable.Border border) {
		final Function<CTTblBorders, Boolean> isSet;
		final Function<CTTblBorders, CTBorder> get;
		final Function<CTTblBorders, CTBorder> addNew;
		switch (border) {
			case INSIDE_V :
				isSet = CTTblBorders::isSetInsideV;
				get = CTTblBorders::getInsideV;
				addNew = CTTblBorders::addNewInsideV;
				break;
			case INSIDE_H :
				isSet = CTTblBorders::isSetInsideH;
				get = CTTblBorders::getInsideH;
				addNew = CTTblBorders::addNewInsideH;
				break;
			case LEFT :
				isSet = CTTblBorders::isSetLeft;
				get = CTTblBorders::getLeft;
				addNew = CTTblBorders::addNewLeft;
				break;
			case TOP :
				isSet = CTTblBorders::isSetTop;
				get = CTTblBorders::getTop;
				addNew = CTTblBorders::addNewTop;
				break;
			case RIGHT :
				isSet = CTTblBorders::isSetRight;
				get = CTTblBorders::getRight;
				addNew = CTTblBorders::addNewRight;
				break;
			case BOTTOM :
				isSet = CTTblBorders::isSetBottom;
				get = CTTblBorders::getBottom;
				addNew = CTTblBorders::addNewBottom;
				break;
			default :
				return null;
		}
		CTTblBorders ctb = getTblBorders(force);
		return ctb == null ? null : isSet.apply(ctb) ? get.apply(ctb) : force ? addNew.apply(ctb) : null;
	}

	public TableRowAlign getTableAlignment() {
		CTTblPr tPr = getTblPr(false);
		return tPr == null ? null : tPr.isSetJc() ? TableRowAlign.valueOf(tPr.getJc().getVal().intValue()) : null;
	}

	public void setTableAlignment(TableRowAlign tra) {
		CTTblPr tPr = getTblPr(true);
		CTJc jc = (tPr.isSetJc()) ? tPr.getJc() : tPr.addNewJc();
		jc.setVal(STJc.Enum.forInt(tra.getValue()));
	}

	public void removeTableAlignment() {
		CTTblPr tPr = getTblPr(false);
		if ((tPr != null) && (tPr.isSetJc())) {
			tPr.unsetJc();
		}
	}

	private void addColumn(XWPFTableRow tabRow, int sizeCol) {
		if (sizeCol > 0) {
			for (int i = 0; i < sizeCol; i++) {
				tabRow.createCell();
			}
		}
	}

	public String getStyleID() {
		String styleId = null;
		CTTblPr tblPr = ctTbl.getTblPr();
		if (tblPr != null) {
			CTString styleStr = tblPr.getTblStyle();
			if (styleStr != null) {
				styleId = styleStr.getVal();
			}
		}
		return styleId;
	}

	public void setStyleID(String styleName) {
		CTTblPr tblPr = getTblPr();
		CTString styleStr = tblPr.getTblStyle();
		if (styleStr == null) {
			styleStr = tblPr.addNewTblStyle();
		}
		styleStr.setVal(styleName);
	}

	public XWPFTable.XWPFBorderType getInsideHBorderType() {
		return getBorderType(XWPFTable.Border.INSIDE_H);
	}

	public int getInsideHBorderSize() {
		return getBorderSize(XWPFTable.Border.INSIDE_H);
	}

	public int getInsideHBorderSpace() {
		return getBorderSpace(XWPFTable.Border.INSIDE_H);
	}

	public String getInsideHBorderColor() {
		return getBorderColor(XWPFTable.Border.INSIDE_H);
	}

	public XWPFTable.XWPFBorderType getInsideVBorderType() {
		return getBorderType(XWPFTable.Border.INSIDE_V);
	}

	public int getInsideVBorderSize() {
		return getBorderSize(XWPFTable.Border.INSIDE_V);
	}

	public int getInsideVBorderSpace() {
		return getBorderSpace(XWPFTable.Border.INSIDE_V);
	}

	public String getInsideVBorderColor() {
		return getBorderColor(XWPFTable.Border.INSIDE_V);
	}

	public XWPFTable.XWPFBorderType getTopBorderType() {
		return getBorderType(XWPFTable.Border.TOP);
	}

	public int getTopBorderSize() {
		return getBorderSize(XWPFTable.Border.TOP);
	}

	public int getTopBorderSpace() {
		return getBorderSpace(XWPFTable.Border.TOP);
	}

	public String getTopBorderColor() {
		return getBorderColor(XWPFTable.Border.TOP);
	}

	public XWPFTable.XWPFBorderType getBottomBorderType() {
		return getBorderType(XWPFTable.Border.BOTTOM);
	}

	public int getBottomBorderSize() {
		return getBorderSize(XWPFTable.Border.BOTTOM);
	}

	public int getBottomBorderSpace() {
		return getBorderSpace(XWPFTable.Border.BOTTOM);
	}

	public String getBottomBorderColor() {
		return getBorderColor(XWPFTable.Border.BOTTOM);
	}

	public XWPFTable.XWPFBorderType getLeftBorderType() {
		return getBorderType(XWPFTable.Border.LEFT);
	}

	public int getLeftBorderSize() {
		return getBorderSize(XWPFTable.Border.LEFT);
	}

	public int getLeftBorderSpace() {
		return getBorderSpace(XWPFTable.Border.LEFT);
	}

	public String getLeftBorderColor() {
		return getBorderColor(XWPFTable.Border.LEFT);
	}

	public XWPFTable.XWPFBorderType getRightBorderType() {
		return getBorderType(XWPFTable.Border.RIGHT);
	}

	public int getRightBorderSize() {
		return getBorderSize(XWPFTable.Border.RIGHT);
	}

	public int getRightBorderSpace() {
		return getBorderSpace(XWPFTable.Border.RIGHT);
	}

	public String getRightBorderColor() {
		return getBorderColor(XWPFTable.Border.RIGHT);
	}

	private XWPFTable.XWPFBorderType getBorderType(XWPFTable.Border border) {
		final CTBorder b = getTblBorder(false, border);
		return b != null ? XWPFTable.stBorderTypeMap.get(b.getVal().intValue()) : null;
	}

	private int getBorderSize(XWPFTable.Border border) {
		final CTBorder b = getTblBorder(false, border);
		return b != null ? b.isSetSz() ? b.getSz().intValue() : -1 : -1;
	}

	private int getBorderSpace(XWPFTable.Border border) {
		final CTBorder b = getTblBorder(false, border);
		return b != null ? b.isSetSpace() ? b.getSpace().intValue() : -1 : -1;
	}

	private String getBorderColor(XWPFTable.Border border) {
		final CTBorder b = getTblBorder(false, border);
		return b != null ? b.isSetColor() ? b.xgetColor().getStringValue() : null : null;
	}

	public int getRowBandSize() {
		int size = 0;
		CTTblPr tblPr = getTblPr();
		if (tblPr.isSetTblStyleRowBandSize()) {
			CTDecimalNumber rowSize = tblPr.getTblStyleRowBandSize();
			size = rowSize.getVal().intValue();
		}
		return size;
	}

	public void setRowBandSize(int size) {
		CTTblPr tblPr = getTblPr();
		CTDecimalNumber rowSize = (tblPr.isSetTblStyleRowBandSize()) ? tblPr.getTblStyleRowBandSize() : tblPr.addNewTblStyleRowBandSize();
		rowSize.setVal(BigInteger.valueOf(size));
	}

	public int getColBandSize() {
		int size = 0;
		CTTblPr tblPr = getTblPr();
		if (tblPr.isSetTblStyleColBandSize()) {
			CTDecimalNumber colSize = tblPr.getTblStyleColBandSize();
			size = colSize.getVal().intValue();
		}
		return size;
	}

	public void setColBandSize(int size) {
		CTTblPr tblPr = getTblPr();
		CTDecimalNumber colSize = (tblPr.isSetTblStyleColBandSize()) ? tblPr.getTblStyleColBandSize() : tblPr.addNewTblStyleColBandSize();
		colSize.setVal(BigInteger.valueOf(size));
	}

	public void setInsideHBorder(XWPFTable.XWPFBorderType type, int size, int space, String rgbColor) {
		setBorder(XWPFTable.Border.INSIDE_H, type, size, space, rgbColor);
	}

	public void setInsideVBorder(XWPFTable.XWPFBorderType type, int size, int space, String rgbColor) {
		setBorder(XWPFTable.Border.INSIDE_V, type, size, space, rgbColor);
	}

	public void setTopBorder(XWPFTable.XWPFBorderType type, int size, int space, String rgbColor) {
		setBorder(XWPFTable.Border.TOP, type, size, space, rgbColor);
	}

	public void setBottomBorder(XWPFTable.XWPFBorderType type, int size, int space, String rgbColor) {
		setBorder(XWPFTable.Border.BOTTOM, type, size, space, rgbColor);
	}

	public void setLeftBorder(XWPFTable.XWPFBorderType type, int size, int space, String rgbColor) {
		setBorder(XWPFTable.Border.LEFT, type, size, space, rgbColor);
	}

	public void setRightBorder(XWPFTable.XWPFBorderType type, int size, int space, String rgbColor) {
		setBorder(XWPFTable.Border.RIGHT, type, size, space, rgbColor);
	}

	private void setBorder(XWPFTable.Border border, XWPFTable.XWPFBorderType type, int size, int space, String rgbColor) {
		final CTBorder b = getTblBorder(true, border);
		assert b != null;
		b.setVal(XWPFTable.xwpfBorderTypeMap.get(type));
		b.setSz(BigInteger.valueOf(size));
		b.setSpace(BigInteger.valueOf(space));
		b.setColor(rgbColor);
	}

	public void removeInsideHBorder() {
		removeBorder(XWPFTable.Border.INSIDE_H);
	}

	public void removeInsideVBorder() {
		removeBorder(XWPFTable.Border.INSIDE_V);
	}

	public void removeTopBorder() {
		removeBorder(XWPFTable.Border.TOP);
	}

	public void removeBottomBorder() {
		removeBorder(XWPFTable.Border.BOTTOM);
	}

	public void removeLeftBorder() {
		removeBorder(XWPFTable.Border.LEFT);
	}

	public void removeRightBorder() {
		removeBorder(XWPFTable.Border.RIGHT);
	}

	public void removeBorders() {
		final CTTblPr pr = getTblPr(false);
		if ((pr != null) && (pr.isSetTblBorders())) {
			pr.unsetTblBorders();
		}
	}

	private void removeBorder(final XWPFTable.Border border) {
		final Function<CTTblBorders, Boolean> isSet;
		final Consumer<CTTblBorders> unSet;
		switch (border) {
			case INSIDE_H :
				isSet = CTTblBorders::isSetInsideH;
				unSet = CTTblBorders::unsetInsideH;
				break;
			case INSIDE_V :
				isSet = CTTblBorders::isSetInsideV;
				unSet = CTTblBorders::unsetInsideV;
				break;
			case LEFT :
				isSet = CTTblBorders::isSetLeft;
				unSet = CTTblBorders::unsetLeft;
				break;
			case TOP :
				isSet = CTTblBorders::isSetTop;
				unSet = CTTblBorders::unsetTop;
				break;
			case RIGHT :
				isSet = CTTblBorders::isSetRight;
				unSet = CTTblBorders::unsetRight;
				break;
			case BOTTOM :
				isSet = CTTblBorders::isSetBottom;
				unSet = CTTblBorders::unsetBottom;
				break;
			default :
				return;
		}
		final CTTblBorders tbl = getTblBorders(false);
		if ((tbl != null) && (isSet.apply(tbl))) {
			unSet.accept(tbl);
			cleanupTblBorders();
		}
	}

	private void cleanupTblBorders() {
		final CTTblPr pr = getTblPr(false);
		if ((pr != null) && (pr.isSetTblBorders())) {
			final CTTblBorders b = pr.getTblBorders();
			if (!((((((b.isSetInsideH()) || (b.isSetInsideV())) || (b.isSetTop())) || (b.isSetBottom())) || (b.isSetLeft())) || (b.isSetRight()))) {
				pr.unsetTblBorders();
			}
		}
	}

	public int getCellMarginTop() {
		return getCellMargin(CTTblCellMar::getTop);
	}

	public int getCellMarginLeft() {
		return getCellMargin(CTTblCellMar::getLeft);
	}

	public int getCellMarginBottom() {
		return getCellMargin(CTTblCellMar::getBottom);
	}

	public int getCellMarginRight() {
		return getCellMargin(CTTblCellMar::getRight);
	}

	private int getCellMargin(Function<CTTblCellMar, CTTblWidth> margin) {
		CTTblPr tblPr = getTblPr();
		CTTblCellMar tcm = tblPr.getTblCellMar();
		if (tcm != null) {
			CTTblWidth tw = margin.apply(tcm);
			if (tw != null) {
				return tw.getW().intValue();
			}
		}
		return 0;
	}

	public void setCellMargins(int top, int left, int bottom, int right) {
		CTTblPr tblPr = getTblPr();
		CTTblCellMar tcm = (tblPr.isSetTblCellMar()) ? tblPr.getTblCellMar() : tblPr.addNewTblCellMar();
		setCellMargin(tcm, CTTblCellMar::isSetTop, CTTblCellMar::getTop, CTTblCellMar::addNewTop, CTTblCellMar::unsetTop, top);
		setCellMargin(tcm, CTTblCellMar::isSetLeft, CTTblCellMar::getLeft, CTTblCellMar::addNewLeft, CTTblCellMar::unsetLeft, left);
		setCellMargin(tcm, CTTblCellMar::isSetBottom, CTTblCellMar::getBottom, CTTblCellMar::addNewBottom, CTTblCellMar::unsetBottom, bottom);
		setCellMargin(tcm, CTTblCellMar::isSetRight, CTTblCellMar::getRight, CTTblCellMar::addNewRight, CTTblCellMar::unsetRight, right);
	}

	private void setCellMargin(CTTblCellMar tcm, Function<CTTblCellMar, Boolean> isSet, Function<CTTblCellMar, CTTblWidth> get, Function<CTTblCellMar, CTTblWidth> addNew, Consumer<CTTblCellMar> unSet, int margin) {
		if (margin == 0) {
			if (isSet.apply(tcm)) {
				unSet.accept(tcm);
			}
		}else {
			CTTblWidth tw = (isSet.apply(tcm) ? get : addNew).apply(tcm);
			tw.setType(STTblWidth.DXA);
			tw.setW(BigInteger.valueOf(margin));
		}
	}

	public void addRow(XWPFTableRow row) {
		ctTbl.addNewTr();
		ctTbl.setTrArray(((getNumberOfRows()) - 1), row.getCtRow());
		tableRows.add(row);
	}

	public boolean addRow(XWPFTableRow row, int pos) {
		if ((pos >= 0) && (pos <= (tableRows.size()))) {
			ctTbl.insertNewTr(pos);
			ctTbl.setTrArray(pos, row.getCtRow());
			tableRows.add(pos, row);
			return true;
		}
		return false;
	}

	public XWPFTableRow insertNewTableRow(int pos) {
		if ((pos >= 0) && (pos <= (tableRows.size()))) {
			CTRow row = ctTbl.insertNewTr(pos);
		}
		return null;
	}

	public boolean removeRow(int pos) throws IndexOutOfBoundsException {
		if ((pos >= 0) && (pos < (tableRows.size()))) {
			if ((ctTbl.sizeOfTrArray()) > 0) {
				ctTbl.removeTr(pos);
			}
			tableRows.remove(pos);
			return true;
		}
		return false;
	}

	public List<XWPFTableRow> getRows() {
		return Collections.unmodifiableList(tableRows);
	}

	@Override
	public BodyElementType getElementType() {
		return BodyElementType.TABLE;
	}

	@Override
	public IBody getBody() {
		return part;
	}

	@Override
	public POIXMLDocumentPart getPart() {
		if ((part) != null) {
			return part.getPart();
		}
		return null;
	}

	@Override
	public BodyType getPartType() {
		return part.getPartType();
	}

	public XWPFTableRow getRow(CTRow row) {
		for (int i = 0; i < (getRows().size()); i++) {
			if ((getRows().get(i).getCtRow()) == row) {
				return getRow(i);
			}
		}
		return null;
	}

	public double getWidthDecimal() {
		return XWPFTable.getWidthDecimal(getTblPr().getTblW());
	}

	protected static double getWidthDecimal(CTTblWidth ctWidth) {
		double result = 0.0;
		STTblWidth.Enum typeValue = ctWidth.getType();
		if (((typeValue == (STTblWidth.DXA)) || (typeValue == (STTblWidth.AUTO))) || (typeValue == (STTblWidth.NIL))) {
			result = 0.0 + (ctWidth.getW().intValue());
		}else
			if (typeValue == (STTblWidth.PCT)) {
				result = (ctWidth.getW().intValue()) / 50.0;
			}else {
			}

		return result;
	}

	public TableWidthType getWidthType() {
		return XWPFTable.getWidthType(getTblPr().getTblW());
	}

	protected static TableWidthType getWidthType(CTTblWidth ctWidth) {
		STTblWidth.Enum typeValue = ctWidth.getType();
		if (typeValue == null) {
			typeValue = STTblWidth.NIL;
			ctWidth.setType(typeValue);
		}
		switch (typeValue.intValue()) {
			case STTblWidth.INT_NIL :
				return TableWidthType.NIL;
			case STTblWidth.INT_AUTO :
				return TableWidthType.AUTO;
			case STTblWidth.INT_DXA :
				return TableWidthType.DXA;
			case STTblWidth.INT_PCT :
				return TableWidthType.PCT;
			default :
				return TableWidthType.AUTO;
		}
	}

	public void setWidth(String widthValue) {
		XWPFTable.setWidthValue(widthValue, getTblPr().getTblW());
	}

	protected static void setWidthValue(String widthValue, CTTblWidth ctWidth) {
		if (!(widthValue.matches(XWPFTable.REGEX_WIDTH_VALUE))) {
			throw new RuntimeException(((((("Table width value \"" + widthValue) + "\" ") + "must match regular expression \"") + (XWPFTable.REGEX_WIDTH_VALUE)) + "\"."));
		}
		if (widthValue.matches("auto")) {
			ctWidth.setType(STTblWidth.AUTO);
			ctWidth.setW(BigInteger.ZERO);
		}else
			if (widthValue.matches(XWPFTable.REGEX_PERCENTAGE)) {
				XWPFTable.setWidthPercentage(ctWidth, widthValue);
			}else {
				ctWidth.setW(new BigInteger(widthValue));
				ctWidth.setType(STTblWidth.DXA);
			}

	}

	protected static void setWidthPercentage(CTTblWidth ctWidth, String widthValue) {
		ctWidth.setType(STTblWidth.PCT);
		if (widthValue.matches(XWPFTable.REGEX_PERCENTAGE)) {
			String numberPart = widthValue.substring(0, ((widthValue.length()) - 1));
			double percentage = (Double.parseDouble(numberPart)) * 50;
			long intValue = Math.round(percentage);
			ctWidth.setW(BigInteger.valueOf(intValue));
		}else
			if (widthValue.matches("[0-9]+")) {
				ctWidth.setW(new BigInteger(widthValue));
			}else {
				throw new RuntimeException((("setWidthPercentage(): Width value must be a percentage (\"33.3%\" or an integer, was \"" + widthValue) + "\""));
			}

	}

	public void setWidthType(TableWidthType widthType) {
		XWPFTable.setWidthType(widthType, getTblPr().getTblW());
	}

	protected static void setWidthType(TableWidthType widthType, CTTblWidth ctWidth) {
		TableWidthType currentType = XWPFTable.getWidthType(ctWidth);
		if (!(currentType.equals(widthType))) {
		}
	}
}

