import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.util.LittleEndianInput;
import org.apache.poi.util.LittleEndianOutput;

/**
 *
 * @author  andy
 * @author Jason Height (jheight at chariot dot net dot au)
 * @author dmui (save existing implementation)
 */
public final class ExpPtg extends ControlPtg {
    private final static int  SIZE = 5;
    public final static short sid  = 0x1;
    private final int field_1_first_row;
    private final int field_2_first_col;

    public ExpPtg(LittleEndianInput in) {
      field_1_first_row = in.readShort();
      field_2_first_col = in.readShort();
    }

    public ExpPtg(int firstRow, int firstCol) {
      this.field_1_first_row = firstRow;
      this.field_2_first_col = firstCol;
    }

    @Override
    public void write(LittleEndianOutput out) {
        out.writeByte(sid + getPtgClass());
        out.writeShort(field_1_first_row);
        out.writeShort(field_2_first_col);
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    public int getRow() {
      return field_1_first_row;
    }

    public int getColumn() {
      return field_2_first_col;
    }

    @Override
    public String toFormulaString() {
        throw new RuntimeException("Coding Error: Expected ExpPtg to be converted from Shared to Non-Shared Formula by ValueRecordsAggregate, but it wasn't");
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("[Array Formula or Shared Formula]\n");
        buffer.append("row = ").append(getRow()).append("\n");
        buffer.append("col = ").append(getColumn()).append("\n");
        return buffer.toString();
    }
}
