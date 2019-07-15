import org.apache.poi.hwpf.model.*;


import org.apache.poi.hwpf.sprm.CharacterSprmUncompressor;
import org.apache.poi.hwpf.sprm.SprmBuffer;
import org.apache.poi.hwpf.usermodel.CharacterProperties;
import org.apache.poi.util.Internal;

/**
 * DANGER - works in bytes!
 * <p>
 * Make sure you call getStart() / getEnd() when you want characters
 * (normal use), but getStartByte() / getEndByte() when you're
 * reading in / writing out!
 *
 * @author Ryan Ackley
 */
@Internal
@SuppressWarnings("deprecation")
public final class CHPX extends BytePropertyNode<CHPX> {

    CHPX(int charStart, int charEnd, SprmBuffer buf) {
        super(charStart, charEnd, buf);
    }

    public byte[] getGrpprl() {
        return ((SprmBuffer) _buf).toByteArray();
    }

    public SprmBuffer getSprmBuf() {
        return (SprmBuffer) _buf;
    }

    public CharacterProperties getCharacterProperties(StyleSheet ss, short istd) {
        if (ss == null) {
            // TODO Fix up for Word 6/95
            return new CharacterProperties();
        }

        CharacterProperties baseStyle = ss.getCharacterStyle(istd);
        return CharacterSprmUncompressor.uncompressCHP(
                ss, baseStyle, getGrpprl(), 0);
    }

    public String toString() {
        return "CHPX from " + getStart() + " to " + getEnd() +
                " (in bytes " + getStartBytes() + " to " + getEndBytes() + ")";
    }
}
