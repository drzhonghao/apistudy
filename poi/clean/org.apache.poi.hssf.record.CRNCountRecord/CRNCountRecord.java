import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.*;


import org.apache.poi.util.LittleEndianOutput;
/**
 * XCT - CRN Count <P>
 *
 * REFERENCE:  5.114<P>
 *
 * @author Josh Micich
 */
public final class CRNCountRecord extends StandardRecord {
	public final static short sid = 0x59;

	private static final short DATA_SIZE = 4;


	private int	 field_1_number_crn_records;
	private int	 field_2_sheet_table_index;

	public CRNCountRecord() {
		throw new RuntimeException("incomplete code");
	}

	public int getNumberOfCRNs() {
		return field_1_number_crn_records;
	}


	public CRNCountRecord(RecordInputStream in) {
		field_1_number_crn_records = in.readShort();
		if(field_1_number_crn_records < 0) {
			// TODO - seems like the sign bit of this field might be used for some other purpose
			// see example file for test case "TestBugs.test19599()"
			field_1_number_crn_records = (short)-field_1_number_crn_records;
		}
		field_2_sheet_table_index = in.readShort();
	 }


	public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName()).append(" [XCT");
        sb.append(" nCRNs=").append(field_1_number_crn_records);
        sb.append(" sheetIx=").append(field_2_sheet_table_index);
        sb.append("]");
        return sb.toString();
	}

	public void serialize(LittleEndianOutput out) {
		out.writeShort((short)field_1_number_crn_records);
		out.writeShort((short)field_2_sheet_table_index);
	}
	protected int getDataSize() {
		return DATA_SIZE;
	}

	/**
	 * return the non static version of the id for this record.
	 */
	public short getSid() {
		return sid;
	}
}
