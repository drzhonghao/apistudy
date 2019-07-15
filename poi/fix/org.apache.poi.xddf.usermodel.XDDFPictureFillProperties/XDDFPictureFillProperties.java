

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.XDDFFillProperties;
import org.apache.poi.xddf.usermodel.XDDFPicture;
import org.apache.poi.xddf.usermodel.XDDFRelativeRectangle;
import org.apache.poi.xddf.usermodel.XDDFStretchInfoProperties;
import org.apache.poi.xddf.usermodel.XDDFTileInfoProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlip;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties.Factory.newInstance;


@Beta
public class XDDFPictureFillProperties implements XDDFFillProperties {
	private CTBlipFillProperties props;

	public XDDFPictureFillProperties() {
		this(newInstance());
	}

	protected XDDFPictureFillProperties(CTBlipFillProperties properties) {
		this.props = properties;
	}

	@org.apache.poi.util.Internal
	public CTBlipFillProperties getXmlObject() {
		return props;
	}

	public XDDFPicture getPicture() {
		if (props.isSetBlip()) {
			return new XDDFPicture(props.getBlip());
		}else {
			return null;
		}
	}

	public void setPicture(XDDFPicture picture) {
		if (picture == null) {
			props.unsetBlip();
		}else {
			props.setBlip(picture.getXmlObject());
		}
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

	public Long getDpi() {
		if (props.isSetDpi()) {
			return props.getDpi();
		}else {
			return null;
		}
	}

	public void setDpi(Long dpi) {
		if (dpi == null) {
			if (props.isSetDpi()) {
				props.unsetDpi();
			}
		}else {
			props.setDpi(dpi);
		}
	}

	public XDDFRelativeRectangle getSourceRectangle() {
		if (props.isSetSrcRect()) {
		}else {
			return null;
		}
		return null;
	}

	public void setSourceRectangle(XDDFRelativeRectangle rectangle) {
		if (rectangle == null) {
			if (props.isSetSrcRect()) {
				props.unsetSrcRect();
			}
		}else {
		}
	}

	public XDDFStretchInfoProperties getStetchInfoProperties() {
		if (props.isSetStretch()) {
		}else {
			return null;
		}
		return null;
	}

	public void setStretchInfoProperties(XDDFStretchInfoProperties properties) {
		if (properties == null) {
			if (props.isSetStretch()) {
				props.unsetStretch();
			}
		}else {
		}
	}

	public XDDFTileInfoProperties getTileInfoProperties() {
		if (props.isSetTile()) {
		}else {
			return null;
		}
		return null;
	}

	public void setTileInfoProperties(XDDFTileInfoProperties properties) {
		if (properties == null) {
			if (props.isSetTile()) {
				props.unsetTile();
			}
		}else {
		}
	}
}

