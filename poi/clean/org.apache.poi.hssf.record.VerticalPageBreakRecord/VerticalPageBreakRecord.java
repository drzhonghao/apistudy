import org.apache.poi.hssf.record.PageBreakRecord;
import org.apache.poi.hssf.record.*;


import java.util.Iterator;

/**
 * VerticalPageBreak (0x001A) record that stores page breaks at columns
 * 
 * @see PageBreakRecord
 */
public final class VerticalPageBreakRecord extends PageBreakRecord {

	public static final short sid = 0x001A;

	/**
	 * Creates an empty vertical page break record
	 */
	public VerticalPageBreakRecord() {

	}

	/**
	 * @param in the RecordInputstream to read the record from
	 */
	public VerticalPageBreakRecord(RecordInputStream in) {
		super(in);
	}

	public short getSid() {
		return sid;
	}

	public Object clone() {
		PageBreakRecord result = new VerticalPageBreakRecord();
		Iterator<Break> iterator = getBreaksIterator();
		while (iterator.hasNext()) {
			Break original = iterator.next();
			result.addBreak(original.main, original.subFrom, original.subTo);
		}
		return result;
	}
}
