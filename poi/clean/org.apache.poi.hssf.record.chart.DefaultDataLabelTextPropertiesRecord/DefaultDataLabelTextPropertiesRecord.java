import org.apache.poi.hssf.record.chart.*;


import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * The default data label text properties record identifies the text characteristics of the preceding text record.
 */
public final class DefaultDataLabelTextPropertiesRecord extends StandardRecord implements Cloneable {
    public final static short      sid                             = 0x1024;
    private  short      field_1_categoryDataType;
    public final static short       CATEGORY_DATA_TYPE_SHOW_LABELS_CHARACTERISTIC = 0;
    public final static short       CATEGORY_DATA_TYPE_VALUE_AND_PERCENTAGE_CHARACTERISTIC = 1;
    public final static short       CATEGORY_DATA_TYPE_ALL_TEXT_CHARACTERISTIC = 2;


    public DefaultDataLabelTextPropertiesRecord()
    {

    }

    public DefaultDataLabelTextPropertiesRecord(RecordInputStream in)
    {
        field_1_categoryDataType       = in.readShort();
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[DEFAULTTEXT]\n");
        buffer.append("    .categoryDataType     = ")
            .append("0x").append(HexDump.toHex(  getCategoryDataType ()))
            .append(" (").append( getCategoryDataType() ).append(" )");
        buffer.append(System.getProperty("line.separator")); 

        buffer.append("[/DEFAULTTEXT]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(field_1_categoryDataType);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    @Override
    public DefaultDataLabelTextPropertiesRecord clone() {
        DefaultDataLabelTextPropertiesRecord rec = new DefaultDataLabelTextPropertiesRecord();
    
        rec.field_1_categoryDataType = field_1_categoryDataType;
        return rec;
    }




    /**
     * Get the category data type field for the DefaultDataLabelTextProperties record.
     *
     * @return  One of 
     *        CATEGORY_DATA_TYPE_SHOW_LABELS_CHARACTERISTIC
     *        CATEGORY_DATA_TYPE_VALUE_AND_PERCENTAGE_CHARACTERISTIC
     *        CATEGORY_DATA_TYPE_ALL_TEXT_CHARACTERISTIC
     */
    public short getCategoryDataType()
    {
        return field_1_categoryDataType;
    }

    /**
     * Set the category data type field for the DefaultDataLabelTextProperties record.
     *
     * @param field_1_categoryDataType
     *        One of 
     *        CATEGORY_DATA_TYPE_SHOW_LABELS_CHARACTERISTIC
     *        CATEGORY_DATA_TYPE_VALUE_AND_PERCENTAGE_CHARACTERISTIC
     *        CATEGORY_DATA_TYPE_ALL_TEXT_CHARACTERISTIC
     */
    public void setCategoryDataType(short field_1_categoryDataType)
    {
        this.field_1_categoryDataType = field_1_categoryDataType;
    }
}
