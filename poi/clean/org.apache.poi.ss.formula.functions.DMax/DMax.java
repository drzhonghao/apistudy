import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.ValueEval;

/**
 * Implementation of the DMax function:
 * Finds the maximum value of a column in an area with given conditions.
 * 
 * TODO:
 * - wildcards ? and * in string conditions
 * - functions as conditions
 */
public final class DMax implements IDStarAlgorithm {
    private ValueEval maximumValue;

    @Override
    public boolean processMatch(ValueEval eval) {
        if(eval instanceof NumericValueEval) {
            if(maximumValue == null) { // First match, just set the value.
                maximumValue = eval;
            } else { // There was a previous match, find the new minimum.
                double currentValue = ((NumericValueEval)eval).getNumberValue();
                double oldValue = ((NumericValueEval)maximumValue).getNumberValue();
                if(currentValue > oldValue) {
                    maximumValue = eval;
                }
            }
        }

        return true;
    }

    @Override
    public ValueEval getResult() {
        if(maximumValue == null) {
            return NumberEval.ZERO;
        } else {
            return maximumValue;
        }
    }
}
