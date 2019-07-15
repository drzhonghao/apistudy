import org.apache.poi.ss.formula.ptg.OperationPtg;
import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.util.LittleEndianOutput;


/**
 * @author Daniel Noll (daniel at nuix dot com dot au)
 */
public final class RangePtg  extends OperationPtg {
    public final static int  SIZE = 1;
    public final static byte sid  = 0x11;

    public static final OperationPtg instance = new RangePtg();

    private RangePtg() {
    	// enforce singleton
    }

    public final boolean isBaseToken() {
        return true;
    }

    public int getSize()
    {
        return SIZE;
    }

    public void write(LittleEndianOutput out) {
        out.writeByte(sid + getPtgClass());
    }

    public String toFormulaString()
    {
        return ":";
    }


    /** implementation of method from OperationsPtg*/
    public String toFormulaString(String[] operands)
    {
         StringBuffer buffer = new StringBuffer();

         buffer.append(operands[ 0 ]);
         buffer.append(":");
         buffer.append(operands[ 1 ]);
         return buffer.toString();
     }

    public int getNumberOfOperands()
    {
        return 2;
    }

}
