

import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;


public final class Hlookup {
	private static final ValueEval DEFAULT_ARG3 = BoolEval.TRUE;

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
		return evaluate(srcRowIndex, srcColumnIndex, arg0, arg1, arg2, Hlookup.DEFAULT_ARG3);
	}

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2, ValueEval arg3) {
		try {
			ValueEval lookupValue = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		return null;
	}
}

