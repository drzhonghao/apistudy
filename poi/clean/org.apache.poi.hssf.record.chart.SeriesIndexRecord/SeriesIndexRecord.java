import org.apache.poi.hssf.record.chart.*;


import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * links a series to its position in the series list.
 */
public final class SeriesIndexRecord extends StandardRecord {
    public final static short      sid                             = 0x1065;
    private  short      field_1_index;


    public SeriesIndexRecord()
    {

    }

    public SeriesIndexRecord(RecordInputStream in)
    {
        field_1_index                  = in.readShort();
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[SINDEX]\n");
        buffer.append("    .index                = ")
            .append("0x").append(HexDump.toHex(  getIndex ()))
            .append(" (").append( getIndex() ).append(" )");
        buffer.append(System.getProperty("line.separator")); 

        buffer.append("[/SINDEX]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(field_1_index);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    public Object clone() {
        SeriesIndexRecord rec = new SeriesIndexRecord();
    
        rec.field_1_index = field_1_index;
        return rec;
    }




    /**
     * Get the index field for the SeriesIndex record.
     */
    public short getIndex()
    {
        return field_1_index;
    }

    /**
     * Set the index field for the SeriesIndex record.
     */
    public void setIndex(short field_1_index)
    {
        this.field_1_index = field_1_index;
    }
}
