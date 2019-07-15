import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.examples.*;


import org.apache.poi.xssf.usermodel.*;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Demonstrates how to work with rich text
 */
public class WorkingWithRichText {

    public static void main(String[] args) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet();
            XSSFRow row = sheet.createRow(2);

            XSSFCell cell = row.createCell(1);
            XSSFRichTextString rt = new XSSFRichTextString("The quick brown fox");

            XSSFFont font1 = wb.createFont();
            font1.setBold(true);
            font1.setColor(new XSSFColor(new java.awt.Color(255, 0, 0), wb.getStylesSource().getIndexedColors()));
            rt.applyFont(0, 10, font1);

            XSSFFont font2 = wb.createFont();
            font2.setItalic(true);
            font2.setUnderline(XSSFFont.U_DOUBLE);
            font2.setColor(new XSSFColor(new java.awt.Color(0, 255, 0), wb.getStylesSource().getIndexedColors()));
            rt.applyFont(10, 19, font2);

            XSSFFont font3 = wb.createFont();
            font3.setColor(new XSSFColor(new java.awt.Color(0, 0, 255), wb.getStylesSource().getIndexedColors()));
            rt.append(" Jumped over the lazy dog", font3);

            cell.setCellValue(rt);

            // Write the output to a file
            try (OutputStream fileOut = new FileOutputStream("xssf-richtext.xlsx")) {
                wb.write(fileOut);
            }
        }
    }
}
