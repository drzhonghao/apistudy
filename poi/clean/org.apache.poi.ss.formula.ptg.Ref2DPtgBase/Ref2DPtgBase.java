import org.apache.poi.ss.formula.ptg.RefPtgBase;
import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

/**
 * @author Josh Micich
 */
abstract class Ref2DPtgBase extends RefPtgBase {
	private final static int SIZE = 5;


	protected Ref2DPtgBase(int row, int column, boolean isRowRelative, boolean isColumnRelative) {
		setRow(row);
		setColumn(column);
		setRowRelative(isRowRelative);
		setColRelative(isColumnRelative);
	}

	protected Ref2DPtgBase(LittleEndianInput in)  {
		readCoordinates(in);
	}

	protected Ref2DPtgBase(CellReference cr) {
		super(cr);
	}

	@Override
    public void write(LittleEndianOutput out) {
		out.writeByte(getSid() + getPtgClass());
		writeCoordinates(out);
	}

	@Override
    public final String toFormulaString() {
		return formatReferenceAsString();
	}

	protected abstract byte getSid();

	@Override
    public final int getSize() {
		return SIZE;
	}

    @Override
    public final String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append(" [");
        sb.append(formatReferenceAsString());
        sb.append("]");
        return sb.toString();
    }
}
