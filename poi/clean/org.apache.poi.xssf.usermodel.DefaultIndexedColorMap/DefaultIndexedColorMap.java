import org.apache.poi.xssf.usermodel.*;


import org.apache.poi.hssf.util.HSSFColor;

/**
 * Uses the legacy colors defined in HSSF for index lookups
 */
public class DefaultIndexedColorMap implements IndexedColorMap {

    /**
     * @see org.apache.poi.xssf.usermodel.IndexedColorMap#getRGB(int)
     */
    public byte[] getRGB(int index) {
        return getDefaultRGB(index);
    }

    /**
     * @param index
     * @return RGB bytes from HSSF default color by index
     */
    public static byte[] getDefaultRGB(int index) {
        HSSFColor hssfColor = HSSFColor.getIndexHash().get(index);
        if (hssfColor == null) return null;
        short[] rgbShort = hssfColor.getTriplet();
        return new byte[] {(byte) rgbShort[0], (byte) rgbShort[1], (byte) rgbShort[2]};
    }

}
