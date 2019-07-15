

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.PresetPattern;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPatternFillProperties;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTPatternFillProperties.Factory.newInstance;


@Beta
public class XDDFPatternFillProperties implements XDDFFillProperties {
	private CTPatternFillProperties props;

	public XDDFPatternFillProperties() {
		this(newInstance());
	}

	protected XDDFPatternFillProperties(CTPatternFillProperties properties) {
		this.props = properties;
	}

	@org.apache.poi.util.Internal
	public CTPatternFillProperties getXmlObject() {
		return props;
	}

	public PresetPattern getPresetPattern() {
		if (props.isSetPrst()) {
		}else {
			return null;
		}
		return null;
	}

	public void setPresetPattern(PresetPattern pattern) {
		if (pattern == null) {
			if (props.isSetPrst()) {
				props.unsetPrst();
			}
		}else {
		}
	}

	public XDDFColor getBackgroundColor() {
		if (props.isSetBgClr()) {
			return XDDFColor.forColorContainer(props.getBgClr());
		}else {
			return null;
		}
	}

	public void setBackgroundColor(XDDFColor color) {
		if (color == null) {
			if (props.isSetBgClr()) {
				props.unsetBgClr();
			}
		}else {
			props.setBgClr(color.getColorContainer());
		}
	}

	public XDDFColor getForegroundColor() {
		if (props.isSetFgClr()) {
			return XDDFColor.forColorContainer(props.getFgClr());
		}else {
			return null;
		}
	}

	public void setForegroundColor(XDDFColor color) {
		if (color == null) {
			if (props.isSetFgClr()) {
				props.unsetFgClr();
			}
		}else {
			props.setFgClr(color.getColorContainer());
		}
	}
}

