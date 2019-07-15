import org.apache.lucene.spatial3d.geom.GeoArea;
import org.apache.lucene.spatial3d.geom.PlanetModel;
import org.apache.lucene.spatial3d.geom.GeoBBoxFactory;
import org.apache.lucene.spatial3d.geom.XYZSolidFactory;
import org.apache.lucene.spatial3d.geom.*;


/**
 * Factory for {@link GeoArea}.
 *
 * @lucene.experimental
 */
public class GeoAreaFactory {
  private GeoAreaFactory() {
  }

  /**
   * Create a GeoArea of the right kind given the specified bounds.
   * @param planetModel is the planet model
   * @param topLat    is the top latitude
   * @param bottomLat is the bottom latitude
   * @param leftLon   is the left longitude
   * @param rightLon  is the right longitude
   * @return a GeoArea corresponding to what was specified.
   */
  public static GeoArea makeGeoArea(final PlanetModel planetModel, final double topLat, final double bottomLat, final double leftLon, final double rightLon) {
    return GeoBBoxFactory.makeGeoBBox(planetModel, topLat, bottomLat, leftLon, rightLon);
  }

  /**
   * Create a GeoArea of the right kind given (x,y,z) bounds.
   * @param planetModel is the planet model
   * @param minX is the min X boundary
   * @param maxX is the max X boundary
   * @param minY is the min Y boundary
   * @param maxY is the max Y boundary
   * @param minZ is the min Z boundary
   * @param maxZ is the max Z boundary
   */
  public static GeoArea makeGeoArea(final PlanetModel planetModel, final double minX, final double maxX, final double minY, final double maxY, final double minZ, final double maxZ) {
    return XYZSolidFactory.makeXYZSolid(planetModel, minX, maxX, minY, maxY, minZ, maxZ);
  }
  
}
