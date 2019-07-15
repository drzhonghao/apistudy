import org.apache.poi.hssf.record.cf.Threshold;
import org.apache.poi.hssf.record.cf.*;


import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Icon / Multi-State specific Threshold / value (CFVO),
 *  for changes in Conditional Formatting
 */
public final class IconMultiStateThreshold extends Threshold implements Cloneable {
    /**
     * Cell values that are equal to the threshold value do not pass the threshold
     */
    public static final byte EQUALS_EXCLUDE = 0;
    /**
     * Cell values that are equal to the threshold value pass the threshold.
     */
    public static final byte EQUALS_INCLUDE = 1;
    
    private byte equals;

    public IconMultiStateThreshold() {
        super();
        equals = EQUALS_INCLUDE;
    }

    /** Creates new Ico Multi-State Threshold */
    public IconMultiStateThreshold(LittleEndianInput in) {
        super(in);
        equals = in.readByte();
        // Reserved, 4 bytes, all 0
        in.readInt();
    }

    public byte getEquals() {
        return equals;
    }
    public void setEquals(byte equals) {
        this.equals = equals;
    }

    public int getDataLength() {
        return super.getDataLength() + 5;
    }

    @Override
    public IconMultiStateThreshold clone() {
      IconMultiStateThreshold rec = new IconMultiStateThreshold();
      super.copyTo(rec);
      rec.equals = equals;
      return rec;
    }

    public void serialize(LittleEndianOutput out) {
        super.serialize(out);
        out.writeByte(equals);
        out.writeInt(0); // Reserved
    }
}
