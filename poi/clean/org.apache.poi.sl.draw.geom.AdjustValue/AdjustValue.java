import org.apache.poi.sl.draw.geom.*;


import org.apache.poi.sl.draw.binding.CTGeomGuide;

/**
 * Represents a shape adjust values (see section 20.1.9.5 in the spec)
 */
public class AdjustValue extends Guide {

    public AdjustValue(CTGeomGuide gd) {
        super(gd.getName(), gd.getFmla());
    }

    @Override
    public double evaluate(Context ctx){
        String name = getName();
        Guide adj = ctx.getAdjustValue(name);
        return (adj != null) ? adj.evaluate(ctx) : super.evaluate(ctx);
    }
}
