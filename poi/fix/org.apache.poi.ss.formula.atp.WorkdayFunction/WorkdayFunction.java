

import java.util.Date;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.atp.WorkdayCalculator;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.FreeRefFunction;
import org.apache.poi.ss.usermodel.DateUtil;


final class WorkdayFunction implements FreeRefFunction {
	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		if (((args.length) < 2) || ((args.length) > 3)) {
			return ErrorEval.VALUE_INVALID;
		}
		int srcCellRow = ec.getRowIndex();
		int srcCellCol = ec.getColumnIndex();
		double start;
		int days;
		double[] holidays;
		ValueEval holidaysCell = ((args.length) == 3) ? args[2] : null;
		days = 0;
		start = 0.0;
		holidays = null;
		return new NumberEval(DateUtil.getExcelDate(WorkdayCalculator.instance.calculateWorkdays(start, days, holidays)));
	}
}

