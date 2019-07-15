

import org.apache.poi.hwpf.converter.AbstractWordUtils;
import org.apache.poi.hwpf.usermodel.BorderCode;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.util.Beta;
import org.w3c.dom.Element;


@Beta
public class WordToFoUtils extends AbstractWordUtils {
	static void compactInlines(Element blockElement) {
	}

	public static void setBold(final Element element, final boolean bold) {
		element.setAttribute("font-weight", (bold ? "bold" : "normal"));
	}

	public static void setBorder(Element element, BorderCode borderCode, String where) {
		if (element == null)
			throw new IllegalArgumentException("element is null");

		if ((borderCode == null) || (borderCode.isEmpty()))
			return;

	}

	public static void setCharactersProperties(final CharacterRun characterRun, final Element inline) {
		StringBuilder textDecorations = new StringBuilder();
		if ((characterRun.getIco24()) != (-1)) {
			inline.setAttribute("color", AbstractWordUtils.getColor24(characterRun.getIco24()));
		}
		if (characterRun.isCapitalized()) {
			inline.setAttribute("text-transform", "uppercase");
		}
		if (characterRun.isHighlighted()) {
			inline.setAttribute("background-color", AbstractWordUtils.getColor(characterRun.getHighlightedColor()));
		}
		if (characterRun.isStrikeThrough()) {
			if ((textDecorations.length()) > 0)
				textDecorations.append(" ");

			textDecorations.append("line-through");
		}
		if (characterRun.isShadowed()) {
			inline.setAttribute("text-shadow", (((characterRun.getFontSize()) / 24) + "pt"));
		}
		if (characterRun.isSmallCaps()) {
			inline.setAttribute("font-variant", "small-caps");
		}
		if ((characterRun.getSubSuperScriptIndex()) == 1) {
			inline.setAttribute("baseline-shift", "super");
			inline.setAttribute("font-size", "smaller");
		}
		if ((characterRun.getSubSuperScriptIndex()) == 2) {
			inline.setAttribute("baseline-shift", "sub");
			inline.setAttribute("font-size", "smaller");
		}
		if ((characterRun.getUnderlineCode()) > 0) {
			if ((textDecorations.length()) > 0)
				textDecorations.append(" ");

			textDecorations.append("underline");
		}
		if (characterRun.isVanished()) {
			inline.setAttribute("visibility", "hidden");
		}
		if ((textDecorations.length()) > 0) {
			inline.setAttribute("text-decoration", textDecorations.toString());
		}
	}

	public static void setFontFamily(final Element element, final String fontFamily) {
		element.setAttribute("font-family", fontFamily);
	}

	public static void setFontSize(final Element element, final int fontSize) {
		element.setAttribute("font-size", String.valueOf(fontSize));
	}

	public static void setIndent(Paragraph paragraph, Element block) {
		if ((paragraph.getFirstLineIndent()) != 0) {
			block.setAttribute("text-indent", (((paragraph.getFirstLineIndent()) / (AbstractWordUtils.TWIPS_PER_PT)) + "pt"));
		}
		if ((paragraph.getIndentFromLeft()) != 0) {
			block.setAttribute("start-indent", (((paragraph.getIndentFromLeft()) / (AbstractWordUtils.TWIPS_PER_PT)) + "pt"));
		}
		if ((paragraph.getIndentFromRight()) != 0) {
			block.setAttribute("end-indent", (((paragraph.getIndentFromRight()) / (AbstractWordUtils.TWIPS_PER_PT)) + "pt"));
		}
		if ((paragraph.getSpacingBefore()) != 0) {
			block.setAttribute("space-before", (((paragraph.getSpacingBefore()) / (AbstractWordUtils.TWIPS_PER_PT)) + "pt"));
		}
		if ((paragraph.getSpacingAfter()) != 0) {
			block.setAttribute("space-after", (((paragraph.getSpacingAfter()) / (AbstractWordUtils.TWIPS_PER_PT)) + "pt"));
		}
	}

	public static void setItalic(final Element element, final boolean italic) {
		element.setAttribute("font-style", (italic ? "italic" : "normal"));
	}

	public static void setJustification(Paragraph paragraph, final Element element) {
		String justification = AbstractWordUtils.getJustification(paragraph.getJustification());
	}

	public static void setLanguage(final CharacterRun characterRun, final Element inline) {
		if ((characterRun.getLanguageCode()) != 0) {
			final String language = AbstractWordUtils.getLanguage(characterRun.getLanguageCode());
		}
	}

	public static void setParagraphProperties(Paragraph paragraph, Element block) {
		WordToFoUtils.setIndent(paragraph, block);
		WordToFoUtils.setJustification(paragraph, block);
		WordToFoUtils.setBorder(block, paragraph.getBottomBorder(), "bottom");
		WordToFoUtils.setBorder(block, paragraph.getLeftBorder(), "left");
		WordToFoUtils.setBorder(block, paragraph.getRightBorder(), "right");
		WordToFoUtils.setBorder(block, paragraph.getTopBorder(), "top");
		if (paragraph.pageBreakBefore()) {
			block.setAttribute("break-before", "page");
		}
		block.setAttribute("hyphenate", String.valueOf(paragraph.isAutoHyphenated()));
		if (paragraph.keepOnPage()) {
			block.setAttribute("keep-together.within-page", "always");
		}
		if (paragraph.keepWithNext()) {
			block.setAttribute("keep-with-next.within-page", "always");
		}
		block.setAttribute("linefeed-treatment", "preserve");
		block.setAttribute("white-space-collapse", "false");
	}

	public static void setPictureProperties(Picture picture, Element graphicElement) {
		final int horizontalScale = picture.getHorizontalScalingFactor();
		final int verticalScale = picture.getVerticalScalingFactor();
		if (horizontalScale > 0) {
			graphicElement.setAttribute("content-width", (((((picture.getDxaGoal()) * horizontalScale) / 1000) / (AbstractWordUtils.TWIPS_PER_PT)) + "pt"));
		}else
			graphicElement.setAttribute("content-width", (((picture.getDxaGoal()) / (AbstractWordUtils.TWIPS_PER_PT)) + "pt"));

		if (verticalScale > 0)
			graphicElement.setAttribute("content-height", (((((picture.getDyaGoal()) * verticalScale) / 1000) / (AbstractWordUtils.TWIPS_PER_PT)) + "pt"));
		else
			graphicElement.setAttribute("content-height", (((picture.getDyaGoal()) / (AbstractWordUtils.TWIPS_PER_PT)) + "pt"));

		if ((horizontalScale <= 0) || (verticalScale <= 0)) {
			graphicElement.setAttribute("scaling", "uniform");
		}else {
			graphicElement.setAttribute("scaling", "non-uniform");
		}
		graphicElement.setAttribute("vertical-align", "text-bottom");
		if (((((picture.getDyaCropTop()) != 0) || ((picture.getDxaCropRight()) != 0)) || ((picture.getDyaCropBottom()) != 0)) || ((picture.getDxaCropLeft()) != 0)) {
			int rectTop = (picture.getDyaCropTop()) / (AbstractWordUtils.TWIPS_PER_PT);
			int rectRight = (picture.getDxaCropRight()) / (AbstractWordUtils.TWIPS_PER_PT);
			int rectBottom = (picture.getDyaCropBottom()) / (AbstractWordUtils.TWIPS_PER_PT);
			int rectLeft = (picture.getDxaCropLeft()) / (AbstractWordUtils.TWIPS_PER_PT);
			graphicElement.setAttribute("clip", (((((((("rect(" + rectTop) + "pt, ") + rectRight) + "pt, ") + rectBottom) + "pt, ") + rectLeft) + "pt)"));
			graphicElement.setAttribute("overflow", "hidden");
		}
	}

	public static void setTableCellProperties(TableRow tableRow, TableCell tableCell, Element element, boolean toppest, boolean bottomest, boolean leftest, boolean rightest) {
		element.setAttribute("width", (((tableCell.getWidth()) / (AbstractWordUtils.TWIPS_PER_INCH)) + "in"));
		element.setAttribute("padding-start", (((tableRow.getGapHalf()) / (AbstractWordUtils.TWIPS_PER_INCH)) + "in"));
		element.setAttribute("padding-end", (((tableRow.getGapHalf()) / (AbstractWordUtils.TWIPS_PER_INCH)) + "in"));
		BorderCode top = (((tableCell.getBrcTop()) != null) && ((tableCell.getBrcTop().getBorderType()) != 0)) ? tableCell.getBrcTop() : toppest ? tableRow.getTopBorder() : tableRow.getHorizontalBorder();
		BorderCode bottom = (((tableCell.getBrcBottom()) != null) && ((tableCell.getBrcBottom().getBorderType()) != 0)) ? tableCell.getBrcBottom() : bottomest ? tableRow.getBottomBorder() : tableRow.getHorizontalBorder();
		BorderCode left = (((tableCell.getBrcLeft()) != null) && ((tableCell.getBrcLeft().getBorderType()) != 0)) ? tableCell.getBrcLeft() : leftest ? tableRow.getLeftBorder() : tableRow.getVerticalBorder();
		BorderCode right = (((tableCell.getBrcRight()) != null) && ((tableCell.getBrcRight().getBorderType()) != 0)) ? tableCell.getBrcRight() : rightest ? tableRow.getRightBorder() : tableRow.getVerticalBorder();
		WordToFoUtils.setBorder(element, bottom, "bottom");
		WordToFoUtils.setBorder(element, left, "left");
		WordToFoUtils.setBorder(element, right, "right");
		WordToFoUtils.setBorder(element, top, "top");
	}

	public static void setTableRowProperties(TableRow tableRow, Element tableRowElement) {
		if ((tableRow.getRowHeight()) > 0) {
			tableRowElement.setAttribute("height", (((tableRow.getRowHeight()) / (AbstractWordUtils.TWIPS_PER_INCH)) + "in"));
		}
		if (!(tableRow.cantSplit())) {
			tableRowElement.setAttribute("keep-together.within-column", "always");
		}
	}
}

