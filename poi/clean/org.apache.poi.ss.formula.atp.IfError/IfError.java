import org.apache.poi.ss.formula.atp.*;


import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.FreeRefFunction;
/**
 * Implementation of 'Analysis Toolpak' Excel function IFERROR()<br>
 *
 * Returns an error text if there is an error in the evaluation<p>
 * 
 * <b>Syntax</b><br>
 * <b>IFERROR</b>(<b>expression</b>, <b>string</b>)
 * 
 * @author Johan Karlsteen
 */
final class IfError implements FreeRefFunction {

	public static final FreeRefFunction instance = new IfError();

	private IfError() {
		// enforce singleton
	}

	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		if (args.length != 2) {
			return ErrorEval.VALUE_INVALID;
		}

		ValueEval val;
		try {
			val = evaluateInternal(args[0], args[1], ec.getRowIndex(), ec.getColumnIndex());
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}

		return val;
	}

	private static ValueEval evaluateInternal(ValueEval arg, ValueEval iferror, int srcCellRow, int srcCellCol) throws EvaluationException {
		arg = WorkbookEvaluator.dereferenceResult(arg, srcCellRow, srcCellCol);
		if(arg instanceof ErrorEval) {
			return iferror;
		} else {
			return arg;
		}
	}
}
