

import java.util.Locale;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Fixed1ArgFunction;
import org.apache.poi.ss.formula.functions.Function;
import org.apache.poi.ss.usermodel.DataFormatter;


public abstract class TextFunction implements Function {
	protected static final DataFormatter formatter = new DataFormatter();

	protected static String evaluateStringArg(ValueEval eval, int srcRow, int srcCol) throws EvaluationException {
		ValueEval ve = OperandResolver.getSingleValue(eval, srcRow, srcCol);
		return OperandResolver.coerceValueToString(ve);
	}

	protected static int evaluateIntArg(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
		ValueEval ve = OperandResolver.getSingleValue(arg, srcCellRow, srcCellCol);
		return OperandResolver.coerceValueToInt(ve);
	}

	protected static double evaluateDoubleArg(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
		ValueEval ve = OperandResolver.getSingleValue(arg, srcCellRow, srcCellCol);
		return OperandResolver.coerceValueToDouble(ve);
	}

	public final ValueEval evaluate(ValueEval[] args, int srcCellRow, int srcCellCol) {
		try {
			return evaluateFunc(args, srcCellRow, srcCellCol);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
	}

	protected abstract ValueEval evaluateFunc(ValueEval[] args, int srcCellRow, int srcCellCol) throws EvaluationException;

	private static abstract class SingleArgTextFunc extends Fixed1ArgFunction {
		protected SingleArgTextFunc() {
		}

		public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
			String arg;
			try {
				arg = TextFunction.evaluateStringArg(arg0, srcRowIndex, srcColumnIndex);
			} catch (EvaluationException e) {
				return e.getErrorEval();
			}
			return evaluate(arg);
		}

		protected abstract ValueEval evaluate(String arg);
	}

	public static final Function CHAR = null;

	public static final Function LEN = null;

	public static final Function LOWER = null;

	public static final Function UPPER = null;

	public static final Function PROPER = null;

	public static final Function TRIM = null;

	public static final Function CLEAN = null;

	public static final Function MID = null;

	private static final class LeftRight {
		private static final ValueEval DEFAULT_ARG1 = new NumberEval(1.0);

		private final boolean _isLeft;

		protected LeftRight(boolean isLeft) {
			_isLeft = isLeft;
		}

		public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
			return evaluate(srcRowIndex, srcColumnIndex, arg0, TextFunction.LeftRight.DEFAULT_ARG1);
		}

		public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
			String arg;
			int index;
			try {
				arg = TextFunction.evaluateStringArg(arg0, srcRowIndex, srcColumnIndex);
				index = TextFunction.evaluateIntArg(arg1, srcRowIndex, srcColumnIndex);
			} catch (EvaluationException e) {
				return e.getErrorEval();
			}
			if (index < 0) {
				return ErrorEval.VALUE_INVALID;
			}
			String result;
			if (_isLeft) {
				result = arg.substring(0, Math.min(arg.length(), index));
			}else {
				result = arg.substring(Math.max(0, ((arg.length()) - index)));
			}
			return new StringEval(result);
		}
	}

	public static final Function CONCATENATE = null;

	public static final Function EXACT = null;

	public static final Function TEXT = null;

	private static final class SearchFind {
		private final boolean _isCaseSensitive;

		public SearchFind(boolean isCaseSensitive) {
			_isCaseSensitive = isCaseSensitive;
		}

		public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
			try {
				String needle = TextFunction.evaluateStringArg(arg0, srcRowIndex, srcColumnIndex);
				String haystack = TextFunction.evaluateStringArg(arg1, srcRowIndex, srcColumnIndex);
				return eval(haystack, needle, 0);
			} catch (EvaluationException e) {
				return e.getErrorEval();
			}
		}

		public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1, ValueEval arg2) {
			try {
				String needle = TextFunction.evaluateStringArg(arg0, srcRowIndex, srcColumnIndex);
				String haystack = TextFunction.evaluateStringArg(arg1, srcRowIndex, srcColumnIndex);
				int startpos = (TextFunction.evaluateIntArg(arg2, srcRowIndex, srcColumnIndex)) - 1;
				if (startpos < 0) {
					return ErrorEval.VALUE_INVALID;
				}
				return eval(haystack, needle, startpos);
			} catch (EvaluationException e) {
				return e.getErrorEval();
			}
		}

		private ValueEval eval(String haystack, String needle, int startIndex) {
			int result;
			if (_isCaseSensitive) {
				result = haystack.indexOf(needle, startIndex);
			}else {
				result = haystack.toUpperCase(Locale.ROOT).indexOf(needle.toUpperCase(Locale.ROOT), startIndex);
			}
			if (result == (-1)) {
				return ErrorEval.VALUE_INVALID;
			}
			return new NumberEval((result + 1));
		}
	}
}

