import org.apache.lucene.spatial3d.geom.*;


import java.io.OutputStream;
import java.io.IOException;

/**
 * All Geo3D shapes can derive from this base class, which furnishes
 * some common code
 *
 * @lucene.internal
 */
public abstract class BasePlanetObject implements PlanetObject {

  /** This is the planet model embedded in all objects derived from this
   * class. */
  protected final PlanetModel planetModel;
  
  /** Constructor creating class instance given a planet model.
   * @param planetModel is the planet model.
   */
  public BasePlanetObject(final PlanetModel planetModel) {
    this.planetModel = planetModel;
  }

  @Override
  public PlanetModel getPlanetModel() {
    return planetModel;
  }
  
  @Override
  public void write(final OutputStream outputStream) throws IOException {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public int hashCode() {
    return planetModel.hashCode();
  }
  
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof BasePlanetObject))
      return false;
    return planetModel.equals(((BasePlanetObject)o).planetModel);
  }
  
  
}



