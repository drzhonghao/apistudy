

import org.apache.poi.sl.usermodel.TextBox;
import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.text.XDDFTextBody;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
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
public class XSLFTextBox extends XSLFAutoShape implements TextBox<XSLFShape, XSLFTextParagraph> {
	static CTShape prototype(int shapeId) {
		CTShape ct = newInstance();
		CTShapeNonVisual nvSpPr = ct.addNewNvSpPr();
		CTNonVisualDrawingProps cnv = nvSpPr.addNewCNvPr();
		cnv.setName(("TextBox " + shapeId));
		cnv.setId(shapeId);
		nvSpPr.addNewCNvSpPr().setTxBox(true);
		nvSpPr.addNewNvPr();
		CTShapeProperties spPr = ct.addNewSpPr();
		CTPresetGeometry2D prst = spPr.addNewPrstGeom();
		prst.setPrst(STShapeType.RECT);
		prst.addNewAvLst();
		XDDFTextBody body = new XDDFTextBody(null);
		XSLFTextShape.initTextBody(body);
		ct.setTxBody(body.getXmlObject());
		return ct;
	}
}

