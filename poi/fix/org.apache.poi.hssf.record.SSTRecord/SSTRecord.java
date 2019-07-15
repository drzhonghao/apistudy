

import java.util.Iterator;
import org.apache.poi.hssf.record.ExtSSTRecord;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.hssf.record.cont.ContinuableRecord;
import org.apache.poi.hssf.record.cont.ContinuableRecordOutput;
import org.apache.poi.util.IntMapper;
import org.apache.poi.util.LittleEndianConsts;


public final class SSTRecord extends ContinuableRecord {
	public static final short sid = 252;

	private static final UnicodeString EMPTY_STRING = new UnicodeString("");

	static final int STD_RECORD_OVERHEAD = 2 * (LittleEndianConsts.SHORT_SIZE);

	static final int SST_RECORD_OVERHEAD = (SSTRecord.STD_RECORD_OVERHEAD) + (2 * (LittleEndianConsts.INT_SIZE));

	static final int MAX_DATA_SPACE = (RecordInputStream.MAX_RECORD_DATA_SIZE) - 8;

	private int field_1_num_strings;

	private int field_2_num_unique_strings;

	private IntMapper<UnicodeString> field_3_strings;

	int[] bucketAbsoluteOffsets;

	int[] bucketRelativeOffsets;

	public SSTRecord() {
		field_1_num_strings = 0;
		field_2_num_unique_strings = 0;
		field_3_strings = new IntMapper<>();
	}

	public int addString(UnicodeString string) {
		(field_1_num_strings)++;
		UnicodeString ucs = (string == null) ? SSTRecord.EMPTY_STRING : string;
		int rval;
		int index = field_3_strings.getIndex(ucs);
		if (index != (-1)) {
			rval = index;
		}else {
			rval = field_3_strings.size();
			(field_2_num_unique_strings)++;
		}
		return rval;
	}

	public int getNumStrings() {
		return field_1_num_strings;
	}

	public int getNumUniqueStrings() {
		return field_2_num_unique_strings;
	}

	public UnicodeString getString(int id) {
		return field_3_strings.get(id);
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[SST]\n");
		buffer.append("    .numstrings     = ").append(Integer.toHexString(getNumStrings())).append("\n");
		buffer.append("    .uniquestrings  = ").append(Integer.toHexString(getNumUniqueStrings())).append("\n");
		for (int k = 0; k < (field_3_strings.size()); k++) {
			UnicodeString s = field_3_strings.get(k);
			buffer.append((("    .string_" + k) + "      = ")).append(s.getDebugInfo()).append("\n");
		}
		buffer.append("[/SST]\n");
		return buffer.toString();
	}

	public short getSid() {
		return SSTRecord.sid;
	}

	public SSTRecord(RecordInputStream in) {
		field_1_num_strings = in.readInt();
		field_2_num_unique_strings = in.readInt();
		field_3_strings = new IntMapper<>();
		if ((field_1_num_strings) == 0) {
			field_2_num_unique_strings = 0;
			return;
		}
	}

	Iterator<UnicodeString> getStrings() {
		return field_3_strings.iterator();
	}

	int countStrings() {
		return field_3_strings.size();
	}

	protected void serialize(ContinuableRecordOutput out) {
	}

	public ExtSSTRecord createExtSSTRecord(int sstOffset) {
		if (((bucketAbsoluteOffsets) == null) || ((bucketRelativeOffsets) == null)) {
			throw new IllegalStateException("SST record has not yet been serialized.");
		}
		ExtSSTRecord extSST = new ExtSSTRecord();
		extSST.setNumStringsPerBucket(((short) (8)));
		int[] absoluteOffsets = bucketAbsoluteOffsets.clone();
		int[] relativeOffsets = bucketRelativeOffsets.clone();
		for (int i = 0; i < (absoluteOffsets.length); i++) {
			absoluteOffsets[i] += sstOffset;
		}
		extSST.setBucketOffsets(absoluteOffsets, relativeOffsets);
		return extSST;
	}

	public int calcExtSSTRecordSize() {
		return ExtSSTRecord.getRecordSizeForStrings(field_3_strings.size());
	}
}

