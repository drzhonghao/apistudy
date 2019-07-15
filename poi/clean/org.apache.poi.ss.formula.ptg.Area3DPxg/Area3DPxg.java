import org.apache.poi.ss.formula.ptg.AreaPtgBase;
import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.SheetIdentifier;
import org.apache.poi.ss.formula.SheetNameFormatter;
import org.apache.poi.ss.formula.SheetRangeAndWorkbookIndexFormatter;
import org.apache.poi.ss.formula.SheetRangeIdentifier;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.util.LittleEndianOutput;

/**
 * <p>Title:        XSSF Area 3D Reference (Sheet + Area)<P>
 * <p>Description:  Defined an area in an external or different sheet. <P>
 * <p>REFERENCE:  </p>
 * 
 * <p>This is XSSF only, as it stores the sheet / book references
 *  in String form. The HSSF equivalent using indexes is {@link Area3DPtg}</p>
 */
public final class Area3DPxg extends AreaPtgBase implements Pxg3D {
    private int externalWorkbookNumber = -1;
    private String firstSheetName;
    private String lastSheetName;

    public Area3DPxg(int externalWorkbookNumber, SheetIdentifier sheetName, String arearef) {
        this(externalWorkbookNumber, sheetName, new AreaReference(arearef, SpreadsheetVersion.EXCEL2007));
    }
    public Area3DPxg(int externalWorkbookNumber, SheetIdentifier sheetName, AreaReference arearef) {
        super(arearef);
        this.externalWorkbookNumber = externalWorkbookNumber;
        this.firstSheetName = sheetName.getSheetIdentifier().getName();
        if (sheetName instanceof SheetRangeIdentifier) {
            this.lastSheetName = ((SheetRangeIdentifier)sheetName).getLastSheetIdentifier().getName();
        } else {
            this.lastSheetName = null;
        }
    }

    public Area3DPxg(SheetIdentifier sheetName, String arearef) {
        this(sheetName, new AreaReference(arearef, SpreadsheetVersion.EXCEL2007));
    }
    public Area3DPxg(SheetIdentifier sheetName, AreaReference arearef) {
        this(-1, sheetName, arearef);
    }

    @Override
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
