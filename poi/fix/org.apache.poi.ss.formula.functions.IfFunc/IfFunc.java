

import org.apache.poi.ss.formula.eval.BlankEval;
import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.MissingArgEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

import static org.apache.poi.ss.formula.functions.Var2or3ArgFunction.<init>;


public final class IfFunc {
	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
		boolean b;
		try {
			b = IfFunc.evaluateFirstArg(arg0, srcRowIndex, srcColumnIndex);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		if (b) {
			if (arg1 == (MissingArgEval.instance)) {
				return BlankEval.instance;
			}
			return arg1;
		}
		return BoolEval.FALSE;
	}

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
		boolean b;
		try {
			b = IfFunc.evaluateFirstArg(arg0, srcRowIndex, srcColumnIndex);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		if (b) {
			if (arg1 == (MissingArgEval.instance)) {
				return BlankEval.instance;
			}
			return arg1;
		}
		if (arg2 == (MissingArgEval.instance)) {
			return BlankEval.instance;
		}
		return arg2;
	}

	public static boolean evaluateFirstArg(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
		ValueEval ve = OperandResolver.getSingleValue(arg, srcCellRow, srcCellCol);
		Boolean b = OperandResolver.coerceValueToBoolean(ve, false);
		if (b == null) {
			return false;
		}
		return b.booleanValue();
	}
}

