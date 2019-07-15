import org.apache.poi.ss.formula.functions.Function1Arg;
import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.ValueEval;

/**
 * Convenience base class for any function which must take two or three
 * arguments
 *
 * @author Josh Micich
 */
abstract class Var1or2ArgFunction implements Function1Arg, Function2Arg {

	public final ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
		switch (args.length) {
			case 1:
				return evaluate(srcRowIndex, srcColumnIndex, args[0]);
			case 2:
				return evaluate(srcRowIndex, srcColumnIndex, args[0], args[1]);
		}
		return ErrorEval.VALUE_INVALID;
	}
}
