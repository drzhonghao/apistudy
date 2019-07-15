import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.ss.formula.function.FunctionMetadata;
import org.apache.poi.ss.formula.function.FunctionMetadataRegistry;
import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

/**
 * @author aviks
 * @author Jason Height (jheight at chariot dot net dot au)
 * @author Danny Mui (dmui at apache dot org) (Leftover handling)
 */
public final class FuncPtg extends AbstractFunctionPtg {

    public final static byte sid  = 0x21;
    public final static int  SIZE = 3;

    public static FuncPtg create(LittleEndianInput in) {
        return create(in.readUShort());
    }

    private FuncPtg(int funcIndex, FunctionMetadata fm) {
        super(funcIndex, fm.getReturnClassCode(), fm.getParameterClassCodes(), fm.getMinParams());  // minParams same as max since these are not var-arg funcs
    }

    public static FuncPtg create(int functionIndex) {
        FunctionMetadata fm = FunctionMetadataRegistry.getFunctionByIndex(functionIndex);
        if(fm == null) {
            throw new RuntimeException("Invalid built-in function index (" + functionIndex + ")");
        }
        return new FuncPtg(functionIndex, fm);
    }


    public void write(LittleEndianOutput out) {
        out.writeByte(sid + getPtgClass());
        out.writeShort(getFunctionIndex());
    }

    public int getSize() {
        return SIZE;
    }
}
