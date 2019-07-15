

import java.util.Collections;
import java.util.List;
import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.TileFlipMode;
import org.apache.poi.xddf.usermodel.XDDFFillProperties;
import org.apache.poi.xddf.usermodel.XDDFGradientStop;
import org.apache.poi.xddf.usermodel.XDDFLinearShadeProperties;
import org.apache.poi.xddf.usermodel.XDDFPathShadeProperties;
import org.apache.poi.xddf.usermodel.XDDFRelativeRectangle;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGradientFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGradientStopList;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTGradientFillProperties.Factory.newInstance;


@Beta
public class XDDFGradientFillProperties implements XDDFFillProperties {
	private CTGradientFillProperties props;

	public XDDFGradientFillProperties() {
		this(newInstance());
	}

	protected XDDFGradientFillProperties(CTGradientFillProperties properties) {
		this.props = properties;
	}

	@org.apache.poi.util.Internal
	public CTGradientFillProperties getXmlObject() {
		return props;
	}

	public Boolean isRotatingWithShape() {
		if (props.isSetRotWithShape()) {
			return props.getRotWithShape();
		}else {
			return null;
		}
	}

	public void setRotatingWithShape(Boolean rotating) {
		if (rotating == null) {
			if (props.isSetRotWithShape()) {
				props.unsetRotWithShape();
			}
		}else {
			props.setRotWithShape(rotating);
		}
	}

	public TileFlipMode getTileFlipMode() {
		if (props.isSetFlip()) {
		}else {
			return null;
		}
		return null;
	}

	public void setTileFlipMode(TileFlipMode mode) {
		if (mode == null) {
			if (props.isSetFlip()) {
				props.unsetFlip();
			}
		}else {
		}
	}

	public XDDFGradientStop addGradientStop() {
		if (!(props.isSetGsLst())) {
			props.addNewGsLst();
		}
		return null;
	}

	public XDDFGradientStop insertGradientStop(int index) {
		if (!(props.isSetGsLst())) {
			props.addNewGsLst();
		}
		return null;
	}

	public void removeGradientStop(int index) {
		if (props.isSetGsLst()) {
			props.getGsLst().removeGs(index);
		}
	}

	public XDDFGradientStop getGradientStop(int index) {
		if (props.isSetGsLst()) {
		}else {
			return null;
		}
		return null;
	}

	public List<XDDFGradientStop> getGradientStops() {
		if (props.isSetGsLst()) {
		}else {
			return Collections.emptyList();
		}
		return null;
	}

	public int countGradientStops() {
		if (props.isSetGsLst()) {
			return props.getGsLst().sizeOfGsArray();
		}else {
			return 0;
		}
	}

	public XDDFLinearShadeProperties getLinearShadeProperties() {
		if (props.isSetLin()) {
		}else {
			return null;
		}
		return null;
	}

	public void setLinearShadeProperties(XDDFLinearShadeProperties properties) {
		if (properties == null) {
			if (props.isSetLin()) {
				props.unsetLin();
			}
		}else {
		}
	}

	public XDDFPathShadeProperties getPathShadeProperties() {
		if (props.isSetPath()) {
		}else {
			return null;
		}
		return null;
	}

	public void setPathShadeProperties(XDDFPathShadeProperties properties) {
		if (properties == null) {
			if (props.isSetPath()) {
				props.unsetPath();
			}
		}else {
		}
	}

	public XDDFRelativeRectangle getTileRectangle() {
		if (props.isSetTileRect()) {
		}else {
			return null;
		}
		return null;
	}

	public void setTileRectangle(XDDFRelativeRectangle rectangle) {
		if (rectangle == null) {
			if (props.isSetTileRect()) {
				props.unsetTileRect();
			}
		}else {
		}
	}
}

