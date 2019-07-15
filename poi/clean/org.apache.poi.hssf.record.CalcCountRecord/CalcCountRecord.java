import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Calc Count Record
 * Description:  Specifies the maximum times the gui should perform a formula
 *               recalculation.  For instance: in the case a formula includes
 *               cells that are themselves a result of a formula and a value
 *               changes.  This is essentially a failsafe against an infinate
 *               loop in the event the formulas are not independant. <P>
 * REFERENCE:  PG 292 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @version 2.0-pre
 * @see org.apache.poi.hssf.record.CalcModeRecord
 */

public final class CalcCountRecord extends StandardRecord implements Cloneable {
    public final static short sid = 0xC;
    private short             field_1_iterations;

    public CalcCountRecord()
    {
    }

    public CalcCountRecord(RecordInputStream in)
    {
        field_1_iterations = in.readShort();
    }

    /**
     * set the number of iterations to perform
     * @param iterations to perform
     */

    public void setIterations(short iterations)
    {
        field_1_iterations = iterations;
    }

    /**
     * get the number of iterations to perform
     * @return iterations
     */

    public short getIterations()
    {
        return field_1_iterations;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[CALCCOUNT]\n");
        buffer.append("    .iterations     = ")
            .append(Integer.toHexString(getIterations())).append("\n");
        buffer.append("[/CALCCOUNT]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(getIterations());
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    @Override
    public CalcCountRecord clone() {
      CalcCountRecord rec = new CalcCountRecord();
      rec.field_1_iterations = field_1_iterations;
      return rec;
    }
}
