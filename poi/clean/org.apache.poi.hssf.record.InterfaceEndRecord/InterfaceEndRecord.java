import org.apache.poi.hssf.record.InterfaceHdrRecord;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.RecordFormatException;

/**
 * Title: Interface End Record (0x00E2)<P>
 * Description: Shows where the Interface Records end (MMS)
 *  (has no fields)<P>
 * REFERENCE:  PG 324 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 */
public final class InterfaceEndRecord extends StandardRecord {

    public static final short sid = 0x00E2;
    public static final InterfaceEndRecord instance = new InterfaceEndRecord();

    private InterfaceEndRecord() {
        // enforce singleton
    }

    public static Record create(RecordInputStream in) {
        switch (in.remaining()) {
            case 0:
                return instance;
            case 2:
                return new InterfaceHdrRecord(in);
        }
        throw new RecordFormatException("Invalid record data size: " + in.remaining());
    }

    public String toString() {
        return "[INTERFACEEND/]\n";
    }

    public void serialize(LittleEndianOutput out) {
        // no instance data
    }

    protected int getDataSize() {
        return 0;
    }

    public short getSid() {
        return sid;
    }
}
