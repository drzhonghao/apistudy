import org.apache.lucene.spatial3d.geom.DistanceStyle;
import org.apache.lucene.spatial3d.geom.GeoPoint;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.Plane;
import org.apache.lucene.spatial3d.geom.*;


/**
 * Normal squared distance computation style.
 *
 * @lucene.experimental
 */
public class NormalSquaredDistance implements DistanceStyle {
  
  /** A convenient instance */
  public final static NormalSquaredDistance INSTANCE = new NormalSquaredDistance();
  
  /** Constructor.
   */
  public NormalSquaredDistance() {
  }
  
  @Override
  public double computeDistance(final GeoPoint point1, final GeoPoint point2) {
    return point1.normalDistanceSquared(point2);
  }
  
  @Override
  public double computeDistance(final GeoPoint point1, final double x2, final double y2, final double z2) {
    return point1.normalDistanceSquared(x2,y2,z2);
  }

  @Override
  public double computeDistance(final PlanetModel planetModel, final Plane plane, final GeoPoint point, final Membership... bounds) {
    return plane.normalDistanceSquared(point, bounds);
  }
  
  @Override
  public double computeDistance(final PlanetModel planetModel, final Plane plane, final double x, final double y, final double z, final Membership... bounds) {
    return plane.normalDistanceSquared(x,y,z, bounds);
  }

  @Override
  public double toAggregationForm(final double distance) {
    return Math.sqrt(distance);
  }

  @Override
  public double fromAggregationForm(final double aggregateDistance) {
    return aggregateDistance * aggregateDistance;
  }

  @Override
  public GeoPoint[] findDistancePoints(final PlanetModel planetModel, final double distanceValue, final GeoPoint startPoint, final Plane plane, final Membership... bounds) {
    throw new IllegalStateException("Reverse mapping not implemented for this distance metric");
  }
  
  @Override
  public double findMinimumArcDistance(final PlanetModel planetModel, final double distanceValue) {
    throw new IllegalStateException("Reverse mapping not implemented for this distance metric");
  }
  
  @Override
  public double findMaximumArcDistance(final PlanetModel planetModel, final double distanceValue) {
    throw new IllegalStateException("Reverse mapping not implemented for this distance metric");
  }

}


