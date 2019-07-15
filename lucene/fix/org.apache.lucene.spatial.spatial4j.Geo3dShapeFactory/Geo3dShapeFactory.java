

import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2Point;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.spatial.prefix.tree.S2ShapeFactory;
import org.apache.lucene.spatial.spatial4j.Geo3dCircleShape;
import org.apache.lucene.spatial.spatial4j.Geo3dPointShape;
import org.apache.lucene.spatial.spatial4j.Geo3dRectangleShape;
import org.apache.lucene.spatial.spatial4j.Geo3dShape;
import org.apache.lucene.spatial.spatial4j.Geo3dSpatialContextFactory;
import org.apache.lucene.spatial3d.geom.GeoBBox;
import org.apache.lucene.spatial3d.geom.GeoBBoxFactory;
import org.apache.lucene.spatial3d.geom.GeoBaseCompositeShape;
import org.apache.lucene.spatial3d.geom.GeoCircle;
import org.apache.lucene.spatial3d.geom.GeoCircleFactory;
import org.apache.lucene.spatial3d.geom.GeoCompositeAreaShape;
import org.apache.lucene.spatial3d.geom.GeoPath;
import org.apache.lucene.spatial3d.geom.GeoPathFactory;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.GeoPointShape;
import org.apache.lucene.spatial3d.geom.GeoPointShapeFactory;
import org.apache.lucene.spatial3d.geom.GeoPolygon;
import org.apache.lucene.spatial3d.geom.GeoPolygonFactory;
import org.apache.lucene.spatial3d.geom.GeoS2ShapeFactory;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.exception.InvalidShapeException;
import org.locationtech.spatial4j.shape.Circle;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.ShapeCollection;
import org.locationtech.spatial4j.shape.ShapeFactory;


public class Geo3dShapeFactory implements S2ShapeFactory {
	private final boolean normWrapLongitude;

	private SpatialContext context;

	private PlanetModel planetModel;

	private static final double DEFAULT_CIRCLE_ACCURACY = 1.0E-4;

	private double circleAccuracy = Geo3dShapeFactory.DEFAULT_CIRCLE_ACCURACY;

	@SuppressWarnings("unchecked")
	public Geo3dShapeFactory(SpatialContext context, SpatialContextFactory factory) {
		this.context = context;
		this.planetModel = ((Geo3dSpatialContextFactory) (factory)).planetModel;
		this.normWrapLongitude = (context.isGeo()) && (factory.normWrapLongitude);
	}

	@Override
	public SpatialContext getSpatialContext() {
		return context;
	}

	public void setCircleAccuracy(double circleAccuracy) {
		this.circleAccuracy = circleAccuracy;
	}

	@Override
	public boolean isNormWrapLongitude() {
		return normWrapLongitude;
	}

	@Override
	public double normX(double x) {
		if (this.normWrapLongitude) {
			x = DistanceUtils.normLonDEG(x);
		}
		return x;
	}

	@Override
	public double normY(double y) {
		return y;
	}

	@Override
	public double normZ(double z) {
		return z;
	}

	@Override
	public double normDist(double distance) {
		return distance;
	}

	@Override
	public void verifyX(double x) {
		Rectangle bounds = this.context.getWorldBounds();
		if ((x < (bounds.getMinX())) || (x > (bounds.getMaxX()))) {
			throw new InvalidShapeException(((("Bad X value " + x) + " is not in boundary ") + bounds));
		}
	}

	@Override
	public void verifyY(double y) {
		Rectangle bounds = this.context.getWorldBounds();
		if ((y < (bounds.getMinY())) || (y > (bounds.getMaxY()))) {
			throw new InvalidShapeException(((("Bad Y value " + y) + " is not in boundary ") + bounds));
		}
	}

	@Override
	public void verifyZ(double v) {
	}

	@Override
	public Point pointXY(double x, double y) {
		GeoPointShape point = GeoPointShapeFactory.makeGeoPointShape(planetModel, (y * (DistanceUtils.DEGREES_TO_RADIANS)), (x * (DistanceUtils.DEGREES_TO_RADIANS)));
		return new Geo3dPointShape(point, context);
	}

	@Override
	public Point pointXYZ(double x, double y, double z) {
		GeoPoint point = new GeoPoint(x, y, z);
		GeoPointShape pointShape = GeoPointShapeFactory.makeGeoPointShape(planetModel, point.getLatitude(), point.getLongitude());
		return new Geo3dPointShape(pointShape, context);
	}

	@Override
	public Rectangle rect(Point point, Point point1) {
		return rect(point.getX(), point1.getX(), point.getY(), point1.getY());
	}

	@Override
	public Rectangle rect(double minX, double maxX, double minY, double maxY) {
		GeoBBox bBox = GeoBBoxFactory.makeGeoBBox(planetModel, (maxY * (DistanceUtils.DEGREES_TO_RADIANS)), (minY * (DistanceUtils.DEGREES_TO_RADIANS)), (minX * (DistanceUtils.DEGREES_TO_RADIANS)), (maxX * (DistanceUtils.DEGREES_TO_RADIANS)));
		return new Geo3dRectangleShape(bBox, context, minX, maxX, minY, maxY);
	}

	@Override
	public Circle circle(double x, double y, double distance) {
		GeoCircle circle;
		if (planetModel.isSphere()) {
			circle = GeoCircleFactory.makeGeoCircle(planetModel, (y * (DistanceUtils.DEGREES_TO_RADIANS)), (x * (DistanceUtils.DEGREES_TO_RADIANS)), (distance * (DistanceUtils.DEGREES_TO_RADIANS)));
		}else {
			circle = GeoCircleFactory.makeExactGeoCircle(planetModel, (y * (DistanceUtils.DEGREES_TO_RADIANS)), (x * (DistanceUtils.DEGREES_TO_RADIANS)), (distance * (DistanceUtils.DEGREES_TO_RADIANS)), ((circleAccuracy) * (DistanceUtils.DEGREES_TO_RADIANS)));
		}
		return new Geo3dCircleShape(circle, context);
	}

	@Override
	public Circle circle(Point point, double distance) {
		return circle(point.getX(), point.getY(), distance);
	}

	@Override
	public Shape lineString(List<Point> list, double distance) {
		ShapeFactory.LineStringBuilder builder = lineString();
		for (Point point : list) {
			builder.pointXY(point.getX(), point.getY());
		}
		builder.buffer(distance);
		return builder.build();
	}

	@Override
	public <S extends Shape> ShapeCollection<S> multiShape(List<S> list) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ShapeFactory.LineStringBuilder lineString() {
		return new Geo3dShapeFactory.Geo3dLineStringBuilder();
	}

	@Override
	public ShapeFactory.PolygonBuilder polygon() {
		return new Geo3dShapeFactory.Geo3dPolygonBuilder();
	}

	@Override
	public <T extends Shape> ShapeFactory.MultiShapeBuilder<T> multiShape(Class<T> aClass) {
		return new Geo3dShapeFactory.Geo3dMultiShapeBuilder<>();
	}

	@Override
	public ShapeFactory.MultiPointBuilder multiPoint() {
		return new Geo3dShapeFactory.Geo3dMultiPointBuilder();
	}

	@Override
	public ShapeFactory.MultiLineStringBuilder multiLineString() {
		return new Geo3dShapeFactory.Geo3dMultiLineBuilder();
	}

	@Override
	public ShapeFactory.MultiPolygonBuilder multiPolygon() {
		return new Geo3dShapeFactory.Geo3dMultiPolygonBuilder();
	}

	@Override
	public Shape getS2CellShape(S2CellId cellId) {
		S2Cell cell = new S2Cell(cellId);
		GeoPoint point1 = getGeoPoint(cell.getVertexRaw(0));
		GeoPoint point2 = getGeoPoint(cell.getVertexRaw(1));
		GeoPoint point3 = getGeoPoint(cell.getVertexRaw(2));
		GeoPoint point4 = getGeoPoint(cell.getVertexRaw(3));
		return new Geo3dShape<>(GeoS2ShapeFactory.makeGeoS2Shape(planetModel, point1, point2, point3, point4), context);
	}

	private GeoPoint getGeoPoint(S2Point point) {
		return planetModel.createSurfacePoint(point.get(0), point.get(1), point.get(2));
	}

	private class Geo3dPointBuilder<T> implements ShapeFactory.PointsBuilder<T> {
		List<GeoPoint> points = new ArrayList<>();

		@SuppressWarnings("unchecked")
		@Override
		public T pointXY(double x, double y) {
			GeoPoint point = new GeoPoint(planetModel, (y * (DistanceUtils.DEGREES_TO_RADIANS)), (x * (DistanceUtils.DEGREES_TO_RADIANS)));
			points.add(point);
			return ((T) (this));
		}

		@SuppressWarnings("unchecked")
		@Override
		public T pointXYZ(double x, double y, double z) {
			GeoPoint point = new GeoPoint(x, y, z);
			if (!(points.contains(point))) {
				points.add(point);
			}
			return ((T) (this));
		}
	}

	private class Geo3dLineStringBuilder extends Geo3dShapeFactory.Geo3dPointBuilder<ShapeFactory.LineStringBuilder> implements ShapeFactory.LineStringBuilder {
		double distance = 0;

		@Override
		public ShapeFactory.LineStringBuilder buffer(double distance) {
			this.distance = distance;
			return this;
		}

		@Override
		public Shape build() {
			GeoPath path = GeoPathFactory.makeGeoPath(planetModel, distance, points.toArray(new GeoPoint[points.size()]));
			return new Geo3dShape<>(path, context);
		}
	}

	private class Geo3dPolygonBuilder extends Geo3dShapeFactory.Geo3dPointBuilder<ShapeFactory.PolygonBuilder> implements ShapeFactory.PolygonBuilder {
		List<GeoPolygonFactory.PolygonDescription> polyHoles = new ArrayList<>();

		@Override
		public ShapeFactory.PolygonBuilder.HoleBuilder hole() {
			return new Geo3dShapeFactory.Geo3dPolygonBuilder.Geo3dHoleBuilder();
		}

		class Geo3dHoleBuilder extends Geo3dShapeFactory.Geo3dPointBuilder<ShapeFactory.PolygonBuilder.HoleBuilder> implements ShapeFactory.PolygonBuilder.HoleBuilder {
			@Override
			public ShapeFactory.PolygonBuilder endHole() {
				polyHoles.add(new GeoPolygonFactory.PolygonDescription(points));
				return Geo3dShapeFactory.Geo3dPolygonBuilder.this;
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public Shape build() {
			GeoPolygonFactory.PolygonDescription description = new GeoPolygonFactory.PolygonDescription(points, polyHoles);
			GeoPolygon polygon = GeoPolygonFactory.makeGeoPolygon(planetModel, description);
			return new Geo3dShape<>(polygon, context);
		}

		@Override
		public Shape buildOrRect() {
			return build();
		}
	}

	private class Geo3dMultiPointBuilder extends Geo3dShapeFactory.Geo3dPointBuilder<ShapeFactory.MultiPointBuilder> implements ShapeFactory.MultiPointBuilder {
		@Override
		public Shape build() {
			GeoCompositeAreaShape areaShape = new GeoCompositeAreaShape(planetModel);
			for (GeoPoint point : points) {
				GeoPointShape pointShape = GeoPointShapeFactory.makeGeoPointShape(planetModel, point.getLatitude(), point.getLongitude());
				areaShape.addShape(pointShape);
			}
			return new Geo3dShape<>(areaShape, context);
		}
	}

	private class Geo3dMultiLineBuilder implements ShapeFactory.MultiLineStringBuilder {
		List<ShapeFactory.LineStringBuilder> builders = new ArrayList<>();

		@Override
		public ShapeFactory.LineStringBuilder lineString() {
			return new Geo3dShapeFactory.Geo3dLineStringBuilder();
		}

		@Override
		public ShapeFactory.MultiLineStringBuilder add(ShapeFactory.LineStringBuilder lineStringBuilder) {
			builders.add(lineStringBuilder);
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Shape build() {
			GeoCompositeAreaShape areaShape = new GeoCompositeAreaShape(planetModel);
			for (ShapeFactory.LineStringBuilder builder : builders) {
				Geo3dShape<GeoPolygon> shape = ((Geo3dShape<GeoPolygon>) (builder.build()));
			}
			return new Geo3dShape<>(areaShape, context);
		}
	}

	private class Geo3dMultiPolygonBuilder implements ShapeFactory.MultiPolygonBuilder {
		List<ShapeFactory.PolygonBuilder> builders = new ArrayList<>();

		@Override
		public ShapeFactory.PolygonBuilder polygon() {
			return new Geo3dShapeFactory.Geo3dPolygonBuilder();
		}

		@Override
		public ShapeFactory.MultiPolygonBuilder add(ShapeFactory.PolygonBuilder polygonBuilder) {
			builders.add(polygonBuilder);
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Shape build() {
			GeoCompositeAreaShape areaShape = new GeoCompositeAreaShape(planetModel);
			for (ShapeFactory.PolygonBuilder builder : builders) {
				Geo3dShape<GeoPolygon> shape = ((Geo3dShape<GeoPolygon>) (builder.build()));
			}
			return new Geo3dShape<>(areaShape, context);
		}
	}

	private class Geo3dMultiShapeBuilder<T extends Shape> implements ShapeFactory.MultiShapeBuilder<T> {
		GeoCompositeAreaShape composite = new GeoCompositeAreaShape(planetModel);

		@Override
		public ShapeFactory.MultiShapeBuilder<T> add(T shape) {
			Geo3dShape<?> areaShape = ((Geo3dShape<?>) (shape));
			return this;
		}

		@Override
		public Shape build() {
			return new Geo3dShape<>(composite, context);
		}
	}
}

