

import java.util.Locale;
import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.XDDFColor;
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
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.text.CapsType;
import org.apache.poi.xddf.usermodel.text.StrikeType;
import org.apache.poi.xddf.usermodel.text.UnderlineType;
import org.apache.poi.xddf.usermodel.text.XDDFFont;
import org.apache.poi.xddf.usermodel.text.XDDFHyperlink;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTEffectContainer;
import org.openxmlformats.schemas.drawingml.x2006.main.CTEffectList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGradientFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGroupFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNoFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeArtExtensionList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPatternFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties.Factory.newInstance;


@Beta
public class XDDFRunProperties {
	private CTTextCharacterProperties props;

	public XDDFRunProperties() {
		this(newInstance());
	}

	@Internal
	protected XDDFRunProperties(CTTextCharacterProperties properties) {
		this.props = properties;
	}

	@Internal
	protected CTTextCharacterProperties getXmlObject() {
		return props;
	}

	public void setBaseline(Integer value) {
		if (value == null) {
			if (props.isSetBaseline()) {
				props.unsetBaseline();
			}
		}else {
			props.setBaseline(value);
		}
	}

	public void setDirty(Boolean dirty) {
		if (dirty == null) {
			if (props.isSetDirty()) {
				props.unsetDirty();
			}
		}else {
			props.setDirty(dirty);
		}
	}

	public void setSpellError(Boolean error) {
		if (error == null) {
			if (props.isSetErr()) {
				props.unsetErr();
			}
		}else {
			props.setErr(error);
		}
	}

	public void setNoProof(Boolean noproof) {
		if (noproof == null) {
			if (props.isSetNoProof()) {
				props.unsetNoProof();
			}
		}else {
			props.setNoProof(noproof);
		}
	}

	public void setNormalizeHeights(Boolean normalize) {
		if (normalize == null) {
			if (props.isSetNormalizeH()) {
				props.unsetNormalizeH();
			}
		}else {
			props.setNormalizeH(normalize);
		}
	}

	public void setKumimoji(Boolean kumimoji) {
		if (kumimoji == null) {
			if (props.isSetKumimoji()) {
				props.unsetKumimoji();
			}
		}else {
			props.setKumimoji(kumimoji);
		}
	}

	public void setBold(Boolean bold) {
		if (bold == null) {
			if (props.isSetB()) {
				props.unsetB();
			}
		}else {
			props.setB(bold);
		}
	}

	public void setItalic(Boolean italic) {
		if (italic == null) {
			if (props.isSetI()) {
				props.unsetI();
			}
		}else {
			props.setI(italic);
		}
	}

	public void setFontSize(Double size) {
		if (size == null) {
			if (props.isSetSz()) {
				props.unsetSz();
			}
		}else
			if ((size < 1) || (400 < size)) {
				throw new IllegalArgumentException("Minimum inclusive = 1. Maximum inclusive = 400.");
			}else {
				props.setSz(((int) (100 * size)));
			}

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

	public void setCharacterKerning(Double kerning) {
		if (kerning == null) {
			if (props.isSetKern()) {
				props.unsetKern();
			}
		}else
			if ((kerning < 0) || (4000 < kerning)) {
				throw new IllegalArgumentException("Minimum inclusive = 0. Maximum inclusive = 4000.");
			}else {
				props.setKern(((int) (100 * kerning)));
			}

	}

	public void setCharacterSpacing(Double spacing) {
		if (spacing == null) {
			if (props.isSetSpc()) {
				props.unsetSpc();
			}
		}else
			if ((spacing < (-4000)) || (4000 < spacing)) {
				throw new IllegalArgumentException("Minimum inclusive = -4000. Maximum inclusive = 4000.");
			}else {
				props.setSpc(((int) (100 * spacing)));
			}

	}

	public void setFonts(XDDFFont[] fonts) {
		for (XDDFFont font : fonts) {
		}
	}

	public void setUnderline(UnderlineType underline) {
		if (underline == null) {
			if (props.isSetU()) {
				props.unsetU();
			}
		}else {
		}
	}

	public void setStrikeThrough(StrikeType strike) {
		if (strike == null) {
			if (props.isSetStrike()) {
				props.unsetStrike();
			}
		}else {
		}
	}

	public void setCapitals(CapsType caps) {
		if (caps == null) {
			if (props.isSetCap()) {
				props.unsetCap();
			}
		}else {
		}
	}

	public void setHyperlink(XDDFHyperlink link) {
		if (link == null) {
			if (props.isSetHlinkClick()) {
				props.unsetHlinkClick();
			}
		}else {
		}
	}

	public void setMouseOver(XDDFHyperlink link) {
		if (link == null) {
			if (props.isSetHlinkMouseOver()) {
				props.unsetHlinkMouseOver();
			}
		}else {
		}
	}

	public void setLanguage(Locale lang) {
		if (lang == null) {
			if (props.isSetLang()) {
				props.unsetLang();
			}
		}else {
			props.setLang(lang.toLanguageTag());
		}
	}

	public void setAlternativeLanguage(Locale lang) {
		if (lang == null) {
			if (props.isSetAltLang()) {
				props.unsetAltLang();
			}
		}else {
			props.setAltLang(lang.toLanguageTag());
		}
	}

	public void setHighlight(XDDFColor color) {
		if (color == null) {
			if (props.isSetHighlight()) {
				props.unsetHighlight();
			}
		}else {
			props.setHighlight(color.getColorContainer());
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

	public void setBookmark(String bookmark) {
		if (bookmark == null) {
			if (props.isSetBmk()) {
				props.unsetBmk();
			}
		}else {
			props.setBmk(bookmark);
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
}

