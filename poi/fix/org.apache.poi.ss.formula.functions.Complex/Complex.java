

import java.util.Locale;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.FreeRefFunction;

import static org.apache.poi.ss.formula.functions.Var2or3ArgFunction.<init>;


public class Complex implements FreeRefFunction {
	public static final FreeRefFunction instance = new Complex();

	public static final String DEFAULT_SUFFIX = "i";

	public static final String SUPPORTED_SUFFIX = "j";

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval real_num, ValueEval i_num) {
		return this.evaluate(srcRowIndex, srcColumnIndex, real_num, i_num, new StringEval(Complex.DEFAULT_SUFFIX));
	}

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval real_num, ValueEval i_num, ValueEval suffix) {
		ValueEval veText1;
		try {
			veText1 = OperandResolver.getSingleValue(real_num, srcRowIndex, srcColumnIndex);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		double realNum = 0;
		try {
			realNum = OperandResolver.coerceValueToDouble(veText1);
		} catch (EvaluationException e) {
			return ErrorEval.VALUE_INVALID;
		}
		ValueEval veINum;
		try {
			veINum = OperandResolver.getSingleValue(i_num, srcRowIndex, srcColumnIndex);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		double realINum = 0;
		try {
			realINum = OperandResolver.coerceValueToDouble(veINum);
		} catch (EvaluationException e) {
			return ErrorEval.VALUE_INVALID;
		}
		String suffixValue = OperandResolver.coerceValueToString(suffix);
		if ((suffixValue.length()) == 0) {
			suffixValue = Complex.DEFAULT_SUFFIX;
		}
		if ((suffixValue.equals(Complex.DEFAULT_SUFFIX.toUpperCase(Locale.ROOT))) || (suffixValue.equals(Complex.SUPPORTED_SUFFIX.toUpperCase(Locale.ROOT)))) {
			return ErrorEval.VALUE_INVALID;
		}
		if (!((suffixValue.equals(Complex.DEFAULT_SUFFIX)) || (suffixValue.equals(Complex.SUPPORTED_SUFFIX)))) {
			return ErrorEval.VALUE_INVALID;
		}
		StringBuffer strb = new StringBuffer("");
		if (realNum != 0) {
			if (isDoubleAnInt(realNum)) {
				strb.append(((int) (realNum)));
			}else {
				strb.append(realNum);
			}
		}
		if (realINum != 0) {
			if ((strb.length()) != 0) {
				if (realINum > 0) {
					strb.append("+");
				}
			}
			if ((realINum != 1) && (realINum != (-1))) {
				if (isDoubleAnInt(realINum)) {
					strb.append(((int) (realINum)));
				}else {
					strb.append(realINum);
				}
			}
			strb.append(suffixValue);
		}
		return new StringEval(strb.toString());
	}

	private boolean isDoubleAnInt(double number) {
		return (number == (Math.floor(number))) && (!(Double.isInfinite(number)));
	}

	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		if ((args.length) == 2) {
			return evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0], args[1]);
		}
		if ((args.length) == 3) {
			return evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0], args[1], args[2]);
		}
		return ErrorEval.VALUE_INVALID;
	}
}

