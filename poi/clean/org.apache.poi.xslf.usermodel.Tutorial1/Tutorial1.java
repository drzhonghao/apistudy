import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.*;


import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Demonstrates how to create slides with predefined layout
 * and fill the placeholder shapes
 */
public class Tutorial1 {

    public static void main(String[] args) throws IOException{
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            // XSLFSlide#createSlide() with no arguments creates a blank slide
            /*XSLFSlide blankSlide =*/
            ppt.createSlide();


            XSLFSlideMaster master = ppt.getSlideMasters().get(0);

            XSLFSlideLayout layout1 = master.getLayout(SlideLayout.TITLE);
            XSLFSlide slide1 = ppt.createSlide(layout1);
            XSLFTextShape[] ph1 = slide1.getPlaceholders();
            XSLFTextShape titlePlaceholder1 = ph1[0];
            titlePlaceholder1.setText("This is a title");
            XSLFTextShape subtitlePlaceholder1 = ph1[1];
            subtitlePlaceholder1.setText("this is a subtitle");

            XSLFSlideLayout layout2 = master.getLayout(SlideLayout.TITLE_AND_CONTENT);
            XSLFSlide slide2 = ppt.createSlide(layout2);
            XSLFTextShape[] ph2 = slide2.getPlaceholders();
            XSLFTextShape titlePlaceholder2 = ph2[0];
            titlePlaceholder2.setText("This is a title");
            XSLFTextShape bodyPlaceholder = ph2[1];
            // we are going to add text by paragraphs. Clear the default placehoder text before that
            bodyPlaceholder.clearText();
            XSLFTextParagraph p1 = bodyPlaceholder.addNewTextParagraph();
            p1.setIndentLevel(0);
            p1.addNewTextRun().setText("Level1 text");
            XSLFTextParagraph p2 = bodyPlaceholder.addNewTextParagraph();
            p2.setIndentLevel(1);
            p2.addNewTextRun().setText("Level2 text");
            XSLFTextParagraph p3 = bodyPlaceholder.addNewTextParagraph();
            p3.setIndentLevel(2);
            p3.addNewTextRun().setText("Level3 text");

            try (FileOutputStream out = new FileOutputStream("slides.pptx")) {
                ppt.write(out);
            }
        }
    }
}
