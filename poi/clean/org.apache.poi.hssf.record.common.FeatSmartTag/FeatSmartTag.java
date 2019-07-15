import org.apache.poi.hssf.record.common.*;


import org.apache.poi.hssf.record.FeatRecord;
//import org.apache.poi.hssf.record.Feat11Record;
//import org.apache.poi.hssf.record.Feat12Record;
import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.util.LittleEndianOutput;

/**
 * Title: FeatSmartTag (Smart Tag Shared Feature) common record part
 * <P>
 * This record part specifies Smart Tag data for a sheet, stored as part
 *  of a Shared Feature. It can be found in records such as  {@link FeatRecord}.
 * It is made up of a hash, and a set of Factoid Data that makes up
 *  the smart tags.
 * For more details, see page 669 of the Excel binary file
 *  format documentation.
 */
public final class FeatSmartTag implements SharedFeature {
	// TODO - process
	private byte[] data;
	
	public FeatSmartTag() {
		data = new byte[0];
	}

	public FeatSmartTag(RecordInputStream in) {
		data = in.readRemainder();
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(" [FEATURE SMART TAGS]\n");
		buffer.append(" [/FEATURE SMART TAGS]\n");
		return buffer.toString();
	}

	public int getDataSize() {
		return data.length;
	}

	public void serialize(LittleEndianOutput out) {
		out.write(data);
	}
}
