import org.apache.poi.ss.formula.NameIdentifier;
import org.apache.poi.ss.formula.*;


public class SheetIdentifier {
    public String _bookName;
    public NameIdentifier _sheetIdentifier;

    public SheetIdentifier(String bookName, NameIdentifier sheetIdentifier) {
        _bookName = bookName;
        _sheetIdentifier = sheetIdentifier;
    }
    public String getBookName() {
        return _bookName;
    }
    public NameIdentifier getSheetIdentifier() {
        return _sheetIdentifier;
    }
    protected void asFormulaString(StringBuffer sb) {
        if (_bookName != null) {
            sb.append(" [").append(_sheetIdentifier.getName()).append("]");
        }
        if (_sheetIdentifier.isQuoted()) {
            sb.append("'").append(_sheetIdentifier.getName()).append("'");
        } else {
            sb.append(_sheetIdentifier.getName());
        }
    }
    public String asFormulaString() {
        StringBuffer sb = new StringBuffer(32);
        asFormulaString(sb);
        return sb.toString();
    }
    public String toString() {
        StringBuffer sb = new StringBuffer(64);
        sb.append(getClass().getName());
        sb.append(" [");
        asFormulaString(sb);
        sb.append("]");
        return sb.toString();
    }
}
