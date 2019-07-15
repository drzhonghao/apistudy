

import org.apache.poi.util.Beta;
import org.apache.poi.xddf.usermodel.RectangleAlignment;
import org.apache.poi.xddf.usermodel.TileFlipMode;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTileInfoProperties;


@Beta
public class XDDFTileInfoProperties {
	private CTTileInfoProperties props;

	protected XDDFTileInfoProperties(CTTileInfoProperties properties) {
		this.props = properties;
	}

	@org.apache.poi.util.Internal
	protected CTTileInfoProperties getXmlObject() {
		return props;
	}

	public void setAlignment(RectangleAlignment alignment) {
		if (alignment == null) {
			if (props.isSetAlgn()) {
				props.unsetAlgn();
			}
		}else {
		}
	}

	public TileFlipMode getFlipMode() {
		if (props.isSetFlip()) {
		}else {
			return null;
		}
		return null;
	}

	public void setFlipMode(TileFlipMode mode) {
		if (mode == null) {
			if (props.isSetFlip()) {
				props.unsetFlip();
			}
		}else {
		}
	}

	public Integer getSx() {
		if (props.isSetSx()) {
			return props.getSx();
		}else {
			return null;
		}
	}

	public void setSx(Integer value) {
		if (value == null) {
			if (props.isSetSx()) {
				props.unsetSx();
			}
		}else {
			props.setSx(value);
		}
	}

	public Integer getSy() {
		if (props.isSetSy()) {
			return props.getSy();
		}else {
			return null;
		}
	}

	public void setSy(Integer value) {
		if (value == null) {
			if (props.isSetSy()) {
				props.unsetSy();
			}
		}else {
			props.setSy(value);
		}
	}

	public Long getTx() {
		if (props.isSetTx()) {
			return props.getTx();
		}else {
			return null;
		}
	}

	public void setTx(Long value) {
		if (value == null) {
			if (props.isSetTx()) {
				props.unsetTx();
			}
		}else {
			props.setTx(value);
		}
	}

	public Long getTy() {
		if (props.isSetTy()) {
			return props.getTy();
		}else {
			return null;
		}
	}

	public void setTy(Long value) {
		if (value == null) {
			if (props.isSetTy()) {
				props.unsetTy();
			}
		}else {
			props.setTy(value);
		}
	}
}

