import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Calc Mode Record<P>
 * Description:  Tells the gui whether to calculate formulas
 *               automatically, manually or automatically
 *               except for tables.<P>
 * REFERENCE:  PG 292 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 * @version 2.0-pre
 * @see org.apache.poi.hssf.record.CalcCountRecord
 */

public final class CalcModeRecord extends StandardRecord implements Cloneable {
    public final static short sid                     = 0xD;

    /**
     * manually calculate formulas (0)
     */

    public final static short MANUAL                  = 0;

    /**
     * automatically calculate formulas (1)
     */

    public final static short AUTOMATIC               = 1;

    /**
     * automatically calculate formulas except for tables (-1)
     */

    public final static short AUTOMATIC_EXCEPT_TABLES = -1;
    private short             field_1_calcmode;

    public CalcModeRecord()
    {
    }

    public CalcModeRecord(RecordInputStream in)
    {
        field_1_calcmode = in.readShort();
    }

    /**
     * set the calc mode flag for formulas
     *
     * @see #MANUAL
     * @see #AUTOMATIC
     * @see #AUTOMATIC_EXCEPT_TABLES
     *
     * @param calcmode one of the three flags above
     */

    public void setCalcMode(short calcmode)
    {
        field_1_calcmode = calcmode;
    }

    /**
     * get the calc mode flag for formulas
     *
     * @see #MANUAL
     * @see #AUTOMATIC
     * @see #AUTOMATIC_EXCEPT_TABLES
     *
     * @return calcmode one of the three flags above
     */

    public short getCalcMode()
    {
        return field_1_calcmode;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[CALCMODE]\n");
        buffer.append("    .calcmode       = ")
            .append(Integer.toHexString(getCalcMode())).append("\n");
        buffer.append("[/CALCMODE]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(getCalcMode());
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    @Override
    public CalcModeRecord clone() {
      CalcModeRecord rec = new CalcModeRecord();
      rec.field_1_calcmode = field_1_calcmode;
      return rec;
    }
}
