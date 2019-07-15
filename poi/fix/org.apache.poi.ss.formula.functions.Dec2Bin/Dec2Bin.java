

import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.StringEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.FreeRefFunction;

import static org.apache.poi.ss.formula.functions.Var1or2ArgFunction.<init>;


public class Dec2Bin implements FreeRefFunction {
	public static final FreeRefFunction instance = new Dec2Bin();

	private static final long MIN_VALUE = -512;

	private static final long MAX_VALUE = 511;

	private static final int DEFAULT_PLACES_VALUE = 10;

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval numberVE, ValueEval placesVE) {
		ValueEval veText1;
		try {
			veText1 = OperandResolver.getSingleValue(numberVE, srcRowIndex, srcColumnIndex);
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}
		String strText1 = OperandResolver.coerceValueToString(veText1);
		Double number = OperandResolver.parseDouble(strText1);
		if (number == null) {
			return ErrorEval.VALUE_INVALID;
		}
		if (((number.longValue()) < (Dec2Bin.MIN_VALUE)) || ((number.longValue()) > (Dec2Bin.MAX_VALUE))) {
			return ErrorEval.NUM_ERROR;
		}
		int placesNumber;
		if ((number < 0) || (placesVE == null)) {
			placesNumber = Dec2Bin.DEFAULT_PLACES_VALUE;
		}else {
			ValueEval placesValueEval;
			try {
				placesValueEval = OperandResolver.getSingleValue(placesVE, srcRowIndex, srcColumnIndex);
			} catch (EvaluationException e) {
				return e.getErrorEval();
			}
			String placesStr = OperandResolver.coerceValueToString(placesValueEval);
			Double placesNumberDouble = OperandResolver.parseDouble(placesStr);
			if (placesNumberDouble == null) {
				return ErrorEval.VALUE_INVALID;
			}
			placesNumber = placesNumberDouble.intValue();
			if ((placesNumber < 0) || (placesNumber == 0)) {
				return ErrorEval.NUM_ERROR;
			}
		}
		String binary = Integer.toBinaryString(number.intValue());
		if ((binary.length()) > (Dec2Bin.DEFAULT_PLACES_VALUE)) {
			binary = binary.substring(((binary.length()) - (Dec2Bin.DEFAULT_PLACES_VALUE)), binary.length());
		}
		if ((binary.length()) > placesNumber) {
			return ErrorEval.NUM_ERROR;
		}
		return new StringEval(binary);
	}

	public ValueEval evaluate(int srcRowIndex, int srcColumnIndex, ValueEval numberVE) {
		return this.evaluate(srcRowIndex, srcColumnIndex, numberVE, null);
	}

	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		if ((args.length) == 1) {
			return evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0]);
		}
		if ((args.length) == 2) {
			return evaluate(ec.getRowIndex(), ec.getColumnIndex(), args[0], args[1]);
		}
		return ErrorEval.VALUE_INVALID;
	}
}

