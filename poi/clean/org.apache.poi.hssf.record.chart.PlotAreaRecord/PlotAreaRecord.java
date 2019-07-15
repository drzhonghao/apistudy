import org.apache.poi.hssf.record.chart.*;


import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.LittleEndianOutput;

/**
 * preceeds and identifies a frame as belonging to the plot area.
 */
public final class PlotAreaRecord extends StandardRecord {
    public final static short      sid                             = 0x1035;


    public PlotAreaRecord()
    {

    }

    /**
     * @param in unused (since this record has no data)
     */
    public PlotAreaRecord(RecordInputStream in)
    {

    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[PLOTAREA]\n");

        buffer.append("[/PLOTAREA]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
    }

    protected int getDataSize() {
        return 0;
    }

    public short getSid()
    {
        return sid;
    }

    public Object clone() {
        return new PlotAreaRecord();
    }
}
