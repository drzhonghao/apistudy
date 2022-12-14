import org.apache.poi.sl.draw.DrawFactory;
import org.apache.poi.sl.draw.*;


import java.awt.Graphics2D;

import org.apache.poi.sl.usermodel.GraphicalFrame;
import org.apache.poi.sl.usermodel.PictureShape;


public class DrawGraphicalFrame extends DrawShape {

    public DrawGraphicalFrame(GraphicalFrame<?,?> shape) {
        super(shape);
    }
    
    public void draw(Graphics2D context) {
        PictureShape<?,?> ps = ((GraphicalFrame<?,?>)getShape()).getFallbackPicture();
        if (ps == null) {
            return;
        }
        DrawPictureShape dps = DrawFactory.getInstance(context).getDrawable(ps);
        dps.draw(context);
    }
}
