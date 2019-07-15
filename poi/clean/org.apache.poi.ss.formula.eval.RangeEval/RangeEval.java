import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.eval.AreaEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.*;


import org.apache.poi.ss.formula.functions.Fixed2ArgFunction;
import org.apache.poi.ss.formula.functions.Function;


/**
 *
 * @author Josh Micich
 */
public final class RangeEval extends Fixed2ArgFunction {

	public static final Function instance = new RangeEval();

	private RangeEval() {
		// enforces singleton
	}

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {

		try {
			AreaEval reA = evaluateRef(arg0);
			AreaEval reB = evaluateRef(arg1);
			return resolveRange(reA, reB);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
	}

	/**
	 * @return simple rectangular {@link AreaEval} which fully encloses both areas
	 * <tt>aeA</tt> and <tt>aeB</tt>
	 */
	private static AreaEval resolveRange(AreaEval aeA, AreaEval aeB) {
		int aeAfr = aeA.getFirstRow();
		int aeAfc = aeA.getFirstColumn();

		int top = Math.min(aeAfr, aeB.getFirstRow());
		int bottom = Math.max(aeA.getLastRow(), aeB.getLastRow());
		int left = Math.min(aeAfc, aeB.getFirstColumn());
		int right = Math.max(aeA.getLastColumn(), aeB.getLastColumn());

		return aeA.offset(top-aeAfr, bottom-aeAfr, left-aeAfc, right-aeAfc);
	}

	private static AreaEval evaluateRef(ValueEval arg) throws EvaluationException {
		if (arg instanceof AreaEval) {
			return (AreaEval) arg;
		}
		if (arg instanceof RefEval) {
			return ((RefEval) arg).offset(0, 0, 0, 0);
		}
		if (arg instanceof ErrorEval) {
			throw new EvaluationException((ErrorEval)arg);
		}
		throw new IllegalArgumentException("Unexpected ref arg class (" + arg.getClass().getName() + ")");
	}
}
