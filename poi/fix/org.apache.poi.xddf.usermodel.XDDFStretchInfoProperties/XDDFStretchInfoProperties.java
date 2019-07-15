

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.XDDFRelativeRectangle;
import org.openxmlformats.schemas.drawingml.x2006.main.CTStretchInfoProperties;


@Beta
public class XDDFStretchInfoProperties {
	private CTStretchInfoProperties props;

	protected XDDFStretchInfoProperties(CTStretchInfoProperties properties) {
		this.props = properties;
	}

	@org.apache.poi.util.Internal
	protected CTStretchInfoProperties getXmlObject() {
		return props;
	}

	public XDDFRelativeRectangle getFillRectangle() {
		if (props.isSetFillRect()) {
		}else {
			return null;
		}
		return null;
	}

	public void setFillRectangle(XDDFRelativeRectangle rectangle) {
		if (rectangle == null) {
			if (props.isSetFillRect()) {
				props.unsetFillRect();
			}
		}else {
		}
	}
}

