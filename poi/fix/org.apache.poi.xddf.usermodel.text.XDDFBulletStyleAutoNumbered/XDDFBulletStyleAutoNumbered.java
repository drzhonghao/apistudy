

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.text.AutonumberScheme;
import org.apache.poi.xddf.usermodel.text.XDDFBulletStyle;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextAutonumberBullet;


@Beta
public class XDDFBulletStyleAutoNumbered implements XDDFBulletStyle {
	private CTTextAutonumberBullet style;

	@Internal
	protected XDDFBulletStyleAutoNumbered(CTTextAutonumberBullet style) {
		this.style = style;
	}

	@Internal
	protected CTTextAutonumberBullet getXmlObject() {
		return style;
	}

	public AutonumberScheme getType() {
		return null;
	}

	public void setType(AutonumberScheme scheme) {
	}

	public int getStartAt() {
		if (style.isSetStartAt()) {
			return style.getStartAt();
		}else {
			return 1;
		}
	}

	public void setStartAt(Integer value) {
		if (value == null) {
			if (style.isSetStartAt()) {
				style.unsetStartAt();
			}
		}else {
			style.setStartAt(value);
		}
	}
}

