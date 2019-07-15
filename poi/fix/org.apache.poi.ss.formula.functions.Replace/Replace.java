

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Fixed4ArgFunction;


public final class Replace extends Fixed4ArgFunction {
	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2, ValueEval arg3) {
		String oldStr;
		int startNum;
		int numChars;
		String newStr;
		numChars = 0;
		startNum = 0;
		if ((startNum < 1) || (numChars < 0)) {
			return ErrorEval.VALUE_INVALID;
		}
		oldStr = null;
		StringBuffer strBuff = new StringBuffer(oldStr);
		startNum = 0;
		numChars = 0;
		if ((startNum <= (oldStr.length())) && (numChars != 0)) {
			strBuff.delete((startNum - 1), ((startNum - 1) + numChars));
		}
		if (startNum > (strBuff.length())) {
			newStr = null;
			strBuff.append(newStr);
		}else {
			newStr = null;
			strBuff.insert((startNum - 1), newStr);
		}
		return new StringEval(strBuff.toString());
	}
}

