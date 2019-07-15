

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.util.Beta;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XSLFBackground;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColorMapping;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextListStyle;
import org.openxmlformats.schemas.presentationml.x2006.main.CTBackground;
import org.openxmlformats.schemas.presentationml.x2006.main.CTCommonSlideData;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideMaster;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideMasterTextStyles;
import org.openxmlformats.schemas.presentationml.x2006.main.SldMasterDocument;

import static org.openxmlformats.schemas.presentationml.x2006.main.SldMasterDocument.Factory.parse;


@Beta
public class XSLFSlideMaster extends XSLFSheet implements MasterSheet<XSLFShape, XSLFTextParagraph> {
	private CTSlideMaster _slide;

	private Map<String, XSLFSlideLayout> _layouts;

	protected XSLFSlideMaster(PackagePart part) throws IOException, XmlException {
		super(part);
		SldMasterDocument doc = parse(getPackagePart().getInputStream(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		_slide = doc.getSldMaster();
	}

	@Override
	public CTSlideMaster getXmlObject() {
		return _slide;
	}

	@Override
	protected String getRootElementName() {
		return "sldMaster";
	}

	@Override
	public XSLFSlideMaster getMasterSheet() {
		return null;
	}

	private Map<String, XSLFSlideLayout> getLayouts() {
		if ((_layouts) == null) {
			_layouts = new HashMap<>();
			for (POIXMLDocumentPart p : getRelations()) {
				if (p instanceof XSLFSlideLayout) {
					XSLFSlideLayout layout = ((XSLFSlideLayout) (p));
					_layouts.put(layout.getName().toLowerCase(Locale.ROOT), layout);
				}
			}
		}
		return _layouts;
	}

	public XSLFSlideLayout[] getSlideLayouts() {
		return getLayouts().values().toArray(new XSLFSlideLayout[_layouts.size()]);
	}

	public XSLFSlideLayout getLayout(SlideLayout type) {
		for (XSLFSlideLayout layout : getLayouts().values()) {
			if ((layout.getType()) == type) {
				return layout;
			}
		}
		return null;
	}

	public XSLFSlideLayout getLayout(String name) {
		return getLayouts().get(name.toLowerCase(Locale.ROOT));
	}

	@SuppressWarnings("unused")
	protected CTTextListStyle getTextProperties(Placeholder textType) {
		CTTextListStyle props;
		CTSlideMasterTextStyles txStyles = getXmlObject().getTxStyles();
		switch (textType) {
			case TITLE :
			case CENTERED_TITLE :
			case SUBTITLE :
				props = txStyles.getTitleStyle();
				break;
			case BODY :
				props = txStyles.getBodyStyle();
				break;
			default :
				props = txStyles.getOtherStyle();
				break;
		}
		return props;
	}

	@Override
	public XSLFBackground getBackground() {
		CTBackground bg = _slide.getCSld().getBg();
		if (bg != null) {
		}else {
			return null;
		}
		return null;
	}

	boolean isSupportTheme() {
		return true;
	}

	CTColorMapping getColorMapping() {
		return _slide.getClrMap();
	}
}

