import org.apache.lucene.spatial.util.*;


import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;

/**
 * Transforms a DoubleValuesSource using the formula v = k / (v + k)
 */
public class ReciprocalDoubleValuesSource extends DoubleValuesSource {

  private final double distToEdge;
  private final DoubleValuesSource input;

  /**
   * Creates a ReciprocalDoubleValuesSource
   * @param distToEdge  the value k in v = k / (v + k)
   * @param input       the input DoubleValuesSource to transform
   */
  public ReciprocalDoubleValuesSource(double distToEdge, DoubleValuesSource input) {
    this.distToEdge = distToEdge;
    this.input = input;
  }

  @Override
  public DoubleValues getValues(LeafReaderContext ctx, DoubleValues scores) throws IOException {
    DoubleValues in = input.getValues(ctx, scores);
    return new DoubleValues() {
      @Override
      public double doubleValue() throws IOException {
        return recip(in.doubleValue());
      }

      @Override
      public boolean advanceExact(int doc) throws IOException {
        return in.advanceExact(doc);
      }
    };
  }

  private double recip(double in) {
    return distToEdge / (in + distToEdge);
  }

  @Override
  public boolean needsScores() {
    return input.needsScores();
  }

  @Override
  public boolean isCacheable(LeafReaderContext ctx) {
    return input.isCacheable(ctx);
  }

  @Override
  public Explanation explain(LeafReaderContext ctx, int docId, Explanation scoreExplanation) throws IOException {
    Explanation expl = input.explain(ctx, docId, scoreExplanation);
    return Explanation.match((float)recip(expl.getValue()),
        distToEdge + " / (v + " + distToEdge + "), computed from:", expl);
  }

  @Override
  public DoubleValuesSource rewrite(IndexSearcher searcher) throws IOException {
    return new ReciprocalDoubleValuesSource(distToEdge, input.rewrite(searcher));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReciprocalDoubleValuesSource that = (ReciprocalDoubleValuesSource) o;
    return Double.compare(that.distToEdge, distToEdge) == 0 &&
        Objects.equals(input, that.input);
  }

  @Override
  public int hashCode() {
    return Objects.hash(distToEdge, input);
  }

  @Override
  public String toString() {
    return "recip(" + distToEdge + ", " + input.toString() + ")";
  }
}
