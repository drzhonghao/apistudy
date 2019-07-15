

import org.apache.poi.ss.formula.eval.AreaEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.ValueEval;


public final class Sumif {
	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
		AreaEval aeRange;
		try {
			aeRange = Sumif.convertRangeArg(arg0);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		return Sumif.eval(srcRowIndex, srcColumnIndex, arg1, aeRange, aeRange);
	}

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
		AreaEval aeRange;
		AreaEval aeSum;
		try {
			aeRange = Sumif.convertRangeArg(arg0);
			aeSum = Sumif.createSumRange(arg2, aeRange);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		return Sumif.eval(srcRowIndex, srcColumnIndex, arg1, aeRange, aeSum);
	}

	private static ValueEval eval(int srcRowIndex, int srcColumnIndex, ValueEval arg1, AreaEval aeRange, AreaEval aeSum) {
		return null;
	}

	private static AreaEval createSumRange(ValueEval eval, AreaEval aeRange) throws EvaluationException {
		if (eval instanceof AreaEval) {
			return ((AreaEval) (eval)).offset(0, ((aeRange.getHeight()) - 1), 0, ((aeRange.getWidth()) - 1));
		}
		if (eval instanceof RefEval) {
			return ((RefEval) (eval)).offset(0, ((aeRange.getHeight()) - 1), 0, ((aeRange.getWidth()) - 1));
		}
		throw new EvaluationException(ErrorEval.VALUE_INVALID);
	}

	private static AreaEval convertRangeArg(ValueEval eval) throws EvaluationException {
		if (eval instanceof AreaEval) {
			return ((AreaEval) (eval));
		}
		if (eval instanceof RefEval) {
			return ((RefEval) (eval)).offset(0, 0, 0, 0);
		}
		throw new EvaluationException(ErrorEval.VALUE_INVALID);
	}
}

