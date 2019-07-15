import org.apache.poi.ss.formula.ptg.*;


/**
 *
 * @author  andy
 * @author Jason Height (jheight at chariot dot net dot au)
 */
public final class PowerPtg extends ValueOperatorPtg {
    public final static byte sid  = 0x07;

    public static final ValueOperatorPtg instance = new PowerPtg();

    private PowerPtg() {
    	// enforce singleton
    }
    
    protected byte getSid() {
    	return sid;
    }

    public int getNumberOfOperands() {
        return 2; // TODO - 2 seems wrong (Jun 2008).  Maybe this method is not relevant
    }
 
    public String toFormulaString(String[] operands) {
         StringBuffer buffer = new StringBuffer();

        
        buffer.append(operands[ 0 ]);
        buffer.append("^");
        buffer.append(operands[ 1 ]);
        return buffer.toString();
    }       
}
