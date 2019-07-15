

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.util.Beta;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBaseStyles;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColorMapping;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColorScheme;
import org.openxmlformats.schemas.drawingml.x2006.main.CTFontCollection;
import org.openxmlformats.schemas.drawingml.x2006.main.CTFontScheme;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeStyleSheet;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextFont;
import org.openxmlformats.schemas.drawingml.x2006.main.STColorSchemeIndex;
import org.openxmlformats.schemas.drawingml.x2006.main.ThemeDocument;
import org.w3c.dom.Node;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeStyleSheet.Factory.newInstance;
import static org.openxmlformats.schemas.drawingml.x2006.main.ThemeDocument.Factory.parse;


@Beta
public class XSLFTheme extends POIXMLDocumentPart {
	private CTOfficeStyleSheet _theme;

	private Map<String, CTColor> _schemeColors;

	XSLFTheme() {
		super();
		_theme = newInstance();
	}

	public XSLFTheme(PackagePart part) throws IOException, XmlException {
		super(part);
		ThemeDocument doc = parse(getPackagePart().getInputStream(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		_theme = doc.getTheme();
		initialize();
	}

	@SuppressWarnings("WeakerAccess")
	public void importTheme(XSLFTheme theme) {
		_theme = theme.getXmlObject();
		_schemeColors = theme._schemeColors;
	}

	private void initialize() {
		CTBaseStyles elems = _theme.getThemeElements();
		CTColorScheme scheme = elems.getClrScheme();
		_schemeColors = new HashMap<>(12);
		for (XmlObject o : scheme.selectPath("*")) {
			CTColor c = ((CTColor) (o));
			String name = c.getDomNode().getLocalName();
			_schemeColors.put(name, c);
		}
	}

	void initColorMap(CTColorMapping cmap) {
		_schemeColors.put("bg1", _schemeColors.get(cmap.getBg1().toString()));
		_schemeColors.put("bg2", _schemeColors.get(cmap.getBg2().toString()));
		_schemeColors.put("tx1", _schemeColors.get(cmap.getTx1().toString()));
		_schemeColors.put("tx2", _schemeColors.get(cmap.getTx2().toString()));
	}

	public String getName() {
		return _theme.getName();
	}

	public void setName(String name) {
		_theme.setName(name);
	}

	@org.apache.poi.util.Internal
	public CTColor getCTColor(String name) {
		return _schemeColors.get(name);
	}

	@org.apache.poi.util.Internal
	public CTOfficeStyleSheet getXmlObject() {
		return _theme;
	}

	@Override
	protected final void commit() throws IOException {
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		getXmlObject().save(out, xmlOptions);
		out.close();
	}

	@SuppressWarnings("WeakerAccess")
	public String getMajorFont() {
		return _theme.getThemeElements().getFontScheme().getMajorFont().getLatin().getTypeface();
	}

	@SuppressWarnings("WeakerAccess")
	public String getMinorFont() {
		return _theme.getThemeElements().getFontScheme().getMinorFont().getLatin().getTypeface();
	}
}

