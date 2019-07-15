import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.ss.formula.SheetNameFormatter;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.util.LittleEndianOutput;


/**
 * An XSSF only representation of a reference to a deleted area
 */
public final class Deleted3DPxg extends OperandPtg implements Pxg {
    private int externalWorkbookNumber = -1;
    private String sheetName;

    public Deleted3DPxg(int externalWorkbookNumber, String sheetName) {
        this.externalWorkbookNumber = externalWorkbookNumber;
        this.sheetName = sheetName;
    }
    public Deleted3DPxg(String sheetName) {
        this(-1, sheetName);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append(" [");
        if (externalWorkbookNumber >= 0) {
            sb.append(" [");
            sb.append("workbook=").append(getExternalWorkbookNumber());
            sb.append("] ");
        }
        sb.append("sheet=").append(getSheetName());
        sb.append(" ! ");
        sb.append(FormulaError.REF.getString());
        sb.append("]");
        return sb.toString();
    }

    public int getExternalWorkbookNumber() {
        return externalWorkbookNumber;
    }
    public String getSheetName() {
        return sheetName;
    }
    
    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public String toFormulaString() {
        StringBuilder sb = new StringBuilder(64);
        if (externalWorkbookNumber >= 0) {
            sb.append('[');
            sb.append(externalWorkbookNumber);
            sb.append(']');
        }
        if (sheetName != null) {
            SheetNameFormatter.appendFormat(sb, sheetName);
        }
        sb.append('!');
        sb.append(FormulaError.REF.getString());
        return sb.toString();
    }
    
    public byte getDefaultOperandClass() {
        return Ptg.CLASS_VALUE;
    }

    public int getSize() {
        return 1;
    }
    public void write(LittleEndianOutput out) {
        throw new IllegalStateException("XSSF-only Ptg, should not be serialised");
    }
}
