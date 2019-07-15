import org.apache.poi.hssf.usermodel.examples.*;


import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;

import java.io.IOException;
import java.io.FileOutputStream;

/**
 * Sets the zoom magnication for a sheet.
 *
 * @author Glen Stampoultzis (glens at apache.org)
 */
public class ZoomSheet
{
    public static void main(String[] args) throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook()) {
            HSSFSheet sheet1 = wb.createSheet("new sheet");
            sheet1.setZoom(75);   // 75 percent magnification

            try (FileOutputStream fileOut = new FileOutputStream("workbook.xls")) {
                wb.write(fileOut);
            }
        }
    }
}
