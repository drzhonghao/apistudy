import org.apache.poi.hssf.record.*;



import org.apache.poi.util.LittleEndianOutput;


/**
 * Record for the bottom margin.
 */
public final class BottomMarginRecord extends StandardRecord implements Margin, Cloneable {
    public final static short sid = 0x29;
    private double field_1_margin;

    public BottomMarginRecord()
    {

    }

    public BottomMarginRecord( RecordInputStream in )
    {
        field_1_margin = in.readDouble();
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "[BottomMargin]\n" );
        buffer.append( "    .margin               = " )
                .append( " (" ).append( getMargin() ).append( " )\n" );
        buffer.append( "[/BottomMargin]\n" );
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeDouble(field_1_margin);
    }

    protected int getDataSize() {
        return 8;
    }

    public short getSid()
    {
        return sid;
    }

    /**
     * Get the margin field for the BottomMargin record.
     */
    public double getMargin()
    {
        return field_1_margin;
    }

    /**
     * Set the margin field for the BottomMargin record.
     */
    public void setMargin( double field_1_margin )
    {
        this.field_1_margin = field_1_margin;
    }

    @Override
    public BottomMarginRecord clone() {
        BottomMarginRecord rec = new BottomMarginRecord();
        rec.field_1_margin = this.field_1_margin;
        return rec;
    }

}  // END OF 
