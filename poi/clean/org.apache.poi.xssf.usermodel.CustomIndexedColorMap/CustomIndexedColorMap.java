import org.apache.poi.xssf.usermodel.*;


import java.util.List;

import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColors;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRgbColor;

/**
 * custom index color map, i.e. from the styles.xml definition
 */
public class CustomIndexedColorMap implements IndexedColorMap {

    private final byte[][] colorIndex;
    
    /**
     * @param colors array of RGB triplets indexed by color index
     */
    private CustomIndexedColorMap(byte [][] colors) {
        this.colorIndex = colors;
    }
    
    public byte[] getRGB(int index) {
        if (colorIndex == null || index < 0 || index >= colorIndex.length) return null;
        return colorIndex[index];
    }

    /**
     * OOXML spec says if this exists it must have all indexes.
     * <p>
     * From the OOXML Spec, Part 1, section 18.8.27:
     * <p><i>
     * This element contains a sequence of RGB color values that correspond to color indexes (zero-based). When
     * using the default indexed color palette, the values are not written out, but instead are implied. When the color
     * palette has been modified from default, then the entire color palette is written out.
     * </i>
     * @param colors CTColors from styles.xml possibly defining a custom color indexing scheme 
     * @return custom indexed color map or null if none defined in the document
     */
    public static CustomIndexedColorMap fromColors(CTColors colors) {
        if (colors == null || ! colors.isSetIndexedColors()) return null;

        List<CTRgbColor> rgbColorList = colors.getIndexedColors().getRgbColorList();
        byte[][] customColorIndex = new byte[rgbColorList.size()][3];
        for (int i=0; i < rgbColorList.size(); i++) {
            customColorIndex[i] = rgbColorList.get(i).getRgb();
        }
        return new CustomIndexedColorMap(customColorIndex);
    }
}
