import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title:        Backup Record <P>
 * Description:  Boolean specifying whether
 *               the GUI should store a backup of the file.<P>
 * REFERENCE:  PG 287 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)<P>
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @version 2.0-pre
 */

public final class BackupRecord
    extends StandardRecord
{
    public final static short sid = 0x40;
    private short             field_1_backup;   // = 0;

    public BackupRecord()
    {
    }

    public BackupRecord(RecordInputStream in)
    {
        field_1_backup = in.readShort();
    }

    /**
     * set the backup flag (0,1)
     *
     * @param backup    backup flag
     */

    public void setBackup(short backup)
    {
        field_1_backup = backup;
    }

    /**
     * get the backup flag
     *
     * @return short 0/1 (off/on)
     */

    public short getBackup()
    {
        return field_1_backup;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[BACKUP]\n");
        buffer.append("    .backup          = ")
            .append(Integer.toHexString(getBackup())).append("\n");
        buffer.append("[/BACKUP]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(getBackup());
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }
}
