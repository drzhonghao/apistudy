

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.BlackWhiteMode;
import org.apache.poi.xddf.usermodel.XDDFCustomGeometry2D;
import org.apache.poi.xddf.usermodel.XDDFEffectContainer;
import org.apache.poi.xddf.usermodel.XDDFEffectList;
import org.apache.poi.xddf.usermodel.XDDFExtensionList;
import org.apache.poi.xddf.usermodel.XDDFFillProperties;
import org.apache.poi.xddf.usermodel.XDDFGradientFillProperties;
import org.apache.poi.xddf.usermodel.XDDFGroupFillProperties;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFNoFillProperties;
import org.apache.poi.xddf.usermodel.XDDFPatternFillProperties;
import org.apache.poi.xddf.usermodel.XDDFPictureFillProperties;
import org.apache.poi.xddf.usermodel.XDDFPresetGeometry2D;
import org.apache.poi.xddf.usermodel.XDDFScene3D;
import org.apache.poi.xddf.usermodel.XDDFShape3D;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.XDDFTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTEffectContainer;
import org.openxmlformats.schemas.drawingml.x2006.main.CTEffectList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGradientFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGroupFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNoFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeArtExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPatternFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTScene3D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShape3D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties.Factory.newInstance;


@Beta
public class XDDFShapeProperties {
	private CTShapeProperties props;

	public XDDFShapeProperties() {
		this(newInstance());
	}

	@Internal
	public XDDFShapeProperties(CTShapeProperties properties) {
		this.props = properties;
	}

	@Internal
	public CTShapeProperties getXmlObject() {
		return props;
	}

	public BlackWhiteMode getBlackWhiteMode() {
		if (props.isSetBwMode()) {
		}else {
			return null;
		}
		return null;
	}

	public void setBlackWhiteMode(BlackWhiteMode mode) {
		if (mode == null) {
			if (props.isSetBwMode()) {
				props.unsetBwMode();
			}
		}else {
		}
	}

	public XDDFFillProperties getFillProperties() {
		if (props.isSetGradFill()) {
		}else
			if (props.isSetGrpFill()) {
			}else
				if (props.isSetNoFill()) {
				}else
					if (props.isSetPattFill()) {
					}else
						if (props.isSetBlipFill()) {
						}else
							if (props.isSetSolidFill()) {
								return new XDDFSolidFillProperties(props.getSolidFill());
							}else {
								return null;
							}





		return null;
	}

	public void setFillProperties(XDDFFillProperties properties) {
		if (props.isSetBlipFill()) {
			props.unsetBlipFill();
		}
		if (props.isSetGradFill()) {
			props.unsetGradFill();
		}
		if (props.isSetGrpFill()) {
			props.unsetGrpFill();
		}
		if (props.isSetNoFill()) {
			props.unsetNoFill();
		}
		if (props.isSetPattFill()) {
			props.unsetPattFill();
		}
		if (props.isSetSolidFill()) {
			props.unsetSolidFill();
		}
		if (properties == null) {
			return;
		}
		if (properties instanceof XDDFGradientFillProperties) {
			props.setGradFill(((XDDFGradientFillProperties) (properties)).getXmlObject());
		}else
			if (properties instanceof XDDFGroupFillProperties) {
				props.setGrpFill(((XDDFGroupFillProperties) (properties)).getXmlObject());
			}else
				if (properties instanceof XDDFNoFillProperties) {
					props.setNoFill(((XDDFNoFillProperties) (properties)).getXmlObject());
				}else
					if (properties instanceof XDDFPatternFillProperties) {
						props.setPattFill(((XDDFPatternFillProperties) (properties)).getXmlObject());
					}else
						if (properties instanceof XDDFPictureFillProperties) {
							props.setBlipFill(((XDDFPictureFillProperties) (properties)).getXmlObject());
						}else
							if (properties instanceof XDDFSolidFillProperties) {
								props.setSolidFill(((XDDFSolidFillProperties) (properties)).getXmlObject());
							}





	}

	public XDDFLineProperties getLineProperties() {
		if (props.isSetLn()) {
			return new XDDFLineProperties(props.getLn());
		}else {
			return null;
		}
	}

	public void setLineProperties(XDDFLineProperties properties) {
		if (properties == null) {
			if (props.isSetLn()) {
				props.unsetLn();
			}
		}else {
			props.setLn(properties.getXmlObject());
		}
	}

	public XDDFCustomGeometry2D getCustomGeometry2D() {
		if (props.isSetCustGeom()) {
		}else {
			return null;
		}
		return null;
	}

	public void setCustomGeometry2D(XDDFCustomGeometry2D geometry) {
		if (geometry == null) {
			if (props.isSetCustGeom()) {
				props.unsetCustGeom();
			}
		}else {
		}
	}

	public XDDFPresetGeometry2D getPresetGeometry2D() {
		if (props.isSetPrstGeom()) {
		}else {
			return null;
		}
		return null;
	}

	public void setPresetGeometry2D(XDDFPresetGeometry2D geometry) {
		if (geometry == null) {
			if (props.isSetPrstGeom()) {
				props.unsetPrstGeom();
			}
		}else {
		}
	}

	public XDDFEffectContainer getEffectContainer() {
		if (props.isSetEffectDag()) {
			return new XDDFEffectContainer(props.getEffectDag());
		}else {
			return null;
		}
	}

	public void setEffectContainer(XDDFEffectContainer container) {
		if (container == null) {
			if (props.isSetEffectDag()) {
				props.unsetEffectDag();
			}
		}else {
			props.setEffectDag(container.getXmlObject());
		}
	}

	public XDDFEffectList getEffectList() {
		if (props.isSetEffectLst()) {
			return new XDDFEffectList(props.getEffectLst());
		}else {
			return null;
		}
	}

	public void setEffectList(XDDFEffectList list) {
		if (list == null) {
			if (props.isSetEffectLst()) {
				props.unsetEffectLst();
			}
		}else {
			props.setEffectLst(list.getXmlObject());
		}
	}

	public XDDFExtensionList getExtensionList() {
		if (props.isSetExtLst()) {
			return new XDDFExtensionList(props.getExtLst());
		}else {
			return null;
		}
	}

	public void setExtensionList(XDDFExtensionList list) {
		if (list == null) {
			if (props.isSetExtLst()) {
				props.unsetExtLst();
			}
		}else {
			props.setExtLst(list.getXmlObject());
		}
	}

	public XDDFScene3D getScene3D() {
		if (props.isSetScene3D()) {
		}else {
			return null;
		}
		return null;
	}

	public void setScene3D(XDDFScene3D scene) {
		if (scene == null) {
			if (props.isSetScene3D()) {
				props.unsetScene3D();
			}
		}else {
			props.setScene3D(scene.getXmlObject());
		}
	}

	public XDDFShape3D getShape3D() {
		if (props.isSetSp3D()) {
		}else {
			return null;
		}
		return null;
	}

	public void setShape3D(XDDFShape3D shape) {
		if (shape == null) {
			if (props.isSetSp3D()) {
				props.unsetSp3D();
			}
		}else {
			props.setSp3D(shape.getXmlObject());
		}
	}

	public XDDFTransform2D getTransform2D() {
		if (props.isSetXfrm()) {
		}else {
			return null;
		}
		return null;
	}

	public void setTransform2D(XDDFTransform2D transform) {
		if (transform == null) {
			if (props.isSetXfrm()) {
				props.unsetXfrm();
			}
		}else {
		}
	}
}

