import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title: Interface Header Record (0x00E1)<P>
 * Description: Defines the beginning of Interface records (MMS)<P>
 * REFERENCE:  PG 324 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 */
public final class InterfaceHdrRecord extends StandardRecord {
    public final static short sid = 0x00E1;
    private final int _codepage;

    /**
     * suggested (and probably correct) default
     */
    public final static int CODEPAGE = 0x04B0;

    public InterfaceHdrRecord(int codePage) {
        _codepage = codePage;
    }

    public InterfaceHdrRecord(RecordInputStream in) {
        _codepage = in.readShort();
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[INTERFACEHDR]\n");
        buffer.append("    .codepage = ").append(HexDump.shortToHex(_codepage)).append("\n");
        buffer.append("[/INTERFACEHDR]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(_codepage);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid() {
        return sid;
    }
}
