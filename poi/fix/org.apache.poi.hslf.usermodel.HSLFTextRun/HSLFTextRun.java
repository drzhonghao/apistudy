

import java.awt.Color;
import java.util.List;
import org.apache.poi.common.usermodel.fonts.FontGroup;
import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hslf.model.textproperties.BitMaskTextProp;
import org.apache.poi.hslf.model.textproperties.CharFlagsTextProp;
import org.apache.poi.hslf.model.textproperties.TextProp;
import org.apache.poi.hslf.model.textproperties.TextPropCollection;
import org.apache.poi.hslf.usermodel.HSLFFontInfo;
import org.apache.poi.hslf.usermodel.HSLFHyperlink;
import org.apache.poi.hslf.usermodel.HSLFMasterSheet;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSheet;
import org.apache.poi.hslf.usermodel.HSLFSimpleShape;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.sl.draw.DrawPaint;
import org.apache.poi.sl.usermodel.ColorStyle;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextRun;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

import static org.apache.poi.hslf.model.textproperties.TextPropCollection.TextPropType.character;
import static org.apache.poi.sl.usermodel.TextRun.FieldType.DATE_TIME;
import static org.apache.poi.sl.usermodel.TextRun.FieldType.SLIDE_NUMBER;
import static org.apache.poi.sl.usermodel.TextRun.TextCap.NONE;


public final class HSLFTextRun implements TextRun {
	protected POILogger logger = POILogFactory.getLogger(this.getClass());

	private HSLFTextParagraph parentParagraph;

	private String _runText = "";

	private HSLFFontInfo[] cachedFontInfo;

	private HSLFHyperlink link;

	private TextPropCollection characterStyle = new TextPropCollection(1, character);

	public HSLFTextRun(HSLFTextParagraph parentParagraph) {
		this.parentParagraph = parentParagraph;
	}

	public TextPropCollection getCharacterStyle() {
		return characterStyle;
	}

	public void setCharacterStyle(TextPropCollection characterStyle) {
		this.characterStyle.copy(characterStyle);
		this.characterStyle.updateTextSize(_runText.length());
	}

	public void updateSheet() {
		if ((cachedFontInfo) != null) {
			for (FontGroup tt : FontGroup.values()) {
				setFontInfo(cachedFontInfo[tt.ordinal()], tt);
			}
			cachedFontInfo = null;
		}
	}

	public int getLength() {
		return _runText.length();
	}

	@Override
	public String getRawText() {
		return _runText;
	}

	@Override
	public void setText(String text) {
		if (text == null) {
			throw new HSLFException("text must not be null");
		}
	}

	private boolean isCharFlagsTextPropVal(int index) {
		return getFlag(index);
	}

	protected boolean getFlag(int index) {
		BitMaskTextProp prop = ((characterStyle) == null) ? null : characterStyle.findByName(CharFlagsTextProp.NAME);
		if ((prop == null) || (!(prop.getSubPropMatches()[index]))) {
			prop = getMasterProp(CharFlagsTextProp.NAME);
		}
		return prop == null ? false : prop.getSubValue(index);
	}

	private <T extends TextProp> T getMasterProp(final String name) {
		final int txtype = parentParagraph.getRunType();
		final HSLFSheet sheet = parentParagraph.getSheet();
		if (sheet == null) {
			logger.log(POILogger.ERROR, "Sheet is not available");
			return null;
		}
		final HSLFMasterSheet master = sheet.getMasterSheet();
		if (master == null) {
			logger.log(POILogger.WARN, "MasterSheet is not available");
			return null;
		}
		final TextPropCollection col = master.getPropCollection(txtype, parentParagraph.getIndentLevel(), name, true);
		return col == null ? null : col.findByName(name);
	}

	private void setCharFlagsTextPropVal(int index, boolean value) {
		if ((getFlag(index)) != value) {
			setFlag(index, value);
			parentParagraph.setDirty();
		}
	}

	public void setCharTextPropVal(String propName, Integer val) {
	}

	@Override
	public boolean isBold() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.BOLD_IDX);
	}

	@Override
	public void setBold(boolean bold) {
		setCharFlagsTextPropVal(CharFlagsTextProp.BOLD_IDX, bold);
	}

	@Override
	public boolean isItalic() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.ITALIC_IDX);
	}

	@Override
	public void setItalic(boolean italic) {
		setCharFlagsTextPropVal(CharFlagsTextProp.ITALIC_IDX, italic);
	}

	@Override
	public boolean isUnderlined() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.UNDERLINE_IDX);
	}

	@Override
	public void setUnderlined(boolean underlined) {
		setCharFlagsTextPropVal(CharFlagsTextProp.UNDERLINE_IDX, underlined);
	}

	public boolean isShadowed() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.SHADOW_IDX);
	}

	public void setShadowed(boolean flag) {
		setCharFlagsTextPropVal(CharFlagsTextProp.SHADOW_IDX, flag);
	}

	public boolean isEmbossed() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.RELIEF_IDX);
	}

	public void setEmbossed(boolean flag) {
		setCharFlagsTextPropVal(CharFlagsTextProp.RELIEF_IDX, flag);
	}

	@Override
	public boolean isStrikethrough() {
		return isCharFlagsTextPropVal(CharFlagsTextProp.STRIKETHROUGH_IDX);
	}

	@Override
	public void setStrikethrough(boolean flag) {
		setCharFlagsTextPropVal(CharFlagsTextProp.STRIKETHROUGH_IDX, flag);
	}

	public int getSuperscript() {
		return 0;
	}

	public void setSuperscript(int val) {
		setCharTextPropVal("superscript", val);
	}

	@Override
	public Double getFontSize() {
		return 0.0;
	}

	@Override
	public void setFontSize(Double fontSize) {
		Integer iFontSize = (fontSize == null) ? null : fontSize.intValue();
		setCharTextPropVal("font.size", iFontSize);
	}

	public int getFontIndex() {
		return 0;
	}

	public void setFontIndex(int idx) {
		setCharTextPropVal("font.index", idx);
	}

	@Override
	public void setFontFamily(String typeface) {
		setFontInfo(new HSLFFontInfo(typeface), FontGroup.LATIN);
	}

	@Override
	public void setFontFamily(String typeface, FontGroup fontGroup) {
		setFontInfo(new HSLFFontInfo(typeface), fontGroup);
	}

	@Override
	public void setFontInfo(FontInfo fontInfo, FontGroup fontGroup) {
		FontGroup fg = safeFontGroup(fontGroup);
		HSLFSheet sheet = parentParagraph.getSheet();
		@SuppressWarnings("resource")
		HSLFSlideShow slideShow = (sheet == null) ? null : sheet.getSlideShow();
		if ((sheet == null) || (slideShow == null)) {
			if ((cachedFontInfo) == null) {
				cachedFontInfo = new HSLFFontInfo[FontGroup.values().length];
			}
			cachedFontInfo[fg.ordinal()] = (fontInfo != null) ? new HSLFFontInfo(fontInfo) : null;
			return;
		}
		String propName;
		switch (fg) {
			default :
			case LATIN :
				propName = "font.index";
				break;
			case COMPLEX_SCRIPT :
			case EAST_ASIAN :
				propName = "asian.font.index";
				break;
			case SYMBOL :
				propName = "symbol.font.index";
				break;
		}
		Integer fontIdx = null;
		if (fontInfo != null) {
			fontIdx = slideShow.addFont(fontInfo).getIndex();
		}
		setCharTextPropVal(propName, fontIdx);
	}

	@Override
	public String getFontFamily() {
		return getFontFamily(null);
	}

	@Override
	public String getFontFamily(FontGroup fontGroup) {
		HSLFFontInfo fi = getFontInfo(fontGroup);
		return fi != null ? fi.getTypeface() : null;
	}

	@Override
	public HSLFFontInfo getFontInfo(final FontGroup fontGroup) {
		FontGroup fg = safeFontGroup(fontGroup);
		HSLFSheet sheet = parentParagraph.getSheet();
		@SuppressWarnings("resource")
		HSLFSlideShow slideShow = (sheet == null) ? null : sheet.getSlideShow();
		if ((sheet == null) || (slideShow == null)) {
			return (cachedFontInfo) != null ? cachedFontInfo[fg.ordinal()] : null;
		}
		String propName;
		switch (fg) {
			default :
			case LATIN :
				propName = "font.index,ansi.font.index";
				break;
			case COMPLEX_SCRIPT :
			case EAST_ASIAN :
				propName = "asian.font.index";
				break;
			case SYMBOL :
				propName = "symbol.font.index";
				break;
		}
		return null;
	}

	@Override
	public PaintStyle.SolidPaint getFontColor() {
		return null;
	}

	public void setFontColor(int bgr) {
		setCharTextPropVal("font.color", bgr);
	}

	@Override
	public void setFontColor(Color color) {
		setFontColor(DrawPaint.createSolidPaint(color));
	}

	@Override
	public void setFontColor(PaintStyle color) {
		if (!(color instanceof PaintStyle.SolidPaint)) {
			throw new IllegalArgumentException("HSLF only supports solid paint");
		}
		PaintStyle.SolidPaint sp = ((PaintStyle.SolidPaint) (color));
		Color c = DrawPaint.applyColorTransform(sp.getSolidColor());
		int rgb = new Color(c.getBlue(), c.getGreen(), c.getRed(), 254).getRGB();
		setFontColor(rgb);
	}

	protected void setFlag(int index, boolean value) {
		BitMaskTextProp prop = ((BitMaskTextProp) (characterStyle.addWithName(CharFlagsTextProp.NAME)));
		prop.setSubValue(value, index);
	}

	public HSLFTextParagraph getTextParagraph() {
		return parentParagraph;
	}

	@Override
	public TextRun.TextCap getTextCap() {
		return NONE;
	}

	@Override
	public boolean isSubscript() {
		return (getSuperscript()) < 0;
	}

	@Override
	public boolean isSuperscript() {
		return (getSuperscript()) > 0;
	}

	@Override
	public byte getPitchAndFamily() {
		return 0;
	}

	protected void setHyperlink(HSLFHyperlink link) {
		this.link = link;
	}

	@Override
	public HSLFHyperlink getHyperlink() {
		return link;
	}

	@Override
	public HSLFHyperlink createHyperlink() {
		if ((link) == null) {
			parentParagraph.setDirty();
		}
		return link;
	}

	@Override
	public TextRun.FieldType getFieldType() {
		HSLFTextShape ts = getTextParagraph().getParentShape();
		Placeholder ph = ts.getPlaceholder();
		if (ph != null) {
			switch (ph) {
				case SLIDE_NUMBER :
					return SLIDE_NUMBER;
				case DATETIME :
					return DATE_TIME;
				default :
					break;
			}
		}
		if ((ts.getSheet()) instanceof MasterSheet) {
			TextShape<?, ? extends TextParagraph<?, ?, ? extends TextRun>> ms = ts.getMetroShape();
			if ((ms == null) || (ms.getTextParagraphs().isEmpty())) {
				return null;
			}
			List<? extends TextRun> trList = ms.getTextParagraphs().get(0).getTextRuns();
			if (trList.isEmpty()) {
				return null;
			}
			return trList.get(0).getFieldType();
		}
		return null;
	}

	private FontGroup safeFontGroup(FontGroup fontGroup) {
		return fontGroup != null ? fontGroup : FontGroup.getFontGroupFirst(getRawText());
	}
}

