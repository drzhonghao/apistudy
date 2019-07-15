

import java.awt.Color;
import java.awt.Color[];
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import org.apache.poi.sl.draw.DrawPictureShape;
import org.apache.poi.sl.draw.DrawShape;
import org.apache.poi.sl.draw.ImageRenderer;
import org.apache.poi.sl.usermodel.AbstractColorStyle;
import org.apache.poi.sl.usermodel.ColorStyle;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.PlaceableShape;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;

import static org.apache.poi.sl.usermodel.PaintStyle.GradientPaint.GradientType.circular;
import static org.apache.poi.sl.usermodel.PaintStyle.GradientPaint.GradientType.linear;
import static org.apache.poi.sl.usermodel.PaintStyle.GradientPaint.GradientType.shape;
import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.DARKEN;
import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.DARKEN_LESS;
import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.LIGHTEN;
import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.LIGHTEN_LESS;
import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.NONE;
import static org.apache.poi.sl.usermodel.PaintStyle.PaintModifier.NORM;


public class DrawPaint {
	private static final POILogger LOG = POILogFactory.getLogger(DrawPaint.class);

	private static final Color TRANSPARENT = new Color(1.0F, 1.0F, 1.0F, 0.0F);

	protected PlaceableShape<?, ?> shape;

	public DrawPaint(PlaceableShape<?, ?> shape) {
		this.shape = shape;
	}

	private static class SimpleSolidPaint implements PaintStyle.SolidPaint {
		private final ColorStyle solidColor;

		SimpleSolidPaint(final Color color) {
			if (color == null) {
				throw new NullPointerException("Color needs to be specified");
			}
			this.solidColor = new AbstractColorStyle() {
				@Override
				public Color getColor() {
					return new Color(color.getRed(), color.getGreen(), color.getBlue());
				}

				@Override
				public int getAlpha() {
					return ((int) (Math.round((((color.getAlpha()) * 100000.0) / 255.0))));
				}

				@Override
				public int getHueOff() {
					return -1;
				}

				@Override
				public int getHueMod() {
					return -1;
				}

				@Override
				public int getSatOff() {
					return -1;
				}

				@Override
				public int getSatMod() {
					return -1;
				}

				@Override
				public int getLumOff() {
					return -1;
				}

				@Override
				public int getLumMod() {
					return -1;
				}

				@Override
				public int getShade() {
					return -1;
				}

				@Override
				public int getTint() {
					return -1;
				}
			};
		}

		SimpleSolidPaint(ColorStyle color) {
			if (color == null) {
				throw new NullPointerException("Color needs to be specified");
			}
			this.solidColor = color;
		}

		@Override
		public ColorStyle getSolidColor() {
			return solidColor;
		}

		@Override
		public boolean equals(Object o) {
			if ((this) == o) {
				return true;
			}
			if (!(o instanceof PaintStyle.SolidPaint)) {
				return false;
			}
			return Objects.equals(getSolidColor(), ((PaintStyle.SolidPaint) (o)).getSolidColor());
		}

		@Override
		public int hashCode() {
			return Objects.hash(solidColor);
		}
	}

	public static PaintStyle.SolidPaint createSolidPaint(final Color color) {
		return color == null ? null : new DrawPaint.SimpleSolidPaint(color);
	}

	public static PaintStyle.SolidPaint createSolidPaint(final ColorStyle color) {
		return color == null ? null : new DrawPaint.SimpleSolidPaint(color);
	}

	public Paint getPaint(Graphics2D graphics, PaintStyle paint) {
		return getPaint(graphics, paint, NORM);
	}

	public Paint getPaint(Graphics2D graphics, PaintStyle paint, PaintStyle.PaintModifier modifier) {
		if (modifier == (NONE)) {
			return null;
		}
		if (paint instanceof PaintStyle.SolidPaint) {
			return getSolidPaint(((PaintStyle.SolidPaint) (paint)), graphics, modifier);
		}else
			if (paint instanceof PaintStyle.GradientPaint) {
				return getGradientPaint(((PaintStyle.GradientPaint) (paint)), graphics);
			}else
				if (paint instanceof PaintStyle.TexturePaint) {
					return getTexturePaint(((PaintStyle.TexturePaint) (paint)), graphics);
				}


		return null;
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	protected Paint getSolidPaint(PaintStyle.SolidPaint fill, Graphics2D graphics, final PaintStyle.PaintModifier modifier) {
		final ColorStyle orig = fill.getSolidColor();
		ColorStyle cs = new AbstractColorStyle() {
			@Override
			public Color getColor() {
				return orig.getColor();
			}

			@Override
			public int getAlpha() {
				return orig.getAlpha();
			}

			@Override
			public int getHueOff() {
				return orig.getHueOff();
			}

			@Override
			public int getHueMod() {
				return orig.getHueMod();
			}

			@Override
			public int getSatOff() {
				return orig.getSatOff();
			}

			@Override
			public int getSatMod() {
				return orig.getSatMod();
			}

			@Override
			public int getLumOff() {
				return orig.getLumOff();
			}

			@Override
			public int getLumMod() {
				return orig.getLumMod();
			}

			@Override
			public int getShade() {
				return scale(orig.getShade(), DARKEN_LESS, DARKEN);
			}

			@Override
			public int getTint() {
				return scale(orig.getTint(), LIGHTEN_LESS, LIGHTEN);
			}

			private int scale(int value, PaintStyle.PaintModifier lessModifier, PaintStyle.PaintModifier moreModifier) {
				int delta = (modifier == lessModifier) ? 20000 : modifier == moreModifier ? 40000 : 0;
				return Math.min(100000, ((Math.max(0, value)) + delta));
			}
		};
		return DrawPaint.applyColorTransform(cs);
	}

	@SuppressWarnings("WeakerAccess")
	protected Paint getGradientPaint(PaintStyle.GradientPaint fill, Graphics2D graphics) {
		switch (fill.getGradientType()) {
			case linear :
				return createLinearGradientPaint(fill, graphics);
			case circular :
				return createRadialGradientPaint(fill, graphics);
			case shape :
				return createPathGradientPaint(fill, graphics);
			default :
				throw new UnsupportedOperationException((("gradient fill of type " + fill) + " not supported."));
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected Paint getTexturePaint(PaintStyle.TexturePaint fill, Graphics2D graphics) {
		InputStream is = fill.getImageData();
		if (is == null) {
			return null;
		}
		assert graphics != null;
		ImageRenderer renderer = DrawPictureShape.getImageRenderer(graphics, fill.getContentType());
		try {
			try {
				renderer.loadImage(is, fill.getContentType());
			} finally {
				is.close();
			}
		} catch (IOException e) {
			DrawPaint.LOG.log(POILogger.ERROR, "Can't load image data - using transparent color", e);
			return null;
		}
		int alpha = fill.getAlpha();
		if ((0 <= alpha) && (alpha < 100000)) {
			renderer.setAlpha((alpha / 100000.0F));
		}
		Rectangle2D textAnchor = shape.getAnchor();
		BufferedImage image;
		if ("image/x-wmf".equals(fill.getContentType())) {
			image = renderer.getImage(new Dimension(((int) (textAnchor.getWidth())), ((int) (textAnchor.getHeight()))));
		}else {
			image = renderer.getImage();
		}
		if (image == null) {
			DrawPaint.LOG.log(POILogger.ERROR, "Can't load image data");
			return null;
		}
		return new TexturePaint(image, textAnchor);
	}

	public static Color applyColorTransform(ColorStyle color) {
		if ((color == null) || ((color.getColor()) == null)) {
			return DrawPaint.TRANSPARENT;
		}
		Color result = color.getColor();
		double alpha = DrawPaint.getAlpha(result, color);
		double[] hsl = DrawPaint.RGB2HSL(result);
		DrawPaint.applyHslModOff(hsl, 0, color.getHueMod(), color.getHueOff());
		DrawPaint.applyHslModOff(hsl, 1, color.getSatMod(), color.getSatOff());
		DrawPaint.applyHslModOff(hsl, 2, color.getLumMod(), color.getLumOff());
		DrawPaint.applyShade(hsl, color);
		DrawPaint.applyTint(hsl, color);
		result = DrawPaint.HSL2RGB(hsl[0], hsl[1], hsl[2], alpha);
		return result;
	}

	private static double getAlpha(Color c, ColorStyle fc) {
		double alpha = (c.getAlpha()) / 255.0;
		int fcAlpha = fc.getAlpha();
		if (fcAlpha != (-1)) {
			alpha *= fcAlpha / 100000.0;
		}
		return Math.min(1, Math.max(0, alpha));
	}

	private static void applyHslModOff(double[] hsl, int hslPart, int mod, int off) {
		if (mod == (-1)) {
			mod = 100000;
		}
		if (off == (-1)) {
			off = 0;
		}
		if (!((mod == 100000) && (off == 0))) {
			double fOff = off / 1000.0;
			double fMod = mod / 100000.0;
			hsl[hslPart] = ((hsl[hslPart]) * fMod) + fOff;
		}
	}

	private static void applyShade(double[] hsl, ColorStyle fc) {
		int shade = fc.getShade();
		if (shade == (-1)) {
			return;
		}
		double shadePct = shade / 100000.0;
		hsl[2] *= 1.0 - shadePct;
	}

	private static void applyTint(double[] hsl, ColorStyle fc) {
		int tint = fc.getTint();
		if (tint == (-1)) {
			return;
		}
		double tintPct = tint / 100000.0;
		hsl[2] = ((hsl[2]) * (1.0 - tintPct)) + (100.0 - (100.0 * (1.0 - tintPct)));
	}

	@SuppressWarnings("WeakerAccess")
	protected Paint createLinearGradientPaint(PaintStyle.GradientPaint fill, Graphics2D graphics) {
		double angle = fill.getGradientAngle();
		if (!(fill.isRotatedWithShape())) {
			angle -= shape.getRotation();
		}
		Rectangle2D anchor = DrawShape.getAnchor(graphics, shape);
		AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians(angle), anchor.getCenterX(), anchor.getCenterY());
		double diagonal = Math.sqrt(((Math.pow(anchor.getWidth(), 2)) + (Math.pow(anchor.getHeight(), 2))));
		final Point2D p1 = at.transform(new Point2D.Double(((anchor.getCenterX()) - (diagonal / 2)), anchor.getCenterY()), null);
		final Point2D p2 = at.transform(new Point2D.Double(anchor.getMaxX(), anchor.getCenterY()), null);
		return p1.equals(p2) ? null : safeFractions(( f, c) -> new LinearGradientPaint(p1, p2, f, c), fill);
	}

	@SuppressWarnings("WeakerAccess")
	protected Paint createRadialGradientPaint(PaintStyle.GradientPaint fill, Graphics2D graphics) {
		Rectangle2D anchor = DrawShape.getAnchor(graphics, shape);
		final Point2D pCenter = new Point2D.Double(anchor.getCenterX(), anchor.getCenterY());
		final float radius = ((float) (Math.max(anchor.getWidth(), anchor.getHeight())));
		return safeFractions(( f, c) -> new RadialGradientPaint(pCenter, radius, f, c), fill);
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	protected Paint createPathGradientPaint(PaintStyle.GradientPaint fill, Graphics2D graphics) {
	}

	private Paint safeFractions(BiFunction<float[], Color[], Paint> init, PaintStyle.GradientPaint fill) {
		float[] fractions = fill.getGradientFractions();
		final ColorStyle[] styles = fill.getGradientColors();
		Map<Float, Color> m = new TreeMap<>();
		for (int i = 0; i < (fractions.length); i++) {
			m.put(fractions[i], ((styles[i]) == null ? DrawPaint.TRANSPARENT : DrawPaint.applyColorTransform(styles[i])));
		}
		final Color[] colors = new Color[m.size()];
		if ((fractions.length) != (m.size())) {
			fractions = new float[m.size()];
		}
		int i = 0;
		for (Map.Entry<Float, Color> me : m.entrySet()) {
			fractions[i] = me.getKey();
			colors[i] = me.getValue();
			i++;
		}
		return init.apply(fractions, colors);
	}

	public static Color HSL2RGB(double h, double s, double l, double alpha) {
		s = Math.max(0, Math.min(100, s));
		l = Math.max(0, Math.min(100, l));
		if ((alpha < 0.0F) || (alpha > 1.0F)) {
			String message = "Color parameter outside of expected range - Alpha: " + alpha;
			throw new IllegalArgumentException(message);
		}
		h = h % 360.0F;
		h /= 360.0F;
		s /= 100.0F;
		l /= 100.0F;
		double q = (l < 0.5) ? l * (1.0 + s) : (l + s) - (s * l);
		double p = (2.0 * l) - q;
		double r = Math.max(0, DrawPaint.HUE2RGB(p, q, (h + (1.0 / 3.0))));
		double g = Math.max(0, DrawPaint.HUE2RGB(p, q, h));
		double b = Math.max(0, DrawPaint.HUE2RGB(p, q, (h - (1.0 / 3.0))));
		r = Math.min(r, 1.0);
		g = Math.min(g, 1.0);
		b = Math.min(b, 1.0);
		return new Color(((float) (r)), ((float) (g)), ((float) (b)), ((float) (alpha)));
	}

	private static double HUE2RGB(double p, double q, double h) {
		if (h < 0.0) {
			h += 1.0;
		}
		if (h > 1.0) {
			h -= 1.0;
		}
		if ((6.0 * h) < 1.0) {
			return p + (((q - p) * 6.0) * h);
		}
		if ((2.0 * h) < 1.0) {
			return q;
		}
		if ((3.0 * h) < 2.0) {
			return p + (((q - p) * 6.0) * ((2.0 / 3.0) - h));
		}
		return p;
	}

	private static double[] RGB2HSL(Color color) {
		float[] rgb = color.getRGBColorComponents(null);
		double r = rgb[0];
		double g = rgb[1];
		double b = rgb[2];
		double min = Math.min(r, Math.min(g, b));
		double max = Math.max(r, Math.max(g, b));
		double h = 0;
		if (max == min) {
			h = 0;
		}else
			if (max == r) {
				h = (((60.0 * (g - b)) / (max - min)) + 360.0) % 360.0;
			}else
				if (max == g) {
					h = ((60.0 * (b - r)) / (max - min)) + 120.0;
				}else
					if (max == b) {
						h = ((60.0 * (r - g)) / (max - min)) + 240.0;
					}



		double l = (max + min) / 2.0;
		final double s;
		if (max == min) {
			s = 0;
		}else
			if (l <= 0.5) {
				s = (max - min) / (max + min);
			}else {
				s = (max - min) / ((2.0 - max) - min);
			}

		return new double[]{ h, s * 100, l * 100 };
	}

	public static int srgb2lin(float sRGB) {
		if (sRGB <= 0.04045) {
			return ((int) (Math.rint(((100000.0 * sRGB) / 12.92))));
		}else {
			return ((int) (Math.rint((100000.0 * (Math.pow(((sRGB + 0.055) / 1.055), 2.4))))));
		}
	}

	public static float lin2srgb(int linRGB) {
		if (linRGB <= 0.0031308) {
			return ((float) ((linRGB / 100000.0) * 12.92));
		}else {
			return ((float) ((1.055 * (Math.pow((linRGB / 100000.0), (1.0 / 2.4)))) - 0.055));
		}
	}
}

