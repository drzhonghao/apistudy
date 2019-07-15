

import java.util.Collections;
import java.util.List;
import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.CompoundLine;
import org.apache.poi.xddf.usermodel.LineCap;
import org.apache.poi.xddf.usermodel.PenAlignment;
import org.apache.poi.xddf.usermodel.XDDFDashStop;
import org.apache.poi.xddf.usermodel.XDDFExtensionList;
import org.apache.poi.xddf.usermodel.XDDFFillProperties;
import org.apache.poi.xddf.usermodel.XDDFGradientFillProperties;
import org.apache.poi.xddf.usermodel.XDDFLineEndProperties;
import org.apache.poi.xddf.usermodel.XDDFLineJoinBevelProperties;
import org.apache.poi.xddf.usermodel.XDDFLineJoinMiterProperties;
import org.apache.poi.xddf.usermodel.XDDFLineJoinProperties;
import org.apache.poi.xddf.usermodel.XDDFLineJoinRoundProperties;
import org.apache.poi.xddf.usermodel.XDDFNoFillProperties;
import org.apache.poi.xddf.usermodel.XDDFPatternFillProperties;
import org.apache.poi.xddf.usermodel.XDDFPresetLineDash;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTDashStopList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGradientFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNoFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeArtExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPatternFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties.Factory.newInstance;


@Beta
public class XDDFLineProperties {
	private CTLineProperties props;

	public XDDFLineProperties() {
		this(newInstance());
	}

	@Internal
	public XDDFLineProperties(CTLineProperties properties) {
		this.props = properties;
	}

	@Internal
	public CTLineProperties getXmlObject() {
		return props;
	}

	public PenAlignment getPenAlignment() {
		if (props.isSetAlgn()) {
		}else {
			return null;
		}
		return null;
	}

	public void setPenAlignment(PenAlignment alignment) {
		if (alignment == null) {
			if (props.isSetAlgn()) {
				props.unsetAlgn();
			}
		}else {
		}
	}

	public LineCap getLineCap() {
		if (props.isSetCap()) {
		}else {
			return null;
		}
		return null;
	}

	public void setLineCap(LineCap cap) {
		if (cap == null) {
			if (props.isSetCap()) {
				props.unsetCap();
			}
		}else {
		}
	}

	public CompoundLine getCompoundLine() {
		if (props.isSetCmpd()) {
		}else {
			return null;
		}
		return null;
	}

	public void setCompoundLine(CompoundLine compound) {
		if (compound == null) {
			if (props.isSetCmpd()) {
				props.unsetCmpd();
			}
		}else {
		}
	}

	public XDDFDashStop addDashStop() {
		if (!(props.isSetCustDash())) {
			props.addNewCustDash();
		}
		return null;
	}

	public XDDFDashStop insertDashStop(int index) {
		if (!(props.isSetCustDash())) {
			props.addNewCustDash();
		}
		return null;
	}

	public void removeDashStop(int index) {
		if (props.isSetCustDash()) {
			props.getCustDash().removeDs(index);
		}
	}

	public XDDFDashStop getDashStop(int index) {
		if (props.isSetCustDash()) {
		}else {
			return null;
		}
		return null;
	}

	public List<XDDFDashStop> getDashStops() {
		if (props.isSetCustDash()) {
		}else {
			return Collections.emptyList();
		}
		return null;
	}

	public int countDashStops() {
		if (props.isSetCustDash()) {
			return props.getCustDash().sizeOfDsArray();
		}else {
			return 0;
		}
	}

	public XDDFPresetLineDash getPresetDash() {
		if (props.isSetPrstDash()) {
		}else {
			return null;
		}
		return null;
	}

	public void setPresetDash(XDDFPresetLineDash properties) {
		if (properties == null) {
			if (props.isSetPrstDash()) {
				props.unsetPrstDash();
			}
		}else {
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

	public XDDFFillProperties getFillProperties() {
		if (props.isSetGradFill()) {
		}else
			if (props.isSetNoFill()) {
			}else
				if (props.isSetPattFill()) {
				}else
					if (props.isSetSolidFill()) {
						return new XDDFSolidFillProperties(props.getSolidFill());
					}else {
						return null;
					}



		return null;
	}

	public void setFillProperties(XDDFFillProperties properties) {
		if (props.isSetGradFill()) {
			props.unsetGradFill();
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
			if (properties instanceof XDDFNoFillProperties) {
				props.setNoFill(((XDDFNoFillProperties) (properties)).getXmlObject());
			}else
				if (properties instanceof XDDFPatternFillProperties) {
					props.setPattFill(((XDDFPatternFillProperties) (properties)).getXmlObject());
				}else
					if (properties instanceof XDDFSolidFillProperties) {
						props.setSolidFill(((XDDFSolidFillProperties) (properties)).getXmlObject());
					}



	}

	public XDDFLineJoinProperties getLineJoinProperties() {
		if (props.isSetBevel()) {
		}else
			if (props.isSetMiter()) {
			}else
				if (props.isSetRound()) {
				}else {
					return null;
				}


		return null;
	}

	public void setLineJoinProperties(XDDFLineJoinProperties properties) {
		if (props.isSetBevel()) {
			props.unsetBevel();
		}
		if (props.isSetMiter()) {
			props.unsetMiter();
		}
		if (props.isSetRound()) {
			props.unsetRound();
		}
		if (properties == null) {
			return;
		}
		if (properties instanceof XDDFLineJoinBevelProperties) {
		}else
			if (properties instanceof XDDFLineJoinMiterProperties) {
			}else
				if (properties instanceof XDDFLineJoinRoundProperties) {
				}


	}

	public XDDFLineEndProperties getHeadEnd() {
		if (props.isSetHeadEnd()) {
		}else {
			return null;
		}
		return null;
	}

	public void setHeadEnd(XDDFLineEndProperties properties) {
		if (properties == null) {
			if (props.isSetHeadEnd()) {
				props.unsetHeadEnd();
			}
		}else {
		}
	}

	public XDDFLineEndProperties getTailEnd() {
		if (props.isSetTailEnd()) {
		}else {
			return null;
		}
		return null;
	}

	public void setTailEnd(XDDFLineEndProperties properties) {
		if (properties == null) {
			if (props.isSetTailEnd()) {
				props.unsetTailEnd();
			}
		}else {
		}
	}

	public Integer getWidth() {
		if (props.isSetW()) {
			return props.getW();
		}else {
			return null;
		}
	}

	public void setWidth(Integer width) {
		if (width == null) {
			if (props.isSetW()) {
				props.unsetW();
			}
		}else {
			props.setW(width);
		}
	}
}

