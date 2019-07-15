import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Title: Uncalced Record
 * <P>
 * If this record occurs in the Worksheet Substream, it indicates that the formulas have not 
 * been recalculated before the document was saved.
 * 
 * @author Olivier Leprince
 */
public final class UncalcedRecord extends StandardRecord  {
	public final static short sid = 0x005E;

    private short _reserved;

	public UncalcedRecord() {
        _reserved = 0;
	}

	public short getSid() {
		return sid;
	}

	public UncalcedRecord(RecordInputStream in) {
		_reserved = in.readShort(); // unused
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[UNCALCED]\n");
        buffer.append("    _reserved: ").append(_reserved).append('\n');
		buffer.append("[/UNCALCED]\n");
		return buffer.toString();
	}

	public void serialize(LittleEndianOutput out) {
		out.writeShort(_reserved);
	}

	protected int getDataSize() {
		return 2;
	}

	public static int getStaticRecordSize() {
		return 6;
	}
}
