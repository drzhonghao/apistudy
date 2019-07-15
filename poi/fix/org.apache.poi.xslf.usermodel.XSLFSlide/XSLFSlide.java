

import java.awt.Graphics2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.ooxml.util.DocumentHelper;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.sl.draw.DrawFactory;
import org.apache.poi.sl.draw.DrawSlide;
import org.apache.poi.sl.draw.Drawable;
import org.apache.poi.sl.usermodel.Notes;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.util.Beta;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFBackground;
import org.apache.poi.xslf.usermodel.XSLFChart;
import org.apache.poi.xslf.usermodel.XSLFComment;
import org.apache.poi.xslf.usermodel.XSLFCommentAuthors;
import org.apache.poi.xslf.usermodel.XSLFComments;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XSLFTheme;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlip;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColorMappingOverride;
import org.openxmlformats.schemas.drawingml.x2006.main.CTEmptyElement;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGroupShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGroupTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualGroupDrawingShapeProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.presentationml.x2006.main.CTApplicationNonVisualDrawingProps;
import org.openxmlformats.schemas.presentationml.x2006.main.CTBackground;
import org.openxmlformats.schemas.presentationml.x2006.main.CTBackgroundProperties;
import org.openxmlformats.schemas.presentationml.x2006.main.CTComment;
import org.openxmlformats.schemas.presentationml.x2006.main.CTCommentList;
import org.openxmlformats.schemas.presentationml.x2006.main.CTCommonSlideData;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShapeNonVisual;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlide;
import org.openxmlformats.schemas.presentationml.x2006.main.SldDocument;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import static org.openxmlformats.schemas.presentationml.x2006.main.CTSlide.Factory.newInstance;
import static org.openxmlformats.schemas.presentationml.x2006.main.SldDocument.Factory.parse;


@Beta
public final class XSLFSlide extends XSLFSheet implements Slide<XSLFShape, XSLFTextParagraph> {
	private final CTSlide _slide;

	private XSLFSlideLayout _layout;

	private XSLFComments _comments;

	private XSLFCommentAuthors _commentAuthors;

	private XSLFNotes _notes;

	XSLFSlide() {
		super();
		_slide = XSLFSlide.prototype();
	}

	XSLFSlide(PackagePart part) throws IOException, XmlException {
		super(part);
		Document _doc;
		try {
			_doc = DocumentHelper.readDocument(getPackagePart().getInputStream());
		} catch (SAXException e) {
			throw new IOException(e);
		}
		SldDocument doc = parse(_doc, POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		_slide = doc.getSld();
	}

	private static CTSlide prototype() {
		CTSlide ctSlide = newInstance();
		CTCommonSlideData cSld = ctSlide.addNewCSld();
		CTGroupShape spTree = cSld.addNewSpTree();
		CTGroupShapeNonVisual nvGrpSpPr = spTree.addNewNvGrpSpPr();
		CTNonVisualDrawingProps cnvPr = nvGrpSpPr.addNewCNvPr();
		cnvPr.setId(1);
		cnvPr.setName("");
		nvGrpSpPr.addNewCNvGrpSpPr();
		nvGrpSpPr.addNewNvPr();
		CTGroupShapeProperties grpSpr = spTree.addNewGrpSpPr();
		CTGroupTransform2D xfrm = grpSpr.addNewXfrm();
		CTPoint2D off = xfrm.addNewOff();
		off.setX(0);
		off.setY(0);
		CTPositiveSize2D ext = xfrm.addNewExt();
		ext.setCx(0);
		ext.setCy(0);
		CTPoint2D choff = xfrm.addNewChOff();
		choff.setX(0);
		choff.setY(0);
		CTPositiveSize2D chExt = xfrm.addNewChExt();
		chExt.setCx(0);
		chExt.setCy(0);
		ctSlide.addNewClrMapOvr().addNewMasterClrMapping();
		return ctSlide;
	}

	@Override
	public CTSlide getXmlObject() {
		return _slide;
	}

	@Override
	protected String getRootElementName() {
		return "sld";
	}

	@SuppressWarnings({ "WeakerAccess", "ProtectedMemberInFinalClass" })
	protected void removeChartRelation(XSLFChart chart) {
		removeRelation(chart);
	}

	@SuppressWarnings({ "WeakerAccess", "ProtectedMemberInFinalClass" })
	protected void removeLayoutRelation(XSLFSlideLayout layout) {
		removeRelation(layout, false);
	}

	@Override
	public XSLFSlideLayout getMasterSheet() {
		return getSlideLayout();
	}

	@Override
	public XSLFSlideLayout getSlideLayout() {
		if ((_layout) == null) {
			for (POIXMLDocumentPart p : getRelations()) {
				if (p instanceof XSLFSlideLayout) {
					_layout = ((XSLFSlideLayout) (p));
				}
			}
		}
		if ((_layout) == null) {
			throw new IllegalArgumentException(("SlideLayout was not found for " + (this)));
		}
		return _layout;
	}

	public XSLFSlideMaster getSlideMaster() {
		return getSlideLayout().getSlideMaster();
	}

	@SuppressWarnings("WeakerAccess")
	public XSLFComments getCommentsPart() {
		if ((_comments) == null) {
			for (POIXMLDocumentPart p : getRelations()) {
				if (p instanceof XSLFComments) {
					_comments = ((XSLFComments) (p));
					break;
				}
			}
		}
		return _comments;
	}

	@SuppressWarnings("WeakerAccess")
	public XSLFCommentAuthors getCommentAuthorsPart() {
		if ((_commentAuthors) == null) {
			for (POIXMLDocumentPart p : getRelations()) {
				if (p instanceof XSLFCommentAuthors) {
					_commentAuthors = ((XSLFCommentAuthors) (p));
					return _commentAuthors;
				}
			}
			for (POIXMLDocumentPart p : getSlideShow().getRelations()) {
				if (p instanceof XSLFCommentAuthors) {
					_commentAuthors = ((XSLFCommentAuthors) (p));
					return _commentAuthors;
				}
			}
		}
		return null;
	}

	@Override
	public List<XSLFComment> getComments() {
		final List<XSLFComment> comments = new ArrayList<>();
		final XSLFComments xComments = getCommentsPart();
		final XSLFCommentAuthors xAuthors = getCommentAuthorsPart();
		if (xComments != null) {
			for (final CTComment xc : xComments.getCTCommentsList().getCmArray()) {
			}
		}
		return comments;
	}

	@Override
	public XSLFNotes getNotes() {
		if ((_notes) == null) {
			for (POIXMLDocumentPart p : getRelations()) {
				if (p instanceof XSLFNotes) {
					_notes = ((XSLFNotes) (p));
				}
			}
		}
		if ((_notes) == null) {
			return null;
		}
		return _notes;
	}

	@Override
	public String getTitle() {
		XSLFTextShape txt = getTextShapeByType(Placeholder.TITLE);
		return txt == null ? null : txt.getText();
	}

	@Override
	public XSLFTheme getTheme() {
		return getSlideLayout().getSlideMaster().getTheme();
	}

	@Override
	public XSLFBackground getBackground() {
		CTBackground bg = _slide.getCSld().getBg();
		if (bg != null) {
		}else {
			return getMasterSheet().getBackground();
		}
		return null;
	}

	@Override
	public boolean getFollowMasterGraphics() {
		return _slide.getShowMasterSp();
	}

	@SuppressWarnings("WeakerAccess")
	public void setFollowMasterGraphics(boolean value) {
		_slide.setShowMasterSp(value);
	}

	@Override
	public boolean getFollowMasterObjects() {
		return getFollowMasterGraphics();
	}

	@Override
	public void setFollowMasterObjects(boolean follow) {
		setFollowMasterGraphics(follow);
	}

	@Override
	public XSLFSlide importContent(XSLFSheet src) {
		super.importContent(src);
		if (!(src instanceof XSLFSlide)) {
			return this;
		}
		CTBackground bgOther = ((XSLFSlide) (src))._slide.getCSld().getBg();
		if (bgOther == null) {
			return this;
		}
		CTBackground bgThis = _slide.getCSld().getBg();
		if (bgThis != null) {
			if ((bgThis.isSetBgPr()) && (bgThis.getBgPr().isSetBlipFill())) {
				String oldId = bgThis.getBgPr().getBlipFill().getBlip().getEmbed();
				removeRelation(oldId);
			}
			_slide.getCSld().unsetBg();
		}
		bgThis = ((CTBackground) (_slide.getCSld().addNewBg().set(bgOther)));
		if ((bgOther.isSetBgPr()) && (bgOther.getBgPr().isSetBlipFill())) {
			String idOther = bgOther.getBgPr().getBlipFill().getBlip().getEmbed();
		}
		return this;
	}

	@Override
	public boolean getFollowMasterBackground() {
		return false;
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void setFollowMasterBackground(boolean follow) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getFollowMasterColourScheme() {
		return false;
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void setFollowMasterColourScheme(boolean follow) {
		throw new UnsupportedOperationException();
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void setNotes(Notes<XSLFShape, XSLFTextParagraph> notes) {
		assert notes instanceof XSLFNotes;
	}

	@Override
	public int getSlideNumber() {
		int idx = getSlideShow().getSlides().indexOf(this);
		return idx == (-1) ? idx : idx + 1;
	}

	@Override
	public void draw(Graphics2D graphics) {
		DrawFactory drawFact = DrawFactory.getInstance(graphics);
		Drawable draw = drawFact.getDrawable(this);
		draw.draw(graphics);
	}

	@Override
	public boolean getDisplayPlaceholder(Placeholder placeholder) {
		return false;
	}

	@Override
	public void setHidden(boolean hidden) {
		CTSlide sld = getXmlObject();
		if (hidden) {
			sld.setShow(false);
		}else {
			if (sld.isSetShow()) {
				sld.unsetShow();
			}
		}
	}

	@Override
	public boolean isHidden() {
		CTSlide sld = getXmlObject();
		return (sld.isSetShow()) && (!(sld.getShow()));
	}

	@Override
	public String getSlideName() {
		final CTCommonSlideData cSld = getXmlObject().getCSld();
		return cSld.isSetName() ? cSld.getName() : "Slide" + (getSlideNumber());
	}
}

