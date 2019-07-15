import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.ValueEval;

/**
 * Implementation of the DMin function:
 * Finds the minimum value of a column in an area with given conditions.
 * 
 * TODO:
 * - wildcards ? and * in string conditions
 * - functions as conditions
 */
public final class DMin implements IDStarAlgorithm {
    private ValueEval minimumValue;

    @Override
    public boolean processMatch(ValueEval eval) {
        if(eval instanceof NumericValueEval) {
            if(minimumValue == null) { // First match, just set the value.
                minimumValue = eval;
            } else { // There was a previous match, find the new minimum.
                double currentValue = ((NumericValueEval)eval).getNumberValue();
                double oldValue = ((NumericValueEval)minimumValue).getNumberValue();
                if(currentValue < oldValue) {
                    minimumValue = eval;
                }
            }
        }

        return true;
    }

    @Override
    public ValueEval getResult() {
        if(minimumValue == null) {
            return NumberEval.ZERO;
        } else {
            return minimumValue;
        }
    }
}
