import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.*;

import java.math.BigDecimal;

/**
 * Implementation for Excel DELTA() function.<p>
 * <p>
 * <b>Syntax</b>:<br> <b>DELTA </b>(<b>number1</b>,<b>number2</b> )<br>
 * <p>
 * Tests whether two values are equal. Returns 1 if number1 = number2; returns 0 otherwise.
 * Use this function to filter a set of values. For example, by summing several DELTA functions
 * you calculate the count of equal pairs. This function is also known as the Kronecker Delta function.
 *
 * <ul>
 *     <li>If number1 is nonnumeric, DELTA returns the #VALUE! error value.</li>
 *     <li>If number2 is nonnumeric, DELTA returns the #VALUE! error value.</li>
 * </ul>
 *
 * @author cedric dot walter @ gmail dot com
 */
public final class Delta extends Fixed2ArgFunction implements FreeRefFunction {

    public static final FreeRefFunction instance = new Delta();

    private final static NumberEval ONE = new NumberEval(1);
    private final static NumberEval ZERO = new NumberEval(0);

    public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg1, ValueEval arg2) {
        ValueEval veText1;
        try {
            veText1 = OperandResolver.getSingleValue(arg1, srcRowIndex, srcColumnIndex);
        } catch (EvaluationException e) {
            return e.getErrorEval();
        }
        String strText1 = OperandResolver.coerceValueToString(veText1);
        Double number1 = OperandResolver.parseDouble(strText1);
        if (number1 == null) {
            return ErrorEval.VALUE_INVALID;
        }

        ValueEval veText2;
        try {
            veText2 = OperandResolver.getSingleValue(arg2, srcRowIndex, srcColumnIndex);
        } catch (EvaluationException e) {
            return e.getErrorEval();
        }

        String strText2 = OperandResolver.coerceValueToString(veText2);
        Double number2 = OperandResolver.parseDouble(strText2);
        if (number2 == null) {
            return ErrorEval.VALUE_INVALID;
        }

        int result = new BigDecimal(number1.doubleValue()).compareTo(new BigDecimal(number2.doubleValue()));
        return result == 0 ? ONE : ZERO;
    }

    public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
         if (args.length == 2) {
            return evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0], args[1]);
        }

        return ErrorEval.VALUE_INVALID;
    }
}
