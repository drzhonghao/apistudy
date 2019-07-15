import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Record for the right margin.
 */
public final class RightMarginRecord extends StandardRecord implements Margin {
    public final static short sid = 0x27;
    private double field_1_margin;

    public RightMarginRecord()    {    }

    public RightMarginRecord( RecordInputStream in )
    {
        field_1_margin = in.readDouble();
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "[RightMargin]\n" );
        buffer.append( "    .margin               = " ).append( " (" ).append( getMargin() ).append( " )\n" );
        buffer.append( "[/RightMargin]\n" );
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeDouble(field_1_margin);
    }

    protected int getDataSize() {
        return 8;
    }

    public short getSid()    {        return sid;    }

    /**
     * Get the margin field for the RightMargin record.
     */
    public double getMargin()    {        return field_1_margin;    }

    /**
     * Set the margin field for the RightMargin record.
     */
    public void setMargin( double field_1_margin )
    {        this.field_1_margin = field_1_margin;    }

    public Object clone()
    {
        RightMarginRecord rec = new RightMarginRecord();
        rec.field_1_margin = this.field_1_margin;
        return rec;
    }
}  // END OF
