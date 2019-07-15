import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title: Scenario Protect Record<P>
 * Description:  I have no idea what a Scenario is or why on would want to 
 * protect it with the lamest "security" ever invented.  However this record tells
 * excel "I want to protect my scenarios" (0xAF) with lame security.  It appears 
 * in conjunction with the PASSWORD and PROTECT records as well as its object 
 * protect cousin.<P>
 * REFERENCE:  PG 383 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 */

public final class ScenarioProtectRecord
    extends StandardRecord
{
    public final static short sid = 0xdd;
    private short             field_1_protect;

    public ScenarioProtectRecord()
    {
    }

    public ScenarioProtectRecord(RecordInputStream in)
    {
        field_1_protect = in.readShort();
    }

    /**
     * set whether the sheet is protected or not
     * @param protect whether to protect the sheet or not
     */

    public void setProtect(boolean protect)
    {
        if (protect)
        {
            field_1_protect = 1;
        }
        else
        {
            field_1_protect = 0;
        }
    }

    /**
     * get whether the sheet is protected or not
     * @return whether to protect the sheet or not
     */

    public boolean getProtect()
    {
        return (field_1_protect == 1);
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[SCENARIOPROTECT]\n");
	    buffer.append("    .protect         = ").append(getProtect())
            .append("\n");
        buffer.append("[/SCENARIOPROTECT]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(field_1_protect);
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }

    public Object clone() {
        ScenarioProtectRecord rec = new ScenarioProtectRecord();
        rec.field_1_protect = field_1_protect;
        return rec;
    }
}
