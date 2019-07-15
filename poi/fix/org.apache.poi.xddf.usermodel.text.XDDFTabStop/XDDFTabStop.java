

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.util.Units;
import org.apache.poi.xddf.usermodel.text.TabAlignment;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextTabStop;


@Beta
public class XDDFTabStop {
	private CTTextTabStop stop;

	@Internal
	protected XDDFTabStop(CTTextTabStop stop) {
		this.stop = stop;
	}

	@Internal
	protected CTTextTabStop getXmlObject() {
		return stop;
	}

	public TabAlignment getAlignment() {
		if (stop.isSetAlgn()) {
		}else {
			return null;
		}
		return null;
	}

	public void setAlignment(TabAlignment align) {
		if (align == null) {
			if (stop.isSetAlgn()) {
				stop.unsetAlgn();
			}
		}else {
		}
	}

	public Double getPosition() {
		if (stop.isSetPos()) {
			return Units.toPoints(stop.getPos());
		}else {
			return null;
		}
	}

	public void setPosition(Double position) {
		if (position == null) {
			if (stop.isSetPos()) {
				stop.unsetPos();
			}
		}else {
			stop.setPos(Units.toEMU(position));
		}
	}
}

