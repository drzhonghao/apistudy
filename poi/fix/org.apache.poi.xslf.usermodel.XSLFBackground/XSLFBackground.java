

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.sl.usermodel.Background;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFColor;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTheme;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNoFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSchemeColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTStyleMatrixReference;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.presentationml.x2006.main.CTBackground;
import org.openxmlformats.schemas.presentationml.x2006.main.CTBackgroundProperties;


public class XSLFBackground extends XSLFSimpleShape implements Background<XSLFShape, XSLFTextParagraph> {
	@Override
	public Rectangle2D getAnchor() {
		Dimension pg = getSheet().getSlideShow().getPageSize();
		return new Rectangle2D.Double(0, 0, pg.getWidth(), pg.getHeight());
	}

	@Override
	protected CTTransform2D getXfrm(boolean create) {
		return null;
	}

	@Override
	public void setPlaceholder(Placeholder placeholder) {
		throw new POIXMLException("Can't set a placeholder for a background");
	}

	protected CTBackgroundProperties getBgPr(boolean create) {
		CTBackground bg = ((CTBackground) (getXmlObject()));
		if ((!(bg.isSetBgPr())) && create) {
			if (bg.isSetBgRef()) {
				bg.unsetBgRef();
			}
			return bg.addNewBgPr();
		}
		return bg.getBgPr();
	}

	public void setFillColor(Color color) {
		CTBackgroundProperties bgPr = getBgPr(true);
		if (bgPr.isSetBlipFill()) {
			bgPr.unsetBlipFill();
		}
		if (bgPr.isSetGradFill()) {
			bgPr.unsetGradFill();
		}
		if (bgPr.isSetGrpFill()) {
			bgPr.unsetGrpFill();
		}
		if (bgPr.isSetPattFill()) {
			bgPr.unsetPattFill();
		}
		if (color == null) {
			if (bgPr.isSetSolidFill()) {
				bgPr.unsetSolidFill();
			}
			if (!(bgPr.isSetNoFill())) {
				bgPr.addNewNoFill();
			}
		}else {
			if (bgPr.isSetNoFill()) {
				bgPr.unsetNoFill();
			}
			CTSolidColorFillProperties fill = (bgPr.isSetSolidFill()) ? bgPr.getSolidFill() : bgPr.addNewSolidFill();
			XSLFColor col = new XSLFColor(fill, getSheet().getTheme(), fill.getSchemeClr());
		}
	}

	@Override
	protected XmlObject getShapeProperties() {
		CTBackground bg = ((CTBackground) (getXmlObject()));
		if (bg.isSetBgPr()) {
			return bg.getBgPr();
		}else
			if (bg.isSetBgRef()) {
				return bg.getBgRef();
			}else {
				return null;
			}

	}
}

