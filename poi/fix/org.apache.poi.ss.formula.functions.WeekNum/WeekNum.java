

import java.util.Calendar;
import java.util.Date;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Fixed2ArgFunction;
import org.apache.poi.ss.formula.functions.FreeRefFunction;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.util.LocaleUtil;


public class WeekNum extends Fixed2ArgFunction implements FreeRefFunction {
	public static final FreeRefFunction instance = new WeekNum();

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval serialNumVE, ValueEval returnTypeVE) {
		double serialNum;
		Calendar serialNumCalendar = LocaleUtil.getLocaleCalendar();
		serialNum = 0.0;
		serialNumCalendar.setTime(DateUtil.getJavaDate(serialNum, false));
		int returnType;
		try {
			ValueEval ve = OperandResolver.getSingleValue(returnTypeVE, srcRowIndex, srcColumnIndex);
			returnType = OperandResolver.coerceValueToInt(ve);
		} catch (EvaluationException e) {
			return ErrorEval.NUM_ERROR;
		}
		if ((returnType != 1) && (returnType != 2)) {
			return ErrorEval.NUM_ERROR;
		}
		return new NumberEval(this.getWeekNo(serialNumCalendar, returnType));
	}

	public int getWeekNo(Calendar cal, int weekStartOn) {
		if (weekStartOn == 1) {
			cal.setFirstDayOfWeek(Calendar.SUNDAY);
		}else {
			cal.setFirstDayOfWeek(Calendar.MONDAY);
		}
		return cal.get(Calendar.WEEK_OF_YEAR);
	}

	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		if ((args.length) == 2) {
			return evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0], args[1]);
		}
		return ErrorEval.VALUE_INVALID;
	}
}

