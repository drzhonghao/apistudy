import org.apache.lucene.spatial.prefix.*;


import org.locationtech.spatial4j.shape.Point;
import org.apache.lucene.spatial.prefix.tree.Cell;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.util.ShapeFieldCacheProvider;
import org.apache.lucene.util.BytesRef;

/**
 * Implementation of {@link ShapeFieldCacheProvider} designed for {@link PrefixTreeStrategy}s that index points
 * (AND ONLY POINTS!).
 *
 * @lucene.internal
 */
public class PointPrefixTreeFieldCacheProvider extends ShapeFieldCacheProvider<Point> {

  private final SpatialPrefixTree grid;
  private Cell scanCell;//re-used in readShape to save GC

  public PointPrefixTreeFieldCacheProvider(SpatialPrefixTree grid, String shapeField, int defaultSize) {
    super( shapeField, defaultSize );
    this.grid = grid;
  }

  @Override
  protected Point readShape(BytesRef term) {
    scanCell = grid.readCell(term, scanCell);
    if (scanCell.getLevel() == grid.getMaxLevels())
      return scanCell.getShape().getCenter();
    return null;
  }
}
