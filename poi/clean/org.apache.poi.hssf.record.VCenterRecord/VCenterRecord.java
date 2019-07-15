import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        VCenter record<P>
 * Description:  tells whether to center the sheet between vertical margins<P>
 * REFERENCE:  PG 420 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 * @version 2.0-pre
 */

public final class VCenterRecord extends StandardRecord {
    public final static short sid = 0x84;
    private int field_1_vcenter;

    public VCenterRecord()
    {
    }

    public VCenterRecord(RecordInputStream in)
    {
        field_1_vcenter = in.readShort();
    }

    /**
     * set whether to center vertically or not
     * @param hc  vcenter or not
     */

    public void setVCenter(boolean hc)
    {
    	field_1_vcenter = hc ? 1 : 0;
    }

    /**
     * get whether to center vertically or not
     * @return vcenter or not
     */

    public boolean getVCenter()
    {
        return (field_1_vcenter == 1);
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[VCENTER]\n");
        buffer.append("    .vcenter        = ").append(getVCenter())
            .append("\n");
        buffer.append("[/VCENTER]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(field_1_vcenter);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    public Object clone() {
      VCenterRecord rec = new VCenterRecord();
      rec.field_1_vcenter = field_1_vcenter;
      return rec;
    }
}
