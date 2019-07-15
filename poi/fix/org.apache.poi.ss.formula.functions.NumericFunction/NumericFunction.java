

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Fixed1ArgFunction;
import org.apache.poi.ss.formula.functions.Fixed2ArgFunction;
import org.apache.poi.ss.formula.functions.Function;

import static Var1or2ArgFunction.<init>;


public abstract class NumericFunction implements Function {
	static final double ZERO = 0.0;

	static final double TEN = 10.0;

	static final double LOG_10_TO_BASE_e = Math.log(NumericFunction.TEN);

	protected static double singleOperandEvaluate(ValueEval arg, int srcRowIndex, int srcColumnIndex) throws EvaluationException {
		if (arg == null) {
			throw new IllegalArgumentException("arg must not be null");
		}
		ValueEval ve = OperandResolver.getSingleValue(arg, srcRowIndex, srcColumnIndex);
		double result = OperandResolver.coerceValueToDouble(ve);
		NumericFunction.checkValue(result);
		return result;
	}

	public static void checkValue(double result) throws EvaluationException {
		if ((Double.isNaN(result)) || (Double.isInfinite(result))) {
			throw new EvaluationException(ErrorEval.NUM_ERROR);
		}
	}

	public final ValueEval evaluate(ValueEval[] args, int srcCellRow, int srcCellCol) {
		double result;
		try {
			result = eval(args, srcCellRow, srcCellCol);
			NumericFunction.checkValue(result);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		return new NumberEval(result);
	}

	protected abstract double eval(ValueEval[] args, int srcCellRow, int srcCellCol) throws EvaluationException;

	public static abstract class OneArg extends Fixed1ArgFunction {
		protected OneArg() {
		}

		public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
			double result;
			try {
				double d = NumericFunction.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
				result = evaluate(d);
				NumericFunction.checkValue(result);
			} catch (EvaluationException e) {
				return e.getErrorEval();
			}
			return new NumberEval(result);
		}

		protected final double eval(ValueEval[] args, int srcCellRow, int srcCellCol) throws EvaluationException {
			if ((args.length) != 1) {
				throw new EvaluationException(ErrorEval.VALUE_INVALID);
			}
			double d = NumericFunction.singleOperandEvaluate(args[0], srcCellRow, srcCellCol);
			return evaluate(d);
		}

		protected abstract double evaluate(double d) throws EvaluationException;
	}

	public static abstract class TwoArg extends Fixed2ArgFunction {
		protected TwoArg() {
		}

		public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
			double result;
			try {
				double d0 = NumericFunction.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
				double d1 = NumericFunction.singleOperandEvaluate(arg1, srcRowIndex, srcColumnIndex);
				result = evaluate(d0, d1);
				NumericFunction.checkValue(result);
			} catch (EvaluationException e) {
				return e.getErrorEval();
			}
			return new NumberEval(result);
		}

		protected abstract double evaluate(double d0, double d1) throws EvaluationException;
	}

	public static final Function ABS;

	public static final Function ACOS;

	public static final Function ACOSH;

	public static final Function ASIN;

	public static final Function ASINH;

	public static final Function ATAN;

	public static final Function ATANH;

	public static final Function COS;

	public static final Function COSH;

	public static final Function DEGREES;

	static final NumberEval DOLLAR_ARG2_DEFAULT = new NumberEval(2.0);

	public static final Function EXP;

	public static final Function FACT;

	public static final Function INT;

	public static final Function LN;

	public static final Function LOG10;

	public static final Function RADIANS;

	public static final Function SIGN;

	public static final Function SIN;

	public static final Function SINH;

	public static final Function SQRT;

	public static final Function TAN;

	public static final Function TANH;

	public static final Function ATAN2;

	public static final Function CEILING;

	public static final Function COMBIN;

	public static final Function FLOOR;

	public static final Function MOD;

	public static final Function POWER;

	public static final Function ROUND;

	public static final Function ROUNDDOWN;

	public static final Function ROUNDUP;

	static final NumberEval TRUNC_ARG2_DEFAULT = new NumberEval(0);

	private static final class Log {
		public Log() {
		}

		public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
			double result;
			try {
				double d0 = NumericFunction.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
				result = (Math.log(d0)) / (NumericFunction.LOG_10_TO_BASE_e);
				NumericFunction.checkValue(result);
			} catch (EvaluationException e) {
				return e.getErrorEval();
			}
			return new NumberEval(result);
		}

		public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
			double result;
			try {
				double d0 = NumericFunction.singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
				double d1 = NumericFunction.singleOperandEvaluate(arg1, srcRowIndex, srcColumnIndex);
				double logE = Math.log(d0);
				if ((Double.compare(d1, Math.E)) == 0) {
					result = logE;
				}else {
					result = logE / (Math.log(d1));
				}
				NumericFunction.checkValue(result);
			} catch (EvaluationException e) {
				return e.getErrorEval();
			}
			return new NumberEval(result);
		}
	}

	static final NumberEval PI_EVAL = new NumberEval(Math.PI);

	public static final Function PI;

	public static final Function RAND;

	public static final Function POISSON;
}

