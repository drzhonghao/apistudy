import org.apache.poi.ss.formula.ptg.*;


/**
 * This PTG implements the standard binomial divide "/"
 * @author  Andrew C. Oliver acoliver at apache dot org
 * @author Jason Height (jheight at chariot dot net dot au)
 */
public final class DividePtg extends ValueOperatorPtg {
    public final static byte sid  = 0x06;

    public static final ValueOperatorPtg instance = new DividePtg();

    private DividePtg() {
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
        buffer.append("/");
        buffer.append(operands[ 1 ]);
        return buffer.toString();
    }      
}
