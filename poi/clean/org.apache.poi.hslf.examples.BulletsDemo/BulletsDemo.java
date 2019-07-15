import org.apache.poi.hslf.examples.*;


import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;

/**
 * How to create a single-level bulleted list
 * and change some of the bullet attributes
 */
public final class BulletsDemo {

    public static void main(String[] args) throws IOException {
        try (HSLFSlideShow ppt = new HSLFSlideShow()) {
            HSLFSlide slide = ppt.createSlide();

            HSLFTextBox shape = new HSLFTextBox();
            HSLFTextParagraph rt = shape.getTextParagraphs().get(0);
            rt.getTextRuns().get(0).setFontSize(42d);
            rt.setBullet(true);
            rt.setIndent(0d);  //bullet offset
            rt.setLeftMargin(50d);   //text offset (should be greater than bullet offset)
            rt.setBulletChar('\u263A'); //bullet character
            shape.setText(
                    "January\r" +
                            "February\r" +
                            "March\r" +
                            "April");
            slide.addShape(shape);

            shape.setAnchor(new java.awt.Rectangle(50, 50, 500, 300));  //position of the text box in the slide
            slide.addShape(shape);

            try (FileOutputStream out = new FileOutputStream("bullets.ppt")) {
                ppt.write(out);
            }
        }
   }
}
