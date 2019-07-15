

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.SchemeColor;
import org.apache.poi.xddf.usermodel.SystemColor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;


@Beta
public abstract class XDDFColor {
	protected CTColor container;

	@Internal
	protected XDDFColor(CTColor container) {
		this.container = container;
	}

	public static XDDFColor from(byte[] color) {
		return null;
	}

	public static XDDFColor from(int red, int green, int blue) {
		return null;
	}

	public static XDDFColor from(PresetColor color) {
		return null;
	}

	public static XDDFColor from(SchemeColor color) {
		return null;
	}

	public static XDDFColor from(SystemColor color) {
		return null;
	}

	@Internal
	public static XDDFColor forColorContainer(CTColor container) {
		if (container.isSetHslClr()) {
		}else
			if (container.isSetPrstClr()) {
			}else
				if (container.isSetSchemeClr()) {
				}else
					if (container.isSetScrgbClr()) {
					}else
						if (container.isSetSrgbClr()) {
						}else
							if (container.isSetSysClr()) {
							}





		return null;
	}

	@Internal
	public CTColor getColorContainer() {
		return container;
	}

	@Internal
	protected abstract XmlObject getXmlObject();
}

