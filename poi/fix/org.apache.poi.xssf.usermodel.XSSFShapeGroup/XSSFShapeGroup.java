

import java.util.Iterator;
import org.apache.poi.ss.usermodel.ShapeContainer;
import org.apache.poi.xssf.usermodel.XSSFAnchor;
import org.apache.poi.xssf.usermodel.XSSFChildAnchor;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFConnector;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.apache.poi.xssf.usermodel.XSSFTextBox;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGroupShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGroupTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualGroupDrawingShapeProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTConnector;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGroupShape;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGroupShapeNonVisual;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTPicture;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTShape;

import static org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGroupShape.Factory.newInstance;


public final class XSSFShapeGroup extends XSSFShape implements ShapeContainer<XSSFShape> {
	private static CTGroupShape prototype;

	private CTGroupShape ctGroup;

	protected XSSFShapeGroup(XSSFDrawing drawing, CTGroupShape ctGroup) {
		this.drawing = drawing;
		this.ctGroup = ctGroup;
	}

	protected static CTGroupShape prototype() {
		if ((XSSFShapeGroup.prototype) == null) {
			CTGroupShape shape = newInstance();
			CTGroupShapeNonVisual nv = shape.addNewNvGrpSpPr();
			CTNonVisualDrawingProps nvpr = nv.addNewCNvPr();
			nvpr.setId(0);
			nvpr.setName("Group 0");
			nv.addNewCNvGrpSpPr();
			CTGroupShapeProperties sp = shape.addNewGrpSpPr();
			CTGroupTransform2D t2d = sp.addNewXfrm();
			CTPositiveSize2D p1 = t2d.addNewExt();
			p1.setCx(0);
			p1.setCy(0);
			CTPoint2D p2 = t2d.addNewOff();
			p2.setX(0);
			p2.setY(0);
			CTPositiveSize2D p3 = t2d.addNewChExt();
			p3.setCx(0);
			p3.setCy(0);
			CTPoint2D p4 = t2d.addNewChOff();
			p4.setX(0);
			p4.setY(0);
			XSSFShapeGroup.prototype = shape;
		}
		return XSSFShapeGroup.prototype;
	}

	public XSSFTextBox createTextbox(XSSFChildAnchor anchor) {
		CTShape ctShape = ctGroup.addNewSp();
		return null;
	}

	public XSSFSimpleShape createSimpleShape(XSSFChildAnchor anchor) {
		CTShape ctShape = ctGroup.addNewSp();
		return null;
	}

	public XSSFConnector createConnector(XSSFChildAnchor anchor) {
		CTConnector ctShape = ctGroup.addNewCxnSp();
		return null;
	}

	public XSSFPicture createPicture(XSSFClientAnchor anchor, int pictureIndex) {
		CTPicture ctShape = ctGroup.addNewPic();
		return null;
	}

	public XSSFShapeGroup createGroup(XSSFChildAnchor anchor) {
		CTGroupShape ctShape = ctGroup.addNewGrpSp();
		ctShape.set(XSSFShapeGroup.prototype());
		XSSFShapeGroup shape = new XSSFShapeGroup(getDrawing(), ctShape);
		shape.anchor = anchor;
		CTGroupTransform2D xfrm = shape.getCTGroupShape().getGrpSpPr().getXfrm();
		CTTransform2D t2 = anchor.getCTTransform2D();
		xfrm.setOff(t2.getOff());
		xfrm.setExt(t2.getExt());
		xfrm.setChExt(t2.getExt());
		xfrm.setFlipH(t2.getFlipH());
		xfrm.setFlipV(t2.getFlipV());
		return shape;
	}

	@org.apache.poi.util.Internal
	public CTGroupShape getCTGroupShape() {
		return ctGroup;
	}

	public void setCoordinates(int x1, int y1, int x2, int y2) {
		CTGroupTransform2D t2d = ctGroup.getGrpSpPr().getXfrm();
		CTPoint2D off = t2d.getOff();
		off.setX(x1);
		off.setY(y1);
		CTPositiveSize2D ext = t2d.getExt();
		ext.setCx(x2);
		ext.setCy(y2);
		CTPoint2D chOff = t2d.getChOff();
		chOff.setX(x1);
		chOff.setY(y1);
		CTPositiveSize2D chExt = t2d.getChExt();
		chExt.setCx(x2);
		chExt.setCy(y2);
	}

	@Override
	protected CTShapeProperties getShapeProperties() {
		throw new IllegalStateException("Not supported for shape group");
	}

	@Override
	public Iterator<XSSFShape> iterator() {
		return null;
	}

	@Override
	public String getShapeName() {
		return ctGroup.getNvGrpSpPr().getCNvPr().getName();
	}
}

