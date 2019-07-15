import org.apache.poi.hssf.record.CodepageRecord;
import org.apache.poi.hssf.record.*;


import java.io.UnsupportedEncodingException;

import org.apache.poi.hpsf.Property;
import org.apache.poi.util.CodePageUtil;
import org.apache.poi.util.IOUtils;


/**
 * Biff2 - Biff 4 Label Record (0x0007 / 0x0207) - read only support for 
 *  formula string results.
 */
public final class OldStringRecord {

    //arbitrarily selected; may need to increase
    private static final int MAX_RECORD_LENGTH = 100_000;

    public final static short biff2_sid = 0x0007;
    public final static short biff345_sid = 0x0207;

    private short             sid;
    private short             field_1_string_len;
    private byte[]            field_2_bytes;
    private CodepageRecord    codepage;

    /**
     * @param in the RecordInputstream to read the record from
     */
    public OldStringRecord(RecordInputStream in) {
        sid = in.getSid();
        
        if (in.getSid() == biff2_sid) {
            field_1_string_len  = (short)in.readUByte();
        } else {
            field_1_string_len   = in.readShort();
        }

        // Can only decode properly later when you know the codepage
        field_2_bytes = IOUtils.safelyAllocate(field_1_string_len, MAX_RECORD_LENGTH);
        in.read(field_2_bytes, 0, field_1_string_len);
    }

    public boolean isBiff2() {
        return sid == biff2_sid;
    }

    public short getSid() {
        return sid;
    }
    
    public void setCodePage(CodepageRecord codepage) {
        this.codepage = codepage;
    }

    /**
     * @return The string represented by this record.
     */
    public String getString()
    {
        return getString(field_2_bytes, codepage);
    }
    
    protected static String getString(byte[] data, CodepageRecord codepage) {
        int cp = Property.DEFAULT_CODEPAGE;
        if (codepage != null) {
            cp = codepage.getCodepage() & 0xffff;
        }
        try {
            return CodePageUtil.getStringFromCodePage(data, cp);
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalArgumentException("Unsupported codepage requested", uee);
        }
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append("[OLD STRING]\n");
        buffer.append("    .string            = ")
            .append(getString()).append("\n");
        buffer.append("[/OLD STRING]\n");
        return buffer.toString();
    }
}
