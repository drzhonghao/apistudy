

import org.apache.poi.ss.formula.FormulaRenderingWorkbook;
import org.apache.poi.ss.formula.WorkbookDependentFormula;
import org.apache.poi.ss.formula.ptg.OperandPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;


public final class DeletedRef3DPtg extends OperandPtg implements WorkbookDependentFormula {
	public static final byte sid = 60;

	private final int field_1_index_extern_sheet;

	private final int unused1;

	public DeletedRef3DPtg(LittleEndianInput in) {
		field_1_index_extern_sheet = in.readUShort();
		unused1 = in.readInt();
	}

	public DeletedRef3DPtg(int externSheetIndex) {
		field_1_index_extern_sheet = externSheetIndex;
		unused1 = 0;
	}

	public String toFormulaString(FormulaRenderingWorkbook book) {
		return null;
	}

	public String toFormulaString() {
		throw new RuntimeException("3D references need a workbook to determine formula text");
	}

	public byte getDefaultOperandClass() {
		return Ptg.CLASS_REF;
	}

	public int getSize() {
		return 7;
	}

	public void write(LittleEndianOutput out) {
		out.writeByte(((DeletedRef3DPtg.sid) + (getPtgClass())));
		out.writeShort(field_1_index_extern_sheet);
		out.writeInt(unused1);
	}
}

