import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title: Object Protect Record<P>
 * Description: Protect embedded object with the lamest "security" ever invented.  
 * This record tells  "I want to protect my objects" with lame security.  It 
 * appears in conjunction with the PASSWORD and PROTECT records as well as its 
 * scenario protect cousin.<P>
 * REFERENCE:  PG 368 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 */

public final class ObjectProtectRecord extends StandardRecord implements Cloneable {
    public final static short sid = 0x63;
    private short             field_1_protect;

    public ObjectProtectRecord()
    {
    }

    public ObjectProtectRecord(RecordInputStream in)
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

    @Override
    public ObjectProtectRecord clone() {
        ObjectProtectRecord rec = new ObjectProtectRecord();
        rec.field_1_protect = field_1_protect;
        return rec;
    }
}
