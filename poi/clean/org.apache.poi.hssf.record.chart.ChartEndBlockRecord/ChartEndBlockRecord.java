import org.apache.poi.hssf.record.chart.*;


import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;

/**
 * ENDBLOCK - Chart Future Record Type End Block (0x0853)<br>
 * 
 * @author Patrick Cheng
 */
public final class ChartEndBlockRecord extends StandardRecord implements Cloneable {
	public static final short sid = 0x0853;

	private short rt;
	private short grbitFrt;
	private short iObjectKind;
	private byte[] unused;

	public ChartEndBlockRecord() {
	}
	
	public ChartEndBlockRecord(RecordInputStream in) {
		rt = in.readShort();
		grbitFrt = in.readShort();
		iObjectKind = in.readShort();

		// Often, but not always has 6 unused bytes at the end
		if(in.available() == 0) {
			unused = new byte[0];
		} else {
			unused = new byte[6];
			in.readFully(unused);
		}
	}

	@Override
	protected int getDataSize() {
		return 2 + 2 + 2 + unused.length;
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
		out.write(unused);
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("[ENDBLOCK]\n");
		buffer.append("    .rt         =").append(HexDump.shortToHex(rt)).append('\n');
		buffer.append("    .grbitFrt   =").append(HexDump.shortToHex(grbitFrt)).append('\n');
		buffer.append("    .iObjectKind=").append(HexDump.shortToHex(iObjectKind)).append('\n');
		buffer.append("    .unused     =").append(HexDump.toHex(unused)).append('\n');
		buffer.append("[/ENDBLOCK]\n");
		return buffer.toString();
	}
	
	@Override
	public ChartEndBlockRecord clone() {
		ChartEndBlockRecord record = new ChartEndBlockRecord();
		
		record.rt = rt ;
		record.grbitFrt = grbitFrt ;
		record.iObjectKind = iObjectKind ;
		record.unused = unused.clone() ;
		
		return record;
	}
}
