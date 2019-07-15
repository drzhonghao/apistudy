

import java.awt.Color;
import java.util.LinkedHashMap;
import org.apache.poi.ddf.AbstractEscherOptRecord;
import org.apache.poi.ddf.EscherChildAnchorRecord;
import org.apache.poi.ddf.EscherClientAnchorRecord;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherProperties;
import org.apache.poi.ddf.EscherProperty;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.hslf.exceptions.HSLFException;
import org.apache.poi.hslf.usermodel.HSLFFill;
import org.apache.poi.hslf.usermodel.HSLFHyperlink;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFShapePlaceholderDetails;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.sl.draw.DrawPaint;
import org.apache.poi.sl.draw.geom.CustomGeometry;
import org.apache.poi.sl.draw.geom.Guide;
import org.apache.poi.sl.draw.geom.PresetGeometries;
import org.apache.poi.sl.usermodel.LineDecoration;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.Shadow;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.SimpleShape;
import org.apache.poi.sl.usermodel.StrokeStyle;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;

import static org.apache.poi.sl.usermodel.LineDecoration.DecorationSize.fromNativeId;
import static org.apache.poi.sl.usermodel.StrokeStyle.LineCap.FLAT;
import static org.apache.poi.sl.usermodel.StrokeStyle.LineCompound.SINGLE;
import static org.apache.poi.sl.usermodel.StrokeStyle.LineDash.SOLID;


public abstract class HSLFSimpleShape extends HSLFShape implements SimpleShape<HSLFShape, HSLFTextParagraph> {
	private static final POILogger LOG = POILogFactory.getLogger(HSLFSimpleShape.class);

	public static final double DEFAULT_LINE_WIDTH = 0.75;

	protected HSLFHyperlink _hyperlink;

	protected HSLFSimpleShape(EscherContainerRecord escherRecord, ShapeContainer<HSLFShape, HSLFTextParagraph> parent) {
		super(escherRecord, parent);
	}

	@Override
	protected EscherContainerRecord createSpContainer(boolean isChild) {
		EscherContainerRecord ecr = super.createSpContainer(isChild);
		ecr.setRecordId(EscherContainerRecord.SP_CONTAINER);
		EscherSpRecord sp = new EscherSpRecord();
		int flags = (EscherSpRecord.FLAG_HAVEANCHOR) | (EscherSpRecord.FLAG_HASSHAPETYPE);
		if (isChild) {
			flags |= EscherSpRecord.FLAG_CHILD;
		}
		sp.setFlags(flags);
		ecr.addChildRecord(sp);
		AbstractEscherOptRecord opt = new EscherOptRecord();
		opt.setRecordId(EscherOptRecord.RECORD_ID);
		ecr.addChildRecord(opt);
		EscherRecord anchor;
		if (isChild) {
			anchor = new EscherChildAnchorRecord();
		}else {
			anchor = new EscherClientAnchorRecord();
			byte[] header = new byte[16];
			LittleEndian.putUShort(header, 0, 0);
			LittleEndian.putUShort(header, 2, 0);
			LittleEndian.putInt(header, 4, 8);
			anchor.fillFields(header, 0, null);
		}
		ecr.addChildRecord(anchor);
		return ecr;
	}

	public double getLineWidth() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__LINEWIDTH);
		return prop == null ? HSLFSimpleShape.DEFAULT_LINE_WIDTH : Units.toPoints(prop.getPropertyValue());
	}

	public void setLineWidth(double width) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__LINEWIDTH, Units.toEMU(width));
	}

	public void setLineColor(Color color) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		if (color == null) {
			HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__NOLINEDRAWDASH, 524288);
		}else {
			int rgb = new Color(color.getBlue(), color.getGreen(), color.getRed(), 0).getRGB();
			HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__COLOR, rgb);
			HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__NOLINEDRAWDASH, 1572888);
		}
	}

	public Color getLineColor() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty p = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__NOLINEDRAWDASH);
		if ((p != null) && (((p.getPropertyValue()) & 8) == 0)) {
			return null;
		}
		return null;
	}

	public Color getLineBackgroundColor() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty p = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__NOLINEDRAWDASH);
		if ((p != null) && (((p.getPropertyValue()) & 8) == 0)) {
			return null;
		}
		return null;
	}

	public void setLineBackgroundColor(Color color) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		if (color == null) {
			HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__NOLINEDRAWDASH, 524288);
			opt.removeEscherProperty(EscherProperties.LINESTYLE__BACKCOLOR);
		}else {
			int rgb = new Color(color.getBlue(), color.getGreen(), color.getRed(), 0).getRGB();
			HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__BACKCOLOR, rgb);
			HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__NOLINEDRAWDASH, 1572888);
		}
	}

	public StrokeStyle.LineCap getLineCap() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__LINEENDCAPSTYLE);
		return prop == null ? FLAT : StrokeStyle.LineCap.fromNativeId(prop.getPropertyValue());
	}

	public void setLineCap(StrokeStyle.LineCap pen) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__LINEENDCAPSTYLE, (pen == (FLAT) ? -1 : pen.nativeId));
	}

	public StrokeStyle.LineDash getLineDash() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__LINEDASHING);
		return prop == null ? SOLID : StrokeStyle.LineDash.fromNativeId(prop.getPropertyValue());
	}

	public void setLineDash(StrokeStyle.LineDash pen) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__LINEDASHING, (pen == (SOLID) ? -1 : pen.nativeId));
	}

	public StrokeStyle.LineCompound getLineCompound() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__LINESTYLE);
		return prop == null ? SINGLE : StrokeStyle.LineCompound.fromNativeId(prop.getPropertyValue());
	}

	public void setLineCompound(StrokeStyle.LineCompound style) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__LINESTYLE, (style == (SINGLE) ? -1 : style.nativeId));
	}

	@Override
	public StrokeStyle getStrokeStyle() {
		return new StrokeStyle() {
			@Override
			public PaintStyle getPaint() {
				return DrawPaint.createSolidPaint(HSLFSimpleShape.this.getLineColor());
			}

			@Override
			public StrokeStyle.LineCap getLineCap() {
				return null;
			}

			@Override
			public StrokeStyle.LineDash getLineDash() {
				return HSLFSimpleShape.this.getLineDash();
			}

			@Override
			public StrokeStyle.LineCompound getLineCompound() {
				return HSLFSimpleShape.this.getLineCompound();
			}

			@Override
			public double getLineWidth() {
				return HSLFSimpleShape.this.getLineWidth();
			}
		};
	}

	@Override
	public Color getFillColor() {
		return getFill().getForegroundColor();
	}

	@Override
	public void setFillColor(Color color) {
		getFill().setForegroundColor(color);
	}

	@Override
	public Guide getAdjustValue(String name) {
		if ((name == null) || (!(name.matches("adj([1-9]|10)?")))) {
			HSLFSimpleShape.LOG.log(POILogger.INFO, (("Adjust value '" + name) + "' not supported. Using default value."));
			return null;
		}
		name = name.replace("adj", "");
		if (name.isEmpty()) {
			name = "1";
		}
		short escherProp;
		switch (Integer.parseInt(name)) {
			case 1 :
				escherProp = EscherProperties.GEOMETRY__ADJUSTVALUE;
				break;
			case 2 :
				escherProp = EscherProperties.GEOMETRY__ADJUST2VALUE;
				break;
			case 3 :
				escherProp = EscherProperties.GEOMETRY__ADJUST3VALUE;
				break;
			case 4 :
				escherProp = EscherProperties.GEOMETRY__ADJUST4VALUE;
				break;
			case 5 :
				escherProp = EscherProperties.GEOMETRY__ADJUST5VALUE;
				break;
			case 6 :
				escherProp = EscherProperties.GEOMETRY__ADJUST6VALUE;
				break;
			case 7 :
				escherProp = EscherProperties.GEOMETRY__ADJUST7VALUE;
				break;
			case 8 :
				escherProp = EscherProperties.GEOMETRY__ADJUST8VALUE;
				break;
			case 9 :
				escherProp = EscherProperties.GEOMETRY__ADJUST9VALUE;
				break;
			case 10 :
				escherProp = EscherProperties.GEOMETRY__ADJUST10VALUE;
				break;
			default :
				throw new HSLFException();
		}
		int adjval = getEscherProperty(escherProp, (-1));
		return adjval == (-1) ? null : new Guide(name, ("val " + adjval));
	}

	@Override
	public CustomGeometry getGeometry() {
		PresetGeometries dict = PresetGeometries.getInstance();
		ShapeType st = getShapeType();
		String name = (st != null) ? st.getOoxmlName() : null;
		CustomGeometry geom = dict.get(name);
		if (geom == null) {
			if (name == null) {
				name = (st != null) ? st.toString() : "<unknown>";
			}
			HSLFSimpleShape.LOG.log(POILogger.WARN, ("No preset shape definition for shapeType: " + name));
		}
		return geom;
	}

	public double getShadowAngle() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.SHADOWSTYLE__OFFSETX);
		int offX = (prop == null) ? 0 : prop.getPropertyValue();
		prop = HSLFShape.getEscherProperty(opt, EscherProperties.SHADOWSTYLE__OFFSETY);
		int offY = (prop == null) ? 0 : prop.getPropertyValue();
		return Math.toDegrees(Math.atan2(offY, offX));
	}

	public double getShadowDistance() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.SHADOWSTYLE__OFFSETX);
		int offX = (prop == null) ? 0 : prop.getPropertyValue();
		prop = HSLFShape.getEscherProperty(opt, EscherProperties.SHADOWSTYLE__OFFSETY);
		int offY = (prop == null) ? 0 : prop.getPropertyValue();
		return Units.toPoints(((long) (Math.hypot(offX, offY))));
	}

	public Color getShadowColor() {
		return null;
	}

	@Override
	public Shadow<HSLFShape, HSLFTextParagraph> getShadow() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		if (opt == null) {
			return null;
		}
		EscherProperty shadowType = opt.lookup(EscherProperties.SHADOWSTYLE__TYPE);
		if (shadowType == null) {
			return null;
		}
		return new Shadow<HSLFShape, HSLFTextParagraph>() {
			@Override
			public SimpleShape<HSLFShape, HSLFTextParagraph> getShadowParent() {
				return HSLFSimpleShape.this;
			}

			@Override
			public double getDistance() {
				return getShadowDistance();
			}

			@Override
			public double getAngle() {
				return getShadowAngle();
			}

			@Override
			public double getBlur() {
				return 0;
			}

			@Override
			public PaintStyle.SolidPaint getFillStyle() {
				return DrawPaint.createSolidPaint(getShadowColor());
			}
		};
	}

	public LineDecoration.DecorationShape getLineHeadDecoration() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__LINESTARTARROWHEAD);
		return prop == null ? null : LineDecoration.DecorationShape.fromNativeId(prop.getPropertyValue());
	}

	public void setLineHeadDecoration(LineDecoration.DecorationShape decoShape) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__LINESTARTARROWHEAD, (decoShape == null ? -1 : decoShape.nativeId));
	}

	public LineDecoration.DecorationSize getLineHeadWidth() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__LINESTARTARROWWIDTH);
		return prop == null ? null : fromNativeId(prop.getPropertyValue());
	}

	public void setLineHeadWidth(LineDecoration.DecorationSize decoSize) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__LINESTARTARROWWIDTH, (decoSize == null ? -1 : decoSize.nativeId));
	}

	public LineDecoration.DecorationSize getLineHeadLength() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__LINESTARTARROWLENGTH);
		return prop == null ? null : fromNativeId(prop.getPropertyValue());
	}

	public void setLineHeadLength(LineDecoration.DecorationSize decoSize) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__LINESTARTARROWLENGTH, (decoSize == null ? -1 : decoSize.nativeId));
	}

	public LineDecoration.DecorationShape getLineTailDecoration() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__LINEENDARROWHEAD);
		return prop == null ? null : LineDecoration.DecorationShape.fromNativeId(prop.getPropertyValue());
	}

	public void setLineTailDecoration(LineDecoration.DecorationShape decoShape) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__LINEENDARROWHEAD, (decoShape == null ? -1 : decoShape.nativeId));
	}

	public LineDecoration.DecorationSize getLineTailWidth() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__LINEENDARROWWIDTH);
		return prop == null ? null : fromNativeId(prop.getPropertyValue());
	}

	public void setLineTailWidth(LineDecoration.DecorationSize decoSize) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__LINEENDARROWWIDTH, (decoSize == null ? -1 : decoSize.nativeId));
	}

	public LineDecoration.DecorationSize getLineTailLength() {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		EscherSimpleProperty prop = HSLFShape.getEscherProperty(opt, EscherProperties.LINESTYLE__LINEENDARROWLENGTH);
		return prop == null ? null : fromNativeId(prop.getPropertyValue());
	}

	public void setLineTailLength(LineDecoration.DecorationSize decoSize) {
		AbstractEscherOptRecord opt = getEscherOptRecord();
		HSLFShape.setEscherProperty(opt, EscherProperties.LINESTYLE__LINEENDARROWLENGTH, (decoSize == null ? -1 : decoSize.nativeId));
	}

	@Override
	public LineDecoration getLineDecoration() {
		return new LineDecoration() {
			@Override
			public LineDecoration.DecorationShape getHeadShape() {
				return HSLFSimpleShape.this.getLineHeadDecoration();
			}

			@Override
			public LineDecoration.DecorationSize getHeadWidth() {
				return HSLFSimpleShape.this.getLineHeadWidth();
			}

			@Override
			public LineDecoration.DecorationSize getHeadLength() {
				return HSLFSimpleShape.this.getLineHeadLength();
			}

			@Override
			public LineDecoration.DecorationShape getTailShape() {
				return HSLFSimpleShape.this.getLineTailDecoration();
			}

			@Override
			public LineDecoration.DecorationSize getTailWidth() {
				return HSLFSimpleShape.this.getLineTailWidth();
			}

			@Override
			public LineDecoration.DecorationSize getTailLength() {
				return HSLFSimpleShape.this.getLineTailLength();
			}
		};
	}

	@Override
	public HSLFShapePlaceholderDetails getPlaceholderDetails() {
		return null;
	}

	@Override
	public Placeholder getPlaceholder() {
		return getPlaceholderDetails().getPlaceholder();
	}

	@Override
	public void setPlaceholder(Placeholder placeholder) {
		getPlaceholderDetails().setPlaceholder(placeholder);
	}

	@Override
	public void setStrokeStyle(Object... styles) {
		if ((styles.length) == 0) {
			setLineColor(null);
			return;
		}
		for (Object st : styles) {
			if (st instanceof Number) {
				setLineWidth(((Number) (st)).doubleValue());
			}else
				if (st instanceof StrokeStyle.LineCap) {
					setLineCap(((StrokeStyle.LineCap) (st)));
				}else
					if (st instanceof StrokeStyle.LineDash) {
						setLineDash(((StrokeStyle.LineDash) (st)));
					}else
						if (st instanceof StrokeStyle.LineCompound) {
							setLineCompound(((StrokeStyle.LineCompound) (st)));
						}else
							if (st instanceof Color) {
								setLineColor(((Color) (st)));
							}




		}
	}

	@Override
	public HSLFHyperlink getHyperlink() {
		return _hyperlink;
	}

	@Override
	public HSLFHyperlink createHyperlink() {
		if ((_hyperlink) == null) {
		}
		return _hyperlink;
	}

	protected void setHyperlink(HSLFHyperlink link) {
		_hyperlink = link;
	}

	@Override
	public boolean isPlaceholder() {
		return false;
	}
}

