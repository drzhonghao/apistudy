import org.apache.poi.xssf.usermodel.*;


import org.apache.poi.ss.usermodel.TableStyle;
import org.apache.poi.ss.usermodel.TableStyleInfo;
import org.apache.poi.xssf.model.StylesTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyleInfo;

/**
 * Wrapper for the CT class, to cache values and add style lookup
 */
public class XSSFTableStyleInfo implements TableStyleInfo {
    private final CTTableStyleInfo styleInfo;
    private final StylesTable stylesTable;
    private TableStyle style;
    private boolean columnStripes;
    private boolean rowStripes;
    private boolean firstColumn;
    private boolean lastColumn;
    
    /**
     * @param stylesTable 
     * @param tableStyleInfo 
     */
    public XSSFTableStyleInfo(StylesTable stylesTable, CTTableStyleInfo tableStyleInfo) {
        this.columnStripes = tableStyleInfo.getShowColumnStripes();
        this.rowStripes = tableStyleInfo.getShowRowStripes();
        this.firstColumn = tableStyleInfo.getShowFirstColumn();
        this.lastColumn = tableStyleInfo.getShowLastColumn();
        this.style = stylesTable.getTableStyle(tableStyleInfo.getName());
        this.stylesTable = stylesTable;
        this.styleInfo = tableStyleInfo;
    }

    public boolean isShowColumnStripes() {
        return columnStripes;
    }
    public void setShowColumnStripes(boolean show) {
        this.columnStripes = show;
        styleInfo.setShowColumnStripes(show);
    }

    public boolean isShowRowStripes() {
        return rowStripes;
    }
    public void setShowRowStripes(boolean show) {
        this.rowStripes = show;
        styleInfo.setShowRowStripes(show);
    }

    public boolean isShowFirstColumn() {
        return firstColumn;
    }
    public void setFirstColumn(boolean showFirstColumn) {
        this.firstColumn = showFirstColumn;
        styleInfo.setShowFirstColumn(showFirstColumn);
    }

    public boolean isShowLastColumn() {
        return lastColumn;
    }
    public void setLastColumn(boolean showLastColumn) {
        this.lastColumn = showLastColumn;
        styleInfo.setShowLastColumn(showLastColumn);
    }

    public String getName() {
        return style.getName();
    }
    public void setName(String name) {
        styleInfo.setName(name);
        style = stylesTable.getTableStyle(name);
    }

    public TableStyle getStyle() {
        return style;
    }
}
