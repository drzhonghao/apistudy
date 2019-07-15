import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title: Double Stream Flag Record (0x0161)<p>
 * Description:  tells if this is a double stream file. (always no for HSSF generated files)<p>
 *               Double Stream files contain both BIFF8 and BIFF7 workbooks.<p>
 * REFERENCE:  PG 305 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 */
public final class DSFRecord extends StandardRecord {
    public final static short sid = 0x0161;

    private static final BitField biff5BookStreamFlag = BitFieldFactory.getInstance(0x0001);

    private int _options;

    private DSFRecord(int options) {
        _options = options;
    }
    public DSFRecord(boolean isBiff5BookStreamPresent) {
        this(0);
        _options = biff5BookStreamFlag.setBoolean(0, isBiff5BookStreamPresent);
    }

    public DSFRecord(RecordInputStream in) {
        this(in.readShort());
    }

    public boolean isBiff5BookStreamPresent() {
        return biff5BookStreamFlag.isSet(_options);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[DSF]\n");
        buffer.append("    .options = ").append(HexDump.shortToHex(_options)).append("\n");
        buffer.append("[/DSF]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(_options);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid() {
        return sid;
    }
}
