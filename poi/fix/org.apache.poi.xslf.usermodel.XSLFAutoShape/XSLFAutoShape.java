

import org.apache.poi.sl.usermodel.AutoShape;
import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.text.XDDFTextBody;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuideList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingShapeProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetGeometry2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.STShapeType;
import org.openxmlformats.schemas.presentationml.x2006.main.CTApplicationNonVisualDrawingProps;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShapeNonVisual;

import static org.openxmlformats.schemas.presentationml.x2006.main.CTShape.Factory.newInstance;


@Beta
public class XSLFAutoShape extends XSLFTextShape implements AutoShape<XSLFShape, XSLFTextParagraph> {
	static XSLFAutoShape create(CTShape shape, XSLFSheet sheet) {
		if (shape.getSpPr().isSetCustGeom()) {
		}else
			if (shape.getNvSpPr().getCNvSpPr().isSetTxBox()) {
			}else {
			}

		return null;
	}

	static CTShape prototype(int shapeId) {
		CTShape ct = newInstance();
		CTShapeNonVisual nvSpPr = ct.addNewNvSpPr();
		CTNonVisualDrawingProps cnv = nvSpPr.addNewCNvPr();
		cnv.setName(("AutoShape " + shapeId));
		cnv.setId(shapeId);
		nvSpPr.addNewCNvSpPr();
		nvSpPr.addNewNvPr();
		CTShapeProperties spPr = ct.addNewSpPr();
		CTPresetGeometry2D prst = spPr.addNewPrstGeom();
		prst.setPrst(STShapeType.RECT);
		prst.addNewAvLst();
		return ct;
	}

	@Override
	protected CTTextBody getTextBody(boolean create) {
		CTShape shape = ((CTShape) (getXmlObject()));
		CTTextBody txBody = shape.getTxBody();
		if ((txBody == null) && create) {
			XDDFTextBody body = new XDDFTextBody(this);
			XSLFTextShape.initTextBody(body);
			shape.setTxBody(body.getXmlObject());
			txBody = shape.getTxBody();
		}
		return txBody;
	}

	@Override
	public String toString() {
		return (("[" + (getClass().getSimpleName())) + "] ") + (getShapeName());
	}
}

