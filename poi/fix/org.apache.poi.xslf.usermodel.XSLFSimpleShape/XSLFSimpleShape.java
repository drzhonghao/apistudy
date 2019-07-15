

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.sl.draw.DrawPaint;
import org.apache.poi.sl.draw.geom.CustomGeometry;
import org.apache.poi.sl.draw.geom.Guide;
import org.apache.poi.sl.draw.geom.PresetGeometries;
import org.apache.poi.sl.usermodel.ColorStyle;
import org.apache.poi.sl.usermodel.FillStyle;
import org.apache.poi.sl.usermodel.LineDecoration;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.SimpleShape;
import org.apache.poi.sl.usermodel.StrokeStyle;
import org.apache.poi.util.Beta;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.model.PropertyFetcher;
import org.apache.poi.xslf.usermodel.XSLFColor;
import org.apache.poi.xslf.usermodel.XSLFHyperlink;
import org.apache.poi.xslf.usermodel.XSLFShadow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTheme;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBaseStyles;
import org.openxmlformats.schemas.drawingml.x2006.main.CTEffectList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTEffectStyleItem;
import org.openxmlformats.schemas.drawingml.x2006.main.CTEffectStyleList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineEndProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineStyleList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNoFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOfficeStyleSheet;
import org.openxmlformats.schemas.drawingml.x2006.main.CTOuterShadowEffect;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPresetLineDashProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSchemeColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeStyle;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTStyleMatrix;
import org.openxmlformats.schemas.drawingml.x2006.main.CTStyleMatrixReference;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.main.STCompoundLine;
import org.openxmlformats.schemas.drawingml.x2006.main.STLineCap;
import org.openxmlformats.schemas.drawingml.x2006.main.STLineEndLength;
import org.openxmlformats.schemas.drawingml.x2006.main.STLineEndType;
import org.openxmlformats.schemas.drawingml.x2006.main.STLineEndWidth;
import org.openxmlformats.schemas.drawingml.x2006.main.STPresetLineDashVal;

import static org.apache.poi.sl.usermodel.LineDecoration.DecorationShape.NONE;
import static org.apache.poi.sl.usermodel.LineDecoration.DecorationSize.MEDIUM;
import static org.apache.poi.sl.usermodel.LineDecoration.DecorationSize.fromOoxmlId;
import static org.apache.poi.sl.usermodel.StrokeStyle.LineCompound.DOUBLE;
import static org.apache.poi.sl.usermodel.StrokeStyle.LineCompound.SINGLE;
import static org.apache.poi.sl.usermodel.StrokeStyle.LineCompound.THICK_THIN;
import static org.apache.poi.sl.usermodel.StrokeStyle.LineCompound.THIN_THICK;
import static org.apache.poi.sl.usermodel.StrokeStyle.LineCompound.TRIPLE;
import static org.openxmlformats.schemas.drawingml.x2006.main.CTOuterShadowEffect.Factory.newInstance;


@Beta
public abstract class XSLFSimpleShape extends XSLFShape implements SimpleShape<XSLFShape, XSLFTextParagraph> {
	private static CTOuterShadowEffect NO_SHADOW = newInstance();

	private static final POILogger LOG = POILogFactory.getLogger(XSLFSimpleShape.class);

	XSLFSimpleShape(XmlObject shape, XSLFSheet sheet) {
		super(shape, sheet);
	}

	@Override
	public void setShapeType(ShapeType type) {
	}

	@Override
	public ShapeType getShapeType() {
		return null;
	}

	protected CTTransform2D getXfrm(boolean create) {
		PropertyFetcher<CTTransform2D> fetcher = new PropertyFetcher<CTTransform2D>() {
			@Override
			public boolean fetch(XSLFShape shape) {
				return false;
			}
		};
		fetchShapeProperty(fetcher);
		CTTransform2D xfrm = fetcher.getValue();
		if ((!create) || (xfrm != null)) {
			return xfrm;
		}else {
			XmlObject xo = getShapeProperties();
			if (xo instanceof CTShapeProperties) {
				return ((CTShapeProperties) (xo)).addNewXfrm();
			}else {
				XSLFSimpleShape.LOG.log(POILogger.WARN, ((getClass()) + " doesn't have xfrm element."));
				return null;
			}
		}
	}

	@Override
	public Rectangle2D getAnchor() {
		CTTransform2D xfrm = getXfrm(false);
		if (xfrm == null) {
			return null;
		}
		CTPoint2D off = xfrm.getOff();
		double x = Units.toPoints(off.getX());
		double y = Units.toPoints(off.getY());
		CTPositiveSize2D ext = xfrm.getExt();
		double cx = Units.toPoints(ext.getCx());
		double cy = Units.toPoints(ext.getCy());
		return new Rectangle2D.Double(x, y, cx, cy);
	}

	@Override
	public void setAnchor(Rectangle2D anchor) {
		CTTransform2D xfrm = getXfrm(true);
		if (xfrm == null) {
			return;
		}
		CTPoint2D off = (xfrm.isSetOff()) ? xfrm.getOff() : xfrm.addNewOff();
		long x = Units.toEMU(anchor.getX());
		long y = Units.toEMU(anchor.getY());
		off.setX(x);
		off.setY(y);
		CTPositiveSize2D ext = (xfrm.isSetExt()) ? xfrm.getExt() : xfrm.addNewExt();
		long cx = Units.toEMU(anchor.getWidth());
		long cy = Units.toEMU(anchor.getHeight());
		ext.setCx(cx);
		ext.setCy(cy);
	}

	@Override
	public void setRotation(double theta) {
		CTTransform2D xfrm = getXfrm(true);
		if (xfrm != null) {
			xfrm.setRot(((int) (theta * 60000)));
		}
	}

	@Override
	public double getRotation() {
		CTTransform2D xfrm = getXfrm(false);
		return (xfrm == null) || (!(xfrm.isSetRot())) ? 0 : (xfrm.getRot()) / 60000.0;
	}

	@Override
	public void setFlipHorizontal(boolean flip) {
		CTTransform2D xfrm = getXfrm(true);
		if (xfrm != null) {
			xfrm.setFlipH(flip);
		}
	}

	@Override
	public void setFlipVertical(boolean flip) {
		CTTransform2D xfrm = getXfrm(true);
		if (xfrm != null) {
			xfrm.setFlipV(flip);
		}
	}

	@Override
	public boolean getFlipHorizontal() {
		CTTransform2D xfrm = getXfrm(false);
		return ((xfrm != null) && (xfrm.isSetFlipH())) && (xfrm.getFlipH());
	}

	@Override
	public boolean getFlipVertical() {
		CTTransform2D xfrm = getXfrm(false);
		return ((xfrm != null) && (xfrm.isSetFlipV())) && (xfrm.getFlipV());
	}

	private CTLineProperties getDefaultLineProperties() {
		CTShapeStyle style = getSpStyle();
		if (style == null) {
			return null;
		}
		CTStyleMatrixReference lnRef = style.getLnRef();
		if (lnRef == null) {
			return null;
		}
		int idx = ((int) (lnRef.getIdx()));
		XSLFTheme theme = getSheet().getTheme();
		if (theme == null) {
			return null;
		}
		CTBaseStyles styles = theme.getXmlObject().getThemeElements();
		if (styles == null) {
			return null;
		}
		CTStyleMatrix styleMatrix = styles.getFmtScheme();
		if (styleMatrix == null) {
			return null;
		}
		CTLineStyleList lineStyles = styleMatrix.getLnStyleLst();
		if ((lineStyles == null) || ((lineStyles.sizeOfLnArray()) < idx)) {
			return null;
		}
		return lineStyles.getLnArray((idx - 1));
	}

	public void setLineColor(Color color) {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, true);
		if (ln == null) {
			return;
		}
		if (ln.isSetSolidFill()) {
			ln.unsetSolidFill();
		}
		if (ln.isSetGradFill()) {
			ln.unsetGradFill();
		}
		if (ln.isSetPattFill()) {
			ln.unsetPattFill();
		}
		if (ln.isSetNoFill()) {
			ln.unsetNoFill();
		}
		if (color == null) {
			ln.addNewNoFill();
		}else {
			CTSolidColorFillProperties fill = ln.addNewSolidFill();
			XSLFColor col = new XSLFColor(fill, getSheet().getTheme(), fill.getSchemeClr());
		}
	}

	@SuppressWarnings("WeakerAccess")
	public Color getLineColor() {
		PaintStyle ps = getLinePaint();
		if (ps instanceof PaintStyle.SolidPaint) {
			return ((PaintStyle.SolidPaint) (ps)).getSolidColor().getColor();
		}
		return null;
	}

	@SuppressWarnings("WeakerAccess")
	protected PaintStyle getLinePaint() {
		XSLFSheet sheet = getSheet();
		final XSLFTheme theme = sheet.getTheme();
		final boolean hasPlaceholder = (getPlaceholder()) != null;
		PropertyFetcher<PaintStyle> fetcher = new PropertyFetcher<PaintStyle>() {
			@Override
			public boolean fetch(XSLFShape shape) {
				CTLineProperties spPr = XSLFSimpleShape.getLn(shape, false);
				PackagePart pp = shape.getSheet().getPackagePart();
				return false;
			}

			PaintStyle getThemePaint(CTShapeStyle style, PackagePart pp) {
				CTStyleMatrixReference lnRef = style.getLnRef();
				if (lnRef == null) {
					return null;
				}
				int idx = ((int) (lnRef.getIdx()));
				CTSchemeColor phClr = lnRef.getSchemeClr();
				if (idx <= 0) {
					return null;
				}
				CTLineProperties props = theme.getXmlObject().getThemeElements().getFmtScheme().getLnStyleLst().getLnArray((idx - 1));
				return null;
			}
		};
		fetchShapeProperty(fetcher);
		return fetcher.getValue();
	}

	@SuppressWarnings("WeakerAccess")
	public void setLineWidth(double width) {
		CTLineProperties lnPr = XSLFSimpleShape.getLn(this, true);
		if (lnPr == null) {
			return;
		}
		if (width == 0.0) {
			if (lnPr.isSetW()) {
				lnPr.unsetW();
			}
			if (!(lnPr.isSetNoFill())) {
				lnPr.addNewNoFill();
			}
			if (lnPr.isSetSolidFill()) {
				lnPr.unsetSolidFill();
			}
			if (lnPr.isSetGradFill()) {
				lnPr.unsetGradFill();
			}
			if (lnPr.isSetPattFill()) {
				lnPr.unsetPattFill();
			}
		}else {
			if (lnPr.isSetNoFill()) {
				lnPr.unsetNoFill();
			}
			lnPr.setW(Units.toEMU(width));
		}
	}

	@SuppressWarnings("WeakerAccess")
	public double getLineWidth() {
		PropertyFetcher<Double> fetcher = new PropertyFetcher<Double>() {
			@Override
			public boolean fetch(XSLFShape shape) {
				CTLineProperties ln = XSLFSimpleShape.getLn(shape, false);
				if (ln != null) {
					if (ln.isSetNoFill()) {
						setValue(0.0);
						return true;
					}
					if (ln.isSetW()) {
						setValue(Units.toPoints(ln.getW()));
						return true;
					}
				}
				return false;
			}
		};
		fetchShapeProperty(fetcher);
		double lineWidth = 0;
		if ((fetcher.getValue()) == null) {
			CTLineProperties defaultLn = getDefaultLineProperties();
			if (defaultLn != null) {
				if (defaultLn.isSetW()) {
					lineWidth = Units.toPoints(defaultLn.getW());
				}
			}
		}else {
			lineWidth = fetcher.getValue();
		}
		return lineWidth;
	}

	@SuppressWarnings("WeakerAccess")
	public void setLineCompound(StrokeStyle.LineCompound compound) {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, true);
		if (ln == null) {
			return;
		}
		if (compound == null) {
			if (ln.isSetCmpd()) {
				ln.unsetCmpd();
			}
		}else {
			STCompoundLine.Enum xCmpd;
			switch (compound) {
				default :
				case SINGLE :
					xCmpd = STCompoundLine.SNG;
					break;
				case DOUBLE :
					xCmpd = STCompoundLine.DBL;
					break;
				case THICK_THIN :
					xCmpd = STCompoundLine.THICK_THIN;
					break;
				case THIN_THICK :
					xCmpd = STCompoundLine.THIN_THICK;
					break;
				case TRIPLE :
					xCmpd = STCompoundLine.TRI;
					break;
			}
			ln.setCmpd(xCmpd);
		}
	}

	@SuppressWarnings("WeakerAccess")
	public StrokeStyle.LineCompound getLineCompound() {
		PropertyFetcher<Integer> fetcher = new PropertyFetcher<Integer>() {
			@Override
			public boolean fetch(XSLFShape shape) {
				CTLineProperties ln = XSLFSimpleShape.getLn(shape, false);
				if (ln != null) {
					STCompoundLine.Enum stCmpd = ln.getCmpd();
					if (stCmpd != null) {
						setValue(stCmpd.intValue());
						return true;
					}
				}
				return false;
			}
		};
		fetchShapeProperty(fetcher);
		Integer cmpd = fetcher.getValue();
		if (cmpd == null) {
			CTLineProperties defaultLn = getDefaultLineProperties();
			if ((defaultLn != null) && (defaultLn.isSetCmpd())) {
				switch (defaultLn.getCmpd().intValue()) {
					default :
					case STCompoundLine.INT_SNG :
						return SINGLE;
					case STCompoundLine.INT_DBL :
						return DOUBLE;
					case STCompoundLine.INT_THICK_THIN :
						return THICK_THIN;
					case STCompoundLine.INT_THIN_THICK :
						return THIN_THICK;
					case STCompoundLine.INT_TRI :
						return TRIPLE;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("WeakerAccess")
	public void setLineDash(StrokeStyle.LineDash dash) {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, true);
		if (ln == null) {
			return;
		}
		if (dash == null) {
			if (ln.isSetPrstDash()) {
				ln.unsetPrstDash();
			}
		}else {
			CTPresetLineDashProperties ldp = (ln.isSetPrstDash()) ? ln.getPrstDash() : ln.addNewPrstDash();
			ldp.setVal(STPresetLineDashVal.Enum.forInt(dash.ooxmlId));
		}
	}

	@SuppressWarnings("WeakerAccess")
	public StrokeStyle.LineDash getLineDash() {
		PropertyFetcher<StrokeStyle.LineDash> fetcher = new PropertyFetcher<StrokeStyle.LineDash>() {
			@Override
			public boolean fetch(XSLFShape shape) {
				CTLineProperties ln = XSLFSimpleShape.getLn(shape, false);
				if ((ln == null) || (!(ln.isSetPrstDash()))) {
					return false;
				}
				setValue(StrokeStyle.LineDash.fromOoxmlId(ln.getPrstDash().getVal().intValue()));
				return true;
			}
		};
		fetchShapeProperty(fetcher);
		StrokeStyle.LineDash dash = fetcher.getValue();
		if (dash == null) {
			CTLineProperties defaultLn = getDefaultLineProperties();
			if ((defaultLn != null) && (defaultLn.isSetPrstDash())) {
				dash = StrokeStyle.LineDash.fromOoxmlId(defaultLn.getPrstDash().getVal().intValue());
			}
		}
		return dash;
	}

	@SuppressWarnings("WeakerAccess")
	public void setLineCap(StrokeStyle.LineCap cap) {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, true);
		if (ln == null) {
			return;
		}
		if (cap == null) {
			if (ln.isSetCap()) {
				ln.unsetCap();
			}
		}else {
			ln.setCap(STLineCap.Enum.forInt(cap.ooxmlId));
		}
	}

	@SuppressWarnings("WeakerAccess")
	public StrokeStyle.LineCap getLineCap() {
		PropertyFetcher<StrokeStyle.LineCap> fetcher = new PropertyFetcher<StrokeStyle.LineCap>() {
			@Override
			public boolean fetch(XSLFShape shape) {
				CTLineProperties ln = XSLFSimpleShape.getLn(shape, false);
				if ((ln != null) && (ln.isSetCap())) {
					setValue(StrokeStyle.LineCap.fromOoxmlId(ln.getCap().intValue()));
					return true;
				}
				return false;
			}
		};
		fetchShapeProperty(fetcher);
		StrokeStyle.LineCap cap = fetcher.getValue();
		if (cap == null) {
			CTLineProperties defaultLn = getDefaultLineProperties();
			if ((defaultLn != null) && (defaultLn.isSetCap())) {
				cap = StrokeStyle.LineCap.fromOoxmlId(defaultLn.getCap().intValue());
			}
		}
		return cap;
	}

	@Override
	public void setFillColor(Color color) {
		if (color == null) {
		}else {
		}
	}

	@Override
	public Color getFillColor() {
		PaintStyle ps = getFillPaint();
		if (ps instanceof PaintStyle.SolidPaint) {
			return DrawPaint.applyColorTransform(((PaintStyle.SolidPaint) (ps)).getSolidColor());
		}
		return null;
	}

	@Override
	public XSLFShadow getShadow() {
		PropertyFetcher<CTOuterShadowEffect> fetcher = new PropertyFetcher<CTOuterShadowEffect>() {
			@Override
			public boolean fetch(XSLFShape shape) {
				return false;
			}
		};
		fetchShapeProperty(fetcher);
		CTOuterShadowEffect obj = fetcher.getValue();
		if (obj == null) {
			CTShapeStyle style = getSpStyle();
			if ((style != null) && ((style.getEffectRef()) != null)) {
				int idx = ((int) (style.getEffectRef().getIdx()));
				if (idx != 0) {
					CTStyleMatrix styleMatrix = getSheet().getTheme().getXmlObject().getThemeElements().getFmtScheme();
					CTEffectStyleItem ef = styleMatrix.getEffectStyleLst().getEffectStyleArray((idx - 1));
					obj = ef.getEffectLst().getOuterShdw();
				}
			}
		}
		return null;
	}

	@Override
	public CustomGeometry getGeometry() {
		CustomGeometry geom;
		PresetGeometries dict = PresetGeometries.getInstance();
		geom = null;
		return geom;
	}

	void copy(XSLFShape sh) {
		XSLFSimpleShape s = ((XSLFSimpleShape) (sh));
		Color srsSolidFill = s.getFillColor();
		Color tgtSoliFill = getFillColor();
		if ((srsSolidFill != null) && (!(srsSolidFill.equals(tgtSoliFill)))) {
			setFillColor(srsSolidFill);
		}
		Color srcLineColor = s.getLineColor();
		Color tgtLineColor = getLineColor();
		if ((srcLineColor != null) && (!(srcLineColor.equals(tgtLineColor)))) {
			setLineColor(srcLineColor);
		}
		double srcLineWidth = s.getLineWidth();
		double tgtLineWidth = getLineWidth();
		if (srcLineWidth != tgtLineWidth) {
			setLineWidth(srcLineWidth);
		}
		StrokeStyle.LineDash srcLineDash = s.getLineDash();
		StrokeStyle.LineDash tgtLineDash = getLineDash();
		if ((srcLineDash != null) && (srcLineDash != tgtLineDash)) {
			setLineDash(srcLineDash);
		}
		StrokeStyle.LineCap srcLineCap = s.getLineCap();
		StrokeStyle.LineCap tgtLineCap = getLineCap();
		if ((srcLineCap != null) && (srcLineCap != tgtLineCap)) {
			setLineCap(srcLineCap);
		}
	}

	@SuppressWarnings("WeakerAccess")
	public void setLineHeadDecoration(LineDecoration.DecorationShape style) {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, true);
		if (ln == null) {
			return;
		}
		CTLineEndProperties lnEnd = (ln.isSetHeadEnd()) ? ln.getHeadEnd() : ln.addNewHeadEnd();
		if (style == null) {
			if (lnEnd.isSetType()) {
				lnEnd.unsetType();
			}
		}else {
			lnEnd.setType(STLineEndType.Enum.forInt(style.ooxmlId));
		}
	}

	@SuppressWarnings("WeakerAccess")
	public LineDecoration.DecorationShape getLineHeadDecoration() {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, false);
		LineDecoration.DecorationShape ds = NONE;
		if (((ln != null) && (ln.isSetHeadEnd())) && (ln.getHeadEnd().isSetType())) {
			ds = LineDecoration.DecorationShape.fromOoxmlId(ln.getHeadEnd().getType().intValue());
		}
		return ds;
	}

	@SuppressWarnings("WeakerAccess")
	public void setLineHeadWidth(LineDecoration.DecorationSize style) {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, true);
		if (ln == null) {
			return;
		}
		CTLineEndProperties lnEnd = (ln.isSetHeadEnd()) ? ln.getHeadEnd() : ln.addNewHeadEnd();
		if (style == null) {
			if (lnEnd.isSetW()) {
				lnEnd.unsetW();
			}
		}else {
			lnEnd.setW(STLineEndWidth.Enum.forInt(style.ooxmlId));
		}
	}

	@SuppressWarnings("WeakerAccess")
	public LineDecoration.DecorationSize getLineHeadWidth() {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, false);
		LineDecoration.DecorationSize ds = MEDIUM;
		if (((ln != null) && (ln.isSetHeadEnd())) && (ln.getHeadEnd().isSetW())) {
			ds = fromOoxmlId(ln.getHeadEnd().getW().intValue());
		}
		return ds;
	}

	@SuppressWarnings("WeakerAccess")
	public void setLineHeadLength(LineDecoration.DecorationSize style) {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, true);
		if (ln == null) {
			return;
		}
		CTLineEndProperties lnEnd = (ln.isSetHeadEnd()) ? ln.getHeadEnd() : ln.addNewHeadEnd();
		if (style == null) {
			if (lnEnd.isSetLen()) {
				lnEnd.unsetLen();
			}
		}else {
			lnEnd.setLen(STLineEndLength.Enum.forInt(style.ooxmlId));
		}
	}

	@SuppressWarnings("WeakerAccess")
	public LineDecoration.DecorationSize getLineHeadLength() {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, false);
		LineDecoration.DecorationSize ds = MEDIUM;
		if (((ln != null) && (ln.isSetHeadEnd())) && (ln.getHeadEnd().isSetLen())) {
			ds = fromOoxmlId(ln.getHeadEnd().getLen().intValue());
		}
		return ds;
	}

	@SuppressWarnings("WeakerAccess")
	public void setLineTailDecoration(LineDecoration.DecorationShape style) {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, true);
		if (ln == null) {
			return;
		}
		CTLineEndProperties lnEnd = (ln.isSetTailEnd()) ? ln.getTailEnd() : ln.addNewTailEnd();
		if (style == null) {
			if (lnEnd.isSetType()) {
				lnEnd.unsetType();
			}
		}else {
			lnEnd.setType(STLineEndType.Enum.forInt(style.ooxmlId));
		}
	}

	@SuppressWarnings("WeakerAccess")
	public LineDecoration.DecorationShape getLineTailDecoration() {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, false);
		LineDecoration.DecorationShape ds = NONE;
		if (((ln != null) && (ln.isSetTailEnd())) && (ln.getTailEnd().isSetType())) {
			ds = LineDecoration.DecorationShape.fromOoxmlId(ln.getTailEnd().getType().intValue());
		}
		return ds;
	}

	@SuppressWarnings("WeakerAccess")
	public void setLineTailWidth(LineDecoration.DecorationSize style) {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, true);
		if (ln == null) {
			return;
		}
		CTLineEndProperties lnEnd = (ln.isSetTailEnd()) ? ln.getTailEnd() : ln.addNewTailEnd();
		if (style == null) {
			if (lnEnd.isSetW()) {
				lnEnd.unsetW();
			}
		}else {
			lnEnd.setW(STLineEndWidth.Enum.forInt(style.ooxmlId));
		}
	}

	@SuppressWarnings("WeakerAccess")
	public LineDecoration.DecorationSize getLineTailWidth() {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, false);
		LineDecoration.DecorationSize ds = MEDIUM;
		if (((ln != null) && (ln.isSetTailEnd())) && (ln.getTailEnd().isSetW())) {
			ds = fromOoxmlId(ln.getTailEnd().getW().intValue());
		}
		return ds;
	}

	@SuppressWarnings("WeakerAccess")
	public void setLineTailLength(LineDecoration.DecorationSize style) {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, true);
		if (ln == null) {
			return;
		}
		CTLineEndProperties lnEnd = (ln.isSetTailEnd()) ? ln.getTailEnd() : ln.addNewTailEnd();
		if (style == null) {
			if (lnEnd.isSetLen()) {
				lnEnd.unsetLen();
			}
		}else {
			lnEnd.setLen(STLineEndLength.Enum.forInt(style.ooxmlId));
		}
	}

	@SuppressWarnings("WeakerAccess")
	public LineDecoration.DecorationSize getLineTailLength() {
		CTLineProperties ln = XSLFSimpleShape.getLn(this, false);
		LineDecoration.DecorationSize ds = MEDIUM;
		if (((ln != null) && (ln.isSetTailEnd())) && (ln.getTailEnd().isSetLen())) {
			ds = fromOoxmlId(ln.getTailEnd().getLen().intValue());
		}
		return ds;
	}

	@Override
	public Guide getAdjustValue(String name) {
		return null;
	}

	@Override
	public LineDecoration getLineDecoration() {
		return new LineDecoration() {
			@Override
			public LineDecoration.DecorationShape getHeadShape() {
				return getLineHeadDecoration();
			}

			@Override
			public LineDecoration.DecorationSize getHeadWidth() {
				return getLineHeadWidth();
			}

			@Override
			public LineDecoration.DecorationSize getHeadLength() {
				return getLineHeadLength();
			}

			@Override
			public LineDecoration.DecorationShape getTailShape() {
				return getLineTailDecoration();
			}

			@Override
			public LineDecoration.DecorationSize getTailWidth() {
				return getLineTailWidth();
			}

			@Override
			public LineDecoration.DecorationSize getTailLength() {
				return getLineTailLength();
			}
		};
	}

	@Override
	public FillStyle getFillStyle() {
		return this::getFillPaint;
	}

	@Override
	public StrokeStyle getStrokeStyle() {
		return new StrokeStyle() {
			@Override
			public PaintStyle getPaint() {
				return XSLFSimpleShape.this.getLinePaint();
			}

			@Override
			public StrokeStyle.LineCap getLineCap() {
				return XSLFSimpleShape.this.getLineCap();
			}

			@Override
			public StrokeStyle.LineDash getLineDash() {
				return XSLFSimpleShape.this.getLineDash();
			}

			@Override
			public double getLineWidth() {
				return XSLFSimpleShape.this.getLineWidth();
			}

			@Override
			public StrokeStyle.LineCompound getLineCompound() {
				return XSLFSimpleShape.this.getLineCompound();
			}
		};
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
	public XSLFHyperlink getHyperlink() {
		CTNonVisualDrawingProps cNvPr = getCNvPr();
		if (!(cNvPr.isSetHlinkClick())) {
			return null;
		}
		return null;
	}

	@Override
	public XSLFHyperlink createHyperlink() {
		XSLFHyperlink hl = getHyperlink();
		if (hl == null) {
			CTNonVisualDrawingProps cNvPr = getCNvPr();
		}
		return hl;
	}

	private static CTLineProperties getLn(XSLFShape shape, boolean create) {
		return null;
	}
}

