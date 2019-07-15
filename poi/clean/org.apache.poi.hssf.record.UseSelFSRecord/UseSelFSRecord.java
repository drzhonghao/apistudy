import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        USESELFS (0x0160) - Use Natural Language Formulas Flag <p>
 * Description:  Tells the GUI if this was written by something that can use
 *               "natural language" formulas. HSSF can't.<p>
 * REFERENCE:  PG 420 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)
 */
public final class UseSelFSRecord extends StandardRecord {
    public final static short sid   = 0x0160;

    private static final BitField useNaturalLanguageFormulasFlag = BitFieldFactory.getInstance(0x0001);

    private int _options;

    private UseSelFSRecord(int options) {
        _options = options;
    }

    public UseSelFSRecord(RecordInputStream in) {
        this(in.readUShort());
    }

    public UseSelFSRecord(boolean b) {
        this(0);
        _options = useNaturalLanguageFormulasFlag.setBoolean(_options, b);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[USESELFS]\n");
        buffer.append("    .options = ").append(HexDump.shortToHex(_options)).append("\n");
        buffer.append("[/USESELFS]\n");
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

    @Override
    public Object clone() {
        return new UseSelFSRecord(_options);
    }
}
