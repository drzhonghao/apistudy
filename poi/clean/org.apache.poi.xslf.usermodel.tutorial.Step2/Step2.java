import org.apache.poi.xslf.usermodel.tutorial.*;


import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.io.FileOutputStream;

/**
 * Create slides from pre-defined slide layouts
 */
public class Step2 {
    public static void main(String[] args) throws Exception{
        try (XMLSlideShow ppt = new XMLSlideShow()) {

            // first see what slide layouts are available by default
            System.out.println("Available slide layouts:");
            for (XSLFSlideMaster master : ppt.getSlideMasters()) {
                for (XSLFSlideLayout layout : master.getSlideLayouts()) {
                    System.out.println(layout.getType());
                }
            }

            // blank slide
        /*XSLFSlide blankSlide =*/
            ppt.createSlide();

            XSLFSlideMaster defaultMaster = ppt.getSlideMasters().get(0);

            // title slide
            XSLFSlideLayout titleLayout = defaultMaster.getLayout(SlideLayout.TITLE);
            XSLFSlide slide1 = ppt.createSlide(titleLayout);
            XSLFTextShape title1 = slide1.getPlaceholder(0);
            title1.setText("First Title");

            // title and content
            XSLFSlideLayout titleBodyLayout = defaultMaster.getLayout(SlideLayout.TITLE_AND_CONTENT);
            XSLFSlide slide2 = ppt.createSlide(titleBodyLayout);

            XSLFTextShape title2 = slide2.getPlaceholder(0);
            title2.setText("Second Title");

            XSLFTextShape body2 = slide2.getPlaceholder(1);
            body2.clearText(); // unset any existing text
            body2.addNewTextParagraph().addNewTextRun().setText("First paragraph");
            body2.addNewTextParagraph().addNewTextRun().setText("Second paragraph");
            body2.addNewTextParagraph().addNewTextRun().setText("Third paragraph");


            try (FileOutputStream out = new FileOutputStream("step2.pptx")) {
                ppt.write(out);
            }
        }
    }
}
