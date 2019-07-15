

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import org.apache.poi.sl.usermodel.FreeformShape;
import org.apache.poi.util.Beta;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlTokenSource;
import org.openxmlformats.schemas.drawingml.x2006.main.CTAdjPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTAdjustHandleList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTConnectionSiteList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTCustomGeometry2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomGuideList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGeomRect;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingShapeProps;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPath2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPath2DClose;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPath2DCubicBezierTo;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPath2DLineTo;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPath2DList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPath2DMoveTo;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPath2DQuadBezierTo;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.presentationml.x2006.main.CTApplicationNonVisualDrawingProps;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShapeNonVisual;

import static org.openxmlformats.schemas.presentationml.x2006.main.CTShape.Factory.newInstance;


@Beta
public class XSLFFreeformShape extends XSLFAutoShape implements FreeformShape<XSLFShape, XSLFTextParagraph> {
	private static final POILogger LOG = POILogFactory.getLogger(XSLFFreeformShape.class);

	@Override
	public int setPath(final Path2D.Double path) {
		final CTPath2D ctPath = CTPath2D.Factory.newInstance();
		final Rectangle2D bounds = path.getBounds2D();
		final int x0 = Units.toEMU(bounds.getX());
		final int y0 = Units.toEMU(bounds.getY());
		final PathIterator it = path.getPathIterator(new AffineTransform());
		int numPoints = 0;
		ctPath.setH(Units.toEMU(bounds.getHeight()));
		ctPath.setW(Units.toEMU(bounds.getWidth()));
		final double[] vals = new double[6];
		while (!(it.isDone())) {
			final int type = it.currentSegment(vals);
			final CTAdjPoint2D[] points;
			switch (type) {
				case PathIterator.SEG_MOVETO :
					points = XSLFFreeformShape.addMoveTo(ctPath);
					break;
				case PathIterator.SEG_LINETO :
					points = XSLFFreeformShape.addLineTo(ctPath);
					break;
				case PathIterator.SEG_QUADTO :
					points = XSLFFreeformShape.addQuadBezierTo(ctPath);
					break;
				case PathIterator.SEG_CUBICTO :
					points = XSLFFreeformShape.addCubicBezierTo(ctPath);
					break;
				case PathIterator.SEG_CLOSE :
					points = XSLFFreeformShape.addClosePath(ctPath);
					break;
				default :
					{
						throw new IllegalStateException(("Unrecognized path segment type: " + type));
					}
			}
			int i = 0;
			for (final CTAdjPoint2D point : points) {
				point.setX(((Units.toEMU(vals[(i++)])) - x0));
				point.setY(((Units.toEMU(vals[(i++)])) - y0));
			}
			numPoints += Math.max(points.length, 1);
			it.next();
		} 
		XmlObject xo = getShapeProperties();
		if (!(xo instanceof CTShapeProperties)) {
			return -1;
		}
		((CTShapeProperties) (xo)).getCustGeom().getPathLst().setPathArray(new CTPath2D[]{ ctPath });
		setAnchor(bounds);
		return numPoints;
	}

	@Override
	public Path2D.Double getPath() {
		final Path2D.Double path = new Path2D.Double();
		final XmlObject xo = getShapeProperties();
		if (!(xo instanceof CTShapeProperties)) {
			return null;
		}
		final CTCustomGeometry2D geom = ((CTShapeProperties) (xo)).getCustGeom();
		for (CTPath2D spPath : geom.getPathLst().getPathArray()) {
			XmlCursor cursor = spPath.newCursor();
			try {
				if (cursor.toFirstChild()) {
					do {
						final XmlObject ch = cursor.getObject();
						if (ch instanceof CTPath2DMoveTo) {
							XSLFFreeformShape.addMoveTo(path, ((CTPath2DMoveTo) (ch)));
						}else
							if (ch instanceof CTPath2DLineTo) {
								XSLFFreeformShape.addLineTo(path, ((CTPath2DLineTo) (ch)));
							}else
								if (ch instanceof CTPath2DQuadBezierTo) {
									XSLFFreeformShape.addQuadBezierTo(path, ((CTPath2DQuadBezierTo) (ch)));
								}else
									if (ch instanceof CTPath2DCubicBezierTo) {
										XSLFFreeformShape.addCubicBezierTo(path, ((CTPath2DCubicBezierTo) (ch)));
									}else
										if (ch instanceof CTPath2DClose) {
											XSLFFreeformShape.addClosePath(path);
										}else {
											XSLFFreeformShape.LOG.log(POILogger.WARN, ("can't handle path of type " + (xo.getClass())));
										}




					} while (cursor.toNextSibling() );
				}
			} finally {
				cursor.dispose();
			}
		}
		final AffineTransform at = new AffineTransform();
		final CTTransform2D xfrm = getXfrm(false);
		final Rectangle2D xfrm2d = new Rectangle2D.Double(xfrm.getOff().getX(), xfrm.getOff().getY(), xfrm.getExt().getCx(), xfrm.getExt().getCy());
		final Rectangle2D bounds = getAnchor();
		at.translate(((bounds.getX()) + (bounds.getCenterX())), ((bounds.getY()) + (bounds.getCenterY())));
		at.scale((1.0 / (Units.EMU_PER_POINT)), (1.0 / (Units.EMU_PER_POINT)));
		at.translate((-(xfrm2d.getCenterX())), (-(xfrm2d.getCenterY())));
		return new Path2D.Double(at.createTransformedShape(path));
	}

	private static CTAdjPoint2D[] addMoveTo(final CTPath2D path) {
		return new CTAdjPoint2D[]{ path.addNewMoveTo().addNewPt() };
	}

	private static void addMoveTo(final Path2D path, final CTPath2DMoveTo xo) {
		final CTAdjPoint2D pt = xo.getPt();
		path.moveTo(((Long) (pt.getX())), ((Long) (pt.getY())));
	}

	private static CTAdjPoint2D[] addLineTo(final CTPath2D path) {
		return new CTAdjPoint2D[]{ path.addNewLnTo().addNewPt() };
	}

	private static void addLineTo(final Path2D path, final CTPath2DLineTo xo) {
		final CTAdjPoint2D pt = xo.getPt();
		path.lineTo(((Long) (pt.getX())), ((Long) (pt.getY())));
	}

	private static CTAdjPoint2D[] addQuadBezierTo(final CTPath2D path) {
		final CTPath2DQuadBezierTo bez = path.addNewQuadBezTo();
		return new CTAdjPoint2D[]{ bez.addNewPt(), bez.addNewPt() };
	}

	private static void addQuadBezierTo(final Path2D path, final CTPath2DQuadBezierTo xo) {
		final CTAdjPoint2D pt1 = xo.getPtArray(0);
		final CTAdjPoint2D pt2 = xo.getPtArray(1);
		path.quadTo(((Long) (pt1.getX())), ((Long) (pt1.getY())), ((Long) (pt2.getX())), ((Long) (pt2.getY())));
	}

	private static CTAdjPoint2D[] addCubicBezierTo(final CTPath2D path) {
		final CTPath2DCubicBezierTo bez = path.addNewCubicBezTo();
		return new CTAdjPoint2D[]{ bez.addNewPt(), bez.addNewPt(), bez.addNewPt() };
	}

	private static void addCubicBezierTo(final Path2D path, final CTPath2DCubicBezierTo xo) {
		final CTAdjPoint2D pt1 = xo.getPtArray(0);
		final CTAdjPoint2D pt2 = xo.getPtArray(1);
		final CTAdjPoint2D pt3 = xo.getPtArray(2);
		path.curveTo(((Long) (pt1.getX())), ((Long) (pt1.getY())), ((Long) (pt2.getX())), ((Long) (pt2.getY())), ((Long) (pt3.getX())), ((Long) (pt3.getY())));
	}

	private static CTAdjPoint2D[] addClosePath(final CTPath2D path) {
		path.addNewClose();
		return new CTAdjPoint2D[0];
	}

	private static void addClosePath(final Path2D path) {
		path.closePath();
	}

	static CTShape prototype(int shapeId) {
		CTShape ct = newInstance();
		CTShapeNonVisual nvSpPr = ct.addNewNvSpPr();
		CTNonVisualDrawingProps cnv = nvSpPr.addNewCNvPr();
		cnv.setName(("Freeform " + shapeId));
		cnv.setId(shapeId);
		nvSpPr.addNewCNvSpPr();
		nvSpPr.addNewNvPr();
		CTShapeProperties spPr = ct.addNewSpPr();
		CTCustomGeometry2D geom = spPr.addNewCustGeom();
		geom.addNewAvLst();
		geom.addNewGdLst();
		geom.addNewAhLst();
		geom.addNewCxnLst();
		CTGeomRect rect = geom.addNewRect();
		rect.setR("r");
		rect.setB("b");
		rect.setT("t");
		rect.setL("l");
		geom.addNewPathLst();
		return ct;
	}
}

