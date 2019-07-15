

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;
import java.nio.charset.Charset;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.apache.poi.common.usermodel.fonts.FontCharset;
import org.apache.poi.common.usermodel.fonts.FontInfo;
import org.apache.poi.hwmf.draw.HwmfDrawProperties;
import org.apache.poi.hwmf.record.HwmfBrushStyle;
import org.apache.poi.hwmf.record.HwmfColorRef;
import org.apache.poi.hwmf.record.HwmfFont;
import org.apache.poi.hwmf.record.HwmfHatchStyle;
import org.apache.poi.hwmf.record.HwmfMapMode;
import org.apache.poi.hwmf.record.HwmfMisc;
import org.apache.poi.hwmf.record.HwmfObjectTableEntry;
import org.apache.poi.hwmf.record.HwmfPenStyle;
import org.apache.poi.sl.draw.DrawFactory;
import org.apache.poi.sl.draw.DrawFontManager;
import org.apache.poi.util.LocaleUtil;

import static org.apache.poi.hwmf.record.HwmfMisc.WmfSetBkMode.HwmfBkMode.OPAQUE;
import static org.apache.poi.hwmf.record.HwmfMisc.WmfSetBkMode.HwmfBkMode.TRANSPARENT;
import static org.apache.poi.hwmf.record.HwmfPenStyle.HwmfLineDash.INSIDEFRAME;
import static org.apache.poi.hwmf.record.HwmfPenStyle.HwmfLineDash.NULL;
import static org.apache.poi.hwmf.record.HwmfPenStyle.HwmfLineDash.SOLID;


public class HwmfGraphics {
	private static final Charset DEFAULT_CHARSET = LocaleUtil.CHARSET_1252;

	private final Graphics2D graphicsCtx;

	private final List<HwmfDrawProperties> propStack = new LinkedList<>();

	private HwmfDrawProperties prop = new HwmfDrawProperties();

	private List<HwmfObjectTableEntry> objectTable = new ArrayList<>();

	private final Rectangle2D bbox;

	private final AffineTransform initialAT;

	public HwmfGraphics(Graphics2D graphicsCtx, Rectangle2D bbox) {
		this.graphicsCtx = graphicsCtx;
		this.bbox = ((Rectangle2D) (bbox.clone()));
		this.initialAT = graphicsCtx.getTransform();
		DrawFactory.getInstance(graphicsCtx).fixFonts(graphicsCtx);
	}

	public HwmfDrawProperties getProperties() {
		return prop;
	}

	public void draw(Shape shape) {
		HwmfPenStyle.HwmfLineDash lineDash = prop.getPenStyle().getLineDash();
		if (lineDash == (NULL)) {
			return;
		}
		BasicStroke stroke = getStroke();
		if (((prop.getBkMode()) == (OPAQUE)) && ((lineDash != (SOLID)) && (lineDash != (INSIDEFRAME)))) {
			graphicsCtx.setStroke(new BasicStroke(stroke.getLineWidth()));
			graphicsCtx.setColor(prop.getBackgroundColor().getColor());
			graphicsCtx.draw(shape);
		}
		graphicsCtx.setStroke(stroke);
		graphicsCtx.setColor(prop.getPenColor().getColor());
		graphicsCtx.draw(shape);
	}

	public void fill(Shape shape) {
		if ((prop.getBrushStyle()) != (HwmfBrushStyle.BS_NULL)) {
			graphicsCtx.setPaint(getFill());
			graphicsCtx.fill(shape);
		}
		draw(shape);
	}

	protected BasicStroke getStroke() {
		float width = ((float) (prop.getPenWidth()));
		if (width == 0) {
			width = 1;
		}
		HwmfPenStyle ps = prop.getPenStyle();
		int cap = ps.getLineCap().awtFlag;
		int join = ps.getLineJoin().awtFlag;
		float miterLimit = ((float) (prop.getPenMiterLimit()));
		float[] dashes = ps.getLineDash().dashes;
		boolean dashAlt = ps.isAlternateDash();
		float dashStart = ((dashAlt && (dashes != null)) && ((dashes.length) > 1)) ? dashes[0] : 0;
		return new BasicStroke(width, cap, join, miterLimit, dashes, dashStart);
	}

	protected Paint getFill() {
		switch (prop.getBrushStyle()) {
			default :
			case BS_INDEXED :
			case BS_PATTERN8X8 :
			case BS_DIBPATTERN8X8 :
			case BS_MONOPATTERN :
			case BS_NULL :
				return null;
			case BS_PATTERN :
			case BS_DIBPATTERN :
			case BS_DIBPATTERNPT :
				return getPatternPaint();
			case BS_SOLID :
				return getSolidFill();
			case BS_HATCHED :
				return getHatchedFill();
		}
	}

	protected Paint getSolidFill() {
		return prop.getBrushColor().getColor();
	}

	protected Paint getHatchedFill() {
		int dim = 7;
		int mid = 3;
		BufferedImage bi = new BufferedImage(dim, dim, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = bi.createGraphics();
		Color c = ((prop.getBkMode()) == (TRANSPARENT)) ? new Color(0, true) : prop.getBackgroundColor().getColor();
		g.setColor(c);
		g.fillRect(0, 0, dim, dim);
		g.setColor(prop.getBrushColor().getColor());
		HwmfHatchStyle h = prop.getBrushHatch();
		if ((h == (HwmfHatchStyle.HS_HORIZONTAL)) || (h == (HwmfHatchStyle.HS_CROSS))) {
			g.drawLine(0, mid, dim, mid);
		}
		if ((h == (HwmfHatchStyle.HS_VERTICAL)) || (h == (HwmfHatchStyle.HS_CROSS))) {
			g.drawLine(mid, 0, mid, dim);
		}
		if ((h == (HwmfHatchStyle.HS_FDIAGONAL)) || (h == (HwmfHatchStyle.HS_DIAGCROSS))) {
			g.drawLine(0, 0, dim, dim);
		}
		if ((h == (HwmfHatchStyle.HS_BDIAGONAL)) || (h == (HwmfHatchStyle.HS_DIAGCROSS))) {
			g.drawLine(0, dim, dim, 0);
		}
		g.dispose();
		return new TexturePaint(bi, new Rectangle(0, 0, dim, dim));
	}

	protected Paint getPatternPaint() {
		BufferedImage bi = prop.getBrushBitmap();
		return bi == null ? null : new TexturePaint(bi, new Rectangle(0, 0, bi.getWidth(), bi.getHeight()));
	}

	public void addObjectTableEntry(HwmfObjectTableEntry entry) {
		ListIterator<HwmfObjectTableEntry> oIter = objectTable.listIterator();
		while (oIter.hasNext()) {
			HwmfObjectTableEntry tableEntry = oIter.next();
			if (tableEntry == null) {
				oIter.set(entry);
				return;
			}
		} 
		objectTable.add(entry);
	}

	public void applyObjectTableEntry(int index) {
		HwmfObjectTableEntry ote = objectTable.get(index);
		if (ote == null) {
			throw new NoSuchElementException((("WMF reference exception - object table entry on index " + index) + " was deleted before."));
		}
	}

	public void unsetObjectTableEntry(int index) {
		objectTable.set(index, null);
	}

	public void saveProperties() {
		propStack.add(prop);
		prop = new HwmfDrawProperties(prop);
	}

	public void restoreProperties(int index) {
		if (index == 0) {
			return;
		}
		int stackIndex = index;
		if (stackIndex < 0) {
			int curIdx = propStack.indexOf(prop);
			if (curIdx == (-1)) {
				curIdx = propStack.size();
			}
			stackIndex = curIdx + index;
		}
		if (stackIndex == (-1)) {
			stackIndex = (propStack.size()) - 1;
		}
		prop = propStack.get(stackIndex);
	}

	public void updateWindowMapMode() {
		Rectangle2D win = prop.getWindow();
		HwmfMapMode mapMode = prop.getMapMode();
		graphicsCtx.setTransform(initialAT);
		switch (mapMode) {
			default :
			case MM_ANISOTROPIC :
				graphicsCtx.scale(((bbox.getWidth()) / (win.getWidth())), ((bbox.getHeight()) / (win.getHeight())));
				graphicsCtx.translate((-(win.getX())), (-(win.getY())));
				break;
			case MM_ISOTROPIC :
				graphicsCtx.scale(((bbox.getWidth()) / (win.getWidth())), ((bbox.getWidth()) / (win.getWidth())));
				graphicsCtx.translate((-(win.getX())), (-(win.getY())));
				break;
			case MM_LOMETRIC :
			case MM_HIMETRIC :
			case MM_LOENGLISH :
			case MM_HIENGLISH :
			case MM_TWIPS :
				{
					GraphicsConfiguration gc = graphicsCtx.getDeviceConfiguration();
					graphicsCtx.transform(gc.getNormalizingTransform());
					graphicsCtx.scale((1.0 / (mapMode.scale)), ((-1.0) / (mapMode.scale)));
					graphicsCtx.translate((-(win.getX())), (-(win.getY())));
					break;
				}
			case MM_TEXT :
				break;
		}
	}

	public void drawString(byte[] text, Rectangle2D bounds) {
		drawString(text, bounds, null);
	}

	public void drawString(byte[] text, Rectangle2D bounds, int[] dx) {
		HwmfFont font = prop.getFont();
		if (((font == null) || (text == null)) || ((text.length) == 0)) {
			return;
		}
		double fontH = getFontHeight(font);
		double fontW = fontH / 1.8;
		Charset charset = ((font.getCharset().getCharset()) == null) ? HwmfGraphics.DEFAULT_CHARSET : font.getCharset().getCharset();
		String textString = new String(text, charset);
		AttributedString as = new AttributedString(textString);
		if ((dx == null) || ((dx.length) == 0)) {
			addAttributes(as, font);
		}else {
			int[] dxNormed = dx;
			if ((textString.length()) != (text.length)) {
				int codePoints = textString.codePointCount(0, textString.length());
				dxNormed = new int[codePoints];
				int dxPosition = 0;
				for (int offset = 0; offset < (textString.length());) {
					dxNormed[offset] = dx[dxPosition];
					int[] chars = new int[1];
					int cp = textString.codePointAt(offset);
					chars[0] = cp;
					int byteLength = new String(chars, 0, chars.length).getBytes(charset).length;
					dxPosition += byteLength;
					offset += Character.charCount(cp);
				}
			}
			for (int i = 0; i < (dxNormed.length); i++) {
				addAttributes(as, font);
				if (i < ((dxNormed.length) - 1)) {
					as.addAttribute(TextAttribute.TRACKING, (((dxNormed[i]) - fontW) / fontH), (i + 1), (i + 2));
				}
			}
		}
		double angle = Math.toRadians(((-(font.getEscapement())) / 10.0));
		final AffineTransform at = graphicsCtx.getTransform();
		try {
			graphicsCtx.translate(bounds.getX(), ((bounds.getY()) + fontH));
			graphicsCtx.rotate(angle);
			if ((prop.getBkMode()) == (OPAQUE)) {
				graphicsCtx.setBackground(prop.getBackgroundColor().getColor());
				graphicsCtx.fill(new Rectangle2D.Double(0, 0, bounds.getWidth(), bounds.getHeight()));
			}
			graphicsCtx.setColor(prop.getTextColor().getColor());
			graphicsCtx.drawString(as.getIterator(), 0, 0);
		} finally {
			graphicsCtx.setTransform(at);
		}
	}

	private void addAttributes(AttributedString as, HwmfFont font) {
		DrawFontManager fontHandler = DrawFactory.getInstance(graphicsCtx).getFontManager(graphicsCtx);
		FontInfo fontInfo = fontHandler.getMappedFont(graphicsCtx, font);
		as.addAttribute(TextAttribute.FAMILY, fontInfo.getTypeface());
		as.addAttribute(TextAttribute.SIZE, getFontHeight(font));
		as.addAttribute(TextAttribute.STRIKETHROUGH, font.isStrikeOut());
		if (font.isUnderline()) {
			as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		}
		if (font.isItalic()) {
			as.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
		}
		as.addAttribute(TextAttribute.WEIGHT, font.getWeight());
	}

	private double getFontHeight(HwmfFont font) {
		double fontHeight = font.getHeight();
		if (fontHeight == 0) {
			return 12;
		}else
			if (fontHeight < 0) {
				return -fontHeight;
			}else {
				return (fontHeight * 3) / 4;
			}

	}
}

