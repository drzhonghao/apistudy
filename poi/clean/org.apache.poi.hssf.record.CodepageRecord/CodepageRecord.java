import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.CodePageUtil;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title: Codepage Record
 * <p>Description:  the default characterset. for the workbook</p>
 * <p>REFERENCE:  PG 293 Microsoft Excel 97 Developer's Kit (ISBN: 1-57231-498-2)</p>
 * <p>Use {@link CodePageUtil} to turn these values into Java code pages
 *  to encode/decode strings.</p>
 * @version 2.0-pre
 */

public final class CodepageRecord
    extends StandardRecord
{
    public final static short sid = 0x42;
    private short             field_1_codepage;   // = 0;

    /**
     * Excel 97+ (Biff 8) should always store strings as UTF-16LE or
     *  compressed versions of that. As such, this should always be
     *  0x4b0 = UTF_16, except for files coming from older versions.
     */
    public final static short CODEPAGE = ( short ) 0x4b0;

    public CodepageRecord()
    {
    }

    public CodepageRecord(RecordInputStream in)
    {
        field_1_codepage = in.readShort();
    }

    /**
     * set the codepage for this workbook
     *
     * @see #CODEPAGE
     * @param cp the codepage to set
     */

    public void setCodepage(short cp)
    {
        field_1_codepage = cp;
    }

    /**
     * get the codepage for this workbook
     *
     * @see #CODEPAGE
     * @return codepage - the codepage to set
     */

    public short getCodepage()
    {
        return field_1_codepage;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[CODEPAGE]\n");
        buffer.append("    .codepage        = ")
            .append(Integer.toHexString(getCodepage())).append("\n");
        buffer.append("[/CODEPAGE]\n");
        return buffer.toString();
    }

    public void serialize(LittleEndianOutput out) {
        out.writeShort(getCodepage());
    }

    protected int getDataSize() {
        return 2;
    }

    public short getSid()
    {
        return sid;
    }
}
