import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.eval.AreaEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.RefEval;
import org.apache.poi.ss.formula.eval.ValueEval;

/**
 * Implementation for the Excel function ROW
 *
 * @author Josh Micich
 */
public final class RowFunc implements Function0Arg, Function1Arg {

    public ValueEval evaluate(int srcRowIndex, int srcColumnIndex) {
        return new NumberEval(srcRowIndex+1);
    }
    public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval arg0) {
        int rnum;

        if (arg0 instanceof AreaEval) {
            rnum = ((AreaEval) arg0).getFirstRow();
        } else if (arg0 instanceof RefEval) {
            rnum = ((RefEval) arg0).getRow();
        } else {
            // anything else is not valid argument
            return ErrorEval.VALUE_INVALID;
        }

        return new NumberEval(rnum + 1);
    }
    public ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
        switch (args.length) {
            case 1:
                return evaluate(srcRowIndex, srcColumnIndex, args[0]);
            case 0:
              return new NumberEval(srcRowIndex+1);
        }
        return ErrorEval.VALUE_INVALID;
    }
}
