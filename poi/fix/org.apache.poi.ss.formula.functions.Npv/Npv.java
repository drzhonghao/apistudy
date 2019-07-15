

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Function;


public final class Npv implements Function {
	public ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
		int nArgs = args.length;
		if (nArgs < 2) {
			return ErrorEval.VALUE_INVALID;
		}
		ValueEval[] vargs = new ValueEval[(args.length) - 1];
		System.arraycopy(args, 1, vargs, 0, vargs.length);
		return null;
	}
}

