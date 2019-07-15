

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageNamespaces;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.sl.draw.DrawFactory;
import org.apache.poi.sl.draw.DrawPictureShape;
import org.apache.poi.sl.draw.DrawSheet;
import org.apache.poi.sl.draw.Drawable;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.Sheet;
import org.apache.poi.util.Beta;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFBackground;
import org.apache.poi.xslf.usermodel.XSLFConnectorShape;
import org.apache.poi.xslf.usermodel.XSLFDrawing;
import org.apache.poi.xslf.usermodel.XSLFFreeformShape;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFObjectShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFPlaceholderDetails;
import org.apache.poi.xslf.usermodel.XSLFRelation;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFShapeContainer;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XSLFTheme;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.apache.xmlbeans.impl.values.XmlAnyTypeImpl;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColorMapping;
import org.openxmlformats.schemas.presentationml.x2006.main.CTConnector;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGraphicalObjectFrame;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape;
import org.openxmlformats.schemas.presentationml.x2006.main.CTOleObject;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPicture;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPlaceholder;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import org.openxmlformats.schemas.presentationml.x2006.main.STPlaceholderType;

import static org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape.Factory.parse;


@Beta
public abstract class XSLFSheet extends POIXMLDocumentPart implements Sheet<XSLFShape, XSLFTextParagraph> , XSLFShapeContainer {
	private static POILogger LOG = POILogFactory.getLogger(XSLFSheet.class);

	private XSLFDrawing _drawing;

	private List<XSLFShape> _shapes;

	private CTGroupShape _spTree;

	private XSLFTheme _theme;

	private List<XSLFTextShape> _placeholders;

	private Map<Integer, XSLFSimpleShape> _placeholderByIdMap;

	private Map<Integer, XSLFSimpleShape> _placeholderByTypeMap;

	private final BitSet shapeIds = new BitSet();

	protected XSLFSheet() {
		super();
	}

	protected XSLFSheet(PackagePart part) {
		super(part);
	}

	@Override
	public XMLSlideShow getSlideShow() {
		POIXMLDocumentPart p = getParent();
		while (p != null) {
			if (p instanceof XMLSlideShow) {
				return ((XMLSlideShow) (p));
			}
			p = p.getParent();
		} 
		throw new IllegalStateException("SlideShow was not found");
	}

	@SuppressWarnings("WeakerAccess")
	protected int allocateShapeId() {
		final int nextId = shapeIds.nextClearBit(1);
		shapeIds.set(nextId);
		return nextId;
	}

	@SuppressWarnings("WeakerAccess")
	protected void registerShapeId(final int shapeId) {
		if (shapeIds.get(shapeId)) {
			XSLFSheet.LOG.log(POILogger.WARN, (("shape id " + shapeId) + " has been already used."));
		}
		shapeIds.set(shapeId);
	}

	@SuppressWarnings("WeakerAccess")
	protected void deregisterShapeId(final int shapeId) {
		if (!(shapeIds.get(shapeId))) {
			XSLFSheet.LOG.log(POILogger.WARN, (("shape id " + shapeId) + " hasn't been registered."));
		}
		shapeIds.clear(shapeId);
	}

	@SuppressWarnings("WeakerAccess")
	protected static List<XSLFShape> buildShapes(CTGroupShape spTree, XSLFShapeContainer parent) {
		List<XSLFShape> shapes = new ArrayList<>();
		XmlCursor cur = spTree.newCursor();
		try {
			for (boolean b = cur.toFirstChild(); b; b = cur.toNextSibling()) {
				XmlObject ch = cur.getObject();
				if (ch instanceof CTShape) {
				}else
					if (ch instanceof CTGroupShape) {
					}else
						if (ch instanceof CTConnector) {
						}else
							if (ch instanceof CTPicture) {
							}else
								if (ch instanceof CTGraphicalObjectFrame) {
								}else
									if (ch instanceof XmlAnyTypeImpl) {
										cur.push();
										if ((cur.toChild(PackageNamespaces.MARKUP_COMPATIBILITY, "Choice")) && (cur.toFirstChild())) {
											try {
												CTGroupShape grp = parse(cur.newXMLStreamReader());
												shapes.addAll(XSLFSheet.buildShapes(grp, parent));
											} catch (XmlException e) {
												XSLFSheet.LOG.log(POILogger.DEBUG, "unparsable alternate content", e);
											}
										}
										cur.pop();
									}





			}
		} finally {
			cur.dispose();
		}
		for (final XSLFShape s : shapes) {
			s.setParent(parent);
		}
		return shapes;
	}

	public abstract XmlObject getXmlObject();

	private XSLFDrawing getDrawing() {
		initDrawingAndShapes();
		return _drawing;
	}

	@Override
	public List<XSLFShape> getShapes() {
		initDrawingAndShapes();
		return _shapes;
	}

	private void initDrawingAndShapes() {
		CTGroupShape cgs = getSpTree();
		if ((_drawing) == null) {
		}
		if ((_shapes) == null) {
			_shapes = XSLFSheet.buildShapes(cgs, this);
		}
	}

	@Override
	public XSLFAutoShape createAutoShape() {
		XSLFAutoShape sh = getDrawing().createAutoShape();
		getShapes().add(sh);
		sh.setParent(this);
		return sh;
	}

	@Override
	public XSLFFreeformShape createFreeform() {
		XSLFFreeformShape sh = getDrawing().createFreeform();
		getShapes().add(sh);
		sh.setParent(this);
		return sh;
	}

	@Override
	public XSLFTextBox createTextBox() {
		XSLFTextBox sh = getDrawing().createTextBox();
		getShapes().add(sh);
		sh.setParent(this);
		return sh;
	}

	@Override
	public XSLFConnectorShape createConnector() {
		XSLFConnectorShape sh = getDrawing().createConnector();
		getShapes().add(sh);
		sh.setParent(this);
		return sh;
	}

	@Override
	public XSLFGroupShape createGroup() {
		XSLFGroupShape sh = getDrawing().createGroup();
		getShapes().add(sh);
		sh.setParent(this);
		return sh;
	}

	@Override
	public XSLFPictureShape createPicture(PictureData pictureData) {
		if (!(pictureData instanceof XSLFPictureData)) {
			throw new IllegalArgumentException("pictureData needs to be of type XSLFPictureData");
		}
		POIXMLDocumentPart.RelationPart rp = addRelation(null, XSLFRelation.IMAGES, ((XSLFPictureData) (pictureData)));
		XSLFPictureShape sh = getDrawing().createPicture(rp.getRelationship().getId());
		new DrawPictureShape(sh).resize();
		getShapes().add(sh);
		sh.setParent(this);
		return sh;
	}

	public XSLFTable createTable() {
		XSLFTable sh = getDrawing().createTable();
		getShapes().add(sh);
		sh.setParent(this);
		return sh;
	}

	@Override
	public XSLFTable createTable(int numRows, int numCols) {
		if ((numRows < 1) || (numCols < 1)) {
			throw new IllegalArgumentException("numRows and numCols must be greater than 0");
		}
		XSLFTable sh = getDrawing().createTable();
		getShapes().add(sh);
		sh.setParent(this);
		for (int r = 0; r < numRows; r++) {
			XSLFTableRow row = sh.addRow();
			for (int c = 0; c < numCols; c++) {
				row.addCell();
			}
		}
		return sh;
	}

	@Override
	public XSLFObjectShape createOleShape(PictureData pictureData) {
		if (!(pictureData instanceof XSLFPictureData)) {
			throw new IllegalArgumentException("pictureData needs to be of type XSLFPictureData");
		}
		POIXMLDocumentPart.RelationPart rp = addRelation(null, XSLFRelation.IMAGES, ((XSLFPictureData) (pictureData)));
		XSLFObjectShape sh = getDrawing().createOleShape(rp.getRelationship().getId());
		CTOleObject oleObj = sh.getCTOleObject();
		Dimension dim = pictureData.getImageDimension();
		oleObj.setImgW(Units.toEMU(dim.getWidth()));
		oleObj.setImgH(Units.toEMU(dim.getHeight()));
		getShapes().add(sh);
		sh.setParent(this);
		return sh;
	}

	@Override
	public Iterator<XSLFShape> iterator() {
		return getShapes().iterator();
	}

	@Override
	public void addShape(XSLFShape shape) {
		throw new UnsupportedOperationException(("Adding a shape from a different container is not supported -" + " create it from scratch witht XSLFSheet.create* methods"));
	}

	@Override
	public boolean removeShape(XSLFShape xShape) {
		XmlObject obj = xShape.getXmlObject();
		CTGroupShape spTree = getSpTree();
		deregisterShapeId(xShape.getShapeId());
		if (obj instanceof CTShape) {
			spTree.getSpList().remove(obj);
		}else
			if (obj instanceof CTGroupShape) {
				XSLFGroupShape gs = ((XSLFGroupShape) (xShape));
				new ArrayList<>(gs.getShapes()).forEach(gs::removeShape);
				spTree.getGrpSpList().remove(obj);
			}else
				if (obj instanceof CTConnector) {
					spTree.getCxnSpList().remove(obj);
				}else
					if (obj instanceof CTGraphicalObjectFrame) {
						spTree.getGraphicFrameList().remove(obj);
					}else
						if (obj instanceof CTPicture) {
							XSLFPictureShape ps = ((XSLFPictureShape) (xShape));
							removePictureRelation(ps);
							spTree.getPicList().remove(obj);
						}else {
							throw new IllegalArgumentException(("Unsupported shape: " + xShape));
						}




		return getShapes().remove(xShape);
	}

	@Override
	public void clear() {
		List<XSLFShape> shapes = new ArrayList<>(getShapes());
		for (XSLFShape shape : shapes) {
			removeShape(shape);
		}
	}

	protected abstract String getRootElementName();

	@SuppressWarnings("WeakerAccess")
	protected CTGroupShape getSpTree() {
		if ((_spTree) == null) {
			XmlObject root = getXmlObject();
			XmlObject[] sp = root.selectPath("declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main' .//*/p:spTree");
			if ((sp.length) == 0) {
				throw new IllegalStateException("CTGroupShape was not found");
			}
			_spTree = ((CTGroupShape) (sp[0]));
		}
		return _spTree;
	}

	@Override
	protected final void commit() throws IOException {
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		String docName = getRootElementName();
		if (docName != null) {
			xmlOptions.setSaveSyntheticDocumentElement(new QName("http://schemas.openxmlformats.org/presentationml/2006/main", docName));
		}
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		getXmlObject().save(out, xmlOptions);
		out.close();
	}

	public XSLFSheet importContent(XSLFSheet src) {
		_spTree = null;
		getSpTree().set(src.getSpTree().copy());
		wipeAndReinitialize(src, 0);
		return this;
	}

	private void wipeAndReinitialize(XSLFSheet src, int offset) {
		_shapes = null;
		_drawing = null;
		initDrawingAndShapes();
		_placeholders = null;
		List<XSLFShape> tgtShapes = getShapes();
		List<XSLFShape> srcShapes = src.getShapes();
		for (int i = 0; i < (srcShapes.size()); i++) {
			XSLFShape s1 = srcShapes.get(i);
			XSLFShape s2 = tgtShapes.get((offset + i));
		}
	}

	@SuppressWarnings("unused")
	public XSLFSheet appendContent(XSLFSheet src) {
		int numShapes = getShapes().size();
		CTGroupShape spTree = getSpTree();
		CTGroupShape srcTree = src.getSpTree();
		for (XmlObject ch : srcTree.selectPath("*")) {
			if (ch instanceof CTShape) {
				spTree.addNewSp().set(ch.copy());
			}else
				if (ch instanceof CTGroupShape) {
					spTree.addNewGrpSp().set(ch.copy());
				}else
					if (ch instanceof CTConnector) {
						spTree.addNewCxnSp().set(ch.copy());
					}else
						if (ch instanceof CTPicture) {
							spTree.addNewPic().set(ch.copy());
						}else
							if (ch instanceof CTGraphicalObjectFrame) {
								spTree.addNewGraphicFrame().set(ch.copy());
							}




		}
		wipeAndReinitialize(src, numShapes);
		return this;
	}

	public XSLFTheme getTheme() {
		if (((_theme) != null) || (!(isSupportTheme()))) {
			return _theme;
		}
		final Optional<XSLFTheme> t = getRelations().stream().filter(( p) -> p instanceof XSLFTheme).map(( p) -> ((XSLFTheme) (p))).findAny();
		if (t.isPresent()) {
			_theme = t.get();
			final CTColorMapping cmap = getColorMapping();
			if (cmap != null) {
			}
		}
		return _theme;
	}

	boolean isSupportTheme() {
		return false;
	}

	CTColorMapping getColorMapping() {
		return null;
	}

	@SuppressWarnings("WeakerAccess")
	protected XSLFTextShape getTextShapeByType(Placeholder type) {
		for (XSLFShape shape : this.getShapes()) {
			if (shape instanceof XSLFTextShape) {
				XSLFTextShape txt = ((XSLFTextShape) (shape));
				if ((txt.getTextType()) == type) {
					return txt;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("WeakerAccess")
	public XSLFSimpleShape getPlaceholder(Placeholder ph) {
		return getPlaceholderByType(ph.ooxmlId);
	}

	XSLFSimpleShape getPlaceholder(CTPlaceholder ph) {
		XSLFSimpleShape shape = null;
		if (ph.isSetIdx()) {
			shape = getPlaceholderById(((int) (ph.getIdx())));
		}
		if ((shape == null) && (ph.isSetType())) {
			shape = getPlaceholderByType(ph.getType().intValue());
		}
		return shape;
	}

	private void initPlaceholders() {
		if ((_placeholders) == null) {
			_placeholders = new ArrayList<>();
			_placeholderByIdMap = new HashMap<>();
			_placeholderByTypeMap = new HashMap<>();
			for (final XSLFShape sh : getShapes()) {
				if (sh instanceof XSLFTextShape) {
					final XSLFTextShape sShape = ((XSLFTextShape) (sh));
				}
			}
		}
	}

	private XSLFSimpleShape getPlaceholderById(int id) {
		initPlaceholders();
		return _placeholderByIdMap.get(id);
	}

	XSLFSimpleShape getPlaceholderByType(int ordinal) {
		initPlaceholders();
		return _placeholderByTypeMap.get(ordinal);
	}

	public XSLFTextShape getPlaceholder(int idx) {
		initPlaceholders();
		return _placeholders.get(idx);
	}

	@SuppressWarnings("WeakerAccess")
	public XSLFTextShape[] getPlaceholders() {
		initPlaceholders();
		return _placeholders.toArray(new XSLFTextShape[0]);
	}

	@Override
	public boolean getFollowMasterGraphics() {
		return false;
	}

	@Override
	public XSLFBackground getBackground() {
		return null;
	}

	@Override
	public void draw(Graphics2D graphics) {
		DrawFactory drawFact = DrawFactory.getInstance(graphics);
		Drawable draw = drawFact.getDrawable(this);
		draw.draw(graphics);
	}

	String importBlip(String blipId, POIXMLDocumentPart parent) {
		final XSLFPictureData parData = parent.getRelationPartById(blipId).getDocumentPart();
		final XSLFPictureData pictureData;
		if ((getPackagePart().getPackage()) == (parent.getPackagePart().getPackage())) {
			pictureData = parData;
		}else {
			XMLSlideShow ppt = getSlideShow();
			pictureData = ppt.addPicture(parData.getData(), parData.getType());
		}
		POIXMLDocumentPart.RelationPart rp = addRelation(blipId, XSLFRelation.IMAGES, pictureData);
		return rp.getRelationship().getId();
	}

	void importPart(PackageRelationship srcRel, PackagePart srcPafrt) {
		PackagePart destPP = getPackagePart();
		PackagePartName srcPPName = srcPafrt.getPartName();
		OPCPackage pkg = destPP.getPackage();
		if (pkg.containPart(srcPPName)) {
			return;
		}
		destPP.addRelationship(srcPPName, TargetMode.INTERNAL, srcRel.getRelationshipType());
		PackagePart part = pkg.createPart(srcPPName, srcPafrt.getContentType());
		try {
			OutputStream out = part.getOutputStream();
			InputStream is = srcPafrt.getInputStream();
			IOUtils.copy(is, out);
			is.close();
			out.close();
		} catch (IOException e) {
			throw new POIXMLException(e);
		}
	}

	void removePictureRelation(XSLFPictureShape pictureShape) {
		int numberOfRelations = 0;
		for (XSLFShape shape : pictureShape.getSheet().getShapes()) {
			if (shape instanceof XSLFPictureShape) {
				XSLFPictureShape currentPictureShape = ((XSLFPictureShape) (shape));
			}
		}
		if (numberOfRelations <= 1) {
		}
	}

	@Override
	public XSLFPlaceholderDetails getPlaceholderDetails(Placeholder placeholder) {
		final XSLFSimpleShape ph = getPlaceholder(placeholder);
		return null;
	}
}

