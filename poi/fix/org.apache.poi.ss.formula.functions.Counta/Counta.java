

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Function;


public final class Counta implements Function {
	public ValueEval evaluate(ValueEval[] args, int srcCellRow, int srcCellCol) {
		int nArgs = args.length;
		if (nArgs < 1) {
			return ErrorEval.VALUE_INVALID;
		}
		if (nArgs > 30) {
			return ErrorEval.VALUE_INVALID;
		}
		int temp = 0;
		for (int i = 0; i < nArgs; i++) {
		}
		return new NumberEval(temp);
	}

	public static Counta subtotalInstance() {
		return null;
	}
}

