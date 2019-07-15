import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Protection Revision 4 password Record (0x01BC)<p>
 * Description:  Stores the (2 byte??!!) encrypted password for a shared workbook<p>
 * REFERENCE:  PG 374 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 */
public final class PasswordRev4Record extends StandardRecord {
    public final static short sid = 0x01BC;
    private int field_1_password;

    public PasswordRev4Record(int pw) {
        field_1_password = pw;
    }

    public PasswordRev4Record(RecordInputStream in) {
        field_1_password = in.readShort();
    }

    /**
     * set the password
     *
     * @param pw  representing the password
     */
    public void setPassword(short pw) {
        field_1_password = pw;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[PROT4REVPASSWORD]\n");
        buffer.append("    .password = ").append(HexDump.shortToHex(field_1_password)).append("\n");
        buffer.append("[/PROT4REVPASSWORD]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(field_1_password);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid() {
        return sid;
    }
}
