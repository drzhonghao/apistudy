import org.apache.poi.ss.formula.functions.Finance;
import org.apache.poi.ss.formula.functions.*;


import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.EvaluationException;
import org.apache.poi.ss.formula.eval.OperandResolver;
import org.apache.poi.ss.formula.eval.ValueEval;

public class IPMT extends NumericFunction {

	@Override
	public double eval(ValueEval[] args, int srcCellRow, int srcCellCol) throws EvaluationException {
   		
		if(args.length != 4)
        		throw new EvaluationException(ErrorEval.VALUE_INVALID);

		double result;

		ValueEval v1 = OperandResolver.getSingleValue(args[0], srcCellRow, srcCellCol); 
		ValueEval v2 = OperandResolver.getSingleValue(args[1], srcCellRow, srcCellCol); 
		ValueEval v3 = OperandResolver.getSingleValue(args[2], srcCellRow, srcCellCol); 
		ValueEval v4 = OperandResolver.getSingleValue(args[3], srcCellRow, srcCellCol); 

		double interestRate = OperandResolver.coerceValueToDouble(v1);
		int period = OperandResolver.coerceValueToInt(v2);
		int numberPayments = OperandResolver.coerceValueToInt(v3);
		double PV = OperandResolver.coerceValueToDouble(v4);

		result = Finance.ipmt(interestRate, period, numberPayments, PV) ;

		checkValue(result);
		
		return result;
	}

	

}
