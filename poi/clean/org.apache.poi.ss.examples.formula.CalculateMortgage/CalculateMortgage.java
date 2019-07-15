import org.apache.poi.ss.examples.formula.*;


import org.apache.poi.ss.formula.OperationEvaluationContext ;
import org.apache.poi.ss.formula.eval.ErrorEval ;
import org.apache.poi.ss.formula.eval.EvaluationException ;
import org.apache.poi.ss.formula.eval.NumberEval ;
import org.apache.poi.ss.formula.eval.OperandResolver ;
import org.apache.poi.ss.formula.eval.ValueEval ;
import org.apache.poi.ss.formula.functions.FreeRefFunction ;

/**
 * A simple user-defined function to calculate principal and interest.
 * 
 * @author Jon Svede ( jon [at] loquatic [dot] com )
 * @author Brian Bush ( brian [dot] bush [at] nrel [dot] gov )
 *
 */
public class CalculateMortgage implements FreeRefFunction {

    @Override
    public ValueEval evaluate( ValueEval[] args, OperationEvaluationContext ec ) {
        
        // verify that we have enough data
        if (args.length != 3) {  
            return ErrorEval.VALUE_INVALID;
        }

        // declare doubles for values
        double principal, rate, years,  result;
        try {
            // extract values as ValueEval
            ValueEval v1 = OperandResolver.getSingleValue( args[0], 
                                                           ec.getRowIndex(), 
                                                           ec.getColumnIndex() ) ;
            ValueEval v2 = OperandResolver.getSingleValue( args[1], 
                                                           ec.getRowIndex(), 
                                                           ec.getColumnIndex() ) ;
            ValueEval v3 = OperandResolver.getSingleValue( args[2], 
                                                           ec.getRowIndex(), 
                                                           ec.getColumnIndex() ) ;

            // get data as doubles
            principal  = OperandResolver.coerceValueToDouble( v1 ) ; 
            rate  = OperandResolver.coerceValueToDouble( v2 ) ;
            years = OperandResolver.coerceValueToDouble( v3 ) ;
            
            result = calculateMortgagePayment( principal, rate, years ) ;
            System.out.println( "Result = " + result ) ;

            checkValue(result);
            
        } catch (EvaluationException e) {
            return e.getErrorEval();
        }

        return new NumberEval( result ) ;
    }
    
    public double calculateMortgagePayment( double p, double r, double y ) {
        double i = r / 12 ;
        double n = y * 12 ;

        return p * (( i * Math.pow((1 + i),n ) ) / ( Math.pow((1 + i),n) - 1));
    }
    /**
     * Excel does not support infinities and NaNs, rather, it gives a #NUM! error in these cases
     *
     * @throws EvaluationException (#NUM!) if <tt>result</tt> is <tt>NaN</> or <tt>Infinity</tt>
     */
     private void checkValue(double result) throws EvaluationException {
         if (Double.isNaN(result) || Double.isInfinite(result)) {
             throw new EvaluationException(ErrorEval.NUM_ERROR);
         }
     }    
}
