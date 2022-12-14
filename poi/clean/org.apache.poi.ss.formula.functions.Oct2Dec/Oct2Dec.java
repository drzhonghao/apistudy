import org.apache.poi.ss.formula.functions.BaseNumberUtils;
import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

/**
 * <p>Implementation for Excel Oct2Dec() function.<p>
 * <p>
 * Converts an octal number to decimal.
 * </p>
 * <p>
 * <b>Syntax</b>:<br> <b>Oct2Dec  </b>(<b>number</b> )
 * </p>
 * <p>
 * Number     is the octal number you want to convert. Number may not contain more than 10 octal characters (30 bits).
 * The most significant bit of number is the sign bit. The remaining 29 bits are magnitude bits.
 * Negative numbers are represented using two's-complement notation..
 * <p>
 * If number is not a valid octal number, OCT2DEC returns the #NUM! error value.
 *
 * @author cedric dot walter @ gmail dot com
 */
public class Oct2Dec extends Fixed1ArgFunction implements FreeRefFunction {

    public static final FreeRefFunction instance = new Oct2Dec();

    static final int MAX_NUMBER_OF_PLACES = 10;
    static final int OCTAL_BASE = 8;

    public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval numberVE) {
        String octal = OperandResolver.coerceValueToString(numberVE);
        try {
           return new NumberEval(BaseNumberUtils.convertToDecimal(octal, OCTAL_BASE, MAX_NUMBER_OF_PLACES));
        }  catch (IllegalArgumentException e) {
            return ErrorEval.NUM_ERROR;
        }
    }

    public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
        if (args.length != 1) {
            return ErrorEval.VALUE_INVALID;
        }
        return evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0]);
    }
}
