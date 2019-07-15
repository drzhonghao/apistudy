import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.*;


import org.apache.poi.ss.usermodel.Sheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDialogsheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTHeaderFooter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPageBreak;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPageMargins;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPrintOptions;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetFormatPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetViews;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;

//YK: TODO: this is only a prototype
public class XSSFDialogsheet extends XSSFSheet implements Sheet{
    protected CTDialogsheet dialogsheet;

    protected XSSFDialogsheet(XSSFSheet sheet) {
        super(sheet.getPackagePart());
        this.dialogsheet = CTDialogsheet.Factory.newInstance();
        this.worksheet = CTWorksheet.Factory.newInstance();
    }

    public XSSFRow createRow(int rowNum) {
        return null;
    }

    protected CTHeaderFooter getSheetTypeHeaderFooter() {
        if (dialogsheet.getHeaderFooter() == null) {
            dialogsheet.setHeaderFooter(CTHeaderFooter.Factory.newInstance());
        }
        return dialogsheet.getHeaderFooter();
    }

    protected CTSheetPr getSheetTypeSheetPr() {
        if (dialogsheet.getSheetPr() == null) {
            dialogsheet.setSheetPr(CTSheetPr.Factory.newInstance());
        }
        return dialogsheet.getSheetPr();
    }

    protected CTPageBreak getSheetTypeColumnBreaks() {
        return null;
    }

    protected CTSheetFormatPr getSheetTypeSheetFormatPr() {
        if (dialogsheet.getSheetFormatPr() == null) {
            dialogsheet.setSheetFormatPr(CTSheetFormatPr.Factory.newInstance());
        }
        return dialogsheet.getSheetFormatPr();
    }

    protected CTPageMargins getSheetTypePageMargins() {
        if (dialogsheet.getPageMargins() == null) {
            dialogsheet.setPageMargins(CTPageMargins.Factory.newInstance());
        }
        return dialogsheet.getPageMargins();
    }

    protected CTPageBreak getSheetTypeRowBreaks() {
        return null;
    }

    protected CTSheetViews getSheetTypeSheetViews() {
        if (dialogsheet.getSheetViews() == null) {
            dialogsheet.setSheetViews(CTSheetViews.Factory.newInstance());
            dialogsheet.getSheetViews().addNewSheetView();
        }
        return dialogsheet.getSheetViews();
    }

    protected CTPrintOptions getSheetTypePrintOptions() {
        if (dialogsheet.getPrintOptions() == null) {
            dialogsheet.setPrintOptions(CTPrintOptions.Factory.newInstance());
        }
        return dialogsheet.getPrintOptions();
    }

    protected CTSheetProtection getSheetTypeProtection() {
        if (dialogsheet.getSheetProtection() == null) {
            dialogsheet.setSheetProtection(CTSheetProtection.Factory.newInstance());
        }
        return dialogsheet.getSheetProtection();
    }

    public boolean getDialog(){
        return true;
    }
}
