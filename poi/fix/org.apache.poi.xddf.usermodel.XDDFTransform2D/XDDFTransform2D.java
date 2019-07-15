

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.XDDFPoint2D;
import org.apache.poi.xddf.usermodel.XDDFPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;


@Beta
public class XDDFTransform2D {
	private CTTransform2D transform;

	protected XDDFTransform2D(CTTransform2D transform) {
		this.transform = transform;
	}

	@org.apache.poi.util.Internal
	protected CTTransform2D getXmlObject() {
		return transform;
	}

	public Boolean getFlipHorizontal() {
		if (transform.isSetFlipH()) {
			return transform.getFlipH();
		}else {
			return null;
		}
	}

	public void setFlipHorizontal(Boolean flip) {
		if (flip == null) {
			if (transform.isSetFlipH()) {
				transform.unsetFlipH();
			}
		}else {
			transform.setFlipH(flip);
		}
	}

	public Boolean getFlipVertical() {
		if (transform.isSetFlipV()) {
			return transform.getFlipV();
		}else {
			return null;
		}
	}

	public void setFlipVertical(Boolean flip) {
		if (flip == null) {
			if (transform.isSetFlipV()) {
				transform.unsetFlipV();
			}
		}else {
			transform.setFlipV(flip);
		}
	}

	public XDDFPositiveSize2D getExtension() {
		if (transform.isSetExt()) {
		}else {
			return null;
		}
		return null;
	}

	public void setExtension(XDDFPositiveSize2D extension) {
		CTPositiveSize2D xformExt;
		if (extension == null) {
			if (transform.isSetExt()) {
				transform.unsetExt();
			}
			return;
		}else
			if (transform.isSetExt()) {
				xformExt = transform.getExt();
			}else {
				xformExt = transform.addNewExt();
			}

		xformExt.setCx(extension.getX());
		xformExt.setCy(extension.getY());
	}

	public XDDFPoint2D getOffset() {
		if (transform.isSetOff()) {
		}else {
			return null;
		}
		return null;
	}

	public void setOffset(XDDFPoint2D offset) {
		CTPoint2D xformOff;
		if (offset == null) {
			if (transform.isSetOff()) {
				transform.unsetOff();
			}
			return;
		}else
			if (transform.isSetOff()) {
				xformOff = transform.getOff();
			}else {
				xformOff = transform.addNewOff();
			}

		xformOff.setX(offset.getX());
		xformOff.setY(offset.getY());
	}

	public Integer getRotation() {
		if (transform.isSetRot()) {
			return transform.getRot();
		}else {
			return null;
		}
	}

	public void setRotation(Integer rotation) {
		if (rotation == null) {
			if (transform.isSetRot()) {
				transform.unsetRot();
			}
		}else {
			transform.setRot(rotation);
		}
	}
}

