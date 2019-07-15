

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetColor;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTColor.Factory.newInstance;


@Beta
public class XDDFColorPreset extends XDDFColor {
	private CTPresetColor color;

	public XDDFColorPreset(PresetColor color) {
		this(CTPresetColor.Factory.newInstance(), newInstance());
		setValue(color);
	}

	@Internal
	protected XDDFColorPreset(CTPresetColor color) {
		this(color, null);
	}

	@Internal
	protected XDDFColorPreset(CTPresetColor color, CTColor container) {
		super(container);
		this.color = color;
	}

	@Override
	@Internal
	protected XmlObject getXmlObject() {
		return color;
	}

	public PresetColor getValue() {
		if (color.isSetVal()) {
		}else {
			return null;
		}
		return null;
	}

	public void setValue(PresetColor value) {
		if (value == null) {
			if (color.isSetVal()) {
				color.unsetVal();
			}
		}else {
		}
	}
}

