import org.apache.poi.hssf.record.PageBreakRecord;
import org.apache.poi.hssf.record.*;


import java.util.Iterator;

/**
 * HorizontalPageBreak (0x001B) record that stores page breaks at rows
 * 
 * @see PageBreakRecord
 */
public final class HorizontalPageBreakRecord extends PageBreakRecord implements Cloneable {

	public static final short sid = 0x001B;

	/**
	 * Creates an empty horizontal page break record
	 */
	public HorizontalPageBreakRecord() {
	}

	/**
	 * @param in the RecordInputstream to read the record from
	 */
	public HorizontalPageBreakRecord(RecordInputStream in) {
		super(in);
	}

	public short getSid() {
		return sid;
	}

	@Override
	public PageBreakRecord clone() {
		PageBreakRecord result = new HorizontalPageBreakRecord();
		Iterator<Break> iterator = getBreaksIterator();
		while (iterator.hasNext()) {
			Break original = iterator.next();
			result.addBreak(original.main, original.subFrom, original.subTo);
		}
		return result;
	}
}
