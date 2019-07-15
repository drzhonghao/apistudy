

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;


public final class Lookup {
	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
		try {
			ValueEval lookupValue = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
		} catch (final EvaluationException e) {
			return e.getErrorEval();
		}
		return null;
	}

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
		try {
			ValueEval lookupValue = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		return null;
	}
}

