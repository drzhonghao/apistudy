import org.apache.poi.sl.draw.geom.*;


import java.awt.geom.Path2D;

import org.apache.poi.sl.draw.binding.CTAdjPoint2D;

public class CurveToCommand implements PathCommand {
    private String arg1, arg2, arg3, arg4, arg5, arg6;

    CurveToCommand(CTAdjPoint2D pt1, CTAdjPoint2D pt2, CTAdjPoint2D pt3){
        arg1 = pt1.getX();
        arg2 = pt1.getY();
        arg3 = pt2.getX();
        arg4 = pt2.getY();
        arg5 = pt3.getX();
        arg6 = pt3.getY();
    }

    @Override
    public void execute(Path2D.Double path, Context ctx){
        double x1 = ctx.getValue(arg1);
        double y1 = ctx.getValue(arg2);
        double x2 = ctx.getValue(arg3);
        double y2 = ctx.getValue(arg4);
        double x3 = ctx.getValue(arg5);
        double y3 = ctx.getValue(arg6);
        path.curveTo(x1, y1, x2, y2, x3, y3);
    }
}
