import org.apache.poi.ss.formula.ptg.*;


/**
 * Unary Plus operator
 * does not have any effect on the operand
 * @author Avik Sengupta
 */
public final class UnaryMinusPtg extends ValueOperatorPtg {
    public final static byte sid  = 0x13;
    
    private final static String MINUS = "-";

    public static final ValueOperatorPtg instance = new UnaryMinusPtg();

    private UnaryMinusPtg() {
    	// enforce singleton
    }
    
    protected byte getSid() {
    	return sid;
    }

    public int getNumberOfOperands() {
        return 1;
    }
    
   /** implementation of method from OperationsPtg*/  
    public String toFormulaString(String[] operands) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(MINUS);
        buffer.append(operands[ 0]);
        return buffer.toString();
    }
}
