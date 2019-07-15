import org.apache.poi.ss.formula.constant.*;


import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
/**
 * Represents a constant error code value as encoded in a constant values array. <p>
 * 
 * This class is a type-safe wrapper for a 16-bit int value performing a similar job to 
 * <tt>ErrorEval</tt>.
 */
public class ErrorConstant {
	private static final POILogger logger = POILogFactory.getLogger(ErrorConstant.class);
    private static final ErrorConstant NULL = new ErrorConstant(FormulaError.NULL.getCode());
    private static final ErrorConstant DIV_0 = new ErrorConstant(FormulaError.DIV0.getCode());
    private static final ErrorConstant VALUE = new ErrorConstant(FormulaError.VALUE.getCode());
    private static final ErrorConstant REF = new ErrorConstant(FormulaError.REF.getCode());
    private static final ErrorConstant NAME = new ErrorConstant(FormulaError.NAME.getCode());
    private static final ErrorConstant NUM = new ErrorConstant(FormulaError.NUM.getCode());
    private static final ErrorConstant NA = new ErrorConstant(FormulaError.NA.getCode());

	private final int _errorCode;

	private ErrorConstant(int errorCode) {
		_errorCode = errorCode;
	}
	
	public int getErrorCode() {
		return _errorCode;
	}

	public String getText() {
		if(FormulaError.isValidCode(_errorCode)) {
			return FormulaError.forInt(_errorCode).getString();
		}
		return "unknown error code (" + _errorCode + ")";
	}

	public static ErrorConstant valueOf(int errorCode) {
	    if (FormulaError.isValidCode(errorCode)) {
    		switch (FormulaError.forInt(errorCode)) {
    			case NULL:  return NULL;
    			case DIV0:  return DIV_0;
    			case VALUE: return VALUE;
    			case REF:   return REF;
    			case NAME:  return NAME;
    			case NUM:   return NUM;
    			case NA:	return NA;
    			default:    break;
    		}
	    }
		logger.log( POILogger.WARN, "Warning - unexpected error code (" + errorCode + ")");
		return new ErrorConstant(errorCode);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(64);
		sb.append(getClass().getName()).append(" [");
		sb.append(getText());
		sb.append("]");
		return sb.toString();
	}
}
