import org.apache.poi.ss.formula.functions.Function0Arg;
import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.ValueEval;

/**
 * Convenience base class for functions that only take zero arguments.
 *
 * @author Josh Micich
 */
public abstract class Fixed0ArgFunction implements Function0Arg {
	public final ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
		if (args.length != 0) {
			return ErrorEval.VALUE_INVALID;
		}
		return evaluate(srcRowIndex, srcColumnIndex);
	}
}
