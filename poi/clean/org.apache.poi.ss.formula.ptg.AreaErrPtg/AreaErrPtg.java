import org.apache.poi.ss.formula.ptg.OperandPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

/**
 * AreaErr - handles deleted cell area references.
 *
 * @author Daniel Noll (daniel at nuix dot com dot au)
 */
public final class AreaErrPtg extends OperandPtg {
	public final static byte sid = 0x2B;
	private final int unused1;
	private final int unused2;

	public AreaErrPtg() {
		unused1 = 0;
		unused2 = 0;
	}

	public AreaErrPtg(LittleEndianInput in)  {
		// 8 bytes unused:
		unused1 = in.readInt();
		unused2 = in.readInt();
	}

	public void write(LittleEndianOutput out) {
		out.writeByte(sid + getPtgClass());
		out.writeInt(unused1);
		out.writeInt(unused2);
	}

	public String toFormulaString() {
		return FormulaError.REF.getString();
	}

	public byte getDefaultOperandClass() {
		return Ptg.CLASS_REF;
	}

	public int getSize() {
		return 9;
	}
}

