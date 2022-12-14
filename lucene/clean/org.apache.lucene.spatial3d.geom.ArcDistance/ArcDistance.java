import org.apache.lucene.spatial3d.geom.*;


/**
 * Arc distance computation style.
 *
 * @lucene.experimental
 */
public class ArcDistance implements DistanceStyle {
  
  /** An instance of the ArcDistance DistanceStyle. */
  public final static ArcDistance INSTANCE = new ArcDistance();
  
  /** Constructor.
   */
  public ArcDistance() {
  }
  
  @Override
  public double computeDistance(final GeoPoint point1, final GeoPoint point2) {
    return point1.arcDistance(point2);
  }
  
  @Override
  public double computeDistance(final GeoPoint point1, final double x2, final double y2, final double z2) {
    return point1.arcDistance(x2,y2,z2);
  }

  @Override
  public double computeDistance(final PlanetModel planetModel, final Plane plane, final GeoPoint point, final Membership... bounds) {
    return plane.arcDistance(planetModel, point, bounds);
  }
  
  @Override
  public double computeDistance(final PlanetModel planetModel, final Plane plane, final double x, final double y, final double z, final Membership... bounds) {
    return plane.arcDistance(planetModel, x,y,z, bounds);
  }

  @Override
  public GeoPoint[] findDistancePoints(final PlanetModel planetModel, final double distanceValue, final GeoPoint startPoint, final Plane plane, final Membership... bounds) {
    return plane.findArcDistancePoints(planetModel, distanceValue, startPoint, bounds);
  }
  
  @Override
  public double findMinimumArcDistance(final PlanetModel planetModel, final double distanceValue) {
    return distanceValue;
  }
  
  @Override
  public double findMaximumArcDistance(final PlanetModel planetModel, final double distanceValue) {
    return distanceValue;
  }

}


