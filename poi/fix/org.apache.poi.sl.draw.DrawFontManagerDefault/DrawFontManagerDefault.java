

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Map;
import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.sl.draw.DrawFontManager;
import org.apache.poi.sl.draw.Drawable;


public class DrawFontManagerDefault implements DrawFontManager {
	@Override
	public FontInfo getMappedFont(Graphics2D graphics, FontInfo fontInfo) {
		return getFontWithFallback(graphics, Drawable.FONT_MAP, fontInfo);
	}

	@Override
	public FontInfo getFallbackFont(Graphics2D graphics, FontInfo fontInfo) {
		FontInfo fi = getFontWithFallback(graphics, Drawable.FONT_FALLBACK, fontInfo);
		if (fi == null) {
		}
		return fi;
	}

	public String mapFontCharset(Graphics2D graphics, FontInfo fontInfo, String text) {
		String attStr = text;
		if ((fontInfo != null) && ("Wingdings".equalsIgnoreCase(fontInfo.getTypeface()))) {
			boolean changed = false;
			char[] chrs = attStr.toCharArray();
			for (int i = 0; i < (chrs.length); i++) {
				if (((32 <= (chrs[i])) && ((chrs[i]) <= 127)) || ((160 <= (chrs[i])) && ((chrs[i]) <= 255))) {
					chrs[i] |= 61440;
					changed = true;
				}
			}
			if (changed) {
				attStr = new String(chrs);
			}
		}
		return attStr;
	}

	@Override
	public Font createAWTFont(Graphics2D graphics, FontInfo fontInfo, double fontSize, boolean bold, boolean italic) {
		int style = (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0);
		Font font = new Font(fontInfo.getTypeface(), style, 12);
		if (Font.DIALOG.equals(font.getFamily())) {
			font = new Font(Font.SANS_SERIF, style, 12);
		}
		return font.deriveFont(((float) (fontSize)));
	}

	private FontInfo getFontWithFallback(Graphics2D graphics, Drawable.DrawableHint hint, FontInfo fontInfo) {
		@SuppressWarnings("unchecked")
		Map<String, String> fontMap = ((Map<String, String>) (graphics.getRenderingHint(hint)));
		if (fontMap == null) {
			return fontInfo;
		}
		String f = (fontInfo != null) ? fontInfo.getTypeface() : null;
		String mappedTypeface = null;
		if (fontMap.containsKey(f)) {
			mappedTypeface = fontMap.get(f);
		}else
			if (fontMap.containsKey("*")) {
				mappedTypeface = fontMap.get("*");
			}

		return null;
	}
}

