import org.apache.poi.xssf.usermodel.examples.*;


import java.io.Closeable;
import java.io.InputStream;

import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/**
 * Demonstrates how you can extract embedded data from a .xlsx file
 */
public class EmbeddedObjects {
    public static void main(String[] args) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(args[0])) {
            for (PackagePart pPart : workbook.getAllEmbeddedParts()) {
                String contentType = pPart.getContentType();
                try (InputStream is = pPart.getInputStream()) {
                    Closeable document;
                    if (contentType.equals("application/vnd.ms-excel")) {
                        // Excel Workbook - either binary or OpenXML
                        document = new HSSFWorkbook(is);
                    } else if (contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                        // Excel Workbook - OpenXML file format
                        document = new XSSFWorkbook(is);
                    } else if (contentType.equals("application/msword")) {
                        // Word Document - binary (OLE2CDF) file format
                        document = new HWPFDocument(is);
                    } else if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                        // Word Document - OpenXML file format
                        document = new XWPFDocument(is);
                    } else if (contentType.equals("application/vnd.ms-powerpoint")) {
                        // PowerPoint Document - binary file format
                        document = new HSLFSlideShow(is);
                    } else if (contentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) {
                        // PowerPoint Document - OpenXML file format
                        document = new XMLSlideShow(is);
                    } else {
                        // Any other type of embedded object.
                        document = is;
                    }
                    document.close();
                }
            }
        }
    }
}
