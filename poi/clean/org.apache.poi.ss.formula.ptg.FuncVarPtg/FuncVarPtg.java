import org.apache.poi.ss.formula.ptg.*;

import org.apache.poi.ss.formula.function.FunctionMetadata;
import org.apache.poi.ss.formula.function.FunctionMetadataRegistry;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

/**
 * @author Jason Height (jheight at chariot dot net dot au)
 */
public final class FuncVarPtg extends AbstractFunctionPtg{

    public final static byte sid  = 0x22;
    private final static int  SIZE = 4;

    /**
     * Single instance of this token for 'sum() taking a single argument'
     */
    public static final OperationPtg SUM = FuncVarPtg.create("SUM", 1);

    private FuncVarPtg(int functionIndex, int returnClass, byte[] paramClasses, int numArgs) {
        super(functionIndex, returnClass, paramClasses, numArgs);
    }

    /**Creates new function pointer from a byte array
     * usually called while reading an excel file.
     */
    public static FuncVarPtg create(LittleEndianInput in)  {
        return create(in.readByte(), in.readShort());
    }

    /**
     * Create a function ptg from a string tokenised by the parser
     */
    public static FuncVarPtg create(String pName, int numArgs) {
        return create(numArgs, lookupIndex(pName));
    }

    private static FuncVarPtg create(int numArgs, int functionIndex) {
        FunctionMetadata fm = FunctionMetadataRegistry.getFunctionByIndex(functionIndex);
        if(fm == null) {
            // Happens only as a result of a call to FormulaParser.parse(), with a non-built-in function name
            return new FuncVarPtg(functionIndex, Ptg.CLASS_VALUE, new byte[] {Ptg.CLASS_VALUE}, numArgs);
        }
        return new FuncVarPtg(functionIndex, fm.getReturnClassCode(), fm.getParameterClassCodes(), numArgs);
    }

    public void write(LittleEndianOutput out) {
        out.writeByte(sid + getPtgClass());
        out.writeByte(getNumberOfOperands());
        out.writeShort(getFunctionIndex());
    }

    public int getSize() {
        return SIZE;
    }
}
