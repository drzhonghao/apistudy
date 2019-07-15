import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.util.LittleEndianOutput;

/**
 * @author andy
 * @author Jason Height (jheight at chariot dot net dot au)
 */
public class UnknownPtg extends Ptg {
    private short size = 1;
    private final int _sid;

    public UnknownPtg(int sid) {
        _sid = sid;
    }

    public boolean isBaseToken() {
        return true;
    }
    public void write(LittleEndianOutput out) {
        out.writeByte(_sid);
    }

    public int getSize() {
        return size;
    }

    public String toFormulaString() {
        return "UNKNOWN";
    }
    public byte getDefaultOperandClass() {
        return Ptg.CLASS_VALUE;
    }
}
