import org.apache.poi.hwmf.record.*;


import java.awt.image.BufferedImage;
import java.io.IOException;

import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndianConsts;
import org.apache.poi.util.LittleEndianInputStream;

public class HwmfBitmap16 {
    final boolean isPartial;
    int type;
    int width;
    int height;
    int widthBytes;
    int planes;
    int bitsPixel;
    
    public HwmfBitmap16() {
        this(false);
    }
    
    public HwmfBitmap16(boolean isPartial) {
        this.isPartial = isPartial;
    }
    
    public int init(LittleEndianInputStream leis) throws IOException {
        // A 16-bit signed integer that defines the bitmap type.
        type = leis.readShort();
        
        // A 16-bit signed integer that defines the width of the bitmap in pixels.
        width = leis.readShort();
        
        // A 16-bit signed integer that defines the height of the bitmap in scan lines.
        height = leis.readShort();
        
        // A 16-bit signed integer that defines the number of bytes per scan line.
        widthBytes = leis.readShort();
        
        // An 8-bit unsigned integer that defines the number of color planes in the 
        // bitmap. The value of this field MUST be 0x01.
        planes = leis.readUByte();
        
        // An 8-bit unsigned integer that defines the number of adjacent color bits on 
        // each plane.
        bitsPixel = leis.readUByte();

        int size = 2*LittleEndianConsts.BYTE_SIZE+4*LittleEndianConsts.SHORT_SIZE;
        if (isPartial) {
            // Bits (4 bytes): This field MUST be ignored.
            long skipSize = leis.skip(LittleEndianConsts.INT_SIZE);
            assert(skipSize == LittleEndianConsts.INT_SIZE);
            // Reserved (18 bytes): This field MUST be ignored.
            skipSize = leis.skip(18);
            assert(skipSize == 18);
            size += 18+LittleEndianConsts.INT_SIZE;
        }

        int length = (((width * bitsPixel + 15) >> 4) << 1) * height;
        /*byte buf[] =*/ IOUtils.toByteArray(leis, length);
        
        // TODO: this is not implemented ... please provide a sample, if it
        // ever happens to you, to come here ...
        
        return size;
    }

    public BufferedImage getImage() {
        return new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
    }
}
