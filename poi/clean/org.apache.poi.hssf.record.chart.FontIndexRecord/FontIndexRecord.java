import org.apache.poi.hssf.record.chart.*;


import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * The font index record indexes into the font table for the text record.<p>
 * 
 * @author Glen Stampoultzis (glens at apache.org)
 */
public final class FontIndexRecord extends StandardRecord implements Cloneable {
    public final static short sid = 0x1026;
    private  short      field_1_fontIndex;


    public FontIndexRecord()
    {

    }

    public FontIndexRecord(RecordInputStream in)
    {
        field_1_fontIndex              = in.readShort();
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[FONTX]\n");
        buffer.append("    .fontIndex            = ")
            .append("0x").append(HexDump.toHex(  getFontIndex ()))
            .append(" (").append( getFontIndex() ).append(" )");
        buffer.append(System.getProperty("line.separator")); 

        buffer.append("[/FONTX]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(field_1_fontIndex);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    @Override
    public FontIndexRecord clone() {
        FontIndexRecord rec = new FontIndexRecord();
    
        rec.field_1_fontIndex = field_1_fontIndex;
        return rec;
    }




    /**
     * Get the font index field for the FontIndex record.
     */
    public short getFontIndex()
    {
        return field_1_fontIndex;
    }

    /**
     * Set the font index field for the FontIndex record.
     */
    public void setFontIndex(short field_1_fontIndex)
    {
        this.field_1_fontIndex = field_1_fontIndex;
    }
}
