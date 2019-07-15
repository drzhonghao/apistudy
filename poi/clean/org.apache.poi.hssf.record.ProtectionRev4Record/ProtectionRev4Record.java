import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Protection Revision 4 Record (0x01AF)<p>
 * Description:  describes whether this is a protected shared/tracked workbook<p>
 * REFERENCE:  PG 373 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 */
public final class ProtectionRev4Record extends StandardRecord {
    public final static short sid = 0x01AF;

    private static final BitField protectedFlag = BitFieldFactory.getInstance(0x0001);

    private int _options;

    private ProtectionRev4Record(int options) {
        _options = options;
    }

    public ProtectionRev4Record(boolean protect) {
        this(0);
        setProtect(protect);
    }

    public ProtectionRev4Record(RecordInputStream in) {
        this(in.readUShort());
    }

    /**
     * set whether the this is protected shared/tracked workbook or not
     * @param protect  whether to protect the workbook or not
     */
    public void setProtect(boolean protect) {
        _options = protectedFlag.setBoolean(_options, protect);
    }

    /**
     * get whether the this is protected shared/tracked workbook or not
     * @return whether to protect the workbook or not
     */
    public boolean getProtect() {
        return protectedFlag.isSet(_options);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[PROT4REV]\n");
        buffer.append("    .options = ").append(HexDump.shortToHex(_options)).append("\n");
        buffer.append("[/PROT4REV]\n");
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
