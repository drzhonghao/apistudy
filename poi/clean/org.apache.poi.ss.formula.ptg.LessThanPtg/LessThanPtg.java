import org.apache.poi.ss.formula.ptg.*;


/**
 * Less than operator PTG "<". The SID is taken from the 
 * Openoffice.orgs Documentation of the Excel File Format,
 * Table 3.5.7
 * @author Cameron Riley (criley at ekmail.com)
 */
public final class LessThanPtg extends ValueOperatorPtg {
    /** the sid for the less than operator as hex */
    public final static byte sid  = 0x09;    

    /** identifier for LESS THAN char */
    private final static String LESSTHAN = "<";

    public static final ValueOperatorPtg instance = new LessThanPtg();

    private LessThanPtg() {
    	// enforce singleton
    }
    
    protected byte getSid() {
    	return sid;
    }

    /**
     * Get the number of operands for the Less than operator
     * @return int the number of operands
     */
    public int getNumberOfOperands() {
        return 2;
    }
    
     /** 
     * Implementation of method from OperationsPtg
     * @param operands a String array of operands
     * @return String the Formula as a String
     */  
    public String toFormulaString(String[] operands) 
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(operands[ 0 ]);
        buffer.append(LESSTHAN);
        buffer.append(operands[ 1 ]);
        return buffer.toString();
    }
}
