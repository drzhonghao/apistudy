import org.apache.poi.hssf.usermodel.examples.*;


import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

public class RepeatingRowsAndColumns {
    public static void main(String[] args) throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            HSSFSheet sheet1 = wb.createSheet("first sheet");
            HSSFSheet sheet2 = wb.createSheet("second sheet");
            HSSFSheet sheet3 = wb.createSheet("third sheet");

            HSSFFont boldFont = wb.createFont();
            boldFont.setFontHeightInPoints((short) 22);
            boldFont.setBold(true);

            HSSFCellStyle boldStyle = wb.createCellStyle();
            boldStyle.setFont(boldFont);

            HSSFRow row = sheet1.createRow(1);
            HSSFCell cell = row.createCell(0);
            cell.setCellValue("This quick brown fox");
            cell.setCellStyle(boldStyle);

            // Set the columns to repeat from column 0 to 2 on the first sheet
            sheet1.setRepeatingColumns(CellRangeAddress.valueOf("A:C"));
            // Set the rows to repeat from row 0 to 2 on the second sheet.
            sheet2.setRepeatingRows(CellRangeAddress.valueOf("1:3"));
            // Set the the repeating rows and columns on the third sheet.
            CellRangeAddress cra = CellRangeAddress.valueOf("D1:E2");
            sheet3.setRepeatingColumns(cra);
            sheet3.setRepeatingRows(cra);

            try (FileOutputStream fileOut = new FileOutputStream("workbook.xls")) {
                wb.write(fileOut);
            }
        }
    }
}
