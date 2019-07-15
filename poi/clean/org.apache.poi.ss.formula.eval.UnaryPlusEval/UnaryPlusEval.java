import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.*;


import org.apache.poi.ss.formula.functions.Fixed1ArgFunction;
import org.apache.poi.ss.formula.functions.Function;


/**
 * @author Amol S. Deshmukh &lt; amolweb at ya hoo dot com &gt;
 */
public final class UnaryPlusEval extends Fixed1ArgFunction {

	public static final Function instance = new UnaryPlusEval();

	private UnaryPlusEval() {
		// enforce singleton
	}

	public ValueEval evaluate(int srcCellRow, int srcCellCol, ValueEval arg0) {
		double d;
		try {
			ValueEval ve = OperandResolver.getSingleValue(arg0, srcCellRow, srcCellCol);
			if(ve instanceof StringEval) {
				// Note - asymmetric with UnaryMinus
				// -"hello" evaluates to #VALUE!
				// but +"hello" evaluates to "hello"
				return ve;
			}
			d = OperandResolver.coerceValueToDouble(ve);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		return new NumberEval(+d);
	}
}
