import org.apache.poi.sl.image.*;


import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.poi.util.Internal;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;

@Internal
public class ImageHeaderBitmap {
    private static final POILogger LOG = POILogFactory.getLogger(ImageHeaderBitmap.class);
    
    private final Dimension size;
    
    public ImageHeaderBitmap(byte data[], int offset) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new ByteArrayInputStream(data, offset, data.length-offset));
        } catch (IOException e) {
            LOG.log(POILogger.WARN, "Can't determine image dimensions", e);
        }
        // set dummy size, in case of dummy dimension can't be set
        size = (img == null)
            ? new Dimension(200,200)
            : new Dimension(
                (int)Units.pixelToPoints(img.getWidth()),
                (int)Units.pixelToPoints(img.getHeight())
            );
    }

    public Dimension getSize() {
        return size;
    }
}
