import org.apache.poi.ss.formula.ptg.*;


/**
 * Percent PTG.
 *
 * @author Daniel Noll (daniel at nuix.com.au)
 */
public final class PercentPtg extends ValueOperatorPtg {
    public final static int  SIZE = 1;
    public final static byte sid  = 0x14;
    
    private final static String PERCENT = "%";

    public static final ValueOperatorPtg instance = new PercentPtg();

    private PercentPtg() {
    	// enforce singleton
    }
    
    protected byte getSid() {
    	return sid;
    }

    public int getNumberOfOperands() {
        return 1;
    }
       
    public String toFormulaString(String[] operands) {
        StringBuffer buffer = new StringBuffer();

        buffer.append(operands[ 0 ]);
        buffer.append(PERCENT);
        return buffer.toString();
    }
}
