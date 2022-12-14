import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.*;


import org.apache.poi.ss.formula.functions.ArrayFunction;
import org.apache.poi.ss.formula.functions.Fixed2ArgFunction;
import org.apache.poi.ss.formula.functions.Function;
import org.apache.poi.ss.formula.functions.MatrixFunction.MutableValueCollector;
import org.apache.poi.ss.formula.functions.MatrixFunction.TwoArrayArg;

/**
 * @author Josh Micich
 */
public abstract class TwoOperandNumericOperation extends Fixed2ArgFunction implements ArrayFunction {

	protected final double singleOperandEvaluate(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
		ValueEval ve = OperandResolver.getSingleValue(arg, srcCellRow, srcCellCol);
		return OperandResolver.coerceValueToDouble(ve);
	}
	
	public ValueEval evaluateArray(ValueEval args[], int srcRowIndex, int srcColumnIndex) {
	    if (args.length != 2) {
	        return ErrorEval.VALUE_INVALID;
	    }
	    return new ArrayEval().evaluate(srcRowIndex, srcColumnIndex, args[0], args[1]);
	}
	
	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0, ValueEval arg1) {
		double result;
		try {
			double d0 = singleOperandEvaluate(arg0, srcRowIndex, srcColumnIndex);
			double d1 = singleOperandEvaluate(arg1, srcRowIndex, srcColumnIndex);
			result = evaluate(d0, d1);
			if (result == 0.0) { // this '==' matches +0.0 and -0.0
				// Excel converts -0.0 to +0.0 for '*', '/', '%', '+' and '^'
				if (!(this instanceof SubtractEvalClass)) {
					return NumberEval.ZERO;
				}
			}
			if (Double.isNaN(result) || Double.isInfinite(result)) {
				return ErrorEval.NUM_ERROR;
			}
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		return new NumberEval(result);
	}

	protected abstract double evaluate(double d0, double d1) throws EvaluationException;

	private final class ArrayEval extends TwoArrayArg {
	    private final MutableValueCollector instance = new MutableValueCollector(true, true);
        
        protected double[] collectValues(ValueEval arg) throws EvaluationException {
            return instance.collectValues(arg);
        }
	    
	    protected double[][] evaluate(double[][] d1, double[][] d2) throws IllegalArgumentException, EvaluationException {
	        int width = (d1[0].length < d2[0].length) ? d1[0].length : d2[0].length;
	        int height = (d1.length < d2.length) ? d1.length : d2.length;
	        
	        double result[][] = new double[height][width];
	        
	        for (int j = 0; j < height; j++) {
	            for (int i = 0; i < width; i++) {
	                result[j][i] = TwoOperandNumericOperation.this.evaluate(d1[j][i], d2[j][i]);
	            }
	        }
	        
	        return result;
	    }
	}
	
	public static final Function AddEval = new TwoOperandNumericOperation() {
		protected double evaluate(double d0, double d1) {
			return d0+d1;
		}
	};
	public static final Function DivideEval = new TwoOperandNumericOperation() {
		protected double evaluate(double d0, double d1) throws EvaluationException {
			if (d1 == 0.0) {
				throw new EvaluationException(ErrorEval.DIV_ZERO);
			}
			return d0/d1;
		}
	};
	public static final Function MultiplyEval = new TwoOperandNumericOperation() {
		protected double evaluate(double d0, double d1) {
			return d0*d1;
		}
	};
	public static final Function PowerEval = new TwoOperandNumericOperation() {
		protected double evaluate(double d0, double d1) {
			if(d0 < 0 && Math.abs(d1) > 0.0 && Math.abs(d1) < 1.0) {
				return -1 * Math.pow(d0 * -1, d1);
			}
			return Math.pow(d0, d1);
		}
	};
	private static final class SubtractEvalClass extends TwoOperandNumericOperation {
		public SubtractEvalClass() {
			//
		}
		protected double evaluate(double d0, double d1) {
			return d0-d1;
		}
	}
	public static final Function SubtractEval = new SubtractEvalClass();
}
