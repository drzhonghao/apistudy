import org.apache.poi.xddf.usermodel.text.*;


import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.XDDFPicture;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBlipBullet;

@Beta
public class XDDFBulletStylePicture implements XDDFBulletStyle {
    private CTTextBlipBullet style;

    @Internal
    protected XDDFBulletStylePicture(CTTextBlipBullet style) {
        this.style = style;
    }

    @Internal
    protected CTTextBlipBullet getXmlObject() {
        return style;
    }

    public XDDFPicture getPicture() {
        return new XDDFPicture(style.getBlip());
    }

    public void setPicture(XDDFPicture picture) {
        if (picture != null) {
            style.setBlip(picture.getXmlObject());
        }
    }
}
