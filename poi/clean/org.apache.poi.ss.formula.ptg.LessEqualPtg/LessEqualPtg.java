import org.apache.poi.ss.formula.ptg.*;





/**
 * Ptg class to implement less than or equal
 *
 * @author fred at stsci dot edu
 */
public final class LessEqualPtg extends ValueOperatorPtg {
    public final static byte sid = 0x0a;

    public static final ValueOperatorPtg instance = new LessEqualPtg();

    private LessEqualPtg() {
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
        buffer.append("<=");
        buffer.append( operands[1] );
        return buffer.toString();
    }
}
