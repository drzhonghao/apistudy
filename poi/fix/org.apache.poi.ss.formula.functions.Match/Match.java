

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;


public final class Match {
	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
		return Match.eval(srcRowIndex, srcColumnIndex, arg0, arg1, 1.0);
	}

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
		double match_type;
		try {
			match_type = Match.evaluateMatchTypeArg(arg2, srcRowIndex, srcColumnIndex);
		} catch (EvaluationException e) {
			return ErrorEval.REF_INVALID;
		}
		return Match.eval(srcRowIndex, srcColumnIndex, arg0, arg1, match_type);
	}

	private static ValueEval eval(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, double match_type) {
		boolean matchExact = match_type == 0;
		boolean findLargestLessThanOrEqual = match_type > 0;
		try {
			ValueEval lookupValue = OperandResolver.getSingleValue(arg0, srcRowIndex, srcColumnIndex);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		return null;
	}

	private static final class SingleValueVector {
		private final ValueEval _value;

		public SingleValueVector(ValueEval value) {
			_value = value;
		}

		public ValueEval getItem(int index) {
			if (index != 0) {
				throw new RuntimeException((("Invalid index (" + index) + ") only zero is allowed"));
			}
			return _value;
		}

		public int getSize() {
			return 1;
		}
	}

	private static double evaluateMatchTypeArg(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
		ValueEval match_type = OperandResolver.getSingleValue(arg, srcCellRow, srcCellCol);
		if (match_type instanceof ErrorEval) {
			throw new EvaluationException(((ErrorEval) (match_type)));
		}
		if (match_type instanceof NumericValueEval) {
			NumericValueEval ne = ((NumericValueEval) (match_type));
			return ne.getNumberValue();
		}
		if (match_type instanceof StringEval) {
			StringEval se = ((StringEval) (match_type));
			Double d = OperandResolver.parseDouble(se.getStringValue());
			if (d == null) {
				throw new EvaluationException(ErrorEval.VALUE_INVALID);
			}
			return d.doubleValue();
		}
		throw new RuntimeException((("Unexpected match_type type (" + (match_type.getClass().getName())) + ")"));
	}
}

