

import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.util.Units;
import org.apache.poi.xddf.usermodel.XDDFExtensionList;
import org.apache.poi.xddf.usermodel.text.AnchorType;
import org.apache.poi.xddf.usermodel.text.XDDFAutoFit;
import org.apache.poi.xddf.usermodel.text.XDDFNoAutoFit;
import org.apache.poi.xddf.usermodel.text.XDDFNormalAutoFit;
import org.apache.poi.xddf.usermodel.text.XDDFShapeAutoFit;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeArtExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBodyProperties;


@Beta
public class XDDFBodyProperties {
	private CTTextBodyProperties props;

	@Internal
	protected XDDFBodyProperties(CTTextBodyProperties properties) {
		this.props = properties;
	}

	@Internal
	protected CTTextBodyProperties getXmlObject() {
		return props;
	}

	public AnchorType getAnchoring() {
		if (props.isSetAnchor()) {
		}else {
			return null;
		}
		return null;
	}

	public void setAnchoring(AnchorType anchor) {
		if (anchor == null) {
			if (props.isSetAnchor()) {
				props.unsetAnchor();
			}
		}else {
		}
	}

	public Boolean isAnchorCentered() {
		if (props.isSetAnchorCtr()) {
			return props.getAnchorCtr();
		}else {
			return null;
		}
	}

	public void setAnchorCentered(Boolean centered) {
		if (centered == null) {
			if (props.isSetAnchorCtr()) {
				props.unsetAnchorCtr();
			}
		}else {
			props.setAnchorCtr(centered);
		}
	}

	public XDDFAutoFit getAutoFit() {
		if (props.isSetNoAutofit()) {
		}else
			if (props.isSetNormAutofit()) {
			}else
				if (props.isSetSpAutoFit()) {
				}


		return new XDDFNormalAutoFit();
	}

	public void setAutoFit(XDDFAutoFit autofit) {
		if (props.isSetNoAutofit()) {
			props.unsetNoAutofit();
		}
		if (props.isSetNormAutofit()) {
			props.unsetNormAutofit();
		}
		if (props.isSetSpAutoFit()) {
			props.unsetSpAutoFit();
		}
		if (autofit instanceof XDDFNoAutoFit) {
		}else
			if (autofit instanceof XDDFNormalAutoFit) {
			}else
				if (autofit instanceof XDDFShapeAutoFit) {
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

	public Double getBottomInset() {
		if (props.isSetBIns()) {
			return Units.toPoints(props.getBIns());
		}else {
			return null;
		}
	}

	public void setBottomInset(Double points) {
		if ((points == null) || (Double.isNaN(points))) {
			if (props.isSetBIns()) {
				props.unsetBIns();
			}
		}else {
			props.setBIns(Units.toEMU(points));
		}
	}

	public Double getLeftInset() {
		if (props.isSetLIns()) {
			return Units.toPoints(props.getLIns());
		}else {
			return null;
		}
	}

	public void setLeftInset(Double points) {
		if ((points == null) || (Double.isNaN(points))) {
			if (props.isSetLIns()) {
				props.unsetLIns();
			}
		}else {
			props.setLIns(Units.toEMU(points));
		}
	}

	public Double getRightInset() {
		if (props.isSetRIns()) {
			return Units.toPoints(props.getRIns());
		}else {
			return null;
		}
	}

	public void setRightInset(Double points) {
		if ((points == null) || (Double.isNaN(points))) {
			if (props.isSetRIns()) {
				props.unsetRIns();
			}
		}else {
			props.setRIns(Units.toEMU(points));
		}
	}

	public Double getTopInset() {
		if (props.isSetTIns()) {
			return Units.toPoints(props.getTIns());
		}else {
			return null;
		}
	}

	public void setTopInset(Double points) {
		if ((points == null) || (Double.isNaN(points))) {
			if (props.isSetTIns()) {
				props.unsetTIns();
			}
		}else {
			props.setTIns(Units.toEMU(points));
		}
	}

	public Boolean hasParagraphSpacing() {
		if (props.isSetSpcFirstLastPara()) {
			return props.getSpcFirstLastPara();
		}else {
			return null;
		}
	}

	public void setParagraphSpacing(Boolean spacing) {
		if (spacing == null) {
			if (props.isSetSpcFirstLastPara()) {
				props.unsetSpcFirstLastPara();
			}
		}else {
			props.setSpcFirstLastPara(spacing);
		}
	}

	public Boolean isRightToLeft() {
		if (props.isSetRtlCol()) {
			return props.getRtlCol();
		}else {
			return null;
		}
	}

	public void setRightToLeft(Boolean rightToLeft) {
		if (rightToLeft == null) {
			if (props.isSetRtlCol()) {
				props.unsetRtlCol();
			}
		}else {
			props.setRtlCol(rightToLeft);
		}
	}
}

