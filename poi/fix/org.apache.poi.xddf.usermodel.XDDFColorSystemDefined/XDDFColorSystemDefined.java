

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.SystemColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSystemColor;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTColor.Factory.newInstance;


@Beta
public class XDDFColorSystemDefined extends XDDFColor {
	private CTSystemColor color;

	public XDDFColorSystemDefined(SystemColor color) {
		this(CTSystemColor.Factory.newInstance(), newInstance());
		setValue(color);
	}

	@Internal
	protected XDDFColorSystemDefined(CTSystemColor color) {
		this(color, null);
	}

	@Internal
	protected XDDFColorSystemDefined(CTSystemColor color, CTColor container) {
		super(container);
		this.color = color;
	}

	@Override
	@Internal
	protected XmlObject getXmlObject() {
		return color;
	}

	public SystemColor getValue() {
		return null;
	}

	public void setValue(SystemColor value) {
	}

	public byte[] getLastColor() {
		if (color.isSetLastClr()) {
			return color.getLastClr();
		}else {
			return null;
		}
	}

	public void setLastColor(byte[] last) {
		if (last == null) {
			if (color.isSetLastClr()) {
				color.unsetLastClr();
			}
		}else {
			color.setLastClr(last);
		}
	}
}

