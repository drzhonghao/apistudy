import org.apache.poi.xslf.usermodel.*;


import java.awt.Color;
import java.awt.geom.Rectangle2D;

import org.apache.poi.sl.draw.DrawPaint;
import org.apache.poi.sl.usermodel.PaintStyle.SolidPaint;
import org.apache.poi.sl.usermodel.Shadow;
import org.apache.poi.util.Units;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOuterShadowEffect;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSchemeColor;

/**
 * Represents a shadow of a shape. For now supports only outer shadows.
 *
 * @author Yegor Kozlov
 */
public class XSLFShadow extends XSLFShape implements Shadow<XSLFShape,XSLFTextParagraph> {

    private XSLFSimpleShape _parent;

    /* package */XSLFShadow(CTOuterShadowEffect shape, XSLFSimpleShape parentShape) {
        super(shape, parentShape.getSheet());

        _parent = parentShape;
    }

    @Override
    public XSLFSimpleShape getShadowParent() {
        return _parent;
    }

    @Override
    public Rectangle2D getAnchor(){
        return _parent.getAnchor();
    }

    public void setAnchor(Rectangle2D anchor){
        throw new IllegalStateException("You can't set anchor of a shadow");
    }

    /**
     * @return the offset of this shadow in points
     */
    public double getDistance(){
        CTOuterShadowEffect ct = (CTOuterShadowEffect)getXmlObject();
        return ct.isSetDist() ? Units.toPoints(ct.getDist()) : 0;        
    }

    /**
     * 
     * @return the direction to offset the shadow in angles
     */
    public double getAngle(){
        CTOuterShadowEffect ct = (CTOuterShadowEffect)getXmlObject();
        return ct.isSetDir() ? (double)ct.getDir() / 60000 : 0;
    }

    /**
     * 
     * @return the blur radius of the shadow
     * TODO: figure out how to make sense of this property when rendering shadows 
     */
    public double getBlur(){
        CTOuterShadowEffect ct = (CTOuterShadowEffect)getXmlObject();
        return ct.isSetBlurRad() ? Units.toPoints(ct.getBlurRad()) : 0;
    }

    /**
     * @return the color of this shadow. 
     * Depending whether the parent shape is filled or stroked, this color is used to fill or stroke this shadow
     */
    public Color getFillColor() {
        SolidPaint ps = getFillStyle();
        if (ps == null) return null;
        return DrawPaint.applyColorTransform(ps.getSolidColor());
    }

    @Override
    public SolidPaint getFillStyle() {
        XSLFTheme theme = getSheet().getTheme();
        CTOuterShadowEffect ct = (CTOuterShadowEffect)getXmlObject();
        if(ct == null) return null;
            
        CTSchemeColor phClr = ct.getSchemeClr();
        final XSLFColor xc = new XSLFColor(ct, theme, phClr);
        return DrawPaint.createSolidPaint(xc.getColorStyle());
    }
}
