import org.apache.poi.sl.draw.geom.*;


import java.awt.geom.Path2D;

import org.apache.poi.sl.draw.binding.CTAdjPoint2D;

public class LineToCommand implements PathCommand {
    private String arg1, arg2;

    LineToCommand(CTAdjPoint2D pt){
        arg1 = pt.getX();
        arg2 = pt.getY();
    }

    LineToCommand(String s1, String s2){
        arg1 = s1;
        arg2 = s2;
    }

    @Override
    public void execute(Path2D.Double path, Context ctx){
        double x = ctx.getValue(arg1);
        double y = ctx.getValue(arg2);
        path.lineTo(x, y);
    }
}
