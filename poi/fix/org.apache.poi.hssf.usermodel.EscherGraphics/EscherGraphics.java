

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;
import org.apache.poi.hssf.usermodel.HSSFChildAnchor;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFPolygon;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFShapeGroup;
import org.apache.poi.hssf.usermodel.HSSFSimpleShape;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;


public class EscherGraphics extends Graphics {
	private final HSSFShapeGroup escherGroup;

	private final HSSFWorkbook workbook;

	private float verticalPointsPerPixel = 1.0F;

	private final float verticalPixelsPerPoint;

	private Color foreground;

	private Color background = Color.white;

	private Font font;

	private static final POILogger logger = POILogFactory.getLogger(EscherGraphics.class);

	public EscherGraphics(HSSFShapeGroup escherGroup, HSSFWorkbook workbook, Color forecolor, float verticalPointsPerPixel) {
		this.escherGroup = escherGroup;
		this.workbook = workbook;
		this.verticalPointsPerPixel = verticalPointsPerPixel;
		this.verticalPixelsPerPoint = 1 / verticalPointsPerPixel;
		this.font = new Font("Arial", 0, 10);
		this.foreground = forecolor;
	}

	EscherGraphics(HSSFShapeGroup escherGroup, HSSFWorkbook workbook, Color foreground, Font font, float verticalPointsPerPixel) {
		this.escherGroup = escherGroup;
		this.workbook = workbook;
		this.foreground = foreground;
		this.font = font;
		this.verticalPointsPerPixel = verticalPointsPerPixel;
		this.verticalPixelsPerPoint = 1 / verticalPointsPerPixel;
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void clearRect(int x, int y, int width, int height) {
		Color color = foreground;
		setColor(background);
		fillRect(x, y, width, height);
		setColor(color);
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void clipRect(int x, int y, int width, int height) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "clipRect not supported");

	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "copyArea not supported");

	}

	@Override
	public Graphics create() {
		return new EscherGraphics(escherGroup, workbook, foreground, font, verticalPointsPerPixel);
	}

	@Override
	public void dispose() {
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "drawArc not supported");

	}

	@Override
	@org.apache.poi.util.NotImplemented
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "drawImage not supported");

		return true;
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "drawImage not supported");

		return true;
	}

	@Override
	public boolean drawImage(Image image, int i, int j, int k, int l, Color color, ImageObserver imageobserver) {
		return drawImage(image, i, j, (i + k), (j + l), 0, 0, image.getWidth(imageobserver), image.getHeight(imageobserver), color, imageobserver);
	}

	@Override
	public boolean drawImage(Image image, int i, int j, int k, int l, ImageObserver imageobserver) {
		return drawImage(image, i, j, (i + k), (j + l), 0, 0, image.getWidth(imageobserver), image.getHeight(imageobserver), imageobserver);
	}

	@Override
	public boolean drawImage(Image image, int i, int j, Color color, ImageObserver imageobserver) {
		return drawImage(image, i, j, image.getWidth(imageobserver), image.getHeight(imageobserver), color, imageobserver);
	}

	@Override
	public boolean drawImage(Image image, int i, int j, ImageObserver imageobserver) {
		return drawImage(image, i, j, image.getWidth(imageobserver), image.getHeight(imageobserver), imageobserver);
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		drawLine(x1, y1, x2, y2, 0);
	}

	public void drawLine(int x1, int y1, int x2, int y2, int width) {
		HSSFSimpleShape shape = escherGroup.createShape(new HSSFChildAnchor(x1, y1, x2, y2));
		shape.setShapeType(HSSFSimpleShape.OBJECT_TYPE_LINE);
		shape.setLineWidth(width);
		shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
	}

	@Override
	public void drawOval(int x, int y, int width, int height) {
		HSSFSimpleShape shape = escherGroup.createShape(new HSSFChildAnchor(x, y, (x + width), (y + height)));
		shape.setShapeType(HSSFSimpleShape.OBJECT_TYPE_OVAL);
		shape.setLineWidth(0);
		shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
		shape.setNoFill(true);
	}

	@Override
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		int right = findBiggest(xPoints);
		int bottom = findBiggest(yPoints);
		int left = findSmallest(xPoints);
		int top = findSmallest(yPoints);
		HSSFPolygon shape = escherGroup.createPolygon(new HSSFChildAnchor(left, top, right, bottom));
		shape.setPolygonDrawArea((right - left), (bottom - top));
		shape.setPoints(addToAll(xPoints, (-left)), addToAll(yPoints, (-top)));
		shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
		shape.setLineWidth(0);
		shape.setNoFill(true);
	}

	private int[] addToAll(int[] values, int amount) {
		int[] result = new int[values.length];
		for (int i = 0; i < (values.length); i++)
			result[i] = (values[i]) + amount;

		return result;
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "drawPolyline not supported");

	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void drawRect(int x, int y, int width, int height) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "drawRect not supported");

	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "drawRoundRect not supported");

	}

	@Override
	public void drawString(String str, int x, int y) {
		if ((str == null) || (str.isEmpty()))
			return;

		Font excelFont = font;
		if (font.getName().equals("SansSerif")) {
			excelFont = new Font("Arial", font.getStyle(), ((int) ((font.getSize()) / (verticalPixelsPerPoint))));
		}else {
			excelFont = new Font(font.getName(), font.getStyle(), ((int) ((font.getSize()) / (verticalPixelsPerPoint))));
		}
		int height = ((int) (((font.getSize()) / (verticalPixelsPerPoint)) + 6)) * 2;
		y -= ((font.getSize()) / (verticalPixelsPerPoint)) + (2 * (verticalPixelsPerPoint));
		HSSFRichTextString s = new HSSFRichTextString(str);
		HSSFFont hssfFont = matchFont(excelFont);
		s.applyFont(hssfFont);
	}

	private HSSFFont matchFont(Font matchFont) {
		HSSFColor hssfColor = workbook.getCustomPalette().findColor(((byte) (foreground.getRed())), ((byte) (foreground.getGreen())), ((byte) (foreground.getBlue())));
		if (hssfColor == null)
			hssfColor = workbook.getCustomPalette().findSimilarColor(((byte) (foreground.getRed())), ((byte) (foreground.getGreen())), ((byte) (foreground.getBlue())));

		boolean bold = ((matchFont.getStyle()) & (Font.BOLD)) != 0;
		boolean italic = ((matchFont.getStyle()) & (Font.ITALIC)) != 0;
		HSSFFont hssfFont = workbook.findFont(bold, hssfColor.getIndex(), ((short) ((matchFont.getSize()) * 20)), matchFont.getName(), italic, false, ((short) (0)), ((byte) (0)));
		if (hssfFont == null) {
			hssfFont = workbook.createFont();
			hssfFont.setBold(bold);
			hssfFont.setColor(hssfColor.getIndex());
			hssfFont.setFontHeight(((short) ((matchFont.getSize()) * 20)));
			hssfFont.setFontName(matchFont.getName());
			hssfFont.setItalic(italic);
			hssfFont.setStrikeout(false);
			hssfFont.setTypeOffset(((short) (0)));
			hssfFont.setUnderline(((byte) (0)));
		}
		return hssfFont;
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, int x, int y) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "drawString not supported");

	}

	@Override
	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "fillArc not supported");

	}

	@Override
	public void fillOval(int x, int y, int width, int height) {
		HSSFSimpleShape shape = escherGroup.createShape(new HSSFChildAnchor(x, y, (x + width), (y + height)));
		shape.setShapeType(HSSFSimpleShape.OBJECT_TYPE_OVAL);
		shape.setLineStyle(HSSFShape.LINESTYLE_NONE);
		shape.setFillColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
		shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
		shape.setNoFill(false);
	}

	@Override
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		int right = findBiggest(xPoints);
		int bottom = findBiggest(yPoints);
		int left = findSmallest(xPoints);
		int top = findSmallest(yPoints);
		HSSFPolygon shape = escherGroup.createPolygon(new HSSFChildAnchor(left, top, right, bottom));
		shape.setPolygonDrawArea((right - left), (bottom - top));
		shape.setPoints(addToAll(xPoints, (-left)), addToAll(yPoints, (-top)));
		shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
		shape.setFillColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
	}

	private int findBiggest(int[] values) {
		int result = Integer.MIN_VALUE;
		for (int i = 0; i < (values.length); i++) {
			if ((values[i]) > result)
				result = values[i];

		}
		return result;
	}

	private int findSmallest(int[] values) {
		int result = Integer.MAX_VALUE;
		for (int i = 0; i < (values.length); i++) {
			if ((values[i]) < result)
				result = values[i];

		}
		return result;
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		HSSFSimpleShape shape = escherGroup.createShape(new HSSFChildAnchor(x, y, (x + width), (y + height)));
		shape.setShapeType(HSSFSimpleShape.OBJECT_TYPE_RECTANGLE);
		shape.setLineStyle(HSSFShape.LINESTYLE_NONE);
		shape.setFillColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
		shape.setLineStyleColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "fillRoundRect not supported");

	}

	@Override
	public Shape getClip() {
		return getClipBounds();
	}

	@Override
	public Rectangle getClipBounds() {
		return null;
	}

	@Override
	public Color getColor() {
		return foreground;
	}

	@Override
	public Font getFont() {
		return font;
	}

	@Override
	@SuppressWarnings("deprecation")
	@org.apache.poi.util.SuppressForbidden
	public FontMetrics getFontMetrics(Font f) {
		return Toolkit.getDefaultToolkit().getFontMetrics(f);
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
		setClip(new Rectangle(x, y, width, height));
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void setClip(Shape shape) {
	}

	@Override
	public void setColor(Color color) {
		foreground = color;
	}

	@Override
	public void setFont(Font f) {
		font = f;
	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void setPaintMode() {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "setPaintMode not supported");

	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void setXORMode(Color color) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "setXORMode not supported");

	}

	@Override
	@org.apache.poi.util.NotImplemented
	public void translate(int x, int y) {
		if (EscherGraphics.logger.check(POILogger.WARN))
			EscherGraphics.logger.log(POILogger.WARN, "translate not supported");

	}

	public Color getBackground() {
		return background;
	}

	public void setBackground(Color background) {
		this.background = background;
	}

	HSSFShapeGroup getEscherGraphics() {
		return escherGroup;
	}
}

