

import org.apache.poi.hssf.record.CodepageRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.IOUtils;


public final class OldSheetRecord {
	private static final int MAX_RECORD_LENGTH = 100000;

	public static final short sid = 133;

	private int field_1_position_of_BOF;

	private int field_2_visibility;

	private int field_3_type;

	private byte[] field_5_sheetname;

	private CodepageRecord codepage;

	public OldSheetRecord(RecordInputStream in) {
		field_1_position_of_BOF = in.readInt();
		field_2_visibility = in.readUByte();
		field_3_type = in.readUByte();
		int field_4_sheetname_length = in.readUByte();
		field_5_sheetname = IOUtils.safelyAllocate(field_4_sheetname_length, OldSheetRecord.MAX_RECORD_LENGTH);
		in.read(field_5_sheetname, 0, field_4_sheetname_length);
	}

	public void setCodePage(CodepageRecord codepage) {
		this.codepage = codepage;
	}

	public short getSid() {
		return OldSheetRecord.sid;
	}

	public int getPositionOfBof() {
		return field_1_position_of_BOF;
	}

	public String getSheetname() {
		return null;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[BOUNDSHEET]\n");
		buffer.append("    .bof        = ").append(HexDump.intToHex(getPositionOfBof())).append("\n");
		buffer.append("    .visibility = ").append(HexDump.shortToHex(field_2_visibility)).append("\n");
		buffer.append("    .type       = ").append(HexDump.byteToHex(field_3_type)).append("\n");
		buffer.append("    .sheetname  = ").append(getSheetname()).append("\n");
		buffer.append("[/BOUNDSHEET]\n");
		return buffer.toString();
	}
}

