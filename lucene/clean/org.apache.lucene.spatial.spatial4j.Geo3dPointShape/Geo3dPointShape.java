import org.apache.lucene.spatial.spatial4j.*;


import org.apache.lucene.spatial3d.geom.GeoPointShape;
import org.apache.lucene.spatial3d.geom.GeoPointShapeFactory;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;

/**
 * Specialization of a {@link Geo3dShape} which represents a {@link Point}.
 *
 * @lucene.experimental
 */
public class Geo3dPointShape extends Geo3dShape<GeoPointShape> implements Point {

  public Geo3dPointShape(final GeoPointShape shape, final SpatialContext spatialcontext) {
    super(shape, spatialcontext);
    center = this;
  }

  @Override
  public void reset(double x, double y) {
    shape = GeoPointShapeFactory.makeGeoPointShape(shape.getPlanetModel(),
        y * DistanceUtils.DEGREES_TO_RADIANS,
        x * DistanceUtils.DEGREES_TO_RADIANS);
    center = this;
    boundingBox = null;
  }

  @Override
  public double getX() {
    return shape.getCenter().getLongitude() * DistanceUtils.RADIANS_TO_DEGREES;
  }

  @Override
  public double getY() {
    return shape.getCenter().getLatitude() * DistanceUtils.RADIANS_TO_DEGREES;
  }

  @Override
  public Rectangle getBoundingBox() {
    Rectangle bbox = this.boundingBox;//volatile read once
    if (bbox == null) {
      bbox = new Geo3dRectangleShape(shape, spatialcontext);
      this.boundingBox = bbox;
    }
    return bbox;
  }

  @Override
  public Shape getBuffered(double distance, SpatialContext spatialContext) {
    return spatialContext.getShapeFactory().circle(getX(), getY(), distance);
  }

  @Override
  public boolean hasArea() {
    return false;
  }
}
