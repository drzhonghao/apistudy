import org.apache.poi.hssf.record.CFHeaderBase;
import org.apache.poi.hssf.record.*;


import org.apache.poi.hssf.record.common.FtrHeader;
import org.apache.poi.hssf.record.common.FutureRecord;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Conditional Formatting Header v12 record CFHEADER12 (0x0879),
 *  for conditional formattings introduced in Excel 2007 and newer.
 */
public final class CFHeader12Record extends CFHeaderBase implements FutureRecord, Cloneable {
    public static final short sid = 0x0879;

    private FtrHeader futureHeader;

    /** Creates new CFHeaderRecord */
    public CFHeader12Record() {
        createEmpty();
        futureHeader = new FtrHeader();
        futureHeader.setRecordType(sid);
    }
    public CFHeader12Record(CellRangeAddress[] regions, int nRules) {
        super(regions, nRules);
        futureHeader = new FtrHeader();
        futureHeader.setRecordType(sid);
    }
    public CFHeader12Record(RecordInputStream in) {
        futureHeader = new FtrHeader(in);
        read(in);
    }

    @Override
    protected String getRecordName() {
        return "CFHEADER12";
    }

    protected int getDataSize() {
        return FtrHeader.getDataSize() + super.getDataSize();
    }

    public void serialize(LittleEndianOutput out) {
        // Sync the associated range
        futureHeader.setAssociatedRange(getEnclosingCellRange());
        // Write the future header first
        futureHeader.serialize(out);
        // Then the rest of the CF Header details
        super.serialize(out);
    }

    public short getSid() {
        return sid;
    }

    public short getFutureRecordType() {
        return futureHeader.getRecordType();
    }
    public FtrHeader getFutureHeader() {
        return futureHeader;
    }
    public CellRangeAddress getAssociatedRange() {
        return futureHeader.getAssociatedRange();
    }
    
    @Override
    public CFHeader12Record clone() {
        CFHeader12Record result = new CFHeader12Record();
        result.futureHeader = (FtrHeader)futureHeader.clone();
        super.copyTo(result);
        return result;
    }
}
