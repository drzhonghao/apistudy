import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.TwoDEval;

/**
 * Implementation for Excel COLUMNS function.
 *
 * @author Josh Micich
 */
public final class Columns extends Fixed1ArgFunction {

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {

		int result;
		if (arg0 instanceof TwoDEval) {
			result = ((TwoDEval) arg0).getWidth();
		} else if (arg0 instanceof RefEval) {
			result = 1;
		} else { // anything else is not valid argument
			return ErrorEval.VALUE_INVALID;
		}
		return new NumberEval(result);
	}
}
