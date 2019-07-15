import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.*;


import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Merge multiple pptx presentations together
 */
public final class MergePresentations {

    public static void main(String args[]) throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            for (String arg : args) {
                try (FileInputStream is = new FileInputStream(arg);
                     XMLSlideShow src = new XMLSlideShow(is)) {
                    for (XSLFSlide srcSlide : src.getSlides()) {
                        ppt.createSlide().importContent(srcSlide);
                    }
                }
            }

            try (FileOutputStream out = new FileOutputStream("merged.pptx")) {
                ppt.write(out);
            }
        }
    }
}
