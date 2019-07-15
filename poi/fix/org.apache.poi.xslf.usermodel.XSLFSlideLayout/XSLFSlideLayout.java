

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XSLFTheme;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.openxmlformats.schemas.presentationml.x2006.main.CTBackground;
import org.openxmlformats.schemas.presentationml.x2006.main.CTCommonSlideData;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideLayout;
import org.openxmlformats.schemas.presentationml.x2006.main.STSlideLayoutType;
import org.openxmlformats.schemas.presentationml.x2006.main.SldLayoutDocument;

import static org.openxmlformats.schemas.presentationml.x2006.main.SldLayoutDocument.Factory.parse;


@Beta
public class XSLFSlideLayout extends XSLFSheet implements MasterSheet<XSLFShape, XSLFTextParagraph> {
	private final CTSlideLayout _layout;

	private XSLFSlideMaster _master;

	public XSLFSlideLayout(PackagePart part) throws IOException, XmlException {
		super(part);
		SldLayoutDocument doc = parse(getPackagePart().getInputStream(), POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		_layout = doc.getSldLayout();
	}

	public String getName() {
		return _layout.getCSld().getName();
	}

	@org.apache.poi.util.Internal
	public CTSlideLayout getXmlObject() {
		return _layout;
	}

	@Override
	protected String getRootElementName() {
		return "sldLayout";
	}

	@SuppressWarnings("WeakerAccess")
	public XSLFSlideMaster getSlideMaster() {
		if ((_master) == null) {
			for (POIXMLDocumentPart p : getRelations()) {
				if (p instanceof XSLFSlideMaster) {
					_master = ((XSLFSlideMaster) (p));
				}
			}
		}
		if ((_master) == null) {
			throw new IllegalStateException(("SlideMaster was not found for " + (this)));
		}
		return _master;
	}

	@Override
	public XSLFSlideMaster getMasterSheet() {
		return getSlideMaster();
	}

	@Override
	public XSLFTheme getTheme() {
		return getSlideMaster().getTheme();
	}

	@Override
	public boolean getFollowMasterGraphics() {
		return _layout.getShowMasterSp();
	}

	@Override
	public XSLFBackground getBackground() {
		CTBackground bg = _layout.getCSld().getBg();
		if (bg != null) {
		}else {
			return getMasterSheet().getBackground();
		}
		return null;
	}

	@SuppressWarnings("WeakerAccess")
	public void copyLayout(XSLFSlide slide) {
		for (XSLFShape sh : getShapes()) {
			if (sh instanceof XSLFTextShape) {
				XSLFTextShape tsh = ((XSLFTextShape) (sh));
				Placeholder ph = tsh.getTextType();
				if (ph == null)
					continue;

			}
		}
	}

	public SlideLayout getType() {
		int ordinal = (_layout.getType().intValue()) - 1;
		return SlideLayout.values()[ordinal];
	}
}

