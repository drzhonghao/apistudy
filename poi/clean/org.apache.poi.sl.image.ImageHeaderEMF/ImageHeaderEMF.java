import org.apache.poi.sl.image.*;


import java.awt.Dimension;
import java.awt.Rectangle;

import org.apache.poi.util.Internal;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

@Internal
public class ImageHeaderEMF {
    private static final POILogger LOG = POILogFactory.getLogger(ImageHeaderEMF.class);

    private static final String EMF_SIGNATURE = " EMF"; // 0x464D4520 (LE)

    // rectangular inclusive-inclusive bounds, in device units, of the smallest
    // rectangle that can be drawn around the image stored in the metafile.
    private final Rectangle deviceBounds;

    public ImageHeaderEMF(final byte data[], final int off) {
        int offset = off;
        int type = (int)LittleEndian.getUInt(data, offset); offset += 4;
        if (type != 1) {
            LOG.log(POILogger.WARN, "Invalid EMF picture - invalid type");
            deviceBounds = new Rectangle(0,0,200,200);
            return;
        }
        // ignore header size
        offset += 4;
        int left = LittleEndian.getInt(data, offset); offset += 4;
        int top = LittleEndian.getInt(data, offset); offset += 4;
        int right = LittleEndian.getInt(data, offset); offset += 4;
        int bottom = LittleEndian.getInt(data, offset); offset += 4;
        deviceBounds = new Rectangle(left, top, right-left, bottom-top);
        // ignore frame bounds
        offset += 16;
        String signature = new String(data, offset, EMF_SIGNATURE.length(), LocaleUtil.CHARSET_1252);
        if (!EMF_SIGNATURE.equals(signature)) {
            LOG.log(POILogger.WARN, "Invalid EMF picture - invalid signature");
        }
    }

    public Dimension getSize() {
        return deviceBounds.getSize();
    }

    public Rectangle getBounds() {
        return deviceBounds;
    }
}
