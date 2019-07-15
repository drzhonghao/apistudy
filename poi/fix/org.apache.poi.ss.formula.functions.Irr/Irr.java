

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Function;


public final class Irr implements Function {
	public ValueEval evaluate(final ValueEval[] args, final int srcRowIndex, final int srcColumnIndex) {
		if (((args.length) == 0) || ((args.length) > 2)) {
			return ErrorEval.VALUE_INVALID;
		}
		double guess;
		if ((args.length) == 2) {
		}else {
			guess = 0.1;
		}
		return null;
	}

	public static double irr(double[] income) {
		return Irr.irr(income, 0.1);
	}

	public static double irr(double[] values, double guess) {
		final int maxIterationCount = 20;
		final double absoluteAccuracy = 1.0E-7;
		double x0 = guess;
		double x1;
		int i = 0;
		while (i < maxIterationCount) {
			final double factor = 1.0 + x0;
			int k = 0;
			double fValue = values[k];
			double fDerivative = 0;
			for (double denominator = factor; (++k) < (values.length);) {
				final double value = values[k];
				fValue += value / denominator;
				denominator *= factor;
				fDerivative -= (k * value) / denominator;
			}
			x1 = x0 - (fValue / fDerivative);
			if ((Math.abs((x1 - x0))) <= absoluteAccuracy) {
				return x1;
			}
			x0 = x1;
			++i;
		} 
		return Double.NaN;
	}
}

