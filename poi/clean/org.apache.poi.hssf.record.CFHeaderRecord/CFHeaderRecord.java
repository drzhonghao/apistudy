import org.apache.poi.hssf.record.CFHeaderBase;
import org.apache.poi.hssf.record.*;


import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Conditional Formatting Header record CFHEADER (0x01B0).
 * Used to describe a {@link CFRuleRecord}.
 * @see CFHeader12Record
 */
public final class CFHeaderRecord extends CFHeaderBase implements Cloneable {
    public static final short sid = 0x01B0;

    /** Creates new CFHeaderRecord */
    public CFHeaderRecord() {
        createEmpty();
    }
    public CFHeaderRecord(CellRangeAddress[] regions, int nRules) {
        super(regions, nRules);
    }

    public CFHeaderRecord(RecordInputStream in) {
        read(in);
    }

    protected String getRecordName() {
        return "CFHEADER";
    }

    public short getSid() {
        return sid;
    }

    @Override
    public CFHeaderRecord clone() {
        CFHeaderRecord result = new CFHeaderRecord();
        super.copyTo(result);
        return result;
    }
}
