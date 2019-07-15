import org.apache.poi.ss.formula.ptg.*;


/**
 *
 * @author  andy
 * @author Jason Height (jheight at chariot dot net dot au)
 */
public final class ConcatPtg extends ValueOperatorPtg {
    public final static byte sid  = 0x08;
    
    private final static String CONCAT = "&";
    
    public static final ValueOperatorPtg instance = new ConcatPtg();

    private ConcatPtg() {
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
        buffer.append(CONCAT);
        buffer.append(operands[ 1 ]);
        return buffer.toString();
    }
}
