import org.apache.poi.ss.formula.functions.*;


import java.util.Date;

import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.usermodel.DateUtil;

/**
 * Implementation of Excel NOW() Function
 *
 * @author Frank Taffelt
 */
public final class Now extends Fixed0ArgFunction {

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex) {
		Date now = new Date(System.currentTimeMillis());
		return new NumberEval(DateUtil.getExcelDate(now));
	}
}
