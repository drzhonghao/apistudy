import org.apache.poi.ss.formula.ptg.*;


/**
 * Ptg class to implement not equal
 *
 * @author fred at stsci dot edu
 */
public final class NotEqualPtg extends ValueOperatorPtg {
    public final static byte sid = 0x0e;

    public static final ValueOperatorPtg instance = new NotEqualPtg();

    private NotEqualPtg() {
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

        buffer.append( operands[0] );

        buffer.append("<>");
        buffer.append( operands[1] );

        return buffer.toString();
    }
}
