

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.PresetLineDash;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetLineDashProperties;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTPresetLineDashProperties.Factory.newInstance;


@Beta
public class XDDFPresetLineDash {
	private CTPresetLineDashProperties props;

	public XDDFPresetLineDash(PresetLineDash dash) {
		this(newInstance());
		setValue(dash);
	}

	protected XDDFPresetLineDash(CTPresetLineDashProperties properties) {
		this.props = properties;
	}

	@org.apache.poi.util.Internal
	protected CTPresetLineDashProperties getXmlObject() {
		return props;
	}

	public PresetLineDash getValue() {
		if (props.isSetVal()) {
		}else {
			return null;
		}
		return null;
	}

	public void setValue(PresetLineDash dash) {
		if (dash == null) {
			if (props.isSetVal()) {
				props.unsetVal();
			}
		}else {
		}
	}
}

