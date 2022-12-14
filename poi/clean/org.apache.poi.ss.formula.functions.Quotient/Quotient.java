import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ValueEval;

import org.apache.poi.ss.formula.eval.*;

/**
 * <p>Implementation for Excel QUOTIENT () function.<p>
 * <p>
 * <b>Syntax</b>:<br> <b>QUOTIENT</b>(<b>Numerator</b>,<b>Denominator</b>)<br>
 * <p>
 * <p>
 * Numerator     is the dividend.
 * Denominator     is the divisor.
 *
 * Returns the integer portion of a division. Use this function when you want to discard the remainder of a division.
 * <p>
 *
 * If either enumerator/denominator is non numeric, QUOTIENT returns the #VALUE! error value.
 * If denominator is equals to zero, QUOTIENT returns the #DIV/0! error value.
 */
public class Quotient extends Fixed2ArgFunction implements FreeRefFunction {

    public static final FreeRefFunction instance = new Quotient();

    public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval venumerator, ValueEval vedenominator) {

        double enumerator = 0;
        try {
            enumerator = OperandResolver.coerceValueToDouble(venumerator);
        } catch (EvaluationException e) {
            return ErrorEval.VALUE_INVALID;
        }

        double denominator = 0;
        try {
            denominator = OperandResolver.coerceValueToDouble(vedenominator);
        } catch (EvaluationException e) {
            return ErrorEval.VALUE_INVALID;
        }

        if (denominator == 0) {
            return ErrorEval.DIV_ZERO;
        }

        return new NumberEval((int)(enumerator / denominator));
    }

    public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
        if (args.length != 2) {
            return ErrorEval.VALUE_INVALID;
        }
        return evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0], args[1]);
    }
}
