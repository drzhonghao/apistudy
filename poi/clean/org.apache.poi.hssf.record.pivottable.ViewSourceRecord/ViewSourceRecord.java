import org.apache.poi.hssf.record.pivottable.*;


import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * SXVS - View Source (0x00E3)<br>
 * 
 * @author Patrick Cheng
 */
public final class ViewSourceRecord extends StandardRecord {
	public static final short sid = 0x00E3;

	private int vs;
	
	public ViewSourceRecord(RecordInputStream in) {
		vs = in.readShort();
	}
	
	@Override
	protected void serialize(LittleEndianOutput out) {
		out.writeShort(vs);
	}

	@Override
	protected int getDataSize() {
		return 2;
	}

	@Override
	public short getSid() {
		return sid;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("[SXVS]\n");
		buffer.append("    .vs      =").append(HexDump.shortToHex(vs)).append('\n');

		buffer.append("[/SXVS]\n");
		return buffer.toString();
	}
}
