import org.apache.poi.hssf.record.chart.*;


import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * STARTBLOCK - Chart Future Record Type Start Block (0x0852)
 */
public final class ChartStartBlockRecord extends StandardRecord implements Cloneable {
	public static final short sid = 0x0852;

	private short rt;
	private short grbitFrt;
	private short iObjectKind;
	private short iObjectContext;
	private short iObjectInstance1;
	private short iObjectInstance2;

	public ChartStartBlockRecord() {
	}
	
	public ChartStartBlockRecord(RecordInputStream in) {
		rt = in.readShort();
		grbitFrt = in.readShort();
		iObjectKind = in.readShort();
		iObjectContext = in.readShort();
		iObjectInstance1 = in.readShort();
		iObjectInstance2 = in.readShort();
	}

	@Override
	protected int getDataSize() {
		return 2 + 2 + 2 + 2 + 2 + 2;
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
		out.writeShort(iObjectContext);
		out.writeShort(iObjectInstance1);
		out.writeShort(iObjectInstance2);
	}

	public String toString() {

		StringBuffer buffer = new StringBuffer();

		buffer.append("[STARTBLOCK]\n");
		buffer.append("    .rt              =").append(HexDump.shortToHex(rt)).append('\n');
		buffer.append("    .grbitFrt        =").append(HexDump.shortToHex(grbitFrt)).append('\n');
		buffer.append("    .iObjectKind     =").append(HexDump.shortToHex(iObjectKind)).append('\n');
		buffer.append("    .iObjectContext  =").append(HexDump.shortToHex(iObjectContext)).append('\n');
		buffer.append("    .iObjectInstance1=").append(HexDump.shortToHex(iObjectInstance1)).append('\n');
		buffer.append("    .iObjectInstance2=").append(HexDump.shortToHex(iObjectInstance2)).append('\n');
		buffer.append("[/STARTBLOCK]\n");
		return buffer.toString();
	}
	
	@Override
	public ChartStartBlockRecord clone() {
		ChartStartBlockRecord record = new ChartStartBlockRecord();
		
		record.rt = rt;
		record.grbitFrt = grbitFrt;
		record.iObjectKind = iObjectKind;
		record.iObjectContext = iObjectContext;
		record.iObjectInstance1 = iObjectInstance1;
		record.iObjectInstance2 = iObjectInstance2;
		
		return record;
	}
}
