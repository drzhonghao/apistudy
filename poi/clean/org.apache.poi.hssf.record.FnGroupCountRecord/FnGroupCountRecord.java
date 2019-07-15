import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title: Function Group Count Record<P>
 * Description:  Number of built in function groups in the current version of the
 *               Spreadsheet (probably only used on Windoze)<P>
 * REFERENCE:  PG 315 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @version 2.0-pre
 */

public final class FnGroupCountRecord
    extends StandardRecord
{
    public final static short sid   = 0x9c;

    /**
     * suggested default (14 dec)
     */

    public final static short COUNT = 14;
    private short             field_1_count;

    public FnGroupCountRecord()
    {
    }

    public FnGroupCountRecord(RecordInputStream in)
    {
        field_1_count = in.readShort();
    }

    /**
     * set the number of built-in functions
     *
     * @param count - number of functions
     */

    public void setCount(short count)
    {
        field_1_count = count;
    }

    /**
     * get the number of built-in functions
     *
     * @return number of built-in functions
     */

    public short getCount()
    {
        return field_1_count;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[FNGROUPCOUNT]\n");
        buffer.append("    .count            = ").append(getCount())
            .append("\n");
        buffer.append("[/FNGROUPCOUNT]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(getCount());
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }
}
