import org.apache.poi.hssf.usermodel.examples.*;


import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * An example on how to cells with dates.  The important thing to note
 * about dates is that they are really normal numeric cells that are
 * formatted specially.
 */
public class CreateDateCells {
    public static void main(String[] args) throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            HSSFSheet sheet = wb.createSheet("new sheet");

            // Create a row and put some cells in it. Rows are 0 based.
            HSSFRow row = sheet.createRow(0);

            // Create a cell and put a date value in it.  The first cell is not styled as a date.
            HSSFCell cell = row.createCell(0);
            cell.setCellValue(new Date());

            // we style the second cell as a date (and time).  It is important to create a new cell style from the workbook
            // otherwise you can end up modifying the built in style and effecting not only this cell but other cells.
            HSSFCellStyle cellStyle = wb.createCellStyle();
            cellStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("m/d/yy h:mm"));
            cell = row.createCell(1);
            cell.setCellValue(new Date());
            cell.setCellStyle(cellStyle);

            // Write the output to a file
            try (FileOutputStream fileOut = new FileOutputStream("workbook.xls")) {
                wb.write(fileOut);
            }
        }
    }
}
