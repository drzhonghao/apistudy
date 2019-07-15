

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.formula.LazyRefEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Function;


public class Subtotal implements Function {
	private static Function findFunction(int functionCode) throws EvaluationException {
		if ((functionCode > 100) && (functionCode < 112)) {
			throw new NotImplementedException("SUBTOTAL - with 'exclude hidden values' option");
		}
		throw EvaluationException.invalidValue();
	}

	public ValueEval evaluate(ValueEval[] args, int srcRowIndex, int srcColumnIndex) {
		int nInnerArgs = (args.length) - 1;
		if (nInnerArgs < 1) {
			return ErrorEval.VALUE_INVALID;
		}
		final Function innerFunc;
		try {
			ValueEval ve = OperandResolver.getSingleValue(args[0], srcRowIndex, srcColumnIndex);
			int functionCode = OperandResolver.coerceValueToInt(ve);
			innerFunc = Subtotal.findFunction(functionCode);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		final List<ValueEval> list = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
		Iterator<ValueEval> it = list.iterator();
		while (it.hasNext()) {
			ValueEval eval = it.next();
			if (eval instanceof LazyRefEval) {
				LazyRefEval lazyRefEval = ((LazyRefEval) (eval));
				if (lazyRefEval.isSubTotal()) {
					it.remove();
				}
			}
		} 
		return innerFunc.evaluate(list.toArray(new ValueEval[list.size()]), srcRowIndex, srcColumnIndex);
	}
}

