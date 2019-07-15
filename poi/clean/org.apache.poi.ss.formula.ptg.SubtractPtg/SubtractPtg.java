import org.apache.poi.ss.formula.ptg.*;


/**
 *
 * @author  andy
 * @author Jason Height (jheight at chariot dot net dot au)
 */
public final class SubtractPtg extends ValueOperatorPtg {
    public final static byte sid  = 0x04;

    public static final ValueOperatorPtg instance = new SubtractPtg();

    private SubtractPtg() {
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
        buffer.append("-");
        buffer.append(operands[ 1 ]);
        return buffer.toString();
    }
}
