import org.apache.poi.ss.formula.ptg.*;


/**
 * Addition operator PTG the "+" binomial operator.  If you need more 
 * explanation than that then well...We really can't help you here.
 * @author  Andrew C. Oliver (acoliver@apache.org)
 * @author Jason Height (jheight at chariot dot net dot au)
 */
public final class AddPtg extends ValueOperatorPtg {
    public final static byte sid  = 0x03;
    
    private final static String ADD = "+";

    public static final ValueOperatorPtg instance = new AddPtg();

    private AddPtg() {
    	// enforce singleton
    }
    
    protected byte getSid() {
    	return sid;
    }

    public int getNumberOfOperands() {
        return 2;
    }
       
   /** implementation of method from OperationsPtg*/  
    public String toFormulaString(String[] operands) {
        StringBuffer buffer = new StringBuffer();

        buffer.append(operands[ 0 ]);
        buffer.append(ADD);
        buffer.append(operands[ 1 ]);
        return buffer.toString();
    }
}
