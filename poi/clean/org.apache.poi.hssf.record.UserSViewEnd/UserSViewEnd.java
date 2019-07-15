import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import java.util.Locale;

import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * The UserSViewEnd record marks the end of the settings for a custom view associated with the sheet
 */
public final class UserSViewEnd extends StandardRecord {

    public final static short sid = 0x01AB;
	private byte[] _rawData;

    public UserSViewEnd(byte[] data) {
        _rawData = data;
    }

	/**
	 * construct an UserSViewEnd record.  No fields are interpreted and the record will
	 * be serialized in its original form more or less
	 * @param in the RecordInputstream to read the record from
	 */
	public UserSViewEnd(RecordInputStream in) {
		_rawData = in.readRemainder();
	}

	/**
	 * spit the record out AS IS. no interpretation or identification
	 */
	public void serialize(LittleEndianOutput out) {
		out.write(_rawData);
	}

	protected int getDataSize() {
		return _rawData.length;
	}

    public short getSid()
    {
        return sid;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append('[').append("USERSVIEWEND").append("] (0x");
        sb.append(Integer.toHexString(sid).toUpperCase(Locale.ROOT)).append(")\n");
        sb.append("  rawData=").append(HexDump.toHex(_rawData)).append("\n");
        sb.append("[/").append("USERSVIEWEND").append("]\n");
        return sb.toString();
    }

    //HACK: do a "cheat" clone, see Record.java for more information
    public Object clone() {
        return cloneViaReserialise();
    }

    
}
