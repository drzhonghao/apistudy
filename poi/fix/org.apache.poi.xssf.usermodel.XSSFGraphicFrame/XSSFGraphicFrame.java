

import javax.xml.namespace.QName;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.xssf.usermodel.XSSFAnchor;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObjectData;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualGraphicFrameProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGraphicalObjectFrame;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGraphicalObjectFrameNonVisual;
import org.openxmlformats.schemas.officeDocument.x2006.relationships.STRelationshipId;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGraphicalObjectFrame.Factory.newInstance;


public final class XSSFGraphicFrame extends XSSFShape {
	private static CTGraphicalObjectFrame prototype;

	private CTGraphicalObjectFrame graphicFrame;

	protected XSSFGraphicFrame(XSSFDrawing drawing, CTGraphicalObjectFrame ctGraphicFrame) {
		this.drawing = drawing;
		this.graphicFrame = ctGraphicFrame;
		CTGraphicalObjectData graphicData = graphicFrame.getGraphic().getGraphicData();
		if (graphicData != null) {
			NodeList nodes = graphicData.getDomNode().getChildNodes();
			for (int i = 0; i < (nodes.getLength()); i++) {
				final Node node = nodes.item(i);
				if (node.getNodeName().equals("c:chart")) {
					POIXMLDocumentPart relation = drawing.getRelationById(node.getAttributes().getNamedItem("r:id").getNodeValue());
					if (relation instanceof XSSFChart) {
					}
				}
			}
		}
	}

	@org.apache.poi.util.Internal
	public CTGraphicalObjectFrame getCTGraphicalObjectFrame() {
		return graphicFrame;
	}

	protected static CTGraphicalObjectFrame prototype() {
		if ((XSSFGraphicFrame.prototype) == null) {
			CTGraphicalObjectFrame graphicFrame = newInstance();
			CTGraphicalObjectFrameNonVisual nvGraphic = graphicFrame.addNewNvGraphicFramePr();
			CTNonVisualDrawingProps props = nvGraphic.addNewCNvPr();
			props.setId(0);
			props.setName("Diagramm 1");
			nvGraphic.addNewCNvGraphicFramePr();
			CTTransform2D transform = graphicFrame.addNewXfrm();
			CTPositiveSize2D extPoint = transform.addNewExt();
			CTPoint2D offPoint = transform.addNewOff();
			extPoint.setCx(0);
			extPoint.setCy(0);
			offPoint.setX(0);
			offPoint.setY(0);
			graphicFrame.addNewGraphic();
			XSSFGraphicFrame.prototype = graphicFrame;
		}
		return XSSFGraphicFrame.prototype;
	}

	public void setMacro(String macro) {
		graphicFrame.setMacro(macro);
	}

	public void setName(String name) {
		getNonVisualProperties().setName(name);
	}

	public String getName() {
		return getNonVisualProperties().getName();
	}

	private CTNonVisualDrawingProps getNonVisualProperties() {
		CTGraphicalObjectFrameNonVisual nvGraphic = graphicFrame.getNvGraphicFramePr();
		return nvGraphic.getCNvPr();
	}

	protected void setAnchor(XSSFClientAnchor anchor) {
		this.anchor = anchor;
	}

	public XSSFClientAnchor getAnchor() {
		return ((XSSFClientAnchor) (anchor));
	}

	protected void setChart(XSSFChart chart, String relId) {
		CTGraphicalObjectData data = graphicFrame.getGraphic().addNewGraphicData();
		appendChartElement(data, relId);
	}

	public long getId() {
		return graphicFrame.getNvGraphicFramePr().getCNvPr().getId();
	}

	protected void setId(long id) {
		graphicFrame.getNvGraphicFramePr().getCNvPr().setId(id);
	}

	private void appendChartElement(CTGraphicalObjectData data, String id) {
		String r_namespaceUri = STRelationshipId.type.getName().getNamespaceURI();
		XmlCursor cursor = data.newCursor();
		cursor.toNextToken();
		cursor.insertAttributeWithValue(new QName(r_namespaceUri, "id", "r"), id);
		cursor.dispose();
	}

	@Override
	protected CTShapeProperties getShapeProperties() {
		return null;
	}

	@Override
	public String getShapeName() {
		return graphicFrame.getNvGraphicFramePr().getCNvPr().getName();
	}
}

