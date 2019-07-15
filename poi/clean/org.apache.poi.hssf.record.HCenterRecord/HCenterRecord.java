import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        HCenter record (0x0083)<P>
 * Description:  whether to center between horizontal margins<P>
 * REFERENCE:  PG 320 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 * @version 2.0-pre
 */
public final class HCenterRecord extends StandardRecord implements Cloneable {
    public final static short sid = 0x0083;
    private short             field_1_hcenter;

    public HCenterRecord() {
    }

    public HCenterRecord(RecordInputStream in)
    {
        field_1_hcenter = in.readShort();
    }

    /**
     * set whether or not to horizonatally center this sheet.
     * @param hc  center - t/f
     */
    public void setHCenter(boolean hc) {
        if (hc) {
            field_1_hcenter = 1;
        } else {
            field_1_hcenter = 0;
        }
    }

    /**
     * get whether or not to horizonatally center this sheet.
     * @return center - t/f
     */
    public boolean getHCenter()
    {
        return (field_1_hcenter == 1);
    }

    public String toString() {
        return "[HCENTER]\n" +
                "    .hcenter        = " + getHCenter() +
                "\n" +
                "[/HCENTER]\n";
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(field_1_hcenter);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    @Override
    public HCenterRecord clone() {
      HCenterRecord rec = new HCenterRecord();
      rec.field_1_hcenter = field_1_hcenter;
      return rec;
    }
}
