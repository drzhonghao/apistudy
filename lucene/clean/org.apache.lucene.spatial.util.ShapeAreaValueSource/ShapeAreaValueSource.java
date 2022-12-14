import org.apache.lucene.spatial.util.*;


import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.spatial.ShapeValues;
import org.apache.lucene.spatial.ShapeValuesSource;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Shape;

/**
 * The area of a Shape retrieved from an ShapeValuesSource
 *
 * @see Shape#getArea(org.locationtech.spatial4j.context.SpatialContext)
 *
 * @lucene.experimental
 */
public class ShapeAreaValueSource extends DoubleValuesSource {
  private final ShapeValuesSource shapeValueSource;
  private final SpatialContext ctx;//not part of identity; should be associated with shapeValueSource indirectly
  private final boolean geoArea;
  private double multiplier;

  public ShapeAreaValueSource(ShapeValuesSource shapeValueSource, SpatialContext ctx, boolean geoArea, double multiplier) {
    this.shapeValueSource = shapeValueSource;
    this.ctx = ctx;
    this.geoArea = geoArea;
    this.multiplier = multiplier;
  }

  @Override
  public String toString() {
    return "area(" + shapeValueSource.toString() + ",geo=" + geoArea + ")";
  }

  @Override
  public DoubleValues getValues(LeafReaderContext readerContext, DoubleValues scores) throws IOException {
    final ShapeValues shapeValues = shapeValueSource.getValues(readerContext);
    return DoubleValues.withDefault(new DoubleValues() {
      @Override
      public double doubleValue() throws IOException {
        return shapeValues.value().getArea(geoArea ? ctx : null) * multiplier;
      }

      @Override
      public boolean advanceExact(int doc) throws IOException {
        return shapeValues.advanceExact(doc);
      }
    }, 0);
  }

  @Override
  public boolean needsScores() {
    return false;
  }

  @Override
  public boolean isCacheable(LeafReaderContext ctx) {
    return shapeValueSource.isCacheable(ctx);
  }

  @Override
  public DoubleValuesSource rewrite(IndexSearcher searcher) throws IOException {
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ShapeAreaValueSource that = (ShapeAreaValueSource) o;

    if (geoArea != that.geoArea) return false;
    if (!shapeValueSource.equals(that.shapeValueSource)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = shapeValueSource.hashCode();
    result = 31 * result + (geoArea ? 1 : 0);
    return result;
  }

}
