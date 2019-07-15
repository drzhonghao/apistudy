

import java.util.LinkedList;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import org.apache.poi.ooxml.POIXMLRelation;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFFillProperties;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.text.CapsType;
import org.apache.poi.xddf.usermodel.text.StrikeType;
import org.apache.poi.xddf.usermodel.text.UnderlineType;
import org.apache.poi.xddf.usermodel.text.XDDFAutoFit;
import org.apache.poi.xddf.usermodel.text.XDDFBodyProperties;
import org.apache.poi.xddf.usermodel.text.XDDFFont;
import org.apache.poi.xddf.usermodel.text.XDDFHyperlink;
import org.apache.poi.xddf.usermodel.text.XDDFRunProperties;
import org.apache.poi.xddf.usermodel.text.XDDFTextBody;
import org.apache.poi.xddf.usermodel.text.XDDFTextParagraph;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextField;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextLineBreak;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextCapsType;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextStrikeType;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextUnderlineType;


@Beta
public class XDDFTextRun {
	private XDDFTextParagraph _parent;

	private XDDFRunProperties _properties;

	private CTTextLineBreak _tlb;

	private CTTextField _tf;

	private CTRegularTextRun _rtr;

	@Internal
	protected XDDFTextRun(CTTextLineBreak run, XDDFTextParagraph parent) {
		this._tlb = run;
		this._parent = parent;
	}

	@Internal
	protected XDDFTextRun(CTTextField run, XDDFTextParagraph parent) {
		this._tf = run;
		this._parent = parent;
	}

	@Internal
	protected XDDFTextRun(CTRegularTextRun run, XDDFTextParagraph parent) {
		this._rtr = run;
		this._parent = parent;
	}

	public XDDFTextParagraph getParentParagraph() {
		return _parent;
	}

	public boolean isLineBreak() {
		return (_tlb) != null;
	}

	public boolean isField() {
		return (_tf) != null;
	}

	public boolean isRegularRun() {
		return (_rtr) != null;
	}

	public String getText() {
		if (isLineBreak()) {
			return "\n";
		}else
			if (isField()) {
				return _tf.getT();
			}else {
				return _rtr.getT();
			}

	}

	public void setText(String text) {
		if (isField()) {
			_tf.setT(text);
		}else
			if (isRegularRun()) {
				_rtr.setT(text);
			}

	}

	public void setDirty(Boolean dirty) {
		getOrCreateProperties().setDirty(dirty);
	}

	public Boolean getDirty() {
		return findDefinedProperty(( props) -> props.isSetDirty(), ( props) -> props.getDirty()).orElse(null);
	}

	public void setSpellError(Boolean error) {
		getOrCreateProperties().setSpellError(error);
	}

	public Boolean getSpellError() {
		return findDefinedProperty(( props) -> props.isSetErr(), ( props) -> props.getErr()).orElse(null);
	}

	public void setNoProof(Boolean noproof) {
		getOrCreateProperties().setNoProof(noproof);
	}

	public Boolean getNoProof() {
		return findDefinedProperty(( props) -> props.isSetNoProof(), ( props) -> props.getNoProof()).orElse(null);
	}

	public void setNormalizeHeights(Boolean normalize) {
		getOrCreateProperties().setNormalizeHeights(normalize);
	}

	public Boolean getNormalizeHeights() {
		return findDefinedProperty(( props) -> props.isSetNormalizeH(), ( props) -> props.getNormalizeH()).orElse(null);
	}

	public void setKumimoji(Boolean kumimoji) {
		getOrCreateProperties().setKumimoji(kumimoji);
	}

	public boolean isKumimoji() {
		return findDefinedProperty(( props) -> props.isSetKumimoji(), ( props) -> props.getKumimoji()).orElse(false);
	}

	public void setBold(Boolean bold) {
		getOrCreateProperties().setBold(bold);
	}

	public boolean isBold() {
		return findDefinedProperty(( props) -> props.isSetB(), ( props) -> props.getB()).orElse(false);
	}

	public void setItalic(Boolean italic) {
		getOrCreateProperties().setItalic(italic);
	}

	public boolean isItalic() {
		return findDefinedProperty(( props) -> props.isSetI(), ( props) -> props.getI()).orElse(false);
	}

	public void setStrikeThrough(StrikeType strike) {
		getOrCreateProperties().setStrikeThrough(strike);
	}

	public boolean isStrikeThrough() {
		return findDefinedProperty(( props) -> props.isSetStrike(), ( props) -> props.getStrike()).map(( strike) -> strike != (STTextStrikeType.NO_STRIKE)).orElse(false);
	}

	public StrikeType getStrikeThrough() {
		return null;
	}

	public void setUnderline(UnderlineType underline) {
		getOrCreateProperties().setUnderline(underline);
	}

	public boolean isUnderline() {
		return findDefinedProperty(( props) -> props.isSetU(), ( props) -> props.getU()).map(( underline) -> underline != (STTextUnderlineType.NONE)).orElse(false);
	}

	public UnderlineType getUnderline() {
		return null;
	}

	public void setCapitals(CapsType caps) {
		getOrCreateProperties().setCapitals(caps);
	}

	public boolean isCapitals() {
		return findDefinedProperty(( props) -> props.isSetCap(), ( props) -> props.getCap()).map(( caps) -> caps != (STTextCapsType.NONE)).orElse(false);
	}

	public CapsType getCapitals() {
		return null;
	}

	public boolean isSubscript() {
		return findDefinedProperty(( props) -> props.isSetBaseline(), ( props) -> props.getBaseline()).map(( baseline) -> baseline < 0).orElse(false);
	}

	public boolean isSuperscript() {
		return findDefinedProperty(( props) -> props.isSetBaseline(), ( props) -> props.getBaseline()).map(( baseline) -> baseline > 0).orElse(false);
	}

	public void setBaseline(Double offset) {
		if (offset == null) {
			getOrCreateProperties().setBaseline(null);
		}else {
			getOrCreateProperties().setBaseline(((int) (offset * 1000)));
		}
	}

	public void setSuperscript(Double offset) {
		setBaseline((offset == null ? null : Math.abs(offset)));
	}

	public void setSubscript(Double offset) {
		setBaseline((offset == null ? null : -(Math.abs(offset))));
	}

	public void setFillProperties(XDDFFillProperties properties) {
		getOrCreateProperties().setFillProperties(properties);
	}

	public void setFontColor(XDDFColor color) {
		XDDFSolidFillProperties props = new XDDFSolidFillProperties();
		props.setColor(color);
		setFillProperties(props);
	}

	public XDDFColor getFontColor() {
		XDDFSolidFillProperties solid = findDefinedProperty(( props) -> props.isSetSolidFill(), ( props) -> props.getSolidFill()).map(( props) -> new XDDFSolidFillProperties(props)).orElse(new XDDFSolidFillProperties());
		return solid.getColor();
	}

	public void setFonts(XDDFFont[] fonts) {
		getOrCreateProperties().setFonts(fonts);
	}

	public XDDFFont[] getFonts() {
		LinkedList<XDDFFont> list = new LinkedList<>();
		return list.toArray(new XDDFFont[list.size()]);
	}

	public void setFontSize(Double size) {
		getOrCreateProperties().setFontSize(size);
	}

	public Double getFontSize() {
		Integer size = findDefinedProperty(( props) -> props.isSetSz(), ( props) -> props.getSz()).orElse((100 * (XSSFFont.DEFAULT_FONT_SIZE)));
		double scale = (_parent.getParentBody().getBodyProperties().getAutoFit().getFontScale()) / 1.0E7;
		return size * scale;
	}

	public void setCharacterKerning(Double kerning) {
		getOrCreateProperties().setCharacterKerning(kerning);
	}

	public Double getCharacterKerning() {
		return findDefinedProperty(( props) -> props.isSetKern(), ( props) -> props.getKern()).map(( kerning) -> 0.01 * kerning).orElse(null);
	}

	public void setCharacterSpacing(Double spacing) {
		getOrCreateProperties().setCharacterSpacing(spacing);
	}

	public Double getCharacterSpacing() {
		return findDefinedProperty(( props) -> props.isSetSpc(), ( props) -> props.getSpc()).map(( spacing) -> 0.01 * spacing).orElse(null);
	}

	public void setBookmark(String bookmark) {
		getOrCreateProperties().setBookmark(bookmark);
	}

	public String getBookmark() {
		return findDefinedProperty(( props) -> props.isSetBmk(), ( props) -> props.getBmk()).orElse(null);
	}

	public XDDFHyperlink linkToExternal(String url, PackagePart localPart, POIXMLRelation relation) {
		PackageRelationship rel = localPart.addExternalRelationship(url, relation.getRelation());
		XDDFHyperlink link = new XDDFHyperlink(rel.getId());
		getOrCreateProperties().setHyperlink(link);
		return link;
	}

	public XDDFHyperlink linkToAction(String action) {
		XDDFHyperlink link = new XDDFHyperlink("", action);
		getOrCreateProperties().setHyperlink(link);
		return link;
	}

	public XDDFHyperlink linkToInternal(String action, PackagePart localPart, POIXMLRelation relation, PackagePartName target) {
		PackageRelationship rel = localPart.addRelationship(target, TargetMode.INTERNAL, relation.getRelation());
		XDDFHyperlink link = new XDDFHyperlink(rel.getId(), action);
		getOrCreateProperties().setHyperlink(link);
		return link;
	}

	public XDDFHyperlink getHyperlink() {
		return null;
	}

	public XDDFHyperlink createMouseOver(String action) {
		XDDFHyperlink link = new XDDFHyperlink("", action);
		getOrCreateProperties().setMouseOver(link);
		return link;
	}

	public XDDFHyperlink getMouseOver() {
		return null;
	}

	public void setLanguage(Locale lang) {
		getOrCreateProperties().setLanguage(lang);
	}

	public Locale getLanguage() {
		return findDefinedProperty(( props) -> props.isSetLang(), ( props) -> props.getLang()).map(( lang) -> Locale.forLanguageTag(lang)).orElse(null);
	}

	public void setAlternativeLanguage(Locale lang) {
		getOrCreateProperties().setAlternativeLanguage(lang);
	}

	public Locale getAlternativeLanguage() {
		return findDefinedProperty(( props) -> props.isSetAltLang(), ( props) -> props.getAltLang()).map(( lang) -> Locale.forLanguageTag(lang)).orElse(null);
	}

	public void setHighlight(XDDFColor color) {
		getOrCreateProperties().setHighlight(color);
	}

	public XDDFColor getHighlight() {
		return findDefinedProperty(( props) -> props.isSetHighlight(), ( props) -> props.getHighlight()).map(( color) -> XDDFColor.forColorContainer(color)).orElse(null);
	}

	public void setLineProperties(XDDFLineProperties properties) {
		getOrCreateProperties().setLineProperties(properties);
	}

	public XDDFLineProperties getLineProperties() {
		return findDefinedProperty(( props) -> props.isSetLn(), ( props) -> props.getLn()).map(( props) -> new XDDFLineProperties(props)).orElse(null);
	}

	private <R> Optional<R> findDefinedProperty(Function<CTTextCharacterProperties, Boolean> isSet, Function<CTTextCharacterProperties, R> getter) {
		CTTextCharacterProperties props = getProperties();
		if ((props != null) && (isSet.apply(props))) {
			return Optional.ofNullable(getter.apply(props));
		}else {
		}
		return null;
	}

	@Internal
	protected CTTextCharacterProperties getProperties() {
		if ((isLineBreak()) && (_tlb.isSetRPr())) {
			return _tlb.getRPr();
		}else
			if ((isField()) && (_tf.isSetRPr())) {
				return _tf.getRPr();
			}else
				if ((isRegularRun()) && (_rtr.isSetRPr())) {
					return _rtr.getRPr();
				}


		return null;
	}

	private XDDFRunProperties getOrCreateProperties() {
		if ((_properties) == null) {
			if (isLineBreak()) {
			}else
				if (isField()) {
				}else
					if (isRegularRun()) {
					}


		}
		return _properties;
	}
}

