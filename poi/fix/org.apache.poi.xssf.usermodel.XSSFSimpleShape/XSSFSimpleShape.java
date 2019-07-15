

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ss.usermodel.SimpleShape;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFColorRgbBinary;
import org.apache.poi.xddf.usermodel.XDDFFillProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.text.TextContainer;
import org.apache.poi.xddf.usermodel.text.XDDFBodyProperties;
import org.apache.poi.xddf.usermodel.text.XDDFRunProperties;
import org.apache.poi.xddf.usermodel.text.XDDFTextBody;
import org.apache.poi.xddf.usermodel.text.XDDFTextParagraph;
import org.apache.poi.xssf.usermodel.ListAutoNumber;
import org.apache.poi.xssf.usermodel.TextAutofit;
import org.apache.poi.xssf.usermodel.TextDirection;
import org.apache.poi.xssf.usermodel.TextHorizontalOverflow;
import org.apache.poi.xssf.usermodel.TextVerticalOverflow;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFTextParagraph;
import org.apache.poi.xssf.usermodel.XSSFTextRun;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuideList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingShapeProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetGeometry2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSRgbColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBodyProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextFont;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextNoAutofit;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextNormalAutofit;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextShapeAutofit;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.main.STShapeType;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextAnchoringType;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextHorzOverflowType;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextUnderlineType;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextVertOverflowType;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextVerticalType;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextWrappingType;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTShape;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTShapeNonVisual;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBooleanProperty;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontName;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontSize;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRElt;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRPrElt;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRst;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTUnderlineProperty;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STUnderlineValues;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph.Factory.newInstance;


public class XSSFSimpleShape extends XSSFShape implements Iterable<XSSFTextParagraph> , SimpleShape , TextContainer {
	private final XDDFTextBody _textBody;

	private final List<XSSFTextParagraph> _paragraphs;

	private static CTShape prototype;

	private CTShape ctShape;

	protected XSSFSimpleShape(XSSFDrawing drawing, CTShape ctShape) {
		this.drawing = drawing;
		this.ctShape = ctShape;
		_paragraphs = new ArrayList<>();
		CTTextBody body = ctShape.getTxBody();
		if (body == null) {
			_textBody = null;
		}else {
			_textBody = new XDDFTextBody(this, body);
			for (int i = 0; i < (body.sizeOfPArray()); i++) {
			}
		}
	}

	protected static CTShape prototype() {
		if ((XSSFSimpleShape.prototype) == null) {
			CTShape shape = CTShape.Factory.newInstance();
			CTShapeNonVisual nv = shape.addNewNvSpPr();
			CTNonVisualDrawingProps nvp = nv.addNewCNvPr();
			nvp.setId(1);
			nvp.setName("Shape 1");
			nv.addNewCNvSpPr();
			CTShapeProperties sp = shape.addNewSpPr();
			CTTransform2D t2d = sp.addNewXfrm();
			CTPositiveSize2D p1 = t2d.addNewExt();
			p1.setCx(0);
			p1.setCy(0);
			CTPoint2D p2 = t2d.addNewOff();
			p2.setX(0);
			p2.setY(0);
			CTPresetGeometry2D geom = sp.addNewPrstGeom();
			geom.setPrst(STShapeType.RECT);
			geom.addNewAvLst();
			XDDFTextBody body = new XDDFTextBody(null, shape.addNewTxBody());
			XDDFTextParagraph p = body.initialize();
			XDDFRunProperties rp = p.getAfterLastRunProperties();
			XDDFColor black = new XDDFColorRgbBinary(new byte[]{ 0, 0, 0 });
			XDDFFillProperties fp = new XDDFSolidFillProperties(black);
			rp.setFillProperties(fp);
			XSSFSimpleShape.prototype = shape;
		}
		return XSSFSimpleShape.prototype;
	}

	@org.apache.poi.util.Internal
	public CTShape getCTShape() {
		return ctShape;
	}

	@org.apache.poi.util.Beta
	public XDDFTextBody getTextBody() {
		return _textBody;
	}

	protected void setXfrm(CTTransform2D t2d) {
		ctShape.getSpPr().setXfrm(t2d);
	}

	@Override
	public Iterator<XSSFTextParagraph> iterator() {
		return _paragraphs.iterator();
	}

	public String getText() {
		final int MAX_LEVELS = 9;
		StringBuilder out = new StringBuilder();
		List<Integer> levelCount = new ArrayList<>(MAX_LEVELS);
		XSSFTextParagraph p = null;
		for (int k = 0; k < MAX_LEVELS; k++) {
			levelCount.add(0);
		}
		for (int i = 0; i < (_paragraphs.size()); i++) {
			if ((out.length()) > 0) {
				out.append('\n');
			}
			p = _paragraphs.get(i);
			if ((p.isBullet()) && ((p.getText().length()) > 0)) {
				int level = Math.min(p.getLevel(), (MAX_LEVELS - 1));
				if (p.isBulletAutoNumber()) {
					i = processAutoNumGroup(i, level, levelCount, out);
				}else {
					for (int j = 0; j < level; j++) {
						out.append('\t');
					}
					String character = p.getBulletCharacter();
					out.append(((character.length()) > 0 ? character + " " : "- "));
					out.append(p.getText());
				}
			}else {
				out.append(p.getText());
				for (int k = 0; k < MAX_LEVELS; k++) {
					levelCount.set(k, 0);
				}
			}
		}
		return out.toString();
	}

	private int processAutoNumGroup(int index, int level, List<Integer> levelCount, StringBuilder out) {
		XSSFTextParagraph p = null;
		XSSFTextParagraph nextp = null;
		ListAutoNumber scheme;
		ListAutoNumber nextScheme;
		int startAt;
		int nextStartAt;
		p = _paragraphs.get(index);
		startAt = p.getBulletAutoNumberStart();
		scheme = p.getBulletAutoNumberScheme();
		if ((levelCount.get(level)) == 0) {
			levelCount.set(level, (startAt == 0 ? 1 : startAt));
		}
		for (int j = 0; j < level; j++) {
			out.append('\t');
		}
		if ((p.getText().length()) > 0) {
			out.append(getBulletPrefix(scheme, levelCount.get(level)));
			out.append(p.getText());
		}
		while (true) {
			nextp = ((index + 1) == (_paragraphs.size())) ? null : _paragraphs.get((index + 1));
			if (nextp == null) {
				break;
			}
			if (!((nextp.isBullet()) && (p.isBulletAutoNumber()))) {
				break;
			}
			if ((nextp.getLevel()) > level) {
				if ((out.length()) > 0) {
					out.append('\n');
				}
				index = processAutoNumGroup((index + 1), nextp.getLevel(), levelCount, out);
				continue;
			}else
				if ((nextp.getLevel()) < level) {
					break;
				}

			nextScheme = nextp.getBulletAutoNumberScheme();
			nextStartAt = nextp.getBulletAutoNumberStart();
			if ((nextScheme == scheme) && (nextStartAt == startAt)) {
				++index;
				if ((out.length()) > 0) {
					out.append('\n');
				}
				for (int j = 0; j < level; j++) {
					out.append('\t');
				}
				if ((nextp.getText().length()) > 0) {
					levelCount.set(level, ((levelCount.get(level)) + 1));
					out.append(getBulletPrefix(nextScheme, levelCount.get(level)));
					out.append(nextp.getText());
				}
			}else {
				break;
			}
		} 
		levelCount.set(level, 0);
		return index;
	}

	private String getBulletPrefix(ListAutoNumber scheme, int value) {
		StringBuilder out = new StringBuilder();
		switch (scheme) {
			case ALPHA_LC_PARENT_BOTH :
			case ALPHA_LC_PARENT_R :
				if (scheme == (ListAutoNumber.ALPHA_LC_PARENT_BOTH)) {
					out.append('(');
				}
				out.append(valueToAlpha(value).toLowerCase(Locale.ROOT));
				out.append(')');
				break;
			case ALPHA_UC_PARENT_BOTH :
			case ALPHA_UC_PARENT_R :
				if (scheme == (ListAutoNumber.ALPHA_UC_PARENT_BOTH)) {
					out.append('(');
				}
				out.append(valueToAlpha(value));
				out.append(')');
				break;
			case ALPHA_LC_PERIOD :
				out.append(valueToAlpha(value).toLowerCase(Locale.ROOT));
				out.append('.');
				break;
			case ALPHA_UC_PERIOD :
				out.append(valueToAlpha(value));
				out.append('.');
				break;
			case ARABIC_PARENT_BOTH :
			case ARABIC_PARENT_R :
				if (scheme == (ListAutoNumber.ARABIC_PARENT_BOTH)) {
					out.append('(');
				}
				out.append(value);
				out.append(')');
				break;
			case ARABIC_PERIOD :
				out.append(value);
				out.append('.');
				break;
			case ARABIC_PLAIN :
				out.append(value);
				break;
			case ROMAN_LC_PARENT_BOTH :
			case ROMAN_LC_PARENT_R :
				if (scheme == (ListAutoNumber.ROMAN_LC_PARENT_BOTH)) {
					out.append('(');
				}
				out.append(valueToRoman(value).toLowerCase(Locale.ROOT));
				out.append(')');
				break;
			case ROMAN_UC_PARENT_BOTH :
			case ROMAN_UC_PARENT_R :
				if (scheme == (ListAutoNumber.ROMAN_UC_PARENT_BOTH)) {
					out.append('(');
				}
				out.append(valueToRoman(value));
				out.append(')');
				break;
			case ROMAN_LC_PERIOD :
				out.append(valueToRoman(value).toLowerCase(Locale.ROOT));
				out.append('.');
				break;
			case ROMAN_UC_PERIOD :
				out.append(valueToRoman(value));
				out.append('.');
				break;
			default :
				out.append('\u2022');
				break;
		}
		out.append(" ");
		return out.toString();
	}

	private String valueToAlpha(int value) {
		String alpha = "";
		int modulo;
		while (value > 0) {
			modulo = (value - 1) % 26;
			alpha = ((char) (65 + modulo)) + alpha;
			value = (value - modulo) / 26;
		} 
		return alpha;
	}

	private static String[] _romanChars = new String[]{ "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I" };

	private static int[] _romanAlphaValues = new int[]{ 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };

	private String valueToRoman(int value) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; (value > 0) && (i < (XSSFSimpleShape._romanChars.length)); i++) {
			while ((XSSFSimpleShape._romanAlphaValues[i]) <= value) {
				out.append(XSSFSimpleShape._romanChars[i]);
				value -= XSSFSimpleShape._romanAlphaValues[i];
			} 
		}
		return out.toString();
	}

	public void clearText() {
		_paragraphs.clear();
		CTTextBody txBody = ctShape.getTxBody();
		txBody.setPArray(null);
	}

	public void setText(String text) {
		clearText();
		addNewTextParagraph().addNewTextRun().setText(text);
	}

	public void setText(XSSFRichTextString str) {
		XSSFWorkbook wb = ((XSSFWorkbook) (getDrawing().getParent().getParent()));
		CTTextParagraph p = newInstance();
		if ((str.numFormattingRuns()) == 0) {
			CTRegularTextRun r = p.addNewR();
			CTTextCharacterProperties rPr = r.addNewRPr();
			rPr.setLang("en-US");
			rPr.setSz(1100);
			r.setT(str.getString());
		}else {
			for (int i = 0; i < (str.getCTRst().sizeOfRArray()); i++) {
				CTRElt lt = str.getCTRst().getRArray(i);
				CTRPrElt ltPr = lt.getRPr();
				if (ltPr == null) {
					ltPr = lt.addNewRPr();
				}
				CTRegularTextRun r = p.addNewR();
				CTTextCharacterProperties rPr = r.addNewRPr();
				rPr.setLang("en-US");
				XSSFSimpleShape.applyAttributes(ltPr, rPr);
				r.setT(lt.getT());
			}
		}
		clearText();
		ctShape.getTxBody().setPArray(new CTTextParagraph[]{ p });
	}

	public List<XSSFTextParagraph> getTextParagraphs() {
		return _paragraphs;
	}

	public XSSFTextParagraph addNewTextParagraph() {
		CTTextBody txBody = ctShape.getTxBody();
		CTTextParagraph p = txBody.addNewP();
		return null;
	}

	public XSSFTextParagraph addNewTextParagraph(String text) {
		XSSFTextParagraph paragraph = addNewTextParagraph();
		paragraph.addNewTextRun().setText(text);
		return paragraph;
	}

	public XSSFTextParagraph addNewTextParagraph(XSSFRichTextString str) {
		CTTextBody txBody = ctShape.getTxBody();
		CTTextParagraph p = txBody.addNewP();
		if ((str.numFormattingRuns()) == 0) {
			CTRegularTextRun r = p.addNewR();
			CTTextCharacterProperties rPr = r.addNewRPr();
			rPr.setLang("en-US");
			rPr.setSz(1100);
			r.setT(str.getString());
		}else {
			for (int i = 0; i < (str.getCTRst().sizeOfRArray()); i++) {
				CTRElt lt = str.getCTRst().getRArray(i);
				CTRPrElt ltPr = lt.getRPr();
				if (ltPr == null) {
					ltPr = lt.addNewRPr();
				}
				CTRegularTextRun r = p.addNewR();
				CTTextCharacterProperties rPr = r.addNewRPr();
				rPr.setLang("en-US");
				XSSFSimpleShape.applyAttributes(ltPr, rPr);
				r.setT(lt.getT());
			}
		}
		return null;
	}

	public void setTextHorizontalOverflow(TextHorizontalOverflow overflow) {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			if (overflow == null) {
				if (bodyPr.isSetHorzOverflow()) {
					bodyPr.unsetHorzOverflow();
				}
			}else {
				bodyPr.setHorzOverflow(STTextHorzOverflowType.Enum.forInt(((overflow.ordinal()) + 1)));
			}
		}
	}

	public TextHorizontalOverflow getTextHorizontalOverflow() {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			if (bodyPr.isSetHorzOverflow()) {
				return TextHorizontalOverflow.values()[((bodyPr.getHorzOverflow().intValue()) - 1)];
			}
		}
		return TextHorizontalOverflow.OVERFLOW;
	}

	public void setTextVerticalOverflow(TextVerticalOverflow overflow) {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			if (overflow == null) {
				if (bodyPr.isSetVertOverflow()) {
					bodyPr.unsetVertOverflow();
				}
			}else {
				bodyPr.setVertOverflow(STTextVertOverflowType.Enum.forInt(((overflow.ordinal()) + 1)));
			}
		}
	}

	public TextVerticalOverflow getTextVerticalOverflow() {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			if (bodyPr.isSetVertOverflow()) {
				return TextVerticalOverflow.values()[((bodyPr.getVertOverflow().intValue()) - 1)];
			}
		}
		return TextVerticalOverflow.OVERFLOW;
	}

	public void setVerticalAlignment(VerticalAlignment anchor) {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			if (anchor == null) {
				if (bodyPr.isSetAnchor()) {
					bodyPr.unsetAnchor();
				}
			}else {
				bodyPr.setAnchor(STTextAnchoringType.Enum.forInt(((anchor.ordinal()) + 1)));
			}
		}
	}

	public VerticalAlignment getVerticalAlignment() {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			if (bodyPr.isSetAnchor()) {
				return VerticalAlignment.values()[((bodyPr.getAnchor().intValue()) - 1)];
			}
		}
		return VerticalAlignment.TOP;
	}

	public void setTextDirection(TextDirection orientation) {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			if (orientation == null) {
				if (bodyPr.isSetVert()) {
					bodyPr.unsetVert();
				}
			}else {
				bodyPr.setVert(STTextVerticalType.Enum.forInt(((orientation.ordinal()) + 1)));
			}
		}
	}

	public TextDirection getTextDirection() {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			STTextVerticalType.Enum val = bodyPr.getVert();
			if (val != null) {
				return TextDirection.values()[((val.intValue()) - 1)];
			}
		}
		return TextDirection.HORIZONTAL;
	}

	public double getBottomInset() {
		Double inset = _textBody.getBodyProperties().getBottomInset();
		if (inset == null) {
			return 3.6;
		}else {
			return inset;
		}
	}

	public double getLeftInset() {
		Double inset = _textBody.getBodyProperties().getLeftInset();
		if (inset == null) {
			return 3.6;
		}else {
			return inset;
		}
	}

	public double getRightInset() {
		Double inset = _textBody.getBodyProperties().getRightInset();
		if (inset == null) {
			return 3.6;
		}else {
			return inset;
		}
	}

	public double getTopInset() {
		Double inset = _textBody.getBodyProperties().getTopInset();
		if (inset == null) {
			return 3.6;
		}else {
			return inset;
		}
	}

	public void setBottomInset(double margin) {
		if (margin == (-1)) {
			_textBody.getBodyProperties().setBottomInset(null);
		}else {
			_textBody.getBodyProperties().setBottomInset(margin);
		}
	}

	public void setLeftInset(double margin) {
		if (margin == (-1)) {
			_textBody.getBodyProperties().setLeftInset(null);
		}else {
			_textBody.getBodyProperties().setLeftInset(margin);
		}
	}

	public void setRightInset(double margin) {
		if (margin == (-1)) {
			_textBody.getBodyProperties().setRightInset(null);
		}else {
			_textBody.getBodyProperties().setRightInset(margin);
		}
	}

	public void setTopInset(double margin) {
		if (margin == (-1)) {
			_textBody.getBodyProperties().setTopInset(null);
		}else {
			_textBody.getBodyProperties().setTopInset(margin);
		}
	}

	public boolean getWordWrap() {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			if (bodyPr.isSetWrap()) {
				return (bodyPr.getWrap()) == (STTextWrappingType.SQUARE);
			}
		}
		return true;
	}

	public void setWordWrap(boolean wrap) {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			bodyPr.setWrap((wrap ? STTextWrappingType.SQUARE : STTextWrappingType.NONE));
		}
	}

	public void setTextAutofit(TextAutofit value) {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			if (bodyPr.isSetSpAutoFit()) {
				bodyPr.unsetSpAutoFit();
			}
			if (bodyPr.isSetNoAutofit()) {
				bodyPr.unsetNoAutofit();
			}
			if (bodyPr.isSetNormAutofit()) {
				bodyPr.unsetNormAutofit();
			}
			switch (value) {
				case NONE :
					bodyPr.addNewNoAutofit();
					break;
				case NORMAL :
					bodyPr.addNewNormAutofit();
					break;
				case SHAPE :
					bodyPr.addNewSpAutoFit();
					break;
			}
		}
	}

	public TextAutofit getTextAutofit() {
		CTTextBodyProperties bodyPr = ctShape.getTxBody().getBodyPr();
		if (bodyPr != null) {
			if (bodyPr.isSetNoAutofit()) {
				return TextAutofit.NONE;
			}else
				if (bodyPr.isSetNormAutofit()) {
					return TextAutofit.NORMAL;
				}else
					if (bodyPr.isSetSpAutoFit()) {
						return TextAutofit.SHAPE;
					}


		}
		return TextAutofit.NORMAL;
	}

	public int getShapeType() {
		return ctShape.getSpPr().getPrstGeom().getPrst().intValue();
	}

	public void setShapeType(int type) {
		ctShape.getSpPr().getPrstGeom().setPrst(STShapeType.Enum.forInt(type));
	}

	@Override
	protected CTShapeProperties getShapeProperties() {
		return ctShape.getSpPr();
	}

	private static void applyAttributes(CTRPrElt pr, CTTextCharacterProperties rPr) {
		if ((pr.sizeOfBArray()) > 0) {
			rPr.setB(pr.getBArray(0).getVal());
		}
		if ((pr.sizeOfUArray()) > 0) {
			STUnderlineValues.Enum u1 = pr.getUArray(0).getVal();
			if (u1 == (STUnderlineValues.SINGLE)) {
				rPr.setU(STTextUnderlineType.SNG);
			}else
				if (u1 == (STUnderlineValues.DOUBLE)) {
					rPr.setU(STTextUnderlineType.DBL);
				}else
					if (u1 == (STUnderlineValues.NONE)) {
						rPr.setU(STTextUnderlineType.NONE);
					}


		}
		if ((pr.sizeOfIArray()) > 0) {
			rPr.setI(pr.getIArray(0).getVal());
		}
		if ((pr.sizeOfRFontArray()) > 0) {
			CTTextFont rFont = (rPr.isSetLatin()) ? rPr.getLatin() : rPr.addNewLatin();
			rFont.setTypeface(pr.getRFontArray(0).getVal());
		}
		if ((pr.sizeOfSzArray()) > 0) {
			int sz = ((int) ((pr.getSzArray(0).getVal()) * 100));
			rPr.setSz(sz);
		}
		if ((pr.sizeOfColorArray()) > 0) {
			CTSolidColorFillProperties fill = (rPr.isSetSolidFill()) ? rPr.getSolidFill() : rPr.addNewSolidFill();
			CTColor xlsColor = pr.getColorArray(0);
			if (xlsColor.isSetRgb()) {
				CTSRgbColor clr = (fill.isSetSrgbClr()) ? fill.getSrgbClr() : fill.addNewSrgbClr();
				clr.setVal(xlsColor.getRgb());
			}else
				if (xlsColor.isSetIndexed()) {
					HSSFColor indexed = HSSFColor.getIndexHash().get(((int) (xlsColor.getIndexed())));
					if (indexed != null) {
						byte[] rgb = new byte[3];
						rgb[0] = ((byte) (indexed.getTriplet()[0]));
						rgb[1] = ((byte) (indexed.getTriplet()[1]));
						rgb[2] = ((byte) (indexed.getTriplet()[2]));
						CTSRgbColor clr = (fill.isSetSrgbClr()) ? fill.getSrgbClr() : fill.addNewSrgbClr();
						clr.setVal(rgb);
					}
				}

		}
	}

	@Override
	public String getShapeName() {
		return ctShape.getNvSpPr().getCNvPr().getName();
	}

	@Override
	public int getShapeId() {
		return ((int) (ctShape.getNvSpPr().getCNvPr().getId()));
	}

	@Override
	public <R> Optional<R> findDefinedParagraphProperty(Function<CTTextParagraphProperties, Boolean> isSet, Function<CTTextParagraphProperties, R> getter) {
		return Optional.empty();
	}

	@Override
	public <R> Optional<R> findDefinedRunProperty(Function<CTTextCharacterProperties, Boolean> isSet, Function<CTTextCharacterProperties, R> getter) {
		return Optional.empty();
	}
}

