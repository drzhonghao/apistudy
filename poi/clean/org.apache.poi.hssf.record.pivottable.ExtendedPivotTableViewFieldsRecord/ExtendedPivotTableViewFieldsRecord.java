import org.apache.poi.hssf.record.pivottable.*;


import org.apache.poi.hssf.record.RecordInputStream;
import org.apache.poi.hssf.record.StandardRecord;
import org.apache.poi.util.HexDump;
import org.apache.poi.util.LittleEndianOutput;
import org.apache.poi.util.RecordFormatException;
import org.apache.poi.util.StringUtil;

/**
 * SXVDEX - Extended PivotTable View Fields (0x0100)<br>
 * 
 * @author Patrick Cheng
 */
public final class ExtendedPivotTableViewFieldsRecord extends StandardRecord {
	public static final short sid = 0x0100;

	/** the value of the subname length when the {@link #_subtotalName} is not present */
	private static final int STRING_NOT_PRESENT_LEN = 0xFFFF;

	private int _grbit1;
	private int _grbit2;
	private int _citmShow;
	private int _isxdiSort;
	private int _isxdiShow;
	private int _reserved1;
	private int _reserved2;
	/** custom sub-total name */
	private String _subtotalName;

	public ExtendedPivotTableViewFieldsRecord(RecordInputStream in) {

		_grbit1 = in.readInt();
		_grbit2 = in.readUByte();
		_citmShow = in.readUByte();
		_isxdiSort = in.readUShort();
		_isxdiShow = in.readUShort();
		// This record seems to have different valid encodings
		switch (in.remaining()) {
			case 0:
				// as per "Microsoft Excel Developer's Kit" book
				// older version of SXVDEX - doesn't seem to have a sub-total name
				_reserved1 = 0;
				_reserved2 = 0;
				_subtotalName = null;
				return;
			case 10:
				// as per "MICROSOFT OFFICE EXCEL 97-2007 BINARY FILE FORMAT SPECIFICATION" pdf
				break;
			default:
				throw new RecordFormatException("Unexpected remaining size (" + in.remaining() + ")");
		}
		int cchSubName = in.readUShort();
		_reserved1 = in.readInt();
		_reserved2 = in.readInt();
		if (cchSubName != STRING_NOT_PRESENT_LEN) {
			_subtotalName = in.readUnicodeLEString(cchSubName);
		}
	}

	@Override
	protected void serialize(LittleEndianOutput out) {

		out.writeInt(_grbit1);
		out.writeByte(_grbit2);
		out.writeByte(_citmShow);
		out.writeShort(_isxdiSort);
		out.writeShort(_isxdiShow);

		if (_subtotalName == null) {
			out.writeShort(STRING_NOT_PRESENT_LEN);
		} else {
			out.writeShort(_subtotalName.length());
		}

		out.writeInt(_reserved1);
		out.writeInt(_reserved2);
		if (_subtotalName != null) {
			StringUtil.putUnicodeLE(_subtotalName, out);
		}
	}

	@Override
	protected int getDataSize() {
		
		return 4 + 1 + 1 + 2 + 2 + 2 +  4 + 4 +
					(_subtotalName == null ? 0 : (2*_subtotalName.length())); // in unicode
	}

	@Override
	public short getSid() {
		return sid;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("[SXVDEX]\n");

		buffer.append("    .grbit1 =").append(HexDump.intToHex(_grbit1)).append("\n");
		buffer.append("    .grbit2 =").append(HexDump.byteToHex(_grbit2)).append("\n");
		buffer.append("    .citmShow =").append(HexDump.byteToHex(_citmShow)).append("\n");
		buffer.append("    .isxdiSort =").append(HexDump.shortToHex(_isxdiSort)).append("\n");
		buffer.append("    .isxdiShow =").append(HexDump.shortToHex(_isxdiShow)).append("\n");
		buffer.append("    .subtotalName =").append(_subtotalName).append("\n");
		buffer.append("[/SXVDEX]\n");
		return buffer.toString();
	}
}
