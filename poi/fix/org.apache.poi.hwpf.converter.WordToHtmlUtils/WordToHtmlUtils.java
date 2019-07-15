

import org.apache.poi.hwpf.converter.AbstractWordUtils;
import org.apache.poi.hwpf.usermodel.BorderCode;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.util.Beta;
import org.w3c.dom.Element;


@Beta
public class WordToHtmlUtils extends AbstractWordUtils {
	public static void addBold(final boolean bold, StringBuilder style) {
		style.append((("font-weight:" + (bold ? "bold" : "normal")) + ";"));
	}

	public static void addBorder(BorderCode borderCode, String where, StringBuilder style) {
		if ((borderCode == null) || (borderCode.isEmpty()))
			return;

		style.append(":");
		if ((borderCode.getLineWidth()) < 8)
			style.append("thin");
		else
			style.append(AbstractWordUtils.getBorderWidth(borderCode));

		style.append(' ');
		style.append(AbstractWordUtils.getBorderType(borderCode));
		style.append(' ');
		style.append(AbstractWordUtils.getColor(borderCode.getColor()));
		style.append(';');
	}

	public static void addCharactersProperties(final CharacterRun characterRun, StringBuilder style) {
		if (characterRun.isCapitalized()) {
			style.append("text-transform:uppercase;");
		}
		if ((characterRun.getIco24()) != (-1)) {
			style.append((("color:" + (AbstractWordUtils.getColor24(characterRun.getIco24()))) + ";"));
		}
		if (characterRun.isHighlighted()) {
			style.append((("background-color:" + (AbstractWordUtils.getColor(characterRun.getHighlightedColor()))) + ";"));
		}
		if (characterRun.isStrikeThrough()) {
			style.append("text-decoration:line-through;");
		}
		if (characterRun.isShadowed()) {
			style.append((("text-shadow:" + ((characterRun.getFontSize()) / 24)) + "pt;"));
		}
		if (characterRun.isSmallCaps()) {
			style.append("font-variant:small-caps;");
		}
		if ((characterRun.getSubSuperScriptIndex()) == 1) {
			style.append("vertical-align:super;");
			style.append("font-size:smaller;");
		}
		if ((characterRun.getSubSuperScriptIndex()) == 2) {
			style.append("vertical-align:sub;");
			style.append("font-size:smaller;");
		}
		if ((characterRun.getUnderlineCode()) > 0) {
			style.append("text-decoration:underline;");
		}
		if (characterRun.isVanished()) {
			style.append("visibility:hidden;");
		}
	}

	public static void addFontFamily(final String fontFamily, StringBuilder style) {
		style.append((("font-family:" + fontFamily) + ";"));
	}

	public static void addFontSize(final int fontSize, StringBuilder style) {
		style.append((("font-size:" + fontSize) + "pt;"));
	}

	public static void addIndent(Paragraph paragraph, StringBuilder style) {
		WordToHtmlUtils.addIndent(style, "text-indent", paragraph.getFirstLineIndent());
		WordToHtmlUtils.addIndent(style, "margin-left", paragraph.getIndentFromLeft());
		WordToHtmlUtils.addIndent(style, "margin-right", paragraph.getIndentFromRight());
		WordToHtmlUtils.addIndent(style, "margin-top", paragraph.getSpacingBefore());
		WordToHtmlUtils.addIndent(style, "margin-bottom", paragraph.getSpacingAfter());
	}

	private static void addIndent(StringBuilder style, final String cssName, final int twipsValue) {
		if (twipsValue == 0)
			return;

		style.append((((cssName + ":") + (twipsValue / (AbstractWordUtils.TWIPS_PER_INCH))) + "in;"));
	}

	public static void addJustification(Paragraph paragraph, final StringBuilder style) {
		String justification = AbstractWordUtils.getJustification(paragraph.getJustification());
	}

	public static void addParagraphProperties(Paragraph paragraph, StringBuilder style) {
		WordToHtmlUtils.addIndent(paragraph, style);
		WordToHtmlUtils.addJustification(paragraph, style);
		WordToHtmlUtils.addBorder(paragraph.getBottomBorder(), "bottom", style);
		WordToHtmlUtils.addBorder(paragraph.getLeftBorder(), "left", style);
		WordToHtmlUtils.addBorder(paragraph.getRightBorder(), "right", style);
		WordToHtmlUtils.addBorder(paragraph.getTopBorder(), "top", style);
		if (paragraph.pageBreakBefore()) {
			style.append("break-before:page;");
		}
		style.append((("hyphenate:" + (paragraph.isAutoHyphenated() ? "auto" : "none")) + ";"));
		if (paragraph.keepOnPage()) {
			style.append("keep-together.within-page:always;");
		}
		if (paragraph.keepWithNext()) {
			style.append("keep-with-next.within-page:always;");
		}
	}

	public static void addTableCellProperties(TableRow tableRow, TableCell tableCell, boolean toppest, boolean bottomest, boolean leftest, boolean rightest, StringBuilder style) {
		style.append((("width:" + ((tableCell.getWidth()) / (AbstractWordUtils.TWIPS_PER_INCH))) + "in;"));
		style.append((("padding-start:" + ((tableRow.getGapHalf()) / (AbstractWordUtils.TWIPS_PER_INCH))) + "in;"));
		style.append((("padding-end:" + ((tableRow.getGapHalf()) / (AbstractWordUtils.TWIPS_PER_INCH))) + "in;"));
		BorderCode top = (((tableCell.getBrcTop()) != null) && ((tableCell.getBrcTop().getBorderType()) != 0)) ? tableCell.getBrcTop() : toppest ? tableRow.getTopBorder() : tableRow.getHorizontalBorder();
		BorderCode bottom = (((tableCell.getBrcBottom()) != null) && ((tableCell.getBrcBottom().getBorderType()) != 0)) ? tableCell.getBrcBottom() : bottomest ? tableRow.getBottomBorder() : tableRow.getHorizontalBorder();
		BorderCode left = (((tableCell.getBrcLeft()) != null) && ((tableCell.getBrcLeft().getBorderType()) != 0)) ? tableCell.getBrcLeft() : leftest ? tableRow.getLeftBorder() : tableRow.getVerticalBorder();
		BorderCode right = (((tableCell.getBrcRight()) != null) && ((tableCell.getBrcRight().getBorderType()) != 0)) ? tableCell.getBrcRight() : rightest ? tableRow.getRightBorder() : tableRow.getVerticalBorder();
		WordToHtmlUtils.addBorder(bottom, "bottom", style);
		WordToHtmlUtils.addBorder(left, "left", style);
		WordToHtmlUtils.addBorder(right, "right", style);
		WordToHtmlUtils.addBorder(top, "top", style);
	}

	public static void addTableRowProperties(TableRow tableRow, StringBuilder style) {
		if ((tableRow.getRowHeight()) > 0) {
			style.append((("height:" + ((tableRow.getRowHeight()) / (AbstractWordUtils.TWIPS_PER_INCH))) + "in;"));
		}
		if (!(tableRow.cantSplit())) {
			style.append("keep-together:always;");
		}
	}

	static void compactSpans(Element pElement) {
	}
}

