

import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;


public final class Vlookup {
	private static final ValueEval DEFAULT_ARG3 = BoolEval.TRUE;

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
		return evaluate(srcRowIndex, srcColumnIndex, arg0, arg1, arg2, Vlookup.DEFAULT_ARG3);
	}

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval lookup_value, ValueEval table_array, ValueEval col_index, ValueEval range_lookup) {
		try {
			ValueEval lookupValue = OperandResolver.getSingleValue(lookup_value, srcRowIndex, srcColumnIndex);
			boolean isRangeLookup;
			try {
			} catch (RuntimeException e) {
				isRangeLookup = true;
			}
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		return null;
	}
}

