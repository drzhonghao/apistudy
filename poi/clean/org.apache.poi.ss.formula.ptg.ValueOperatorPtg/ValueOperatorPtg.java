import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * Common superclass of all value operators. Subclasses include all unary and
 * binary operators except for the reference operators (IntersectionPtg,
 * RangePtg, UnionPtg)
 * 
 * @author Josh Micich
 */
public abstract class ValueOperatorPtg extends OperationPtg {

	/**
	 * All Operator <tt>Ptg</tt>s are base tokens (i.e. are not RVA classified)
	 */
	public final boolean isBaseToken() {
		return true;
	}

	public final byte getDefaultOperandClass() {
		return Ptg.CLASS_VALUE;
	}

	public void write(LittleEndianOutput out) {
		out.writeByte(getSid());
	}

	protected abstract byte getSid();

	public final int getSize() {
		return 1;
	}

	public final String toFormulaString() {
		// TODO - prune this method out of the hierarchy
		throw new RuntimeException("toFormulaString(String[] operands) should be used for subclasses of OperationPtgs");
	}
}
