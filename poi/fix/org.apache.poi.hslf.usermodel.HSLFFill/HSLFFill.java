

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.EscherArrayProperty;
import org.apache.poi.ddf.EscherBSERecord;
import org.apache.poi.ddf.EscherColorRef;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.hslf.record.Document;
import org.apache.poi.hslf.record.PPDrawingGroup;
import org.apache.poi.hslf.record.RecordTypes;
import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSheet;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.sl.draw.DrawPaint;
import org.apache.poi.sl.usermodel.ColorStyle;
import org.apache.poi.sl.usermodel.FillStyle;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.util.BitField;
import org.apache.poi.util.BitFieldFactory;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;


public final class HSLFFill {
	private static final POILogger LOG = POILogFactory.getLogger(HSLFFill.class);

	public static final int FILL_SOLID = 0;

	public static final int FILL_PATTERN = 1;

	public static final int FILL_TEXTURE = 2;

	public static final int FILL_PICTURE = 3;

	public static final int FILL_SHADE = 4;

	public static final int FILL_SHADE_CENTER = 5;

	public static final int FILL_SHADE_SHAPE = 6;

	public static final int FILL_SHADE_SCALE = 7;

	public static final int FILL_SHADE_TITLE = 8;

	public static final int FILL_BACKGROUND = 9;

	private static final BitField FILL_USE_RECOLOR_FILL_AS_PICTURE = BitFieldFactory.getInstance(4194304);

	private static final BitField FILL_USE_USE_SHAPE_ANCHOR = BitFieldFactory.getInstance(2097152);

	private static final BitField FILL_USE_FILLED = BitFieldFactory.getInstance(1048576);

	private static final BitField FILL_USE_HIT_TEST_FILL = BitFieldFactory.getInstance(524288);

	private static final BitField FILL_USE_FILL_SHAPE = BitFieldFactory.getInstance(262144);

	private static final BitField FILL_USE_FILL_USE_RECT = BitFieldFactory.getInstance(131072);

	private static final BitField FILL_USE_NO_FILL_HIT_TEST = BitFieldFactory.getInstance(65536);

	private static final BitField FILL_RECOLOR_FILL_AS_PICTURE = BitFieldFactory.getInstance(64);

	private static final BitField FILL_USE_SHAPE_ANCHOR = BitFieldFactory.getInstance(32);

	private static final BitField FILL_FILLED = BitFieldFactory.getInstance(16);

	private static final BitField FILL_HIT_TEST_FILL = BitFieldFactory.getInstance(8);

	private static final BitField FILL_FILL_SHAPE = BitFieldFactory.getInstance(4);

	private static final BitField FILL_FILL_USE_RECT = BitFieldFactory.getInstance(2);

	private static final BitField FILL_NO_FILL_HIT_TEST = BitFieldFactory.getInstance(1);

	private HSLFShape shape;

	public HSLFFill(HSLFShape shape) {
		this.shape = shape;
	}

	public FillStyle getFillStyle() {
		return new FillStyle() {
			@Override
			public PaintStyle getPaint() {
				final int fillType = getFillType();
				return null;
			}
		};
	}

	private PaintStyle.GradientPaint getGradientPaint(final PaintStyle.GradientPaint.GradientType gradientType) {
		AbstractEscherOptRecord opt = shape.getEscherOptRecord();
		final EscherArrayProperty ep = HSLFShape.getEscherProperty(opt, EscherProperties.FILL__SHADECOLORS);
		final int colorCnt = (ep == null) ? 0 : ep.getNumberOfElementsInArray();
		opt = shape.getEscherChild(RecordTypes.EscherUserDefined);
		EscherSimpleProperty p = HSLFShape.getEscherProperty(opt, EscherProperties.FILL__NOFILLHITTEST);
		int propVal = (p == null) ? 0 : p.getPropertyValue();
		final boolean rotateWithShape = (HSLFFill.FILL_USE_USE_SHAPE_ANCHOR.isSet(propVal)) && (HSLFFill.FILL_USE_SHAPE_ANCHOR.isSet(propVal));
		return new PaintStyle.GradientPaint() {
			@Override
			public double getGradientAngle() {
				int rot = shape.getEscherProperty(EscherProperties.FILL__ANGLE);
				return 90 - (Units.fixedPointToDouble(rot));
			}

			@Override
			public ColorStyle[] getGradientColors() {
				ColorStyle[] cs;
				if (colorCnt == 0) {
					cs = new ColorStyle[2];
					cs[0] = wrapColor(getBackgroundColor());
					cs[1] = wrapColor(getForegroundColor());
				}else {
					cs = new ColorStyle[colorCnt];
					int idx = 0;
					for (byte[] data : ep) {
						EscherColorRef ecr = new EscherColorRef(data, 0, 4);
					}
				}
				return cs;
			}

			private ColorStyle wrapColor(Color col) {
				return col == null ? null : DrawPaint.createSolidPaint(col).getSolidColor();
			}

			@Override
			public float[] getGradientFractions() {
				float[] frc;
				if (colorCnt == 0) {
					frc = new float[]{ 0, 1 };
				}else {
					frc = new float[colorCnt];
					int idx = 0;
					for (byte[] data : ep) {
						double pos = Units.fixedPointToDouble(LittleEndian.getInt(data, 4));
						frc[(idx++)] = ((float) (pos));
					}
				}
				return frc;
			}

			@Override
			public boolean isRotatedWithShape() {
				return rotateWithShape;
			}

			@Override
			public PaintStyle.GradientPaint.GradientType getGradientType() {
				return gradientType;
			}
		};
	}

	private PaintStyle.TexturePaint getTexturePaint() {
		final HSLFPictureData pd = getPictureData();
		if (pd == null) {
			return null;
		}
		return new PaintStyle.TexturePaint() {
			@Override
			public InputStream getImageData() {
				return new ByteArrayInputStream(pd.getData());
			}

			@Override
			public String getContentType() {
				return pd.getContentType();
			}

			@Override
			public int getAlpha() {
				return 0;
			}
		};
	}

	public int getFillType() {
		AbstractEscherOptRecord opt = shape.getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.FILL__FILLTYPE);
		return prop == null ? HSLFFill.FILL_SOLID : prop.getPropertyValue();
	}

	protected void afterInsert(HSLFSheet sh) {
		AbstractEscherOptRecord opt = shape.getEscherOptRecord();
		EscherSimpleProperty p = HSLFShape.getEscherProperty(opt, EscherProperties.FILL__PATTERNTEXTURE);
		if (p != null) {
			int idx = p.getPropertyValue();
			EscherBSERecord bse = getEscherBSERecord(idx);
			if (bse != null) {
				bse.setRef(((bse.getRef()) + 1));
			}
		}
	}

	@SuppressWarnings("resource")
	protected EscherBSERecord getEscherBSERecord(int idx) {
		HSLFSheet sheet = shape.getSheet();
		if (sheet == null) {
			HSLFFill.LOG.log(POILogger.DEBUG, "Fill has not yet been assigned to a sheet");
			return null;
		}
		HSLFSlideShow ppt = sheet.getSlideShow();
		Document doc = ppt.getDocumentRecord();
		EscherContainerRecord dggContainer = doc.getPPDrawingGroup().getDggContainer();
		EscherContainerRecord bstore = HSLFShape.getEscherChild(dggContainer, EscherContainerRecord.BSTORE_CONTAINER);
		if (bstore == null) {
			HSLFFill.LOG.log(POILogger.DEBUG, "EscherContainerRecord.BSTORE_CONTAINER was not found ");
			return null;
		}
		List<EscherRecord> lst = bstore.getChildRecords();
		return ((EscherBSERecord) (lst.get((idx - 1))));
	}

	public void setFillType(int type) {
		AbstractEscherOptRecord opt = shape.getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.FILL__FILLTYPE, type);
	}

	public Color getForegroundColor() {
		AbstractEscherOptRecord opt = shape.getEscherOptRecord();
		EscherSimpleProperty p = HSLFShape.getEscherProperty(opt, EscherProperties.FILL__NOFILLHITTEST);
		int propVal = (p == null) ? 0 : p.getPropertyValue();
		return null;
	}

	public void setForegroundColor(Color color) {
		AbstractEscherOptRecord opt = shape.getEscherOptRecord();
		opt.removeEscherProperty(EscherProperties.FILL__FILLOPACITY);
		opt.removeEscherProperty(EscherProperties.FILL__FILLCOLOR);
		if (color != null) {
			int rgb = new Color(color.getBlue(), color.getGreen(), color.getRed(), 0).getRGB();
			HSLFShape.setEscherProperty(opt, EscherProperties.FILL__FILLCOLOR, rgb);
			int alpha = color.getAlpha();
			if (alpha < 255) {
				int alphaFP = Units.doubleToFixedPoint((alpha / 255.0));
				HSLFShape.setEscherProperty(opt, EscherProperties.FILL__FILLOPACITY, alphaFP);
			}
		}
		EscherSimpleProperty p = HSLFShape.getEscherProperty(opt, EscherProperties.FILL__NOFILLHITTEST);
		int propVal = (p == null) ? 0 : p.getPropertyValue();
		propVal = HSLFFill.FILL_FILLED.setBoolean(propVal, (color != null));
		propVal = HSLFFill.FILL_NO_FILL_HIT_TEST.setBoolean(propVal, (color != null));
		propVal = HSLFFill.FILL_USE_FILLED.set(propVal);
		propVal = HSLFFill.FILL_USE_FILL_SHAPE.set(propVal);
		propVal = HSLFFill.FILL_USE_NO_FILL_HIT_TEST.set(propVal);
		propVal = HSLFFill.FILL_FILL_SHAPE.clear(propVal);
		HSLFShape.setEscherProperty(opt, EscherProperties.FILL__NOFILLHITTEST, propVal);
	}

	public Color getBackgroundColor() {
		AbstractEscherOptRecord opt = shape.getEscherOptRecord();
		EscherSimpleProperty p = HSLFShape.getEscherProperty(opt, EscherProperties.FILL__NOFILLHITTEST);
		int propVal = (p == null) ? 0 : p.getPropertyValue();
		return null;
	}

	public void setBackgroundColor(Color color) {
		AbstractEscherOptRecord opt = shape.getEscherOptRecord();
		if (color == null) {
			HSLFShape.setEscherProperty(opt, EscherProperties.FILL__FILLBACKCOLOR, (-1));
		}else {
			int rgb = new Color(color.getBlue(), color.getGreen(), color.getRed(), 0).getRGB();
			HSLFShape.setEscherProperty(opt, EscherProperties.FILL__FILLBACKCOLOR, rgb);
		}
	}

	@SuppressWarnings("resource")
	public HSLFPictureData getPictureData() {
		AbstractEscherOptRecord opt = shape.getEscherOptRecord();
		EscherSimpleProperty p = HSLFShape.getEscherProperty(opt, EscherProperties.FILL__PATTERNTEXTURE);
		if (p == null) {
			return null;
		}
		HSLFSlideShow ppt = shape.getSheet().getSlideShow();
		List<HSLFPictureData> pict = ppt.getPictureData();
		Document doc = ppt.getDocumentRecord();
		EscherContainerRecord dggContainer = doc.getPPDrawingGroup().getDggContainer();
		EscherContainerRecord bstore = HSLFShape.getEscherChild(dggContainer, EscherContainerRecord.BSTORE_CONTAINER);
		List<EscherRecord> lst = bstore.getChildRecords();
		int idx = p.getPropertyValue();
		if (idx == 0) {
			HSLFFill.LOG.log(POILogger.WARN, "no reference to picture data found ");
		}else {
			EscherBSERecord bse = ((EscherBSERecord) (lst.get((idx - 1))));
			for (HSLFPictureData pd : pict) {
				if ((pd.getOffset()) == (bse.getOffset())) {
					return pd;
				}
			}
		}
		return null;
	}

	public void setPictureData(HSLFPictureData data) {
		AbstractEscherOptRecord opt = shape.getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, ((short) ((EscherProperties.FILL__PATTERNTEXTURE) + 16384)), (data == null ? 0 : data.getIndex()));
		if ((data != null) && ((shape.getSheet()) != null)) {
			EscherBSERecord bse = getEscherBSERecord(data.getIndex());
			if (bse != null) {
				bse.setRef(((bse.getRef()) + 1));
			}
		}
	}
}

