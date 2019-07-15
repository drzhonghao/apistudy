import org.apache.poi.hslf.blip.*;


import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.Units;

/**
 * Represents a bitmap picture data:  JPEG or PNG.
 * The data is not compressed and the exact file content is written in the stream.
 */
public abstract class Bitmap extends HSLFPictureData {

    @Override
    public byte[] getData(){
        byte[] rawdata = getRawData();
        int prefixLen = 16*getUIDInstanceCount()+1;
        byte[] imgdata = IOUtils.safelyAllocate(rawdata.length-prefixLen, rawdata.length);
        System.arraycopy(rawdata, prefixLen, imgdata, 0, imgdata.length);
        return imgdata;
    }

    @Override
    public void setData(byte[] data) throws IOException {
        byte[] checksum = getChecksum(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(checksum);
        if (getUIDInstanceCount() == 2) {
            out.write(checksum);
        }
        out.write(0);
        out.write(data);

        setRawData(out.toByteArray());
    }

    @Override
    public Dimension getImageDimension() {
        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(getData()));
            return new Dimension(
                (int)Units.pixelToPoints(bi.getWidth()),
                (int)Units.pixelToPoints(bi.getHeight())
            );
        } catch (IOException e) {
            return new Dimension(200,200);
        }
    }
}
