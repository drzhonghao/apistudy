import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Save Recalc Record <P>
 * Description:  defines whether to recalculate before saving (set to true)<P>
 * REFERENCE:  PG 381 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 * @version 2.0-pre
 */
public final class SaveRecalcRecord
    extends StandardRecord
{
    public final static short sid = 0x5f;
    private short             field_1_recalc;

    public SaveRecalcRecord() {
    }

    public SaveRecalcRecord(RecordInputStream in)
    {
        field_1_recalc = in.readShort();
    }

    /**
     * set whether to recalculate formulas/etc before saving or not
     * @param recalc - whether to recalculate or not
     */
    public void setRecalc(boolean recalc) {
        field_1_recalc = ( short ) (recalc ? 1 : 0);
    }

    /**
     * get whether to recalculate formulas/etc before saving or not
     * @return recalc - whether to recalculate or not
     */
    public boolean getRecalc()
    {
        return (field_1_recalc == 1);
    }

    public String toString() {
        return "[SAVERECALC]\n" +
                "    .recalc         = " + getRecalc() +
                "\n" +
                "[/SAVERECALC]\n";
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(field_1_recalc);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    public Object clone() {
      SaveRecalcRecord rec = new SaveRecalcRecord();
      rec.field_1_recalc = field_1_recalc;
      return rec;
    }
}
