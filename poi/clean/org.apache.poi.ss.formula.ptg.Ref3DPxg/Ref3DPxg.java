import org.apache.poi.ss.formula.ptg.RefPtgBase;
import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.ss.formula.SheetIdentifier;
import org.apache.poi.ss.formula.SheetRangeAndWorkbookIndexFormatter;
import org.apache.poi.ss.formula.SheetRangeIdentifier;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.LittleEndianOutput;

/**
 * <p>Title:        XSSF 3D Reference</p>
 * <p>Description:  Defines a cell in an external or different sheet.</p>
 * <p>REFERENCE: </p>
 * 
 * <p>This is XSSF only, as it stores the sheet / book references
 *  in String form. The HSSF equivalent using indexes is {@link Ref3DPtg}</p>
 */
public final class Ref3DPxg extends RefPtgBase implements Pxg3D {
    private int externalWorkbookNumber = -1;
    private String firstSheetName;
    private String lastSheetName;

    public Ref3DPxg(int externalWorkbookNumber, SheetIdentifier sheetName, String cellref) {
        this(externalWorkbookNumber, sheetName, new CellReference(cellref));
    }
    public Ref3DPxg(int externalWorkbookNumber, SheetIdentifier sheetName, CellReference c) {
        super(c);
        this.externalWorkbookNumber = externalWorkbookNumber;
        
        this.firstSheetName = sheetName.getSheetIdentifier().getName();
        if (sheetName instanceof SheetRangeIdentifier) {
            this.lastSheetName = ((SheetRangeIdentifier)sheetName).getLastSheetIdentifier().getName();
        } else {
            this.lastSheetName = null;
        }
    }
    
    public Ref3DPxg(SheetIdentifier sheetName, String cellref) {
        this(sheetName, new CellReference(cellref));
    }
    public Ref3DPxg(SheetIdentifier sheetName, CellReference c) {
        this(-1, sheetName, c);
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
        sb.append("sheet=").append(firstSheetName);
        if (lastSheetName != null) {
            sb.append(" : ");
            sb.append("sheet=").append(lastSheetName);
        }
        sb.append(" ! ");
        sb.append(formatReferenceAsString());
        sb.append("]");
        return sb.toString();
    }
    
    public int getExternalWorkbookNumber() {
        return externalWorkbookNumber;
    }
    public String getSheetName() {
        return firstSheetName;
    }
    public String getLastSheetName() {
        return lastSheetName;
    }
    
    public void setSheetName(String sheetName) {
        this.firstSheetName = sheetName;
    }
    public void setLastSheetName(String sheetName) {
        this.lastSheetName = sheetName;
    }
    
    public String format2DRefAsString() {
        return formatReferenceAsString();
    }

    public String toFormulaString() {
        StringBuilder sb = new StringBuilder(64);

        SheetRangeAndWorkbookIndexFormatter.format(sb, externalWorkbookNumber, firstSheetName, lastSheetName);
        sb.append('!');
        sb.append(formatReferenceAsString());
        return sb.toString();
    }
    
    public int getSize() {
        return 1;
    }
    public void write(LittleEndianOutput out) {
        throw new IllegalStateException("XSSF-only Ptg, should not be serialised");
    }
}
