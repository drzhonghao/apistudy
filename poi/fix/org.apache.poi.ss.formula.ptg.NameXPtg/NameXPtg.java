

import org.apache.poi.ss.formula.FormulaRenderingWorkbook;
import org.apache.poi.ss.formula.WorkbookDependentFormula;
import org.apache.poi.ss.formula.ptg.OperandPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;


public final class NameXPtg extends OperandPtg implements WorkbookDependentFormula {
	public static final short sid = 57;

	private static final int SIZE = 7;

	private final int _sheetRefIndex;

	private final int _nameNumber;

	private final int _reserved;

	private NameXPtg(int sheetRefIndex, int nameNumber, int reserved) {
		_sheetRefIndex = sheetRefIndex;
		_nameNumber = nameNumber;
		_reserved = reserved;
	}

	public NameXPtg(int sheetRefIndex, int nameIndex) {
		this(sheetRefIndex, (nameIndex + 1), 0);
	}

	public NameXPtg(LittleEndianInput in) {
		this(in.readUShort(), in.readUShort(), in.readUShort());
	}

	public void write(LittleEndianOutput out) {
		out.writeByte(((NameXPtg.sid) + (getPtgClass())));
		out.writeShort(_sheetRefIndex);
		out.writeShort(_nameNumber);
		out.writeShort(_reserved);
	}

	public int getSize() {
		return NameXPtg.SIZE;
	}

	public String toFormulaString(FormulaRenderingWorkbook book) {
		return null;
	}

	public String toFormulaString() {
		throw new RuntimeException("3D references need a workbook to determine formula text");
	}

	public String toString() {
		return ((("NameXPtg:[sheetRefIndex:" + (_sheetRefIndex)) + " , nameNumber:") + (_nameNumber)) + "]";
	}

	public byte getDefaultOperandClass() {
		return Ptg.CLASS_VALUE;
	}

	public int getSheetRefIndex() {
		return _sheetRefIndex;
	}

	public int getNameIndex() {
		return (_nameNumber) - 1;
	}
}

