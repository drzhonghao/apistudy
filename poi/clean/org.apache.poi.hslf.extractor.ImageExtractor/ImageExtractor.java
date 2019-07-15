import org.apache.poi.hslf.extractor.*;


import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlideShowImpl;
import org.apache.poi.sl.usermodel.PictureData.PictureType;

/**
 * Utility to extract pictures from a PowerPoint file.
 */
public final class ImageExtractor {
    public static void main(String args[]) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage:");
            System.err.println("\tImageExtractor <file>");
            return;
        }
        HSLFSlideShow ppt = new HSLFSlideShow(new HSLFSlideShowImpl(args[0]));

        //extract all pictures contained in the presentation
        int i = 0;
        for (HSLFPictureData pict : ppt.getPictureData()) {
            // picture data
            byte[] data = pict.getData();

            PictureType type = pict.getType();
            FileOutputStream out = new FileOutputStream("pict_" + i++ + type.extension);
            out.write(data);
            out.close();
        }
        
        ppt.close();
    }
}
