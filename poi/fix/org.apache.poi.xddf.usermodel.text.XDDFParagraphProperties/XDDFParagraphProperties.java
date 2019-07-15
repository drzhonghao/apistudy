

import java.util.Collections;
import java.util.List;
import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.util.Units;
import org.apache.poi.xddf.usermodel.XDDFExtensionList;
import org.apache.poi.xddf.usermodel.text.FontAlignment;
import org.apache.poi.xddf.usermodel.text.TextAlignment;
import org.apache.poi.xddf.usermodel.text.XDDFParagraphBulletProperties;
import org.apache.poi.xddf.usermodel.text.XDDFRunProperties;
import org.apache.poi.xddf.usermodel.text.XDDFSpacing;
import org.apache.poi.xddf.usermodel.text.XDDFTabStop;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeArtExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextTabStopList;


@Beta
public class XDDFParagraphProperties {
	private CTTextParagraphProperties props;

	private XDDFParagraphBulletProperties bullet;

	@Internal
	protected XDDFParagraphProperties(CTTextParagraphProperties properties) {
		this.props = properties;
	}

	@Internal
	protected CTTextParagraphProperties getXmlObject() {
		return props;
	}

	public XDDFParagraphBulletProperties getBulletProperties() {
		return bullet;
	}

	public int getLevel() {
		if (props.isSetLvl()) {
			return 1 + (props.getLvl());
		}else {
			return 0;
		}
	}

	public void setLevel(Integer level) {
		if (level == null) {
			if (props.isSetLvl()) {
				props.unsetLvl();
			}
		}else
			if ((level < 1) || (9 < level)) {
				throw new IllegalArgumentException("Minimum inclusive: 1. Maximum inclusive: 9.");
			}else {
				props.setLvl((level - 1));
			}

	}

	public XDDFRunProperties addDefaultRunProperties() {
		if (!(props.isSetDefRPr())) {
			props.addNewDefRPr();
		}
		return getDefaultRunProperties();
	}

	public XDDFRunProperties getDefaultRunProperties() {
		if (props.isSetDefRPr()) {
		}else {
			return null;
		}
		return null;
	}

	public void setDefaultRunProperties(XDDFRunProperties properties) {
		if (properties == null) {
			if (props.isSetDefRPr()) {
				props.unsetDefRPr();
			}
		}else {
		}
	}

	public void setEastAsianLineBreak(Boolean value) {
		if (value == null) {
			if (props.isSetEaLnBrk()) {
				props.unsetEaLnBrk();
			}
		}else {
			props.setEaLnBrk(value);
		}
	}

	public void setLatinLineBreak(Boolean value) {
		if (value == null) {
			if (props.isSetLatinLnBrk()) {
				props.unsetLatinLnBrk();
			}
		}else {
			props.setLatinLnBrk(value);
		}
	}

	public void setHangingPunctuation(Boolean value) {
		if (value == null) {
			if (props.isSetHangingPunct()) {
				props.unsetHangingPunct();
			}
		}else {
			props.setHangingPunct(value);
		}
	}

	public void setRightToLeft(Boolean value) {
		if (value == null) {
			if (props.isSetRtl()) {
				props.unsetRtl();
			}
		}else {
			props.setRtl(value);
		}
	}

	public void setFontAlignment(FontAlignment align) {
		if (align == null) {
			if (props.isSetFontAlgn()) {
				props.unsetFontAlgn();
			}
		}else {
		}
	}

	public void setTextAlignment(TextAlignment align) {
		if (align == null) {
			if (props.isSetAlgn()) {
				props.unsetAlgn();
			}
		}else {
		}
	}

	public void setDefaultTabSize(Double points) {
		if (points == null) {
			if (props.isSetDefTabSz()) {
				props.unsetDefTabSz();
			}
		}else {
			props.setDefTabSz(Units.toEMU(points));
		}
	}

	public void setIndentation(Double points) {
		if (points == null) {
			if (props.isSetIndent()) {
				props.unsetIndent();
			}
		}else
			if ((points < (-4032)) || (4032 < points)) {
				throw new IllegalArgumentException("Minimum inclusive = -4032. Maximum inclusive = 4032.");
			}else {
				props.setIndent(Units.toEMU(points));
			}

	}

	public void setMarginLeft(Double points) {
		if (points == null) {
			if (props.isSetMarL()) {
				props.unsetMarL();
			}
		}else
			if ((points < 0) || (4032 < points)) {
				throw new IllegalArgumentException("Minimum inclusive = 0. Maximum inclusive = 4032.");
			}else {
				props.setMarL(Units.toEMU(points));
			}

	}

	public void setMarginRight(Double points) {
		if (points == null) {
			if (props.isSetMarR()) {
				props.unsetMarR();
			}
		}else
			if ((points < 0) || (4032 < points)) {
				throw new IllegalArgumentException("Minimum inclusive = 0. Maximum inclusive = 4032.");
			}else {
				props.setMarR(Units.toEMU(points));
			}

	}

	public void setLineSpacing(XDDFSpacing spacing) {
		if (spacing == null) {
			if (props.isSetLnSpc()) {
				props.unsetLnSpc();
			}
		}else {
		}
	}

	public void setSpaceAfter(XDDFSpacing spacing) {
		if (spacing == null) {
			if (props.isSetSpcAft()) {
				props.unsetSpcAft();
			}
		}else {
		}
	}

	public void setSpaceBefore(XDDFSpacing spacing) {
		if (spacing == null) {
			if (props.isSetSpcBef()) {
				props.unsetSpcBef();
			}
		}else {
		}
	}

	public XDDFTabStop addTabStop() {
		if (!(props.isSetTabLst())) {
			props.addNewTabLst();
		}
		return null;
	}

	public XDDFTabStop insertTabStop(int index) {
		if (!(props.isSetTabLst())) {
			props.addNewTabLst();
		}
		return null;
	}

	public void removeTabStop(int index) {
		if (props.isSetTabLst()) {
			props.getTabLst().removeTab(index);
		}
	}

	public XDDFTabStop getTabStop(int index) {
		if (props.isSetTabLst()) {
		}else {
			return null;
		}
		return null;
	}

	public List<XDDFTabStop> getTabStops() {
		if (props.isSetTabLst()) {
		}else {
			return Collections.emptyList();
		}
		return null;
	}

	public int countTabStops() {
		if (props.isSetTabLst()) {
			return props.getTabLst().sizeOfTabArray();
		}else {
			return 0;
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
}

