import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.*;



/**
 * Common superclass of all {@link Ptg}s that represent simple constant values.
 *
 * @author Josh Micich
 */
public abstract class ScalarConstantPtg extends Ptg {
	public final boolean isBaseToken() {
		return true;
	}

	public final byte getDefaultOperandClass() {
		return Ptg.CLASS_VALUE;
	}

	public final String toString() {
		StringBuffer sb = new StringBuffer(64);
		sb.append(getClass().getName()).append(" [");
		sb.append(toFormulaString());
		sb.append("]");
		return sb.toString();
	}
}
