import org.apache.poi.ss.formula.SheetIdentifier;
import org.apache.poi.ss.formula.NameIdentifier;
import org.apache.poi.ss.formula.*;


public class SheetRangeIdentifier extends SheetIdentifier {
    public NameIdentifier _lastSheetIdentifier;

    public SheetRangeIdentifier(String bookName, NameIdentifier firstSheetIdentifier, NameIdentifier lastSheetIdentifier) {
        super(bookName, firstSheetIdentifier);
        _lastSheetIdentifier = lastSheetIdentifier;
    }
    public NameIdentifier getFirstSheetIdentifier() {
        return super.getSheetIdentifier();
    }
    public NameIdentifier getLastSheetIdentifier() {
        return _lastSheetIdentifier;
    }
    protected void asFormulaString(StringBuffer sb) {
        super.asFormulaString(sb);
        sb.append(':');
        if (_lastSheetIdentifier.isQuoted()) {
            sb.append("'").append(_lastSheetIdentifier.getName()).append("'");
        } else {
            sb.append(_lastSheetIdentifier.getName());
        }
    }
}
