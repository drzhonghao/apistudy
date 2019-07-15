

import org.apache.poi.ss.formula.FormulaRenderingWorkbook;
import org.apache.poi.ss.formula.WorkbookDependentFormula;
import org.apache.poi.ss.formula.ptg.OperandPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;


public final class NamePtg extends OperandPtg implements WorkbookDependentFormula {
	public static final short sid = 35;

	private static final int SIZE = 5;

	private int field_1_label_index;

	private short field_2_zero;

	public NamePtg(int nameIndex) {
		field_1_label_index = 1 + nameIndex;
	}

	public NamePtg(LittleEndianInput in) {
		field_1_label_index = in.readUShort();
		field_2_zero = in.readShort();
	}

	public int getIndex() {
		return (field_1_label_index) - 1;
	}

	@Override
	public void write(LittleEndianOutput out) {
		out.writeByte(((NamePtg.sid) + (getPtgClass())));
		out.writeShort(field_1_label_index);
		out.writeShort(field_2_zero);
	}

	@Override
	public int getSize() {
		return NamePtg.SIZE;
	}

	@Override
	public String toFormulaString(FormulaRenderingWorkbook book) {
		return null;
	}

	@Override
	public String toFormulaString() {
		throw new RuntimeException("3D references need a workbook to determine formula text");
	}

	@Override
	public byte getDefaultOperandClass() {
		return Ptg.CLASS_REF;
	}
}

