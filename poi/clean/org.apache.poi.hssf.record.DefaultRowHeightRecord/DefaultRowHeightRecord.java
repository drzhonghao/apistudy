import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Default Row Height Record
 * Description:  Row height for rows with undefined or not explicitly defined
 *               heights.
 * REFERENCE:  PG 301 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 * @version 2.0-pre
 */

public final class DefaultRowHeightRecord extends StandardRecord implements Cloneable {
    public final static short sid = 0x225;
    private short             field_1_option_flags;
    private short             field_2_row_height;

    /**
     * The default row height for empty rows is 255 twips (255 / 20 == 12.75 points)
     */
    public static final short DEFAULT_ROW_HEIGHT = 0xFF;

    public DefaultRowHeightRecord()
    {
        field_1_option_flags = 0x0000;
        field_2_row_height = DEFAULT_ROW_HEIGHT;
    }

    public DefaultRowHeightRecord(RecordInputStream in)
    {
        field_1_option_flags = in.readShort();
        field_2_row_height   = in.readShort();
    }

    /**
     * set the (currently unimportant to HSSF) option flags
     * @param flags the bitmask to set
     */

    public void setOptionFlags(short flags)
    {
        field_1_option_flags = flags;
    }

    /**
     * set the default row height
     * @param height    for undefined rows/rows w/undefined height
     */

    public void setRowHeight(short height)
    {
        field_2_row_height = height;
    }

    /**
     * get the (currently unimportant to HSSF) option flags
     * @return flags - the current bitmask
     */

    public short getOptionFlags()
    {
        return field_1_option_flags;
    }

    /**
     * get the default row height
     * @return rowheight for undefined rows/rows w/undefined height
     */

    public short getRowHeight()
    {
        return field_2_row_height;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[DEFAULTROWHEIGHT]\n");
        buffer.append("    .optionflags    = ")
            .append(Integer.toHexString(getOptionFlags())).append("\n");
        buffer.append("    .rowheight      = ")
            .append(Integer.toHexString(getRowHeight())).append("\n");
        buffer.append("[/DEFAULTROWHEIGHT]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(getOptionFlags());
        out.writeShort(getRowHeight());
    }

    protected int getDataSize() {
        return 4;
    }

    public short getSid()
    {
        return sid;
    }

    @Override
    public DefaultRowHeightRecord clone() {
      DefaultRowHeightRecord rec = new DefaultRowHeightRecord();
      rec.field_1_option_flags = field_1_option_flags;
      rec.field_2_row_height = field_2_row_height;
      return rec;
    }
}
