import org.apache.poi.hssf.record.cf.Threshold;
import org.apache.poi.hssf.record.cf.*;


import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Color Gradient / Color Scale specific Threshold / value (CFVO),
 *  for changes in Conditional Formatting
 */
public final class ColorGradientThreshold extends Threshold implements Cloneable {
    private double position;

    public ColorGradientThreshold() {
        super();
        position = 0d;
    }

    /** Creates new Color Gradient Threshold */
    public ColorGradientThreshold(LittleEndianInput in) {
        super(in);
        position = in.readDouble();
    }

    public double getPosition() {
        return position;
    }
    public void setPosition(double position) {
        this.position = position;
    }

    public int getDataLength() {
        return super.getDataLength() + 8;
    }

    @Override
    public ColorGradientThreshold clone() {
      ColorGradientThreshold rec = new ColorGradientThreshold();
      super.copyTo(rec);
      rec.position = position;
      return rec;
    }

    public void serialize(LittleEndianOutput out) {
        super.serialize(out);
        out.writeDouble(position);
    }
}
