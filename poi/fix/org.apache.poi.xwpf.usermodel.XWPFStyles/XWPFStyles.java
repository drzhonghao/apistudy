

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.XWPFDefaultParagraphStyle;
import org.apache.poi.xwpf.usermodel.XWPFDefaultRunStyle;
import org.apache.poi.xwpf.usermodel.XWPFLatentStyles;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocDefaults;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLanguage;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPrDefault;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPrDefault;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.StylesDocument;

import static org.openxmlformats.schemas.wordprocessingml.x2006.main.StylesDocument.Factory.parse;


public class XWPFStyles extends POIXMLDocumentPart {
	private CTStyles ctStyles;

	private List<XWPFStyle> listStyle = new ArrayList<>();

	private XWPFLatentStyles latentStyles;

	private XWPFDefaultRunStyle defaultRunStyle;

	private XWPFDefaultParagraphStyle defaultParaStyle;

	public XWPFStyles(PackagePart part) throws IOException, OpenXML4JException {
		super(part);
	}

	public XWPFStyles() {
	}

	@Override
	protected void onDocumentRead() throws IOException {
		StylesDocument stylesDoc;
		InputStream is = getPackagePart().getInputStream();
		try {
			stylesDoc = parse(is, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
			setStyles(stylesDoc.getStyles());
		} catch (XmlException e) {
			throw new POIXMLException("Unable to read styles", e);
		} finally {
			is.close();
		}
	}

	@Override
	protected void commit() throws IOException {
		if ((ctStyles) == null) {
			throw new IllegalStateException("Unable to write out styles that were never read in!");
		}
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		xmlOptions.setSaveSyntheticDocumentElement(new QName(CTStyles.type.getName().getNamespaceURI(), "styles"));
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		ctStyles.save(out, xmlOptions);
		out.close();
	}

	protected void ensureDocDefaults() {
		if (!(ctStyles.isSetDocDefaults())) {
			ctStyles.addNewDocDefaults();
		}
		CTDocDefaults docDefaults = ctStyles.getDocDefaults();
		if (!(docDefaults.isSetPPrDefault()))
			docDefaults.addNewPPrDefault();

		if (!(docDefaults.isSetRPrDefault()))
			docDefaults.addNewRPrDefault();

		CTPPrDefault pprd = docDefaults.getPPrDefault();
		CTRPrDefault rprd = docDefaults.getRPrDefault();
		if (!(pprd.isSetPPr()))
			pprd.addNewPPr();

		if (!(rprd.isSetRPr()))
			rprd.addNewRPr();

		defaultRunStyle = new XWPFDefaultRunStyle(rprd.getRPr());
		defaultParaStyle = new XWPFDefaultParagraphStyle(pprd.getPPr());
	}

	public void setStyles(CTStyles styles) {
		ctStyles = styles;
		for (CTStyle style : ctStyles.getStyleArray()) {
		}
		if (ctStyles.isSetDocDefaults()) {
			CTDocDefaults docDefaults = ctStyles.getDocDefaults();
			if ((docDefaults.isSetRPrDefault()) && (docDefaults.getRPrDefault().isSetRPr())) {
				defaultRunStyle = new XWPFDefaultRunStyle(docDefaults.getRPrDefault().getRPr());
			}
			if ((docDefaults.isSetPPrDefault()) && (docDefaults.getPPrDefault().isSetPPr())) {
				defaultParaStyle = new XWPFDefaultParagraphStyle(docDefaults.getPPrDefault().getPPr());
			}
		}
	}

	public boolean styleExist(String styleID) {
		for (XWPFStyle style : listStyle) {
			if (style.getStyleId().equals(styleID))
				return true;

		}
		return false;
	}

	public void addStyle(XWPFStyle style) {
		listStyle.add(style);
		ctStyles.addNewStyle();
		int pos = (ctStyles.sizeOfStyleArray()) - 1;
		ctStyles.setStyleArray(pos, style.getCTStyle());
	}

	public XWPFStyle getStyle(String styleID) {
		for (XWPFStyle style : listStyle) {
			try {
				if (style.getStyleId().equals(styleID))
					return style;

			} catch (NullPointerException e) {
			}
		}
		return null;
	}

	public int getNumberOfStyles() {
		return listStyle.size();
	}

	public List<XWPFStyle> getUsedStyleList(XWPFStyle style) {
		List<XWPFStyle> usedStyleList = new ArrayList<>();
		usedStyleList.add(style);
		return getUsedStyleList(style, usedStyleList);
	}

	private List<XWPFStyle> getUsedStyleList(XWPFStyle style, List<XWPFStyle> usedStyleList) {
		String basisStyleID = style.getBasisStyleID();
		XWPFStyle basisStyle = getStyle(basisStyleID);
		if ((basisStyle != null) && (!(usedStyleList.contains(basisStyle)))) {
			usedStyleList.add(basisStyle);
			getUsedStyleList(basisStyle, usedStyleList);
		}
		String linkStyleID = style.getLinkStyleID();
		XWPFStyle linkStyle = getStyle(linkStyleID);
		if ((linkStyle != null) && (!(usedStyleList.contains(linkStyle)))) {
			usedStyleList.add(linkStyle);
			getUsedStyleList(linkStyle, usedStyleList);
		}
		String nextStyleID = style.getNextStyleID();
		XWPFStyle nextStyle = getStyle(nextStyleID);
		if ((nextStyle != null) && (!(usedStyleList.contains(nextStyle)))) {
			usedStyleList.add(linkStyle);
			getUsedStyleList(linkStyle, usedStyleList);
		}
		return usedStyleList;
	}

	protected CTLanguage getCTLanguage() {
		ensureDocDefaults();
		CTLanguage lang = null;
		return lang;
	}

	public void setSpellingLanguage(String strSpellingLanguage) {
		CTLanguage lang = getCTLanguage();
		lang.setVal(strSpellingLanguage);
		lang.setBidi(strSpellingLanguage);
	}

	public void setEastAsia(String strEastAsia) {
		CTLanguage lang = getCTLanguage();
		lang.setEastAsia(strEastAsia);
	}

	public void setDefaultFonts(CTFonts fonts) {
		ensureDocDefaults();
	}

	public XWPFStyle getStyleWithSameName(XWPFStyle style) {
		for (XWPFStyle ownStyle : listStyle) {
			if (ownStyle.hasSameName(style)) {
				return ownStyle;
			}
		}
		return null;
	}

	public XWPFDefaultRunStyle getDefaultRunStyle() {
		ensureDocDefaults();
		return defaultRunStyle;
	}

	public XWPFDefaultParagraphStyle getDefaultParagraphStyle() {
		ensureDocDefaults();
		return defaultParaStyle;
	}

	public XWPFLatentStyles getLatentStyles() {
		return latentStyles;
	}

	public XWPFStyle getStyleWithName(String styleName) {
		XWPFStyle style = null;
		for (XWPFStyle cand : listStyle) {
			if (styleName.equals(cand.getName())) {
				style = cand;
				break;
			}
		}
		return style;
	}
}

