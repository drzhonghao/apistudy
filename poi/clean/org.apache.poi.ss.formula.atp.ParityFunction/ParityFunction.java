import org.apache.poi.ss.formula.atp.*;


import org.apache.poi.ss.formula.eval.BoolEval;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.OperationEvaluationContext;
/**
 * Implementation of Excel 'Analysis ToolPak' function ISEVEN() ISODD()<br>
 *
 * @author Josh Micich
 */
final class ParityFunction implements FreeRefFunction {

	public static final FreeRefFunction IS_EVEN = new ParityFunction(0);
	public static final FreeRefFunction IS_ODD = new ParityFunction(1);
	private final int _desiredParity;

	private ParityFunction(int desiredParity) {
		_desiredParity = desiredParity;
	}

	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		if (args.length != 1) {
			return ErrorEval.VALUE_INVALID;
		}

		int val;
		try {
			val = evaluateArgParity(args[0], ec.getRowIndex(), ec.getColumnIndex());
		} catch (EvaluationException e) {
			return e.getErrorEval();
		}

		return BoolEval.valueOf(val == _desiredParity);
	}

	private static int evaluateArgParity(ValueEval arg, int srcCellRow, int srcCellCol) throws EvaluationException {
		ValueEval ve = OperandResolver.getSingleValue(arg, srcCellRow, (short)srcCellCol);

		double d = OperandResolver.coerceValueToDouble(ve);
		if (d < 0) {
			d = -d;
		}
		long v = (long) Math.floor(d);
		return (int) (v & 0x0001);
	}
}
