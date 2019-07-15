import org.apache.poi.ss.formula.ptg.*;


/**
 * Implements the standard mathmatical multiplication - *
 * @author  Andrew C. Oliver (acoliver at apache dot org)
 * @author Jason Height (jheight at chariot dot net dot au)
 */
public final class MultiplyPtg extends ValueOperatorPtg {
    public final static byte sid  = 0x05;

    public static final ValueOperatorPtg instance = new MultiplyPtg();

    private MultiplyPtg() {
    	// enforce singleton
    }
    
    protected byte getSid() {
    	return sid;
    }

    public int getNumberOfOperands() {
        return 2;
    }
    
    public String toFormulaString(String[] operands) {
        StringBuffer buffer = new StringBuffer();

        buffer.append(operands[ 0 ]);
        buffer.append("*");
        buffer.append(operands[ 1 ]);
        return buffer.toString();
    }                  
}
