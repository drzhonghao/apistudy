import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.*;

/**
 * Record for the top margin.
 */
public final class TopMarginRecord extends StandardRecord implements Margin {
    public final static short sid = 0x28;
    private double field_1_margin;

    public TopMarginRecord()    {    }

    /**
     * @param in the RecordInputstream to read the record from
     */
    public TopMarginRecord( RecordInputStream in )
    {
        field_1_margin = in.readDouble();
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "[TopMargin]\n" );
        buffer.append( "    .margin               = " ).append( " (" ).append( getMargin() ).append( " )\n" );
        buffer.append( "[/TopMargin]\n" );
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
     * Get the margin field for the TopMargin record.
     */
    public double getMargin()    {        return field_1_margin;    }

    /**
     * Set the margin field for the TopMargin record.
     */
    public void setMargin( double field_1_margin )
    {        this.field_1_margin = field_1_margin;    }

    public Object clone()
    {
        TopMarginRecord rec = new TopMarginRecord();
        rec.field_1_margin = this.field_1_margin;
        return rec;
    }
}  // END OF 
