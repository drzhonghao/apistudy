import org.apache.lucene.spatial.spatial4j.*;


import org.apache.lucene.spatial3d.geom.GeoCircle;
import org.apache.lucene.spatial3d.geom.GeoCircleFactory;
import org.apache.lucene.spatial3d.geom.GeoPointShapeFactory;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Circle;
import org.locationtech.spatial4j.shape.Point;

/**
 * Specialization of a {@link Geo3dShape} which represents a {@link Circle}.
 *
 * @lucene.experimental
 */
public class Geo3dCircleShape extends Geo3dShape<GeoCircle> implements Circle {

  public Geo3dCircleShape(final GeoCircle shape, final SpatialContext spatialcontext) {
    super(shape, spatialcontext);
  }

  @Override
  public void reset(double x, double y, double radiusDEG) {
    shape = GeoCircleFactory.makeGeoCircle(shape.getPlanetModel(),
        y * DistanceUtils.DEGREES_TO_RADIANS,
        x * DistanceUtils.DEGREES_TO_RADIANS,
        radiusDEG * DistanceUtils.DEGREES_TO_RADIANS);
    center = null;
    boundingBox = null;
  }

  @Override
  public double getRadius() {
    return shape.getRadius() * DistanceUtils.RADIANS_TO_DEGREES;
  }

  @Override
  public Point getCenter() {
    Point center = this.center;//volatile read once
    if (center == null) {
      center = new Geo3dPointShape(
          GeoPointShapeFactory.makeGeoPointShape(shape.getPlanetModel(),
              shape.getCenter().getLatitude(),
              shape.getCenter().getLongitude()),
          spatialcontext);
      this.center = center;
    }
    return center;
  }
}
