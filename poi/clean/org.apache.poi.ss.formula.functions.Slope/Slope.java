import org.apache.poi.ss.formula.functions.LinearRegressionFunction;
import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.LinearRegressionFunction.FUNCTION;

/**
 * Implementation of Excel function SLOPE()<p>
 *
 * Calculates the SLOPE of the linear regression line that is used to predict y values from x values<br>
 * (http://introcs.cs.princeton.edu/java/97data/LinearRegression.java.html)
 * <b>Syntax</b>:<br>
 * <b>SLOPE</b>(<b>arrayX</b>, <b>arrayY</b>)<p>
 *
 *
 * @author Johan Karlsteen
 */
public final class Slope extends Fixed2ArgFunction {
	
	private final LinearRegressionFunction func;
	public Slope() {
		func = new LinearRegressionFunction(FUNCTION.SLOPE);
	}
	
	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex,
			ValueEval arg0, ValueEval arg1) {
		return func.evaluate(srcRowIndex, srcColumnIndex, arg0, arg1);
	}
}

