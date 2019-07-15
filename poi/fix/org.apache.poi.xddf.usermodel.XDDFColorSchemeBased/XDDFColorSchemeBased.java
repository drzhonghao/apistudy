

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.SchemeColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSchemeColor;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTColor.Factory.newInstance;


@Beta
public class XDDFColorSchemeBased extends XDDFColor {
	private CTSchemeColor color;

	public XDDFColorSchemeBased(SchemeColor color) {
		this(CTSchemeColor.Factory.newInstance(), newInstance());
		setValue(color);
	}

	@Internal
	protected XDDFColorSchemeBased(CTSchemeColor color) {
		this(color, null);
	}

	@Internal
	protected XDDFColorSchemeBased(CTSchemeColor color, CTColor container) {
		super(container);
		this.color = color;
	}

	@Override
	@Internal
	protected XmlObject getXmlObject() {
		return color;
	}

	public SchemeColor getValue() {
		return null;
	}

	public void setValue(SchemeColor scheme) {
	}
}

