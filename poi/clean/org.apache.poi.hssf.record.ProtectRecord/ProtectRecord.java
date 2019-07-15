import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title: Protect Record (0x0012)<p>
 * Description:  defines whether a sheet or workbook is protected (HSSF DOES NOT SUPPORT ENCRYPTION)<p>
 * HSSF now supports the simple "protected" sheets (where they are not encrypted and open office et al
 * ignore the password record entirely).
 * REFERENCE:  PG 373 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 */
public final class ProtectRecord extends StandardRecord {
    public final static short sid = 0x0012;

    private static final BitField protectFlag = BitFieldFactory.getInstance(0x0001);

    private int _options;

    private ProtectRecord(int options) {
        _options = options;
    }

    public ProtectRecord(boolean isProtected) {
        this(0);
        setProtect(isProtected);
    }

    public ProtectRecord(RecordInputStream in) {
        this(in.readShort());
    }

    /**
     * set whether the sheet is protected or not
     * @param protect whether to protect the sheet or not
     */
    public void setProtect(boolean protect) {
        _options = protectFlag.setBoolean(_options, protect);
    }

    /**
     * get whether the sheet is protected or not
     * @return whether to protect the sheet or not
     */
    public boolean getProtect() {
        return protectFlag.isSet(_options);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[PROTECT]\n");
        buffer.append("    .options = ").append(HexDump.shortToHex(_options)).append("\n");
        buffer.append("[/PROTECT]\n");
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

    public Object clone() {
        return new ProtectRecord(_options);
    }
}
