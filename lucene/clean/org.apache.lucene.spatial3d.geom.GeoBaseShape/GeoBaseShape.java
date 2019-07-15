import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.Bounds;
import org.apache.lucene.spatial3d.geom.*;


/**
 * Base extended shape object.
 *
 * @lucene.internal
 */
public abstract class GeoBaseShape extends BasePlanetObject implements GeoShape {

  /** Constructor.
   *@param planetModel is the planet model to use.
   */
  public GeoBaseShape(final PlanetModel planetModel) {
    super(planetModel);
  }

  @Override
  public void getBounds(Bounds bounds) {
    if (isWithin(planetModel.NORTH_POLE)) {
      bounds.noTopLatitudeBound().noLongitudeBound()
        .addPoint(planetModel.NORTH_POLE);
    }
    if (isWithin(planetModel.SOUTH_POLE)) {
      bounds.noBottomLatitudeBound().noLongitudeBound()
        .addPoint(planetModel.SOUTH_POLE);
    }
    if (isWithin(planetModel.MIN_X_POLE)) {
      bounds.addPoint(planetModel.MIN_X_POLE);
    }
    if (isWithin(planetModel.MAX_X_POLE)) {
      bounds.addPoint(planetModel.MAX_X_POLE);
    }
    if (isWithin(planetModel.MIN_Y_POLE)) {
      bounds.addPoint(planetModel.MIN_Y_POLE);
    }
    if (isWithin(planetModel.MAX_Y_POLE)) {
      bounds.addPoint(planetModel.MAX_Y_POLE);
    }
  }

}


