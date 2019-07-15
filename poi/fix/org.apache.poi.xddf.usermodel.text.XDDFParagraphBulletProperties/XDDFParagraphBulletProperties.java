

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.text.XDDFBulletSize;
import org.apache.poi.xddf.usermodel.text.XDDFBulletSizeFollowText;
import org.apache.poi.xddf.usermodel.text.XDDFBulletSizePercent;
import org.apache.poi.xddf.usermodel.text.XDDFBulletSizePoints;
import org.apache.poi.xddf.usermodel.text.XDDFBulletStyle;
import org.apache.poi.xddf.usermodel.text.XDDFBulletStyleAutoNumbered;
import org.apache.poi.xddf.usermodel.text.XDDFBulletStyleCharacter;
import org.apache.poi.xddf.usermodel.text.XDDFBulletStyleNone;
import org.apache.poi.xddf.usermodel.text.XDDFBulletStylePicture;
import org.apache.poi.xddf.usermodel.text.XDDFFont;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBulletColorFollowText;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBulletTypefaceFollowText;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;


@Beta
public class XDDFParagraphBulletProperties {
	private CTTextParagraphProperties props;

	@Internal
	protected XDDFParagraphBulletProperties(CTTextParagraphProperties properties) {
		this.props = properties;
	}

	public XDDFBulletStyle getBulletStyle() {
		if (props.isSetBuAutoNum()) {
		}else
			if (props.isSetBuBlip()) {
			}else
				if (props.isSetBuChar()) {
				}else
					if (props.isSetBuNone()) {
					}else {
						return null;
					}



		return null;
	}

	public void setBulletStyle(XDDFBulletStyle style) {
		if (props.isSetBuAutoNum()) {
			props.unsetBuAutoNum();
		}
		if (props.isSetBuBlip()) {
			props.unsetBuBlip();
		}
		if (props.isSetBuChar()) {
			props.unsetBuChar();
		}
		if (props.isSetBuNone()) {
			props.unsetBuNone();
		}
		if (style != null) {
			if (style instanceof XDDFBulletStyleAutoNumbered) {
			}else
				if (style instanceof XDDFBulletStyleCharacter) {
				}else
					if (style instanceof XDDFBulletStyleNone) {
					}else
						if (style instanceof XDDFBulletStylePicture) {
						}



		}
	}

	public XDDFColor getBulletColor() {
		if (props.isSetBuClr()) {
			return XDDFColor.forColorContainer(props.getBuClr());
		}else {
			return null;
		}
	}

	public void setBulletColor(XDDFColor color) {
		if (props.isSetBuClrTx()) {
			props.unsetBuClrTx();
		}
		if (color == null) {
			if (props.isSetBuClr()) {
				props.unsetBuClr();
			}
		}else {
			props.setBuClr(color.getColorContainer());
		}
	}

	public void setBulletColorFollowText() {
		if (props.isSetBuClr()) {
			props.unsetBuClr();
		}
		if (props.isSetBuClrTx()) {
		}else {
			props.addNewBuClrTx();
		}
	}

	public XDDFFont getBulletFont() {
		if (props.isSetBuFont()) {
		}else {
			return null;
		}
		return null;
	}

	public void setBulletFont(XDDFFont font) {
		if (props.isSetBuFontTx()) {
			props.unsetBuFontTx();
		}
		if (font == null) {
			if (props.isSetBuFont()) {
				props.unsetBuFont();
			}
		}else {
		}
	}

	public void setBulletFontFollowText() {
		if (props.isSetBuFont()) {
			props.unsetBuFont();
		}
		if (props.isSetBuFontTx()) {
		}else {
			props.addNewBuFontTx();
		}
	}

	public XDDFBulletSize getBulletSize() {
		if (props.isSetBuSzPct()) {
		}else
			if (props.isSetBuSzPts()) {
			}else
				if (props.isSetBuSzTx()) {
				}else {
					return null;
				}


		return null;
	}

	public void setBulletSize(XDDFBulletSize size) {
		if (props.isSetBuSzPct()) {
			props.unsetBuSzPct();
		}
		if (props.isSetBuSzPts()) {
			props.unsetBuSzPts();
		}
		if (props.isSetBuSzTx()) {
			props.unsetBuSzTx();
		}
		if (size != null) {
			if (size instanceof XDDFBulletSizeFollowText) {
			}else
				if (size instanceof XDDFBulletSizePercent) {
				}else
					if (size instanceof XDDFBulletSizePoints) {
					}


		}
	}

	public void clearAll() {
		if (props.isSetBuAutoNum()) {
			props.unsetBuAutoNum();
		}
		if (props.isSetBuBlip()) {
			props.unsetBuBlip();
		}
		if (props.isSetBuChar()) {
			props.unsetBuChar();
		}
		if (props.isSetBuNone()) {
			props.unsetBuNone();
		}
		if (props.isSetBuClr()) {
			props.unsetBuClr();
		}
		if (props.isSetBuClrTx()) {
			props.unsetBuClrTx();
		}
		if (props.isSetBuFont()) {
			props.unsetBuFont();
		}
		if (props.isSetBuFontTx()) {
			props.unsetBuFontTx();
		}
		if (props.isSetBuSzPct()) {
			props.unsetBuSzPct();
		}
		if (props.isSetBuSzPts()) {
			props.unsetBuSzPts();
		}
		if (props.isSetBuSzTx()) {
			props.unsetBuSzTx();
		}
	}
}

