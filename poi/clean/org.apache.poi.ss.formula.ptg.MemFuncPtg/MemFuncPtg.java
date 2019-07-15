import org.apache.poi.ss.formula.ptg.OperandPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

/**
 * @author Glen Stampoultzis (glens at apache.org)
 */
public final class MemFuncPtg extends OperandPtg {

	public final static byte sid = 0x29;
	private final int field_1_len_ref_subexpression;

	/**
	 * Creates new function pointer from a byte array usually called while
	 * reading an excel file.
	 */
	public MemFuncPtg(LittleEndianInput in)  {
		this(in.readUShort());
	}

	public MemFuncPtg(int subExprLen) {
		field_1_len_ref_subexpression = subExprLen;
	}

	public int getSize() {
		return 3;
	}

	public void write(LittleEndianOutput out) {
		out.writeByte(sid + getPtgClass());
		out.writeShort(field_1_len_ref_subexpression);
	}

	public String toFormulaString() {
		return "";
	}

	public byte getDefaultOperandClass() {
		return Ptg.CLASS_REF;
	}

	public int getNumberOfOperands() {
		return field_1_len_ref_subexpression;
	}

	public int getLenRefSubexpression() {
		return field_1_len_ref_subexpression;
	}
	@Override
	public final String toString() {
		StringBuffer sb = new StringBuffer(64);
		sb.append(getClass().getName()).append(" [len=");
		sb.append(field_1_len_ref_subexpression);
		sb.append("]");
		return sb.toString();
	}
}
