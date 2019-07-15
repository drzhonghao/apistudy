import org.apache.poi.hssf.record.chart.*;


import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * DATALABEXT - Chart Data Label Extension (0x086A)
 */
public final class DataLabelExtensionRecord extends StandardRecord {
	public static final short sid = 0x086A;
	
	private int rt;
	private int grbitFrt;
	private byte[] unused = new byte[8];
	
	public DataLabelExtensionRecord(RecordInputStream in) {
		rt = in.readShort();
		grbitFrt = in.readShort();
		in.readFully(unused);
	}
	
	@Override
	protected int getDataSize() {
		return 2 + 2 + 8;
	}

	@Override
	public short getSid() {
		return sid;
	}

	@Override
	protected void serialize(LittleEndianOutput out) {
		out.writeShort(rt);
		out.writeShort(grbitFrt);
		out.write(unused);
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("[DATALABEXT]\n");
		buffer.append("    .rt      =").append(HexDump.shortToHex(rt)).append('\n');
		buffer.append("    .grbitFrt=").append(HexDump.shortToHex(grbitFrt)).append('\n');
		buffer.append("    .unused  =").append(HexDump.toHex(unused)).append('\n');

		buffer.append("[/DATALABEXT]\n");
		return buffer.toString();
	}
}
