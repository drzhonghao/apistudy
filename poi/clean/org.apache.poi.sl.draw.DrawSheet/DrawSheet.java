import org.apache.poi.sl.draw.Drawable;
import org.apache.poi.sl.draw.*;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.Sheet;


public class DrawSheet implements Drawable {

    protected final Sheet<?,?> sheet;
    
    public DrawSheet(Sheet<?,?> sheet) {
        this.sheet = sheet;
    }
    
    @Override
    public void draw(Graphics2D graphics) {
        Dimension dim = sheet.getSlideShow().getPageSize();
        Color whiteTrans = new Color(1f,1f,1f,0f);
        graphics.setColor(whiteTrans);
        graphics.fillRect(0, 0, (int)dim.getWidth(), (int)dim.getHeight());
        
        DrawFactory drawFact = DrawFactory.getInstance(graphics);
        MasterSheet<?,?> master = sheet.getMasterSheet();
        
        if(sheet.getFollowMasterGraphics() && master != null) {
            Drawable drawer = drawFact.getDrawable(master);
            drawer.draw(graphics);
        }
        
        graphics.setRenderingHint(Drawable.GROUP_TRANSFORM, new AffineTransform());

        for (Shape<?,?> shape : sheet.getShapes()) {
            if(!canDraw(graphics, shape)) {
                continue;
            }
            
            // remember the initial transform and restore it after we are done with drawing
            AffineTransform at = graphics.getTransform();

            // concrete implementations can make sense of this hint,
            // for example PSGraphics2D or PDFGraphics2D would call gsave() / grestore
            graphics.setRenderingHint(Drawable.GSAVE, true);

            // apply rotation and flipping
            Drawable drawer = drawFact.getDrawable(shape);
            drawer.applyTransform(graphics);
            // draw stuff
            drawer.draw(graphics);

            // restore the coordinate system
            graphics.setTransform(at);

            graphics.setRenderingHint(Drawable.GRESTORE, true);
        }
    }

    @Override
    public void applyTransform(Graphics2D context) {
    }

    @Override
    public void drawContent(Graphics2D context) {
    }

    /**
     * Checks if this <code>sheet</code> displays the specified shape.
     *
     * Subclasses can override it and skip certain shapes from drawings,
     * for instance, slide masters and layouts don't display placeholders
     */
    protected boolean canDraw(Graphics2D graphics, Shape<?,?> shape){
        return true;
    }
}
