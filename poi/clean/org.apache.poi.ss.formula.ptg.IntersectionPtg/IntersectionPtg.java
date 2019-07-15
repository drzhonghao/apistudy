import org.apache.poi.ss.formula.ptg.OperationPtg;
import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * @author Daniel Noll (daniel at nuix dot com dot au)
 */
public final class IntersectionPtg extends OperationPtg {
	public final static byte sid = 0x0f;

	public static final OperationPtg instance = new IntersectionPtg();

	private IntersectionPtg() {
		// enforce singleton
	}

	public final boolean isBaseToken() {
		return true;
	}

	public int getSize() {
		return 1;
	}

	public void write(LittleEndianOutput out) {
		out.writeByte(sid + getPtgClass());
	}

	public String toFormulaString() {
		return " ";
	}

	public String toFormulaString(String[] operands) {
		StringBuffer buffer = new StringBuffer();

		buffer.append(operands[0]);
		buffer.append(" ");
		buffer.append(operands[1]);
		return buffer.toString();
	}

	public int getNumberOfOperands() {
		return 2;
	}
}
