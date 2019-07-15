

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.ooxml.util.DocumentHelper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.HexDump;
import org.apache.poi.wp.usermodel.CharacterRun;
import org.apache.poi.xwpf.usermodel.BreakClear;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.IRunBody;
import org.apache.poi.xwpf.usermodel.IRunElement;
import org.apache.poi.xwpf.usermodel.ISDTContents;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.VerticalAlign;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SimpleValue;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlString;
import org.apache.xmlbeans.XmlToken;
import org.apache.xmlbeans.XmlTokenSource;
import org.apache.xmlbeans.impl.values.XmlAnyTypeImpl;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlip;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuideList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObjectData;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualPictureProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPictureLocking;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetGeometry2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTStretchInfoProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.main.STShapeType;
import org.openxmlformats.schemas.drawingml.x2006.picture.CTPictureNonVisual;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTEm;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTEmpty;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFFCheckBox;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFFData;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFtnEdnRef;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHighlight;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHpsMeasure;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLanguage;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPTab;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPicture;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRuby;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRubyContent;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSignedHpsMeasure;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSignedTwipsMeasure;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTextScale;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTUnderline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVerticalAlignRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBrClear;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBrType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STEm;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHexColor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHexColorAuto;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHexColorRGB;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHighlightColor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STOnOff;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STThemeColor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalAlignRun;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.apache.xmlbeans.XmlToken.Factory.parse;
import static org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture.type;
import static org.openxmlformats.schemas.wordprocessingml.x2006.main.STEm.Factory.newInstance;


public class XWPFRun implements CharacterRun , IRunElement , ISDTContents {
	private CTR run;

	private String pictureText;

	private IRunBody parent;

	private List<XWPFPicture> pictures;

	public XWPFRun(CTR r, IRunBody p) {
		this.run = r;
		this.parent = p;
		for (CTDrawing ctDrawing : r.getDrawingArray()) {
			for (CTAnchor anchor : ctDrawing.getAnchorArray()) {
				if ((anchor.getDocPr()) != null) {
				}
			}
			for (CTInline inline : ctDrawing.getInlineArray()) {
				if ((inline.getDocPr()) != null) {
				}
			}
		}
		StringBuilder text = new StringBuilder();
		List<XmlObject> pictTextObjs = new ArrayList<>();
		pictTextObjs.addAll(Arrays.asList(r.getPictArray()));
		pictTextObjs.addAll(Arrays.asList(r.getDrawingArray()));
		for (XmlObject o : pictTextObjs) {
			XmlObject[] ts = o.selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:t");
			for (XmlObject t : ts) {
				NodeList kids = t.getDomNode().getChildNodes();
				for (int n = 0; n < (kids.getLength()); n++) {
					if ((kids.item(n)) instanceof Text) {
						if ((text.length()) > 0) {
							text.append("\n");
						}
						text.append(kids.item(n).getNodeValue());
					}
				}
			}
		}
		pictureText = text.toString();
		pictures = new ArrayList<>();
		for (XmlObject o : pictTextObjs) {
			for (org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture pict : getCTPictures(o)) {
			}
		}
	}

	@Deprecated
	public XWPFRun(CTR r, XWPFParagraph p) {
		this(r, ((IRunBody) (p)));
	}

	static void preserveSpaces(XmlString xs) {
		String text = xs.getStringValue();
		if ((text != null) && ((text.startsWith(" ")) || (text.endsWith(" ")))) {
			XmlCursor c = xs.newCursor();
			c.toNextToken();
			c.insertAttributeWithValue(new QName("http://www.w3.org/XML/1998/namespace", "space"), "preserve");
			c.dispose();
		}
	}

	private List<org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture> getCTPictures(XmlObject o) {
		List<org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture> pics = new ArrayList<>();
		XmlObject[] picts = o.selectPath((("declare namespace pic='" + (type.getName().getNamespaceURI())) + "' .//pic:pic"));
		for (XmlObject pict : picts) {
			if (pict instanceof XmlAnyTypeImpl) {
				try {
					pict = org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture.Factory.parse(pict.toString(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
				} catch (XmlException e) {
					throw new POIXMLException(e);
				}
			}
			if (pict instanceof org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture) {
				pics.add(((org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture) (pict)));
			}
		}
		return pics;
	}

	@org.apache.poi.util.Internal
	public CTR getCTR() {
		return run;
	}

	public IRunBody getParent() {
		return parent;
	}

	@Deprecated
	public XWPFParagraph getParagraph() {
		if ((parent) instanceof XWPFParagraph) {
			return ((XWPFParagraph) (parent));
		}
		return null;
	}

	public XWPFDocument getDocument() {
		if ((parent) != null) {
			return parent.getDocument();
		}
		return null;
	}

	private static boolean isCTOnOff(CTOnOff onoff) {
		if (!(onoff.isSetVal())) {
			return true;
		}
		final STOnOff.Enum val = onoff.getVal();
		return (((STOnOff.TRUE) == val) || ((STOnOff.X_1) == val)) || ((STOnOff.ON) == val);
	}

	public String getLang() {
		CTRPr pr = getRunProperties(false);
		Object lang = ((pr == null) || (!(pr.isSetLang()))) ? null : pr.getLang().getVal();
		return ((String) (lang));
	}

	@Override
	public boolean isBold() {
		CTRPr pr = getRunProperties(false);
		return ((pr != null) && (pr.isSetB())) && (XWPFRun.isCTOnOff(pr.getB()));
	}

	@Override
	public void setBold(boolean value) {
		CTRPr pr = getRunProperties(true);
		CTOnOff bold = (pr.isSetB()) ? pr.getB() : pr.addNewB();
		bold.setVal((value ? STOnOff.TRUE : STOnOff.FALSE));
	}

	public String getColor() {
		String color = null;
		if (run.isSetRPr()) {
			CTRPr pr = getRunProperties(false);
			if ((pr != null) && (pr.isSetColor())) {
				CTColor clr = pr.getColor();
				color = clr.xgetVal().getStringValue();
			}
		}
		return color;
	}

	public void setColor(String rgbStr) {
		CTRPr pr = getRunProperties(true);
		CTColor color = (pr.isSetColor()) ? pr.getColor() : pr.addNewColor();
		color.setVal(rgbStr);
	}

	public String getText(int pos) {
		return (run.sizeOfTArray()) == 0 ? null : run.getTArray(pos).getStringValue();
	}

	public String getPictureText() {
		return pictureText;
	}

	public void setText(String value) {
		setText(value, run.sizeOfTArray());
	}

	public void setText(String value, int pos) {
		if (pos > (run.sizeOfTArray())) {
			throw new ArrayIndexOutOfBoundsException("Value too large for the parameter position in XWPFRun.setText(String value,int pos)");
		}
		CTText t = ((pos < (run.sizeOfTArray())) && (pos >= 0)) ? run.getTArray(pos) : run.addNewT();
		t.setStringValue(value);
		XWPFRun.preserveSpaces(t);
	}

	@Override
	public boolean isItalic() {
		CTRPr pr = getRunProperties(false);
		return ((pr != null) && (pr.isSetI())) && (XWPFRun.isCTOnOff(pr.getI()));
	}

	@Override
	public void setItalic(boolean value) {
		CTRPr pr = getRunProperties(true);
		CTOnOff italic = (pr.isSetI()) ? pr.getI() : pr.addNewI();
		italic.setVal((value ? STOnOff.TRUE : STOnOff.FALSE));
	}

	public UnderlinePatterns getUnderline() {
		UnderlinePatterns value = UnderlinePatterns.NONE;
		CTUnderline underline = getCTUnderline(false);
		if (underline != null) {
			STUnderline.Enum baseValue = underline.getVal();
			if (baseValue != null) {
				value = UnderlinePatterns.valueOf(baseValue.intValue());
			}
		}
		return value;
	}

	public void setUnderline(UnderlinePatterns value) {
		CTUnderline underline = getCTUnderline(true);
		underline.setVal(STUnderline.Enum.forInt(value.getValue()));
	}

	private CTUnderline getCTUnderline(boolean create) {
		CTRPr pr = getRunProperties(true);
		CTUnderline underline = pr.getU();
		if (create && (underline == null)) {
			underline = pr.addNewU();
		}
		return underline;
	}

	public void setUnderlineColor(String color) {
		CTUnderline underline = getCTUnderline(true);
		SimpleValue svColor = null;
		if (color.equals("auto")) {
			STHexColorAuto hexColor = STHexColorAuto.Factory.newInstance();
			hexColor.set(STHexColorAuto.Enum.forString(color));
			svColor = ((SimpleValue) (hexColor));
		}else {
			STHexColorRGB rgbColor = STHexColorRGB.Factory.newInstance();
			rgbColor.setStringValue(color);
			svColor = ((SimpleValue) (rgbColor));
		}
		underline.setColor(svColor);
	}

	public void setUnderlineThemeColor(String themeColor) {
		CTUnderline underline = getCTUnderline(true);
		STThemeColor.Enum val = STThemeColor.Enum.forString(themeColor);
		if (val != null) {
			underline.setThemeColor(val);
		}
	}

	public STThemeColor.Enum getUnderlineThemeColor() {
		CTUnderline underline = getCTUnderline(false);
		STThemeColor.Enum color = STThemeColor.NONE;
		if (underline != null) {
			color = underline.getThemeColor();
		}
		return color;
	}

	public String getUnderlineColor() {
		CTUnderline underline = getCTUnderline(true);
		String colorName = "auto";
		Object rawValue = underline.getColor();
		if (rawValue != null) {
			if (rawValue instanceof String) {
				colorName = ((String) (rawValue));
			}else {
				byte[] rgbColor = ((byte[]) (rawValue));
				colorName = ((HexDump.toHex(rgbColor[0])) + (HexDump.toHex(rgbColor[1]))) + (HexDump.toHex(rgbColor[2]));
			}
		}
		return colorName;
	}

	@Override
	public boolean isStrikeThrough() {
		CTRPr pr = getRunProperties(false);
		return ((pr != null) && (pr.isSetStrike())) && (XWPFRun.isCTOnOff(pr.getStrike()));
	}

	@Override
	public void setStrikeThrough(boolean value) {
		CTRPr pr = getRunProperties(true);
		CTOnOff strike = (pr.isSetStrike()) ? pr.getStrike() : pr.addNewStrike();
		strike.setVal((value ? STOnOff.TRUE : STOnOff.FALSE));
	}

	@Deprecated
	public boolean isStrike() {
		return isStrikeThrough();
	}

	@Deprecated
	public void setStrike(boolean value) {
		setStrikeThrough(value);
	}

	@Override
	public boolean isDoubleStrikeThrough() {
		CTRPr pr = getRunProperties(false);
		return ((pr != null) && (pr.isSetDstrike())) && (XWPFRun.isCTOnOff(pr.getDstrike()));
	}

	@Override
	public void setDoubleStrikethrough(boolean value) {
		CTRPr pr = getRunProperties(true);
		CTOnOff dstrike = (pr.isSetDstrike()) ? pr.getDstrike() : pr.addNewDstrike();
		dstrike.setVal((value ? STOnOff.TRUE : STOnOff.FALSE));
	}

	@Override
	public boolean isSmallCaps() {
		CTRPr pr = getRunProperties(false);
		return ((pr != null) && (pr.isSetSmallCaps())) && (XWPFRun.isCTOnOff(pr.getSmallCaps()));
	}

	@Override
	public void setSmallCaps(boolean value) {
		CTRPr pr = getRunProperties(true);
		CTOnOff caps = (pr.isSetSmallCaps()) ? pr.getSmallCaps() : pr.addNewSmallCaps();
		caps.setVal((value ? STOnOff.TRUE : STOnOff.FALSE));
	}

	@Override
	public boolean isCapitalized() {
		CTRPr pr = getRunProperties(false);
		return ((pr != null) && (pr.isSetCaps())) && (XWPFRun.isCTOnOff(pr.getCaps()));
	}

	@Override
	public void setCapitalized(boolean value) {
		CTRPr pr = getRunProperties(true);
		CTOnOff caps = (pr.isSetCaps()) ? pr.getCaps() : pr.addNewCaps();
		caps.setVal((value ? STOnOff.TRUE : STOnOff.FALSE));
	}

	@Override
	public boolean isShadowed() {
		CTRPr pr = getRunProperties(false);
		return ((pr != null) && (pr.isSetShadow())) && (XWPFRun.isCTOnOff(pr.getShadow()));
	}

	@Override
	public void setShadow(boolean value) {
		CTRPr pr = getRunProperties(true);
		CTOnOff shadow = (pr.isSetShadow()) ? pr.getShadow() : pr.addNewShadow();
		shadow.setVal((value ? STOnOff.TRUE : STOnOff.FALSE));
	}

	@Override
	public boolean isImprinted() {
		CTRPr pr = getRunProperties(false);
		return ((pr != null) && (pr.isSetImprint())) && (XWPFRun.isCTOnOff(pr.getImprint()));
	}

	@Override
	public void setImprinted(boolean value) {
		CTRPr pr = getRunProperties(true);
		CTOnOff imprinted = (pr.isSetImprint()) ? pr.getImprint() : pr.addNewImprint();
		imprinted.setVal((value ? STOnOff.TRUE : STOnOff.FALSE));
	}

	@Override
	public boolean isEmbossed() {
		CTRPr pr = getRunProperties(false);
		return ((pr != null) && (pr.isSetEmboss())) && (XWPFRun.isCTOnOff(pr.getEmboss()));
	}

	@Override
	public void setEmbossed(boolean value) {
		CTRPr pr = getRunProperties(true);
		CTOnOff emboss = (pr.isSetEmboss()) ? pr.getEmboss() : pr.addNewEmboss();
		emboss.setVal((value ? STOnOff.TRUE : STOnOff.FALSE));
	}

	@org.apache.poi.util.Removal(version = "4.2")
	public VerticalAlign getSubscript() {
		CTRPr pr = getRunProperties(false);
		return (pr != null) && (pr.isSetVertAlign()) ? VerticalAlign.valueOf(pr.getVertAlign().getVal().intValue()) : VerticalAlign.BASELINE;
	}

	public void setSubscript(VerticalAlign valign) {
		CTRPr pr = getRunProperties(true);
		CTVerticalAlignRun ctValign = (pr.isSetVertAlign()) ? pr.getVertAlign() : pr.addNewVertAlign();
		ctValign.setVal(STVerticalAlignRun.Enum.forInt(valign.getValue()));
	}

	@Override
	public int getKerning() {
		CTRPr pr = getRunProperties(false);
		if ((pr == null) || (!(pr.isSetKern()))) {
			return 0;
		}
		return pr.getKern().getVal().intValue();
	}

	@Override
	public void setKerning(int kern) {
		CTRPr pr = getRunProperties(true);
		CTHpsMeasure kernmes = (pr.isSetKern()) ? pr.getKern() : pr.addNewKern();
		kernmes.setVal(BigInteger.valueOf(kern));
	}

	@Override
	public boolean isHighlighted() {
		CTRPr pr = getRunProperties(false);
		if ((pr == null) || (!(pr.isSetHighlight()))) {
			return false;
		}
		STHighlightColor.Enum val = pr.getHighlight().getVal();
		if ((val == null) || (val == (STHighlightColor.NONE))) {
			return false;
		}
		return true;
	}

	@Override
	public int getCharacterSpacing() {
		CTRPr pr = getRunProperties(false);
		if ((pr == null) || (!(pr.isSetSpacing()))) {
			return 0;
		}
		return pr.getSpacing().getVal().intValue();
	}

	@Override
	public void setCharacterSpacing(int twips) {
		CTRPr pr = getRunProperties(true);
		CTSignedTwipsMeasure spc = (pr.isSetSpacing()) ? pr.getSpacing() : pr.addNewSpacing();
		spc.setVal(BigInteger.valueOf(twips));
	}

	public String getFontFamily() {
		return getFontFamily(null);
	}

	public void setFontFamily(String fontFamily) {
		setFontFamily(fontFamily, null);
	}

	@Override
	public String getFontName() {
		return getFontFamily();
	}

	public String getFontFamily(XWPFRun.FontCharRange fcr) {
		CTRPr pr = getRunProperties(false);
		if ((pr == null) || (!(pr.isSetRFonts()))) {
			return null;
		}
		CTFonts fonts = pr.getRFonts();
		switch (fcr == null ? XWPFRun.FontCharRange.ascii : fcr) {
			default :
			case ascii :
				return fonts.getAscii();
			case cs :
				return fonts.getCs();
			case eastAsia :
				return fonts.getEastAsia();
			case hAnsi :
				return fonts.getHAnsi();
		}
	}

	public void setFontFamily(String fontFamily, XWPFRun.FontCharRange fcr) {
		CTRPr pr = getRunProperties(true);
		CTFonts fonts = (pr.isSetRFonts()) ? pr.getRFonts() : pr.addNewRFonts();
		if (fcr == null) {
			fonts.setAscii(fontFamily);
			if (!(fonts.isSetHAnsi())) {
				fonts.setHAnsi(fontFamily);
			}
			if (!(fonts.isSetCs())) {
				fonts.setCs(fontFamily);
			}
			if (!(fonts.isSetEastAsia())) {
				fonts.setEastAsia(fontFamily);
			}
		}else {
			switch (fcr) {
				case ascii :
					fonts.setAscii(fontFamily);
					break;
				case cs :
					fonts.setCs(fontFamily);
					break;
				case eastAsia :
					fonts.setEastAsia(fontFamily);
					break;
				case hAnsi :
					fonts.setHAnsi(fontFamily);
					break;
			}
		}
	}

	@Override
	public int getFontSize() {
		CTRPr pr = getRunProperties(false);
		return (pr != null) && (pr.isSetSz()) ? pr.getSz().getVal().divide(new BigInteger("2")).intValue() : -1;
	}

	@Override
	public void setFontSize(int size) {
		BigInteger bint = new BigInteger(Integer.toString(size));
		CTRPr pr = getRunProperties(true);
		CTHpsMeasure ctSize = (pr.isSetSz()) ? pr.getSz() : pr.addNewSz();
		ctSize.setVal(bint.multiply(new BigInteger("2")));
	}

	public int getTextPosition() {
		CTRPr pr = getRunProperties(false);
		return (pr != null) && (pr.isSetPosition()) ? pr.getPosition().getVal().intValue() : -1;
	}

	public void setTextPosition(int val) {
		BigInteger bint = new BigInteger(Integer.toString(val));
		CTRPr pr = getRunProperties(true);
		CTSignedHpsMeasure position = (pr.isSetPosition()) ? pr.getPosition() : pr.addNewPosition();
		position.setVal(bint);
	}

	public void removeBreak() {
	}

	public void addBreak() {
		run.addNewBr();
	}

	public void addBreak(BreakType type) {
		CTBr br = run.addNewBr();
		br.setType(STBrType.Enum.forInt(type.getValue()));
	}

	public void addBreak(BreakClear clear) {
		CTBr br = run.addNewBr();
		br.setType(STBrType.Enum.forInt(BreakType.TEXT_WRAPPING.getValue()));
		br.setClear(STBrClear.Enum.forInt(clear.getValue()));
	}

	public void addTab() {
		run.addNewTab();
	}

	public void removeTab() {
	}

	public void addCarriageReturn() {
		run.addNewCr();
	}

	public void removeCarriageReturn() {
	}

	public XWPFPicture addPicture(InputStream pictureData, int pictureType, String filename, int width, int height) throws IOException, InvalidFormatException {
		String relationId;
		XWPFPictureData picData;
		if ((parent.getPart()) instanceof XWPFHeaderFooter) {
			XWPFHeaderFooter headerFooter = ((XWPFHeaderFooter) (parent.getPart()));
			relationId = headerFooter.addPictureData(pictureData, pictureType);
			picData = ((XWPFPictureData) (headerFooter.getRelationById(relationId)));
		}else {
			@SuppressWarnings("resource")
			XWPFDocument doc = parent.getDocument();
			relationId = doc.addPictureData(pictureData, pictureType);
			picData = ((XWPFPictureData) (doc.getRelationById(relationId)));
		}
		try {
			CTDrawing drawing = run.addNewDrawing();
			CTInline inline = drawing.addNewInline();
			String xml = ((((((((("<a:graphic xmlns:a=\"" + (CTGraphicalObject.type.getName().getNamespaceURI())) + "\">") + "<a:graphicData uri=\"") + (type.getName().getNamespaceURI())) + "\">") + "<pic:pic xmlns:pic=\"") + (type.getName().getNamespaceURI())) + "\" />") + "</a:graphicData>") + "</a:graphic>";
			InputSource is = new InputSource(new StringReader(xml));
			Document doc = DocumentHelper.readDocument(is);
			inline.set(parse(doc.getDocumentElement(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS));
			inline.setDistT(0);
			inline.setDistR(0);
			inline.setDistB(0);
			inline.setDistL(0);
			CTNonVisualDrawingProps docPr = inline.addNewDocPr();
			docPr.setDescr(filename);
			CTPositiveSize2D extent = inline.addNewExtent();
			extent.setCx(width);
			extent.setCy(height);
			CTGraphicalObject graphic = inline.getGraphic();
			CTGraphicalObjectData graphicData = graphic.getGraphicData();
			org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture pic = getCTPictures(graphicData).get(0);
			CTPictureNonVisual nvPicPr = pic.addNewNvPicPr();
			CTNonVisualDrawingProps cNvPr = nvPicPr.addNewCNvPr();
			cNvPr.setId(0L);
			cNvPr.setDescr(filename);
			CTNonVisualPictureProperties cNvPicPr = nvPicPr.addNewCNvPicPr();
			cNvPicPr.addNewPicLocks().setNoChangeAspect(true);
			CTBlipFillProperties blipFill = pic.addNewBlipFill();
			CTBlip blip = blipFill.addNewBlip();
			blip.setEmbed(parent.getPart().getRelationId(picData));
			blipFill.addNewStretch().addNewFillRect();
			CTShapeProperties spPr = pic.addNewSpPr();
			CTTransform2D xfrm = spPr.addNewXfrm();
			CTPoint2D off = xfrm.addNewOff();
			off.setX(0);
			off.setY(0);
			CTPositiveSize2D ext = xfrm.addNewExt();
			ext.setCx(width);
			ext.setCy(height);
			CTPresetGeometry2D prstGeom = spPr.addNewPrstGeom();
			prstGeom.setPrst(STShapeType.RECT);
			prstGeom.addNewAvLst();
		} catch (XmlException | SAXException e) {
			throw new IllegalStateException(e);
		}
		return null;
	}

	@org.apache.poi.util.Internal
	public CTInline addChart(String chartRelId) throws IOException, InvalidFormatException {
		try {
			CTInline inline = run.addNewDrawing().addNewInline();
			String xml = ((((((((((("<a:graphic xmlns:a=\"" + (CTGraphicalObject.type.getName().getNamespaceURI())) + "\">") + "<a:graphicData uri=\"") + (CTChart.type.getName().getNamespaceURI())) + "\">") + "<c:chart xmlns:c=\"") + (CTChart.type.getName().getNamespaceURI())) + "\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" r:id=\"") + chartRelId) + "\" />") + "</a:graphicData>") + "</a:graphic>";
			InputSource is = new InputSource(new StringReader(xml));
			Document doc = DocumentHelper.readDocument(is);
			inline.set(parse(doc.getDocumentElement(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS));
			inline.setDistT(0);
			inline.setDistR(0);
			inline.setDistB(0);
			inline.setDistL(0);
			CTNonVisualDrawingProps docPr = inline.addNewDocPr();
			return inline;
		} catch (XmlException e) {
			throw new IllegalStateException(e);
		} catch (SAXException e) {
			throw new IllegalStateException(e);
		}
	}

	public List<XWPFPicture> getEmbeddedPictures() {
		return pictures;
	}

	public void setStyle(String styleId) {
		CTRPr pr = getCTR().getRPr();
		if (null == pr) {
			pr = getCTR().addNewRPr();
		}
		CTString style = ((pr.getRStyle()) != null) ? pr.getRStyle() : pr.addNewRStyle();
		style.setVal(styleId);
	}

	@Override
	public String toString() {
		String phonetic = getPhonetic();
		if ((phonetic.length()) > 0) {
			return (((text()) + " (") + phonetic) + ")";
		}else {
			return text();
		}
	}

	@Override
	public String text() {
		StringBuilder text = new StringBuilder(64);
		XmlCursor c = run.newCursor();
		c.selectPath("./*");
		while (c.toNextSelection()) {
			XmlObject o = c.getObject();
			if (o instanceof CTRuby) {
				handleRuby(o, text, false);
				continue;
			}
			_getText(o, text);
		} 
		c.dispose();
		return text.toString();
	}

	public String getPhonetic() {
		StringBuilder text = new StringBuilder(64);
		XmlCursor c = run.newCursor();
		c.selectPath("./*");
		while (c.toNextSelection()) {
			XmlObject o = c.getObject();
			if (o instanceof CTRuby) {
				handleRuby(o, text, true);
			}
		} 
		if (((pictureText) != null) && ((pictureText.length()) > 0)) {
			text.append("\n").append(pictureText).append("\n");
		}
		c.dispose();
		return text.toString();
	}

	private void handleRuby(XmlObject rubyObj, StringBuilder text, boolean extractPhonetic) {
		XmlCursor c = rubyObj.newCursor();
		c.selectPath(".//*");
		boolean inRT = false;
		boolean inBase = false;
		while (c.toNextSelection()) {
			XmlObject o = c.getObject();
			if (o instanceof CTRubyContent) {
				String tagName = o.getDomNode().getNodeName();
				if ("w:rt".equals(tagName)) {
					inRT = true;
				}else
					if ("w:rubyBase".equals(tagName)) {
						inRT = false;
						inBase = true;
					}

			}else {
				if (extractPhonetic && inRT) {
					_getText(o, text);
				}else
					if ((!extractPhonetic) && inBase) {
						_getText(o, text);
					}

			}
		} 
		c.dispose();
	}

	private void _getText(XmlObject o, StringBuilder text) {
		if (o instanceof CTText) {
			String tagName = o.getDomNode().getNodeName();
			if (!("w:instrText".equals(tagName))) {
				text.append(((CTText) (o)).getStringValue());
			}
		}
		if (o instanceof CTFldChar) {
			CTFldChar ctfldChar = ((CTFldChar) (o));
			if ((ctfldChar.getFldCharType()) == (STFldCharType.BEGIN)) {
				if ((ctfldChar.getFfData()) != null) {
					for (CTFFCheckBox checkBox : ctfldChar.getFfData().getCheckBoxList()) {
						if (((checkBox.getDefault()) != null) && ((checkBox.getDefault().getVal()) == (STOnOff.X_1))) {
							text.append("|X|");
						}else {
							text.append("|_|");
						}
					}
				}
			}
		}
		if (o instanceof CTPTab) {
			text.append('\t');
		}
		if (o instanceof CTBr) {
			text.append('\n');
		}
		if (o instanceof CTEmpty) {
			String tagName = o.getDomNode().getNodeName();
			if (("w:tab".equals(tagName)) || ("tab".equals(tagName))) {
				text.append('\t');
			}
			if (("w:br".equals(tagName)) || ("br".equals(tagName))) {
				text.append('\n');
			}
			if (("w:cr".equals(tagName)) || ("cr".equals(tagName))) {
				text.append('\n');
			}
		}
		if (o instanceof CTFtnEdnRef) {
			CTFtnEdnRef ftn = ((CTFtnEdnRef) (o));
			String footnoteRef = (ftn.getDomNode().getLocalName().equals("footnoteReference")) ? ("[footnoteRef:" + (ftn.getId().intValue())) + "]" : ("[endnoteRef:" + (ftn.getId().intValue())) + "]";
			text.append(footnoteRef);
		}
	}

	public static enum FontCharRange {

		ascii,
		cs,
		eastAsia,
		hAnsi;}

	public void setTextScale(int percentage) {
		CTRPr pr = getRunProperties(true);
		CTTextScale scale = (pr.isSetW()) ? pr.getW() : pr.addNewW();
		scale.setVal(percentage);
	}

	public int getTextScale() {
		CTRPr pr = getRunProperties(true);
		CTTextScale scale = (pr.isSetW()) ? pr.getW() : pr.addNewW();
		int value = scale.getVal();
		if (value == 0) {
			value = 100;
		}
		return value;
	}

	public void setTextHighlightColor(String colorName) {
		CTRPr pr = getRunProperties(true);
		CTHighlight highlight = (pr.isSetHighlight()) ? pr.getHighlight() : pr.addNewHighlight();
		STHighlightColor color = highlight.xgetVal();
		if (color == null) {
			color = STHighlightColor.Factory.newInstance();
		}
		STHighlightColor.Enum val = STHighlightColor.Enum.forString(colorName);
		if (val != null) {
			color.setStringValue(val.toString());
			highlight.xsetVal(color);
		}
	}

	public STHighlightColor.Enum getTextHightlightColor() {
		CTRPr pr = getRunProperties(true);
		CTHighlight highlight = (pr.isSetHighlight()) ? pr.getHighlight() : pr.addNewHighlight();
		STHighlightColor color = highlight.xgetVal();
		if (color == null) {
			color = STHighlightColor.Factory.newInstance();
			color.set(STHighlightColor.NONE);
		}
		return ((STHighlightColor.Enum) (color.enumValue()));
	}

	public boolean isVanish() {
		CTRPr pr = getRunProperties(true);
		return ((pr != null) && (pr.isSetVanish())) && (XWPFRun.isCTOnOff(pr.getVanish()));
	}

	public void setVanish(boolean value) {
		CTRPr pr = getRunProperties(true);
		CTOnOff vanish = (pr.isSetVanish()) ? pr.getVanish() : pr.addNewVanish();
		vanish.setVal((value ? STOnOff.TRUE : STOnOff.FALSE));
	}

	public STVerticalAlignRun.Enum getVerticalAlignment() {
		CTRPr pr = getRunProperties(true);
		CTVerticalAlignRun vertAlign = (pr.isSetVertAlign()) ? pr.getVertAlign() : pr.addNewVertAlign();
		STVerticalAlignRun.Enum val = vertAlign.getVal();
		if (val == null) {
			val = STVerticalAlignRun.BASELINE;
		}
		return val;
	}

	public void setVerticalAlignment(String verticalAlignment) {
		CTRPr pr = getRunProperties(true);
		CTVerticalAlignRun vertAlign = (pr.isSetVertAlign()) ? pr.getVertAlign() : pr.addNewVertAlign();
		STVerticalAlignRun align = vertAlign.xgetVal();
		if (align == null) {
			align = STVerticalAlignRun.Factory.newInstance();
		}
		STVerticalAlignRun.Enum val = STVerticalAlignRun.Enum.forString(verticalAlignment);
		if (val != null) {
			align.setStringValue(val.toString());
			vertAlign.xsetVal(align);
		}
	}

	public STEm.Enum getEmphasisMark() {
		CTRPr pr = getRunProperties(true);
		CTEm emphasis = (pr.isSetEm()) ? pr.getEm() : pr.addNewEm();
		STEm.Enum val = emphasis.getVal();
		if (val == null) {
			val = STEm.NONE;
		}
		return val;
	}

	public void setEmphasisMark(String markType) {
		CTRPr pr = getRunProperties(true);
		CTEm emphasisMark = (pr.isSetEm()) ? pr.getEm() : pr.addNewEm();
		STEm mark = emphasisMark.xgetVal();
		if (mark == null) {
			mark = newInstance();
		}
		STEm.Enum val = STEm.Enum.forString(markType);
		if (val != null) {
			mark.setStringValue(val.toString());
			emphasisMark.xsetVal(mark);
		}
	}

	private CTRPr getRunProperties(boolean create) {
		CTRPr pr = (run.isSetRPr()) ? run.getRPr() : null;
		if (create && (pr == null)) {
			pr = run.addNewRPr();
		}
		return pr;
	}
}

