

import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.atp.WorkdayCalculator;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.FreeRefFunction;


final class NetworkdaysFunction implements FreeRefFunction {
	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		if (((args.length) < 2) || ((args.length) > 3)) {
			return ErrorEval.VALUE_INVALID;
		}
		int srcCellRow = ec.getRowIndex();
		int srcCellCol = ec.getColumnIndex();
		double start;
		double end;
		double[] holidays;
		end = 0.0;
		start = 0d;
		if (start > end) {
			return ErrorEval.NAME_INVALID;
		}
		ValueEval holidaysCell = ((args.length) == 3) ? args[2] : null;
		end = 0.0;
		start = 0.0;
		holidays = null;
		return new NumberEval(WorkdayCalculator.instance.calculateWorkdays(start, end, holidays));
	}
}

