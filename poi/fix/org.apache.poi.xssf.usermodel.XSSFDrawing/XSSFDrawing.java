

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLRelation;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.ImageUtils;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFAnchor;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFConnector;
import org.apache.poi.xssf.usermodel.XSSFFactory;
import org.apache.poi.xssf.usermodel.XSSFGraphicFrame;
import org.apache.poi.xssf.usermodel.XSSFObjectData;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFShapeGroup;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.apache.poi.xssf.usermodel.XSSFTextBox;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.apache.xmlbeans.impl.values.XmlAnyTypeImpl;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlip;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGroupShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGroupTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeArtExtension;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeArtExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTStretchInfoProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTAbsoluteAnchor;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTAnchorClientData;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTConnector;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTDrawing;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGraphicalObjectFrame;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGroupShape;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTOneCellAnchor;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTPicture;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTPictureNonVisual;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTShape;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTShapeNonVisual;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTTwoCellAnchor;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.STEditAs;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTOleObject;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTOleObjects;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;

import static org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE;
import static org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.MOVE_AND_RESIZE;
import static org.apache.poi.ss.usermodel.ClientAnchor.AnchorType.MOVE_DONT_RESIZE;
import static org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D.Factory.newInstance;
import static org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTDrawing.Factory.parse;


public final class XSSFDrawing extends POIXMLDocumentPart implements Drawing<XSSFShape> {
	private static final POILogger LOG = POILogFactory.getLogger(XSSFDrawing.class);

	private CTDrawing drawing;

	private long numOfGraphicFrames;

	protected static final String NAMESPACE_A = XSSFRelation.NS_DRAWINGML;

	protected static final String NAMESPACE_C = XSSFRelation.NS_CHART;

	protected XSSFDrawing() {
		super();
		drawing = XSSFDrawing.newDrawing();
	}

	public XSSFDrawing(PackagePart part) throws IOException, XmlException {
		super(part);
		XmlOptions options = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		options.setLoadReplaceDocumentElement(null);
		InputStream is = part.getInputStream();
		try {
			drawing = CTDrawing.Factory.parse(is, options);
		} finally {
			is.close();
		}
	}

	private static CTDrawing newDrawing() {
		return CTDrawing.Factory.newInstance();
	}

	@org.apache.poi.util.Internal
	public CTDrawing getCTDrawing() {
		return drawing;
	}

	@Override
	protected void commit() throws IOException {
		XmlOptions xmlOptions = new XmlOptions(POIXMLTypeLoader.DEFAULT_XML_OPTIONS);
		xmlOptions.setSaveSyntheticDocumentElement(new QName(CTDrawing.type.getName().getNamespaceURI(), "wsDr", "xdr"));
		PackagePart part = getPackagePart();
		OutputStream out = part.getOutputStream();
		drawing.save(out, xmlOptions);
		out.close();
	}

	@Override
	public XSSFClientAnchor createAnchor(int dx1, int dy1, int dx2, int dy2, int col1, int row1, int col2, int row2) {
		return new XSSFClientAnchor(dx1, dy1, dx2, dy2, col1, row1, col2, row2);
	}

	public XSSFTextBox createTextbox(XSSFClientAnchor anchor) {
		long shapeId = newShapeId();
		CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
		CTShape ctShape = ctAnchor.addNewSp();
		ctShape.getNvSpPr().getCNvPr().setId(shapeId);
		return null;
	}

	public XSSFPicture createPicture(XSSFClientAnchor anchor, int pictureIndex) {
		PackageRelationship rel = addPictureReference(pictureIndex);
		long shapeId = newShapeId();
		CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
		CTPicture ctShape = ctAnchor.addNewPic();
		ctShape.getNvPicPr().getCNvPr().setId(shapeId);
		ctShape.getSpPr().setXfrm(createXfrm(anchor));
		return null;
	}

	@Override
	public XSSFPicture createPicture(ClientAnchor anchor, int pictureIndex) {
		return createPicture(((XSSFClientAnchor) (anchor)), pictureIndex);
	}

	public XSSFChart createChart(XSSFClientAnchor anchor) {
		int chartNumber = (getPackagePart().getPackage().getPartsByContentType(XSSFRelation.CHART.getContentType()).size()) + 1;
		POIXMLDocumentPart.RelationPart rp = createRelationship(XSSFRelation.CHART, XSSFFactory.getInstance(), chartNumber, false);
		XSSFChart chart = rp.getDocumentPart();
		String chartRelId = rp.getRelationship().getId();
		XSSFGraphicFrame frame = createGraphicFrame(anchor);
		frame.getCTGraphicalObjectFrame().setXfrm(createXfrm(anchor));
		return chart;
	}

	public XSSFChart createChart(ClientAnchor anchor) {
		return createChart(((XSSFClientAnchor) (anchor)));
	}

	public XSSFChart importChart(XSSFChart srcChart) throws IOException, XmlException {
		CTTwoCellAnchor anchor = ((XSSFDrawing) (srcChart.getParent())).getCTDrawing().getTwoCellAnchorArray(0);
		CTMarker from = ((CTMarker) (anchor.getFrom().copy()));
		CTMarker to = ((CTMarker) (anchor.getTo().copy()));
		return null;
	}

	@SuppressWarnings("resource")
	protected PackageRelationship addPictureReference(int pictureIndex) {
		XSSFWorkbook wb = ((XSSFWorkbook) (getParent().getParent()));
		XSSFPictureData data = wb.getAllPictures().get(pictureIndex);
		return null;
	}

	public XSSFSimpleShape createSimpleShape(XSSFClientAnchor anchor) {
		long shapeId = newShapeId();
		CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
		CTShape ctShape = ctAnchor.addNewSp();
		ctShape.getNvSpPr().getCNvPr().setId(shapeId);
		ctShape.getSpPr().setXfrm(createXfrm(anchor));
		return null;
	}

	public XSSFConnector createConnector(XSSFClientAnchor anchor) {
		CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
		CTConnector ctShape = ctAnchor.addNewCxnSp();
		return null;
	}

	public XSSFShapeGroup createGroup(XSSFClientAnchor anchor) {
		CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
		CTGroupShape ctGroup = ctAnchor.addNewGrpSp();
		CTTransform2D xfrm = createXfrm(anchor);
		CTGroupTransform2D grpXfrm = ctGroup.getGrpSpPr().getXfrm();
		grpXfrm.setOff(xfrm.getOff());
		grpXfrm.setExt(xfrm.getExt());
		grpXfrm.setChExt(xfrm.getExt());
		return null;
	}

	@Override
	public XSSFComment createCellComment(ClientAnchor anchor) {
		XSSFClientAnchor ca = ((XSSFClientAnchor) (anchor));
		XSSFSheet sheet = getSheet();
		if (ca.isSet()) {
			int dx1Pixels = (ca.getDx1()) / (Units.EMU_PER_PIXEL);
			int dy1Pixels = (ca.getDy1()) / (Units.EMU_PER_PIXEL);
			int dx2Pixels = (ca.getDx2()) / (Units.EMU_PER_PIXEL);
			int dy2Pixels = (ca.getDy2()) / (Units.EMU_PER_PIXEL);
			String position = ((((((((((((((ca.getCol1()) + ", ") + dx1Pixels) + ", ") + (ca.getRow1())) + ", ") + dy1Pixels) + ", ") + (ca.getCol2())) + ", ") + dx2Pixels) + ", ") + (ca.getRow2())) + ", ") + dy2Pixels;
		}
		CellAddress ref = new CellAddress(ca.getRow1(), ca.getCol1());
		return null;
	}

	private XSSFGraphicFrame createGraphicFrame(XSSFClientAnchor anchor) {
		CTTwoCellAnchor ctAnchor = createTwoCellAnchor(anchor);
		CTGraphicalObjectFrame ctGraphicFrame = ctAnchor.addNewGraphicFrame();
		ctGraphicFrame.setXfrm(createXfrm(anchor));
		long frameId = (numOfGraphicFrames)++;
		return null;
	}

	@Override
	public XSSFObjectData createObjectData(ClientAnchor anchor, int storageId, int pictureIndex) {
		XSSFSheet sh = getSheet();
		PackagePart sheetPart = sh.getPackagePart();
		XSSFSheet sheet = getSheet();
		XSSFWorkbook wb = sheet.getWorkbook();
		int sheetIndex = wb.getSheetIndex(sheet);
		long shapeId = ((sheetIndex + 1L) * 1024) + (newShapeId());
		PackagePartName olePN;
		try {
			olePN = PackagingURIHelper.createPartName((("/xl/embeddings/oleObject" + storageId) + ".bin"));
		} catch (InvalidFormatException e) {
			throw new POIXMLException(e);
		}
		PackageRelationship olePR = sheetPart.addRelationship(olePN, TargetMode.INTERNAL, POIXMLDocument.OLE_OBJECT_REL_TYPE);
		XSSFPictureData imgPD = sh.getWorkbook().getAllPictures().get(pictureIndex);
		PackagePartName imgPN = imgPD.getPackagePart().getPartName();
		PackageRelationship imgSheetPR = sheetPart.addRelationship(imgPN, TargetMode.INTERNAL, PackageRelationshipTypes.IMAGE_PART);
		PackageRelationship imgDrawPR = getPackagePart().addRelationship(imgPN, TargetMode.INTERNAL, PackageRelationshipTypes.IMAGE_PART);
		CTWorksheet cwb = sh.getCTWorksheet();
		CTOleObjects oo = (cwb.isSetOleObjects()) ? cwb.getOleObjects() : cwb.addNewOleObjects();
		CTOleObject ole1 = oo.addNewOleObject();
		ole1.setProgId("Package");
		ole1.setShapeId(shapeId);
		ole1.setId(olePR.getId());
		XmlCursor cur1 = ole1.newCursor();
		cur1.toEndToken();
		cur1.beginElement("objectPr", XSSFRelation.NS_SPREADSHEETML);
		cur1.insertAttributeWithValue("id", PackageRelationshipTypes.CORE_PROPERTIES_ECMA376_NS, imgSheetPR.getId());
		cur1.insertAttributeWithValue("defaultSize", "0");
		cur1.beginElement("anchor", XSSFRelation.NS_SPREADSHEETML);
		cur1.insertAttributeWithValue("moveWithCells", "1");
		CTTwoCellAnchor ctAnchor = createTwoCellAnchor(((XSSFClientAnchor) (anchor)));
		XmlCursor cur2 = ctAnchor.newCursor();
		cur2.copyXmlContents(cur1);
		cur2.dispose();
		cur1.toParent();
		cur1.toFirstChild();
		cur1.setName(new QName(XSSFRelation.NS_SPREADSHEETML, "from"));
		cur1.toNextSibling();
		cur1.setName(new QName(XSSFRelation.NS_SPREADSHEETML, "to"));
		cur1.dispose();
		CTShape ctShape = ctAnchor.addNewSp();
		ctShape.getSpPr().setXfrm(createXfrm(((XSSFClientAnchor) (anchor))));
		CTBlipFillProperties blipFill = ctShape.getSpPr().addNewBlipFill();
		blipFill.addNewBlip().setEmbed(imgDrawPR.getId());
		blipFill.addNewStretch().addNewFillRect();
		CTNonVisualDrawingProps cNvPr = ctShape.getNvSpPr().getCNvPr();
		cNvPr.setId(shapeId);
		cNvPr.setName(("Object " + shapeId));
		XmlCursor extCur = cNvPr.getExtLst().getExtArray(0).newCursor();
		extCur.toFirstChild();
		extCur.setAttributeText(new QName("spid"), ("_x0000_s" + shapeId));
		extCur.dispose();
		return null;
	}

	public List<XSSFChart> getCharts() {
		List<XSSFChart> charts = new ArrayList<>();
		for (POIXMLDocumentPart part : getRelations()) {
			if (part instanceof XSSFChart) {
				charts.add(((XSSFChart) (part)));
			}
		}
		return charts;
	}

	private CTTwoCellAnchor createTwoCellAnchor(XSSFClientAnchor anchor) {
		CTTwoCellAnchor ctAnchor = drawing.addNewTwoCellAnchor();
		ctAnchor.setFrom(anchor.getFrom());
		ctAnchor.setTo(anchor.getTo());
		ctAnchor.addNewClientData();
		STEditAs.Enum editAs;
		switch (anchor.getAnchorType()) {
			case DONT_MOVE_AND_RESIZE :
				editAs = STEditAs.ABSOLUTE;
				break;
			case MOVE_AND_RESIZE :
				editAs = STEditAs.TWO_CELL;
				break;
			case MOVE_DONT_RESIZE :
				editAs = STEditAs.ONE_CELL;
				break;
			default :
				editAs = STEditAs.ONE_CELL;
		}
		ctAnchor.setEditAs(editAs);
		return ctAnchor;
	}

	private CTTransform2D createXfrm(XSSFClientAnchor anchor) {
		CTTransform2D xfrm = newInstance();
		CTPoint2D off = xfrm.addNewOff();
		off.setX(anchor.getDx1());
		off.setY(anchor.getDy1());
		XSSFSheet sheet = getSheet();
		double widthPx = 0;
		for (int col = anchor.getCol1(); col < (anchor.getCol2()); col++) {
			widthPx += sheet.getColumnWidthInPixels(col);
		}
		double heightPx = 0;
		for (int row = anchor.getRow1(); row < (anchor.getRow2()); row++) {
			heightPx += ImageUtils.getRowHeightInPixels(sheet, row);
		}
		long width = Units.pixelToEMU(((int) (widthPx)));
		long height = Units.pixelToEMU(((int) (heightPx)));
		CTPositiveSize2D ext = xfrm.addNewExt();
		ext.setCx(((width - (anchor.getDx1())) + (anchor.getDx2())));
		ext.setCy(((height - (anchor.getDy1())) + (anchor.getDy2())));
		return xfrm;
	}

	private long newShapeId() {
		return ((1 + (drawing.sizeOfAbsoluteAnchorArray())) + (drawing.sizeOfOneCellAnchorArray())) + (drawing.sizeOfTwoCellAnchorArray());
	}

	public List<XSSFShape> getShapes() {
		List<XSSFShape> lst = new ArrayList<>();
		XmlCursor cur = drawing.newCursor();
		try {
			if (cur.toFirstChild()) {
				addShapes(cur, lst);
			}
		} finally {
			cur.dispose();
		}
		return lst;
	}

	public List<XSSFShape> getShapes(XSSFShapeGroup groupshape) {
		List<XSSFShape> lst = new ArrayList<>();
		XmlCursor cur = groupshape.getCTGroupShape().newCursor();
		try {
			addShapes(cur, lst);
		} finally {
			cur.dispose();
		}
		return lst;
	}

	private void addShapes(XmlCursor cur, List<XSSFShape> lst) {
		try {
			do {
				cur.push();
				if (cur.toFirstChild()) {
					do {
						XmlObject obj = cur.getObject();
						XSSFShape shape;
						if (obj instanceof CTMarker) {
							continue;
						}else
							if (obj instanceof CTPicture) {
							}else
								if (obj instanceof CTConnector) {
								}else
									if (obj instanceof CTShape) {
									}else
										if (obj instanceof CTGraphicalObjectFrame) {
										}else
											if (obj instanceof CTGroupShape) {
											}else
												if (obj instanceof XmlAnyTypeImpl) {
													XSSFDrawing.LOG.log(POILogger.WARN, ("trying to parse AlternateContent, " + (("this unlinks the returned Shapes from the underlying xml content, " + "so those shapes can't be used to modify the drawing, ") + "i.e. modifications will be ignored!")));
													cur.push();
													cur.toFirstChild();
													XmlCursor cur2 = null;
													try {
														CTDrawing alterWS = parse(cur.newXMLStreamReader());
														cur2 = alterWS.newCursor();
														if (cur2.toFirstChild()) {
															addShapes(cur2, lst);
														}
													} catch (XmlException e) {
														XSSFDrawing.LOG.log(POILogger.WARN, "unable to parse CTDrawing in alternate content.", e);
													} finally {
														if (cur2 != null) {
															cur2.dispose();
														}
														cur.pop();
													}
													continue;
												}else {
													continue;
												}






						shape = null;
						assert shape != null;
						lst.add(shape);
					} while (cur.toNextSibling() );
				}
				cur.pop();
			} while (cur.toNextSibling() );
		} finally {
			cur.dispose();
		}
	}

	private boolean hasOleLink(XmlObject shape) {
		QName uriName = new QName(null, "uri");
		String xquery = ("declare namespace a='" + (XSSFRelation.NS_DRAWINGML)) + "' .//a:extLst/a:ext";
		XmlCursor cur = shape.newCursor();
		cur.selectPath(xquery);
		try {
			while (cur.toNextSelection()) {
				String uri = cur.getAttributeText(uriName);
				if ("{63B3BB69-23CF-44E3-9099-C40C66FF867C}".equals(uri)) {
					return true;
				}
			} 
		} finally {
			cur.dispose();
		}
		return false;
	}

	private XSSFAnchor getAnchorFromParent(XmlObject obj) {
		XSSFAnchor anchor = null;
		XmlObject parentXbean = null;
		XmlCursor cursor = obj.newCursor();
		if (cursor.toParent()) {
			parentXbean = cursor.getObject();
		}
		cursor.dispose();
		if (parentXbean != null) {
			if (parentXbean instanceof CTTwoCellAnchor) {
				CTTwoCellAnchor ct = ((CTTwoCellAnchor) (parentXbean));
			}else
				if (parentXbean instanceof CTOneCellAnchor) {
					CTOneCellAnchor ct = ((CTOneCellAnchor) (parentXbean));
				}else
					if (parentXbean instanceof CTAbsoluteAnchor) {
						CTAbsoluteAnchor ct = ((CTAbsoluteAnchor) (parentXbean));
					}


		}
		return anchor;
	}

	@Override
	public Iterator<XSSFShape> iterator() {
		return getShapes().iterator();
	}

	public XSSFSheet getSheet() {
		return ((XSSFSheet) (getParent()));
	}
}

