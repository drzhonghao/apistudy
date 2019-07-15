

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFColorHsl;
import org.apache.poi.xddf.usermodel.XDDFColorPreset;
import org.apache.poi.xddf.usermodel.XDDFColorRgbBinary;
import org.apache.poi.xddf.usermodel.XDDFColorRgbPercent;
import org.apache.poi.xddf.usermodel.XDDFColorSchemeBased;
import org.apache.poi.xddf.usermodel.XDDFColorSystemDefined;
import org.apache.poi.xddf.usermodel.XDDFFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties.Factory.newInstance;


@Beta
public class XDDFSolidFillProperties implements XDDFFillProperties {
	private CTSolidColorFillProperties props;

	public XDDFSolidFillProperties() {
		this(newInstance());
	}

	public XDDFSolidFillProperties(XDDFColor color) {
		this(newInstance());
		setColor(color);
	}

	@Internal
	public XDDFSolidFillProperties(CTSolidColorFillProperties properties) {
		this.props = properties;
	}

	@Internal
	public CTSolidColorFillProperties getXmlObject() {
		return props;
	}

	public XDDFColor getColor() {
		if (props.isSetHslClr()) {
		}else
			if (props.isSetPrstClr()) {
			}else
				if (props.isSetSchemeClr()) {
				}else
					if (props.isSetScrgbClr()) {
					}else
						if (props.isSetSrgbClr()) {
						}else
							if (props.isSetSysClr()) {
							}





		return null;
	}

	public void setColor(XDDFColor color) {
		if (props.isSetHslClr()) {
			props.unsetHslClr();
		}
		if (props.isSetPrstClr()) {
			props.unsetPrstClr();
		}
		if (props.isSetSchemeClr()) {
			props.unsetSchemeClr();
		}
		if (props.isSetScrgbClr()) {
			props.unsetScrgbClr();
		}
		if (props.isSetSrgbClr()) {
			props.unsetSrgbClr();
		}
		if (props.isSetSysClr()) {
			props.unsetSysClr();
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

