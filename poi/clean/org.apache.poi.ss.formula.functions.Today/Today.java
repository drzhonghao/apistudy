import org.apache.poi.ss.formula.functions.*;


import java.util.Calendar;

import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.util.LocaleUtil;

/**
 * Implementation of Excel TODAY() Function<br>
 */
public final class Today extends Fixed0ArgFunction {
	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex) {
		Calendar now = LocaleUtil.getLocaleCalendar();
		now.clear(Calendar.HOUR);
        now.set(Calendar.HOUR_OF_DAY,0);
		now.clear(Calendar.MINUTE);
		now.clear(Calendar.SECOND);
		now.clear(Calendar.MILLISECOND);
		return new NumberEval(DateUtil.getExcelDate(now.getTime()));
	}
}
