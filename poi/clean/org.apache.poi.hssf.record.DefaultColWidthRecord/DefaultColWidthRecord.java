import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Default Column Width Record (0x0055) <P>
 * Description:  Specifies the default width for columns that have no specific
 *               width set.<P>
 * REFERENCE:  PG 302 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 * @version 2.0-pre
 */
public final class DefaultColWidthRecord extends StandardRecord implements Cloneable {
    public final static short sid = 0x0055;
    private int             field_1_col_width;

    /**
     *  The default column width is 8 characters
     */
    public final static int DEFAULT_COLUMN_WIDTH = 0x0008;

    public DefaultColWidthRecord()
    {
        field_1_col_width = DEFAULT_COLUMN_WIDTH;
    }

    public DefaultColWidthRecord(RecordInputStream in)
    {
        field_1_col_width = in.readUShort();
    }

    /**
     * set the default column width
     * @param width defaultwidth for columns
     */

    public void setColWidth(int width)
    {
        field_1_col_width = width;
    }

    /**
     * get the default column width
     * @return defaultwidth for columns
     */

    public int getColWidth()
    {
        return field_1_col_width;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[DEFAULTCOLWIDTH]\n");
        buffer.append("    .colwidth      = ")
            .append(Integer.toHexString(getColWidth())).append("\n");
        buffer.append("[/DEFAULTCOLWIDTH]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(getColWidth());
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    @Override
    public DefaultColWidthRecord clone() {
      DefaultColWidthRecord rec = new DefaultColWidthRecord();
      rec.field_1_col_width = field_1_col_width;
      return rec;
    }
}
