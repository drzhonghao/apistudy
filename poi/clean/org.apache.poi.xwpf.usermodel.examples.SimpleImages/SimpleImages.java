import org.apache.poi.xwpf.usermodel.examples.*;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * Demonstrates how to add pictures in a .docx document
 */
public class SimpleImages {

    public static void main(String[] args) throws IOException, InvalidFormatException {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p = doc.createParagraph();

            XWPFRun r = p.createRun();

            for (String imgFile : args) {
                int format;

                if (imgFile.endsWith(".emf")) {
                    format = XWPFDocument.PICTURE_TYPE_EMF;
                } else if (imgFile.endsWith(".wmf")) {
                    format = XWPFDocument.PICTURE_TYPE_WMF;
                } else if (imgFile.endsWith(".pict")) {
                    format = XWPFDocument.PICTURE_TYPE_PICT;
                } else if (imgFile.endsWith(".jpeg") || imgFile.endsWith(".jpg")) {
                    format = XWPFDocument.PICTURE_TYPE_JPEG;
                } else if (imgFile.endsWith(".png")) {
                    format = XWPFDocument.PICTURE_TYPE_PNG;
                } else if (imgFile.endsWith(".dib")) {
                    format = XWPFDocument.PICTURE_TYPE_DIB;
                } else if (imgFile.endsWith(".gif")) {
                    format = XWPFDocument.PICTURE_TYPE_GIF;
                } else if (imgFile.endsWith(".tiff")) {
                    format = XWPFDocument.PICTURE_TYPE_TIFF;
                } else if (imgFile.endsWith(".eps")) {
                    format = XWPFDocument.PICTURE_TYPE_EPS;
                } else if (imgFile.endsWith(".bmp")) {
                    format = XWPFDocument.PICTURE_TYPE_BMP;
                } else if (imgFile.endsWith(".wpg")) {
                    format = XWPFDocument.PICTURE_TYPE_WPG;
                } else {
                    System.err.println("Unsupported picture: " + imgFile +
                            ". Expected emf|wmf|pict|jpeg|png|dib|gif|tiff|eps|bmp|wpg");
                    continue;
                }

                r.setText(imgFile);
                r.addBreak();
                try (FileInputStream is = new FileInputStream(imgFile)) {
                    r.addPicture(is, format, imgFile, Units.toEMU(200), Units.toEMU(200)); // 200x200 pixels
                }
                r.addBreak(BreakType.PAGE);
            }

            try (FileOutputStream out = new FileOutputStream("images.docx")) {
                doc.write(out);
            }
        }
    }


}
