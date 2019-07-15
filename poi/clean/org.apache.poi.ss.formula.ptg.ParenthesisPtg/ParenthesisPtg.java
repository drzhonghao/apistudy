import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * While formula tokens are stored in RPN order and thus do not need parenthesis
 * for precedence reasons, Parenthesis tokens ARE written to ensure that user
 * entered parenthesis are displayed as-is on reading back
 * 
 * Avik Sengupta &lt;lists@aviksengupta.com&gt; Andrew C. Oliver (acoliver at
 * apache dot org)
 * 
 * @author Jason Height (jheight at chariot dot net dot au)
 */
public final class ParenthesisPtg extends ControlPtg {

	private final static int SIZE = 1;
	public final static byte sid = 0x15;

	public static final ControlPtg instance = new ParenthesisPtg();

	private ParenthesisPtg() {
		// enforce singleton
	}

	public void write(LittleEndianOutput out) {
		out.writeByte(sid + getPtgClass());
	}

	public int getSize() {
		return SIZE;
	}

	public String toFormulaString() {
		return "()";
	}

	public String toFormulaString(String[] operands) {
		return "(" + operands[0] + ")";
	}
}
