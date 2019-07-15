import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Delta Record (0x0010)<p>
 * Description:  controls the accuracy of the calculations<p>
 * REFERENCE:  PG 303 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 */
public final class DeltaRecord extends StandardRecord implements Cloneable {
    public final static short sid = 0x0010;
    public final static double DEFAULT_VALUE = 0.0010;   // should be .001

    // a double is an IEEE 8-byte float...damn IEEE and their goofy standards an
    // ambiguous numeric identifiers
    private double field_1_max_change;

    public DeltaRecord(double maxChange) {
        field_1_max_change = maxChange;
    }

    public DeltaRecord(RecordInputStream in) {
        field_1_max_change = in.readDouble();
    }

    /**
     * get the maximum change
     * @return maxChange - maximum rounding error
     */
    public double getMaxChange() {
        return field_1_max_change;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[DELTA]\n");
        buffer.append("    .maxchange = ").append(getMaxChange()).append("\n");
        buffer.append("[/DELTA]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeDouble(getMaxChange());
    }

    protected int getDataSize() {
        return 8;
    }

    public short getSid() {
        return sid;
    }

    @Override
    public DeltaRecord clone() {
        // immutable
        return this;
    }
}
