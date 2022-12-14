import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.functions.BaseNumberUtils;
import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.*;

/**
 * Implementation for Excel HEX2DEC() function.<p>
 * <p>
 * <b>Syntax</b>:<br> <b>HEX2DEC  </b>(<b>number</b>)<br>
 * <p>
 * Converts a hexadecimal number to decimal.
 * <p>
 * Number     is the hexadecimal number you want to convert. Number cannot contain more than 10 characters (40 bits).
 * The most significant bit of number is the sign bit.
 * The remaining 39 bits are magnitude bits. Negative numbers are represented using two's-complement notation.
 * Remark
 * If number is not a valid hexadecimal number, HEX2DEC returns the #NUM! error value.
 *
 * @author cedric dot walter @ gmail dot com
 */
public class Hex2Dec extends Fixed1ArgFunction implements FreeRefFunction {

    public static final FreeRefFunction instance = new Hex2Dec();

    static final int HEXADECIMAL_BASE = 16;
    static final int MAX_NUMBER_OF_PLACES = 10;

    public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval numberVE) {
        final String hex;
        if (numberVE instanceof RefEval) {
            RefEval re = (RefEval) numberVE;
            hex = OperandResolver.coerceValueToString(re.getInnerValueEval(re.getFirstSheetIndex()));
        } else {
            hex = OperandResolver.coerceValueToString(numberVE);
        }
        try {
            return new NumberEval(BaseNumberUtils.convertToDecimal(hex, HEXADECIMAL_BASE, MAX_NUMBER_OF_PLACES));
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
