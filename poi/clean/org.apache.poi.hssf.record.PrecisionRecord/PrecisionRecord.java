import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Precision Record<P>
 * Description:  defines whether to store with full precision or what's displayed by the gui
 *               (meaning have really screwed up and skewed figures or only think you do!)<P>
 * REFERENCE:  PG 372 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @version 2.0-pre
 */
public final class PrecisionRecord extends StandardRecord {
    public final static short sid = 0xE;
    public short              field_1_precision;

    public PrecisionRecord() {
    }

    public PrecisionRecord(RecordInputStream in)
    {
        field_1_precision = in.readShort();
    }

    /**
     * set whether to use full precision or just skew all you figures all to hell.
     *
     * @param fullprecision - or not
     */
    public void setFullPrecision(boolean fullprecision) {
        if (fullprecision) {
            field_1_precision = 1;
        } else {
            field_1_precision = 0;
        }
    }

    /**
     * get whether to use full precision or just skew all you figures all to hell.
     *
     * @return fullprecision - or not
     */
    public boolean getFullPrecision()
    {
        return (field_1_precision == 1);
    }

    public String toString() {
        return "[PRECISION]\n" +
                "    .precision       = " + getFullPrecision() +
                "\n" +
                "[/PRECISION]\n";
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(field_1_precision);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }
}
