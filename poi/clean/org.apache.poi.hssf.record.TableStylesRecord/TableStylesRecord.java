import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.StringUtil;

/**
 * TABLESTYLES (0x088E)
 */
public final class TableStylesRecord extends StandardRecord {
	public static final short sid = 0x088E;
	
	private int rt;
	private int grbitFrt;
	private byte[] unused = new byte[8];
	private int cts;
	
	private String rgchDefListStyle;
	private String rgchDefPivotStyle;
	
	
	public TableStylesRecord(RecordInputStream in) {
		rt = in.readUShort();
		grbitFrt = in.readUShort();
		in.readFully(unused);
		cts = in.readInt();
		int cchDefListStyle = in.readUShort();
		int cchDefPivotStyle = in.readUShort();
		
		rgchDefListStyle = in.readUnicodeLEString(cchDefListStyle);
		rgchDefPivotStyle = in.readUnicodeLEString(cchDefPivotStyle);
	}
	
	@Override
	protected void serialize(LittleEndianOutput out) {
		out.writeShort(rt);
		out.writeShort(grbitFrt);
		out.write(unused);
		out.writeInt(cts);
		
		out.writeShort(rgchDefListStyle.length());
		out.writeShort(rgchDefPivotStyle.length());
		
		StringUtil.putUnicodeLE(rgchDefListStyle, out);
		StringUtil.putUnicodeLE(rgchDefPivotStyle, out);
	}

	@Override
	protected int getDataSize() {
		return 2 + 2 + 8 + 4 + 2 + 2
			+ (2*rgchDefListStyle.length()) + (2*rgchDefPivotStyle.length());
	}

	@Override
	public short getSid() {
		return sid;
	}


	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("[TABLESTYLES]\n");
		buffer.append("    .rt      =").append(HexDump.shortToHex(rt)).append('\n');
		buffer.append("    .grbitFrt=").append(HexDump.shortToHex(grbitFrt)).append('\n');
		buffer.append("    .unused  =").append(HexDump.toHex(unused)).append('\n');
		buffer.append("    .cts=").append(HexDump.intToHex(cts)).append('\n');
		buffer.append("    .rgchDefListStyle=").append(rgchDefListStyle).append('\n');
		buffer.append("    .rgchDefPivotStyle=").append(rgchDefPivotStyle).append('\n');

		buffer.append("[/TABLESTYLES]\n");
		return buffer.toString();
	}
}
