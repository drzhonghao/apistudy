import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Blank cell record (0x0201) <P>
 * Description:  Represents a column in a row with no value but with styling.<P>
 * REFERENCE:  PG 287 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 * @version 2.0-pre
 */
public final class BlankRecord extends StandardRecord implements CellValueRecordInterface, Cloneable {
    public final static short sid = 0x0201;
    private int             field_1_row;
    private short             field_2_col;
    private short             field_3_xf;

    /** Creates a new instance of BlankRecord */
    public BlankRecord()
    {
    }

    public BlankRecord(RecordInputStream in)
    {
        field_1_row = in.readUShort();
        field_2_col = in.readShort();
        field_3_xf  = in.readShort();
    }

    /**
     * set the row this cell occurs on
     * @param row the row this cell occurs within
     */
    public void setRow(int row)
    {
        field_1_row = row;
    }

    /**
     * get the row this cell occurs on
     *
     * @return the row
     */
    public int getRow()
    {
        return field_1_row;
    }

    /**
     * get the column this cell defines within the row
     *
     * @return the column
     */
    public short getColumn()
    {
        return field_2_col;
    }

    /**
     * set the index of the extended format record to style this cell with
     *
     * @param xf - the 0-based index of the extended format
     * @see org.apache.poi.hssf.record.ExtendedFormatRecord
     */
    public void setXFIndex(short xf)
    {
        field_3_xf = xf;
    }

    /**
     * get the index of the extended format record to style this cell with
     *
     * @return extended format index
     */
    public short getXFIndex()
    {
        return field_3_xf;
    }

    /**
     * set the column this cell defines within the row
     *
     * @param col the column this cell defines
     */

    public void setColumn(short col)
    {
        field_2_col = col;
    }

    /**
     * return the non static version of the id for this record.
     */
    public short getSid()
    {
        return sid;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("[BLANK]\n");
        sb.append("    row= ").append(HexDump.shortToHex(getRow())).append("\n");
        sb.append("    col= ").append(HexDump.shortToHex(getColumn())).append("\n");
        sb.append("    xf = ").append(HexDump.shortToHex(getXFIndex())).append("\n");
        sb.append("[/BLANK]\n");
        return sb.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(getRow());
        out.writeShort(getColumn());
        out.writeShort(getXFIndex());
    }

    protected int getDataSize() {
        return 6;
    }

    @Override
    public BlankRecord clone() {
      BlankRecord rec = new BlankRecord();
      rec.field_1_row = field_1_row;
      rec.field_2_col = field_2_col;
      rec.field_3_xf = field_3_xf;
      return rec;
    }
}
