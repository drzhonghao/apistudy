import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.NumericValueEval;
import org.apache.poi.ss.formula.eval.ValueEval;

/**
 * Implementation of the DSum function:
 * Finds the total value of matching values in a column in an area with given conditions.
 * 
 * TODO:
 * - wildcards ? and * in string conditions
 * - functions as conditions
 */
public final class DSum implements IDStarAlgorithm {
    private double totalValue = 0;

    @Override
    public boolean processMatch(ValueEval eval) {
        if(eval instanceof NumericValueEval) {
            double currentValue = ((NumericValueEval)eval).getNumberValue();
            totalValue += currentValue;
        }

        return true;
    }

    @Override
    public ValueEval getResult() {
        return new NumberEval(totalValue);
    }
}
