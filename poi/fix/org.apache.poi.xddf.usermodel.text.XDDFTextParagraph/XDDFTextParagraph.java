

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.collections4.iterators.IteratorIterable;
import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.poi.util.Beta;
import org.apache.poi.util.Internal;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.util.Units;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.text.FontAlignment;
import org.apache.poi.xddf.usermodel.text.TextAlignment;
import org.apache.poi.xddf.usermodel.text.XDDFAutoFit;
import org.apache.poi.xddf.usermodel.text.XDDFBodyProperties;
import org.apache.poi.xddf.usermodel.text.XDDFBulletSize;
import org.apache.poi.xddf.usermodel.text.XDDFBulletStyle;
import org.apache.poi.xddf.usermodel.text.XDDFFont;
import org.apache.poi.xddf.usermodel.text.XDDFParagraphBulletProperties;
import org.apache.poi.xddf.usermodel.text.XDDFParagraphProperties;
import org.apache.poi.xddf.usermodel.text.XDDFRunProperties;
import org.apache.poi.xddf.usermodel.text.XDDFSpacing;
import org.apache.poi.xddf.usermodel.text.XDDFTabStop;
import org.apache.poi.xddf.usermodel.text.XDDFTextBody;
import org.apache.poi.xddf.usermodel.text.XDDFTextRun;
import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextField;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextLineBreak;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextSpacing;


@Beta
public class XDDFTextParagraph {
	private XDDFTextBody _parent;

	private XDDFParagraphProperties _properties;

	private final CTTextParagraph _p;

	private final ArrayList<XDDFTextRun> _runs;

	@Internal
	protected XDDFTextParagraph(CTTextParagraph paragraph, XDDFTextBody parent) {
		this._p = paragraph;
		this._parent = parent;
		final int count = ((paragraph.sizeOfBrArray()) + (paragraph.sizeOfFldArray())) + (paragraph.sizeOfRArray());
		this._runs = new ArrayList<>(count);
		for (XmlObject xo : _p.selectChildren(QNameSet.ALL)) {
			if (xo instanceof CTTextLineBreak) {
			}else
				if (xo instanceof CTTextField) {
				}else
					if (xo instanceof CTRegularTextRun) {
					}


		}
		addDefaultRunProperties();
		addAfterLastRunProperties();
	}

	public void setText(String text) {
		for (int i = (_p.sizeOfBrArray()) - 1; i >= 0; i--) {
			_p.removeBr(i);
		}
		for (int i = (_p.sizeOfFldArray()) - 1; i >= 0; i--) {
			_p.removeFld(i);
		}
		for (int i = (_p.sizeOfRArray()) - 1; i >= 0; i--) {
			_p.removeR(i);
		}
		_runs.clear();
		appendRegularRun(text);
	}

	public String getText() {
		StringBuilder out = new StringBuilder();
		for (XDDFTextRun r : _runs) {
			out.append(r.getText());
		}
		return out.toString();
	}

	public XDDFTextBody getParentBody() {
		return _parent;
	}

	public List<XDDFTextRun> getTextRuns() {
		return _runs;
	}

	public Iterator<XDDFTextRun> iterator() {
		return _runs.iterator();
	}

	public XDDFTextRun appendLineBreak() {
		CTTextLineBreak br = _p.addNewBr();
		for (XDDFTextRun tr : new IteratorIterable<>(new ReverseListIterator<>(_runs))) {
		}
		return null;
	}

	public XDDFTextRun appendField(String id, String type, String text) {
		CTTextField f = _p.addNewFld();
		f.setId(id);
		f.setType(type);
		f.setT(text);
		CTTextCharacterProperties rPr = f.addNewRPr();
		rPr.setLang(LocaleUtil.getUserLocale().toLanguageTag());
		return null;
	}

	public XDDFTextRun appendRegularRun(String text) {
		CTRegularTextRun r = _p.addNewR();
		r.setT(text);
		CTTextCharacterProperties rPr = r.addNewRPr();
		rPr.setLang(LocaleUtil.getUserLocale().toLanguageTag());
		return null;
	}

	public TextAlignment getTextAlignment() {
		return null;
	}

	public void setTextAlignment(TextAlignment align) {
		if ((align != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setTextAlignment(align);
		}
	}

	public FontAlignment getFontAlignment() {
		return null;
	}

	public void setFontAlignment(FontAlignment align) {
		if ((align != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setFontAlignment(align);
		}
	}

	public Double getIndentation() {
		return findDefinedParagraphProperty(( props) -> props.isSetIndent(), ( props) -> props.getIndent()).map(( emu) -> Units.toPoints(emu)).orElse(null);
	}

	public void setIndentation(Double points) {
		if ((points != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setIndentation(points);
		}
	}

	public Double getMarginLeft() {
		return findDefinedParagraphProperty(( props) -> props.isSetMarL(), ( props) -> props.getMarL()).map(( emu) -> Units.toPoints(emu)).orElse(null);
	}

	public void setMarginLeft(Double points) {
		if ((points != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setMarginLeft(points);
		}
	}

	public Double getMarginRight() {
		return findDefinedParagraphProperty(( props) -> props.isSetMarR(), ( props) -> props.getMarR()).map(( emu) -> Units.toPoints(emu)).orElse(null);
	}

	public void setMarginRight(Double points) {
		if ((points != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setMarginRight(points);
		}
	}

	public Double getDefaultTabSize() {
		return findDefinedParagraphProperty(( props) -> props.isSetDefTabSz(), ( props) -> props.getDefTabSz()).map(( emu) -> Units.toPoints(emu)).orElse(null);
	}

	public void setDefaultTabSize(Double points) {
		if ((points != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setDefaultTabSize(points);
		}
	}

	public XDDFSpacing getLineSpacing() {
		return findDefinedParagraphProperty(( props) -> props.isSetLnSpc(), ( props) -> props.getLnSpc()).map(( spacing) -> extractSpacing(spacing)).orElse(null);
	}

	public void setLineSpacing(XDDFSpacing linespacing) {
		if ((linespacing != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setLineSpacing(linespacing);
		}
	}

	public XDDFSpacing getSpaceBefore() {
		return findDefinedParagraphProperty(( props) -> props.isSetSpcBef(), ( props) -> props.getSpcBef()).map(( spacing) -> extractSpacing(spacing)).orElse(null);
	}

	public void setSpaceBefore(XDDFSpacing spaceBefore) {
		if ((spaceBefore != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setSpaceBefore(spaceBefore);
		}
	}

	public XDDFSpacing getSpaceAfter() {
		return findDefinedParagraphProperty(( props) -> props.isSetSpcAft(), ( props) -> props.getSpcAft()).map(( spacing) -> extractSpacing(spacing)).orElse(null);
	}

	public void setSpaceAfter(XDDFSpacing spaceAfter) {
		if ((spaceAfter != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setSpaceAfter(spaceAfter);
		}
	}

	public XDDFColor getBulletColor() {
		return null;
	}

	public void setBulletColor(XDDFColor color) {
		if ((color != null) || (_p.isSetPPr())) {
			getOrCreateBulletProperties().setBulletColor(color);
		}
	}

	public void setBulletColorFollowText() {
		getOrCreateBulletProperties().setBulletColorFollowText();
	}

	public XDDFFont getBulletFont() {
		return null;
	}

	public void setBulletFont(XDDFFont font) {
		if ((font != null) || (_p.isSetPPr())) {
			getOrCreateBulletProperties().setBulletFont(font);
		}
	}

	public void setBulletFontFollowText() {
		getOrCreateBulletProperties().setBulletFontFollowText();
	}

	public XDDFBulletSize getBulletSize() {
		return null;
	}

	public void setBulletSize(XDDFBulletSize size) {
		if ((size != null) || (_p.isSetPPr())) {
			getOrCreateBulletProperties().setBulletSize(size);
		}
	}

	public XDDFBulletStyle getBulletStyle() {
		return null;
	}

	public void setBulletStyle(XDDFBulletStyle style) {
		if ((style != null) || (_p.isSetPPr())) {
			getOrCreateBulletProperties().setBulletStyle(style);
		}
	}

	public boolean hasEastAsianLineBreak() {
		return findDefinedParagraphProperty(( props) -> props.isSetEaLnBrk(), ( props) -> props.getEaLnBrk()).orElse(false);
	}

	public void setEastAsianLineBreak(Boolean value) {
		if ((value != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setEastAsianLineBreak(value);
		}
	}

	public boolean hasLatinLineBreak() {
		return findDefinedParagraphProperty(( props) -> props.isSetLatinLnBrk(), ( props) -> props.getLatinLnBrk()).orElse(false);
	}

	public void setLatinLineBreak(Boolean value) {
		if ((value != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setLatinLineBreak(value);
		}
	}

	public boolean hasHangingPunctuation() {
		return findDefinedParagraphProperty(( props) -> props.isSetHangingPunct(), ( props) -> props.getHangingPunct()).orElse(false);
	}

	public void setHangingPunctuation(Boolean value) {
		if ((value != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setHangingPunctuation(value);
		}
	}

	public boolean isRightToLeft() {
		return findDefinedParagraphProperty(( props) -> props.isSetRtl(), ( props) -> props.getRtl()).orElse(false);
	}

	public void setRightToLeft(Boolean value) {
		if ((value != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setRightToLeft(value);
		}
	}

	public XDDFTabStop addTabStop() {
		return getOrCreateProperties().addTabStop();
	}

	public XDDFTabStop insertTabStop(int index) {
		return getOrCreateProperties().insertTabStop(index);
	}

	public void removeTabStop(int index) {
		if (_p.isSetPPr()) {
			getProperties().removeTabStop(index);
		}
	}

	public XDDFTabStop getTabStop(int index) {
		if (_p.isSetPPr()) {
			return getProperties().getTabStop(index);
		}else {
			return null;
		}
	}

	public List<XDDFTabStop> getTabStops() {
		if (_p.isSetPPr()) {
			return getProperties().getTabStops();
		}else {
			return Collections.emptyList();
		}
	}

	public int countTabStops() {
		if (_p.isSetPPr()) {
			return getProperties().countTabStops();
		}else {
			return 0;
		}
	}

	public XDDFParagraphBulletProperties getOrCreateBulletProperties() {
		return getOrCreateProperties().getBulletProperties();
	}

	public XDDFParagraphBulletProperties getBulletProperties() {
		if (_p.isSetPPr()) {
			return getProperties().getBulletProperties();
		}else {
			return null;
		}
	}

	public XDDFRunProperties addDefaultRunProperties() {
		return getOrCreateProperties().addDefaultRunProperties();
	}

	public XDDFRunProperties getDefaultRunProperties() {
		if (_p.isSetPPr()) {
			return getProperties().getDefaultRunProperties();
		}else {
			return null;
		}
	}

	public void setDefaultRunProperties(XDDFRunProperties properties) {
		if ((properties != null) || (_p.isSetPPr())) {
			getOrCreateProperties().setDefaultRunProperties(properties);
		}
	}

	public XDDFRunProperties addAfterLastRunProperties() {
		if (!(_p.isSetEndParaRPr())) {
			_p.addNewEndParaRPr();
		}
		return getAfterLastRunProperties();
	}

	public XDDFRunProperties getAfterLastRunProperties() {
		if (_p.isSetEndParaRPr()) {
		}else {
			return null;
		}
		return null;
	}

	public void setAfterLastRunProperties(XDDFRunProperties properties) {
		if (properties == null) {
			if (_p.isSetEndParaRPr()) {
				_p.unsetEndParaRPr();
			}
		}else {
		}
	}

	private XDDFSpacing extractSpacing(CTTextSpacing spacing) {
		if (spacing.isSetSpcPct()) {
			double scale = 1 - ((_parent.getBodyProperties().getAutoFit().getLineSpaceReduction()) / 100000.0);
		}else
			if (spacing.isSetSpcPts()) {
			}

		return null;
	}

	private XDDFParagraphProperties getProperties() {
		if ((_properties) == null) {
		}
		return _properties;
	}

	private XDDFParagraphProperties getOrCreateProperties() {
		if (!(_p.isSetPPr())) {
		}
		return getProperties();
	}

	protected <R> Optional<R> findDefinedParagraphProperty(Function<CTTextParagraphProperties, Boolean> isSet, Function<CTTextParagraphProperties, R> getter) {
		if (_p.isSetPPr()) {
			int level = (_p.getPPr().isSetLvl()) ? 1 + (_p.getPPr().getLvl()) : 0;
			return findDefinedParagraphProperty(isSet, getter, level);
		}else {
		}
		return null;
	}

	private <R> Optional<R> findDefinedParagraphProperty(Function<CTTextParagraphProperties, Boolean> isSet, Function<CTTextParagraphProperties, R> getter, int level) {
		final CTTextParagraphProperties props = _p.getPPr();
		if ((props != null) && (isSet.apply(props))) {
			return Optional.ofNullable(getter.apply(props));
		}else {
		}
		return null;
	}

	protected <R> Optional<R> findDefinedRunProperty(Function<CTTextCharacterProperties, Boolean> isSet, Function<CTTextCharacterProperties, R> getter) {
		if (_p.isSetPPr()) {
			int level = (_p.getPPr().isSetLvl()) ? 1 + (_p.getPPr().getLvl()) : 0;
			return findDefinedRunProperty(isSet, getter, level);
		}else {
		}
		return null;
	}

	private <R> Optional<R> findDefinedRunProperty(Function<CTTextCharacterProperties, Boolean> isSet, Function<CTTextCharacterProperties, R> getter, int level) {
		final CTTextCharacterProperties props = (_p.getPPr().isSetDefRPr()) ? _p.getPPr().getDefRPr() : null;
		if ((props != null) && (isSet.apply(props))) {
			return Optional.ofNullable(getter.apply(props));
		}else {
		}
		return null;
	}
}

