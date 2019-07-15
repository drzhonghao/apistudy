import org.apache.poi.hslf.examples.*;


import java.io.FileInputStream;
import java.util.List;
import java.util.Locale;

import org.apache.poi.hslf.usermodel.HSLFHyperlink;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSimpleShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;

/**
 * Demonstrates how to read hyperlinks from  a presentation
 *
 * @author Yegor Kozlov
 */
public final class Hyperlinks {

    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            FileInputStream is = new FileInputStream(arg);
            HSLFSlideShow ppt = new HSLFSlideShow(is);
            is.close();

            for (HSLFSlide slide : ppt.getSlides()) {
                System.out.println("\nslide " + slide.getSlideNumber());

                // read hyperlinks from the slide's text runs
                System.out.println("- reading hyperlinks from the text runs");
                for (List<HSLFTextParagraph> paras : slide.getTextParagraphs()) {
                    for (HSLFTextParagraph para : paras) {
                        for (HSLFTextRun run : para) {
                            HSLFHyperlink link = run.getHyperlink();
                            if (link != null) {
                                System.out.println(toStr(link, run.getRawText()));
                            }
                        }
                    }
                }

                // in PowerPoint you can assign a hyperlink to a shape without text,
                // for example to a Line object. The code below demonstrates how to
                // read such hyperlinks
                System.out.println("- reading hyperlinks from the slide's shapes");
                for (HSLFShape sh : slide.getShapes()) {
                    if (sh instanceof HSLFSimpleShape) {
                        HSLFHyperlink link = ((HSLFSimpleShape) sh).getHyperlink();
                        if (link != null) {
                            System.out.println(toStr(link, null));
                        }
                    }
                }
            }
            ppt.close();
        }
   }

    static String toStr(HSLFHyperlink link, String rawText) {
        //in ppt end index is inclusive
        String formatStr = "title: %1$s, address: %2$s" + (rawText == null ? "" : ", start: %3$s, end: %4$s, substring: %5$s");
        return String.format(Locale.ROOT, formatStr, link.getLabel(), link.getAddress(), link.getStartIndex(), link.getEndIndex(), rawText);
    }
}
