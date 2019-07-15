import org.apache.poi.hssf.record.chart.*;


import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * ENDOBJECT - Chart Future Record Type End Object (0x0855)
 */
public final class ChartEndObjectRecord extends StandardRecord {
	public static final short sid = 0x0855;

	private short rt;
	private short grbitFrt;
	private short iObjectKind;
	private byte[] reserved;

	public ChartEndObjectRecord(RecordInputStream in) {
		rt = in.readShort();
		grbitFrt = in.readShort();
		iObjectKind = in.readShort();

		// The spec says that there should be 6 bytes at the
		//  end, which must be there and must be zero
		// However, sometimes Excel forgets them...
		reserved = new byte[6];
		if(in.available() == 0) {
		   // They've gone missing...
		} else {
		   // Read the reserved bytes 
		   in.readFully(reserved);
		}
	}

	@Override
	protected int getDataSize() {
		return 2 + 2 + 2 + 6;
	}

	@Override
	public short getSid() {
		return sid;
	}

	@Override
	public void serialize(LittleEndianOutput out) {
		out.writeShort(rt);
		out.writeShort(grbitFrt);
		out.writeShort(iObjectKind);
		// 6 bytes unused
		out.write(reserved);
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("[ENDOBJECT]\n");
		buffer.append("    .rt         =").append(HexDump.shortToHex(rt)).append('\n');
		buffer.append("    .grbitFrt   =").append(HexDump.shortToHex(grbitFrt)).append('\n');
		buffer.append("    .iObjectKind=").append(HexDump.shortToHex(iObjectKind)).append('\n');
		buffer.append("    .reserved   =").append(HexDump.toHex(reserved)).append('\n');
		buffer.append("[/ENDOBJECT]\n");
		return buffer.toString();
	}
}
