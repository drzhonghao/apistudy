import org.apache.poi.xddf.usermodel.text.*;


import org.apache.poi.common.usermodel.fonts.FontGroup;
import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextFont;

@Beta
public class XDDFFont {
    private FontGroup group;
    private CTTextFont font;

    public static XDDFFont unsetFontForGroup(FontGroup group) {
        return new XDDFFont(group, null);
    }

    public XDDFFont(FontGroup group, String typeface, Byte charset, Byte pitch, byte[] panose) {
        this(group, CTTextFont.Factory.newInstance());
        if (typeface == null) {
            if (font.isSetTypeface()) {
                font.unsetTypeface();
            }
        } else {
            font.setTypeface(typeface);
        }
        if (charset == null) {
            if (font.isSetCharset()) {
                font.unsetCharset();
            }
        } else {
            font.setCharset(charset);
        }
        if (pitch == null) {
            if (font.isSetPitchFamily()) {
                font.unsetPitchFamily();
            }
        } else {
            font.setPitchFamily(pitch);
        }
        if (panose == null || panose.length == 0) {
            if (font.isSetPanose()) {
                font.unsetPanose();
            }
        } else {
            font.setPanose(panose);
        }
    }

    @Internal
    protected XDDFFont(FontGroup group, CTTextFont font) {
        this.group = group;
        this.font = font;
    }

    @Internal
    protected CTTextFont getXmlObject() {
        return font;
    }

    public FontGroup getGroup() {
        return group;
    }

    public String getTypeface() {
        if (font.isSetTypeface()) {
            return font.getTypeface();
        } else {
            return null;
        }
    }

    public Byte getCharset() {
        if (font.isSetCharset()) {
            return font.getCharset();
        } else {
            return null;
        }
    }

    public Byte getPitchFamily() {
        if (font.isSetPitchFamily()) {
            return font.getPitchFamily();
        } else {
            return null;
        }
    }

    public byte[] getPanose() {
        if (font.isSetPanose()) {
            return font.getPanose();
        } else {
            return null;
        }
    }
}
