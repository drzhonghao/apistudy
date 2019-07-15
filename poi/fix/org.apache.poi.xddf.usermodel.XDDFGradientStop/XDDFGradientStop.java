

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFColorHsl;
import org.apache.poi.xddf.usermodel.XDDFColorPreset;
import org.apache.poi.xddf.usermodel.XDDFColorRgbBinary;
import org.apache.poi.xddf.usermodel.XDDFColorRgbPercent;
import org.apache.poi.xddf.usermodel.XDDFColorSchemeBased;
import org.apache.poi.xddf.usermodel.XDDFColorSystemDefined;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGradientStop;


@Beta
public class XDDFGradientStop {
	private CTGradientStop stop;

	@Internal
	protected XDDFGradientStop(CTGradientStop stop) {
		this.stop = stop;
	}

	@Internal
	protected CTGradientStop getXmlObject() {
		return stop;
	}

	public int getPosition() {
		return stop.getPos();
	}

	public void setPosition(int position) {
		stop.setPos(position);
	}

	public XDDFColor getColor() {
		if (stop.isSetHslClr()) {
		}else
			if (stop.isSetPrstClr()) {
			}else
				if (stop.isSetSchemeClr()) {
				}else
					if (stop.isSetScrgbClr()) {
					}else
						if (stop.isSetSrgbClr()) {
						}else
							if (stop.isSetSysClr()) {
							}





		return null;
	}

	public void setColor(XDDFColor color) {
		if (stop.isSetHslClr()) {
			stop.unsetHslClr();
		}
		if (stop.isSetPrstClr()) {
			stop.unsetPrstClr();
		}
		if (stop.isSetSchemeClr()) {
			stop.unsetSchemeClr();
		}
		if (stop.isSetScrgbClr()) {
			stop.unsetScrgbClr();
		}
		if (stop.isSetSrgbClr()) {
			stop.unsetSrgbClr();
		}
		if (stop.isSetSysClr()) {
			stop.unsetSysClr();
		}
		if (color == null) {
			return;
		}
		if (color instanceof XDDFColorHsl) {
		}else
			if (color instanceof XDDFColorPreset) {
			}else
				if (color instanceof XDDFColorSchemeBased) {
				}else
					if (color instanceof XDDFColorRgbPercent) {
					}else
						if (color instanceof XDDFColorRgbBinary) {
						}else
							if (color instanceof XDDFColorSystemDefined) {
							}





	}
}

