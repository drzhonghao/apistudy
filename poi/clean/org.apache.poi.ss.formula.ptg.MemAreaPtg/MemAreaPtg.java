import org.apache.poi.ss.formula.ptg.OperandPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

/**
 * @author Daniel Noll (daniel at nuix dot com dot au)
 */
public final class MemAreaPtg extends OperandPtg {
	public final static short sid = 0x26;
	private final static int SIZE = 7;
	private final int field_1_reserved;
	private final int field_2_subex_len;

	/** Creates new MemAreaPtg */

	public MemAreaPtg(int subexLen) {
		field_1_reserved = 0;
		field_2_subex_len = subexLen;
	}

	public MemAreaPtg(LittleEndianInput in)  {
		field_1_reserved = in.readInt();
		field_2_subex_len = in.readShort();
	}

	public int getLenRefSubexpression() {
		return field_2_subex_len;
	}

	public void write(LittleEndianOutput out) {
		out.writeByte(sid + getPtgClass());
		out.writeInt(field_1_reserved);
		out.writeShort(field_2_subex_len);
	}

	public int getSize() {
		return SIZE;
	}

	public String toFormulaString() {
		return ""; // TODO: Not sure how to format this. -- DN
	}

	public byte getDefaultOperandClass() {
		return Ptg.CLASS_VALUE;
	}

	@Override
	public final String toString() {
		StringBuffer sb = new StringBuffer(64);
		sb.append(getClass().getName()).append(" [len=");
		sb.append(field_2_subex_len);
		sb.append("]");
		return sb.toString();
	}
}
