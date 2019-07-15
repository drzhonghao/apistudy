import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Specifies the window's zoom magnification.<p>
 * If this record isn't present then the windows zoom is 100%. see p384 Excel Dev Kit
 */
public final class SCLRecord extends StandardRecord {
    public final static short      sid                             = 0x00A0;
    private  short      field_1_numerator;
    private  short      field_2_denominator;


    public SCLRecord()
    {

    }

    public SCLRecord(RecordInputStream in)
    {
        field_1_numerator              = in.readShort();
        field_2_denominator            = in.readShort();
    }

    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[SCL]\n");
        buffer.append("    .numerator            = ")
            .append("0x").append(HexDump.toHex(  getNumerator ()))
            .append(" (").append( getNumerator() ).append(" )");
        buffer.append(System.getProperty("line.separator")); 
        buffer.append("    .denominator          = ")
            .append("0x").append(HexDump.toHex(  getDenominator ()))
            .append(" (").append( getDenominator() ).append(" )");
        buffer.append(System.getProperty("line.separator")); 

        buffer.append("[/SCL]\n");
        return buffer.toString();
    }

    @Override
    public void serialize(LittleEndianOutput out) {
        out.writeShort(field_1_numerator);
        out.writeShort(field_2_denominator);
    }

    @Override
    protected int getDataSize() {
        return 2 + 2;
    }

    @Override
    public short getSid()
    {
        return sid;
    }

    @Override
    public Object clone() {
        SCLRecord rec = new SCLRecord();
    
        rec.field_1_numerator = field_1_numerator;
        rec.field_2_denominator = field_2_denominator;
        return rec;
    }

    /**
     * Get the numerator field for the SCL record.
     * 
     * @return the numerator
     */
    public short getNumerator()
    {
        return field_1_numerator;
    }

    /**
     * Set the numerator field for the SCL record.
     * 
     * @param field_1_numerator the numerator
     */
    public void setNumerator(short field_1_numerator)
    {
        this.field_1_numerator = field_1_numerator;
    }

    /**
     * Get the denominator field for the SCL record.
     * 
     * @return the denominator
     */
    public short getDenominator()
    {
        return field_2_denominator;
    }

    /**
     * Set the denominator field for the SCL record.
     * 
     * @param field_2_denominator the denominator
     */
    public void setDenominator(short field_2_denominator)
    {
        this.field_2_denominator = field_2_denominator;
    }
}
