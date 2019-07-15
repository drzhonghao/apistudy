import org.apache.poi.ss.formula.functions.Function3Arg;
import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.ValueEval;

/**
 * Convenience base class for any function which must take two or three
 * arguments
 *
 * @author Josh Micich
 */
abstract class Var2or3ArgFunction implements Function2Arg, Function3Arg {

	public final ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
		switch (args.length) {
			case 2:
				return evaluate(srcRowIndex, srcColumnIndex, args[0], args[1]);
			case 3:
				return evaluate(srcRowIndex, srcColumnIndex, args[0], args[1], args[2]);
		}
		return ErrorEval.VALUE_INVALID;
	}
}
