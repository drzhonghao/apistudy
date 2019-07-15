

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.LineEndLength;
import org.apache.poi.xddf.usermodel.LineEndType;
import org.apache.poi.xddf.usermodel.LineEndWidth;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineEndProperties;


@Beta
public class XDDFLineEndProperties {
	private CTLineEndProperties props;

	protected XDDFLineEndProperties(CTLineEndProperties properties) {
		this.props = properties;
	}

	@org.apache.poi.util.Internal
	protected CTLineEndProperties getXmlObject() {
		return props;
	}

	public LineEndLength getLength() {
		return null;
	}

	public void setLength(LineEndLength length) {
	}

	public LineEndType getType() {
		return null;
	}

	public void setType(LineEndType type) {
	}

	public LineEndWidth getWidth() {
		return null;
	}

	public void setWidth(LineEndWidth width) {
	}
}

