

import java.awt.Insets;
import java.net.URI;
import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.sl.usermodel.PictureShape;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.util.Beta;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlip;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuideList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualPictureProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeArtExtension;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeArtExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPictureLocking;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetGeometry2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTStretchInfoProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.STShapeType;
import org.openxmlformats.schemas.presentationml.x2006.main.CTApplicationNonVisualDrawingProps;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPicture;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPictureNonVisual;
import org.w3c.dom.Node;

import static org.openxmlformats.schemas.presentationml.x2006.main.CTPicture.Factory.newInstance;
import static org.openxmlformats.schemas.presentationml.x2006.main.CTPicture.Factory.parse;


@Beta
public class XSLFPictureShape extends XSLFSimpleShape implements PictureShape<XSLFShape, XSLFTextParagraph> {
	private static final POILogger LOG = POILogFactory.getLogger(XSLFPictureShape.class);

	private XSLFPictureData _data;

	static CTPicture prototype(int shapeId, String rel) {
		CTPicture ct = newInstance();
		CTPictureNonVisual nvSpPr = ct.addNewNvPicPr();
		CTNonVisualDrawingProps cnv = nvSpPr.addNewCNvPr();
		cnv.setName(("Picture " + shapeId));
		cnv.setId(shapeId);
		nvSpPr.addNewCNvPicPr().addNewPicLocks().setNoChangeAspect(true);
		nvSpPr.addNewNvPr();
		CTBlipFillProperties blipFill = ct.addNewBlipFill();
		CTBlip blip = blipFill.addNewBlip();
		blip.setEmbed(rel);
		blipFill.addNewStretch().addNewFillRect();
		CTShapeProperties spPr = ct.addNewSpPr();
		CTPresetGeometry2D prst = spPr.addNewPrstGeom();
		prst.setPrst(STShapeType.RECT);
		prst.addNewAvLst();
		return ct;
	}

	public boolean isExternalLinkedPicture() {
		return ((getBlipId()) == null) && ((getBlipLink()) != null);
	}

	public XSLFPictureData getPictureData() {
		if ((_data) == null) {
			String blipId = getBlipId();
			if (blipId == null) {
				return null;
			}
			_data = ((XSLFPictureData) (getSheet().getRelationById(blipId)));
		}
		return _data;
	}

	@Override
	public void setPlaceholder(Placeholder placeholder) {
		super.setPlaceholder(placeholder);
	}

	public URI getPictureLink() {
		if ((getBlipId()) != null) {
			return null;
		}
		String rId = getBlipLink();
		if (rId == null) {
			return null;
		}
		PackagePart p = getSheet().getPackagePart();
		PackageRelationship rel = p.getRelationship(rId);
		if (rel != null) {
			return rel.getTargetURI();
		}
		return null;
	}

	protected CTBlipFillProperties getBlipFill() {
		CTPicture ct = ((CTPicture) (getXmlObject()));
		CTBlipFillProperties bfp = ct.getBlipFill();
		if (bfp != null) {
			return bfp;
		}
		String xquery = "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main'; " + ("declare namespace mc='http://schemas.openxmlformats.org/markup-compatibility/2006' " + ".//mc:Fallback/p:blipFill");
		XmlObject xo = selectProperty(XmlObject.class, xquery);
		try {
			xo = parse(xo.getDomNode());
		} catch (XmlException xe) {
			return null;
		}
		return ((CTPicture) (xo)).getBlipFill();
	}

	protected CTBlip getBlip() {
		return getBlipFill().getBlip();
	}

	@SuppressWarnings("WeakerAccess")
	protected String getBlipLink() {
		CTBlip blip = getBlip();
		if (blip != null) {
			String link = blip.getLink();
			return link.isEmpty() ? null : link;
		}else {
			return null;
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected String getBlipId() {
		CTBlip blip = getBlip();
		if (blip != null) {
			String id = blip.getEmbed();
			return id.isEmpty() ? null : id;
		}else {
			return null;
		}
	}

	@Override
	public Insets getClipping() {
		CTRelativeRect r = getBlipFill().getSrcRect();
		return r == null ? null : new Insets(r.getT(), r.getL(), r.getB(), r.getR());
	}

	void copy(XSLFShape sh) {
		XSLFPictureShape p = ((XSLFPictureShape) (sh));
		String blipId = p.getBlipId();
		if (blipId == null) {
			XSLFPictureShape.LOG.log(POILogger.WARN, "unable to copy invalid picture shape");
			return;
		}
		CTPicture ct = ((CTPicture) (getXmlObject()));
		CTBlip blip = getBlipFill().getBlip();
		CTApplicationNonVisualDrawingProps nvPr = ct.getNvPicPr().getNvPr();
		if (nvPr.isSetCustDataLst()) {
			nvPr.unsetCustDataLst();
		}
		if (blip.isSetExtLst()) {
			CTOfficeArtExtensionList extLst = blip.getExtLst();
			for (CTOfficeArtExtension ext : extLst.getExtArray()) {
				String xpath = "declare namespace a14='http://schemas.microsoft.com/office/drawing/2010/main' $this//a14:imgProps/a14:imgLayer";
				XmlObject[] obj = ext.selectPath(xpath);
				if ((obj != null) && ((obj.length) == 1)) {
					XmlCursor c = obj[0].newCursor();
					String id = c.getAttributeText(new QName("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed"));
					c.dispose();
				}
			}
		}
	}
}

