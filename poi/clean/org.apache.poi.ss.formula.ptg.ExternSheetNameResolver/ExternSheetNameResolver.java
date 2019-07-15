import org.apache.poi.ss.formula.ptg.*;


import org.apache.poi.ss.formula.EvaluationWorkbook.ExternalSheet;
import org.apache.poi.ss.formula.EvaluationWorkbook.ExternalSheetRange;
import org.apache.poi.ss.formula.FormulaRenderingWorkbook;
import org.apache.poi.ss.formula.SheetNameFormatter;

/**
 * @author Josh Micich
 */
final class ExternSheetNameResolver {
    private ExternSheetNameResolver() {
        // no instances of this class
    }

    public static String prependSheetName(FormulaRenderingWorkbook book, int field_1_index_extern_sheet, String cellRefText) {
        ExternalSheet externalSheet = book.getExternalSheet(field_1_index_extern_sheet);
        StringBuilder sb;
        if (externalSheet != null) {
            String wbName = externalSheet.getWorkbookName();
            String sheetName = externalSheet.getSheetName();
            if (wbName != null) {
                sb = new StringBuilder(wbName.length() + sheetName.length() + cellRefText.length() + 4);
                SheetNameFormatter.appendFormat(sb, wbName, sheetName);
            } else {
                sb = new StringBuilder(sheetName.length() + cellRefText.length() + 4);
                SheetNameFormatter.appendFormat(sb, sheetName);
            }
            if (externalSheet instanceof ExternalSheetRange) {
                ExternalSheetRange r = (ExternalSheetRange)externalSheet;
                if (! r.getFirstSheetName().equals(r.getLastSheetName())) {
                    sb.append(':');
                    SheetNameFormatter.appendFormat(sb, r.getLastSheetName());
                }
            }
        } else {
            String firstSheetName = book.getSheetFirstNameByExternSheet(field_1_index_extern_sheet);
            String lastSheetName = book.getSheetLastNameByExternSheet(field_1_index_extern_sheet);
            sb = new StringBuilder(firstSheetName.length() + cellRefText.length() + 4);
            if (firstSheetName.length() < 1) {
                // What excel does if sheet has been deleted
                sb.append("#REF"); // note - '!' added just once below
            } else {
                SheetNameFormatter.appendFormat(sb, firstSheetName);
                if (! firstSheetName.equals(lastSheetName)) {
                    sb.append(':');
                    sb.append(lastSheetName);
                }
            }
        }
        sb.append('!');
        sb.append(cellRefText);
        return sb.toString();
    }
}
