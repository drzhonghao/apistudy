

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.PathShadeType;
import org.apache.poi.xddf.usermodel.XDDFRelativeRectangle;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPathShadeProperties;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTPathShadeProperties.Factory.newInstance;


@Beta
public class XDDFPathShadeProperties {
	private CTPathShadeProperties props;

	public XDDFPathShadeProperties() {
		this(newInstance());
	}

	protected XDDFPathShadeProperties(CTPathShadeProperties properties) {
		this.props = properties;
	}

	@org.apache.poi.util.Internal
	protected CTPathShadeProperties getXmlObject() {
		return props;
	}

	public XDDFRelativeRectangle getFillToRectangle() {
		if (props.isSetFillToRect()) {
		}else {
			return null;
		}
		return null;
	}

	public void setFillToRectangle(XDDFRelativeRectangle rectangle) {
		if (rectangle == null) {
			if (props.isSetFillToRect()) {
				props.unsetFillToRect();
			}
		}else {
		}
	}

	public PathShadeType getPathShadeType() {
		if (props.isSetPath()) {
		}else {
			return null;
		}
		return null;
	}

	public void setPathShadeType(PathShadeType path) {
		if (path == null) {
			if (props.isSetPath()) {
				props.unsetPath();
			}
		}else {
		}
	}
}

