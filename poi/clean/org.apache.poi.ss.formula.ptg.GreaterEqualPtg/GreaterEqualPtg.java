import org.apache.poi.ss.formula.ptg.*;



/**
 * PTG class to implement greater or equal to
 *
 * @author  fred at stsci dot edu
 */
public final class GreaterEqualPtg extends ValueOperatorPtg {
    public final static int  SIZE = 1;
    public final static byte sid  = 0x0c;

    public static final ValueOperatorPtg instance = new GreaterEqualPtg();

    private GreaterEqualPtg() {
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

        buffer.append(">=");
        buffer.append(operands[ 1 ]);

        return buffer.toString();
    }
}
