import org.apache.poi.hssf.usermodel.examples.*;


import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * This example creates a new blank workbook.  This workbook will contain a single blank sheet.
 */
public class NewWorkbook {
    public static void main(String[] args) throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            try (FileOutputStream fileOut = new FileOutputStream("workbook.xls")) {
                wb.write(fileOut);
            }
        }
    }
}
